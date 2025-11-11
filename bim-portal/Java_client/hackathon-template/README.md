# 🚀 BIM Portal Hackathon Template

Ready-to-use template for building innovative solutions with the BIM Portal API.

## ⚡ Quick Start

### 1. Setup Credentials
```bash
cp .env_example .env
# Edit .env with your BIM Portal credentials
```

### 2. Run Examples
```bash
# From project root
./gradlew :hackathon-template:runBasicExample    # Learn the API
./gradlew :hackathon-template:run                # Start building
```

## 📁 What's Included

```
hackathon-template/
├── .env                              # Your credentials (create from template)
├── src/main/java/com/bimportal/hackathon/
│   ├── HackathonApplication.java     # 🎯 Main entry point - start here
│   └── examples/
│       └── BasicExample.java        # 📚 Learn the API basics
└── README.md                        # This file
```

## 🛠️ Building Your Solution

### Start with the Main Application
```java
// HackathonApplication.java - Your innovation starts here
public class HackathonApplication {
    public static void main(String[] args) {
        EnhancedBimPortalClient client = BimPortalClientBuilder.buildDefault();

        // 🚀 Build your innovative solution here!

        client.logout();
    }
}
```

### Learn from the Basic Example
```bash
./gradlew :hackathon-template:runBasicExample
```
Shows you how to:
- Search projects and LOINs
- Retrieve detailed information
- Export data in different formats
- Handle authentication

## 🔧 Core API Usage

```java
// Initialize client
EnhancedBimPortalClient client = BimPortalClientBuilder.buildDefault();

// Search for data
List<SimpleAiaProjectPublicDto> projects = client.searchProjects();
List<SimpleLoinPublicDto> loins = client.searchLoins();
List<SimpleDomainSpecificModelPublicDto> domainModels = client.searchDomainModels();

// Export data
Optional<byte[]> pdfContent = client.exportProjectPdf(projectGuid);
Optional<byte[]> odtContent = client.exportProjectOpenOffice(projectGuid);

// Always clean up
client.logout();
```

## 🆘 Need Help?

1. **API Issues**: Check your `.env` file credentials
2. **Build Problems**:
- Make sure to not use sudo when using Gradle
- Run `./gradlew clean build`
4. **Examples**: Study `BasicExample.java` for patterns
5. **Documentation**: Check the enhanced client in `../bim-portal-enhanced/`
6. **Java Version Issues?**
- Use `java -version` and `javac -version` to verify the versions, they should both be 21
- In IntelliJ, check the Project and the Modules tab under `File -> Project Structure` and verify the Java versions there
- In the console, run an export `export JAVA_HOME=/path/to/jdk-21` that points to the directory of the Java SDK
- Open a different console

---

**Happy Hacking! 🎯 Build something amazing!**
**Questions?** Check the [main project README](../README.md) for project overview.
