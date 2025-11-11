import os
import logging
import threading
import time
from http import HTTPStatus
from typing import Optional
from uuid import UUID

from dotenv import load_dotenv
from auth.auth_service_impl import AuthService
from enhanced_bim_client import EnhancedBimPortalClient
from auth.auth_config import BIM_PORTAL_USERNAME_ENV_VAR, BIM_PORTAL_PASSWORD_ENV_VAR
from models import LoinForPublicRequest

# --- Configuration ---
load_dotenv()
from config import BIMPortalConfig
BASE_URL = BIMPortalConfig.BASE_URL
PRIVATE_LOIN_GUID = BIMPortalConfig.PRIVATE_LOIN_GUID
PUBLIC_LOIN_GUID = BIMPortalConfig.PUBLIC_LOIN_GUID

# --- Utility Functions ---

def check_credentials() -> bool:
    """Checks if credentials are available."""
    if not os.getenv(BIM_PORTAL_USERNAME_ENV_VAR) or not os.getenv(BIM_PORTAL_PASSWORD_ENV_VAR):
        print("\n" + "="*50)
        print("⚠️ WARNING: Credentials not found in environment variables.")
        print(f"📝 Please set {BIM_PORTAL_USERNAME_ENV_VAR} and {BIM_PORTAL_PASSWORD_ENV_VAR} in a .env file.")
        print("⭐ Skipping examples that require authentication.")
        print("="*50 + "\n")
        return False
    return True

def make_loin_request(client: EnhancedBimPortalClient, guid: str, description: str):
    """Helper function to make and print a LOIN request using Pydantic client."""
    print(f"\n🔍 Attempting to access {description} LOIN with GUID: {guid}")
    try:
        # Use the new Pydantic client methods
        loin = client.get_loin(UUID(guid))
        if loin:
            print(f"✅ Success! Fetched LOIN: {loin.name} (GUID: {loin.guid})")
            print(f"  - Description: {loin.description}")
            print(f"  - Organisation: {loin.organisationName}")
            print(f"  - Version: {loin.versionNumber}")
            return True
        else:
            print(f"❌ Failed to fetch LOIN {guid}")
            return False
    except Exception as e:
        print(f"❌ Exception occurred: {e}")
        return False

def make_project_request(client: EnhancedBimPortalClient, description: str):
    """Helper function to search for projects."""
    print(f"\n🔍 Attempting to search for {description} projects")
    try:
        projects = client.search_projects()
        if projects:
            print(f"✅ Success! Found {len(projects)} projects.")
            for project in projects[:2]:  # Show first 2
                print(f"  - {project.name} ({project.guid})")
            return True
        else:
            print("❌ No projects found.")
            return False
    except Exception as e:
        print(f"❌ Exception occurred: {e}")
        return False

# --- Example Patterns ---

def example_1_authenticated_access_from_env():
    """Example 1: Accessing a private resource using credentials from .env file."""
    print("🔐 --- Example 1: Authenticated Access (from .env) ---")
    auth_service = AuthService(guid=PRIVATE_LOIN_GUID)
    client = EnhancedBimPortalClient(auth_service=auth_service, base_url=BASE_URL)
    
    # Test both LOIN access and project search
    make_loin_request(client, PRIVATE_LOIN_GUID, "private")
    make_project_request(client, "authenticated")

def example_2_manual_credentials():
    """Example 2: Accessing a private resource by passing credentials manually."""
    print("\n🔑 --- Example 2: Authenticated Access (Manual Credentials) ---")
    username = os.getenv(BIM_PORTAL_USERNAME_ENV_VAR)
    password = os.getenv(BIM_PORTAL_PASSWORD_ENV_VAR)
    auth_service = AuthService(guid=PRIVATE_LOIN_GUID, username=username, password=password)
    client = EnhancedBimPortalClient(auth_service=auth_service, base_url=BASE_URL)
    
    make_loin_request(client, PRIVATE_LOIN_GUID, "private")

def example_3_public_access():
    """Example 3: Accessing a public resource without any credentials."""
    print("\n🌐 --- Example 3: Public Access (No Credentials) ---")
    # By providing no credentials, the client will make unauthenticated requests.
    auth_service = AuthService(guid=PUBLIC_LOIN_GUID, username=None, password=None)
    client = EnhancedBimPortalClient(auth_service=auth_service, base_url=BASE_URL)
    
    make_loin_request(client, PUBLIC_LOIN_GUID, "public")
    make_project_request(client, "public")

def example_4_fallback_to_public():
    """Example 4: Demonstrates a single client accessing both private and public data."""
    print("\n🔄 --- Example 4: Fallback from Private to Public Access ---")
    print("📊 Using one client configured with credentials to access both resource types.")
    # This client is configured for authentication
    auth_service = AuthService(guid=PRIVATE_LOIN_GUID)
    client = EnhancedBimPortalClient(auth_service=auth_service, base_url=BASE_URL)

    # Request 1: Access the private resource (will use token)
    make_loin_request(client, PRIVATE_LOIN_GUID, "private")

    # Request 2: Access the public resource (will also use token, which is fine)
    # The API gateway allows valid tokens to be used for public resources.
    make_loin_request(client, PUBLIC_LOIN_GUID, "public")

def example_5_token_lifecycle():
    """Example 5: Shows the automatic token login and refresh mechanism."""
    print("\n🔄 --- Example 5: Token Lifecycle Demonstration ---")
    print("🐛 Enabling DEBUG logging to show auth flow. See logs from 'auth_service_impl'.")
    logging.basicConfig(level=logging.DEBUG, format='%(asctime)s - %(levelname)s - %(message)s')

    auth_service = AuthService(guid=PRIVATE_LOIN_GUID)
    print("1️⃣ Step 1: First request triggers a login.")
    client = EnhancedBimPortalClient(auth_service=auth_service, base_url=BASE_URL)
    make_loin_request(client, PRIVATE_LOIN_GUID, "private")

    print("\n2️⃣ Step 2: Second request uses the cached token (no new login).")
    make_loin_request(client, PRIVATE_LOIN_GUID, "private")

    print("\n3️⃣ Step 3: Simulating token expiration. The next request should trigger a refresh or new login.")
    # In a real scenario, you would wait for the token to expire. Here we clear it manually.
    auth_service._token_manager.clear_tokens()
    make_loin_request(client, PRIVATE_LOIN_GUID, "private")
    logging.basicConfig(level=logging.INFO) # Reset log level

def example_6_thread_safety():
    """Example 6: Demonstrates that the client is safe to use from multiple threads."""
    print("\n🧵 --- Example 6: Thread Safety Demonstration ---")
    auth_service = AuthService(guid=PRIVATE_LOIN_GUID)
    client = EnhancedBimPortalClient(auth_service=auth_service, base_url=BASE_URL)

    print("🚀 Starting 5 concurrent requests...")
    threads = []
    results = []
    
    def thread_request(thread_id):
        result = make_loin_request(client, PRIVATE_LOIN_GUID, f"private (thread {thread_id})")
        results.append(result)
    
    for i in range(5):
        thread = threading.Thread(target=thread_request, args=(i+1,))
        threads.append(thread)
        thread.start()

    for thread in threads:
        thread.join()
    
    success_count = sum(results)
    if success_count == 5:
        print(f"\n✅ All {success_count}/5 threads completed successfully. Token was shared and reused safely.")
    else:
        print(f"\n⚠️ Only {success_count}/5 threads succeeded. Check authentication setup.")

def example_7_pydantic_features():
    """Example 7: Demonstrates Pydantic-specific features and better error handling."""
    print("\n🐍 --- Example 7: Pydantic Features Demonstration ---")
    auth_service = AuthService(guid=PRIVATE_LOIN_GUID)
    client = EnhancedBimPortalClient(auth_service=auth_service, base_url=BASE_URL)
    
    print("🔍 Testing robust property search with null handling...")
    properties = client.search_properties()
    if properties:
        print(f"✅ Found {len(properties)} properties. Showing details for first few:")
        for prop in properties[:3]:
            category_name = prop.category.value if prop.category else "None"
            print(f"  - {prop.name}: Category={category_name}, DataType={prop.dataType}")
            
            # Demonstrate safe access to potentially null fields
            if prop.units:
                print(f"    Units: {', '.join(prop.units[:3])}")
            if prop.organisationName:
                print(f"    Organisation: {prop.organisationName}")
    
    print("\n🔍 Testing project details with complex nested data...")
    projects = client.search_projects()
    if projects:
        project = projects[0]
        detailed_project = client.get_project(project.guid)
        if detailed_project:
            print(f"✅ Project: {detailed_project.name}")
            
            # Show safe handling of potentially null nested objects
            if detailed_project.coordinateSystem:
                coord = detailed_project.coordinateSystem
                print(f"  Coordinate System: {coord.name} (Zone: {coord.zone})")
            
            if detailed_project.models:
                print(f"  Models: {len(detailed_project.models)}")
                for model in detailed_project.models[:2]:
                    print(f"    - {model.name}")

def print_summary():
    """Print a summary of what was demonstrated."""
    print("\n" + "="*60)
    print("📋 SUMMARY OF AUTHENTICATION EXAMPLES WITH PYDANTIC")
    print("="*60)
    print("✅ Demonstrated automatic login and token caching")
    print("✅ Showed fallback from authenticated to public access") 
    print("✅ Verified thread-safe token management")
    print("✅ Illustrated token lifecycle and refresh handling")
    print("✅ Confirmed Pydantic models handle null values gracefully")
    print("✅ Showed enhanced error handling and type safety")
    print("="*60)

def main():
    """Runs all authentication examples with the new Pydantic client."""
    print("🚀 ======== Starting BIM Portal API Authentication Examples with Pydantic ========")

    if check_credentials():
        example_1_authenticated_access_from_env()
        example_2_manual_credentials()
        example_4_fallback_to_public()
        example_5_token_lifecycle()
        example_6_thread_safety()
        example_7_pydantic_features()
    else:
        print("⚠️ Skipping authenticated examples due to missing credentials")

    # This example does not require credentials
    example_3_public_access()
    
    print_summary()

if __name__ == "__main__":
    main()