import argparse
import os
from uuid import UUID
from dotenv import load_dotenv

from auth.auth_service_impl import AuthService
from enhanced_bim_client import EnhancedBimPortalClient
from models import AiaProjectForPublicRequest

# --- Configuration ---
load_dotenv()
from config import BIMPortalConfig
BASE_URL = BIMPortalConfig.BASE_URL
# A default GUID for the auth context. The actual GUID of the resource
# being accessed is more specific and should be used if possible.
DEFAULT_AUTH_GUID = BIMPortalConfig.DEFAULT_AUTH_GUID


def setup_client(guid: str) -> EnhancedBimPortalClient:
    """Sets up the enhanced client with a specific GUID for context."""
    auth_service = AuthService(guid=guid)
    return EnhancedBimPortalClient(auth_service=auth_service, base_url=BASE_URL)

def handle_get_project(args):
    """Handles the 'get-project' command."""
    print(f"Fetching project with GUID: {args.guid}")
    try:
        client = setup_client(str(args.guid))
        project = client.get_project(args.guid)

        if project:
            print("\n--- Project Details ---")
            print(f"Name:         {project.name}")
            print(f"GUID:         {project.guid}")
            print(f"Description:  {project.description}")
            print(f"Version:      {project.versionNumber}")
            print(f"Organisation: {project.organisationName}")
            
            if project.coordinateSystem:
                coord = project.coordinateSystem
                print(f"Coord System: {coord.name} (Zone: {coord.zone})")
            
            if project.models:
                print(f"Models:       {len(project.models)} available")
                for model in project.models[:3]:  # Show first 3
                    print(f"  - {model.name}")
            
            print("-----------------------")
        else:
            print("Error: Could not retrieve project or project not found.")

    except Exception as e:
        print(f"An unexpected error occurred: {e}")

def handle_search_projects(args):
    """Handles the 'search-projects' command."""
    search_term = args.name
    print(f"Searching for projects with name containing: '{search_term}'")
    try:
        # For search, a default auth context is usually sufficient.
        client = setup_client(DEFAULT_AUTH_GUID)
        
        if search_term:
            search_request = AiaProjectForPublicRequest(searchString=search_term)
            projects = client.search_projects(search_request)
        else:
            projects = client.search_projects()

        if not projects:
            print("No projects found matching your criteria.")
            return

        print(f"\n--- Found {len(projects)} Projects ---")
        for proj in projects:
            print(f"- {proj.name} (GUID: {proj.guid})")
            if proj.description:
                print(f"  Description: {proj.description}")
            if proj.organisation:
                print(f"  Organisation: {proj.organisation}")
        print("--------------------------")

    except Exception as e:
        print(f"An unexpected error occurred: {e}")

def handle_export_project(args):
    """Handles the 'export-project' command."""
    print(f"Exporting project {args.guid} as PDF...")
    try:
        client = setup_client(str(args.guid))
        pdf_content = client.export_project_pdf(args.guid)
        
        if pdf_content:
            filename = args.output or f"project_{args.guid}.pdf"
            with open(filename, "wb") as f:
                f.write(pdf_content)
            print(f"Success: Project exported to {filename}")
        else:
            print("Error: Failed to export project as PDF")
    
    except Exception as e:
        print(f"An unexpected error occurred: {e}")

def handle_list_properties(args):
    """Handles the 'list-properties' command."""
    search_term = args.search if hasattr(args, 'search') else "a"
    print(f"Searching for properties containing: '{search_term}'")
    try:
        client = setup_client(DEFAULT_AUTH_GUID)
        properties = client.search_properties()
        
        if not properties:
            print("No properties found.")
            return
        
        # Filter by search term if provided
        if hasattr(args, 'search') and args.search:
            filtered_properties = [p for p in properties if args.search.lower() in p.name.lower()]
            properties = filtered_properties
        
        print(f"\n--- Found {len(properties)} Properties ---")
        for prop in properties[:20]:  # Limit to first 20
            category = prop.category.value if prop.category else "None"
            print(f"- {prop.name} (Category: {category}, Type: {prop.dataType})")
            if prop.units:
                print(f"  Units: {', '.join(prop.units[:3])}")
        
        if len(properties) > 20:
            print(f"... and {len(properties) - 20} more")
        print("--------------------------")
    
    except Exception as e:
        print(f"An unexpected error occurred: {e}")

def main():
    """
    Main function to parse arguments and dispatch commands.
    """
    if not os.getenv("BIM_PORTAL_USERNAME"):
        print("Warning: Credentials not found in .env file.")
        print("Please run 'python demo/credentials_setup.py' first.")
        print("The CLI might fail if you access private resources.")

    parser = argparse.ArgumentParser(
        description="A CLI tool to interact with the BIM Portal API using Pydantic models."
    )
    subparsers = parser.add_subparsers(dest="command", required=True)

    # --- get-project command ---
    parser_get = subparsers.add_parser("get-project", help="Get details for a specific project.")
    parser_get.add_argument("guid", type=UUID, help="The GUID of the project to fetch.")
    parser_get.set_defaults(func=handle_get_project)

    # --- search-projects command ---
    parser_search = subparsers.add_parser("search-projects", help="Search for projects by name.")
    parser_search.add_argument("name", type=str, nargs='?', help="The search term for the project name (optional).")
    parser_search.set_defaults(func=handle_search_projects)

    # --- export-project command ---
    parser_export = subparsers.add_parser("export-project", help="Export a project as PDF.")
    parser_export.add_argument("guid", type=UUID, help="The GUID of the project to export.")
    parser_export.add_argument("-o", "--output", type=str, help="Output filename (default: project_<guid>.pdf)")
    parser_export.set_defaults(func=handle_export_project)

    # --- list-properties command ---
    parser_props = subparsers.add_parser("list-properties", help="List available properties.")
    parser_props.add_argument("-s", "--search", type=str, help="Search term to filter properties.")
    parser_props.set_defaults(func=handle_list_properties)

    args = parser.parse_args()
    args.func(args)


if __name__ == "__main__":
    main()