//package com.ninja.jsontoobjects.util
//
//import com.intellij.pom.java.LanguageLevel
//import org.junit.jupiter.api.Assertions.*
//import org.junit.jupiter.api.DisplayName
//import org.junit.jupiter.api.Nested
//import org.junit.jupiter.api.Test
//
//class JavaVersionUtilTest {
//
//    @Nested
//    @DisplayName("supportsRecord 테스트")
//    inner class SupportsRecordTests {
//
//        @Test
//        @DisplayName("null이면 false 반환")
//        fun nullLanguageLevelReturnsFalse() {
//            assertFalse(JavaVersionUtil.supportsRecord(null))
//        }
//
//        @Test
//        @DisplayName("Java 8은 Record 미지원")
//        fun java8DoesNotSupportRecord() {
//            assertFalse(JavaVersionUtil.supportsRecord(LanguageLevel.JDK_1_8))
//        }
//
//        @Test
//        @DisplayName("Java 11은 Record 미지원")
//        fun java11DoesNotSupportRecord() {
//            assertFalse(JavaVersionUtil.supportsRecord(LanguageLevel.JDK_11))
//        }
//
//        @Test
//        @DisplayName("Java 13은 Record 미지원")
//        fun java13DoesNotSupportRecord() {
//            assertFalse(JavaVersionUtil.supportsRecord(LanguageLevel.JDK_13))
//        }
//
//        @Test
//        @DisplayName("Java 14는 Record 지원 (preview)")
//        fun java14SupportsRecord() {
//            assertTrue(JavaVersionUtil.supportsRecord(LanguageLevel.JDK_14))
//        }
//
//        @Test
//        @DisplayName("Java 16은 Record 지원 (정식)")
//        fun java16SupportsRecord() {
//            assertTrue(JavaVersionUtil.supportsRecord(LanguageLevel.JDK_16))
//        }
//
//        @Test
//        @DisplayName("Java 17은 Record 지원")
//        fun java17SupportsRecord() {
//            assertTrue(JavaVersionUtil.supportsRecord(LanguageLevel.JDK_17))
//        }
//
//        @Test
//        @DisplayName("Java 21은 Record 지원")
//        fun java21SupportsRecord() {
//            assertTrue(JavaVersionUtil.supportsRecord(LanguageLevel.JDK_21))
//        }
//    }
//}
