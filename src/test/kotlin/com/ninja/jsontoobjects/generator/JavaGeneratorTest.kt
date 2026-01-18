//package com.ninja.jsontoobjects.generator
//
//import com.ninja.jsontoobjects.model.JavaOptions
//import com.ninja.jsontoobjects.model.StructureMode
//import com.ninja.jsontoobjects.parser.JsonParser
//import org.junit.jupiter.api.Assertions.*
//import org.junit.jupiter.api.DisplayName
//import org.junit.jupiter.api.Nested
//import org.junit.jupiter.api.Test
//
//class JavaGeneratorTest {
//
//    private val parser = JsonParser()
//
//    @Nested
//    @DisplayName("케이스 1: 동적 키 Map 구조")
//    inner class DynamicKeyMapTests {
//
//        @Test
//        @DisplayName("동적 사용자 ID 키 - Lombok")
//        fun generateDynamicUserIdsWithLombok() {
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
//            val generator = JavaGenerator(JavaOptions(useData = true))
//            val files = generator.generate(parseResult, "Response")
//
//            assertEquals(1, files.size)
//            val code = files["Response.java"]!!
//
//            assertTrue(code.contains("@Data"))
//            assertTrue(code.contains("class Response"))
//            assertTrue(code.contains("private Users users"))
//        }
//    }
//
//    @Nested
//    @DisplayName("케이스 2: 같은 필드, 다른 타입")
//    inner class MixedTypeFieldTests {
//
//        @Test
//        @DisplayName("mixed type은 Object로 생성")
//        fun generateMixedTypeAsObject() {
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
//            val generator = JavaGenerator(JavaOptions(useData = true))
//            val files = generator.generate(parseResult, "Response")
//
//            val code = files["Response.java"]!!
//
//            // value 필드가 Object 타입으로 생성되어야 함
//            assertTrue(code.contains("private Object value"))
//        }
//    }
//
//    @Nested
//    @DisplayName("케이스 3: 같은 이름, 다른 구조")
//    inner class SameNameDifferentStructureTests {
//
//        @Test
//        @DisplayName("items가 배열과 객체로 각각 생성")
//        fun generateItemsAsArrayAndObject() {
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
//            val generator = JavaGenerator(JavaOptions(useData = true))
//            val files = generator.generate(parseResult, "Response")
//
//            val code = files["Response.java"]!!
//
//            // Response에는 List<Item> (singularize가 적용됨)
//            assertTrue(code.contains("List<Item> items"))
//            // Next에는 Items items (단일 객체, singularize 안 됨)
//            assertTrue(code.contains("class Next"))
//        }
//    }
//
//    @Nested
//    @DisplayName("케이스 4: 날짜 문자열 키")
//    inner class DateKeyTests {
//
//        @Test
//        @DisplayName("날짜 키가 유효한 Java 필드명으로 변환")
//        fun generateDateKeysAsValidFieldNames() {
//            val json = """
//            {
//              "2026-01-01": { "sales": 10 },
//              "2026-01-02": { "sales": 20 }
//            }
//            """.trimIndent()
//
//            val parseResult = parser.parse(json, "SalesData")
//            val generator = JavaGenerator(JavaOptions(useData = true, useJsonProperty = true))
//            val files = generator.generate(parseResult, "SalesData")
//
//            val code = files["SalesData.java"]!!
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
//        @DisplayName("모든 이벤트 필드가 병합된 클래스 생성")
//        fun generateMergedEventClass() {
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
//            val generator = JavaGenerator(JavaOptions(useData = true))
//            val files = generator.generate(parseResult, "EventLog")
//
//            val code = files["EventLog.java"]!!
//
//            // 모든 필드가 포함되어야 함
//            assertTrue(code.contains("private String type"))
//            assertTrue(code.contains("private Integer x"))
//            assertTrue(code.contains("private Integer y"))
//            assertTrue(code.contains("private String orderId"))
//            assertTrue(code.contains("private Integer amount"))
//            assertTrue(code.contains("private String provider"))
//        }
//    }
//
//    @Nested
//    @DisplayName("Lombok 옵션 테스트")
//    inner class LombokOptionsTests {
//
//        @Test
//        @DisplayName("@Data 어노테이션")
//        fun generateWithDataAnnotation() {
//            val json = """{"name": "test"}"""
//            val parseResult = parser.parse(json, "Sample")
//            val generator = JavaGenerator(JavaOptions(useData = true))
//            val files = generator.generate(parseResult, "Sample")
//
//            val code = files["Sample.java"]!!
//            assertTrue(code.contains("@Data"))
//            assertTrue(code.contains("import lombok.Data;"))
//        }
//
//        @Test
//        @DisplayName("@Getter/@Setter 어노테이션")
//        fun generateWithGetterSetterAnnotations() {
//            val json = """{"name": "test"}"""
//            val parseResult = parser.parse(json, "Sample")
//            val generator = JavaGenerator(JavaOptions(
//                useData = false,
//                useGetter = true,
//                useSetter = true
//            ))
//            val files = generator.generate(parseResult, "Sample")
//
//            val code = files["Sample.java"]!!
//            assertTrue(code.contains("@Getter"))
//            assertTrue(code.contains("@Setter"))
//            assertFalse(code.contains("@Data"))
//        }
//
//        @Test
//        @DisplayName("@NoArgsConstructor/@AllArgsConstructor 어노테이션")
//        fun generateWithConstructorAnnotations() {
//            val json = """{"name": "test"}"""
//            val parseResult = parser.parse(json, "Sample")
//            val generator = JavaGenerator(JavaOptions(
//                useNoArgsConstructor = true,
//                useAllArgsConstructor = true
//            ))
//            val files = generator.generate(parseResult, "Sample")
//
//            val code = files["Sample.java"]!!
//            assertTrue(code.contains("@NoArgsConstructor"))
//            assertTrue(code.contains("@AllArgsConstructor"))
//        }
//    }
//
//    @Nested
//    @DisplayName("수동 생성 옵션 테스트")
//    inner class ManualGenerationTests {
//
//        @Test
//        @DisplayName("Lombok 없이 Getter/Setter 수동 생성")
//        fun generateManualGetterSetter() {
//            val json = """{"name": "test", "age": 30}"""
//            val parseResult = parser.parse(json, "Person")
//            val generator = JavaGenerator(JavaOptions(
//                useData = false,
//                useGetter = false,
//                useSetter = false,
//                useNoArgsConstructor = false,
//                useAllArgsConstructor = false,
//                generateGetter = true,
//                generateSetter = true,
//                generateNoArgsConstructor = true
//            ))
//            val files = generator.generate(parseResult, "Person")
//
//            val code = files["Person.java"]!!
//
//            // Lombok 어노테이션 없음
//            assertFalse(code.contains("@Data"))
//            assertFalse(code.contains("@Getter"))
//            assertFalse(code.contains("@Setter"))
//
//            // 수동 생성된 메서드
//            assertTrue(code.contains("public String getName()"))
//            assertTrue(code.contains("public void setName(String name)"))
//            assertTrue(code.contains("public Integer getAge()"))
//            assertTrue(code.contains("public void setAge(Integer age)"))
//            assertTrue(code.contains("public Person() {}"))
//        }
//    }
//
//    @Nested
//    @DisplayName("Java Record 테스트")
//    inner class JavaRecordTests {
//
//        @Test
//        @DisplayName("Record로 생성")
//        fun generateAsRecord() {
//            val json = """{"name": "test", "age": 30}"""
//            val parseResult = parser.parse(json, "Person")
//            val generator = JavaGenerator(JavaOptions(useRecord = true))
//            val files = generator.generate(parseResult, "Person")
//
//            val code = files["Person.java"]!!
//            assertTrue(code.contains("public record Person("))
//            assertTrue(code.contains("String name"))
//            assertTrue(code.contains("Integer age"))
//        }
//
//        @Test
//        @DisplayName("Record + @JsonProperty")
//        fun generateRecordWithJsonProperty() {
//            val json = """{"user_name": "test"}"""
//            val parseResult = parser.parse(json, "User")
//            val generator = JavaGenerator(JavaOptions(
//                useRecord = true,
//                useJsonProperty = true
//            ))
//            val files = generator.generate(parseResult, "User")
//
//            val code = files["User.java"]!!
//            assertTrue(code.contains("public record User("))
//            assertTrue(code.contains("@JsonProperty(\"user_name\")"))
//        }
//
//        @Test
//        @DisplayName("Record + 중첩 객체 (Inner Class 모드)")
//        fun generateRecordWithNestedObjects() {
//            val json = """
//            {
//              "user": {
//                "name": "test",
//                "address": {
//                  "city": "Seoul",
//                  "zip": "12345"
//                }
//              }
//            }
//            """.trimIndent()
//
//            val parseResult = parser.parse(json, "Response")
//            val generator = JavaGenerator(JavaOptions(
//                useRecord = true,
//                structureMode = StructureMode.INNER_CLASS
//            ))
//            val files = generator.generate(parseResult, "Response")
//
//            assertEquals(1, files.size)
//            val code = files["Response.java"]!!
//
//            // 모든 클래스가 record로 생성되어야 함
//            assertTrue(code.contains("public record Response("), "Response record 없음")
//            assertTrue(code.contains("public record User("), "User record 없음")
//            assertTrue(code.contains("public record Address("), "Address record 없음")
//        }
//
//        @Test
//        @DisplayName("Record + Multiple Files 모드")
//        fun generateRecordWithMultipleFiles() {
//            val json = """{"user": {"name": "test"}}"""
//            val parseResult = parser.parse(json, "Response")
//            val generator = JavaGenerator(JavaOptions(
//                useRecord = true,
//                structureMode = StructureMode.MULTIPLE_FILES
//            ))
//            val files = generator.generate(parseResult, "Response")
//
//            assertEquals(2, files.size)
//            assertTrue(files.containsKey("Response.java"))
//            assertTrue(files.containsKey("User.java"))
//
//            assertTrue(files["Response.java"]!!.contains("public record Response("))
//            assertTrue(files["User.java"]!!.contains("public record User("))
//        }
//    }
//
//    @Nested
//    @DisplayName("Structure Mode 테스트")
//    inner class StructureModeTests {
//
//        @Test
//        @DisplayName("Inner Class 모드")
//        fun generateAsInnerClass() {
//            val json = """{"user": {"name": "test"}}"""
//            val parseResult = parser.parse(json, "Response")
//            val generator = JavaGenerator(JavaOptions(structureMode = StructureMode.INNER_CLASS))
//            val files = generator.generate(parseResult, "Response")
//
//            assertEquals(1, files.size)
//            val code = files["Response.java"]!!
//            assertTrue(code.contains("public class Response"))
//            assertTrue(code.contains("public static class User"))
//        }
//
//        @Test
//        @DisplayName("Separate Classes 모드 (같은 파일)")
//        fun generateAsSeparateClassesSameFile() {
//            val json = """{"user": {"name": "test"}}"""
//            val parseResult = parser.parse(json, "Response")
//            val generator = JavaGenerator(JavaOptions(structureMode = StructureMode.SEPARATE_CLASSES))
//            val files = generator.generate(parseResult, "Response")
//
//            assertEquals(1, files.size)
//            val code = files["Response.java"]!!
//            // 두 클래스 모두 public class로 선언 (static 없음)
//            assertTrue(code.contains("public class Response"))
//            assertTrue(code.contains("public class User"))
//            assertFalse(code.contains("public static class"))
//        }
//
//        @Test
//        @DisplayName("Multiple Files 모드")
//        fun generateAsMultipleFiles() {
//            val json = """{"user": {"name": "test"}}"""
//            val parseResult = parser.parse(json, "Response")
//            val generator = JavaGenerator(JavaOptions(structureMode = StructureMode.MULTIPLE_FILES))
//            val files = generator.generate(parseResult, "Response")
//
//            assertEquals(2, files.size)
//            assertTrue(files.containsKey("Response.java"))
//            assertTrue(files.containsKey("User.java"))
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
//            val generator = JavaGenerator(JavaOptions(useJsonProperty = true))
//            val files = generator.generate(parseResult, "User")
//
//            val code = files["User.java"]!!
//            assertTrue(code.contains("@JsonProperty(\"user_name\")"))
//            assertTrue(code.contains("@JsonProperty(\"created_at\")"))
//            assertTrue(code.contains("private String userName"))
//            assertTrue(code.contains("private String createdAt"))
//        }
//    }
//}
