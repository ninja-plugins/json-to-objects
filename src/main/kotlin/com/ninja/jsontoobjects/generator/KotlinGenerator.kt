package com.ninja.jsontoobjects.generator

import com.ninja.jsontoobjects.model.*
import com.ninja.jsontoobjects.parser.ParseResult
import com.ninja.jsontoobjects.util.StringUtils

class KotlinGenerator(private val options: KotlinOptions) {

    fun generate(parseResult: ParseResult, rootClassName: String): Map<String, String> {
        val results = mutableMapOf<String, String>()

        when (options.structureMode) {
            StructureMode.MULTIPLE_FILES -> {
                for ((className, objectType) in parseResult.allClasses) {
                    val code = generateSingleClass(className, objectType)
                    results["$className.kt"] = code
                }
            }
            StructureMode.INNER_CLASS, StructureMode.SEPARATE_CLASSES -> {
                val code = generateAllClassesSingleFile(rootClassName, parseResult)
                results["$rootClassName.kt"] = code
            }
        }

        return results
    }

    private fun generateAllClassesSingleFile(rootClassName: String, parseResult: ParseResult): String {
        val sb = StringBuilder()
        addImports(sb)
        sb.appendLine()

        for ((className, objectType) in parseResult.allClasses) {
            generateDataClass(sb, className, objectType, indent = "")
            sb.appendLine()
        }

        return sb.toString().trimEnd()
    }

    private fun generateSingleClass(className: String, objectType: ParsedType.ObjectType): String {
        val sb = StringBuilder()
        addImports(sb)
        sb.appendLine()
        generateDataClass(sb, className, objectType, indent = "")
        return sb.toString()
    }

    private fun addImports(sb: StringBuilder) {
        if (options.useJsonProperty) {
            sb.appendLine("import com.fasterxml.jackson.annotation.JsonProperty")
        }
    }

    private fun generateDataClass(
        sb: StringBuilder,
        className: String,
        objectType: ParsedType.ObjectType,
        indent: String
    ) {
        val fields = objectType.fields.entries.map { (fieldName, fieldType) ->
            val kotlinType = toKotlinType(fieldType)
            val camelName = StringUtils.toCamelCase(fieldName)

            if (options.useJsonProperty) {
                "$indent    @JsonProperty(\"$fieldName\") val $camelName: $kotlinType"
            } else {
                "$indent    val $camelName: $kotlinType"
            }
        }

        if (fields.isEmpty()) {
            sb.appendLine("${indent}data class $className()")
        } else if (fields.size <= 3 && !options.useJsonProperty) {
            // Single line for simple classes
            val params = objectType.fields.entries.joinToString(", ") { (fieldName, fieldType) ->
                val kotlinType = toKotlinType(fieldType)
                val camelName = StringUtils.toCamelCase(fieldName)
                "val $camelName: $kotlinType"
            }
            sb.appendLine("${indent}data class $className($params)")
        } else {
            // Multi-line format
            sb.appendLine("${indent}data class $className(")
            fields.forEachIndexed { index, field ->
                val suffix = if (index < fields.size - 1) "," else ""
                sb.appendLine("$field$suffix")
            }
            sb.appendLine("$indent)")
        }
    }

    private fun toKotlinType(type: ParsedType): String {
        return when (type) {
            is ParsedType.Primitive -> toKotlinPrimitive(type.typeName)
            is ParsedType.ArrayType -> "List<${toKotlinType(type.elementType)}>"
            is ParsedType.ObjectType -> type.typeName
        }
    }

    private fun toKotlinPrimitive(javaType: String): String {
        return when (javaType) {
            "Integer" -> "Int"
            "Long" -> "Long"
            "Double" -> "Double"
            "Boolean" -> "Boolean"
            "String" -> "String"
            "Object" -> "Any"
            else -> javaType
        }
    }
}
