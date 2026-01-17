plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.1.20"
    id("org.jetbrains.intellij.platform") version "2.10.2"
}

group = "com.ninja"
version = "1.0.1"

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
            sinceBuild = "252.25557"
        }

        changeNotes = """
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
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "21"
        targetCompatibility = "21"
    }
//
//    // Default test task is configured by IntelliJ Platform plugin for integration tests
//    // For unit tests, use the unitTest task instead
//    test {
//        // Disable the default test task - use unitTest for pure unit tests
//        enabled = false
//    }
//}
//
//// IntelliJ Platform test 환경을 사용하지 않는 순수 단위 테스트
//val unitTest by tasks.registering(Test::class) {
//    description = "Run pure unit tests without IntelliJ Platform"
//    group = "verification"
//
//    useJUnitPlatform()
//
//    testClassesDirs = sourceSets["test"].output.classesDirs
//    classpath = sourceSets["test"].runtimeClasspath
//
//    // IntelliJ Platform의 클래스로더 사용 안 함
//    jvmArgs = listOf()
//    systemProperties.clear()
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}
