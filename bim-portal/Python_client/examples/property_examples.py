"""
Property examples demonstrating various property and property group operations.
Uses the enhanced Pydantic client with error handling.
"""

import os
from uuid import UUID
from dotenv import load_dotenv

from auth.auth_service_impl import AuthService
from enhanced_bim_client import EnhancedBimPortalClient
from models import PropertyOrGroupForPublicRequest

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

def run_basic_property_examples(client: EnhancedBimPortalClient):
    """Demonstrates basic property search operations."""
    print("--- Running Basic Property Examples ---")

    # Example 1: Search for all properties (limited)
    print("\n1. Searching for properties with 'a' in the name (first 10 results)...")
    try:
        search_request = PropertyOrGroupForPublicRequest(searchString="a")
        properties = client.search_properties(search_request)
        if properties:
            print(f"Found {len(properties)} properties:")
            for prop in properties[:10]:  # Show first 10
                category_name = prop.category.value if prop.category else "None"
                print(f"  - {prop.name}: Category={category_name}, DataType={prop.dataType}")
                if prop.units:
                    print(f"    Units: {', '.join(prop.units[:3])}")  # First 3 units
        else:
            print("No properties found.")
    except Exception as e:
        print(f"An error occurred during property search: {e}")

    # Example 2: Search for properties with specific terms
    print("\n2. Searching for properties containing 'IFC'...")
    try:
        search_request = PropertyOrGroupForPublicRequest(searchString="IFC")
        properties = client.search_properties(search_request)
        if properties:
            print(f"Found {len(properties)} properties containing 'IFC':")
            for prop in properties[:5]:  # Show first 5
                print(f"  - {prop.name}")
                if prop.definition:
                    print(f"    Definition: {prop.definition[:100]}...")  # First 100 chars
        else:
            print("No properties found containing 'IFC'.")
    except Exception as e:
        print(f"An error occurred during IFC property search: {e}")

def run_detailed_property_examples(client: EnhancedBimPortalClient):
    """Demonstrates detailed property operations."""
    print("\n--- Running Detailed Property Examples ---")

    try:
        # Get some properties to work with
        properties = client.search_properties()
        if not properties:
            print("No properties available for detailed examples.")
            return

        selected_property = properties[0]
        print(f"\nUsing property '{selected_property.name}' for detailed examples...")

        # Example 1: Get detailed property information
        print(f"\n1. Fetching detailed information for property {selected_property.guid}...")
        if selected_property.guid:
            detailed_property = client.get_property(selected_property.guid)
            if detailed_property:
                print("Successfully retrieved detailed property information:")
                
                # Show multilingual names if available
                if detailed_property.namesInLanguage:
                    print(f"  - Names ({len(detailed_property.namesInLanguage)} languages):")
                    for name_info in detailed_property.namesInLanguage[:3]:  # First 3
                        print(f"    * {name_info.name} ({name_info.language})")
                
                # Show definitions if available
                if detailed_property.definitionsInLanguage:
                    print(f"  - Definitions ({len(detailed_property.definitionsInLanguage)} languages):")
                    for def_info in detailed_property.definitionsInLanguage[:2]:  # First 2
                        if def_info.definition:
                            print(f"    * {def_info.definition[:150]}... ({def_info.language})")
                
                # Show technical details
                print(f"  - Data Type: {detailed_property.dataType}")
                print(f"  - Organisation: {detailed_property.organisationName}")
                
                if detailed_property.units:
                    print(f"  - Units: {', '.join(detailed_property.units)}")
                
                if detailed_property.dimension:
                    print(f"  - Dimension: {detailed_property.dimension}")
                
                if detailed_property.physicalQuantity:
                    print(f"  - Physical Quantities: {len(detailed_property.physicalQuantity)}")
            
            else:
                print(f"Could not retrieve detailed information for property {selected_property.guid}")
        else:
            print("Selected property has no GUID for detailed lookup")

    except Exception as e:
        print(f"An error occurred during detailed property examples: {e}")

def run_property_analysis_examples(client: EnhancedBimPortalClient):
    """Demonstrates property analysis and categorization."""
    print("\n--- Running Property Analysis Examples ---")

    try:
        properties = client.search_properties()
        if not properties:
            print("No properties available for analysis.")
            return

        print(f"\nAnalyzing {len(properties)} properties...")

        # Analyze by category
        category_counts = {}
        data_type_counts = {}
        org_counts = {}
        
        for prop in properties:
            # Count by category
            category = prop.category.value if prop.category else "None"
            category_counts[category] = category_counts.get(category, 0) + 1
            
            # Count by data type
            data_type = prop.dataType or "Unknown"
            data_type_counts[data_type] = data_type_counts.get(data_type, 0) + 1
            
            # Count by organisation
            org = prop.organisationName or "Unknown"
            org_counts[org] = org_counts.get(org, 0) + 1

        print("\n1. Property category distribution:")
        for category, count in sorted(category_counts.items(), key=lambda x: x[1], reverse=True):
            print(f"  - {category}: {count}")

        print("\n2. Data type distribution (top 10):")
        sorted_data_types = sorted(data_type_counts.items(), key=lambda x: x[1], reverse=True)
        for data_type, count in sorted_data_types[:10]:
            print(f"  - {data_type}: {count}")

        print("\n3. Organisation distribution (top 5):")
        sorted_orgs = sorted(org_counts.items(), key=lambda x: x[1], reverse=True)
        for org, count in sorted_orgs[:5]:
            print(f"  - {org}: {count}")

        # Find properties with specific characteristics
        print("\n4. Properties with special characteristics:")
        
        properties_with_units = [p for p in properties if p.units]
        print(f"  - Properties with units: {len(properties_with_units)}")
        
        deprecated_properties = [p for p in properties if p.deprecated]
        print(f"  - Deprecated properties: {len(deprecated_properties)}")
        
        properties_with_parents = [p for p in properties if p.parentGuids]
        print(f"  - Properties with parent relationships: {len(properties_with_parents)}")

    except Exception as e:
        print(f"An error occurred during property analysis: {e}")

def run_property_search_examples(client: EnhancedBimPortalClient):
    """Demonstrates advanced property search capabilities."""
    print("\n--- Running Advanced Property Search Examples ---")

    # Example 1: Search by specific criteria
    print("\n1. Searching for measurement properties...")
    try:
        measurement_terms = ["length", "width", "height", "dimension", "measure"]
        measurement_properties = []
        
        for term in measurement_terms[:2]:  # Limit to prevent too many requests
            search_request = PropertyOrGroupForPublicRequest(searchString=term)
            properties = client.search_properties(search_request)
            if properties:
                measurement_properties.extend(properties[:5])  # Add first 5 from each search
        
        if measurement_properties:
            print(f"Found measurement-related properties:")
            unique_properties = {p.guid: p for p in measurement_properties if p.guid}.values()
            for prop in list(unique_properties)[:10]:  # Show up to 10 unique properties
                print(f"  - {prop.name} ({prop.dataType})")
                if prop.units:
                    print(f"    Units: {', '.join(prop.units[:3])}")
        else:
            print("No measurement properties found.")
            
    except Exception as e:
        print(f"An error occurred during measurement property search: {e}")

    # Example 2: Search for properties from specific organisations
    print("\n2. Finding properties by organisation pattern...")
    try:
        properties = client.search_properties()
        if properties:
            # Group by organisation patterns
            bim_related = [p for p in properties if p.organisationName and 'BIM' in p.organisationName.upper()]
            standard_related = [p for p in properties if p.organisationName and any(term in p.organisationName.upper() for term in ['STANDARD', 'ISO', 'DIN'])]
            
            print(f"  - BIM-related organisations: {len(bim_related)} properties")
            if bim_related:
                for prop in bim_related[:3]:
                    print(f"    * {prop.name} - {prop.organisationName}")
            
            print(f"  - Standards organisations: {len(standard_related)} properties")
            if standard_related:
                for prop in standard_related[:3]:
                    print(f"    * {prop.name} - {prop.organisationName}")
        
    except Exception as e:
        print(f"An error occurred during organisation-based search: {e}")

def run_property_group_examples(client: EnhancedBimPortalClient):
    """Demonstrates property group operations."""
    print("\n--- Running Property Group Examples ---")

    try:
        # Search for property groups
        print("\n1. Searching for property groups...")
        property_groups = client.search_property_groups()
        if property_groups:
            print(f"Found {len(property_groups)} property groups:")
            for group in property_groups[:5]:  # Show first 5
                category_name = group.category.value if group.category else "None"
                print(f"  - {group.name}: Category={category_name}")
                if group.definition:
                    print(f"    Definition: {group.definition[:100]}...")
        else:
            print("No property groups found.")

        # Get detailed information for a property group
        if property_groups and property_groups[0].guid:
            selected_group = property_groups[0]
            print(f"\n2. Getting detailed information for group '{selected_group.name}'...")
            
            detailed_group = client.get_property_group(selected_group.guid)
            if detailed_group:
                print("Successfully retrieved detailed property group information:")
                print(f"  - GUID: {detailed_group.guid}")
                print(f"  - Organisation: {detailed_group.organisationName}")
                print(f"  - Version: {detailed_group.versionNumber}")
                
                if detailed_group.properties:
                    print(f"  - Contains {len(detailed_group.properties)} properties:")
                    for prop in detailed_group.properties[:3]:  # Show first 3
                        if prop.namesInLanguage and len(prop.namesInLanguage) > 0:
                            prop_name = prop.namesInLanguage[0].name
                        else:
                            prop_name = "Unnamed property"
                        print(f"    * {prop_name} ({prop.dataType})")
                
                if detailed_group.childrenPropertyGroups:
                    print(f"  - Has {len(detailed_group.childrenPropertyGroups)} child groups")
            else:
                print(f"Could not retrieve detailed information for group {selected_group.guid}")

    except Exception as e:
        print(f"An error occurred during property group examples: {e}")

def run_property_relationship_examples(client: EnhancedBimPortalClient):
    """Demonstrates property relationship analysis."""
    print("\n--- Running Property Relationship Examples ---")

    try:
        properties = client.search_properties()
        if not properties:
            print("No properties available for relationship analysis.")
            return

        print(f"\nAnalyzing relationships in {len(properties)} properties...")

        # Find properties with parent relationships
        properties_with_parents = [p for p in properties if p.parentGuids and len(p.parentGuids) > 0]
        print(f"1. Properties with parent relationships: {len(properties_with_parents)}")
        
        if properties_with_parents:
            for prop in properties_with_parents[:3]:
                print(f"  - {prop.name}: {len(prop.parentGuids)} parent(s)")

        # Analyze version information
        versioned_properties = [p for p in properties if p.versionNumber and p.versionNumber > 1]
        print(f"2. Properties with version > 1: {len(versioned_properties)}")
        
        if versioned_properties:
            for prop in versioned_properties[:3]:
                print(f"  - {prop.name}: v{prop.versionNumber}")

        # Find properties with catalog information
        cataloged_properties = [p for p in properties if p.catalogInformation]
        print(f"3. Properties with catalog information: {len(cataloged_properties)}")

    except Exception as e:
        print(f"An error occurred during relationship analysis: {e}")

def main():
    """Runs all property examples with the enhanced Pydantic client."""
    print("======== Starting BIM Portal API Property Examples ========")
    
    if not os.getenv("BIM_PORTAL_USERNAME"):
        print("Credentials not found. Some examples may fail if private resources are accessed.")

    client = setup_client()
    
    run_basic_property_examples(client)
    run_detailed_property_examples(client)
    run_property_analysis_examples(client)
    run_property_search_examples(client)
    run_property_group_examples(client)
    run_property_relationship_examples(client)
    
    print("\n======== Property Examples Complete ========")

if __name__ == "__main__":
    main()