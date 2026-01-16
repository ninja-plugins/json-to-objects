package com.ninja.jsontoobjects.util

object StringUtils {
    fun toPascalCase(input: String): String {
        if (input.isBlank()) return input
        return input.split(Regex("[_\\-\\s]+"))
            .filter { it.isNotEmpty() }
            .joinToString("") { word ->
                word.replaceFirstChar { it.uppercaseChar() }
            }
            .ifEmpty { input.replaceFirstChar { it.uppercaseChar() } }
    }

    fun toCamelCase(input: String): String {
        val pascal = toPascalCase(input)
        return if (pascal.isNotEmpty()) {
            pascal.replaceFirstChar { it.lowercaseChar() }
        } else {
            pascal
        }
    }

    fun toSnakeCase(input: String): String {
        return input
            .replace(Regex("([a-z])([A-Z])"), "$1_$2")
            .replace(Regex("[\\s-]+"), "_")
            .lowercase()
    }

    fun singularize(word: String): String {
        return when {
            word.endsWith("ies") -> word.dropLast(3) + "y"
            word.endsWith("es") && (word.endsWith("ses") || word.endsWith("xes") || word.endsWith("zes") || word.endsWith("ches") || word.endsWith("shes")) -> word.dropLast(2)
            word.endsWith("s") && !word.endsWith("ss") -> word.dropLast(1)
            else -> word
        }
    }

    fun isValidIdentifier(name: String): Boolean {
        if (name.isBlank()) return false
        val first = name.first()
        if (!first.isLetter() && first != '_') return false
        return name.all { it.isLetterOrDigit() || it == '_' }
    }

    fun sanitizeIdentifier(name: String): String {
        if (name.isBlank()) return "field"
        val sanitized = name.filter { it.isLetterOrDigit() || it == '_' }
        return if (sanitized.isEmpty() || (!sanitized.first().isLetter() && sanitized.first() != '_')) {
            "_$sanitized"
        } else {
            sanitized
        }
    }
}
