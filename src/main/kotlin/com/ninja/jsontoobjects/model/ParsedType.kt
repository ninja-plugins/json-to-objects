package com.ninja.jsontoobjects.model

sealed class ParsedType {
    abstract val typeName: String

    data class Primitive(override val typeName: String) : ParsedType()

    data class ArrayType(val elementType: ParsedType) : ParsedType() {
        override val typeName: String
            get() = "List<${elementType.typeName}>"
    }

    data class ObjectType(
        override val typeName: String,
        val fields: MutableMap<String, ParsedType> = mutableMapOf()
    ) : ParsedType()

    companion object {
        val STRING = Primitive("String")
        val INT = Primitive("Integer")
        val LONG = Primitive("Long")
        val DOUBLE = Primitive("Double")
        val BOOLEAN = Primitive("Boolean")
        val ANY = Primitive("Object")
    }
}
