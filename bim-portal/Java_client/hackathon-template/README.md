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

## 🏗️ CheckMate: check an IFC model against a LOIN

CheckMate asks the BIM Portal for a LOIN as buildingSMART IDS and checks an IFC model
against it. A bridge model is bundled inside the JAR, so there is no file to load.

### What you need

1. **The Apstex JARs.** Not redistributable, so not in the repo. Drop
   `com.apstex.ifctoolbox*.jar` and `com.apstex.stepparser*.jar` into
   `hackathon-template/lib/`. Without them the build excludes the whole `test/fx`
   package - no GUI and no LOIN check - and says so in a single warning line that is
   easy to miss. The `ids` package itself needs no IFC library and always compiles.
2. **A JDK 21 that bundles JavaFX** for the GUI, e.g. Zulu 21 FX. `runGui` checks this
   and names the problem. The headless check needs no JavaFX.
3. **No credentials.** The client falls back to anonymous access, and the LOINs used
   here are published ones. A `.env` only unlocks private content.

### Run it

```bash
# GUI: pick a LOIN on the left, hit "Validate LOIN", read the report at the bottom
./gradlew :hackathon-template:runGui

# Headless, for a container or a pipeline; the argument is the LOIN search term
./gradlew :hackathon-template:runLoinCheck --args="Kappe"

# Against your own model instead of the bundled one
CHECKMATE_IFC=/models/bruecke.ifc ./gradlew :hackathon-template:runLoinCheck --args="Schaft"
```

On Windows use `.\gradlew.bat`, and run both from `bim-portal/Java_client`.

### Reading the result

A specification selecting no element is reported as "nicht prüfbar" - never as a pass -
followed by a diagnosis: what each applicability facet matches on its own, and which
elements came closest, with the values that kept them out. Expect that case here: the
bundled TBW1 model classifies after Objektkatalog `Ingenieurbau_V005`, whose level
names differ from what the portal's LOINs ask for.

## 📁 What's Included

```
hackathon-template/
├── .env                              # Your credentials (create from template)
├── lib/                              # Apstex IFC Toolbox + STEP parser (not in the repo)
├── src/main/java/com/bimportal/hackathon/
│   ├── HackathonApplication.java     # 🎯 Main entry point - start here
│   ├── examples/
│   │   └── BasicExample.java        # 📚 Learn the API basics
│   └── ids/                         # ✅ LOIN check: read an IDS export, check a model
├── src/main/java/test/fx/           # 🖼️ CheckMate GUI (App) and headless CLI (LoinCheckCli)
├── src/main/resources/ifc/          # Bundled bridge model
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


