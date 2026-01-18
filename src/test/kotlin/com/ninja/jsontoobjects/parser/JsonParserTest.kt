//package com.ninja.jsontoobjects.parser
//
//import com.ninja.jsontoobjects.model.ParsedType
//import org.junit.jupiter.api.Assertions.*
//import org.junit.jupiter.api.DisplayName
//import org.junit.jupiter.api.Nested
//import org.junit.jupiter.api.Test
//
//class JsonParserTest {
//
//    private val parser = JsonParser()
//
//    @Nested
//    @DisplayName("기본 파싱 테스트")
//    inner class BasicParsingTests {
//
//        @Test
//        @DisplayName("단순 객체 파싱")
//        fun parseSimpleObject() {
//            val json = """{"name": "홍길동", "age": 30}"""
//            val result = parser.parse(json, "Person")
//
//            assertEquals(1, result.allClasses.size)
//            val person = result.allClasses["Person"]!!
//            assertEquals(2, person.fields.size)
//            assertEquals(ParsedType.STRING, person.fields["name"])
//            assertEquals(ParsedType.INT, person.fields["age"])
//        }
//
//        @Test
//        @DisplayName("중첩 객체 파싱")
//        fun parseNestedObject() {
//            val json = """{"user": {"name": "철수", "age": 25}}"""
//            val result = parser.parse(json, "Response")
//
//            assertTrue(result.allClasses.containsKey("Response"))
//            assertTrue(result.allClasses.containsKey("User"))
//        }
//
//        @Test
//        @DisplayName("배열 파싱")
//        fun parseArray() {
//            val json = """{"items": [1, 2, 3]}"""
//            val result = parser.parse(json, "Data")
//
//            val data = result.allClasses["Data"]!!
//            val itemsType = data.fields["items"]
//            assertTrue(itemsType is ParsedType.ArrayType)
//            assertEquals(ParsedType.INT, (itemsType as ParsedType.ArrayType).elementType)
//        }
//    }
//
//    @Nested
//    @DisplayName("케이스 1: 동적 키 Map 구조")
//    inner class DynamicKeyMapTests {
//
//        @Test
//        @DisplayName("동적 사용자 ID 키로 구성된 Map")
//        fun parseDynamicUserIds() {
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
//            val result = parser.parse(json, "Response")
//
//            // Response 클래스가 존재해야 함
//            assertTrue(result.allClasses.containsKey("Response"))
//            val response = result.allClasses["Response"]!!
//            assertTrue(response.fields.containsKey("users"))
//
//            // users 필드는 객체 타입
//            val usersType = response.fields["users"]
//            assertTrue(usersType is ParsedType.ObjectType)
//
//            // 동적 키들이 필드로 파싱됨 (현재 구현의 동작)
//            val usersObject = usersType as ParsedType.ObjectType
//            assertEquals(3, usersObject.fields.size)
//            assertTrue(usersObject.fields.containsKey("u_19283"))
//            assertTrue(usersObject.fields.containsKey("u_84711"))
//            assertTrue(usersObject.fields.containsKey("u_aaaaa"))
//        }
//    }
//
//    @Nested
//    @DisplayName("케이스 2: 같은 필드, 다른 타입 (Union Type)")
//    inner class MixedTypeFieldTests {
//
//        @Test
//        @DisplayName("value 필드가 여러 타입을 가지는 경우")
//        fun parseMixedValueTypes() {
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
//            val result = parser.parse(json, "Response")
//
//            assertTrue(result.allClasses.containsKey("Response"))
//            // singularize("result") = "result" (doesn't end with s after other rules)
//            assertTrue(result.allClasses.containsKey("Result"))
//
//            val resultClass = result.allClasses["Result"]!!
//            assertTrue(resultClass.fields.containsKey("id"))
//            assertTrue(resultClass.fields.containsKey("value"))
//
//            // value 필드는 mixed type이므로 ANY(Object)로 처리되어야 함
//            val valueType = resultClass.fields["value"]
//            assertEquals(ParsedType.ANY, valueType)
//        }
//
//        @Test
//        @DisplayName("숫자 타입 간의 병합 (Int + Long -> Long)")
//        fun parseNumericTypeMerging() {
//            val json = """
//            {
//              "numbers": [
//                { "value": 100 },
//                { "value": 9999999999999 }
//              ]
//            }
//            """.trimIndent()
//
//            val result = parser.parse(json, "Data")
//            // singularize("numbers") = "number"
//            val numberClass = result.allClasses["Number"]!!
//            val valueType = numberClass.fields["value"]
//
//            // Int와 Long이 섞이면 Long으로 병합
//            assertEquals(ParsedType.LONG, valueType)
//        }
//
//        @Test
//        @DisplayName("숫자 타입 간의 병합 (Int + Double -> Double)")
//        fun parseNumericTypeWithDouble() {
//            val json = """
//            {
//              "numbers": [
//                { "value": 100 },
//                { "value": 3.14 }
//              ]
//            }
//            """.trimIndent()
//
//            val result = parser.parse(json, "Data")
//            // singularize("numbers") = "number"
//            val numberClass = result.allClasses["Number"]!!
//            val valueType = numberClass.fields["value"]
//
//            // Int와 Double이 섞이면 Double로 병합
//            assertEquals(ParsedType.DOUBLE, valueType)
//        }
//    }
//
//    @Nested
//    @DisplayName("케이스 3: 같은 이름, 다른 구조")
//    inner class SameNameDifferentStructureTests {
//
//        @Test
//        @DisplayName("items가 배열과 객체로 동시에 존재")
//        fun parseItemsAsArrayAndObject() {
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
//            val result = parser.parse(json, "Response")
//
//            assertTrue(result.allClasses.containsKey("Response"))
//            assertTrue(result.allClasses.containsKey("Next"))
//
//            val response = result.allClasses["Response"]!!
//            val next = result.allClasses["Next"]!!
//
//            // Response.items는 배열
//            val responseItems = response.fields["items"]
//            assertTrue(responseItems is ParsedType.ArrayType)
//
//            // Next.items는 객체
//            val nextItems = next.fields["items"]
//            assertTrue(nextItems is ParsedType.ObjectType)
//
//            // cursor 필드 확인
//            assertEquals(ParsedType.STRING, next.fields["cursor"])
//        }
//    }
//
//    @Nested
//    @DisplayName("케이스 4: 날짜 문자열을 키로 사용")
//    inner class DateKeyTests {
//
//        @Test
//        @DisplayName("날짜를 키로 사용하는 Map 구조")
//        fun parseDateKeys() {
//            val json = """
//            {
//              "2026-01-01": { "sales": 10 },
//              "2026-01-02": { "sales": 20 },
//              "2026-01-03": { "sales": 0 }
//            }
//            """.trimIndent()
//
//            val result = parser.parse(json, "SalesData")
//
//            assertTrue(result.allClasses.containsKey("SalesData"))
//            val salesData = result.allClasses["SalesData"]!!
//
//            // 날짜 키들이 필드로 파싱됨
//            assertEquals(3, salesData.fields.size)
//            assertTrue(salesData.fields.containsKey("2026-01-01"))
//            assertTrue(salesData.fields.containsKey("2026-01-02"))
//            assertTrue(salesData.fields.containsKey("2026-01-03"))
//
//            // 각 필드는 ObjectType
//            salesData.fields.values.forEach { fieldType ->
//                assertTrue(fieldType is ParsedType.ObjectType)
//            }
//        }
//    }
//
//    @Nested
//    @DisplayName("케이스 5: 다형성 배열 (Polymorphic Array)")
//    inner class PolymorphicArrayTests {
//
//        @Test
//        @DisplayName("서로 다른 구조의 이벤트 객체들")
//        fun parsePolymorphicEvents() {
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
//            val result = parser.parse(json, "EventLog")
//
//            assertTrue(result.allClasses.containsKey("EventLog"))
//            // singularize("events") = "event"
//            assertTrue(result.allClasses.containsKey("Event"))
//
//            val event = result.allClasses["Event"]!!
//
//            // 모든 이벤트의 필드가 병합됨
//            assertTrue(event.fields.containsKey("type"))
//            assertEquals(ParsedType.STRING, event.fields["type"])
//
//            // CLICK 이벤트 필드
//            assertTrue(event.fields.containsKey("x"))
//            assertTrue(event.fields.containsKey("y"))
//
//            // PURCHASE 이벤트 필드
//            assertTrue(event.fields.containsKey("orderId"))
//            assertTrue(event.fields.containsKey("amount"))
//
//            // LOGIN 이벤트 필드
//            assertTrue(event.fields.containsKey("provider"))
//
//            // 총 필드 수: type, x, y, orderId, amount, provider = 6개
//            assertEquals(6, event.fields.size)
//        }
//    }
//
//    @Nested
//    @DisplayName("엣지 케이스")
//    inner class EdgeCaseTests {
//
//        @Test
//        @DisplayName("빈 배열")
//        fun parseEmptyArray() {
//            val json = """{"items": []}"""
//            val result = parser.parse(json, "Data")
//
//            val data = result.allClasses["Data"]!!
//            val itemsType = data.fields["items"]
//            assertTrue(itemsType is ParsedType.ArrayType)
//            assertEquals(ParsedType.ANY, (itemsType as ParsedType.ArrayType).elementType)
//        }
//
//        @Test
//        @DisplayName("null 값")
//        fun parseNullValue() {
//            val json = """{"value": null}"""
//            val result = parser.parse(json, "Data")
//
//            val data = result.allClasses["Data"]!!
//            assertEquals(ParsedType.ANY, data.fields["value"])
//        }
//
//        @Test
//        @DisplayName("깊은 중첩 구조")
//        fun parseDeepNesting() {
//            val json = """
//            {
//              "level1": {
//                "level2": {
//                  "level3": {
//                    "value": "deep"
//                  }
//                }
//              }
//            }
//            """.trimIndent()
//
//            val result = parser.parse(json, "Root")
//
//            assertTrue(result.allClasses.containsKey("Root"))
//            assertTrue(result.allClasses.containsKey("Level1"))
//            assertTrue(result.allClasses.containsKey("Level2"))
//            assertTrue(result.allClasses.containsKey("Level3"))
//        }
//
//        @Test
//        @DisplayName("루트가 배열인 경우")
//        fun parseRootArray() {
//            val json = """[{"id": 1}, {"id": 2}]"""
//            val result = parser.parse(json, "Item")
//
//            val rootType = result.rootType
//            assertTrue(rootType is ParsedType.ArrayType)
//        }
//    }
//}
