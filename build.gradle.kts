plugins {
    java
    id("org.jetbrains.intellij.platform")
}

group = "org.jev"
version = "0.1.0"

java { toolchain { languageVersion = JavaLanguageVersion.of(25) } }

dependencies {
    intellijPlatform {
        intellijIdea("2026.2.3")
    }
    testImplementation("org.junit.jupiter:junit-jupiter:5.13.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.13.4")
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
