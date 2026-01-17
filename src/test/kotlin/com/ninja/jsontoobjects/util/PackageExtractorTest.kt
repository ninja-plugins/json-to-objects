package com.ninja.jsontoobjects.util

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class PackageExtractorTest {

    @Nested
    @DisplayName("extractPackage 테스트")
    inner class ExtractPackageTests {

        @Test
        @DisplayName("Kotlin 파일에서 패키지 추출")
        fun extractFromKotlinFile() {
            val kotlinCode = """
                package com.ninja.jsontoobjects.model

                data class User(
                    val name: String,
                    val age: Int
                )
            """.trimIndent()

            val result = PackageExtractor.extractPackage(kotlinCode)
            assertEquals("com.ninja.jsontoobjects.model", result)
        }

        @Test
        @DisplayName("Java 파일에서 패키지 추출")
        fun extractFromJavaFile() {
            val javaCode = """
                package com.ninja.jsontoobjects.generator;

                public class JavaGenerator {
                    // ...
                }
            """.trimIndent()

            val result = PackageExtractor.extractPackage(javaCode)
            assertEquals("com.ninja.jsontoobjects.generator", result)
        }

        @Test
        @DisplayName("패키지 선언 앞에 공백이 있는 경우")
        fun extractWithLeadingWhitespace() {
            val code = """

                   package com.example.app

                class App
            """.trimIndent()

            val result = PackageExtractor.extractPackage(code)
            assertEquals("com.example.app", result)
        }

        @Test
        @DisplayName("패키지 선언이 없는 경우 null 반환")
        fun returnNullWhenNoPackage() {
            val code = """
                class NoPackageClass {
                    fun doSomething() {}
                }
            """.trimIndent()

            val result = PackageExtractor.extractPackage(code)
            assertNull(result)
        }

        @Test
        @DisplayName("빈 문자열에서 null 반환")
        fun returnNullForEmptyString() {
            val result = PackageExtractor.extractPackage("")
            assertNull(result)
        }

        @Test
        @DisplayName("JSON 파일 내용에서 null 반환")
        fun returnNullForJsonContent() {
            val jsonContent = """
                {
                    "name": "test",
                    "package": "com.example"
                }
            """.trimIndent()

            val result = PackageExtractor.extractPackage(jsonContent)
            assertNull(result)
        }

        @Test
        @DisplayName("주석 뒤에 패키지 선언이 있는 경우")
        fun extractWithCommentBefore() {
            val code = """
                // This is a comment
                /* Multi-line
                   comment */
                package com.example.service

                class Service
            """.trimIndent()

            val result = PackageExtractor.extractPackage(code)
            assertEquals("com.example.service", result)
        }

        @Test
        @DisplayName("단일 단어 패키지")
        fun extractSingleWordPackage() {
            val code = """
                package myapp

                class App
            """.trimIndent()

            val result = PackageExtractor.extractPackage(code)
            assertEquals("myapp", result)
        }

        @Test
        @DisplayName("깊은 패키지 구조")
        fun extractDeepPackage() {
            val code = """
                package com.company.project.module.submodule.feature

                class Feature
            """.trimIndent()

            val result = PackageExtractor.extractPackage(code)
            assertEquals("com.company.project.module.submodule.feature", result)
        }

        @Test
        @DisplayName("탭으로 들여쓰기된 패키지 선언")
        fun extractWithTabIndent() {
            val code = "\tpackage com.example.app\n\nclass App"

            val result = PackageExtractor.extractPackage(code)
            assertEquals("com.example.app", result)
        }
    }

    @Nested
    @DisplayName("isSourceFile 테스트")
    inner class IsSourceFileTests {

        @Test
        @DisplayName("Kotlin 파일 확장자")
        fun kotlinExtension() {
            assertTrue(PackageExtractor.isSourceFile("kt"))
            assertTrue(PackageExtractor.isSourceFile("KT"))
            assertTrue(PackageExtractor.isSourceFile("Kt"))
        }

        @Test
        @DisplayName("Java 파일 확장자")
        fun javaExtension() {
            assertTrue(PackageExtractor.isSourceFile("java"))
            assertTrue(PackageExtractor.isSourceFile("JAVA"))
            assertTrue(PackageExtractor.isSourceFile("Java"))
        }

        @Test
        @DisplayName("JSON 파일은 소스 파일이 아님")
        fun jsonExtensionIsNotSource() {
            assertFalse(PackageExtractor.isSourceFile("json"))
            assertFalse(PackageExtractor.isSourceFile("JSON"))
        }

        @Test
        @DisplayName("기타 확장자는 소스 파일이 아님")
        fun otherExtensionsAreNotSource() {
            assertFalse(PackageExtractor.isSourceFile("txt"))
            assertFalse(PackageExtractor.isSourceFile("xml"))
            assertFalse(PackageExtractor.isSourceFile("md"))
            assertFalse(PackageExtractor.isSourceFile("gradle"))
            assertFalse(PackageExtractor.isSourceFile("kts"))
        }

        @Test
        @DisplayName("null 확장자는 소스 파일이 아님")
        fun nullExtensionIsNotSource() {
            assertFalse(PackageExtractor.isSourceFile(null))
        }

        @Test
        @DisplayName("빈 문자열은 소스 파일이 아님")
        fun emptyExtensionIsNotSource() {
            assertFalse(PackageExtractor.isSourceFile(""))
        }
    }
}
