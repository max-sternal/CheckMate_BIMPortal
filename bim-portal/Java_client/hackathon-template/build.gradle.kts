plugins {
    java
    application
    id("org.springframework.boot") version "3.4.0"
    id("io.spring.dependency-management") version "1.1.6"
}

group = "com.bimportal.hackathon"
version = "1.0.0"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

repositories {
    mavenCentral()
}

dependencies {
    // Dependency on the BIM Portal enhanced client
    implementation(project(":bim-portal-enhanced"))

    // Spring Boot starters
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-json")

    // Utilities
    implementation("org.apache.commons:commons-lang3:3.12.0")
    implementation("org.apache.commons:commons-csv:1.10.0")
    implementation("com.google.guava:guava:32.1.2-jre")

    // JSON
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.15.2")

    // Logging
    implementation("org.slf4j:slf4j-api:2.0.9")
    implementation("ch.qos.logback:logback-classic:1.4.11")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testImplementation("org.mockito:mockito-core:5.5.0")
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

tasks.test {
    useJUnitPlatform()
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
