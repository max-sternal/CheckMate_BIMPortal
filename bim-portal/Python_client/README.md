# BIM Portal API - Hackathon Interface

A simplified Python interface to access the German BIM Portal API for hackathons and rapid prototyping.

## Setup

This project requires Python 3.12 or higher. All exports will be saved to the `exports/` directory.

### Recommended: `venv` and `pip`

This is the standard, lightweight way to set up a Python project.

**Linux/macOS:**
```bash
# Create a virtual environment
python3 -m venv .venv

# Activate it
source .venv/bin/activate

# Install dependencies
pip install -r requirements.txt
```

**Windows:**
```powershell
# Create a virtual environment
python -m venv .venv

# Activate it
.venv\Scripts\Activate.ps1

# Install dependencies
pip install -r requirements.txt
```

### Alternative: `conda`

If you prefer using Conda, you can use the provided `environment.yml` file.

**Linux/macOS/Windows:**
```bash
# Create the environment from the file
conda env create -f environment.yml

# Activate the environment
conda activate bim-portal-hackathon
```

## Quick Start

### 1. Setup Credentials
```bash
python demo/credentials_setup.py
```
Enter your BIM Portal username and password when prompted.

### 2. Basic Usage
```python
from hackathon_interface import BIMPortal

# Initialize
bim = BIMPortal()

# Search for projects
projects = bim.get_projects(search="Beispiel")
print(f"Found {len(projects)} projects")

# Get project details
if projects:
    project = bim.get_project_details(projects[0].guid)
    print(f"Project: {project.name}")
    
    # Export as PDF
    pdf_path = bim.export_project_pdf(projects[0].guid)
    print(f"Exported to: {pdf_path}")
```

### 3. Health Check
```bash
python -m demo.health_check
```

## Advanced Usage

### Using the Raw Enhanced Client
If you need more control, access the underlying client:
```python
bim = BIMPortal()
raw_client = bim.client

# Use any method from enhanced_bim_client.py
projects = raw_client.search_projects()
properties = raw_client.search_properties()
```

## File Structure
```
BIM_Portal_API_Interfaces/
├── hackathon_interface.py    # Main interface (start here!)
├── models.py                 # Data models (for Data Validation, Type Safety and Serialization -  conversion between Python objects and JSON for API requests/responses)
├── enhanced_bim_client.py    # Advanced client with all the available endpoints
├── demo/
│   ├── credentials_setup.py  # Setup credentials
│   ├── health_check.py       # Test connection
│   └── authenticated_cli.py  # Command-line tool
├── examples/                 # More examples
└── exports/                  # Downloaded files go here
```

## Troubleshooting

### Credentials Issues
- Run `python demo/credentials_setup.py` to reset credentials
- Check that `.env` file exists in project root
- Verify credentials work at: https://via.bund.de/bim

### API Issues
- Run `python -m demo.health_check` for diagnostics
- Check internet connection
- Some resources may require special permissions

### Common Errors
- `FileNotFoundError`: Run credentials setup first
- `AuthenticationError`: Check username/password
- `Empty results`: Try broader search terms


## Support

- Check `examples/` folder for more detailed examples
- Run `python hackathon_interface.py` for a quick demo
- Use `bim.health_check()` to test your setup