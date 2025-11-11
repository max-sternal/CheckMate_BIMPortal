# BIM Portal Java Client 🚀

**Ready-to-use Java client for the German BIM Portal API - Perfect for the [BIM Portal Hackathon 2025](https://www.bimdeutschland.de/veranstaltungen/hackathon-2025)!**

The BIM Portal is Germany's central platform for Building Information Modeling (BIM) data, providing standardized access to AIA projects, LOINs (Level of Information Need), domain models, and BIM requirements. This enhanced Java client eliminates the complexity of direct API integration, offering automatic authentication, simplified data access, and ready-to-use export capabilities.


## 📋 Table of Contents
- [Quick Start](#-quick-start--5-minutes)
- [What You Get](#-what-you-get)
- [Hackathon Usage](#-hackathon-usage)
- [Key Commands](#-key-commands)
- [Project Structure](#-project-structure)
- [Troubleshooting](#-troubleshooting)
- [Learn More](#-learn-more)
- [Official Resources](#-official-resources)

## ⚡ Quick Start (< 5 minutes)

### 1. Prerequisites
- Java 21+
- Git

### 2. Get Running
```bash
cd bim-portal-client-suite
./gradlew build
```

### 3. Set Credentials
After setting up an account on the [BIM Portal](https://via.bund.de/bmdv/bim-portal/edu/bim), add your credentials as follows:

**For Hackathon Participants** (Recommended):
```bash
cp .env_example .env
# Edit .env with your credentials
```

**For Enhanced Client Examples**:
Create `.env` file in `bim-portal-enhanced/`:
```properties
BIM_PORTAL_USERNAME=your_email
BIM_PORTAL_PASSWORD=your_password
```

### 4. See It Work

**Start Hacking (Recommended for Hackathon)**:
```bash
./gradlew :hackathon-template:run
```

**Full Feature Showcase**:
```bash
./gradlew :bim-portal-enhanced:quickStart
./gradlew runAllExamples
```

## 🎯 What You Get

### 🏆 Hackathon Template
Ready-to-code template with:
- ✅ Quick setup in 2 minutes
- ✅ Basic examples to learn from
- ✅ Main application to build in
- ✅ Clean, focused structure for rapid development

### 🛠️ Enhanced Java Client
Production-ready client that handles all the complexity:
- ✅ Automatic authentication & token management
- ✅ Search projects, LOINs, domain models, properties
- ✅ Export to PDF, OpenOffice, OKSTRA, LOIN-XML, IDS
- ✅ Comprehensive examples
- ✅ Error handling & health checks

## 🎖️ Hackathon Usage

### Simple API Access
```java
// One-liner setup
var client = BimPortalClientBuilder.buildDefault();

// Search & export projects
var projects = client.searchProjects();
var exportableProject = client.findExportableProject();
if (exportableProject.isPresent()) {
    var pdf = client.exportProjectPdf(exportableProject.get().getGuid());
    // Use your PDF data!
}
```

### Available Data & Exports

**Search & Access:**
- 📋 Projects (AIA projects with BIM requirements)
- 🔗 LOINs (Level of Information Need)
- 🗏️ Domain Models (Building, Infrastructure, etc.)
- 🏷️ Properties & Classifications

**Export Formats:**
- 📄 PDF, 📝 OpenOffice, 🗏️ OKSTRA, 📋 LOIN-XML, 🏷️ IDS (in case of several files they are zipped)

## 🛠️ Key Commands

### Hackathon Development
```bash
# Start building your solution
./gradlew :hackathon-template:run
```

### Enhanced Client Examples
```bash
# Health check (verify API access)
./gradlew :bim-portal-enhanced:healthCheck

# Run comprehensive examples
./gradlew runAllExamples

# Explore available API methods
./gradlew :bim-portal-enhanced:checkApiMethods
```

## 📁 Project Structure

```
bim-portal-client-suite/
├── hackathon-template/          # 🎯 START HERE for hackathon
│   ├── .env                     # Your credentials
│   ├── src/.../hackathon/
│   │   ├── HackathonApplication.java  # Main entry point
│   │   └── examples/
│   │       └── BasicExample.java     # Learn the API
│   └── README.md                # Hackathon guide
├── bim-portal-enhanced/         # 🛠️ examples & features
│   ├── src/.../examples/        # Comprehensive examples
│   ├── exports/                 # Exported files
│   └── .env                     # Enhanced client credentials
└── openapi-generated/           # Raw API client (auto-generated)
```

## 🚨 Troubleshooting

**Authentication Issues?**
```bash
# Check hackathon template
./gradlew :hackathon-template:run

# Or check enhanced client
./gradlew :bim-portal-enhanced:healthCheck
```

**Need Fresh Build?**
```bash
./gradlew clean build
```

**Hackathon Template Issues?**
- Ensure `.env` file is in `hackathon-template/` directory
- Verify credentials are correct
- Check `hackathon-template/README.md` for detailed setup

**Java Version / Build Issues?**
- Make sure to not use sudo when using Gradle
- Use `java -version` and `javac -version` to verify the versions, they should both be 21
- In IntelliJ, check the Project and the Modules tab under `File -> Project Structure` and verify the Java versions there
- In the console, run an export `export JAVA_HOME=/path/to/jdk-21` that points to the directory of the Java SDK
- Open a different console

## 📖 Learn More

- Check `hackathon-template/README.md` for focused hackathon guide
- Study `hackathon-template/src/.../examples/BasicExample.java`
- Start coding in `HackathonApplication.java`

- Explore `bim-portal-enhanced/src/.../examples/` for comprehensive examples
- Check the [bim-portal-enhanced/README](./bim-portal-enhanced/README.md) for detailed instructions
- Review individual example commands for specific use cases


---

## 📗 Official Resources
- **BIM Portal (EDU)**: [via.bund.de/bmdv/bim-portal/edu/bim](https://via.bund.de/bmdv/bim-portal/edu/bim)
- **API Documentation (Swagger UI)**: [bimdeutschland.github.io/BIM-Portal-REST-API-Dokumentation](https://bimdeutschland.github.io/BIM-Portal-REST-API-Dokumentation/)
- **OpenAPI Spec**: [github.com/bimdeutschland/BIM-Portal-REST-API-Dokumentation](https://github.com/bimdeutschland/BIM-Portal-REST-API-Dokumentation)
