import os
import requests
import threading
from typing import Optional

from .auth_config import (
    LOGIN_URL,
    REFRESH_URL,
    BIM_PORTAL_USERNAME_ENV_VAR,
    BIM_PORTAL_PASSWORD_ENV_VAR,
    logger,
)
from .token_manager import TokenManager
# Import from your new Pydantic models instead of generated client
from models import (
    UserLoginPublicDto,
    RefreshTokenRequestDTO,
    JWTTokenPublicDto,
)

class AuthenticationError(Exception):
    """Custom exception for authentication failures."""
    pass


class AuthService:
    """
    Handles the authentication process, including login and token refreshing.
    Adapted to work with Pydantic models.
    """

    def __init__(self, guid: str, username: Optional[str] = None, password: Optional[str] = None):
        """
        Initializes the AuthService.

        Args:
            guid (str): The GUID required for authentication headers.
            username (str, optional): The user's email. Defaults to env var.
            password (str, optional): The user's password. Defaults to env var.
        """
        self.username = username or os.getenv(BIM_PORTAL_USERNAME_ENV_VAR)
        self.password = password or os.getenv(BIM_PORTAL_PASSWORD_ENV_VAR)
        self.guid = guid

        if not self.guid:
            raise ValueError("A GUID must be provided for authentication.")

        self._token_manager = TokenManager()
        self._lock = threading.Lock()

    def get_valid_token(self) -> Optional[str]:
        """
        Ensures a valid token is available and returns it.
        It handles token expiration by refreshing or re-logging in.
        Returns None if authentication is not configured or fails.
        """
        with self._lock:
            if not self.username or not self.password:
                logger.debug("No credentials provided; cannot get token. Proceeding with public access.")
                return None

            if not self._token_manager.is_token_expiring():
                return self._token_manager.get_access_token()

            logger.info("Token is missing or expiring. Attempting to refresh.")
            if self._refresh_token():
                return self._token_manager.get_access_token()

            logger.info("Token refresh failed or not possible. Attempting to log in.")
            if self._login():
                return self._token_manager.get_access_token()

            logger.error("Could not obtain a valid token after login attempt.")
            raise AuthenticationError("Failed to authenticate. Please check credentials and GUID.")

    def _login(self) -> bool:
        """
        Performs a login to get new access and refresh tokens.
        """
        logger.debug(f"Attempting login for user '{self.username}' with GUID '{self.guid}'")
        headers = {
            "accept": "application/json",
            "Content-Type": "application/json",
            "GUID": self.guid,
        }
        login_data = UserLoginPublicDto(mail=self.username, password=self.password)

        try:
            # Use Pydantic's model_dump instead of to_dict
            response = requests.post(LOGIN_URL, headers=headers, json=login_data.model_dump())

            if response.status_code == 200:
                # Use Pydantic's model_validate instead of from_dict
                token_dto = JWTTokenPublicDto.model_validate(response.json())
                self._token_manager.set_token(token_dto)
                logger.info("Login successful. Token received.")
                return True
            else:
                logger.error(f"Login failed with status {response.status_code}: {response.text}")
                self._token_manager.clear_tokens()
                return False
        except requests.RequestException as e:
            logger.error(f"An error occurred during login request: {e}")
            self._token_manager.clear_tokens()
            return False

    def _refresh_token(self) -> bool:
        """
        Refreshes the access token using the stored refresh token.
        """
        refresh_token = self._token_manager.get_refresh_token()
        if not refresh_token:
            logger.debug("No refresh token available. Cannot refresh.")
            return False

        logger.debug("Attempting to refresh token.")
        headers = {"accept": "application/json", "Content-Type": "application/json"}
        refresh_data = RefreshTokenRequestDTO(refreshToken=refresh_token)

        try:
            # Use Pydantic's model_dump instead of to_dict
            response = requests.post(REFRESH_URL, headers=headers, json=refresh_data.model_dump())

            if response.status_code == 200:
                # Use Pydantic's model_validate instead of from_dict
                token_dto = JWTTokenPublicDto.model_validate(response.json())
                self._token_manager.set_token(token_dto)
                logger.info("Token refreshed successfully.")
                return True
            else:
                logger.warning(f"Refresh token failed with status {response.status_code}: {response.text}")
                # If refresh fails, the tokens are likely invalid.
                self._token_manager.clear_tokens()
                return False
        except requests.RequestException as e:
            logger.error(f"An error occurred during token refresh request: {e}")
            return False