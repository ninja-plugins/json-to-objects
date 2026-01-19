plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.1.20"
    id("org.jetbrains.intellij.platform") version "2.10.2"
}

group = "com.ninja"
version = "1.0.2"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

// Read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html
dependencies {
    implementation("com.google.code.gson:gson:2.10.1")

//    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
//    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

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
            sinceBuild = "231"  // IntelliJ 2023.1+
        }

        changeNotes = """
            <h3>1.0.2</h3>
            <ul>
                <li>Add Tool Window for quick access from IDE sidebar</li>
                <li>Package name autocomplete support</li>
                <li>Auto-detect package from currently open file</li>
                <li>Generate files in correct package directory</li>
                <li>Improved JSON input cleaning (trailing semicolon removal)</li>
            </ul>
            <h3>1.0.1</h3>
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

    pluginVerification {
        ides {
            recommended()
        }
    }
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "21"
        targetCompatibility = "21"
    }

    // 테스트 비활성화 (로컬에서만 수동 실행)
    test {
        enabled = false
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}
