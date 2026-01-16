package com.ninja.jsontoobjects.parser

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser as GsonParser
import com.google.gson.JsonPrimitive
import com.ninja.jsontoobjects.model.ParsedType
import com.ninja.jsontoobjects.util.StringUtils

class JsonParser {
    private val nestedClasses = mutableMapOf<String, ParsedType.ObjectType>()

    fun parse(jsonString: String, rootClassName: String): ParseResult {
        nestedClasses.clear()
        val json = GsonParser.parseString(jsonString)
        val rootType = parseElement(json, rootClassName)
        return ParseResult(rootType, nestedClasses.toMap())
    }

    private fun parseElement(element: JsonElement, suggestedName: String): ParsedType {
        return when {
            element.isJsonNull -> ParsedType.ANY
            element.isJsonPrimitive -> parsePrimitive(element.asJsonPrimitive)
            element.isJsonArray -> parseArray(element.asJsonArray, suggestedName)
            element.isJsonObject -> parseObject(element.asJsonObject, suggestedName)
            else -> ParsedType.ANY
        }
    }

    private fun parsePrimitive(primitive: JsonPrimitive): ParsedType {
        return when {
            primitive.isBoolean -> ParsedType.BOOLEAN
            primitive.isNumber -> {
                val num = primitive.asNumber
                val str = num.toString()
                when {
                    str.contains(".") || str.contains("e", ignoreCase = true) -> ParsedType.DOUBLE
                    num.toLong() > Int.MAX_VALUE || num.toLong() < Int.MIN_VALUE -> ParsedType.LONG
                    else -> ParsedType.INT
                }
            }
            primitive.isString -> ParsedType.STRING
            else -> ParsedType.ANY
        }
    }

    private fun parseArray(array: JsonArray, suggestedName: String): ParsedType {
        if (array.isEmpty) {
            return ParsedType.ArrayType(ParsedType.ANY)
        }

        val singularName = StringUtils.singularize(suggestedName)
        val elementTypes = array.map { parseElement(it, singularName) }
        val mergedType = mergeTypes(elementTypes, singularName)

        return ParsedType.ArrayType(mergedType)
    }

    private fun parseObject(obj: JsonObject, className: String): ParsedType {
        val pascalName = StringUtils.toPascalCase(className)
        val objectType = ParsedType.ObjectType(pascalName)

        for ((key, value) in obj.entrySet()) {
            val fieldType = parseElement(value, key)
            objectType.fields[key] = fieldType
        }

        if (objectType.fields.isNotEmpty()) {
            nestedClasses[pascalName] = objectType
        }

        return objectType
    }

    private fun mergeTypes(types: List<ParsedType>, suggestedName: String): ParsedType {
        if (types.isEmpty()) return ParsedType.ANY
        if (types.size == 1) return types.first()

        val distinctTypes = types.distinct()
        if (distinctTypes.size == 1) return distinctTypes.first()

        // All primitives of same base type
        val allPrimitives = distinctTypes.all { it is ParsedType.Primitive }
        if (allPrimitives) {
            val primitiveTypes = distinctTypes.filterIsInstance<ParsedType.Primitive>()
            // Number type coercion: Int -> Long -> Double
            val hasDouble = primitiveTypes.any { it.typeName == "Double" }
            val hasLong = primitiveTypes.any { it.typeName == "Long" }
            val hasInt = primitiveTypes.any { it.typeName == "Integer" }

            if (hasDouble || hasLong || hasInt) {
                return when {
                    hasDouble -> ParsedType.DOUBLE
                    hasLong -> ParsedType.LONG
                    else -> ParsedType.INT
                }
            }
            return ParsedType.ANY
        }

        // All objects - merge their fields
        val allObjects = distinctTypes.all { it is ParsedType.ObjectType }
        if (allObjects) {
            val objects = distinctTypes.filterIsInstance<ParsedType.ObjectType>()
            return mergeObjectTypes(objects, suggestedName)
        }

        return ParsedType.ANY
    }

    private fun mergeObjectTypes(objects: List<ParsedType.ObjectType>, suggestedName: String): ParsedType.ObjectType {
        val pascalName = StringUtils.toPascalCase(suggestedName)
        val mergedFields = mutableMapOf<String, ParsedType>()

        for (obj in objects) {
            for ((fieldName, fieldType) in obj.fields) {
                val existingType = mergedFields[fieldName]
                mergedFields[fieldName] = if (existingType != null) {
                    mergeTypes(listOf(existingType, fieldType), fieldName)
                } else {
                    fieldType
                }
            }
        }

        val mergedType = ParsedType.ObjectType(pascalName, mergedFields)
        nestedClasses[pascalName] = mergedType
        return mergedType
    }
}

data class ParseResult(
    val rootType: ParsedType,
    val allClasses: Map<String, ParsedType.ObjectType>
)
