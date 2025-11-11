plugins {
    java
    `java-library`
}

group = "com.bimportal"
version = "1.0.0"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

repositories {
    mavenCentral()
}

dependencies {
    // Core Feign dependencies
    api("io.github.openfeign:feign-core:13.1")
    api("io.github.openfeign:feign-jackson:13.1")
    api("io.github.openfeign:feign-slf4j:13.1")
    api("io.github.openfeign:feign-okhttp:13.1")

    // Form encoding support (required by generated client)
    api("io.github.openfeign.form:feign-form:3.8.0")

    // Jackson dependencies with JSR310 support
    api("com.fasterxml.jackson.core:jackson-databind:2.15.2")
    api("com.fasterxml.jackson.core:jackson-annotations:2.15.2")
    api("com.fasterxml.jackson.core:jackson-core:2.15.2")
    api("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.15.2")

    // Jackson nullable support (required by generated client)
    api("org.openapitools:jackson-databind-nullable:0.2.6")

    // HTTP client
    api("com.squareup.okhttp3:okhttp:4.11.0")
    api("com.squareup.okhttp3:logging-interceptor:4.11.0")

    // OpenAPI/Swagger annotations
    api("io.swagger:swagger-annotations:1.6.8")

    // Jakarta Validation (for Spring Boot 3.x compatibility)
    api("jakarta.validation:jakarta.validation-api:3.0.2")
    api("jakarta.annotation:jakarta.annotation-api:2.1.1")
    api("org.hibernate.validator:hibernate-validator:8.0.1.Final")

    // Logging
    api("org.slf4j:slf4j-api:2.0.9")

    // Testing
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // Keep existing test dependencies
    testImplementation("org.mockito:mockito-core:5.5.0")
    testImplementation("io.github.cdimascio:java-dotenv:5.2.2")
}

tasks.test {
    useJUnitPlatform()
}


// Fix incorrect imports and configure Jackson properly in generated code
// Simpler fix that works with Spotless formatting
tasks.register("fixGeneratedCode") {
    doLast {
        val srcDir = file("src/main/java")
        if (srcDir.exists()) {
            srcDir.walkTopDown()
                .filter { it.name.endsWith("Api.java") }
                .forEach { file ->
                    var content = file.readText()

                    // Fix imports
                    content = content.replace(
                        "import org.gradle.internal.impldep.org.apache.http.RequestLine;",
                        "import feign.RequestLine;"
                    ).replace(
                        "import org.junit.jupiter.params.shadow.com.univocity.parsers.annotations.Headers;",
                        "import feign.Headers;"
                    )

                    // Fix QueryMap
                    content = content.replace("@QueryMap(encoded=true)", "@QueryMap")

                    // REMOVE ALL duplicate annotations first
                    content = content.replace(
                        Regex("@SuppressWarnings\\(\"deprecation\"\\)\\s+@SuppressWarnings\\(\"deprecation\"\\)"),
                        "@SuppressWarnings(\"deprecation\")"
                    )

                    // Remove @SuppressWarnings that are immediately followed by @QueryMap
                    content = content.replace(
                        Regex("@SuppressWarnings\\(\"deprecation\"\\)\\s+@QueryMap"),
                        "@QueryMap"
                    )

                    file.writeText(content)
                    //println("Fixed code issues in: ${file.relativeTo(srcDir)}")
                }
        }
    }
}

// Configure compiler options
tasks.compileJava {
    dependsOn("fixGeneratedCode")
    options.encoding = "UTF-8"

    // Remove -Werror and configure appropriate warnings
    options.compilerArgs.addAll(listOf(
        "-Xlint:unchecked",
        "-Xlint:deprecation",
        "-Xlint:-processing"  // Suppress annotation processing warnings
        // Note: Removed -Werror to prevent warnings from failing the build
    ))

    // Set source and target compatibility explicitly
    sourceCompatibility = JavaVersion.VERSION_21.toString()
    targetCompatibility = JavaVersion.VERSION_21.toString()
}
