plugins {
    java
    id("org.jetbrains.intellij.platform") version "2.19.0"
}

group = "org.jev"
version = "0.1.0"

java { toolchain { languageVersion = JavaLanguageVersion.of(21) } }

dependencies {
    intellijPlatform {
        intellijIdea("2026.2.3")
        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Platform)
    }
    testImplementation("org.junit.jupiter:junit-jupiter:5.13.4")
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "262"
            untilBuild = provider { null }
        }
    }
}

tasks.test { useJUnitPlatform() }
