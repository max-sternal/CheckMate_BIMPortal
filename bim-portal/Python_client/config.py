"""
Centralized configuration for the BIM Portal API client.

This module provides a single source of truth for all configuration values,
including the base URL, authentication settings, and other application parameters.
"""

import os
from pathlib import Path
from typing import Optional
from dotenv import load_dotenv

# --- Load Environment Variables ---
# Load from project root directory
_project_root = Path(__file__).parent
_env_path = _project_root / ".env"
load_dotenv(dotenv_path=_env_path)

class BIMPortalConfig:
    """
    Centralized configuration for BIM Portal API client.
    
    This class manages all configuration settings including URLs, authentication,
    and application-specific parameters.
    """
    
    # --- API Configuration ---
    BASE_URL: str = "https://via.bund.de/bim" # PROD
    #BASE_URL: str = "https://via.bund.de/bmdv/bim-portal/edu/bim" #EDU -> for the Hackaton

    
    # Authentication endpoints (relative to BASE_URL)
    LOGIN_ENDPOINT: str = "/infrastruktur/api/v1/public/auth/login"
    REFRESH_ENDPOINT: str = "/infrastruktur/api/v1/public/auth/refresh-token"
    
    # --- Default GUIDs ---
    DEFAULT_AUTH_GUID: str = "4559818c-faea-4bb7-bbdd-e6470df8261b"
    PUBLIC_RESOURCE_GUID: str = "80730a51-a953-4a80-9eaa-debfab31f6e9"
    PRIVATE_RESOURCE_GUID: str = "4559818c-faea-4bb7-bbdd-e6470df8261b" # A known private resource to test authentication against

    # --- Authentication Configuration ---
    USERNAME_ENV_VAR: str = "BIM_PORTAL_USERNAME"
    PASSWORD_ENV_VAR: str = "BIM_PORTAL_PASSWORD"
    TOKEN_REFRESH_MARGIN_MINUTES: int = 5
    AUTH_RETRY_LIMIT: int = 1
    
    # --- HTTP Client Configuration ---
    REQUEST_TIMEOUT: int = int(os.getenv("REQUEST_TIMEOUT", "30"))
    VERIFY_SSL: bool = os.getenv("VERIFY_SSL", "true").lower() == "true"
    
    # --- Application Configuration ---
    LOG_LEVEL: str = os.getenv("LOG_LEVEL", "INFO").upper()
    EXPORT_DIRECTORY: str = os.getenv("EXPORT_DIRECTORY", "exports")
    MAX_RETRIES: int = int(os.getenv("MAX_RETRIES", "3"))
    
    @classmethod
    def get_full_url(cls, endpoint: str) -> str:
        """
        Combine base URL with endpoint to create full URL.
        
        Args:
            endpoint: API endpoint (with or without leading slash)
            
        Returns:
            Complete URL
        """
        if not endpoint.startswith('/'):
            endpoint = '/' + endpoint
        return cls.BASE_URL + endpoint
    
    @classmethod
    def get_login_url(cls) -> str:
        """Get the complete login URL."""
        return cls.BASE_URL + cls.LOGIN_ENDPOINT
    
    @classmethod
    def get_refresh_url(cls) -> str:
        """Get the complete refresh token URL."""
        return cls.BASE_URL + cls.REFRESH_ENDPOINT
    
    @classmethod
    def get_credentials(cls) -> tuple[Optional[str], Optional[str]]:
        """
        Get username and password from environment variables.
        
        Returns:
            Tuple of (username, password) or (None, None) if not found
        """
        username = os.getenv(cls.USERNAME_ENV_VAR)
        password = os.getenv(cls.PASSWORD_ENV_VAR)
        return username, password
    
    @classmethod
    def has_credentials(cls) -> bool:
        """Check if credentials are available."""
        username, password = cls.get_credentials()
        return bool(username and password)
    
    @classmethod
    def get_export_path(cls, filename: str) -> Path:
        """
        Get the full path for an export file.
        
        Args:
            filename: Name of the file to export
            
        Returns:
            Path object for the export file
        """
        export_dir = Path(cls.EXPORT_DIRECTORY)
        export_dir.mkdir(exist_ok=True)
        return export_dir / filename
    
    @classmethod
    def validate_config(cls) -> list[str]:
        """
        Validate the configuration and return any issues found.
        
        Returns:
            List of configuration issues (empty if valid)
        """
        issues = []
        
        if not cls.BASE_URL:
            issues.append("BASE_URL is not set")
        elif not cls.BASE_URL.startswith(('http://', 'https://')):
            issues.append("BASE_URL must start with http:// or https://")
        
        if cls.REQUEST_TIMEOUT < 1:
            issues.append("REQUEST_TIMEOUT must be at least 1 second")
        
        if cls.MAX_RETRIES < 0 or cls.MAX_RETRIES > 10:
            issues.append("MAX_RETRIES should be between 0 and 10")
        
        if cls.LOG_LEVEL not in ["DEBUG", "INFO", "WARNING", "ERROR", "CRITICAL"]:
            issues.append(f"Invalid LOG_LEVEL: {cls.LOG_LEVEL}")
        
        return issues
    
    @classmethod
    def display_config(cls, show_credentials: bool = False) -> None:
        """
        Display current configuration.
        
        Args:
            show_credentials: Whether to show credential status
        """
        print("--- BIM Portal Configuration ---")
        print(f"Base URL:          {cls.BASE_URL}")
        print(f"Login URL:         {cls.get_login_url()}")
        print(f"Default Auth GUID: {cls.DEFAULT_AUTH_GUID}")
        print(f"Log Level:         {cls.LOG_LEVEL}")
        print(f"Export Directory:  {cls.EXPORT_DIRECTORY}")
        print(f"Request Timeout:   {cls.REQUEST_TIMEOUT}s")
        print(f"Max Retries:       {cls.MAX_RETRIES}")
        print(f"SSL Verification:  {cls.VERIFY_SSL}")
        
        if show_credentials:
            if cls.has_credentials():
                username, _ = cls.get_credentials()
                print(f"Username:          {username}")
                print("Password:          [configured]")
            else:
                print("Credentials:       Not configured")
        
        # Check for configuration issues
        issues = cls.validate_config()
        if issues:
            print("\nConfiguration Issues:")
            for issue in issues:
                print(f"  - {issue}")
        else:
            print("\nConfiguration: Valid")
        print("-" * 32)

# --- Convenience Functions ---

def get_base_url() -> str:
    """Get the base URL for the BIM Portal API."""
    return BIMPortalConfig.BASE_URL

def set_base_url(url: str) -> None:
    """
    Set a custom base URL for the BIM Portal API.
    
    Args:
        url: New base URL (e.g., "https://test.via.bund.de/bim")
    """
    BIMPortalConfig.BASE_URL = url

def get_config() -> BIMPortalConfig:
    """Get the configuration class."""
    return BIMPortalConfig

# --- Module-level exports ---
# These can be imported directly for convenience
BASE_URL = BIMPortalConfig.BASE_URL
LOGIN_URL = BIMPortalConfig.get_login_url()
REFRESH_URL = BIMPortalConfig.get_refresh_url()
DEFAULT_AUTH_GUID = BIMPortalConfig.DEFAULT_AUTH_GUID

# For backward compatibility with existing imports
USERNAME_ENV_VAR = BIMPortalConfig.USERNAME_ENV_VAR
PASSWORD_ENV_VAR = BIMPortalConfig.PASSWORD_ENV_VAR
AUTH_RETRY_LIMIT = BIMPortalConfig.AUTH_RETRY_LIMIT

if __name__ == "__main__":
    # Display configuration when run directly
    BIMPortalConfig.display_config(show_credentials=True)