import os
from http import HTTPStatus
from uuid import UUID
from dotenv import load_dotenv

from auth.auth_service_impl import AuthService, AuthenticationError
from enhanced_bim_client import EnhancedBimPortalClient
from auth.auth_config import BIM_PORTAL_USERNAME_ENV_VAR

# --- Configuration ---
load_dotenv()
from config import BIMPortalConfig
BASE_URL = BIMPortalConfig.BASE_URL
# A known private resource to test authentication against
PRIVATE_RESOURCE_GUID = BIMPortalConfig.PRIVATE_RESOURCE_GUID
# A known public resource for fallback testing
PUBLIC_RESOURCE_GUID = BIMPortalConfig.PUBLIC_RESOURCE_GUID

def test_api_connectivity():
    """Test basic API connectivity without authentication."""
    print("Step 1: Testing basic API connectivity...")
    try:
        auth_service = AuthService(guid=PUBLIC_RESOURCE_GUID, username=None, password=None)
        client = EnhancedBimPortalClient(auth_service=auth_service, base_url=BASE_URL)
        
        # Try to search for public projects
        projects = client.search_projects()
        if projects:
            print(f"   Success: Found {len(projects)} public projects")
            return True
        else:
            print("   Warning: API connected but no public projects found")
            return True
    except Exception as e:
        print(f"   Error: API connectivity failed - {e}")
        return False

def test_authentication():
    """Test authentication with credentials."""
    print("\nStep 2: Testing authentication...")
    
    if not os.getenv(BIM_PORTAL_USERNAME_ENV_VAR):
        print("   Error: Credentials not found")
        print("   Please run 'python demo/credentials_setup.py' first")
        return False
    
    print("   Credentials found in .env file")
    
    try:
        auth_service = AuthService(guid=PRIVATE_RESOURCE_GUID)
        client = EnhancedBimPortalClient(auth_service=auth_service, base_url=BASE_URL)
        print("   Client setup complete")
        
        # Test authentication by accessing a private resource
        loin = client.get_loin(UUID(PRIVATE_RESOURCE_GUID))
        if loin:
            print(f"   Success: Authenticated and fetched private resource '{loin.name}'")
            return True
        else:
            print("   Error: Authentication may have failed - could not access private resource")
            return False
            
    except AuthenticationError as e:
        print(f"   Error: Authentication failed - {e}")
        return False
    except Exception as e:
        print(f"   Error: Unexpected error during authentication test - {e}")
        return False

def test_api_features(client: EnhancedBimPortalClient):
    """Test various API features."""
    print("\nStep 3: Testing API features...")
    
    features_passed = 0
    total_features = 4
    
    # Test 1: Project search
    try:
        projects = client.search_projects()
        if projects:
            print(f"   Project search: SUCCESS ({len(projects)} projects found)")
            features_passed += 1
        else:
            print("   Project search: PARTIAL (no projects found)")
    except Exception as e:
        print(f"   Project search: FAILED - {e}")
    
    # Test 2: Project details
    try:
        projects = client.search_projects()
        if projects:
            project_details = client.get_project(projects[0].guid)
            if project_details:
                print(f"   Project details: SUCCESS (retrieved '{project_details.name}')")
                features_passed += 1
            else:
                print("   Project details: FAILED (could not retrieve details)")
    except Exception as e:
        print(f"   Project details: FAILED - {e}")
    
    # Test 3: Property search  
    try:
        properties = client.search_properties()
        if properties:
            print(f"   Property search: SUCCESS ({len(properties)} properties found)")
            features_passed += 1
        else:
            print("   Property search: PARTIAL (no properties found)")
    except Exception as e:
        print(f"   Property search: FAILED - {e}")
    
    # Test 4: LOIN search
    try:
        loins = client.search_loins()
        if loins:
            print(f"   LOIN search: SUCCESS ({len(loins)} LOINs found)")
            features_passed += 1
        else:
            print("   LOIN search: PARTIAL (no LOINs found)")
    except Exception as e:
        print(f"   LOIN search: FAILED - {e}")
    
    return features_passed, total_features

def test_export_functionality(client: EnhancedBimPortalClient):
    """Test export functionality."""
    print("\nStep 4: Testing export functionality...")
    
    try:
        projects = client.search_projects()
        if projects:
            # Test PDF export
            pdf_content = client.export_project_pdf(projects[0].guid)
            if pdf_content:
                print("   PDF export: SUCCESS")
                return True
            else:
                print("   PDF export: FAILED")
                return False
        else:
            print("   PDF export: SKIPPED (no projects to export)")
            return False
    except Exception as e:
        print(f"   PDF export: FAILED - {e}")
        return False

def main():
    """
    Comprehensive health check for the BIM Portal API with Pydantic client.
    """
    print("--- BIM Portal API Health Check (Enhanced Pydantic Client) ---")
    
    # Test 1: Basic connectivity
    connectivity_ok = test_api_connectivity()
    
    # Test 2: Authentication
    auth_ok = test_authentication()
    
    if not auth_ok:
        print("\nHealth check stopped - authentication failed")
        print("Please verify your credentials and try again")
        return
    
    # Set up authenticated client for remaining tests
    auth_service = AuthService(guid=PRIVATE_RESOURCE_GUID)
    client = EnhancedBimPortalClient(auth_service=auth_service, base_url=BASE_URL)
    
    # Test 3: API features
    features_passed, total_features = test_api_features(client)
    
    # Test 4: Export functionality
    export_ok = test_export_functionality(client)
    
    # Summary
    print("\n" + "="*50)
    print("HEALTH CHECK SUMMARY")
    print("="*50)
    print(f"API Connectivity:    {'PASS' if connectivity_ok else 'FAIL'}")
    print(f"Authentication:      {'PASS' if auth_ok else 'FAIL'}")
    print(f"API Features:        {features_passed}/{total_features} PASS")
    print(f"Export Functions:    {'PASS' if export_ok else 'FAIL'}")
    
    # Overall assessment
    if connectivity_ok and auth_ok and features_passed >= 3 and export_ok:
        print("\nOVERALL STATUS: HEALTHY")
        print("The BIM Portal API integration is working correctly!")
    elif connectivity_ok and auth_ok and features_passed >= 2:
        print("\nOVERALL STATUS: MOSTLY HEALTHY")  
        print("Basic functionality works, some features may have issues")
    else:
        print("\nOVERALL STATUS: UNHEALTHY")
        print("Significant issues detected - check configuration and credentials")
    
    print("="*50)

if __name__ == "__main__":
    main()