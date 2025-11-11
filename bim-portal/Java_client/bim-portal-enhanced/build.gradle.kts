plugins {
    java
    application
    id("org.springframework.boot") version "3.4.0"
    id("io.spring.dependency-management") version "1.1.6"
}

dependencies {
    // Dependency on the generated OpenAPI client
    implementation(project(":openapi-generated"))

    // Spring Boot starters
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-web")

    // Utilities using version catalog
    implementation(libs.bundles.utilities)

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation(libs.bundles.testing)
}

application {
    mainClass.set("com.pb40.bimportal.examples.QuickStart")
}

tasks.jar {
    dependsOn(":openapi-generated:jar")
    manifest {
        attributes["Main-Class"] = "com.pb40.bimportal.examples.QuickStart"
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
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
                    val value = parts[1].trim().removeSurrounding("\"").removeSurrounding("'")
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

// Load .env once
val envProperties by lazy { loadEnvFile() }

// --- Configure JavaExec tasks ---
tasks.withType<JavaExec> {
    val logLevel = envProperties["LOG_LEVEL"] ?: "INFO"

    // System properties for logging
    systemProperty("LOG_LEVEL", logLevel)
    systemProperty("logging.level.root", logLevel)
    systemProperty("logging.level.com.pb40.bimportal", logLevel)
    systemProperty("logging.level.com.bimportal.client", logLevel)
    systemProperty("logging.level.feign", envProperties["FEIGN_LOG_LEVEL"] ?: "WARN")
    systemProperty("logging.level.okhttp3", envProperties["OKHTTP_LOG_LEVEL"] ?: "WARN")
    systemProperty("logging.level.com.fasterxml.jackson", envProperties["JACKSON_LOG_LEVEL"] ?: "WARN")
    systemProperty("logging.level.org.springframework.web", envProperties["SPRING_WEB_LOG_LEVEL"] ?: "WARN")
    systemProperty("logging.level.org.springframework.security", envProperties["SPRING_SECURITY_LOG_LEVEL"] ?: "WARN")
    systemProperty("logging.level.org.springframework.boot", envProperties["SPRING_BOOT_LOG_LEVEL"] ?: "WARN")
    systemProperty("logging.level.org.springframework.context", envProperties["SPRING_CONTEXT_LOG_LEVEL"] ?: "WARN")

    // Propagate all .env vars both as env vars and system properties
    envProperties.forEach { (key, value) ->
        environment(key, value)    // available via System.getenv()
        systemProperty(key, value) // available via System.getProperty()
    }
}

// --- Custom tasks ---
tasks.register<JavaExec>("quickStart") {
    group = "examples"
    description = "Run the quick start example"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("com.pb40.bimportal.examples.QuickStart")
}

tasks.register<JavaExec>("exportExamples") {
    group = "examples"
    description = "Run comprehensive export examples"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("com.pb40.bimportal.examples.ExportExamples")
}

tasks.register<JavaExec>("healthCheck") {
    group = "examples"
    description = "Run API health check"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("com.pb40.bimportal.examples.HealthCheck")
}

tasks.register<JavaExec>("checkApiMethods") {
    group = "examples"
    description = "Check available methods in generated API classes"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("com.bimportal.client.ApiMethodChecker")
}

tasks.register<JavaExec>("configDebugTest") {
    group = "examples"
    description = "Run configuration debug test with proper logging"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("com.pb40.bimportal.examples.ConfigDebugTest")
}

tasks.register<JavaExec>("exportProjects") {
    group = "examples"
    description = "Export projects in multiple formats (PDF, OpenOffice, OKSTRA, LOIN-XML, IDS)"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("com.pb40.bimportal.examples.ProjectExportExample")
}

tasks.register<JavaExec>("exportLoins") {
    group = "examples"
    description = "Export LOINs in multiple formats (PDF, OpenOffice, OKSTRA, LOIN-XML, IDS)"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("com.pb40.bimportal.examples.LoinExportExample")
}

tasks.register<JavaExec>("exportDomainModels") {
    group = "examples"
    description = "Export domain models in multiple formats (PDF, OpenOffice, OKSTRA, LOIN-XML, IDS)"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("com.pb40.bimportal.examples.DomainModelExportExample")
}

tasks.register<JavaExec>("exportContextInfo") {
    group = "examples"
    description = "Export context information in multiple formats (PDF, OpenOffice)"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("com.pb40.bimportal.examples.ContextInfoExportExample")
}

tasks.register<JavaExec>("exportTemplates") {
    group = "examples"
    description = "Export AIA templates in multiple formats (PDF, OpenOffice)"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("com.pb40.bimportal.examples.TemplateExportExample")
}

tasks.register<JavaExec>("batchExport") {
    group = "examples"
    description = "Run batch export examples for multiple projects and LOINs"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("com.pb40.bimportal.examples.BatchExportExample")
}

tasks.register<JavaExec>("searchExamples") {
    group = "examples"
    description = "Run search and filter examples"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("com.pb40.bimportal.examples.SearchExample")
}

tasks.register<JavaExec>("organizationExamples") {
    group = "examples"
    description = "Run organization examples"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("com.pb40.bimportal.examples.OrganizationExample")
}

tasks.register("runAllExamples") {
    group = "examples"
    description = "Run all export examples sequentially"
    dependsOn(
        "exportProjects",
        "exportLoins",
        "exportDomainModels",
        "exportContextInfo",
        "exportTemplates",
        "batchExport",
        "searchExamples",
        "organizationExamples"
    )
    doLast {
        println("All export examples completed!")
    }
}

// --- Resource processing ---
//tasks.processResources {
//    filesMatching("application.properties") {
//        expand(project.properties)
//    }
//}
