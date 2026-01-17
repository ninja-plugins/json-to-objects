package com.ninja.jsontoobjects.util

/**
 * 소스 코드에서 패키지 선언을 추출하는 유틸리티
 */
object PackageExtractor {

    private val PACKAGE_REGEX = Regex("""^\s*package\s+([\w.]+)""", RegexOption.MULTILINE)

    /**
     * 소스 코드 텍스트에서 패키지 선언을 추출합니다.
     * Kotlin/Java 모두 지원합니다.
     *
     * @param sourceCode 소스 코드 텍스트
     * @return 패키지명 또는 없으면 null
     */
    fun extractPackage(sourceCode: String): String? {
        return PACKAGE_REGEX.find(sourceCode)?.groupValues?.get(1)
    }

    /**
     * 파일 확장자가 Kotlin 또는 Java 소스 파일인지 확인합니다.
     *
     * @param extension 파일 확장자 (예: "kt", "java")
     * @return Kotlin/Java 소스 파일이면 true
     */
    fun isSourceFile(extension: String?): Boolean {
        val ext = extension?.lowercase()
        return ext == "kt" || ext == "java"
    }
}
