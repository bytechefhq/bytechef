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

package com.bytechef.platform.component.util;

import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.date;
import static com.bytechef.component.definition.ComponentDsl.dateTime;
import static com.bytechef.component.definition.ComponentDsl.dynamicProperties;
import static com.bytechef.component.definition.ComponentDsl.fileEntry;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.nullable;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.time;
import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.Option;
import com.bytechef.platform.component.domain.Property;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ActionInputSchemaGeneratorTest {

    @Test
    void testGeneratesNestedObjectsArraysOptionsAndConditionalFields() {
        List<Property> properties = List.of(
            Property.toProperty(string("name").description("Full name")
                .required(true)),
            Property.toProperty(
                string("priority").options(option("Low", "low"), option("High", "high"))
                    .defaultValue("low")),
            Property.toProperty(integer("retries").required(true)
                .displayCondition("priority == 'high'")),
            Property.toProperty(
                array("lines").required(true)
                    .items(
                        object("line").properties(
                            string("sku").required(true),
                            integer("quantity").required(true)
                                .defaultValue(1),
                            bool("gift")))),
            Property.toProperty(date("dueDate")),
            Property.toProperty(
                dynamicProperties("extra").properties(
                    (ActionDefinition.PropertiesFunction) (
                        inputParameters, connectionParameters, lookupDependsOnPaths, context) -> List.of())));

        Map<String, Object> schema = ActionInputSchemaGenerator.generateInputSchema(properties);

        assertThat(schema).containsEntry("type", "object");
        assertThat(schema).containsEntry("additionalProperties", false);
        assertThat(schema.get("required")).isEqualTo(List.of("name", "lines"));

        @SuppressWarnings("unchecked")
        Map<String, Object> props = (Map<String, Object>) schema.get("properties");

        assertThat(props.get("name")).isEqualTo(Map.of("type", "string", "description", "Full name"));
        assertThat(props.get("priority"))
            .isEqualTo(Map.of("type", "string", "enum", List.of("low", "high"), "default", "low"));
        assertThat(props.get("retries")).isEqualTo(Map.of("type", "integer"));
        assertThat(props.get("dueDate")).isEqualTo(Map.of("type", "string", "format", "date"));
        assertThat(props.get("extra")).isEqualTo(Map.of("type", "object", "additionalProperties", true));

        @SuppressWarnings("unchecked")
        Map<String, Object> lines = (Map<String, Object>) props.get("lines");

        assertThat(lines.get("type")).isEqualTo("array");

        @SuppressWarnings("unchecked")
        Map<String, Object> line = (Map<String, Object>) lines.get("items");

        assertThat(line.get("type")).isEqualTo("object");
        assertThat(line.get("required")).isEqualTo(List.of("sku", "quantity"));
        assertThat(line.get("additionalProperties")).isEqualTo(false);

        @SuppressWarnings("unchecked")
        Map<String, Object> lineProps = (Map<String, Object>) line.get("properties");

        assertThat(lineProps.get("quantity")).isEqualTo(Map.of("type", "integer", "default", 1L));
        assertThat(lineProps.get("gift")).isEqualTo(Map.of("type", "boolean"));
    }

    @Test
    void testArrayWithoutItemTypeAndWithSeveralItemTypes() {
        Map<String, Object> schema = ActionInputSchemaGenerator.generateInputSchema(List.of(
            Property.toProperty(array("tags")),
            Property.toProperty(array("mixed").items(string("text"), integer("count")))));

        @SuppressWarnings("unchecked")
        Map<String, Object> props = (Map<String, Object>) schema.get("properties");

        assertThat(props.get("tags")).isEqualTo(Map.of("type", "array", "items", Map.of()));
        assertThat(props.get("mixed")).isEqualTo(
            Map.of(
                "type", "array",
                "items", Map.of("anyOf", List.of(Map.of("type", "string"), Map.of("type", "integer")))));
    }

    @Test
    void testBooleanOptionsAreNotAnEnumAndEmptyInputHasNoRequired() {
        Map<String, Object> schema = ActionInputSchemaGenerator.generateInputSchema(
            List.of(Property.toProperty(bool("flag"))));

        @SuppressWarnings("unchecked")
        Map<String, Object> props = (Map<String, Object>) schema.get("properties");

        assertThat(props.get("flag")).isEqualTo(Map.of("type", "boolean"));
        assertThat(schema.get("required")).isEqualTo(List.of());
        assertThat(ActionInputSchemaGenerator.generateInputSchema(List.of())
            .get("properties"))
                .isEqualTo(Map.of());
    }

    @Test
    void testHiddenPropertiesAreExcludedAtEveryLevel() {
        Map<String, Object> schema = ActionInputSchemaGenerator.generateInputSchema(List.of(
            Property.toProperty(string("visible")),
            Property.toProperty(string("internal").hidden(true)
                .required(true)),
            Property.toProperty(
                object("wrapper").properties(
                    string("shown").required(true),
                    string("secret").hidden(true)
                        .required(true))),
            Property.toProperty(array("onlyHidden").items(string("item").hidden(true)))));

        assertThat(schema.get("required")).isEqualTo(List.of());

        Map<String, Object> props = castMap(schema.get("properties"));

        assertThat(props.keySet()).containsExactly("visible", "wrapper", "onlyHidden");

        Map<String, Object> wrapper = castMap(props.get("wrapper"));

        assertThat(castMap(wrapper.get("properties")).keySet()).containsExactly("shown");
        assertThat(wrapper.get("required")).isEqualTo(List.of("shown"));
        assertThat(props.get("onlyHidden")).isEqualTo(Map.of("type", "array", "items", Map.of()));
    }

    @Test
    void testTemporalOptionsBecomeIsoStringEnums() {
        Map<String, Object> schema = ActionInputSchemaGenerator.generateInputSchema(List.of(
            Property.toProperty(date("d").options(option("Launch", LocalDate.of(2026, 10, 7)))),
            Property.toProperty(
                dateTime("dt").options(option("Launch", LocalDateTime.of(2026, 10, 7, 10, 15, 30)))),
            Property.toProperty(time("t").options(timeOption(LocalTime.of(10, 15, 30))))));

        Map<String, Object> props = castMap(schema.get("properties"));

        assertThat(props.get("d")).isEqualTo(Map.of("type", "string", "format", "date", "enum", List.of("2026-10-07")));
        assertThat(props.get("dt")).isEqualTo(
            Map.of("type", "string", "format", "date-time", "enum", List.of("2026-10-07T10:15:30")));
        assertThat(props.get("t"))
            .isEqualTo(Map.of("type", "string", "format", "time", "enum", List.of("10:15:30")));
    }

    @Test
    void testIntegerAndNumberOptionsBecomeEnums() {
        Map<String, Object> schema = ActionInputSchemaGenerator.generateInputSchema(List.of(
            Property.toProperty(integer("n").options(option("One", 1), option("Two", 2))),
            Property.toProperty(number("x").options(option("Half", 0.5), option("One", 1.5)))));

        Map<String, Object> props = castMap(schema.get("properties"));

        assertThat(props.get("n")).isEqualTo(Map.of("type", "integer", "enum", List.of(1L, 2L)));
        assertThat(props.get("x")).isEqualTo(Map.of("type", "number", "enum", List.of(0.5, 1.5)));
    }

    @Test
    void testNumberNullTimeAndDateTimeTypeMappings() {
        Map<String, Object> schema = ActionInputSchemaGenerator.generateInputSchema(List.of(
            Property.toProperty(number("amount")),
            Property.toProperty(nullable("nothing")),
            Property.toProperty(time("at")),
            Property.toProperty(dateTime("when"))));

        Map<String, Object> props = castMap(schema.get("properties"));

        assertThat(props.get("amount")).isEqualTo(Map.of("type", "number"));
        assertThat(props.get("nothing")).isEqualTo(Map.of("type", "null"));
        assertThat(props.get("at")).isEqualTo(Map.of("type", "string", "format", "time"));
        assertThat(props.get("when")).isEqualTo(Map.of("type", "string", "format", "date-time"));
    }

    @Test
    void testFileEntryIsAnOpenObjectWithItsProperties() {
        Map<String, Object> schema = ActionInputSchemaGenerator.generateInputSchema(
            List.of(Property.toProperty(fileEntry("file"))));

        Map<String, Object> file = castMap(castMap(schema.get("properties")).get("file"));

        assertThat(file).containsEntry("type", "object");
        assertThat(file).doesNotContainKey("additionalProperties");
        assertThat(castMap(file.get("properties")).keySet())
            .containsExactlyInAnyOrder("extension", "mimeType", "name", "url");
        assertThat(file.get("required")).isEqualTo(List.of("extension", "mimeType", "name", "url"));
    }

    @Test
    void testObjectWithoutPropertiesIsOpen() {
        Map<String, Object> schema = ActionInputSchemaGenerator.generateInputSchema(
            List.of(Property.toProperty(object("payload"))));

        assertThat(castMap(schema.get("properties")).get("payload"))
            .isEqualTo(Map.of("type", "object", "additionalProperties", true));
    }

    @Test
    void testBooleanAndNumberDefaults() {
        Map<String, Object> schema = ActionInputSchemaGenerator.generateInputSchema(List.of(
            Property.toProperty(bool("flag").defaultValue(true)),
            Property.toProperty(number("ratio").defaultValue(2.5))));

        Map<String, Object> props = castMap(schema.get("properties"));

        assertThat(props.get("flag")).isEqualTo(Map.of("type", "boolean", "default", true));
        assertThat(props.get("ratio")).isEqualTo(Map.of("type", "number", "default", 2.5));
    }

    @Test
    void testNestedObjectKeepsItsDescriptionAndArrayOfArraysNests() {
        Map<String, Object> schema = ActionInputSchemaGenerator.generateInputSchema(List.of(
            Property.toProperty(object("address").description("Postal address")
                .properties(string("city"))),
            Property.toProperty(array("matrix").items(array().items(integer())))));

        Map<String, Object> props = castMap(schema.get("properties"));

        assertThat(castMap(props.get("address"))).containsEntry("description", "Postal address");
        assertThat(props.get("matrix")).isEqualTo(
            Map.of("type", "array", "items", Map.of("type", "array", "items", Map.of("type", "integer"))));
    }

    @Test
    void testArrayOptionsBecomeAnItemsEnum() {
        Map<String, Object> schema = ActionInputSchemaGenerator.generateInputSchema(List.of(
            Property.toProperty(
                array("colors").items(string("color"))
                    .options(List.of(option("Red", "red"), option("Blue", "blue")))),
            Property.toProperty(
                array("shades").options(List.of(option("Red", "red"), option("Blue", "blue")))),
            Property.toProperty(
                array("mixed").items(string("text"), integer("count"))
                    .options(List.of(option("Red", "red"), option("One", 1))))));

        Map<String, Object> props = castMap(schema.get("properties"));

        assertThat(props.get("colors")).isEqualTo(
            Map.of("type", "array", "items", Map.of("type", "string", "enum", List.of("red", "blue"))));
        assertThat(props.get("shades")).isEqualTo(
            Map.of("type", "array", "items", Map.of("enum", List.of("red", "blue"))));
        assertThat(props.get("mixed")).isEqualTo(
            Map.of("type", "array", "items", Map.of("enum", List.of("red", 1L))));
    }

    @Test
    void testObjectWithAdditionalPropertiesStaysOpen() {
        Map<String, Object> schema = ActionInputSchemaGenerator.generateInputSchema(List.of(
            Property.toProperty(
                object("labels").properties(string("a")
                    .required(true))
                    .additionalProperties(string())),
            Property.toProperty(object("closed").properties(string("b")))));

        Map<String, Object> props = castMap(schema.get("properties"));
        Map<String, Object> labels = castMap(props.get("labels"));

        assertThat(labels).containsEntry("additionalProperties", true);
        assertThat(labels.get("required")).isEqualTo(List.of("a"));
        assertThat(castMap(labels.get("properties")).keySet()).containsExactly("a");
        assertThat(castMap(props.get("closed"))).containsEntry("additionalProperties", false);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Object value) {
        return (Map<String, Object>) value;
    }

    private static Option<LocalTime> timeOption(LocalTime value) {
        return new Option<>() {

            @Override
            public Optional<String> getDescription() {
                return Optional.empty();
            }

            @Override
            public String getLabel() {
                return "Time";
            }

            @Override
            public LocalTime getValue() {
                return value;
            }
        };
    }
}
