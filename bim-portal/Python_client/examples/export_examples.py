"""
Enhanced export examples using comprehensive BIM Portal API client functions.

This module demonstrates various export workflows using the full range of 
enhanced client methods for projects, LOINs, domain models, context info, and templates.
"""

import os
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
PUBLIC_LOIN_GUID = BIMPortalConfig.PUBLIC_RESOURCE_GUID


def find_exportable_project(client: EnhancedBimPortalClient):
    """Find the first project that can actually be exported."""
    try:
        projects = client.search_projects()
        if not projects:
            return None
        
        print(f"🔍 Checking {len(projects)} projects for export capability...")
        
        for i, project in enumerate(projects, 1):
            print(f"   🧪 Testing project {i}: {project.name[:30]}...")
            
            # First check if we can get project details
            detailed_project = client.get_project(project.guid)
            if detailed_project is None:
                print(f"      ⏭️ Skip: Cannot access project details")
                continue
            
            # Then test if PDF export works
            response = client.get(f"/aia/api/v1/public/aiaProject/{project.guid}/pdf")
            if response.status_code == 200:
                print(f"      ✅ Found exportable project: {project.name}")
                return project
            else:
                print(f"      ⏭️ Skip: Export not available (status: {response.status_code})")
        
        print("   ❌ No exportable projects found")
        return None
        
    except Exception as e:
        print(f"❌ Error finding exportable project: {e}")
        return None

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

def setup_export_directory() -> Path:
    """Setup and return the export directory."""
    export_dir = Path(BIMPortalConfig.EXPORT_DIRECTORY)
    export_dir.mkdir(exist_ok=True)
    print(f"📁 Export directory: {export_dir.absolute()}")
    return export_dir

def run_project_export_examples(client: EnhancedBimPortalClient):
    """Demonstrates project export workflows with smart project selection."""
    print("\n" + "="*60)
    print("📊 PROJECT EXPORT EXAMPLES")
    print("="*60)
    
    export_dir = setup_export_directory()
    
    print("\n1️⃣ Searching for available projects...")
    try:
        projects = client.search_projects()
        if not projects:
            print("📭 No projects found for export")
            return
        
        print(f"✅ Found {len(projects)} projects:")
        print("📝 First 3 available projects:")
        for i, project in enumerate(projects[:13], 1):
            print(f"   {i}. {project.name} ({project.guid})")
        
            # # Use smart selection instead of blindly picking first project
            # print(f"\n2️⃣ Finding exportable project...")
            # selected_project = find_exportable_project(client)
            
            # if not selected_project:
            #     print("❌ No exportable projects found. All projects may be restricted or incomplete.")
            #     return
            selected_project = project
            print(f"\n🎯 Using project: '{selected_project.name}'")
            
            # Test all available project export formats
            export_results = {}
            
            print("\n3️⃣ Exporting project in multiple formats...")
            
            # PDF Export
            print("   📄 Exporting as PDF...")
            pdf_content = client.export_project_pdf(selected_project.guid)
            if pdf_content:
                pdf_path = export_dir / f"project_{selected_project.guid}.pdf"
                with open(pdf_path, "wb") as f:
                    f.write(pdf_content)
                export_results['PDF'] = pdf_path
                print(f"   ✅ PDF exported: {pdf_path}")
            else:
                print("   ❌ PDF export failed")
            
            # OpenOffice Export
            print("   📝 Exporting as OpenOffice...")
            odt_content = client.export_project_openoffice(selected_project.guid)
            if odt_content:
                odt_path = export_dir / f"project_{selected_project.guid}.odt"
                with open(odt_path, "wb") as f:
                    f.write(odt_content)
                export_results['OpenOffice'] = odt_path
                print(f"   ✅ OpenOffice exported: {odt_path}")
            else:
                print("   ❌ OpenOffice export failed")
            
            # OKSTRA Export
            print("   🏗️ Exporting as OKSTRA...")
            okstra_content = client.export_project_okstra(selected_project.guid)
            if okstra_content:
                okstra_path = export_dir / f"project_{selected_project.guid}.zip"
                with open(okstra_path, "wb") as f:
                    f.write(okstra_content)
                export_results['OKSTRA'] = okstra_path
                print(f"   ✅ OKSTRA exported: {okstra_path}")
            else:
                print("   ❌ OKSTRA export failed")
            
            # LOIN-XML Export (may require special permissions)
            print("   🔗 Exporting as LOIN-XML...")
            try:
                loin_xml_content = client.export_project_loin_xml(selected_project.guid)
                if loin_xml_content:
                    loin_xml_path = export_dir / f"project_{selected_project.guid}_loin.zip"
                    with open(loin_xml_path, "wb") as f:
                        f.write(loin_xml_content)
                    export_results['LOIN-XML'] = loin_xml_path
                    print(f"   ✅ LOIN-XML exported: {loin_xml_path}")
                else:
                    print("   ⚠️ LOIN-XML export failed (may require special permissions)")
            except Exception as e:
                print(f"   ⚠️ LOIN-XML export error: {e}")
            
            # IDS Export (may require special permissions)
            print("   🆔 Exporting as IDS...")
            try:
                ids_content = client.export_project_ids(selected_project.guid)
                if ids_content:
                    ids_path = export_dir / f"project_{selected_project.guid}.ids"
                    with open(ids_path, "wb") as f:
                        f.write(ids_content)
                    export_results['IDS'] = ids_path
                    print(f"   ✅ IDS exported: {ids_path}")
                else:
                    print("   ⚠️ IDS export failed (may require special permissions)")
            except Exception as e:
                print(f"   ⚠️ IDS export error: {e}")
            
            # Summary
            total_formats = 5
            successful_formats = len(export_results)
            print(f"\n📈 Export Summary: {successful_formats}/{total_formats} formats successful")
            
            if export_results:
                for format_name, path in export_results.items():
                    print(f"   ✅ {format_name}: {path}")
            
            if successful_formats < 3:
                print("💡 Note: Some export formats may require special permissions or project setup")
            
    except Exception as e:
        print(f"❌ Error in project export examples: {e}")

def run_loin_export_examples(client: EnhancedBimPortalClient):
    """Demonstrates comprehensive LOIN export workflows."""
    print("\n" + "="*60)
    print("📋 LOIN EXPORT EXAMPLES")
    print("="*60)
    
    export_dir = setup_export_directory()
    
    print("\n1️⃣ Searching for available LOINs...")
    try:
        loins = client.search_loins()
        if not loins:
            print("📭 No LOINs found for export")
            return
        
        print(f"✅ Found {len(loins)} LOINs:")
        for i, loin in enumerate(loins[:3], 1):
            print(f"   {i}. {loin.name} ({loin.guid})")
        
        selected_loin = loins[0]
        print(f"\n🎯 Using LOIN: '{selected_loin.name}'")
        
        export_results = {}
        
        print("\n2️⃣ Exporting LOIN in multiple formats...")
        
        # PDF Export
        print("   📄 Exporting as PDF...")
        pdf_content = client.export_loin_pdf(selected_loin.guid)
        if pdf_content:
            pdf_path = export_dir / f"loin_{selected_loin.guid}.pdf"
            with open(pdf_path, "wb") as f:
                f.write(pdf_content)
            export_results['PDF'] = pdf_path
            print(f"   ✅ PDF exported: {pdf_path}")
        else:
            print("   ❌ PDF export failed")
        
        # OpenOffice Export
        print("   📝 Exporting as OpenOffice...")
        odt_content = client.export_loin_openoffice(selected_loin.guid)
        if odt_content:
            odt_path = export_dir / f"loin_{selected_loin.guid}.odt"
            with open(odt_path, "wb") as f:
                f.write(odt_content)
            export_results['OpenOffice'] = odt_path
            print(f"   ✅ OpenOffice exported: {odt_path}")
        else:
            print("   ❌ OpenOffice export failed")
        
        # OKSTRA Export
        print("   🏗️ Exporting as OKSTRA...")
        okstra_content = client.export_loin_okstra(selected_loin.guid)
        if okstra_content:
            okstra_path = export_dir / f"loin_{selected_loin.guid}.zip"
            with open(okstra_path, "wb") as f:
                f.write(okstra_content)
            export_results['OKSTRA'] = okstra_path
            print(f"   ✅ OKSTRA exported: {okstra_path}")
        else:
            print("   ❌ OKSTRA export failed")
        
        # XML Export
        print("   🔗 Exporting as LOIN-XML...")
        xml_content = client.export_loin_xml(selected_loin.guid)
        if xml_content:
            xml_path = export_dir / f"loin_{selected_loin.guid}.xml"
            with open(xml_path, "wb") as f:
                f.write(xml_content)
            export_results['LOIN-XML'] = xml_path
            print(f"   ✅ LOIN-XML exported: {xml_path}")
        else:
            print("   ❌ LOIN-XML export failed")
        
        # IDS Export
        print("   🆔 Exporting as IDS...")
        ids_content = client.export_loin_ids(selected_loin.guid)
        if ids_content:
            ids_path = export_dir / f"loin_{selected_loin.guid}.ids"
            with open(ids_path, "wb") as f:
                f.write(ids_content)
            export_results['IDS'] = ids_path
            print(f"   ✅ IDS exported: {ids_path}")
        else:
            print("   ❌ IDS export failed")
        
        # Summary
        print(f"\n📈 Export Summary: {len(export_results)}/5 formats successful")
        for format_name, path in export_results.items():
            print(f"   ✅ {format_name}: {path}")
        
    except Exception as e:
        print(f"❌ Error in LOIN export examples: {e}")

def run_domain_model_export_examples(client: EnhancedBimPortalClient):
    """Demonstrates domain-specific model export workflows."""
    print("\n" + "="*60)
    print("🏗️  DOMAIN MODEL EXPORT EXAMPLES")
    print("="*60)
    
    export_dir = setup_export_directory()
    
    print("\n1️⃣ Searching for available domain models...")
    try:
        domain_models = client.search_domain_models()
        if not domain_models:
            print("📭 No domain models found for export")
            return
        
        print(f"✅ Found {len(domain_models)} domain models:")
        for i, model in enumerate(domain_models[:3], 1):
            print(f"   {i}. {model.name} ({model.guid})")
        
        selected_model = domain_models[0]
        print(f"\n🎯 Using domain model: '{selected_model.name}'")
        
        export_results = {}
        
        print("\n2️⃣ Exporting domain model in multiple formats...")
        
        # PDF Export
        print("   📄 Exporting as PDF...")
        pdf_content = client.export_domain_model_pdf(selected_model.guid)
        if pdf_content:
            pdf_path = export_dir / f"domain_model_{selected_model.guid}.pdf"
            with open(pdf_path, "wb") as f:
                f.write(pdf_content)
            export_results['PDF'] = pdf_path
            print(f"   ✅ PDF exported: {pdf_path}")
        else:
            print("   ❌ PDF export failed")
        
        # OpenOffice Export
        print("   📝 Exporting as OpenOffice...")
        odt_content = client.export_domain_model_openoffice(selected_model.guid)
        if odt_content:
            odt_path = export_dir / f"domain_model_{selected_model.guid}.odt"
            with open(odt_path, "wb") as f:
                f.write(odt_content)
            export_results['OpenOffice'] = odt_path
            print(f"   ✅ OpenOffice exported: {odt_path}")
        else:
            print("   ❌ OpenOffice export failed")
        
        # OKSTRA Export
        print("   🏗️ Exporting as OKSTRA...")
        okstra_content = client.export_domain_model_okstra(selected_model.guid)
        if okstra_content:
            okstra_path = export_dir / f"domain_model_{selected_model.guid}.zip"
            with open(okstra_path, "wb") as f:
                f.write(okstra_content)
            export_results['OKSTRA'] = okstra_path
            print(f"   ✅ OKSTRA exported: {okstra_path}")
        else:
            print("   ❌ OKSTRA export failed")
        
        # LOIN-XML Export
        print("   🔗 Exporting as LOIN-XML...")
        loin_xml_content = client.export_domain_model_loin_xml(selected_model.guid)
        if loin_xml_content:
            loin_xml_path = export_dir / f"domain_model_{selected_model.guid}_loin.zip"
            with open(loin_xml_path, "wb") as f:
                f.write(loin_xml_content)
            export_results['LOIN-XML'] = loin_xml_path
            print(f"   ✅ LOIN-XML exported: {loin_xml_path}")
        else:
            print("   ❌ LOIN-XML export failed")
        
        # IDS Export
        print("   🆔 Exporting as IDS...")
        ids_content = client.export_domain_model_ids(selected_model.guid)
        if ids_content:
            ids_path = export_dir / f"domain_model_{selected_model.guid}.ids"
            with open(ids_path, "wb") as f:
                f.write(ids_content)
            export_results['IDS'] = ids_path
            print(f"   ✅ IDS exported: {ids_path}")
        else:
            print("   ❌ IDS export failed")
        
        # Summary
        print(f"\n📈 Export Summary: {len(export_results)}/5 formats successful")
        for format_name, path in export_results.items():
            print(f"   ✅ {format_name}: {path}")
        
    except Exception as e:
        print(f"❌ Error in domain model export examples: {e}")

def run_context_info_export_examples(client: EnhancedBimPortalClient):
    """Demonstrates context information export workflows."""
    print("\n" + "="*60)
    print("📚 CONTEXT INFORMATION EXPORT EXAMPLES")
    print("="*60)
    
    export_dir = setup_export_directory()
    
    print("\n1️⃣ Searching for available context information...")
    try:
        context_infos = client.search_context_info()
        if not context_infos:
            print("📭 No context information found for export")
            return
        
        print(f"✅ Found {len(context_infos)} context information items:")
        for i, context in enumerate(context_infos[:3], 1):
            print(f"   {i}. {context.name} ({context.guid})")
        
        selected_context = context_infos[0]
        print(f"\n🎯 Using context info: '{selected_context.name}'")
        
        export_results = {}
        
        print("\n2️⃣ Exporting context information...")
        
        # PDF Export
        print("   📄 Exporting as PDF...")
        pdf_content = client.export_context_info_pdf(selected_context.guid)
        if pdf_content:
            pdf_path = export_dir / f"context_{selected_context.guid}.pdf"
            with open(pdf_path, "wb") as f:
                f.write(pdf_content)
            export_results['PDF'] = pdf_path
            print(f"   ✅ PDF exported: {pdf_path}")
        else:
            print("   ❌ PDF export failed")
        
        # OpenOffice Export
        print("   📝 Exporting as OpenOffice...")
        odt_content = client.export_context_info_openoffice(selected_context.guid)
        if odt_content:
            odt_path = export_dir / f"context_{selected_context.guid}.odt"
            with open(odt_path, "wb") as f:
                f.write(odt_content)
            export_results['OpenOffice'] = odt_path
            print(f"   ✅ OpenOffice exported: {odt_path}")
        else:
            print("   ❌ OpenOffice export failed")
        
        # Summary
        print(f"\n📈 Export Summary: {len(export_results)}/2 formats successful")
        for format_name, path in export_results.items():
            print(f"   ✅ {format_name}: {path}")
        
    except Exception as e:
        print(f"❌ Error in context info export examples: {e}")

def run_template_export_examples(client: EnhancedBimPortalClient):
    """Demonstrates AIA template export workflows."""
    print("\n" + "="*60)
    print("📋 AIA TEMPLATE EXPORT EXAMPLES")
    print("="*60)
    
    export_dir = setup_export_directory()
    
    print("\n1️⃣ Searching for available templates...")
    try:
        templates = client.search_templates()
        if not templates:
            print("📭 No templates found for export")
            return
        
        print(f"✅ Found {len(templates)} templates:")
        for i, template in enumerate(templates[:3], 1):
            print(f"   {i}. {template.name} ({template.guid})")
        
        selected_template = templates[0]
        print(f"\n🎯 Using template: '{selected_template.name}'")
        
        export_results = {}
        
        print("\n2️⃣ Exporting template...")
        
        # PDF Export
        print("   📄 Exporting as PDF...")
        pdf_content = client.export_template_pdf(selected_template.guid)
        if pdf_content:
            pdf_path = export_dir / f"template_{selected_template.guid}.pdf"
            with open(pdf_path, "wb") as f:
                f.write(pdf_content)
            export_results['PDF'] = pdf_path
            print(f"   ✅ PDF exported: {pdf_path}")
        else:
            print("   ❌ PDF export failed")
        
        # OpenOffice Export
        print("   📝 Exporting as OpenOffice...")
        odt_content = client.export_template_openoffice(selected_template.guid)
        if odt_content:
            odt_path = export_dir / f"template_{selected_template.guid}.odt"
            with open(odt_path, "wb") as f:
                f.write(odt_content)
            export_results['OpenOffice'] = odt_path
            print(f"   ✅ OpenOffice exported: {odt_path}")
        else:
            print("   ❌ OpenOffice export failed")
        
        # Summary
        print(f"\n📈 Export Summary: {len(export_results)}/2 formats successful")
        for format_name, path in export_results.items():
            print(f"   ✅ {format_name}: {path}")
        
    except Exception as e:
        print(f"❌ Error in template export examples: {e}")

def run_batch_export_examples(client: EnhancedBimPortalClient):
    """Demonstrates batch export workflows across multiple resource types."""
    print("\n" + "="*60)
    print("🔄 BATCH EXPORT EXAMPLES")
    print("="*60)
    
    export_dir = setup_export_directory()
    total_exported = 0
    
    print("\n1️⃣ Batch exporting projects as PDF...")
    try:
        projects = client.search_projects()
        if projects:
            project_count = min(3, len(projects))
            for i, project in enumerate(projects[:project_count], 1):
                print(f"   📊 Exporting project {i}/{project_count}: {project.name}")
                pdf_content = client.export_project_pdf(project.guid)
                if pdf_content:
                    pdf_path = export_dir / f"batch_project_{i}_{project.guid}.pdf"
                    with open(pdf_path, "wb") as f:
                        f.write(pdf_content)
                    total_exported += 1
                    print(f"   ✅ Success: {pdf_path}")
                else:
                    print(f"   ❌ Failed: {project.name}")
        else:
            print("   📭 No projects available")
    except Exception as e:
        print(f"   ❌ Error in project batch export: {e}")
    
    print("\n2️⃣ Batch exporting LOINs as PDF...")
    try:
        loins = client.search_loins()
        if loins:
            loin_count = min(2, len(loins))
            for i, loin in enumerate(loins[:loin_count], 1):
                print(f"   📋 Exporting LOIN {i}/{loin_count}: {loin.name}")
                pdf_content = client.export_loin_pdf(loin.guid)
                if pdf_content:
                    pdf_path = export_dir / f"batch_loin_{i}_{loin.guid}.pdf"
                    with open(pdf_path, "wb") as f:
                        f.write(pdf_content)
                    total_exported += 1
                    print(f"   ✅ Success: {pdf_path}")
                else:
                    print(f"   ❌ Failed: {loin.name}")
        else:
            print("   📭 No LOINs available")
    except Exception as e:
        print(f"   ❌ Error in LOIN batch export: {e}")
    
    print(f"\n🏁 **BATCH EXPORT COMPLETE:** {total_exported} files exported successfully")

def run_filter_examples(client: EnhancedBimPortalClient):
    """Demonstrates filter functionality."""
    print("\n" + "="*60)
    print("🔍 FILTER EXAMPLES")
    print("="*60)
    
    print("\n1️⃣ Getting AIA filters...")
    try:
        aia_filters = client.get_aia_filters()
        if aia_filters:
            print(f"✅ Found {len(aia_filters)} AIA filter groups:")
            for filter_group in aia_filters[:3]:
                print(f"   📂 {filter_group.name}")
                if filter_group.filters:
                    for filter_item in filter_group.filters[:2]:
                        print(f"     - {filter_item.name}")
        else:
            print("📭 No AIA filters found")
    except Exception as e:
        print(f"❌ Error getting AIA filters: {e}")
    
    print("\n2️⃣ Getting property filters...")
    try:
        merkmale_filters = client.get_merkmale_filters()
        if merkmale_filters:
            print(f"✅ Found {len(merkmale_filters)} property filter groups:")
            for filter_group in merkmale_filters[:3]:
                print(f"   📂 {filter_group.name}")
                if filter_group.filters:
                    for filter_item in filter_group.filters[:2]:
                        print(f"     - {filter_item.name}")
        else:
            print("📭 No property filters found")
    except Exception as e:
        print(f"❌ Error getting property filters: {e}")

def run_organization_examples(client: EnhancedBimPortalClient):
    """Demonstrates organization-related functionality."""
    print("\n" + "="*60)
    print("🏢 ORGANIZATION EXAMPLES")
    print("="*60)
    
    print("\n1️⃣ Getting all organizations...")
    try:
        all_orgs = client.get_organisations()
        if all_orgs:
            print(f"✅ Found {len(all_orgs)} organizations:")
            for org in all_orgs[:5]:
                print(f"   🏢 {org.name}")
        else:
            print("📭 No organizations found")
    except Exception as e:
        print(f"❌ Error getting organizations: {e}")
    
    print("\n2️⃣ Getting user's organizations...")
    try:
        my_orgs = client.get_my_organisations()
        if my_orgs:
            print(f"✅ User is member of {len(my_orgs)} organizations:")
            for org in my_orgs:
                print(f"   🏢 {org.name}")
        else:
            print("📭 User is not member of any organizations (or authentication required)")
    except Exception as e:
        print(f"❌ Error getting user organizations: {e}")

def main():
    """Runs comprehensive export examples showcasing all enhanced client capabilities."""
    print("🚀 " + "="*70)
    print("🚀 COMPREHENSIVE BIM PORTAL API EXPORT EXAMPLES")
    print("🚀 " + "="*70)
    
    if check_credentials():
        print("🔐 Setting up authenticated client...")
        auth_service = AuthService(guid=PRIVATE_LOIN_GUID)
        client = EnhancedBimPortalClient(auth_service=auth_service, base_url=BASE_URL)
        
        # # Run all export examples
        # run_project_export_examples(client)
        # run_loin_export_examples(client)
        # run_domain_model_export_examples(client)
        # run_context_info_export_examples(client)
        # run_template_export_examples(client)
        # run_batch_export_examples(client)
        #
        # # Run utility examples
        # run_filter_examples(client)
        run_organization_examples(client)
        
        print("\n" + "🎉 " + "="*70)
        print("🎉 ALL EXPORT EXAMPLES COMPLETE!")
        print("🎉 " + "="*70)
        print(f"📁 Check the '{BIMPortalConfig.EXPORT_DIRECTORY}' directory for exported files")
        
    else:
        print("❌ Cannot run export examples without credentials")
        print("🛠️ Run: python demo/credentials_setup.py")

if __name__ == "__main__":
    main()