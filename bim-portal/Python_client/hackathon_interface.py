"""
BIM Portal API - Hackathon Interface

This is a simplified interface for hackathon participants to easily interact
with the German BIM Portal API. It provides clean, easy-to-use methods for
common operations without requiring deep knowledge of the underlying API.

Quick Start:
    from hackathon_interface import BIMPortal
    
    # Initialize
    bim = BIMPortal()
    
    # Search for projects
    projects = bim.get_projects(search="Beispiel")
    
    # Get project details
    project = bim.get_project_details(projects[0].guid)
    
    # Export project as PDF
    bim.export_project_pdf(projects[0].guid, "my_project.pdf")
"""

import os
from pathlib import Path
from typing import List, Optional, Dict, Any
from uuid import UUID

from auth.auth_service_impl import AuthService
from enhanced_bim_client import EnhancedBimPortalClient
from config import BIMPortalConfig 
from models import (
    SimpleAiaProjectPublicDto,
    AIAProjectPublicDto, 
    PropertyOrGroupForPublicDto,
    SimpleLoinPublicDto,
    LOINPublicDto,
    AiaProjectForPublicRequest,
    PropertyOrGroupForPublicRequest,
    LoinForPublicRequest
)


class BIMPortal:
    """
    Simplified interface to the German BIM Portal API for hackathon use.
    
    This class wraps the enhanced client to provide easy-to-use methods
    for common BIM Portal operations.
    """
    
    def __init__(self, credentials_file: str = ".env", base_url: Optional[str] = None):
        """
        Initialize the BIM Portal interface.
        
        Args:
            credentials_file: Path to .env file with BIM_PORTAL_USERNAME and BIM_PORTAL_PASSWORD
            base_url: Optional custom base URL (defaults to centralized config)
        """
        # Load credentials from .env file
        if not os.path.exists(credentials_file):
            raise FileNotFoundError(
                f"Credentials file '{credentials_file}' not found. "
                f"Please run: python demo/credentials_setup.py"
            )
        
        # Use custom base URL if provided, otherwise use centralized config
        if base_url:
            self.base_url = base_url
        else:
            self.base_url = BIMPortalConfig.BASE_URL
        
        # Use default auth GUID from centralized config
        self.auth_guid = BIMPortalConfig.DEFAULT_AUTH_GUID
        
        # Setup the enhanced client
        auth_service = AuthService(guid=self.auth_guid)
        self.client = EnhancedBimPortalClient(
            auth_service=auth_service, 
            base_url=self.base_url
        )
        
        # Create exports directory using centralized config
        self.exports_dir = Path(BIMPortalConfig.EXPORT_DIRECTORY)
        self.exports_dir.mkdir(exist_ok=True)
    
    # === PROJECT METHODS ===
    
    def get_projects(self, search: Optional[str] = None, limit: int = 50) -> List[SimpleAiaProjectPublicDto]:
        """
        Get a list of projects from the BIM Portal.
        
        Args:
            search: Optional search term to filter projects
            limit: Maximum number of projects to return
            
        Returns:
            List of project objects with basic information
        """
        try:
            if search:
                request = AiaProjectForPublicRequest(searchString=search)
                projects = self.client.search_projects(request)
            else:
                projects = self.client.search_projects()
            
            return projects[:limit] if projects else []
        except Exception as e:
            print(f"Error getting projects: {e}")
            return []
    
    def get_project_details(self, project_guid: UUID) -> Optional[AIAProjectPublicDto]:
        """
        Get detailed information about a specific project.
        
        Args:
            project_guid: The GUID of the project
            
        Returns:
            Detailed project object or None if not found
        """
        try:
            return self.client.get_project(project_guid)
        except Exception as e:
            print(f"Error getting project details: {e}")
            return None
    
    def export_project_pdf(self, project_guid: UUID, filename: Optional[str] = None) -> Optional[str]:
        """
        Export a project as PDF.
        
        Args:
            project_guid: The GUID of the project to export
            filename: Optional filename (defaults to project_<guid>.pdf)
            
        Returns:
            Path to the exported file or None if export failed
        """
        try:
            pdf_content = self.client.export_project_pdf(project_guid)
            if pdf_content:
                if not filename:
                    filename = f"project_{project_guid}.pdf"
                
                filepath = self.exports_dir / filename
                with open(filepath, "wb") as f:
                    f.write(pdf_content)
                
                return str(filepath)
            return None
        except Exception as e:
            print(f"Error exporting project: {e}")
            return None
    
    # === PROPERTY METHODS ===
    
    def get_properties(self, search: str = "a", limit: int = 100) -> List[PropertyOrGroupForPublicDto]:
        """
        Get a list of properties from the BIM Portal.
        
        Args:
            search: Search term to filter properties
            limit: Maximum number of properties to return
            
        Returns:
            List of property objects
        """
        try:
            request = PropertyOrGroupForPublicRequest(searchString=search)
            properties = self.client.search_properties(request)
            return properties[:limit] if properties else []
        except Exception as e:
            print(f"Error getting properties: {e}")
            return []
    
    def find_properties_by_category(self, category: str, limit: int = 50) -> List[PropertyOrGroupForPublicDto]:
        """
        Find properties by category.
        
        Args:
            category: Category to search for (e.g., "IFC", "DIN", "length")
            limit: Maximum number of properties to return
            
        Returns:
            List of matching properties
        """
        all_properties = self.get_properties(limit=1000)  # Get more for filtering
        
        matching = []
        for prop in all_properties:
            if (category.lower() in prop.name.lower() or 
                (prop.dataType and category.lower() in prop.dataType.lower()) or
                (prop.category and category.upper() in prop.category.value)):
                matching.append(prop)
                if len(matching) >= limit:
                    break
        
        return matching
    
    # === LOIN METHODS ===
    
    def get_loins(self, search: Optional[str] = None, limit: int = 50) -> List[SimpleLoinPublicDto]:
        """
        Get a list of LOINs (Level of Information Need) from the BIM Portal.
        
        Args:
            search: Optional search term to filter LOINs
            limit: Maximum number of LOINs to return
            
        Returns:
            List of LOIN objects
        """
        try:
            if search:
                request = LoinForPublicRequest(searchString=search)
                loins = self.client.search_loins(request)
            else:
                loins = self.client.search_loins()
            
            return loins[:limit] if loins else []
        except Exception as e:
            print(f"Error getting LOINs: {e}")
            return []
    
    def get_loin_details(self, loin_guid: UUID) -> Optional[LOINPublicDto]:
        """
        Get detailed information about a specific LOIN.
        
        Args:
            loin_guid: The GUID of the LOIN
            
        Returns:
            Detailed LOIN object or None if not found
        """
        try:
            return self.client.get_loin(loin_guid)
        except Exception as e:
            print(f"Error getting LOIN details: {e}")
            return None
    
    # === UTILITY METHODS ===
    
    def get_stats(self) -> Dict[str, Any]:
        """
        Get basic statistics about available data.
        
        Returns:
            Dictionary with counts of projects, properties, and LOINs
        """
        try:
            projects = self.get_projects(limit=1000)
            properties = self.get_properties(limit=1000) 
            loins = self.get_loins(limit=1000)
            
            return {
                "projects": len(projects),
                "properties": len(properties), 
                "loins": len(loins),
                "exports_directory": str(self.exports_dir)
            }
        except Exception as e:
            print(f"Error getting stats: {e}")
            return {}
    
    def search_everything(self, term: str, limit: int = 20) -> Dict[str, List]:
        """
        Search across projects, properties, and LOINs.
        
        Args:
            term: Search term
            limit: Maximum results per category
            
        Returns:
            Dictionary with results from each category
        """
        results = {
            "projects": self.get_projects(search=term, limit=limit),
            "properties": [p for p in self.get_properties(limit=limit*2) 
                          if term.lower() in p.name.lower()][:limit],
            "loins": self.get_loins(search=term, limit=limit)
        }
        
        return results
    
    def health_check(self) -> tuple[bool, bool, str]:
        """
        Perform a comprehensive health check of the API connection.
        
        Returns:
            Tuple of (api_accessible, auth_working, status_message)
        """
        try:
            # Test basic API connectivity
            projects = self.get_projects(limit=1)
            api_accessible = True
            
            # Test authentication status by checking if we have a valid token
            auth_working = False
            status_message = ""
            
            try:
                token = self.client.auth_service.get_valid_token()
                if token:
                    auth_working = True
                    status_message = "Authentication successful"
                else:
                    status_message = "Authentication failed - using public access only"
            except Exception:
                status_message = "Authentication failed - using public access only"
            
            return api_accessible, auth_working, status_message
            
        except Exception as e:
            return False, False, f"API connection failed: {e}"


# === CONVENIENCE FUNCTIONS FOR HACKATHON PARTICIPANTS ===

def quick_start():
    """
    Quick start function that sets up the BIM Portal interface and returns basic info.
    """
    print("Setting up BIM Portal connection...")
    
    try:
        bim = BIMPortal()
        
        api_accessible, auth_working, status_message = bim.health_check()
        
        if not api_accessible:
            print(f"API connection failed. {status_message}")
            return None
        
        stats = bim.get_stats()
        
        if auth_working:
            print("============================================================")
            print("✅ 🎉 CONNECTED TO BIM PORTAL WITH FULL AUTHENTICATION! 🎉")
            print("============================================================")
            print(f"   - 📊 Available projects: {stats.get('projects', 'Unknown')}")
            print(f"   - 🏷️ Available properties: {stats.get('properties', 'Unknown')}")
            print(f"   - 📋 Available LOINs: {stats.get('loins', 'Unknown')}")
            print(f"   - 💾 Exports will be saved to: {stats.get('exports_directory', 'Unknown')}")
            print("\nReady for hackathon! Use the 'bim' object to interact with the API.")
        else:
            print("============================================================")
            print("⚠️  CONNECTED TO BIM PORTAL WITH LIMITED PUBLIC ACCESS")
            print("============================================================")
            print(f"   Status: {status_message}")
            print(f"   - 📊 Available projects: {stats.get('projects', 'Unknown')} (public only)")
            print(f"   - 🏷️ Available properties: {stats.get('properties', 'Unknown')} (public only)")
            print(f"   - 📋 Available LOINs: {stats.get('loins', 'Unknown')} (public only)")
            print(f"   - 💾 Exports will be saved to: {stats.get('exports_directory', 'Unknown')}")
            print("\n❗ **AUTHENTICATION ISSUE DETECTED:**")
            print("   - 🔐 Check that the email address is verified in the BIM Portal")
            print("   - 🛠️ Verify credentials by running: python demo/credentials_setup.py")
            print("   - 🚫 Some features may not be available without authentication")
            print("\n ⚡Proceeding with public access only...")
        
        return bim
    
    except FileNotFoundError as e:
        print(f"{e}")
        print("Run: python demo/credentials_setup.py")
        return None
    except Exception as e:
        print(f"Error setting up BIM Portal: {e}")
        return None


# Example usage
if __name__ == "__main__":
    # Quick demo
    bim = quick_start()
    
    if bim:
        api_accessible, auth_working, status_message = bim.health_check()
        
        organizations= bim.get_organisations()
        if organizations:
            for organization in organizations:
                print(f"  📁 {organization.name} ({organization.guid})")

        print("\n🔍 --- Demo: Finding projects ---")
        projects = bim.get_projects(limit=3)
        if projects:
            for project in projects:
                print(f"  📁 {project.name} ({project.guid})")
        else:
            print("  📭 No projects found in demo")
        
        print("\n🏗️  --- Demo: Finding IFC properties ---")
        ifc_props = bim.find_properties_by_category("IFC", limit=3)
        if ifc_props:
            for prop in ifc_props:
                print(f"  🏷️  {prop.name} ({prop.dataType})")
        else:
            print("  📭 No IFC properties found in demo")
        
        # Only show "Ready for hackathon" if authentication is working
        if auth_working:
            print("\n🎯 **READY FOR HACKATHON!** Use the 'bim' object to interact with the API.")
            print("============================================================")
        else:
            print("\n⚡ **Setup complete with public access only.**")
            print("🔧 For full functionality, resolve authentication issues first.")
            print("============================================================")