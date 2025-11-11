import os
import getpass

ENV_FILE = ".env"
USERNAME_VAR = "BIM_PORTAL_USERNAME"
PASSWORD_VAR = "BIM_PORTAL_PASSWORD"

def main():
    """
    An interactive script to help users set up their credentials in a .env file.
    """
    print("--- BIM Portal API Credentials Setup ---")

    if os.path.exists(ENV_FILE):
        print(f"A '{ENV_FILE}' file already exists in this directory.")
        overwrite = input("Do you want to overwrite it? (y/n): ").lower()
        if overwrite != 'y':
            print("Setup cancelled.")
            return

    print("\nPlease enter your BIM Portal credentials.")
    username = input("Username (email): ")
    password = getpass.getpass("Password (input will be hidden): ")

    if not username or not password:
        print("\nUsername and password cannot be empty. Setup failed.")
        return

    try:
        with open(ENV_FILE, "w") as f:
            f.write(f"{USERNAME_VAR}='{username}'\n")
            f.write(f"{PASSWORD_VAR}='{password}'\n")
        print(f"\n✅ Success! Credentials have been saved to '{ENV_FILE}'.")
        print("You can now run the examples and the demo application.")
    except IOError as e:
        print(f"\n❌ Error: Could not write to file '{ENV_FILE}'.")
        print(f"Please check file permissions. Error: {e}")

if __name__ == "__main__":
    main()
