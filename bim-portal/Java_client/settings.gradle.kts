rootProject.name = "bim-portal-client-suite"

// Enable Gradle's configuration cache for better performance
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

// Plugin management for consistent versions across modules
pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

// Dependency resolution management
dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }

    // Version catalog for consistent dependency versions
    versionCatalogs {
        create("libs") {
            // Core dependencies
            library("slf4j-api", "org.slf4j", "slf4j-api").version("2.0.9")
            library("logback-classic", "ch.qos.logback", "logback-classic").version("1.4.11")

            // HTTP and JSON
            library("okhttp", "com.squareup.okhttp3", "okhttp").version("4.11.0")
            library("okhttp-logging", "com.squareup.okhttp3", "logging-interceptor").version("4.11.0")
            library("gson", "com.google.code.gson", "gson").version("2.10.1")

            // Utilities
            library("commons-lang3", "org.apache.commons", "commons-lang3").version("3.12.0")
            library("commons-io", "commons-io", "commons-io").version("2.11.0")
            library("dotenv", "io.github.cdimascio", "dotenv-java").version("3.0.0")

            // Testing
            library("junit-jupiter", "org.junit.jupiter", "junit-jupiter").version("5.10.0")
            library("mockito-core", "org.mockito", "mockito-core").version("5.5.0")
            library("mockwebserver", "com.squareup.okhttp3", "mockwebserver").version("4.11.0")

            // OpenAPI/Swagger
            library("swagger-annotations", "io.swagger", "swagger-annotations").version("1.6.8")

            // Jakarta validation (for Spring Boot 3.x)
            library("jakarta-validation", "jakarta.validation", "jakarta.validation-api").version("3.0.2")
            library("jakarta-annotation", "jakarta.annotation", "jakarta.annotation-api").version("2.1.1")
            library("hibernate-validator", "org.hibernate.validator", "hibernate-validator").version("8.0.1.Final")

            // Bundle definitions for common dependency groups
            bundle("logging", listOf("slf4j-api", "logback-classic"))
            bundle("http-client", listOf("okhttp", "okhttp-logging", "gson"))
            bundle("utilities", listOf("commons-lang3", "commons-io", "dotenv"))
            bundle("testing", listOf("junit-jupiter", "mockito-core", "mockwebserver"))
            bundle("validation", listOf("jakarta-validation", "jakarta-annotation", "hibernate-validator"))
        }
    }
}

// Module declarations
include("openapi-generated")
include("bim-portal-enhanced")
include("hackathon-template")


// Configure project directories
project(":openapi-generated").apply {
    projectDir = file("openapi-generated")
}

project(":bim-portal-enhanced").apply {
    projectDir = file("bim-portal-enhanced")
}

project(":hackathon-template").apply {
    projectDir = file("hackathon-template")
}