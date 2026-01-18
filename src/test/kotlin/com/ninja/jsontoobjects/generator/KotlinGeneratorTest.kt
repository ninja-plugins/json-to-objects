//package com.ninja.jsontoobjects.generator
//
//import com.ninja.jsontoobjects.model.KotlinOptions
//import com.ninja.jsontoobjects.model.StructureMode
//import com.ninja.jsontoobjects.parser.JsonParser
//import org.junit.jupiter.api.Assertions.*
//import org.junit.jupiter.api.DisplayName
//import org.junit.jupiter.api.Nested
//import org.junit.jupiter.api.Test
//
//class KotlinGeneratorTest {
//
//    private val parser = JsonParser()
//
//    @Nested
//    @DisplayName("케이스 1: 동적 키 Map 구조")
//    inner class DynamicKeyMapTests {
//
//        @Test
//        @DisplayName("동적 사용자 ID 키 - data class")
//        fun generateDynamicUserIds() {
//            val json = """
//            {
//              "users": {
//                "u_19283": { "name": "철수", "age": 31 },
//                "u_84711": { "name": "영희", "age": 28 },
//                "u_aaaaa": { "name": "민수", "age": 40 }
//              }
//            }
//            """.trimIndent()
//
//            val parseResult = parser.parse(json, "Response")
//            val generator = KotlinGenerator(KotlinOptions())
//            val files = generator.generate(parseResult, "Response")
//
//            assertEquals(1, files.size)
//            val code = files["Response.kt"]!!
//
//            assertTrue(code.contains("data class Response"))
//            assertTrue(code.contains("val users: Users"))
//        }
//    }
//
//    @Nested
//    @DisplayName("케이스 2: 같은 필드, 다른 타입")
//    inner class MixedTypeFieldTests {
//
//        @Test
//        @DisplayName("mixed type은 Any로 생성")
//        fun generateMixedTypeAsAny() {
//            val json = """
//            {
//              "result": [
//                { "id": 1, "value": 123 },
//                { "id": 2, "value": "123" },
//                { "id": 3, "value": { "raw": "123", "parsed": 123 } },
//                { "id": 4, "value": null }
//              ]
//            }
//            """.trimIndent()
//
//            val parseResult = parser.parse(json, "Response")
//            val generator = KotlinGenerator(KotlinOptions())
//            val files = generator.generate(parseResult, "Response")
//
//            val code = files["Response.kt"]!!
//
//            // value 필드가 Any 타입으로 생성되어야 함
//            assertTrue(code.contains("val value: Any"))
//        }
//    }
//
//    @Nested
//    @DisplayName("케이스 3: 같은 이름, 다른 구조")
//    inner class SameNameDifferentStructureTests {
//
//        @Test
//        @DisplayName("items가 배열과 객체로 각각 생성")
//        fun generateItemsAsListAndObject() {
//            val json = """
//            {
//              "items": [
//                { "name": "A", "price": 1000 },
//                { "name": "B", "price": 2000 }
//              ],
//              "next": {
//                "cursor": "abc",
//                "items": {
//                  "name": "C",
//                  "price": 3000
//                }
//              }
//            }
//            """.trimIndent()
//
//            val parseResult = parser.parse(json, "Response")
//            val generator = KotlinGenerator(KotlinOptions())
//            val files = generator.generate(parseResult, "Response")
//
//            val code = files["Response.kt"]!!
//
//            // Response에는 List<Item> (singularize 적용됨)
//            assertTrue(code.contains("val items: List<Item>"))
//            // Next 클래스도 존재
//            assertTrue(code.contains("data class Next"))
//        }
//    }
//
//    @Nested
//    @DisplayName("케이스 4: 날짜 문자열 키")
//    inner class DateKeyTests {
//
//        @Test
//        @DisplayName("날짜 키가 유효한 Kotlin 필드명으로 변환")
//        fun generateDateKeysWithJsonProperty() {
//            val json = """
//            {
//              "2026-01-01": { "sales": 10 },
//              "2026-01-02": { "sales": 20 }
//            }
//            """.trimIndent()
//
//            val parseResult = parser.parse(json, "SalesData")
//            val generator = KotlinGenerator(KotlinOptions(useJsonProperty = true))
//            val files = generator.generate(parseResult, "SalesData")
//
//            val code = files["SalesData.kt"]!!
//
//            // @JsonProperty로 원본 키 보존
//            assertTrue(code.contains("@JsonProperty(\"2026-01-01\")"))
//            assertTrue(code.contains("@JsonProperty(\"2026-01-02\")"))
//        }
//    }
//
//    @Nested
//    @DisplayName("케이스 5: 다형성 배열")
//    inner class PolymorphicArrayTests {
//
//        @Test
//        @DisplayName("모든 이벤트 필드가 병합된 data class 생성")
//        fun generateMergedEventDataClass() {
//            val json = """
//            {
//              "events": [
//                { "type": "CLICK", "x": 10, "y": 20 },
//                { "type": "PURCHASE", "orderId": "O-123", "amount": 59000 },
//                { "type": "LOGIN", "provider": "kakao" }
//              ]
//            }
//            """.trimIndent()
//
//            val parseResult = parser.parse(json, "EventLog")
//            val generator = KotlinGenerator(KotlinOptions())
//            val files = generator.generate(parseResult, "EventLog")
//
//            val code = files["EventLog.kt"]!!
//
//            // 모든 필드가 포함되어야 함
//            assertTrue(code.contains("val type: String"))
//            assertTrue(code.contains("val x: Int"))
//            assertTrue(code.contains("val y: Int"))
//            assertTrue(code.contains("val orderId: String"))
//            assertTrue(code.contains("val amount: Int"))
//            assertTrue(code.contains("val provider: String"))
//        }
//    }
//
//    @Nested
//    @DisplayName("기본 생성 테스트")
//    inner class BasicGenerationTests {
//
//        @Test
//        @DisplayName("단순 data class 생성")
//        fun generateSimpleDataClass() {
//            val json = """{"name": "test", "age": 30}"""
//            val parseResult = parser.parse(json, "Person")
//            val generator = KotlinGenerator(KotlinOptions())
//            val files = generator.generate(parseResult, "Person")
//
//            val code = files["Person.kt"]!!
//            assertTrue(code.contains("data class Person"))
//            assertTrue(code.contains("val name: String"))
//            assertTrue(code.contains("val age: Int"))
//        }
//
//        @Test
//        @DisplayName("중첩 객체 data class 생성")
//        fun generateNestedDataClass() {
//            val json = """{"user": {"name": "test", "profile": {"bio": "hello"}}}"""
//            val parseResult = parser.parse(json, "Response")
//            val generator = KotlinGenerator(KotlinOptions())
//            val files = generator.generate(parseResult, "Response")
//
//            val code = files["Response.kt"]!!
//            assertTrue(code.contains("data class Response"))
//            assertTrue(code.contains("data class User"))
//            assertTrue(code.contains("data class Profile"))
//        }
//
//        @Test
//        @DisplayName("배열 필드 생성")
//        fun generateListField() {
//            val json = """{"items": [{"id": 1}, {"id": 2}]}"""
//            val parseResult = parser.parse(json, "Response")
//            val generator = KotlinGenerator(KotlinOptions())
//            val files = generator.generate(parseResult, "Response")
//
//            val code = files["Response.kt"]!!
//            // singularize("items") = "item"
//            assertTrue(code.contains("val items: List<Item>"))
//        }
//    }
//
//    @Nested
//    @DisplayName("Kotlin 타입 매핑 테스트")
//    inner class TypeMappingTests {
//
//        @Test
//        @DisplayName("Int 타입")
//        fun generateIntType() {
//            val json = """{"count": 42}"""
//            val parseResult = parser.parse(json, "Data")
//            val generator = KotlinGenerator(KotlinOptions())
//            val files = generator.generate(parseResult, "Data")
//
//            assertTrue(files["Data.kt"]!!.contains("val count: Int"))
//        }
//
//        @Test
//        @DisplayName("Long 타입")
//        fun generateLongType() {
//            val json = """{"bigNumber": 9999999999999}"""
//            val parseResult = parser.parse(json, "Data")
//            val generator = KotlinGenerator(KotlinOptions())
//            val files = generator.generate(parseResult, "Data")
//
//            assertTrue(files["Data.kt"]!!.contains("val bigNumber: Long"))
//        }
//
//        @Test
//        @DisplayName("Double 타입")
//        fun generateDoubleType() {
//            val json = """{"price": 19.99}"""
//            val parseResult = parser.parse(json, "Data")
//            val generator = KotlinGenerator(KotlinOptions())
//            val files = generator.generate(parseResult, "Data")
//
//            assertTrue(files["Data.kt"]!!.contains("val price: Double"))
//        }
//
//        @Test
//        @DisplayName("Boolean 타입")
//        fun generateBooleanType() {
//            val json = """{"active": true}"""
//            val parseResult = parser.parse(json, "Data")
//            val generator = KotlinGenerator(KotlinOptions())
//            val files = generator.generate(parseResult, "Data")
//
//            assertTrue(files["Data.kt"]!!.contains("val active: Boolean"))
//        }
//
//        @Test
//        @DisplayName("null은 Any 타입")
//        fun generateAnyTypeForNull() {
//            val json = """{"value": null}"""
//            val parseResult = parser.parse(json, "Data")
//            val generator = KotlinGenerator(KotlinOptions())
//            val files = generator.generate(parseResult, "Data")
//
//            assertTrue(files["Data.kt"]!!.contains("val value: Any"))
//        }
//    }
//
//    @Nested
//    @DisplayName("Structure Mode 테스트")
//    inner class StructureModeTests {
//
//        @Test
//        @DisplayName("Single File 모드 (기본)")
//        fun generateSingleFile() {
//            val json = """{"user": {"name": "test"}}"""
//            val parseResult = parser.parse(json, "Response")
//            val generator = KotlinGenerator(KotlinOptions(structureMode = StructureMode.SEPARATE_CLASSES))
//            val files = generator.generate(parseResult, "Response")
//
//            assertEquals(1, files.size)
//            assertTrue(files.containsKey("Response.kt"))
//
//            val code = files["Response.kt"]!!
//            assertTrue(code.contains("data class Response"))
//            assertTrue(code.contains("data class User"))
//        }
//
//        @Test
//        @DisplayName("Multiple Files 모드")
//        fun generateMultipleFiles() {
//            val json = """{"user": {"name": "test"}}"""
//            val parseResult = parser.parse(json, "Response")
//            val generator = KotlinGenerator(KotlinOptions(structureMode = StructureMode.MULTIPLE_FILES))
//            val files = generator.generate(parseResult, "Response")
//
//            assertEquals(2, files.size)
//            assertTrue(files.containsKey("Response.kt"))
//            assertTrue(files.containsKey("User.kt"))
//        }
//    }
//
//    @Nested
//    @DisplayName("@JsonProperty 테스트")
//    inner class JsonPropertyTests {
//
//        @Test
//        @DisplayName("snake_case 필드에 @JsonProperty 추가")
//        fun generateJsonPropertyForSnakeCase() {
//            val json = """{"user_name": "test", "created_at": "2024-01-01"}"""
//            val parseResult = parser.parse(json, "User")
//            val generator = KotlinGenerator(KotlinOptions(useJsonProperty = true))
//            val files = generator.generate(parseResult, "User")
//
//            val code = files["User.kt"]!!
//            assertTrue(code.contains("import com.fasterxml.jackson.annotation.JsonProperty"))
//            assertTrue(code.contains("@JsonProperty(\"user_name\")"))
//            assertTrue(code.contains("@JsonProperty(\"created_at\")"))
//            assertTrue(code.contains("val userName: String"))
//            assertTrue(code.contains("val createdAt: String"))
//        }
//
//        @Test
//        @DisplayName("@JsonProperty 없이 생성")
//        fun generateWithoutJsonProperty() {
//            val json = """{"user_name": "test"}"""
//            val parseResult = parser.parse(json, "User")
//            val generator = KotlinGenerator(KotlinOptions(useJsonProperty = false))
//            val files = generator.generate(parseResult, "User")
//
//            val code = files["User.kt"]!!
//            assertFalse(code.contains("@JsonProperty"))
//            assertFalse(code.contains("import com.fasterxml.jackson.annotation.JsonProperty"))
//        }
//    }
//
//    @Nested
//    @DisplayName("포맷팅 테스트")
//    inner class FormattingTests {
//
//        @Test
//        @DisplayName("필드가 3개 이하면 한 줄로 생성")
//        fun generateSingleLineForFewFields() {
//            val json = """{"a": 1, "b": 2}"""
//            val parseResult = parser.parse(json, "Data")
//            val generator = KotlinGenerator(KotlinOptions())
//            val files = generator.generate(parseResult, "Data")
//
//            val code = files["Data.kt"]!!
//            // 한 줄 포맷: data class Data(val a: Int, val b: Int)
//            assertTrue(code.contains("data class Data(val a: Int, val b: Int)"))
//        }
//
//        @Test
//        @DisplayName("필드가 4개 이상이면 여러 줄로 생성")
//        fun generateMultiLineForManyFields() {
//            val json = """{"a": 1, "b": 2, "c": 3, "d": 4}"""
//            val parseResult = parser.parse(json, "Data")
//            val generator = KotlinGenerator(KotlinOptions())
//            val files = generator.generate(parseResult, "Data")
//
//            val code = files["Data.kt"]!!
//            // 여러 줄 포맷
//            assertTrue(code.contains("data class Data("))
//            assertTrue(code.contains("    val a: Int"))
//        }
//
//        @Test
//        @DisplayName("빈 객체는 필드 타입으로만 참조됨")
//        fun generateEmptyObjectReference() {
//            val json = """{"empty": {}}"""
//            val parseResult = parser.parse(json, "Response")
//
//            // 빈 객체는 allClasses에 추가되지 않음 (필드가 없으면 추가 안 함)
//            assertFalse(parseResult.allClasses.containsKey("Empty"))
//
//            val generator = KotlinGenerator(KotlinOptions())
//            val files = generator.generate(parseResult, "Response")
//
//            val code = files["Response.kt"]!!
//            // Response에 empty 필드가 Empty 타입으로 존재
//            assertTrue(code.contains("val empty: Empty"))
//        }
//    }
//}
