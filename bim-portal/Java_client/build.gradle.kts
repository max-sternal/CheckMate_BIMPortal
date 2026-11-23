plugins {
    java
    id("com.diffplug.spotless") version "7.2.1" apply false
}

group = "com.bimportal"
version = "1.0.0"

// Common configuration for all subprojects
subprojects {
    apply(plugin = "java")
    apply(plugin = "com.diffplug.spotless")

    group = "com.bimportal"
    version = "1.0.0"

    java {
        sourceCompatibility = JavaVersion.VERSION_25
        targetCompatibility = JavaVersion.VERSION_25
    }

    repositories {
        mavenCentral()
    }

    // Configure Spotless for code formatting
    configure<com.diffplug.gradle.spotless.SpotlessExtension> {
        java {
            target("src/**/*.java")
            googleJavaFormat("1.28.0") // Use Google Java Format
            removeUnusedImports()
            trimTrailingWhitespace()
            leadingTabsToSpaces(4)
            endWithNewline()

            // Optional: exclude specific files or directories
            // targetExclude("src/**/generated/**/*.java")
        }

        // Format build files too
        format("buildscripts") {
            target("*.gradle.kts", "**/*.gradle.kts")
            leadingTabsToSpaces(4)
            trimTrailingWhitespace()
            endWithNewline()
        }

        // Format other text files
        format("misc") {
            target("**/*.md", "**/.gitignore", "**/*.yml", "**/*.yaml", "**/*.properties")
            trimTrailingWhitespace()
            leadingTabsToSpaces(2)
            endWithNewline()
        }
    }

    // Common compiler options
    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.compilerArgs.addAll(listOf("-Xlint:unchecked", "-Xlint:deprecation"))
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}

// Root project configuration
repositories {
    mavenCentral()
}