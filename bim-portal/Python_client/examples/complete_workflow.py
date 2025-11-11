"""
Complete workflow example using the new Enhanced BIM Portal client.
This demonstrates a robust approach that handles API inconsistencies gracefully.
"""

import os
from dotenv import load_dotenv
from uuid import UUID

from auth.auth_service_impl import AuthService
from enhanced_bim_client import EnhancedBimPortalClient
from models import (
    AiaProjectForPublicRequest,
    PropertyOrGroupForPublicRequest
)

# --- Configuration ---
load_dotenv()
from config import BIMPortalConfig
BASE_URL = BIMPortalConfig.BASE_URL
AUTH_GUID = BIMPortalConfig.DEFAULT_AUTH_GUID


def setup_client() -> EnhancedBimPortalClient:
    """Sets up the enhanced BIM Portal client with integrated authentication."""
    auth_service = AuthService(guid=AUTH_GUID)
    return EnhancedBimPortalClient(auth_service=auth_service, base_url=BASE_URL)


def run_complete_workflow(client: EnhancedBimPortalClient):
    """
    Demonstrates a complete, end-to-end workflow using Pydantic models.
    This workflow finds a project, inspects its properties, and exports it.
    """
    print("--- Running Complete End-to-End Workflow with Pydantic Models ---")
    
    # --- Step 1: Find a project to work with ---
    print("\nStep 1: Searching for projects...")
    try:
        search_request = AiaProjectForPublicRequest()  # Empty request for all public projects
        projects = client.search_projects(search_request)
        
        if not projects:
            print("X Workflow failed: No projects found.")
            return
        
        project_to_process = projects[0]
        print(f"✓ Found {len(projects)} projects. Using: '{project_to_process.name}' ({project_to_process.guid})")
    except Exception as e:
        print(f"X Error in Step 1: {e}")
        return

    # --- Step 2: Get the full details of the project ---
    print(f"\nStep 2: Fetching full details for project '{project_to_process.name}'...")
    try:
        project_details = client.get_project(project_to_process.guid)
        
        if project_details:
            print("✓ Successfully fetched project details using Pydantic models.")
            print(f"  - Name: {project_details.name}")
            print(f"  - Description: {project_details.description}")
            print(f"  - Version: {project_details.versionNumber}")
            print(f"  - Organisation: {project_details.organisationName}")
            print(f"  - Template Type: {project_details.templateType}")
            
            # Show coordinate system if available
            if project_details.coordinateSystem:
                coord = project_details.coordinateSystem
                print(f"  - Coordinate System: {coord.name} (Zone: {coord.zone})")
            
            # Show models if available
            if project_details.models:
                print(f"  - Models: {len(project_details.models)} available")
                for model in project_details.models[:3]:  # Show first 3
                    print(f"    * {model.name} ({model.guid})")
            
            # Show data formats if available
            if project_details.dataFormats:
                print(f"  - Data Formats: {len(project_details.dataFormats)} available")
        else:
            print("X Could not fetch project details.")
    except Exception as e:
        print(f"X Error in Step 2: {e}")

    # --- Step 3: Find relevant properties ---
    print("\nStep 3: Searching for properties...")
    try:
        property_search = PropertyOrGroupForPublicRequest(searchString="a")
        properties = client.search_properties(property_search)
        
        if properties:
            print(f"✓ Found {len(properties)} properties using Pydantic models.")
            for prop in properties[:5]:  # Show first 5
                category_str = prop.category.value if prop.category else "None"
                print(f"  - Property: {prop.name} (Category: {category_str})")
                if prop.dataType:
                    print(f"    Data Type: {prop.dataType}")
                if prop.units:
                    print(f"    Units: {', '.join(prop.units[:3])}")  # First 3 units
        else:
            print("i No properties found.")
    except Exception as e:
        print(f"X Error in Step 3: {e}")

    # --- Step 4: Get details of a specific property ---
    print("\nStep 4: Getting detailed property information...")
    try:
        if properties and len(properties) > 0:
            first_property = properties[0]
            if first_property.guid:
                property_details = client.get_property(first_property.guid)
                
                if property_details:
                    # PropertyDto stores names in namesInLanguage array, not direct name field
                    property_name = first_property.name  # Use name from search results
                    if property_details.namesInLanguage and len(property_details.namesInLanguage) > 0:
                        property_name = property_details.namesInLanguage[0].name
                    
                    print(f"✓ Successfully fetched details for property: {property_name}")
                    print(f"  - GUID: {property_details.guid}")
                    print(f"  - Data Type: {property_details.dataType}")
                    print(f"  - Organisation: {property_details.organisationName}")
                    
                    if property_details.dimension:
                        print(f"  - Dimension: {property_details.dimension}")
                    
                    if property_details.namesInLanguage:
                        print(f"  - Available names: {len(property_details.namesInLanguage)}")
                        for name_info in property_details.namesInLanguage[:2]:  # Show first 2
                            print(f"    * {name_info.name} ({name_info.language})")
                    
                    if property_details.units:
                        print(f"  - Units: {', '.join(property_details.units[:3])}")
                    
                    if property_details.definitionsInLanguage:
                        print(f"  - Definitions available: {len(property_details.definitionsInLanguage)}")
                        for def_info in property_details.definitionsInLanguage[:1]:  # Show first definition
                            if def_info.definition:
                                print(f"    * {def_info.definition[:100]}..." if len(def_info.definition) > 100 else f"    * {def_info.definition}")
                else:
                    print("X Could not fetch property details.")
            else:
                print("i Skipping property details - no GUID available.")
        else:
            print("i Skipping property details - no properties to examine.")
    except Exception as e:
        print(f"X Error in Step 4: {e}")

    # --- Step 5: Export the project for verification ---
    print(f"\nStep 5: Exporting project '{project_to_process.name}' to PDF...")
    try:
        pdf_content = client.export_project_pdf(project_to_process.guid)
        
        if pdf_content:
            filename = f"enhanced_export_{project_to_process.guid}.pdf"
            with open(filename, "wb") as f:
                f.write(pdf_content)
            print(f"✓ Successfully exported project to {filename}")
        else:
            print("X Could not export project to PDF.")
    except Exception as e:
        print(f"X Error in Step 5: {e}")

    print("\n--- Workflow completed ---")
    print("\n=== ENHANCED CLIENT BENEFITS DEMONSTRATED ===")
    print("- Integrated authentication handling with retry logic")
    print("- Graceful handling of null values in API responses")
    print("- Type-safe access to all fields")
    print("- Clean, readable code with proper error handling")


def demonstrate_error_handling(client: EnhancedBimPortalClient):
    """Demonstrate how Pydantic handles malformed data gracefully."""
    print("\n=== DEMONSTRATING ERROR HANDLING ===")
    
    # Try to get a non-existent project
    fake_guid = UUID('00000000-0000-0000-0000-000000000000')
    result = client.get_project(fake_guid)
    print(f"Non-existent project result: {result}")


if __name__ == "__main__":
    if not os.getenv("BIM_PORTAL_USERNAME"):
        print("⚠️ Credentials not found. The workflow may fail if it encounters private resources.")

    client = setup_client()
    run_complete_workflow(client)
    demonstrate_error_handling(client)
