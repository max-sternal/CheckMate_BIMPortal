# Copilot instructions for CheckMate BIM Portal

## Repository shape

The implementation lives under `bim-portal/`. It contains two API-client
implementations for the German BIM Portal:

- `bim-portal/Python_client/` is the rapid-prototyping client. Start with
  `hackathon_interface.py` and use `BIMPortal` for the simplified workflow.
  `enhanced_bim_client.py` contains the lower-level endpoint and export
  operations, while `models.py` contains the Pydantic request/response models.
- `bim-portal/Java_client/` is a Gradle multi-project build targeting Java 21.
  `openapi-generated/` is the low-level Feign/OpenAPI client; do not hand-edit
  generated API/model classes unless the change is specifically a generated
  source repair. `bim-portal-enhanced/` wraps that client with authentication,
  configuration, search, and export behavior. `hackathon-template/` is the
  consumer-facing example application and depends on the enhanced module.

The Java dependency direction is:

`hackathon-template` -> `bim-portal-enhanced` -> `openapi-generated`.

The clients call the BIM Portal REST API, authenticate through the
`InfrastrukturApi` login/refresh endpoints, and expose searches for projects,
LOINs, domain models, context information, properties, and classifications.
Export operations write returned PDF, OpenOffice, OKSTRA, LOIN-XML, and IDS
data to the configured export directory.

## Build, test, and formatting commands

Run Java commands from `bim-portal/Java_client/`. On Windows use
`gradlew.bat`; on Linux/macOS use `./gradlew`.

```powershell
# Compile and run all Java tests
.\gradlew.bat build

# Clean rebuild
.\gradlew.bat clean build

# Run all tests in one module
.\gradlew.bat :bim-portal-enhanced:test
.\gradlew.bat :hackathon-template:test
.\gradlew.bat :openapi-generated:test

# Run one test class or one test method
.\gradlew.bat :bim-portal-enhanced:test --tests "com.pb40.bimportal.config.ConfigLoaderTest"
.\gradlew.bat :hackathon-template:test --tests "com.bimportal.hackathon.examples.BasicExampleIntegrationTest.someTestMethod"

# Check or apply the repository's Spotless formatting
.\gradlew.bat spotlessCheck
.\gradlew.bat spotlessApply
```

The Java build uses JUnit Platform and Java 21. The root Gradle build applies
Spotless to Java, Gradle Kotlin scripts, Markdown, YAML, and properties files.
The `openapi-generated` compile task runs its `fixGeneratedCode` task first;
that task repairs known generated-source imports and annotations.

Useful Java example/diagnostic tasks include:

```powershell
.\gradlew.bat :bim-portal-enhanced:healthCheck
.\gradlew.bat :bim-portal-enhanced:quickStart
.\gradlew.bat runAllExamples
.\gradlew.bat :hackathon-template:runBasicExample
```

These example tasks make network requests and require valid credentials where
the selected API operation is authenticated. They may create files under the
module's `exports/` directory.

Set up the Python client from `bim-portal/Python_client/`:

```powershell
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
python demo/credentials_setup.py
python -m demo.health_check
```

Conda is also supported through `environment.yml` (`bim-portal-hackathon`,
Python 3.12). There is currently no repository-configured Python test or lint
command; validate Python changes with the relevant demo/example execution and
targeted imports or scripts. No Python test suite is checked in.

## Configuration and credentials

Never commit credentials. Copy the relevant `env.example`/`.env_example` to a
local `.env` and set `BIM_PORTAL_USERNAME` and `BIM_PORTAL_PASSWORD`.

Python loads `.env` from `bim-portal/Python_client/`. Its centralized defaults
and runtime overrides are in `config.py`, including `REQUEST_TIMEOUT`,
`VERIFY_SSL`, `LOG_LEVEL`, `EXPORT_DIRECTORY`, and `MAX_RETRIES`.

The Java enhanced and hackathon modules search for `.env` in the current
module, parent modules, and Java client root. Gradle passes loaded values both
as environment variables and Java system properties to `JavaExec` tasks.
Important settings include `BIM_PORTAL_USERNAME`, `BIM_PORTAL_PASSWORD`,
`BIM_PORTAL_BASE_URL`, `EXPORT_DIRECTORY`, `LOG_LEVEL`, `REQUEST_TIMEOUT`, and
`MAX_RETRIES`.

## Code conventions and integration boundaries

- Prefer the high-level client APIs and builders over constructing generated
  Feign APIs directly. In Java, use `BimPortalClientBuilder` and
  `EnhancedBimPortalClient`; in Python, use `BIMPortal` unless lower-level
  behavior is required.
- Keep authentication concerns in the existing auth/configuration layers
  (`auth/` and `config` packages). Preserve token refresh and retry behavior
  when changing request code.
- Reuse the existing model types and API method naming rather than passing
  untyped dictionaries or duplicating endpoint wrappers.
- Match the established split between search/access methods and export
  methods. Export helpers should continue to use the configured export
  directory and retain the API's returned format/extension.
- Java examples are executable Gradle `JavaExec` tasks registered in the
  enhanced module. When adding an example, wire its task and main class into
  the Gradle build rather than relying on an undocumented manual invocation.
- Keep generated OpenAPI changes reproducible. If the API specification or
  generator output changes, update the generated module consistently and
  retain the `fixGeneratedCode` compile prerequisite.
- Java source is formatted with Google Java Format through Spotless, uses
  UTF-8, and compiles with unchecked/deprecation warnings enabled. Preserve
  JUnit 5 conventions in `src/test/java`.
- Use the existing environment-variable pattern for runtime configuration;
  do not hard-code secrets, user-specific paths, or credentials into examples
  or tests.

## Relevant project documentation

Use the module READMEs for user-facing workflows:

- `bim-portal/README.md` gives the project overview and client choice.
- `bim-portal/Java_client/README.md` documents the Java modules and commands.
- `bim-portal/Java_client/bim-portal-enhanced/README.md` documents the
  available search/export examples.
- `bim-portal/Python_client/README.md` documents Python setup and basic usage.
