import threading
from datetime import datetime, timezone
from typing import Optional, Union, Dict, Any
import jwt

# Import from your new Pydantic models instead of generated client
from models import JWTTokenPublicDto
from .auth_config import TOKEN_REFRESH_MARGIN, logger

class TokenManager:
    """
    Manages the storage and lifecycle of authentication tokens in a thread-safe manner.
    Adapted to work with Pydantic models.
    """

    def __init__(self):
        self._access_token: Optional[str] = None
        self._refresh_token: Optional[str] = None
        self._expires_at: Optional[datetime] = None
        self._lock = threading.Lock()

    def set_token(self, token_data: Union[JWTTokenPublicDto, Dict[str, Any]]) -> None:
        """
        Sets the access and refresh tokens from various response types.

        This method can handle:
        1. A JWTTokenPublicDto object from Pydantic models.
        2. A simple dictionary with a 'token' key (from the old client).
        3. A dictionary with 'token', 'refreshToken', and 'validTill' keys.
        """
        with self._lock:
            logger.debug("Setting new token in TokenManager.")
            if isinstance(token_data, JWTTokenPublicDto):
                self._access_token = token_data.token if token_data.token is not None else None
                self._refresh_token = token_data.refreshToken if token_data.refreshToken is not None else None
                # The DTO provides a datetime object directly
                self._expires_at = token_data.validTill if token_data.validTill is not None else None
            elif isinstance(token_data, dict):
                self._access_token = token_data.get("token")
                self._refresh_token = token_data.get("refreshToken")

                # Handle expiration
                if "validTill" in token_data and token_data["validTill"]:
                    # Parse ISO format string and ensure timezone-aware
                    expires_str = token_data["validTill"]
                    if expires_str.endswith('Z'):
                        expires_str = expires_str[:-1] + '+00:00'
                    self._expires_at = datetime.fromisoformat(expires_str)
                    # Ensure timezone-aware
                    if self._expires_at.tzinfo is None:
                        self._expires_at = self._expires_at.replace(tzinfo=timezone.utc)
                elif self._access_token:
                    # Fallback to decoding the JWT to get the 'exp' claim
                    try:
                        decoded_token = jwt.decode(self._access_token, options={"verify_signature": False})
                        exp = decoded_token.get("exp")
                        if exp:
                            self._expires_at = datetime.fromtimestamp(exp, tz=timezone.utc)
                    except jwt.PyJWTError as e:
                        logger.error(f"Failed to decode JWT to get expiration: {e}")
                        self._expires_at = None
            else:
                logger.error(f"Unsupported token data type: {type(token_data)}")
                self._clear_tokens()

            if self._access_token:
                 logger.debug(f"Token set successfully. Expiration: {self._expires_at}")
            else:
                 logger.warning("Attempted to set token, but no access token was found in the provided data.")

    def get_access_token(self) -> Optional[str]:
        """Returns the current access token."""
        with self._lock:
            return self._access_token

    def get_refresh_token(self) -> Optional[str]:
        """Returns the current refresh token."""
        with self._lock:
            return self._refresh_token

    def is_token_expiring(self) -> bool:
        """
        Checks if the access token is missing, expired, or about to expire.
        """
        with self._lock:
            if not self._access_token or not self._expires_at:
                logger.debug("Token is considered expiring because it's missing or has no expiration.")
                return True

            # Ensure timezone-aware comparison
            now = datetime.now(timezone.utc)
            
            # Ensure expires_at is timezone-aware
            if self._expires_at.tzinfo is None:
                self._expires_at = self._expires_at.replace(tzinfo=timezone.utc)

            is_expiring = self._expires_at <= (now + TOKEN_REFRESH_MARGIN)
            if is_expiring:
                logger.debug(f"Token is expiring. Expiration: {self._expires_at}, Now: {now}")
            return is_expiring

    def clear_tokens(self) -> None:
        """Clears all stored token data."""
        with self._lock:
            logger.debug("Clearing all tokens.")
            self._access_token = None
            self._refresh_token = None
            self._expires_at = None

    def _clear_tokens(self) -> None:
        """Internal method to clear tokens (called by set_token on error)."""
        self.clear_tokens()