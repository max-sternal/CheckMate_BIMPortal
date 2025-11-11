"""
Debug and fix export issues for BIM Portal API.
"""

import os
import json
from pathlib import Path
from uuid import UUID

from dotenv import load_dotenv

from auth.auth_config import (BIM_PORTAL_PASSWORD_ENV_VAR,
                              BIM_PORTAL_USERNAME_ENV_VAR)
from auth.auth_service_impl import AuthService
from enhanced_bim_client import EnhancedBimPortalClient
from config import BIMPortalConfig

# --- Configuration ---
load_dotenv()
BASE_URL = BIMPortalConfig.BASE_URL
PRIVATE_LOIN_GUID = BIMPortalConfig.PRIVATE_RESOURCE_GUID


def check_credentials() -> bool:
    """Checks if credentials are available."""
    if not os.getenv(BIM_PORTAL_USERNAME_ENV_VAR) or not os.getenv(BIM_PORTAL_PASSWORD_ENV_VAR):
        print("\n" + "="*60)
        print("⚠️  WARNING: Credentials not found in environment variables.")
        print(f"🔑 Please set {BIM_PORTAL_USERNAME_ENV_VAR} and {BIM_PORTAL_PASSWORD_ENV_VAR} in a .env file.")
        print("⏭️  Skipping examples that require authentication.")
        print("="*60 + "\n")
        return False
    return True

def debug_filter_structure(client: EnhancedBimPortalClient):
    """Debug filter structure to identify the correct attribute names."""
    print("DEBUG: Analyzing filter structure...")
    
    try:
        # Get raw response for property filters
        response = client.get("/merkmale/api/v1/public/filter")
        if response.status_code == 200:
            raw_data = response.json()
            print(f"Raw filter response: {json.dumps(raw_data, indent=2)}")
            
            if isinstance(raw_data, list) and raw_data:
                first_filter = raw_data[0]
                print(f"Available attributes: {list(first_filter.keys())}")
        else:
            print(f"Failed to get filters: {response.status_code}")
            
    except Exception as e:
        print(f"Error debugging filters: {e}")

def find_exportable_projects(client: EnhancedBimPortalClient):
    """Find projects that can actually be exported."""
    print("\nDEBUG: Finding exportable projects...")
    
    try:
        projects = client.search_projects()
        exportable_projects = []
        
        for project in projects[:10]:  # Test first 10
            # Check if project has detailed information that might indicate export capability
            detailed_project = client.get_project(project.guid)
            if detailed_project:
                has_models = hasattr(detailed_project, 'models') and detailed_project.models
                has_loins = hasattr(detailed_project, 'loins') and detailed_project.loins
                
                print(f"Project: {project.name}")
                print(f"  Has models: {has_models}")
                print(f"  Has LOINs: {has_loins}")
                print(f"  Version: {getattr(detailed_project, 'versionNumber', 'Unknown')}")
                print(f"  Organization: {getattr(detailed_project, 'organisationName', 'Unknown')}")
                
                # Test if PDF export works
                response = client.get(f"/aia/api/v1/public/aiaProject/{project.guid}/pdf")
                print(f"  PDF export status: {response.status_code}")
                
                if response.status_code == 200:
                    exportable_projects.append(project)
                    print("  ✅ EXPORTABLE")
                else:
                    print("  ❌ NOT EXPORTABLE")
                print()
        
        print(f"Found {len(exportable_projects)} exportable projects out of {len(projects[:10])} tested")
        return exportable_projects
        
    except Exception as e:
        print(f"Error finding exportable projects: {e}")
        return []

def fixed_filter_example(client: EnhancedBimPortalClient):
    """Fixed version of filter example with proper attribute handling."""
    print("\nFIXED: Property filters with proper attribute handling...")
    
    try:
        # Get raw response first
        response = client.get("/merkmale/api/v1/public/filter")
        if response.status_code == 200:
            raw_filters = response.json()
            
            print(f"Found {len(raw_filters)} filter groups:")
            for filter_group in raw_filters[:3]:
                print(f"  Filter Group: {filter_group.get('name', 'Unnamed')}")
                
                # Check all possible attribute names for filters
                filter_items = None
                for attr in ['filters', 'filterItems', 'items', 'values', 'options', 'children']:
                    if attr in filter_group:
                        filter_items = filter_group[attr]
                        print(f"    Found filters under attribute: '{attr}'")
                        break
                
                if filter_items and isinstance(filter_items, list):
                    print(f"    Contains {len(filter_items)} filter items:")
                    for item in filter_items[:3]:
                        item_name = item.get('name', item.get('value', item.get('label', 'Unknown')))
                        print(f"      - {item_name}")
                else:
                    print(f"    No filter items found (available keys: {list(filter_group.keys())})")
                print()
        else:
            print(f"Failed to get property filters: {response.status_code}")
            
    except Exception as e:
        print(f"Error in fixed filter example: {e}")

def test_export_formats_systematically(client: EnhancedBimPortalClient):
    """Test all export formats systematically to understand patterns."""
    print("\nDEBUG: Testing export patterns...")
    
    # Test LOIN exports (known to work)
    print("1. Testing LOIN exports (baseline)...")
    try:
        loins = client.search_loins()
        if loins:
            test_loin = loins[0]
            formats = ['pdf', 'openOffice', 'okstra', 'loinXML', 'IDS']
            
            print(f"Testing LOIN '{test_loin.name}':")
            for fmt in formats:
                endpoint = f"/aia/api/v1/public/loin/{test_loin.guid}/{fmt}"
                response = client.get(endpoint)
                print(f"  {fmt}: {response.status_code}")
    except Exception as e:
        print(f"Error testing LOIN exports: {e}")
    
    # Test project exports (known to fail)
    print("\n2. Testing project exports (problematic)...")
    try:
        projects = client.search_projects()
        if projects:
            test_project = projects[1]  # Try the second project (Beispiel_AIA_P_SST worked in batch)
            formats = ['pdf', 'openOffice', 'okstra', 'loinXML', 'IDS']
            
            print(f"Testing project '{test_project.name}':")
            for fmt in formats:
                endpoint = f"/aia/api/v1/public/aiaProject/{test_project.guid}/{fmt}"
                response = client.get(endpoint)
                print(f"  {fmt}: {response.status_code}")
                
                if response.status_code not in [200, 404]:
                    print(f"    Response: {response.text[:100]}")
    except Exception as e:
        print(f"Error testing project exports: {e}")

def main():
    """Run focused debugging to identify and fix issues."""
    print("DEBUG: Starting focused debugging session...")
    
    if not check_credentials():
        return
    
    auth_service = AuthService(guid=PRIVATE_LOIN_GUID)
    client = EnhancedBimPortalClient(auth_service=auth_service, base_url=BASE_URL)
    
    # Debug the specific issues
    debug_filter_structure(client)
    find_exportable_projects(client)
    fixed_filter_example(client)
    test_export_formats_systematically(client)
    
    print("\nDEBUG SESSION COMPLETE")

if __name__ == "__main__":
    main()