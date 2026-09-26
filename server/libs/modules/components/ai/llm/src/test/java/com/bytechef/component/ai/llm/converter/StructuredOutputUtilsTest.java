/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.component.ai.llm.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.ai.util.JacksonUtils;
import tools.jackson.core.type.TypeReference;

/**
 * @author Ivica Cardic
 */
class StructuredOutputUtilsTest {

    private static final String PRODUCT_SCHEMA = """
        {"type":"object","required":[],"properties":{"result":{"type":"array","items":{"type":"object",
        "required":[],"properties":{"name":{"type":"string"},"loyaltyPrice":{"type":"object",
        "required":["amount","currency"],"properties":{"amount":{"type":"number",
        "description":"This is loyalty price value","title":"Amount"},"currency":{"type":"string"}},
        "title":"Loyalty Price"},"productId":{"type":"number","title":"Product Id"}}}}}}
        """;

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "'```json\n{\"a\":1}\n```'|{\"a\":1}",
        "'```JSON\n{\"a\":1}\n```'|{\"a\":1}",
        "'```\n{\"a\":1}\n```'|{\"a\":1}",
        "'  {\"a\":1}  '|{\"a\":1}",
        "'{\"a\":\"```\"}'|{\"a\":\"```\"}",
        "'```'|```"
    })
    void testStripCodeFence(String text, String expected) {
        assertEquals(expected, StructuredOutputUtils.stripCodeFence(text));
    }

    @Test
    void testToStrictJsonSchemaClosesEveryObject() {
        Map<String, Object> schema = toStrictSchemaMap(PRODUCT_SCHEMA);

        assertEquals(false, schema.get("additionalProperties"));
        assertEquals(List.of(), schema.get("required"));

        Map<String, Object> item = getPath(schema, "properties", "result", "items");

        assertEquals(false, item.get("additionalProperties"));

        Map<String, Object> loyaltyPrice = getPath(item, "properties", "loyaltyPrice");

        assertEquals(false, loyaltyPrice.get("additionalProperties"));
        assertEquals(List.of("amount", "currency"), loyaltyPrice.get("required"));
        assertEquals("Loyalty Price", loyaltyPrice.get("title"));

        Map<String, Object> amount = getPath(loyaltyPrice, "properties", "amount");

        assertFalse(amount.containsKey("additionalProperties"));
        assertEquals("This is loyalty price value", amount.get("description"));
    }

    @Test
    void testToStrictJsonSchemaDropsUnsupportedConstraints() {
        Map<String, Object> schema = toStrictSchemaMap("""
            {"type":"object","required":["tags","price","code"],"properties":{
            "tags":{"type":"array","minItems":1,"maxItems":5,"uniqueItems":true,"items":{"type":"string"}},
            "price":{"type":"number","minimum":0,"maximum":100,"multipleOf":0.01},
            "code":{"type":"string","minLength":2,"maxLength":8,"enum":["AB","CD"]}}}
            """);

        assertEquals(Map.of("type", "array", "items", Map.of("type", "string")), getPath(schema, "properties", "tags"));
        assertEquals(Map.of("type", "number"), getPath(schema, "properties", "price"));
        assertEquals(Map.of("type", "string", "enum", List.of("AB", "CD")), getPath(schema, "properties", "code"));
    }

    @Test
    void testToStrictJsonSchemaKeepsPropertiesNamedLikeKeywords() {
        Map<String, Object> schema = toStrictSchemaMap("""
            {"type":"object","properties":{"minimum":{"type":"number"},"maxLength":{"type":"integer"}}}
            """);

        Map<String, Object> properties = getPath(schema, "properties");

        assertEquals(Map.of("type", "number"), properties.get("minimum"));
        assertEquals(Map.of("type", "integer"), properties.get("maxLength"));
    }

    @Test
    void testToStrictJsonSchemaRequiresAllRootPropertiesWhenRequiredIsMissing() {
        Map<String, Object> schema = toStrictSchemaMap("""
            {"type":"object","properties":{"name":{"type":"string"},"nested":{"type":"object",
            "properties":{"value":{"type":"string"}}}}}
            """);

        assertEquals(List.of("name", "nested"), schema.get("required"));
        assertFalse(getPath(schema, "properties", "nested").containsKey("required"));
    }

    @Test
    void testToStrictJsonSchemaClosesObjectsInsideAnyOfAndDefs() {
        Map<String, Object> schema = toStrictSchemaMap("""
            {"type":"object","properties":{"value":{"anyOf":[{"type":"object","properties":{}},{"type":"null"}]},
            "ref":{"$ref":"#/$defs/item"}},"$defs":{"item":{"type":"object","additionalProperties":true}}}
            """);

        Map<String, Object> value = getPath(schema, "properties", "value");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> anyOf = (List<Map<String, Object>>) value.get("anyOf");

        assertEquals(false, anyOf.getFirst()
            .get("additionalProperties"));
        assertFalse(anyOf.get(1)
            .containsKey("additionalProperties"));
        assertEquals(false, getPath(schema, "$defs", "item").get("additionalProperties"));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> getPath(Map<String, Object> schema, String... keys) {
        Map<String, Object> current = schema;

        for (String key : keys) {
            current = (Map<String, Object>) current.get(key);
        }

        return current;
    }

    private static Map<String, Object> toStrictSchemaMap(String jsonSchema) {
        String strictJsonSchema = StructuredOutputUtils.toStrictJsonSchema(jsonSchema);

        return JacksonUtils.getDefaultJsonMapper()
            .readValue(strictJsonSchema, new TypeReference<>() {});
    }
}
