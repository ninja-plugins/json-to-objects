package com.ninja.jsontoobjects.generator

import com.ninja.jsontoobjects.model.*
import com.ninja.jsontoobjects.parser.ParseResult
import com.ninja.jsontoobjects.util.StringUtils

class JavaGenerator(private val options: JavaOptions) {

    fun generate(parseResult: ParseResult, rootClassName: String): Map<String, String> {
        val results = mutableMapOf<String, String>()

        when (options.structureMode) {
            StructureMode.MULTIPLE_FILES -> {
                for ((className, objectType) in parseResult.allClasses) {
                    val code = generateSingleClass(className, objectType, isInner = false)
                    results["$className.java"] = code
                }
            }
            StructureMode.INNER_CLASS -> {
                val code = generateWithInnerClasses(rootClassName, parseResult)
                results["$rootClassName.java"] = code
            }
            StructureMode.SEPARATE_CLASSES -> {
                val code = generateSeparateClassesSingleFile(rootClassName, parseResult)
                results["$rootClassName.java"] = code
            }
        }

        return results
    }

    private fun generateWithInnerClasses(rootClassName: String, parseResult: ParseResult): String {
        val sb = StringBuilder()
        addImports(sb)

        val rootType = parseResult.allClasses[rootClassName]
            ?: return "// Error: Root class not found"

        sb.appendLine()
        generateClassContent(sb, rootClassName, rootType, parseResult.allClasses, indent = "", isInner = false, includeInnerClasses = true)

        return sb.toString()
    }

    private fun generateSeparateClassesSingleFile(rootClassName: String, parseResult: ParseResult): String {
        val sb = StringBuilder()
        addImports(sb)
        sb.appendLine()

        for ((className, objectType) in parseResult.allClasses) {
            generateClassContent(sb, className, objectType, parseResult.allClasses, indent = "", isInner = false, includeInnerClasses = false)
            sb.appendLine()
        }

        return sb.toString().trimEnd()
    }

    private fun generateSingleClass(className: String, objectType: ParsedType.ObjectType, isInner: Boolean): String {
        val sb = StringBuilder()
        if (!isInner) {
            addImports(sb)
            sb.appendLine()
        }
        generateClassContent(sb, className, objectType, emptyMap(), indent = "", isInner = isInner, includeInnerClasses = false)
        return sb.toString()
    }

    private fun addImports(sb: StringBuilder) {
        val imports = mutableListOf<String>()

        if (options.useJsonProperty) {
            imports.add("import com.fasterxml.jackson.annotation.JsonProperty;")
        }
        if (options.useData) {
            imports.add("import lombok.Data;")
        }
        if (options.useGetter) {
            imports.add("import lombok.Getter;")
        }
        if (options.useSetter) {
            imports.add("import lombok.Setter;")
        }
        if (options.useNoArgsConstructor) {
            imports.add("import lombok.NoArgsConstructor;")
        }
        if (options.useAllArgsConstructor) {
            imports.add("import lombok.AllArgsConstructor;")
        }
        imports.add("import java.util.List;")

        imports.sorted().forEach { sb.appendLine(it) }
    }

    private fun generateClassContent(
        sb: StringBuilder,
        className: String,
        objectType: ParsedType.ObjectType,
        allClasses: Map<String, ParsedType.ObjectType>,
        indent: String,
        isInner: Boolean,
        includeInnerClasses: Boolean
    ) {
        if (options.useRecord) {
            generateRecord(sb, className, objectType, allClasses, indent, includeInnerClasses)
            return
        }

        // Lombok annotations
        if (options.useData) {
            sb.appendLine("${indent}@Data")
        }
        if (options.useGetter) {
            sb.appendLine("${indent}@Getter")
        }
        if (options.useSetter) {
            sb.appendLine("${indent}@Setter")
        }
        if (options.useNoArgsConstructor) {
            sb.appendLine("${indent}@NoArgsConstructor")
        }
        if (options.useAllArgsConstructor) {
            sb.appendLine("${indent}@AllArgsConstructor")
        }

        val staticModifier = if (isInner) "static " else ""
        sb.appendLine("${indent}public ${staticModifier}class $className {")

        val innerIndent = "$indent    "

        // Fields
        for ((fieldName, fieldType) in objectType.fields) {
            val javaType = toJavaType(fieldType)
            val camelName = StringUtils.toCamelCase(fieldName)

            if (options.useJsonProperty) {
                sb.appendLine("${innerIndent}@JsonProperty(\"$fieldName\")")
            }
            sb.appendLine("${innerIndent}private $javaType $camelName;")
        }

        // Manual NoArgs Constructor
        if (options.shouldGenerateNoArgsConstructor) {
            sb.appendLine()
            sb.appendLine("${innerIndent}public $className() {}")
        }

        // Manual AllArgs Constructor
        if (options.shouldGenerateAllArgsConstructor) {
            sb.appendLine()
            generateAllArgsConstructor(sb, className, objectType, innerIndent)
        }

        // Manual Getter
        if (options.shouldGenerateGetter) {
            sb.appendLine()
            generateGetters(sb, objectType, innerIndent)
        }

        // Manual Setter
        if (options.shouldGenerateSetter) {
            sb.appendLine()
            generateSetters(sb, objectType, innerIndent)
        }

        // Inner classes
        if (includeInnerClasses) {
            val nestedTypes = objectType.fields.values
                .filterIsInstance<ParsedType.ObjectType>()
                .map { it.typeName }
                .toSet()

            val arrayNestedTypes = objectType.fields.values
                .filterIsInstance<ParsedType.ArrayType>()
                .mapNotNull { (it.elementType as? ParsedType.ObjectType)?.typeName }
                .toSet()

            val allNestedTypes = nestedTypes + arrayNestedTypes

            for (nestedTypeName in allNestedTypes) {
                val nestedType = allClasses[nestedTypeName]
                if (nestedType != null && nestedTypeName != className) {
                    sb.appendLine()
                    generateClassContent(sb, nestedTypeName, nestedType, allClasses, innerIndent, isInner = true, includeInnerClasses = true)
                }
            }
        }

        sb.appendLine("$indent}")
    }

    private fun generateRecord(
        sb: StringBuilder,
        className: String,
        objectType: ParsedType.ObjectType,
        allClasses: Map<String, ParsedType.ObjectType>,
        indent: String,
        includeInnerClasses: Boolean
    ) {
        val params = objectType.fields.map { (fieldName, fieldType) ->
            val javaType = toJavaType(fieldType)
            val camelName = StringUtils.toCamelCase(fieldName)

            if (options.useJsonProperty) {
                "@JsonProperty(\"$fieldName\") $javaType $camelName"
            } else {
                "$javaType $camelName"
            }
        }.joinToString(",\n$indent    ")

        // Inner classes가 있으면 record body 안에 넣어야 함
        val nestedTypes = if (includeInnerClasses) {
            val directNestedTypes = objectType.fields.values
                .filterIsInstance<ParsedType.ObjectType>()
                .map { it.typeName }
                .toSet()

            val arrayNestedTypes = objectType.fields.values
                .filterIsInstance<ParsedType.ArrayType>()
                .mapNotNull { (it.elementType as? ParsedType.ObjectType)?.typeName }
                .toSet()

            (directNestedTypes + arrayNestedTypes).filter { it != className }
        } else {
            emptyList()
        }

        sb.appendLine("${indent}public record $className(")
        sb.appendLine("$indent    $params")

        if (nestedTypes.isEmpty()) {
            sb.appendLine("$indent) {}")
        } else {
            sb.appendLine("$indent) {")
            val innerIndent = "$indent    "

            for (nestedTypeName in nestedTypes) {
                val nestedType = allClasses[nestedTypeName]
                if (nestedType != null) {
                    sb.appendLine()
                    generateRecord(sb, nestedTypeName, nestedType, allClasses, innerIndent, includeInnerClasses = true)
                }
            }

            sb.appendLine("$indent}")
        }
    }

    private fun generateAllArgsConstructor(sb: StringBuilder, className: String, objectType: ParsedType.ObjectType, indent: String) {
        val params = objectType.fields.map { (fieldName, fieldType) ->
            "${toJavaType(fieldType)} ${StringUtils.toCamelCase(fieldName)}"
        }.joinToString(", ")

        sb.appendLine("${indent}public $className($params) {")
        for (fieldName in objectType.fields.keys) {
            val camelName = StringUtils.toCamelCase(fieldName)
            sb.appendLine("$indent    this.$camelName = $camelName;")
        }
        sb.appendLine("$indent}")
    }

    private fun generateGetters(sb: StringBuilder, objectType: ParsedType.ObjectType, indent: String) {
        for ((fieldName, fieldType) in objectType.fields) {
            val javaType = toJavaType(fieldType)
            val camelName = StringUtils.toCamelCase(fieldName)
            val pascalName = StringUtils.toPascalCase(fieldName)

            val getterPrefix = if (fieldType == ParsedType.BOOLEAN) "is" else "get"
            sb.appendLine("${indent}public $javaType $getterPrefix$pascalName() {")
            sb.appendLine("$indent    return $camelName;")
            sb.appendLine("$indent}")
            sb.appendLine()
        }
    }

    private fun generateSetters(sb: StringBuilder, objectType: ParsedType.ObjectType, indent: String) {
        for ((fieldName, fieldType) in objectType.fields) {
            val javaType = toJavaType(fieldType)
            val camelName = StringUtils.toCamelCase(fieldName)
            val pascalName = StringUtils.toPascalCase(fieldName)

            sb.appendLine("${indent}public void set$pascalName($javaType $camelName) {")
            sb.appendLine("$indent    this.$camelName = $camelName;")
            sb.appendLine("$indent}")
            sb.appendLine()
        }
    }

    private fun toJavaType(type: ParsedType): String {
        return when (type) {
            is ParsedType.Primitive -> type.typeName
            is ParsedType.ArrayType -> "List<${toJavaType(type.elementType)}>"
            is ParsedType.ObjectType -> type.typeName
        }
    }
}
