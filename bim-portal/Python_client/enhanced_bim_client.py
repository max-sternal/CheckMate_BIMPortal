"""
Comprehensive BIM Portal HTTP Client covering all documented endpoints.
"""

import json
from typing import List, Optional, Dict, Any, Union
from uuid import UUID
from http import HTTPStatus

import httpx
from pydantic import ValidationError

from models import (
    # Existing imports
    AIAProjectPublicDto, SimpleAiaProjectPublicDto, AiaProjectForPublicRequest,
    PropertyOrGroupForPublicDto, PropertyOrGroupForPublicRequest,
    PropertyDto, PropertyGroupDto, SimpleLoinPublicDto, LoinForPublicRequest, LOINPublicDto,
    
    # Additional imports needed for comprehensive coverage
    OrganisationForPublicDTO, SimpleDomainSpecificModelPublicDto, 
    AiaDomainSpecificModelForPublicRequest, AIADomainSpecificModelPublicDto,
    SimpleContextInfoPublicDto, AiaContextInfoPublicRequest, AIAContextInfoPublicDto,
    SimpleAiaTemplatePublicDto, AiaTemplateForPublicRequest, AIATemplatePublicDto,
    FilterForPublicDto, FilterGroupForPublicDto, TagForPublicDto, TagGroupForPublicDto,
    UserLoginPublicDto, JWTTokenPublicDto, RefreshTokenRequestDTO
)
from auth.auth_service_impl import AuthService, AuthenticationError
from auth.auth_config import AUTH_RETRY_LIMIT, logger
from config import BIMPortalConfig

class EnhancedBimPortalClient:
    """
    Comprehensive HTTP client for BIM Portal API covering all documented endpoints.
    """
    
    def __init__(self, auth_service: AuthService, base_url: str = BIMPortalConfig.BASE_URL, 
             raise_on_unexpected_status: bool = False):
        self.auth_service = auth_service
        self.base_url = base_url
        self.raise_on_unexpected_status = raise_on_unexpected_status
        
        self._httpx_client = httpx.Client(
            base_url=base_url,
            timeout=30.0,
            verify=True,
        )
    
    def _get_auth_headers(self) -> Dict[str, str]:
        """Get authentication headers if token is available."""
        headers = {
            "accept": "application/json",
            "Content-Type": "application/json"
        }
        
        try:
            token = self.auth_service.get_valid_token()
            if token:
                headers["Authorization"] = f"Bearer {token}"
        except AuthenticationError as e:
            logger.warning(f"Authentication failed: {e}. Proceeding with public access.")
        
        return headers
    
    def _make_authenticated_request(self, method: str, endpoint: str, 
                                   json_data: Optional[Dict] = None) -> httpx.Response:
        """Make an authenticated HTTP request with retry logic."""
        for attempt in range(AUTH_RETRY_LIMIT + 1):
            headers = self._get_auth_headers()
            
            try:
                response = self._httpx_client.request(
                    method=method.upper(), 
                    url=endpoint, 
                    headers=headers, 
                    json=json_data
                )
                
                if response.status_code in (HTTPStatus.UNAUTHORIZED, HTTPStatus.FORBIDDEN):
                    if attempt >= AUTH_RETRY_LIMIT:
                        if self.raise_on_unexpected_status:
                            response.raise_for_status()
                        return response
                    
                    self.auth_service._token_manager.clear_tokens()
                    continue
                
                if self.raise_on_unexpected_status and response.status_code >= 400:
                    response.raise_for_status()
                
                return response
                
            except httpx.HTTPError as e:
                if attempt >= AUTH_RETRY_LIMIT:
                    raise e
        
        raise RuntimeError("Exited retry loop unexpectedly.")
    
    def _parse_response_json(self, response: httpx.Response) -> Optional[Dict[str, Any]]:
        """Parse response JSON with error handling."""
        try:
            if response.status_code == 200:
                return response.json()
            return None
        except json.JSONDecodeError as e:
            logger.error(f"Failed to parse JSON response: {e}")
            return None
    
    def _parse_model(self, data: Any, model_class) -> Optional[Any]:
        """Parse data into a Pydantic model with error handling."""
        if data is None:
            return None
            
        try:
            if isinstance(data, list):
                return [model_class.model_validate(item) for item in data if item is not None]
            else:
                return model_class.model_validate(data)
        except ValidationError as e:
            logger.warning(f"Failed to parse {model_class.__name__}: {e}")
            return None
        except Exception as e:
            logger.error(f"Unexpected error parsing {model_class.__name__}: {e}")
            return None
    
    # === INFRASTRUCTURE/AUTH ENDPOINTS ===
    
    def login(self, credentials: UserLoginPublicDto) -> Optional[JWTTokenPublicDto]:
        """Login to the system."""
        try:
            response = self._make_authenticated_request(
                "POST", "/infrastruktur/api/v1/public/auth/login", 
                credentials.model_dump()
            )
            data = self._parse_response_json(response)
            return self._parse_model(data, JWTTokenPublicDto)
        except Exception as e:
            logger.error(f"Error during login: {e}")
            return None
    
    def refresh_token(self, refresh_request: RefreshTokenRequestDTO) -> Optional[JWTTokenPublicDto]:
        """Refresh the authorization token."""
        try:
            response = self._make_authenticated_request(
                "POST", "/infrastruktur/api/v1/public/auth/refresh", 
                refresh_request.model_dump()
            )
            data = self._parse_response_json(response)
            return self._parse_model(data, JWTTokenPublicDto)
        except Exception as e:
            logger.error(f"Error refreshing token: {e}")
            return None
    
    def logout(self) -> bool:
        """Logout from the system."""
        try:
            response = self._make_authenticated_request("POST", "/infrastruktur/api/v1/public/auth/logout")
            return response.status_code == 200
        except Exception as e:
            logger.error(f"Error during logout: {e}")
            return False
    
    def get_organisations(self) -> List[OrganisationForPublicDTO]:
        """Get list of all organizations available via the REST API."""
        try:
            response = self._make_authenticated_request("GET", "/infrastruktur/api/v1/public/organisation")
            data = self._parse_response_json(response)
            organisations = self._parse_model(data, OrganisationForPublicDTO)
            return organisations if organisations else []
        except Exception as e:
            logger.error(f"Error getting organisations: {e}")
            return []
    
    def get_my_organisations(self) -> List[OrganisationForPublicDTO]:
        """Get list of organizations where the user is a member."""
        try:
            response = self._make_authenticated_request("GET", "/infrastruktur/api/v1/public/organisation/my")
            data = self._parse_response_json(response)
            organisations = self._parse_model(data, OrganisationForPublicDTO)
            return organisations if organisations else []
        except Exception as e:
            logger.error(f"Error getting user organisations: {e}")
            return []
    
    # === PROPERTY GROUPS (MERKMALSGRUPPEN) ===
    
    def search_property_groups(self, request: Optional[PropertyOrGroupForPublicRequest] = None) -> List[PropertyOrGroupForPublicDto]:
        """Search for property groups matching the given criteria."""
        request_data = request.model_dump(exclude_none=True) if request else {}
        
        try:
            response = self._make_authenticated_request(
                "POST", "/merkmale/api/v1/public/propertygroup", request_data
            )
            data = self._parse_response_json(response)
            groups = self._parse_model(data, PropertyOrGroupForPublicDto)
            return groups if groups else []
        except Exception as e:
            logger.error(f"Error searching property groups: {e}")
            return []
    
    def get_property_group(self, guid: UUID) -> Optional[PropertyGroupDto]:
        """Get detailed information about a specific property group."""
        try:
            response = self._make_authenticated_request("GET", f"/merkmale/api/v1/public/propertygroup/{guid}")
            data = self._parse_response_json(response)
            return self._parse_model(data, PropertyGroupDto)
        except Exception as e:
            logger.error(f"Error getting property group {guid}: {e}")
            return None
    

    
    # === PROPERTIES (MERKMALE) ===
    
    def search_properties(self, request: Optional[PropertyOrGroupForPublicRequest] = None) -> List[PropertyOrGroupForPublicDto]:
        """Search for properties matching the given criteria."""
        request_data = request.model_dump(exclude_none=True) if request else {"searchString": "a"}
        
        try:
            response = self._make_authenticated_request("POST", "/merkmale/api/v1/public/property", request_data)
            data = self._parse_response_json(response)
            properties = self._parse_model(data, PropertyOrGroupForPublicDto)
            return properties if properties else []
        except Exception as e:
            logger.error(f"Error searching properties: {e}")
            return []
    
    def get_property(self, guid: UUID) -> Optional[PropertyDto]:
        """Get detailed information about a specific property."""
        try:
            response = self._make_authenticated_request("GET", f"/merkmale/api/v1/public/property/{guid}")
            data = self._parse_response_json(response)
            return self._parse_model(data, PropertyDto)
        except Exception as e:
            logger.error(f"Error getting property {guid}: {e}")
            return None

    def get_merkmale_filters(self) -> List[FilterGroupForPublicDto]:
        """Get all global filters for properties."""
        try:
            response = self._make_authenticated_request("GET", "/merkmale/api/v1/public/filter")
            data = self._parse_response_json(response)
            filter = self._parse_model(data, FilterGroupForPublicDto)
            return filter if filter else []
        except Exception as e:
            logger.error(f"Error getting property filters: {e}")
            return []

    # === LOINS ===
    
    def search_loins(self, request: Optional[LoinForPublicRequest] = None) -> List[SimpleLoinPublicDto]:
        """Search for LOINs matching the given criteria."""
        request_data = request.model_dump(exclude_none=True) if request else {}
        
        try:
            response = self._make_authenticated_request("POST", "/aia/api/v1/public/loin", request_data)
            data = self._parse_response_json(response)
            loins = self._parse_model(data, SimpleLoinPublicDto)
            return loins if loins else []
        except Exception as e:
            logger.error(f"Error searching LOINs: {e}")
            return []
    
    def get_loin(self, guid: UUID) -> Optional[LOINPublicDto]:
        """Get detailed information about a specific LOIN."""
        try:
            response = self._make_authenticated_request("GET", f"/aia/api/v1/public/loin/{guid}")
            data = self._parse_response_json(response)
            return self._parse_model(data, LOINPublicDto)
        except Exception as e:
            logger.error(f"Error getting LOIN {guid}: {e}")
            return None
    
    def export_loin_pdf(self, guid: UUID) -> Optional[bytes]:
        """Export LOIN as PDF."""
        try:
            response = self._make_authenticated_request("GET", f"/aia/api/v1/public/loin/{guid}/pdf")
            return response.content if response.status_code == 200 else None
        except Exception as e:
            logger.error(f"Error exporting LOIN {guid} to PDF: {e}")
            return None
    
    def export_loin_openoffice(self, guid: UUID) -> Optional[bytes]:
        """Export LOIN as OpenOffice format."""
        try:
            response = self._make_authenticated_request("GET", f"/aia/api/v1/public/loin/{guid}/openOffice")
            return response.content if response.status_code == 200 else None
        except Exception as e:
            logger.error(f"Error exporting LOIN {guid} to OpenOffice: {e}")
            return None
    
    def export_loin_okstra(self, guid: UUID) -> Optional[bytes]:
        """Export LOIN as OKSTRA zip file."""
        try:
            response = self._make_authenticated_request("GET", f"/aia/api/v1/public/loin/{guid}/okstra")
            return response.content if response.status_code == 200 else None
        except Exception as e:
            logger.error(f"Error exporting LOIN {guid} to OKSTRA: {e}")
            return None
    
    def export_loin_xml(self, guid: UUID) -> Optional[bytes]:
        """Export LOIN as LOIN-XML format."""
        try:
            response = self._make_authenticated_request("GET", f"/aia/api/v1/public/loin/{guid}/loinXML")
            return response.content if response.status_code == 200 else None
        except Exception as e:
            logger.error(f"Error exporting LOIN {guid} to XML: {e}")
            return None
    
    def export_loin_ids(self, guid: UUID) -> Optional[bytes]:
        """Export LOIN as IDS-XML format."""
        try:
            response = self._make_authenticated_request("GET", f"/aia/api/v1/public/loin/{guid}/IDS")
            return response.content if response.status_code == 200 else None
        except Exception as e:
            logger.error(f"Error exporting LOIN {guid} to IDS: {e}")
            return None
    
    # === DOMAIN-SPECIFIC MODELS (FACHMODELLE) ===
    
    def search_domain_models(self, request: Optional[AiaDomainSpecificModelForPublicRequest] = None) -> List[SimpleDomainSpecificModelPublicDto]:
        """Search for domain-specific models matching the given criteria."""
        request_data = request.model_dump(exclude_none=True) if request else {}
        
        try:
            response = self._make_authenticated_request(
                "POST", "/aia/api/v1/public/domainSpecificModel", request_data
            )
            data = self._parse_response_json(response)
            models = self._parse_model(data, SimpleDomainSpecificModelPublicDto)
            return models if models else []
        except Exception as e:
            logger.error(f"Error searching domain models: {e}")
            return []
    
    def get_domain_model(self, guid: UUID) -> Optional[AIADomainSpecificModelPublicDto]:
        """Get detailed information about a specific domain-specific model."""
        try:
            response = self._make_authenticated_request("GET", f"/aia/api/v1/public/domainSpecificModel/{guid}")
            data = self._parse_response_json(response)
            return self._parse_model(data, AIADomainSpecificModelPublicDto)
        except Exception as e:
            logger.error(f"Error getting domain model {guid}: {e}")
            return None
    
    def export_domain_model_pdf(self, guid: UUID) -> Optional[bytes]:
        """Export domain-specific model as PDF."""
        try:
            response = self._make_authenticated_request("GET", f"/aia/api/v1/public/domainSpecificModel/{guid}/pdf")
            return response.content if response.status_code == 200 else None
        except Exception as e:
            logger.error(f"Error exporting domain model {guid} to PDF: {e}")
            return None
    
    def export_domain_model_openoffice(self, guid: UUID) -> Optional[bytes]:
        """Export domain-specific model as OpenOffice format."""
        try:
            response = self._make_authenticated_request("GET", f"/aia/api/v1/public/domainSpecificModel/{guid}/openOffice")
            return response.content if response.status_code == 200 else None
        except Exception as e:
            logger.error(f"Error exporting domain model {guid} to OpenOffice: {e}")
            return None
    
    def export_domain_model_okstra(self, guid: UUID) -> Optional[bytes]:
        """Export domain-specific model as OKSTRA zip file."""
        try:
            response = self._make_authenticated_request("GET", f"/aia/api/v1/public/domainSpecificModel/{guid}/okstra")
            return response.content if response.status_code == 200 else None
        except Exception as e:
            logger.error(f"Error exporting domain model {guid} to OKSTRA: {e}")
            return None
    
    def export_domain_model_loin_xml(self, guid: UUID) -> Optional[bytes]:
        """Export domain-specific model as LOIN-XML zip file."""
        try:
            response = self._make_authenticated_request("GET", f"/aia/api/v1/public/domainSpecificModel/{guid}/loinXML")
            return response.content if response.status_code == 200 else None
        except Exception as e:
            logger.error(f"Error exporting domain model {guid} to LOIN XML: {e}")
            return None
    
    def export_domain_model_ids(self, guid: UUID) -> Optional[bytes]:
        """Export domain-specific model as IDS format."""
        try:
            response = self._make_authenticated_request("GET", f"/aia/api/v1/public/domainSpecificModel/{guid}/IDS")
            return response.content if response.status_code == 200 else None
        except Exception as e:
            logger.error(f"Error exporting domain model {guid} to IDS: {e}")
            return None
    
    # === CONTEXT INFORMATION (KONTEXTINFORMATIONEN) ===
    
    def search_context_info(self, request: Optional[AiaContextInfoPublicRequest] = None) -> List[SimpleContextInfoPublicDto]:
        """Search for context information matching the given criteria."""
        request_data = request.model_dump(exclude_none=True) if request else {}
        
        try:
            response = self._make_authenticated_request(
                "POST", "/aia/api/v1/public/contextInfo", request_data
            )
            data = self._parse_response_json(response)
            contexts = self._parse_model(data, SimpleContextInfoPublicDto)
            return contexts if contexts else []
        except Exception as e:
            logger.error(f"Error searching context info: {e}")
            return []
    
    def get_context_info(self, guid: UUID) -> Optional[AIAContextInfoPublicDto]:
        """Get detailed information about specific context information."""
        try:
            response = self._make_authenticated_request("GET", f"/aia/api/v1/public/contextInfo/{guid}")
            data = self._parse_response_json(response)
            return self._parse_model(data, AIAContextInfoPublicDto)
        except Exception as e:
            logger.error(f"Error getting context info {guid}: {e}")
            return None
    
    def export_context_info_pdf(self, guid: UUID) -> Optional[bytes]:
        """Export context information as PDF."""
        try:
            response = self._make_authenticated_request("GET", f"/aia/api/v1/public/contextInfo/{guid}/pdf")
            return response.content if response.status_code == 200 else None
        except Exception as e:
            logger.error(f"Error exporting context info {guid} to PDF: {e}")
            return None
    
    def export_context_info_openoffice(self, guid: UUID) -> Optional[bytes]:
        """Export context information as OpenOffice format."""
        try:
            response = self._make_authenticated_request("GET", f"/aia/api/v1/public/contextInfo/{guid}/openOffice")
            return response.content if response.status_code == 200 else None
        except Exception as e:
            logger.error(f"Error exporting context info {guid} to OpenOffice: {e}")
            return None
    
    # === AIA TEMPLATES (AIA-VORLAGEN) ===
    
    def search_templates(self, request: Optional[AiaTemplateForPublicRequest] = None) -> List[SimpleAiaTemplatePublicDto]:
        """Search for AIA templates matching the given criteria."""
        request_data = request.model_dump(exclude_none=True) if request else {}
        
        try:
            response = self._make_authenticated_request(
                "POST", "/aia/api/v1/public/aiaTemplate", request_data
            )
            data = self._parse_response_json(response)
            templates = self._parse_model(data, SimpleAiaTemplatePublicDto)
            return templates if templates else []
        except Exception as e:
            logger.error(f"Error searching templates: {e}")
            return []
    
    def get_template(self, guid: UUID) -> Optional[AIATemplatePublicDto]:
        """Get detailed information about a specific AIA template."""
        try:
            response = self._make_authenticated_request("GET", f"/aia/api/v1/public/aiaTemplate/{guid}")
            data = self._parse_response_json(response)
            return self._parse_model(data, AIATemplatePublicDto)
        except Exception as e:
            logger.error(f"Error getting template {guid}: {e}")
            return None
    
    def export_template_pdf(self, guid: UUID) -> Optional[bytes]:
        """Export AIA template as PDF."""
        try:
            response = self._make_authenticated_request("GET", f"/aia/api/v1/public/aiaTemplate/{guid}/pdf")
            return response.content if response.status_code == 200 else None
        except Exception as e:
            logger.error(f"Error exporting template {guid} to PDF: {e}")
            return None
    
    def export_template_openoffice(self, guid: UUID) -> Optional[bytes]:
        """Export AIA template as OpenOffice format."""
        try:
            response = self._make_authenticated_request("GET", f"/aia/api/v1/public/aiaTemplate/{guid}/openOffice")
            return response.content if response.status_code == 200 else None
        except Exception as e:
            logger.error(f"Error exporting template {guid} to OpenOffice: {e}")
            return None
    
    # === AIA PROJECTS ===
    
    def search_projects(self, request: Optional[AiaProjectForPublicRequest] = None) -> List[SimpleAiaProjectPublicDto]:
        """Search for projects matching the given criteria."""
        request_data = request.model_dump(exclude_none=True) if request else {}
        
        try:
            response = self._make_authenticated_request("POST", "/aia/api/v1/public/aiaProject", request_data)
            data = self._parse_response_json(response)
            projects = self._parse_model(data, SimpleAiaProjectPublicDto)
            return projects if projects else []
        except Exception as e:
            logger.error(f"Error searching projects: {e}")
            return []
    
    def get_project(self, guid: UUID) -> Optional[AIAProjectPublicDto]:
        """Get detailed information about a specific project."""
        try:
            response = self._make_authenticated_request("GET", f"/aia/api/v1/public/aiaProject/{guid}")
            data = self._parse_response_json(response)
            return self._parse_model(data, AIAProjectPublicDto)
        except Exception as e:
            logger.error(f"Error getting project {guid}: {e}")
            return None
    
    def export_project_pdf(self, guid: UUID) -> Optional[bytes]:
        """Export project as PDF."""
        try:
            response = self._make_authenticated_request("GET", f"/aia/api/v1/public/aiaProject/{guid}/pdf")
            return response.content if response.status_code == 200 else None
        except Exception as e:
            logger.error(f"Error exporting project {guid} to PDF: {e}")
            return None
    
    def export_project_openoffice(self, guid: UUID) -> Optional[bytes]:
        """Export project as OpenOffice format."""
        try:
            response = self._make_authenticated_request("GET", f"/aia/api/v1/public/aiaProject/{guid}/openOffice")
            return response.content if response.status_code == 200 else None
        except Exception as e:
            logger.error(f"Error exporting project {guid} to OpenOffice: {e}")
            return None
    
    def export_project_okstra(self, guid: UUID) -> Optional[bytes]:
        """Export project as OKSTRA zip file."""
        try:
            response = self._make_authenticated_request("GET", f"/aia/api/v1/public/aiaProject/{guid}/okstra")
            return response.content if response.status_code == 200 else None
        except Exception as e:
            logger.error(f"Error exporting project {guid} to OKSTRA: {e}")
            return None
    
    def export_project_loin_xml(self, guid: UUID) -> Optional[bytes]:
        """Export project as LOIN-XML zip file."""
        try:
            response = self._make_authenticated_request("GET", f"/aia/api/v1/public/aiaProject/{guid}/loinXML")
            return response.content if response.status_code == 200 else None
        except Exception as e:
            logger.error(f"Error exporting project {guid} to LOIN XML: {e}")
            return None
    
    def export_project_ids(self, guid: UUID) -> Optional[bytes]:
        """Export project as IDS format."""
        try:
            response = self._make_authenticated_request("GET", f"/aia/api/v1/public/aiaProject/{guid}/IDS")
            return response.content if response.status_code == 200 else None
        except Exception as e:
            logger.error(f"Error exporting project {guid} to IDS: {e}")
            return None
    
    # === AIA FILTERS ===
    
    def get_aia_filters(self) -> List[FilterGroupForPublicDto]:
        """Get all global AIA filters."""
        try:
            response = self._make_authenticated_request("GET", "/aia/api/v1/public/filter")
            data = self._parse_response_json(response)
            filter = self._parse_model(data, FilterGroupForPublicDto)
            return filter if filter else []
        except Exception as e:
            logger.error(f"Error getting AIA filters: {e}")
            return []
    
    # === COMPATIBILITY METHODS ===
    
    def get_httpx_client(self):
        """Return self to maintain compatibility with existing code."""
        return self
    
    def get(self, endpoint: str, **kwargs) -> httpx.Response:
        """GET request with authentication."""
        return self._make_authenticated_request("GET", endpoint)
    
    def post(self, endpoint: str, json: Optional[Dict] = None, **kwargs) -> httpx.Response:
        """POST request with authentication."""
        return self._make_authenticated_request("POST", endpoint, json)
    
    def request(self, method: str, endpoint: str, **kwargs) -> httpx.Response:
        """Generic request method."""
        json_data = kwargs.get('json')
        return self._make_authenticated_request(method, endpoint, json_data)
    
    # === CONTEXT MANAGER SUPPORT ===
    
    def __enter__(self):
        return self
    
    def __exit__(self, exc_type, exc_val, exc_tb):
        self._httpx_client.close()