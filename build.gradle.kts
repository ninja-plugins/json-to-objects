plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.1.20"
    id("org.jetbrains.intellij.platform") version "2.10.2"
}

group = "com.ninja"
version = "1.0.0"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

// Read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html
dependencies {
    implementation("com.google.code.gson:gson:2.10.1")

    intellijPlatform {
        intellijIdea("2025.2.4")
        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Platform)

        // Add plugin dependencies for compilation here:


        bundledPlugin("com.intellij.java")
    }
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "252.25557"
        }

        changeNotes = """
            <h3>1.0.0</h3>
            <ul>
                <li>Initial release</li>
                <li>Support Java classes and Kotlin data classes generation</li>
                <li>Lombok annotations support (@Data, @Getter, @Setter, @NoArgsConstructor, @AllArgsConstructor)</li>
                <li>Java Record support</li>
                <li>@JsonProperty annotation support</li>
            </ul>
        """.trimIndent()
    }

    publishing {
        token = providers.gradleProperty("intellijPublishToken")
    }
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "21"
        targetCompatibility = "21"
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}
