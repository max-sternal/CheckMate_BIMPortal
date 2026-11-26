import java.net.URI
import java.util.Properties

plugins {
    java
    application
    id("org.springframework.boot") version "3.4.0"
    id("io.spring.dependency-management") version "1.1.6"
}

group = "com.bimportal.hackathon"
version = "1.0.0"

java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

// Spring Boot 3.4 BOM pins ByteBuddy 1.15.x, which predates Java 25 (classfile version 69).
extra["byte-buddy.version"] = "1.17.5"
extra["mockito.version"] = "5.18.0"
// ... and jackson-bom 2.18.1, which would drag jackson-annotations back to 2.18.1 while
// databind stays 2.22.4. In 2.22 JsonProperty.isRequired() returns OptBoolean, not boolean,
// so the mixed pair fails at runtime with NoSuchMethodError.
extra["jackson-bom.version"] = "2.22.4"

repositories {
    mavenCentral()
}

// The CheckMate GUI (test.fx) uses JavaFX. JDK-FX builds (e.g. Zulu/Liberica 25 FX) ship it as
// platform modules, plain JDKs omit it - so the GUI compiles against the org.openjfx artifacts,
// which resolve on any JDK. They are compileOnly: at run time the JDK's own modules are used, and
// runGui checks that they are there.
//
// It used to exclude test/fx from the source set instead when the running JDK had no JavaFX. That
// made the build depend on who started it: an editor whose Java extension runs Gradle in its own
// bundled JRE (Red Hat's has no JavaFX) dropped the GUI and deleted its classes from the shared
// build directory, and the next launch failed with ClassNotFoundException: test.fx.App.
val javaFxVersion = "25"

val javaFxPlatform =
    with(System.getProperty("os.name").lowercase()) {
        val aarch64 = System.getProperty("os.arch").lowercase() == "aarch64"
        when {
            contains("win") -> "win"
            contains("mac") -> if (aarch64) "mac-aarch64" else "mac"
            else -> if (aarch64) "linux-aarch64" else "linux"
        }
    }

dependencies {
    // Dependency on the BIM Portal enhanced client
    implementation(project(":bim-portal-enhanced"))

    // The generated portal client, used directly (com.bimportal.client.model.*, .api.*) in the
    // catalogue, the IDS download and the examples. bim-portal-enhanced depends on it with
    // `implementation`, so it is not passed on - this module compiled only because that project's
    // jar used to bundle its dependencies.
    implementation(project(":openapi-generated"))

    // Spring Boot starters
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-json")

    // Utilities
    implementation("org.apache.commons:commons-lang3:3.12.0")
    implementation("org.apache.commons:commons-csv:1.10.0")
    implementation("com.google.guava:guava:32.1.2-jre")

    // JSON
    implementation("com.fasterxml.jackson.core:jackson-databind:2.22.4")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.22.4")

    // MCP server (com.bimportal.hackathon.mcp): official MCP Java SDK 2.2.0 with Jackson 2 binding.
    implementation("io.modelcontextprotocol.sdk:mcp-core:2.2.0")
    implementation("io.modelcontextprotocol.sdk:mcp-json-jackson2:2.2.0")

    // LLM chat inside the GUI (com.bimportal.hackathon.chat): official Anthropic Java SDK.
    implementation("com.anthropic:anthropic-java:3.3.0")

    // JavaFX for the GUI (test.fx), to compile against only - see the note below the dependencies.
    compileOnly("org.openjfx:javafx-base:$javaFxVersion:$javaFxPlatform")
    compileOnly("org.openjfx:javafx-graphics:$javaFxVersion:$javaFxPlatform")
    compileOnly("org.openjfx:javafx-controls:$javaFxVersion:$javaFxPlatform")

    // Logging
    implementation("org.slf4j:slf4j-api:2.0.9")
    implementation("ch.qos.logback:logback-classic:1.4.11")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.mockito:mockito-core:5.18.0")
    testImplementation("org.assertj:assertj-core:3.24.2")
}

application {
    mainClass.set("com.bimportal.hackathon.HackathonApplication")
}


// --- Helper to load .env ---
fun loadEnvFile(): Map<String, String> {
    val envProps = mutableMapOf<String, String>()
    val searchPaths = listOf(
        file(".env"),
        file("../.env"),
        file("../../.env"),
        rootProject.file(".env")
    )

    val envFile = searchPaths.firstOrNull { it.exists() }

    if (envFile != null) {
        envFile.readLines().forEach { line ->
            val cleanLine = line.trim()
            if (cleanLine.isNotEmpty() && !cleanLine.startsWith("#") && cleanLine.contains("=")) {
                val parts = cleanLine.split("=", limit = 2)
                if (parts.size == 2) {
                    val key = parts[0].trim()
                    var value = parts[1].trim()
                    // Strip inline comments if not quoted
                    if (!value.startsWith("\"") && !value.startsWith("'")) {
                        value = value.split("#", limit = 2)[0].trim()
                    }
                    value = value.removeSurrounding("\"").removeSurrounding("'")
                    envProps[key] = value
                }
            }
        }
        println("✅ Loaded ${envProps.size} environment variables from ${envFile.name}")
        envProps["LOG_LEVEL"]?.let { println("   LOG_LEVEL: $it") }
    } else {
        println("❌ No .env file found in search paths")
    }

    return envProps
}

val envProperties by lazy { loadEnvFile() }

// --- Configure JavaExec tasks ---
tasks.withType<JavaExec> {
    val logLevel = envProperties["LOG_LEVEL"] ?: "INFO"

    // System properties for logging
    systemProperty("LOG_LEVEL", logLevel)
    systemProperty("logging.level.root", logLevel)
    systemProperty("logging.level.com.bimportal.hackathon", logLevel)

    // Propagate all .env vars
    envProperties.forEach { (key, value) ->
        environment(key, value)    // available via System.getenv()
        systemProperty(key, value) // available via System.getProperty()
    }
}

// --- Custom tasks ---
tasks.register<JavaExec>("runBasicExample") {
    group = "hackathon"
    description = "Run basic BIM Portal API example"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("com.bimportal.hackathon.examples.BasicExample")
}

// MCP server: an AI app starts it as a program of its own and talks to it over stdin/stdout,
// so it can't run through Gradle, which writes to stdout too. installMcp puts a start script
// and every JAR it needs into build/install/checkmate-mcp/ (docs/WORKFLOW.md, section 2.7).
val mcpStartScripts = tasks.register<CreateStartScripts>("mcpStartScripts") {
    applicationName = "checkmate-mcp"
    mainClass.set("com.bimportal.hackathon.mcp.CheckMateMcpServer")
    outputDir = layout.buildDirectory.dir("mcp-scripts").get().asFile
    classpath = files(tasks.jar) + configurations.runtimeClasspath.get()
    // Listing every JAR makes the Windows script's line longer than cmd allows (8191
    // characters); a wildcard, which Java expands itself, keeps it short.
    doLast {
        val scripts = checkNotNull(outputDir)
        val windows = scripts.resolve("checkmate-mcp.bat")
        windows.writeText(
            windows.readText().replace(Regex("set CLASSPATH=.*")) { "set CLASSPATH=%APP_HOME%\\lib\\*" }
        )
        val unix = scripts.resolve("checkmate-mcp")
        unix.writeText(
            unix.readText().replace(Regex("(?m)^CLASSPATH=.*$")) { "CLASSPATH=\$APP_HOME/lib/*" }
        )
    }
}

tasks.register<Sync>("installMcp") {
    group = "hackathon"
    description = "Install the CheckMate MCP server with a start script into build/install/checkmate-mcp"
    into(layout.buildDirectory.dir("install/checkmate-mcp"))
    into("lib") {
        from(tasks.jar)
        from(configurations.runtimeClasspath)
    }
    into("bin") { from(mcpStartScripts) }
}

// Antigravity plugin bundle: packages the MCP server together with plugin.json and mcp_config.json
// using ${PLUGIN_ROOT} so it can be validated with `agy plugin validate <dir> --strict` and
// installed locally with `agy plugin install <dir>`.
tasks.register("installAntigravityPlugin") {
    group = "hackathon"
    description = "Package CheckMate MCP server as an Antigravity plugin in build/install/checkmate-antigravity-plugin"
    dependsOn("installMcp")

    val mcpDir = layout.buildDirectory.dir("install/checkmate-mcp")
    val pluginDir = layout.buildDirectory.dir("install/checkmate-antigravity-plugin")
    inputs.dir(mcpDir)
    outputs.dir(pluginDir)

    doLast {
        val out = pluginDir.get().asFile
        out.deleteRecursively()
        out.mkdirs()
        copy {
            from(mcpDir)
            into(out)
        }
        val assetsDir = out.resolve("assets").apply { mkdirs() }
        val repoLogo = rootProject.file("../../website/src/assets/checkmate-logo.png")
        val logoPath = if (repoLogo.isFile) {
            repoLogo.copyTo(assetsDir.resolve("logo.png"), overwrite = true)
            "assets/logo.png"
        } else {
            assetsDir.resolve("logo.svg").writeText(
                """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 64 64"><rect width="64" height="64" rx="12" fill="#990000"/><path d="M18 34l10 10 18-22" fill="none" stroke="#fff" stroke-width="6" stroke-linecap="round" stroke-linejoin="round"/></svg>"""
            )
            "assets/logo.svg"
        }
        val isWindows = System.getProperty("os.name").lowercase().contains("win")
        val pluginRoot = "\${PLUGIN_ROOT}"
        val mcpConfig = if (isWindows) {
            """
            {
              "mcpServers": {
                "checkmate": {
                  "command": "cmd",
                  "args": ["/c", "$pluginRoot/bin/checkmate-mcp.bat"]
                }
              }
            }
            """.trimIndent() + "\n"
        } else {
            """
            {
              "mcpServers": {
                "checkmate": {
                  "command": "/bin/sh",
                  "args": ["$pluginRoot/bin/checkmate-mcp"]
                }
              }
            }
            """.trimIndent() + "\n"
        }
        out.resolve("plugin.json").writeText(
            """
            {
              "name": "checkmate-bimportal",
              "version": "0.3.3-stable",
              "description": "CheckMate BIM Portal — IFC/IDS model checker, LOIN & Merkmale lookup, and MCP Apps views",
              "logo": "$logoPath"
            }
            """.trimIndent() + "\n"
        )
        out.resolve("mcp_config.json").writeText(mcpConfig)
        logger.lifecycle("Antigravity plugin written to ${out.absolutePath}")
    }
}

// Headless LOIN check: same validation as the GUI button, usable in a container or a pipeline.
// Needs no JavaFX.
tasks.register<JavaExec>("runLoinCheck") {
    group = "hackathon"
    description = "Check the configured IFC model against the LOINs matching a search term"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("com.bimportal.hackathon.cli.LoinCheckCli")
}

// The GUI needs javafx.* on the runtime image - a Zulu/Liberica "JDK FX" build has them,
// a plain JDK does not. Checked here because the failure is otherwise a bare NoClassDefFoundError.
tasks.register<JavaExec>("runGui") {
    group = "hackathon"
    description = "Run the CheckMate JavaFX GUI"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("test.fx.App")

    doFirst {
        if (!javaLauncher.get().metadata.installationPath.dir("lib/javafx.properties").asFile.exists()) {
            throw GradleException(
                "This JDK does not bundle JavaFX. Use a JDK-FX build (e.g. Zulu 25 FX), " +
                    "or add the org.openjfx dependencies."
            )
        }
    }
}

// --- Sample IFC models for the IFC reading tests ---
// Third-party files, not all of them licensed for redistribution, so they are not in git:
// downloadTestModels fetches the ones listed in samples.properties into test-models/, and the
// tests that need a missing one are skipped and say which.
val testModelDir = layout.projectDirectory.dir("test-models")
val testModelList = file("src/test/resources/ifc/samples.properties")

fun testModels(): Map<String, String> {
    val list = Properties()
    testModelList.reader(Charsets.UTF_8).use { list.load(it) }
    return list.entries.associate { it.key.toString() to it.value.toString() }.toSortedMap()
}

fun missingTestModels(): Map<String, String> =
    testModels().filterKeys { !testModelDir.file(it).asFile.isFile }

tasks.register("downloadTestModels") {
    group = "hackathon"
    description = "Download the sample IFC models the IFC reading tests use into test-models/"

    doLast {
        val missing = missingTestModels()
        if (missing.isEmpty()) {
            logger.lifecycle("All ${testModels().size} test models are present in test-models/.")
            return@doLast
        }
        missing.forEach { (name, url) ->
            val target = testModelDir.file(name).asFile
            val partial = File(target.path + ".part")
            target.parentFile.mkdirs()
            logger.lifecycle("Downloading $name")
            URI(url).toURL().openStream().use { input ->
                partial.outputStream().use { input.copyTo(it) }
            }
            // A Git LFS pointer or an HTML error page would otherwise pass as a model.
            val header = partial.inputStream().use { String(it.readNBytes(12), Charsets.US_ASCII) }
            if (header != "ISO-10303-21") {
                partial.delete()
                throw GradleException("$url did not return an IFC (STEP) file")
            }
            if (!partial.renameTo(target)) {
                throw GradleException("Could not move $partial to $target")
            }
        }
    }
}

tasks.test {
    useJUnitPlatform()
    systemProperty("checkmate.testModels", testModelDir.asFile.absolutePath)
    inputs.files(fileTree(testModelDir)).withPropertyName("testModels")

    // A skipped test says what is missing to run it (see PrerequisitesTest); list them.
    testLogging { events("skipped", "failed") }

    doFirst {
        val missing = missingTestModels()
        if (missing.isNotEmpty()) {
            logger.lifecycle(
                "⚠ ${missing.size} of ${testModels().size} test models are missing in " +
                    "test-models/ - the tests reading them are skipped. " +
                    "Run ./gradlew :hackathon-template:downloadTestModels to fetch them."
            )
        }
    }
}

// --- Fat JAR for deployment ---
tasks.register<Jar>("hackathonJar") {
    group = "hackathon"
    description = "Create a fat JAR for hackathon deployment"
    archiveClassifier.set("hackathon")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    manifest {
        attributes["Main-Class"] = "com.bimportal.hackathon.HackathonApplication"
    }

    from(configurations.runtimeClasspath.get().map {
        if (it.isDirectory) it else zipTree(it)
    })

    with(tasks.jar.get() as CopySpec)
}
