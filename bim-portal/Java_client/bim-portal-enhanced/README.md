# BIM Portal Enhanced Client 🏗️

**High-level Java client for the BIM Portal Hackathon 2025 - Get building with German BIM data in minutes!**

This enhanced wrapper eliminates API complexity, providing automatic authentication, simplified data access, and ready-to-use export capabilities for Germany's central BIM platform.

## ⚡ Hackathon Quick Start

### 1. Set Credentials
Create `.env` file here:
```properties
BIM_PORTAL_USERNAME=your_email
BIM_PORTAL_PASSWORD=your_password
```

### 2. See It Work
```bash
# From project root - full feature showcase
./gradlew runAllExamples

# Basic demo
./gradlew :bim-portal-enhanced:quickStart
```

### 3. Start Building
```java
// One-liner setup
var client = BimPortalClientBuilder.buildDefault();

// Access German BIM data
var projects = client.searchProjects();
var loins = client.searchLoins();
var domainModels = client.searchDomainModels();

// Export in multiple formats
var pdf = client.exportProjectPdf(projectGuid);
var xml = client.exportProjectLoinXml(projectGuid);
```

## 🎯 What You Get for Your Hackathon

### 🔍 **Search & Access BIM Data**
- **Projects**: AIA projects with BIM requirements
- **LOINs**: Level of Information Need definitions
- **Domain Models**: Building, infrastructure, transport models
- **Properties**: Classification systems and attributes

### 📁 **Export in Professional Formats**
- **PDF**: Human-readable reports
- **OpenOffice**: Editable documents
- **OKSTRA**: German infrastructure standard
- **LOIN-XML**: Machine-readable requirements
- **IDS**: Information Delivery Specification

## 🚀 Individual Example Commands

Run focused examples for specific BIM entity types:

```bash
# Export projects in all available formats
./gradlew exportProjects

# Export LOINs in all available formats
./gradlew exportLoins

# Export domain models
./gradlew exportDomainModels

# Export context information
./gradlew exportContextInfo

# Export AIA templates
./gradlew exportTemplates

# Run batch export (multiple items as PDF)
./gradlew batchExport

# Run search and filter examples
./gradlew searchExamples

# Run Organization Examples
./gradlew organizationExamples
```
The results are saved in the `exports` directory by default.

### Quick Testing Commands
```bash
# Test your setup
./gradlew :bim-portal-enhanced:healthCheck

# Check available API methods
./gradlew :bim-portal-enhanced:checkApiMethods
```

## 📚 Example Classes Reference

| Example Class | Perfect For | Command | What You'll Learn |
|---------------|-------------|---------|-------------------|
| `QuickStart.java` | First-time setup | `./gradlew :bim-portal-enhanced:quickStart` | Basic API usage, authentication |
| `ProjectExportExample.java` | Project data | `./gradlew exportProjects` | Project exports in all formats |
| `LoinExportExample.java` | Requirements data | `./gradlew exportLoins` | LOIN exports and processing |
| `DomainModelExportExample.java` | Domain models | `./gradlew exportDomainModels` | Domain-specific exports |
| `ContextInfoExportExample.java` | Context data | `./gradlew exportContextInfo` | Context information handling |
| `TemplateExportExample.java` | AIA templates | `./gradlew exportTemplates` | Template processing |
| `BatchExportExample.java` | Bulk operations | `./gradlew batchExport` | Batch processing patterns |
| `SearchExample.java` | Data discovery | `./gradlew searchExamples` | Search and filtering |
| `HealthCheck.java` | Troubleshooting | `./gradlew :bim-portal-enhanced:healthCheck` | API connectivity, credentials |

## 🔥 Copy-Paste Code Snippets

### Get All Available Data
```java
var client = BimPortalClientBuilder.buildDefault();

// Search everything
var projects = client.searchProjects();
var loins = client.searchLoins();
var domainModels = client.searchDomainModels();
var properties = client.searchProperties();
```

### Export Everything
```java
// Find a good project
var project = client.findExportableProject().orElseThrow();

// Export all formats
var pdf = client.exportProjectPdf(project.getGuid());
var odt = client.exportProjectOpenOffice(project.getGuid());
var xml = client.exportProjectLoinXml(project.getGuid());
```

### Search with Filters
```java
AiaProjectForPublicRequest request = new AiaProjectForPublicRequest();
request.setSearchString("bridge");
var bridgeProjects = client.searchProjects(request);
```

### Batch Processing
```java
var projects = client.searchProjects();
for (var project : projects.subList(0, Math.min(5, projects.size()))) {
    var pdf = client.exportProjectPdf(project.getGuid());
    if (pdf.isPresent()) {
        ExportUtils.saveExportFile(pdf.get(),
            "project_" + project.getGuid() + ".pdf");
    }
}
```

## 🔧 Advanced Configuration

### Custom Client Builder
```java
var client = new BimPortalClientBuilder()
    .withBaseUrl("https://test.via.bund.de/bim")
    .withCredentials("username", "password")
    .withTimeout(60)
    .withMaxRetries(5)
    .withExportDirectory("custom-exports")
    .build();
```

### Optional .env Settings
```properties
# Optional configuration
BIM_PORTAL_BASE_URL=https://via.bund.de/bmdv/bim-portal/edu/bim
EXPORT_DIRECTORY=my-exports
LOG_LEVEL=DEBUG  # For troubleshooting
REQUEST_TIMEOUT=30
MAX_RETRIES=3
```

### Direct API Access
```java
// Access raw generated APIs for advanced features
var client = BimPortalClientBuilder.buildDefault();
var projectsApi = client.getProjectsApi();
var loinApi = client.getLoinApi();
```

## 🆘 Hackathon Troubleshooting

**Authentication Issues?**
```bash
./gradlew :bim-portal-enhanced:healthCheck
```

**Export Not Working?**
```java
// Use this to find exportable projects
var exportableProject = client.findExportableProject();
```

**"Authentication failed"**
- Check your credentials in `.env`
- Verify email is confirmed in BIM Portal web interface

**Need Debug Info?**
```properties
# Add to .env
LOG_LEVEL=DEBUG
```

## 🏆 Success Pattern

1. **Start with health check**: `./gradlew :bim-portal-enhanced:healthCheck`
2. **Run full demo**: `./gradlew runAllExamples`
3. **Try individual examples** that match your hackathon idea
4. **Copy example code** and adapt for your project
5. **Iterate and build** your awesome BIM solution!

---

**Ready to hack? Start with `./gradlew runAllExamples` to see everything! 🚀**

**Questions?** Check the [main project README](../README.md) for project overview.
