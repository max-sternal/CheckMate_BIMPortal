"""
Project examples demonstrating various project-related operations.
Uses the enhanced Pydantic client with error handling.
"""

import os
from uuid import UUID
from dotenv import load_dotenv
from pathlib import Path

from auth.auth_service_impl import AuthService
from enhanced_bim_client import EnhancedBimPortalClient
from models import AiaProjectForPublicRequest

# --- Configuration ---
load_dotenv()
from config import BIMPortalConfig
BASE_URL = BIMPortalConfig.BASE_URL
AUTH_GUID = BIMPortalConfig.DEFAULT_AUTH_GUID


def setup_client() -> EnhancedBimPortalClient:
    """Sets up the enhanced Pydantic client."""
    auth_service = AuthService(guid=AUTH_GUID)
    client = EnhancedBimPortalClient(auth_service=auth_service, base_url=BASE_URL)
    return client

def run_basic_project_examples(client: EnhancedBimPortalClient):
    """Demonstrates basic project operations."""
    print("--- Running Basic Project Examples ---")

    # Example 1: Search for all projects
    print("\n1. Searching for all available projects...")
    try:
        projects = client.search_projects()
        if projects:
            print(f"Found {len(projects)} projects:")
            for proj in projects:
                print(f"  - {proj.name} ({proj.guid})")
                if proj.description:
                    print(f"    Description: {proj.description}")
                if proj.organisation:
                    print(f"    Organisation: {proj.organisation}")
        else:
            print("No projects found.")
    except Exception as e:
        print(f"An error occurred during project search: {e}")

    # Example 2: Search for projects with specific criteria
    print("\n2. Searching for projects with 'Beispiel' in the name...")
    try:
        search_request = AiaProjectForPublicRequest(searchString="Beispiel")
        projects = client.search_projects(search_request)
        if projects:
            print(f"Found {len(projects)} projects matching 'Beispiel':")
            for proj in projects:
                print(f"  - {proj.name} ({proj.guid})")
        else:
            print("No projects found matching 'Beispiel'.")
    except Exception as e:
        print(f"An error occurred during filtered project search: {e}")

def run_detailed_project_examples(client: EnhancedBimPortalClient):
    """Demonstrates detailed project operations."""
    print("\n--- Running Detailed Project Examples ---")

    # Get a project to work with
    try:
        projects = client.search_projects()
        if not projects:
            print("No projects available for detailed examples.")
            return
        
        selected_project = projects[0]
        print(f"\nUsing project '{selected_project.name}' for detailed examples...")

        # Example 1: Get full project details
        print(f"\n1. Fetching detailed information for project {selected_project.guid}...")
        detailed_project = client.get_project(selected_project.guid)
        if detailed_project:
            print(f"Successfully retrieved detailed project information:")
            print(f"  - Name: {detailed_project.name}")
            print(f"  - Description: {detailed_project.description}")
            print(f"  - Version: {detailed_project.versionNumber}")
            print(f"  - Organisation: {detailed_project.organisationName}")
            
            # Show coordinate system if available
            if detailed_project.coordinateSystem:
                coord = detailed_project.coordinateSystem
                print(f"  - Coordinate System: {coord.name}")
                if coord.zone:
                    print(f"    Zone: {coord.zone}")
                if coord.east is not None and coord.north is not None:
                    print(f"    Coordinates: East={coord.east}, North={coord.north}")
            
            # Show models if available
            if detailed_project.models:
                print(f"  - Associated Models ({len(detailed_project.models)}):")
                for model in detailed_project.models:
                    print(f"    * {model.name} ({model.guid})")
            
            # Show chapters if available
            if detailed_project.chapters:
                print(f"  - Chapters ({len(detailed_project.chapters)}):")
                for chapter in detailed_project.chapters[:3]:  # Show first 3
                    print(f"    * {chapter.chapterNumber}: {chapter.title}")
            
            # Show data formats if available
            if detailed_project.dataFormats:
                print(f"  - Data Formats ({len(detailed_project.dataFormats)}):")
                for data_format in detailed_project.dataFormats:
                    if hasattr(data_format, 'name') and data_format.name:
                        print(f"    * {data_format.name}")
        
        else:
            print(f"Could not retrieve detailed information for project {selected_project.guid}")

        # Example 2: Export project as PDF
        print(f"\n2. Exporting project '{selected_project.name}' as PDF...")
        pdf_content = client.export_project_pdf(selected_project.guid)
        if pdf_content:
            filename = Path(BIMPortalConfig.EXPORT_DIRECTORY) /f"detailed_project_{selected_project.guid}.pdf"
            with open(filename, "wb") as f:
                f.write(pdf_content)
            print(f"Successfully exported project to {filename}")
        else:
            print("Failed to export project as PDF")

    except Exception as e:
        print(f"An error occurred during detailed project examples: {e}")

def run_advanced_project_examples(client: EnhancedBimPortalClient):
    """Demonstrates advanced project operations."""
    print("\n--- Running Advanced Project Examples ---")

    try:
        projects = client.search_projects()
        if not projects:
            print("No projects available for advanced examples.")
            return

        print("\n1. Analyzing project metadata...")
        visibility_counts = {}
        organisation_counts = {}
        
        for project in projects:
            # Count by visibility
            visibility = project.visibility.value if project.visibility else "Unknown"
            visibility_counts[visibility] = visibility_counts.get(visibility, 0) + 1
            
            # Count by organisation
            org = project.organisation or "Unknown"
            organisation_counts[org] = organisation_counts.get(org, 0) + 1
        
        print("Project visibility distribution:")
        for visibility, count in visibility_counts.items():
            print(f"  - {visibility}: {count}")
        
        print("Project organisation distribution:")
        for org, count in sorted(organisation_counts.items()):
            print(f"  - {org}: {count}")

        print("\n2. Testing project accessibility...")
        accessible_projects = 0
        detailed_projects = 0
        
        for project in projects:
            try:
                detailed = client.get_project(project.guid)
                accessible_projects += 1
                if detailed and detailed.coordinateSystem:
                    detailed_projects += 1
            except Exception as e:
                print(f"  Could not access {project.name}: {e}")
        
        print(f"Accessibility results:")
        print(f"  - Total projects: {len(projects)}")
        print(f"  - Accessible projects: {accessible_projects}")
        print(f"  - Projects with coordinate systems: {detailed_projects}")

        print("\n3. Finding projects with specific features...")
        projects_with_models = []
        projects_with_chapters = []
        
        for project in projects[:5]:  # Limit to first 5 for performance
            try:
                detailed = client.get_project(project.guid)
                if detailed:
                    if detailed.models:
                        projects_with_models.append(project.name)
                    if detailed.chapters:
                        projects_with_chapters.append(project.name)
            except Exception:
                continue
        
        print(f"Projects with models: {projects_with_models}")
        print(f"Projects with chapters: {projects_with_chapters}")

    except Exception as e:
        print(f"An error occurred during advanced project examples: {e}")

def run_project_comparison_example(client: EnhancedBimPortalClient):
    """Demonstrates comparing multiple projects."""
    print("\n--- Running Project Comparison Example ---")
    
    try:
        projects = client.search_projects()
        if len(projects) < 2:
            print("Need at least 2 projects for comparison.")
            return
        
        print(f"\nComparing first two projects:")
        
        for i, project in enumerate(projects[:2], 1):
            print(f"\nProject {i}: {project.name}")
            detailed = client.get_project(project.guid)
            if detailed:
                print(f"  Version: {detailed.versionNumber}")
                print(f"  Organisation: {detailed.organisationName}")
                print(f"  Has coordinate system: {'Yes' if detailed.coordinateSystem else 'No'}")
                print(f"  Number of models: {len(detailed.models) if detailed.models else 0}")
                print(f"  Number of chapters: {len(detailed.chapters) if detailed.chapters else 0}")
                print(f"  Export formats: {', '.join(detailed.exportFormats) if detailed.exportFormats else 'None'}")
            else:
                print("  Could not retrieve detailed information")
    
    except Exception as e:
        print(f"An error occurred during project comparison: {e}")

def main():
    """Runs all project examples with the enhanced Pydantic client."""
    print("======== Starting BIM Portal API Project Examples ========")
    
    if not os.getenv("BIM_PORTAL_USERNAME"):
        print("Credentials not found. Some examples may fail if private resources are accessed.")

    client = setup_client()
    
    run_basic_project_examples(client)
    run_detailed_project_examples(client)
    run_advanced_project_examples(client)
    run_project_comparison_example(client)
    
    print("\n======== Project Examples Complete ========")

if __name__ == "__main__":
    main()