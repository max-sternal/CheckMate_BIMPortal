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

1. **JDK 25.** Enough to build and run the pure-Java IFC reader (`com.bimportal.hackathon.ifc`),
   the headless CLI (`runLoinCheck`), the MCP server (`installMcp`), and all tests — no Apstex
   JARs or external IFC runtime needed.
2. **A JDK 25 that bundles JavaFX** to *run* the GUI, e.g. Zulu 25 FX. Compiling it needs no such
   JDK: `test/fx` is built against the `org.openjfx` artifacts (`compileOnly`), and `runGui` checks
   the runtime JDK for JavaFX and names the problem when it is missing.
3. **No credentials.** The client falls back to anonymous access, and the LOINs used
   here are published ones. A `.env` only unlocks private content.
4. **`ANTHROPIC_API_KEY`, optional**, in the environment or in `.env`, only for the chat in the GUI
   (`com.bimportal.hackathon.chat`). Without it the chat stays inactive and says so; the GUI, the
   CLI and the MCP server are unaffected. Requests with this key are billed per token, unlike the
   MCP server, which runs inside the user's own AI app. `CHECKMATE_LLM_MODEL` and
   `CHECKMATE_LLM_BASE_URL` choose the model and the endpoint, so a gateway such as OpenRouter can
   answer with the models of other vendors; see `.env_example`.

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
├── src/main/java/com/bimportal/hackathon/
│   ├── HackathonApplication.java     # 🎯 Main entry point - start here
│   ├── catalogue/                    # 🔎 BIM Portal LOIN catalogue filter & fit ranking
│   ├── check/                        # ⚙️ Unified CheckService shared by all front ends
│   ├── cli/                          # 💻 Headless CLI (LoinCheckCli)
│   ├── examples/
│   │   └── BasicExample.java         # 📚 Learn the API basics
│   ├── ids/                          # ✅ IDS 1.0 parser, archive & validator
│   ├── ifc/                          # 🧱 Pure-Java STEP/IFC reader (IFC2X3, IFC4, IFC4X3)
│   ├── mcp/                          # 🤖 MCP server for AI apps (CheckMateMcpServer)
│   └── report/                       # 📝 Text & JSON check reports
├── src/main/java/test/fx/            # 🖼️ CheckMate JavaFX GUI (App, ElementUI, RequirementSource)
├── src/main/resources/ifc/           # Bundled bridge model & IFC schema tables
└── README.md                         # This file
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
