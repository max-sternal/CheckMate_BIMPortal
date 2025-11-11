"""
Authentication configuration module.

This module provides authentication-specific configuration,
importing base settings from the centralized config.
"""

import logging
from datetime import timedelta
import os

# Import centralized configuration
from config import BIMPortalConfig

# --- Logging Configuration ---
log_level = os.getenv("LOG_LEVEL", "INFO").upper()
logging.basicConfig(
    level=log_level,
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
)
logger = logging.getLogger("BimAuth")

# --- API Configuration (from centralized config) ---
LOGIN_URL = BIMPortalConfig.get_login_url()
REFRESH_URL = BIMPortalConfig.get_refresh_url()

# --- Token Management ---
TOKEN_REFRESH_MARGIN = timedelta(minutes=BIMPortalConfig.TOKEN_REFRESH_MARGIN_MINUTES)

# --- Credentials (from centralized config) ---
BIM_PORTAL_USERNAME_ENV_VAR = BIMPortalConfig.USERNAME_ENV_VAR
BIM_PORTAL_PASSWORD_ENV_VAR = BIMPortalConfig.PASSWORD_ENV_VAR

# --- Retry Logic (from centralized config) ---
AUTH_RETRY_LIMIT = BIMPortalConfig.AUTH_RETRY_LIMIT