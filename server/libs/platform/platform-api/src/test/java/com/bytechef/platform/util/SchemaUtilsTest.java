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

package com.bytechef.platform.util;

import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.string;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.ComponentDsl.ModifiableArrayProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableObjectProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableValueProperty;
import com.bytechef.definition.BaseOutputDefinition;
import com.bytechef.definition.BaseProperty;
import com.bytechef.definition.BaseProperty.BaseArrayProperty;
import com.bytechef.definition.BaseProperty.BaseBooleanProperty;
import com.bytechef.definition.BaseProperty.BaseDateProperty;
import com.bytechef.definition.BaseProperty.BaseDateTimeProperty;
import com.bytechef.definition.BaseProperty.BaseIntegerProperty;
import com.bytechef.definition.BaseProperty.BaseNullProperty;
import com.bytechef.definition.BaseProperty.BaseNumberProperty;
import com.bytechef.definition.BaseProperty.BaseObjectProperty;
import com.bytechef.definition.BaseProperty.BaseStringProperty;
import com.bytechef.definition.BaseProperty.BaseTimeProperty;
import com.bytechef.definition.BaseProperty.BaseValueProperty;
import com.bytechef.platform.domain.OutputResponse;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class SchemaUtilsTest {

    private static final TestSchemaPropertyFactory SCHEMA_PROPERTY_FACTORY = new TestSchemaPropertyFactory();

    @SuppressWarnings("unchecked")
    private static final SchemaUtils.JsonSchemaPropertyFactory JSON_SCHEMA_PROPERTY_FACTORY =
        new SchemaUtils.JsonSchemaPropertyFactory() {

            @Override
            public void addChildren(BaseValueProperty<?> property, List<BaseValueProperty<?>> children) {
                if (property instanceof ModifiableArrayProperty modifiableArrayProperty) {
                    modifiableArrayProperty.items(
                        children.stream()
                            .map(child -> (ModifiableValueProperty<?, ?>) child)
                            .toList());
                } else {
                    ((ModifiableObjectProperty) property).properties(
                        children.stream()
                            .map(child -> (ModifiableValueProperty<?, ?>) child)
                            .toList());
                }
            }

            @Override
            public BaseValueProperty<?> create(String name, String type) {
                return switch (type) {
                    case "array" -> array(name);
                    case "boolean" -> bool(name);
                    case "integer" -> integer(name);
                    case "number" -> number(name);
                    case "object" -> object(name);
                    case "string" -> string(name);
                    default -> throw new IllegalArgumentException("Unsupported JSON schema type: " + type);
                };
            }

            @Override
            public List<BaseValueProperty<?>> getChildren(BaseValueProperty<?> property) {
                if (property instanceof ModifiableArrayProperty modifiableArrayProperty) {
                    return (List<BaseValueProperty<?>>) (List<?>) modifiableArrayProperty.getItems()
                        .map(ArrayList::new)
                        .orElseGet(ArrayList::new);
                } else {
                    return (List<BaseValueProperty<?>>) (List<?>) ((ModifiableObjectProperty) property)
                        .getProperties()
                        .map(ArrayList::new)
                        .orElseGet(ArrayList::new);
                }
            }
        };

    // getOutputSchema tests

    @Test
    void testGetOutputSchemaWithNull() {
        BaseProperty result = SchemaUtils.getOutputSchema(null, SCHEMA_PROPERTY_FACTORY);

        assertThat(result).isInstanceOf(BaseNullProperty.class);
    }

    @Test
    void testGetOutputSchemaWithString() {
        BaseProperty result = SchemaUtils.getOutputSchema("testName", "hello", SCHEMA_PROPERTY_FACTORY);

        assertThat(result).isInstanceOf(BaseStringProperty.class);
        assertThat(result.getName()).isEqualTo("testName");
    }

    @Test
    void testGetOutputSchemaWithBoolean() {
        BaseProperty result = SchemaUtils.getOutputSchema("flag", true, SCHEMA_PROPERTY_FACTORY);

        assertThat(result).isInstanceOf(BaseBooleanProperty.class);
        assertThat(result.getName()).isEqualTo("flag");
    }

    @Test
    void testGetOutputSchemaWithInteger() {
        BaseProperty result = SchemaUtils.getOutputSchema("count", 42, SCHEMA_PROPERTY_FACTORY);

        assertThat(result).isInstanceOf(BaseIntegerProperty.class);
    }

    @Test
    void testGetOutputSchemaWithDouble() {
        BaseProperty result = SchemaUtils.getOutputSchema("price", 19.99, SCHEMA_PROPERTY_FACTORY);

        assertThat(result).isInstanceOf(BaseNumberProperty.class);
    }

    @Test
    void testGetOutputSchemaWithList() {
        BaseProperty result = SchemaUtils.getOutputSchema("items", List.of("a", "b"), SCHEMA_PROPERTY_FACTORY);

        assertThat(result).isInstanceOf(BaseArrayProperty.class);
    }

    @Test
    void testGetOutputSchemaWithMap() {
        BaseProperty result = SchemaUtils.getOutputSchema("data", Map.of("key", "value"), SCHEMA_PROPERTY_FACTORY);

        assertThat(result).isInstanceOf(BaseObjectProperty.class);
    }

    @Test
    void testGetOutputSchemaWithLocalDate() {
        BaseProperty result = SchemaUtils.getOutputSchema("date", LocalDate.now(), SCHEMA_PROPERTY_FACTORY);

        assertThat(result).isInstanceOf(BaseDateProperty.class);
    }

    @Test
    void testGetOutputSchemaWithLocalDateTime() {
        BaseProperty result = SchemaUtils.getOutputSchema("dateTime", LocalDateTime.now(), SCHEMA_PROPERTY_FACTORY);

        assertThat(result).isInstanceOf(BaseDateTimeProperty.class);
    }

    @Test
    void testGetOutputSchemaWithLocalTime() {
        BaseProperty result = SchemaUtils.getOutputSchema("time", LocalTime.now(), SCHEMA_PROPERTY_FACTORY);

        assertThat(result).isInstanceOf(BaseTimeProperty.class);
    }

    @Test
    void testGetOutputSchemaWithoutName() {
        BaseProperty result = SchemaUtils.getOutputSchema("test", SCHEMA_PROPERTY_FACTORY);

        assertThat(result).isInstanceOf(BaseStringProperty.class);
        assertThat(result.getName()).isNull();
    }

    // getJsonSchemaProperty tests

    @Test
    void testGetJsonSchemaPropertyWithNullSchema() {
        BaseValueProperty<?> result = SchemaUtils.getJsonSchemaProperty(null, JSON_SCHEMA_PROPERTY_FACTORY);

        assertThat(result).isNull();
    }

    @Test
    void testGetJsonSchemaPropertyWithStringType() {
        String jsonSchema = """
            {"type": "string"}""";

        BaseValueProperty<?> result = SchemaUtils.getJsonSchemaProperty(jsonSchema, JSON_SCHEMA_PROPERTY_FACTORY);

        assertThat(result).isInstanceOf(BaseStringProperty.class);
    }

    @Test
    void testGetJsonSchemaPropertyWithBooleanType() {
        String jsonSchema = """
            {"type": "boolean"}""";

        BaseValueProperty<?> result = SchemaUtils.getJsonSchemaProperty(jsonSchema, JSON_SCHEMA_PROPERTY_FACTORY);

        assertThat(result).isInstanceOf(BaseBooleanProperty.class);
    }

    @Test
    void testGetJsonSchemaPropertyWithIntegerType() {
        String jsonSchema = """
            {"type": "integer"}""";

        BaseValueProperty<?> result = SchemaUtils.getJsonSchemaProperty(jsonSchema, JSON_SCHEMA_PROPERTY_FACTORY);

        assertThat(result).isInstanceOf(BaseIntegerProperty.class);
    }

    @Test
    void testGetJsonSchemaPropertyWithNumberType() {
        String jsonSchema = """
            {"type": "number"}""";

        BaseValueProperty<?> result = SchemaUtils.getJsonSchemaProperty(jsonSchema, JSON_SCHEMA_PROPERTY_FACTORY);

        assertThat(result).isInstanceOf(BaseNumberProperty.class);
    }

    @Test
    void testGetJsonSchemaPropertyWithObjectType() {
        String jsonSchema = """
            {
                "type": "object",
                "properties": {
                    "name": {"type": "string"},
                    "age": {"type": "integer"}
                }
            }""";

        BaseValueProperty<?> result = SchemaUtils.getJsonSchemaProperty(jsonSchema, JSON_SCHEMA_PROPERTY_FACTORY);

        assertThat(result).isInstanceOf(BaseObjectProperty.class);

        @SuppressWarnings("unchecked")
        BaseObjectProperty<? extends BaseProperty> objectProperty =
            (BaseObjectProperty<? extends BaseProperty>) result;

        List<? extends BaseProperty> properties = objectProperty.getProperties()
            .orElse(List.of());

        assertThat(properties).hasSize(2);
        assertThat(properties.get(0)
            .getName()).isEqualTo("name");
        assertThat(properties.get(0)).isInstanceOf(BaseStringProperty.class);
        assertThat(properties.get(1)
            .getName()).isEqualTo("age");
        assertThat(properties.get(1)).isInstanceOf(BaseIntegerProperty.class);
    }

    @Test
    void testGetJsonSchemaPropertyWithArrayType() {
        String jsonSchema = """
            {
                "type": "array",
                "items": {"type": "string"}
            }""";

        BaseValueProperty<?> result = SchemaUtils.getJsonSchemaProperty(jsonSchema, JSON_SCHEMA_PROPERTY_FACTORY);

        assertThat(result).isInstanceOf(BaseArrayProperty.class);

        @SuppressWarnings("unchecked")
        BaseArrayProperty<? extends BaseProperty> arrayProperty =
            (BaseArrayProperty<? extends BaseProperty>) result;

        List<? extends BaseProperty> items = arrayProperty.getItems()
            .orElse(List.of());

        assertThat(items).hasSize(1);
        assertThat(items.getFirst()).isInstanceOf(BaseStringProperty.class);
    }

    @Test
    void testGetJsonSchemaPropertyWithNestedObject() {
        String jsonSchema = """
            {
                "type": "object",
                "properties": {
                    "address": {
                        "type": "object",
                        "properties": {
                            "street": {"type": "string"},
                            "zip": {"type": "integer"}
                        }
                    }
                }
            }""";

        BaseValueProperty<?> result = SchemaUtils.getJsonSchemaProperty(jsonSchema, JSON_SCHEMA_PROPERTY_FACTORY);

        assertThat(result).isInstanceOf(BaseObjectProperty.class);

        @SuppressWarnings("unchecked")
        BaseObjectProperty<? extends BaseProperty> objectProperty =
            (BaseObjectProperty<? extends BaseProperty>) result;

        List<? extends BaseProperty> properties = objectProperty.getProperties()
            .orElse(List.of());

        assertThat(properties).hasSize(1);

        BaseProperty addressProperty = properties.getFirst();

        assertThat(addressProperty.getName()).isEqualTo("address");
        assertThat(addressProperty).isInstanceOf(BaseObjectProperty.class);

        @SuppressWarnings("unchecked")
        BaseObjectProperty<? extends BaseProperty> addressObjectProperty =
            (BaseObjectProperty<? extends BaseProperty>) addressProperty;

        List<? extends BaseProperty> addressProperties =
            addressObjectProperty.getProperties()
                .orElse(List.of());

        assertThat(addressProperties).hasSize(2);
        assertThat(addressProperties.get(0)
            .getName()).isEqualTo("street");
        assertThat(addressProperties.get(1)
            .getName()).isEqualTo("zip");
    }

    @Test
    void testGetJsonSchemaPropertyWithNestedArrayOfObjects() {
        String jsonSchema = """
            {
                "type": "array",
                "items": {
                    "type": "object",
                    "properties": {
                        "id": {"type": "integer"},
                        "label": {"type": "string"}
                    }
                }
            }""";

        BaseValueProperty<?> result = SchemaUtils.getJsonSchemaProperty(jsonSchema, JSON_SCHEMA_PROPERTY_FACTORY);

        assertThat(result).isInstanceOf(BaseArrayProperty.class);

        @SuppressWarnings("unchecked")
        BaseArrayProperty<? extends BaseProperty> arrayProperty =
            (BaseArrayProperty<? extends BaseProperty>) result;

        List<? extends BaseProperty> items = arrayProperty.getItems()
            .orElse(List.of());

        assertThat(items).hasSize(1);
        assertThat(items.getFirst()).isInstanceOf(BaseObjectProperty.class);

        @SuppressWarnings("unchecked")
        BaseObjectProperty<? extends BaseProperty> itemObject =
            (BaseObjectProperty<? extends BaseProperty>) items.getFirst();

        List<? extends BaseProperty> objectProperties = itemObject.getProperties()
            .orElse(List.of());

        assertThat(objectProperties).hasSize(2);
        assertThat(objectProperties.get(0)).isInstanceOf(BaseIntegerProperty.class);
        assertThat(objectProperties.get(1)).isInstanceOf(BaseStringProperty.class);
    }

    @Test
    void testGetJsonSchemaPropertyWithNamedProperty() {
        String jsonSchema = """
            {"type": "string"}""";

        BaseValueProperty<?> result = SchemaUtils.getJsonSchemaProperty(
            "fieldName", jsonSchema, JSON_SCHEMA_PROPERTY_FACTORY);

        assertThat(result).isInstanceOf(BaseStringProperty.class);
        assertThat(result.getName()).isEqualTo("fieldName");
    }

    @Test
    void testGetJsonSchemaPropertyDefaultsToString() {
        String jsonSchema = """
            {}""";

        BaseValueProperty<?> result = SchemaUtils.getJsonSchemaProperty(jsonSchema, JSON_SCHEMA_PROPERTY_FACTORY);

        assertThat(result).isInstanceOf(BaseStringProperty.class);
    }

    @Test
    void testGetJsonSchemaPropertyWithUnsupportedType() {
        String jsonSchema = """
            {"type": "unknown"}""";

        assertThatThrownBy(() -> SchemaUtils.getJsonSchemaProperty(jsonSchema, JSON_SCHEMA_PROPERTY_FACTORY))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unsupported JSON schema type");
    }

    @Test
    void testGetJsonSchemaPropertyWithEmptyObject() {
        String jsonSchema = """
            {"type": "object"}""";

        BaseValueProperty<?> result = SchemaUtils.getJsonSchemaProperty(jsonSchema, JSON_SCHEMA_PROPERTY_FACTORY);

        assertThat(result).isInstanceOf(BaseObjectProperty.class);

        @SuppressWarnings("unchecked")
        BaseObjectProperty<? extends BaseProperty> objectProperty =
            (BaseObjectProperty<? extends BaseProperty>) result;

        assertThat(objectProperty.getProperties()
            .orElse(List.of())).isEmpty();
    }

    @Test
    void testGetJsonSchemaPropertyWithEmptyArray() {
        String jsonSchema = """
            {"type": "array"}""";

        BaseValueProperty<?> result = SchemaUtils.getJsonSchemaProperty(jsonSchema, JSON_SCHEMA_PROPERTY_FACTORY);

        assertThat(result).isInstanceOf(BaseArrayProperty.class);

        @SuppressWarnings("unchecked")
        BaseArrayProperty<? extends BaseProperty> arrayProperty =
            (BaseArrayProperty<? extends BaseProperty>) result;

        assertThat(arrayProperty.getItems()
            .orElse(List.of())).isEmpty();
    }

    // toOutput tests

    @Test
    void testToOutputWithValue() {
        OutputResponse result = SchemaUtils.toOutput(
            "hello",
            (outputSchema, sampleOutput, placeholder) -> new OutputResponse(null, sampleOutput, placeholder),
            SCHEMA_PROPERTY_FACTORY);

        assertThat(result).isNotNull();
        assertThat(result.sampleOutput()).isEqualTo("hello");
    }

    @Test
    void testToOutputWithOutputResponse() {
        BaseValueProperty<?> schema = string("testField");

        BaseOutputDefinition.OutputResponse outputResponse = BaseOutputDefinition.OutputResponse.of(schema, "sample");

        OutputResponse result = SchemaUtils.toOutput(
            outputResponse,
            (outputSchema, sampleOutput, placeholder) -> new OutputResponse(null, sampleOutput, placeholder),
            SCHEMA_PROPERTY_FACTORY);

        assertThat(result).isNotNull();
        assertThat(result.sampleOutput()).isEqualTo("sample");
    }

    @Test
    void testToOutputGeneratesSampleOutputFromSchema() {
        BaseValueProperty<?> schema = string("testField");

        BaseOutputDefinition.OutputResponse outputResponse = BaseOutputDefinition.OutputResponse.of(schema);

        OutputResponse result = SchemaUtils.toOutput(
            outputResponse,
            (outputSchema, sampleOutput, placeholder) -> new OutputResponse(null, sampleOutput, placeholder),
            SCHEMA_PROPERTY_FACTORY);

        assertThat(result).isNotNull();
        assertThat(result.sampleOutput()).isInstanceOf(String.class);
        assertThat((String) result.sampleOutput()).startsWith("sample");
    }

    private static class TestSchemaPropertyFactory implements SchemaUtils.SchemaPropertyFactory {

        @Override
        public BaseProperty create(String name, Object value, Class<? extends BaseProperty> basePropertyClass) {
            if (basePropertyClass == BaseArrayProperty.class) {
                ModifiableArrayProperty arrayProperty = array(name);

                if (value instanceof List<?> list && !list.isEmpty()) {
                    arrayProperty.items(
                        (ModifiableValueProperty<?, ?>) SchemaUtils.getOutputSchema(null, list.getFirst(), this));
                }

                return arrayProperty;
            } else if (basePropertyClass == BaseBooleanProperty.class) {
                return bool(name);
            } else if (basePropertyClass == BaseDateProperty.class) {
                return ComponentDsl.date(name);
            } else if (basePropertyClass == BaseDateTimeProperty.class) {
                return ComponentDsl.dateTime(name);
            } else if (basePropertyClass == BaseIntegerProperty.class) {
                return integer(name);
            } else if (basePropertyClass == BaseNullProperty.class) {
                return ComponentDsl.nullable(name);
            } else if (basePropertyClass == BaseNumberProperty.class) {
                return number(name);
            } else if (basePropertyClass == BaseObjectProperty.class) {
                ModifiableObjectProperty objectProperty = object(name);

                if (value instanceof Map<?, ?> map) {
                    List<ModifiableValueProperty<?, ?>> properties = new ArrayList<>();

                    for (Map.Entry<?, ?> entry : map.entrySet()) {
                        properties.add(
                            (ModifiableValueProperty<?, ?>) SchemaUtils.getOutputSchema(
                                (String) entry.getKey(), entry.getValue(), this));
                    }

                    objectProperty.properties(properties);
                }

                return objectProperty;
            } else if (basePropertyClass == BaseStringProperty.class) {
                return string(name);
            } else if (basePropertyClass == BaseTimeProperty.class) {
                return ComponentDsl.time(name);
            } else {
                return object(name);
            }
        }
    }
}
