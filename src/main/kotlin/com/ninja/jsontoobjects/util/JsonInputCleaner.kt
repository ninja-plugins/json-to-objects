package com.ninja.jsontoobjects.util

object JsonInputCleaner {

    /**
     * 다양한 형식의 JSON 입력을 정리하여 파싱 가능한 JSON으로 변환
     * - 주석 처리된 JSON (// prefix)
     * - Java 문자열 연결 형식 ("...\n" +)
     * - Kotlin raw string (""")
     */
    fun clean(input: String): String {
        val trimmed = input.trim()

        return when {
            looksLikeCommentedJson(trimmed) -> cleanCommentedJson(trimmed)
            looksLikeJavaStringConcat(trimmed) -> cleanJavaStringConcat(trimmed)
            looksLikeKotlinRawString(trimmed) -> cleanKotlinRawString(trimmed)
            else -> trimmed
        }
    }

    /**
     * // 주석으로 시작하는 줄들인지 확인
     */
    private fun looksLikeCommentedJson(input: String): Boolean {
        val lines = input.lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return false
        // 대부분의 줄이 // 로 시작하면 주석 JSON으로 판단
        val commentedLines = lines.count { it.trim().startsWith("//") }
        return commentedLines > lines.size / 2
    }

    /**
     * 주석 JSON 정리: 각 줄에서 // 제거
     */
    private fun cleanCommentedJson(input: String): String {
        return input.lines()
            .map { line ->
                val trimmed = line.trim()
                if (trimmed.startsWith("//")) {
                    trimmed.removePrefix("//").trim()
                } else {
                    trimmed
                }
            }
            .joinToString("\n")
    }

    /**
     * Java 문자열 연결 형식인지 확인
     * 예: "{\n" +
     */
    private fun looksLikeJavaStringConcat(input: String): Boolean {
        // " + 패턴이나 "\n" 패턴이 있으면 Java 문자열로 판단
        return input.contains("\" +") ||
               input.contains("\\n\"") ||
               (input.contains("\\\"") && input.contains("\\n"))
    }

    /**
     * Java 문자열 연결 형식 정리
     * "{\n" + "  \"name\": \"value\"\n" + "}" -> { "name": "value" }
     */
    private fun cleanJavaStringConcat(input: String): String {
        var result = input

        // 문자열 연결 연산자 제거: " +, "+ 등
        result = result.replace(Regex("\"\\s*\\+\\s*\""), "")
        result = result.replace(Regex("\"\\s*\\+\\s*\n\\s*\""), "")

        // 시작과 끝의 따옴표 제거
        result = result.trim()
        if (result.startsWith("\"")) {
            result = result.substring(1)
        }
        if (result.endsWith("\";") || result.endsWith("\"")) {
            result = result.dropLast(if (result.endsWith("\";")) 2 else 1)
        }

        // 이스케이프된 문자 복원
        result = result.replace("\\\"", "\"")
        result = result.replace("\\n", "\n")
        result = result.replace("\\t", "\t")
        result = result.replace("\\\\", "\\")

        return result.trim()
    }

    /**
     * Kotlin raw string 형식인지 확인
     */
    private fun looksLikeKotlinRawString(input: String): Boolean {
        return input.contains("\"\"\"") || input.contains(".trimIndent()")
    }

    /**
     * Kotlin raw string 정리
     */
    private fun cleanKotlinRawString(input: String): String {
        var result = input

        // trimIndent(), trimMargin() 제거
        result = result.replace(Regex("\\.trimIndent\\(\\)"), "")
        result = result.replace(Regex("\\.trimMargin\\(.*?\\)"), "")

        // """ 제거
        result = result.replace("\"\"\"", "")

        return result.trim()
    }
}
