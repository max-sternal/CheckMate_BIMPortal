import os
from pathlib import Path
from dotenv import load_dotenv
from config import BIMPortalConfig

# --- Application Configuration ---
# This file demonstrates a best practice for managing configuration in a Python application.
# Instead of loading environment variables in every script, you can centralize it here.
#
# Usage in your application:
# from demo.config_examples.config import settings
#
# print(settings.base_url)
# print(settings.username)

class Settings:
    """
    A class to hold all application settings, loaded from environment variables.
    Updated to work with the enhanced Pydantic client.
    """
    def __init__(self):
        # Load the .env file from the project root
        # Use pathlib for more robust path handling
        env_path = Path(__file__).parent.parent.parent / ".env"
        load_dotenv(dotenv_path=env_path)

        # API Configuration
        self.base_url: str = BIMPortalConfig.BASE_URL
        
        # Default GUID for authentication context
        self.default_auth_guid: str = BIMPortalConfig.DEFAULT_AUTH_GUID

        # Credentials
        self.username: str | None = os.getenv("BIM_PORTAL_USERNAME")
        self.password: str | None = os.getenv("BIM_PORTAL_PASSWORD")

        # Application-specific settings
        self.log_level: str = BIMPortalConfig.LOG_LEVEL
        
        # Export settings
        self.export_directory: str = BIMPortalConfig.EXPORT_DIRECTORY
        
        # API Client settings
        self.request_timeout: int = BIMPortalConfig.REQUEST_TIMEOUT
        self.max_retries: int = BIMPortalConfig.MAX_RETRIES

    def check_credentials(self) -> bool:
        """Returns True if both username and password are set."""
        return bool(self.username and self.password)
    
    def get_export_path(self, filename: str) -> Path:
        """Returns the full path for an export file."""
        export_dir = Path(self.export_directory)
        export_dir.mkdir(exist_ok=True)
        return export_dir / filename
    
    def validate_configuration(self) -> list[str]:
        """
        Validates the configuration and returns a list of issues found.
        Returns empty list if configuration is valid.
        """
        issues = []
        
        if not self.check_credentials():
            issues.append("Missing BIM_PORTAL_USERNAME or BIM_PORTAL_PASSWORD")
        
        if self.request_timeout < 5:
            issues.append("REQUEST_TIMEOUT should be at least 5 seconds")
        
        if self.max_retries < 1 or self.max_retries > 10:
            issues.append("MAX_RETRIES should be between 1 and 10")
        
        if self.log_level not in ["DEBUG", "INFO", "WARNING", "ERROR"]:
            issues.append(f"Invalid LOG_LEVEL: {self.log_level}")
        
        return issues
    
    def display_config(self):
        """Display the current configuration (safely, without exposing passwords)."""
        print("--- Current Configuration ---")
        print(f"API Base URL:      {self.base_url}")
        print(f"Default Auth GUID: {self.default_auth_guid}")
        print(f"Log Level:         {self.log_level}")
        print(f"Export Directory:  {self.export_directory}")
        print(f"Request Timeout:   {self.request_timeout}s")
        print(f"Max Retries:       {self.max_retries}")
        
        if self.check_credentials():
            print(f"Username:          {self.username}")
            print("Password:          [configured]")
        else:
            print("Credentials:       Not configured")
        
        # Check for issues
        issues = self.validate_configuration()
        if issues:
            print("\nConfiguration Issues:")
            for issue in issues:
                print(f"  - {issue}")
        else:
            print("\nConfiguration: Valid")

# Create a single, importable instance of the settings
settings = Settings()

# You can add a simple check to run when the file is imported (optional)
if __name__ == "__main__":
    settings.display_config()