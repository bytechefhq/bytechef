/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.hermes.component.definition;

import static com.bytechef.hermes.component.ComponentDSL.display;
import static com.bytechef.hermes.component.constants.Versions.VERSION_1;

import com.bytechef.hermes.component.ComponentDSL;
import com.bytechef.hermes.definition.DisplayOption;
import com.bytechef.hermes.definition.Property;
import com.bytechef.hermes.definition.PropertyOption;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

/**
 * @author Ivica Cardic
 */
public class ComponentDefinitionTest {

    private final ObjectMapper objectMapper = new ObjectMapper() {
        {
            registerModule(new JavaTimeModule());

            setSerializationInclusion(JsonInclude.Include.NON_NULL);
        }
    };

    @Test
    @SuppressWarnings("checkstyle:methodlengthcheck")
    public void testDisplayOption() throws JSONException, JsonProcessingException {
        DisplayOption displayOption =
                DisplayOption.build(List.of(ComponentDSL.hideWhen("name").in(true, false)));

        jsonAssertEquals("""
                {"hideWhen":{"name":[true,false]}}
                """, displayOption);

        displayOption =
                DisplayOption.build(List.of(ComponentDSL.hideWhen("name").in(1, 2)));

        jsonAssertEquals(
                """
            {
                "hideWhen":{"name":[1,2]}
            }
            """,
                displayOption);

        displayOption =
                DisplayOption.build(List.of(ComponentDSL.hideWhen("name").in(1L, 2L)));

        jsonAssertEquals(
                """
            {
                "hideWhen":{"name":[1,2]}
            }
            """,
                displayOption);

        displayOption =
                DisplayOption.build(List.of(ComponentDSL.hideWhen("name").in(1F, 2F)));

        jsonAssertEquals("""
        {
            "hideWhen":{"name":[1.0,2.0]}
        }
        """, displayOption);

        displayOption =
                DisplayOption.build(List.of(ComponentDSL.hideWhen("name").in(1D, 2D)));

        jsonAssertEquals("""
        {
            "hideWhen":{"name":[1.0,2.0]}
        }
        """, displayOption);

        displayOption =
                DisplayOption.build(List.of(ComponentDSL.hideWhen("name").in("value1", "value2")));

        jsonAssertEquals(
                """
        {
            "hideWhen":{"name":["value1","value2"]}
        }
        """, displayOption);

        displayOption =
                DisplayOption.build(List.of(ComponentDSL.hideWhen("name1").eq(1)));

        jsonAssertEquals("""
        {
         "hideWhen":{"name1":[1]}
        }
        """, displayOption);

        displayOption = DisplayOption.build(List.of(
                ComponentDSL.hideWhen("name1").eq(1),
                ComponentDSL.hideWhen("name2").eq(2)));

        jsonAssertEquals(
                """
        {
            "hideWhen":{"name1":[1],"name2":[2]}
        }
        """, displayOption);

        displayOption =
                DisplayOption.build(List.of(ComponentDSL.showWhen("name").in(true, false)));

        jsonAssertEquals(
                """
        {
            "showWhen":{"name":[true, false]}
        }
        """, displayOption);

        displayOption =
                DisplayOption.build(List.of(ComponentDSL.showWhen("name").in(1, 2)));

        jsonAssertEquals(
                """
            {
                "showWhen":{"name": [1,2]}
            }
            """,
                displayOption);

        displayOption =
                DisplayOption.build(List.of(ComponentDSL.showWhen("name").in(1L, 2L)));

        jsonAssertEquals(
                """
            {
                "showWhen":{"name":[1,2]}
            }
            """,
                displayOption);

        displayOption =
                DisplayOption.build(List.of(ComponentDSL.showWhen("name").in(1F, 2F)));

        jsonAssertEquals("""
        {
            "showWhen":{"name":[1.0,2.0]}
        }
        """, displayOption);

        displayOption =
                DisplayOption.build(List.of(ComponentDSL.showWhen("name").in(1D, 2D)));

        jsonAssertEquals("""
        {
            "showWhen":{"name":[1.0,2.0]}
        }
        """, displayOption);

        displayOption =
                DisplayOption.build(List.of(ComponentDSL.showWhen("name").in("value1", "value2")));

        jsonAssertEquals(
                """
        {
            "showWhen":{"name":["value1","value2"]}
        }
        """, displayOption);

        displayOption =
                DisplayOption.build(List.of(ComponentDSL.showWhen("name1").eq(1)));

        jsonAssertEquals("""
        {
         "showWhen":{"name1":[1]}
        }
        """, displayOption);

        displayOption = DisplayOption.build(List.of(
                ComponentDSL.showWhen("name1").eq(1),
                ComponentDSL.showWhen("name2").eq(2)));

        jsonAssertEquals(
                """
        {
            "showWhen":{"name1":[1],"name2":[2]}
        }
        """, displayOption);
    }

    @Test
    public void testAnyProperty() throws JSONException, JsonProcessingException {
        Property<Property.AnyProperty> property = ComponentDSL.any("name")
                .description("description")
                .label("label")
                .placeholder("placeholder")
                .required(true)
                .types(ComponentDSL.string());

        jsonAssertEquals(
                """
                {
                    "description":"description",
                    "label":"label",
                    "name":"name",
                    "placeholder":"placeholder",
                    "required":true,
                    "types":[{"type":"STRING"}],
                    "type": "ANY"
                }
                """,
                property);
    }

    @Test
    public void testArrayProperty() throws JSONException, JsonProcessingException {
        Property<Property.ArrayProperty> property = ComponentDSL.array("name")
                .defaultValue(1, 2)
                .description("description")
                .label("label")
                .placeholder("placeholder")
                .required(true)
                .items(ComponentDSL.string());

        jsonAssertEquals(
                """
                {
                    "defaultValue":[1,2],
                    "description":"description",
                    "label":"label",
                    "items":[{"type":"STRING"}],
                    "name":"name",
                    "placeholder":"placeholder",
                    "required":true,
                    "type":"ARRAY"
                }
                """,
                property);
    }

    @Test
    public void testBooleanProperty() throws JSONException, JsonProcessingException {
        Property<Property.BooleanProperty> property = ComponentDSL.bool("name")
                .defaultValue(true)
                .description("description")
                .label("label")
                .placeholder("placeholder")
                .required(true);

        jsonAssertEquals(
                """
        {
            "defaultValue":true,
            "description":"description",
            "label":"label",
            "name":"name",
            "placeholder":"placeholder",
            "required":true,
            "type":"BOOLEAN"
        }
        """,
                property);
    }

    @Test
    public void testDateProperty() throws JSONException, JsonProcessingException {
        Property<Property.DateProperty> property = ComponentDSL.date("name")
                .defaultValue(LocalDate.MIN)
                .description("description")
                .label("label")
                .placeholder("placeholder")
                .required(true);

        jsonAssertEquals(
                """
        {
            "defaultValue":[-999999999,1,1],
            "description":"description",
            "label":"label",
            "name":"name",
            "placeholder":"placeholder",
            "required":true,
            "type":"DATE"
        }
        """,
                property);
    }

    @Test
    public void testDateTimeProperty() throws JSONException, JsonProcessingException {
        Property<Property.DateTimeProperty> property = ComponentDSL.dateTime("name")
                .defaultValue(LocalDateTime.MIN)
                .description("description")
                .label("label")
                .placeholder("placeholder")
                .required(true);

        jsonAssertEquals(
                """
            {
                "defaultValue":[-999999999,1,1,0,0],
                "description":"description",
                "label":"label",
                "name":"name",
                "placeholder":"placeholder",
                "required":true,
                "type":"DATE_TIME"
            }
            """,
                property);
    }

    @Test
    public void testIntegerProperty() throws JSONException, JsonProcessingException {
        Property<Property.IntegerProperty> property = ComponentDSL.integer("name")
                .defaultValue(2)
                .description("description")
                .label("label")
                .placeholder("placeholder")
                .required(true);

        jsonAssertEquals(
                """
            {
                "defaultValue":2,
                "description":"description",
                "label":"label",
                "name":"name",
                "placeholder":"placeholder",
                "required":true,
                "type":"INTEGER"
            }
            """,
                property);
    }

    @Test
    public void testNumberProperty() throws JSONException, JsonProcessingException {
        Property<Property.NumberProperty> property = ComponentDSL.number("name")
                .defaultValue(2)
                .description("description")
                .label("label")
                .options(ComponentDSL.option("option1", 1), ComponentDSL.option("option2", 2))
                .placeholder("placeholder")
                .required(true);

        jsonAssertEquals(
                """
            {
                "defaultValue":2,
                "description":"description",
                "label":"label",
                "name":"name",
                "options":[
                    {
                        "name":"option1",
                        "value":1
                    },
                    {
                        "name":"option2",
                        "value":2
                    }
                ],
                "placeholder":"placeholder",
                "required":true,
                "type":"NUMBER"
            }
            """,
                property);
    }

    @Test
    public void testObjectProperty() throws JSONException, JsonProcessingException {
        Property<Property.ObjectProperty> property = ComponentDSL.object("name")
                .defaultValue(Map.of("key", Map.of("key1", "value1")))
                .description("description")
                .label("label")
                .placeholder("placeholder")
                .required(true);

        jsonAssertEquals(
                """
            {
                "defaultValue":{"key":{"key1":"value1"}},
                "description":"description",
                "label":"label",
                "name":"name",
                "placeholder":"placeholder",
                "required":true,
                "type":"OBJECT"
            }
            """,
                property);
    }

    @Test
    public void testTaskOperation() throws JSONException, JsonProcessingException {
        Action action = ComponentDSL.action("name").display(display("label").description("description"));

        jsonAssertEquals(
                """
            {"display":{"description":"description","label":"label"},"name":"name"}
            """,
                action);
    }

    @Test
    public void testStringProperty() throws JSONException, JsonProcessingException {
        Property<Property.StringProperty> property = ComponentDSL.string("name")
                .defaultValue("defaultValue")
                .description("description")
                .label("label")
                .placeholder("placeholder")
                .required(true);

        jsonAssertEquals(
                """
            {
                "defaultValue":"defaultValue",
                "description":"description",
                "label":"label",
                "name":"name",
                "placeholder":"placeholder",
                "required":true,
                "type":"STRING"
            }
            """,
                property);
    }

    @Test
    public void testTaskHandlerDescription() throws JSONException, JsonProcessingException {
        ComponentDefinition componentDefinition;
        componentDefinition = ComponentDSL.createComponent("name")
                .display(display("label")
                        .description("description")
                        .subtitle("subtitle")
                        .icon("icon"))
                .connections()
                .version(VERSION_1);

        jsonAssertEquals(
                """
                    {"connections":[],"display":{"description":"description","icon":"icon","label":"label","subtitle":"subtitle"},"name":"name","version":1.0}
            """,
                componentDefinition);
    }

    @Test
    public void testPropertyOption() throws JSONException, JsonProcessingException {
        PropertyOption propertyOption = ComponentDSL.option("name", 1);

        jsonAssertEquals(
                """
            {
                "name":"name",
                "value":1
            }
            """,
                propertyOption);

        propertyOption = ComponentDSL.option("name", "value");

        jsonAssertEquals(
                """
            {
                "name":"name",
                "value":"value"
            }
            """,
                propertyOption);

        propertyOption = ComponentDSL.option("name", 1, "description");

        jsonAssertEquals(
                """
            {
                "name":"name",
                "value":1,
                "description":"description"
            }
            """,
                propertyOption);

        propertyOption = ComponentDSL.option("name", "value", "description");

        jsonAssertEquals(
                """
            {
                "name":"name",
                "value":"value",
                "description":"description"
            }
            """,
                propertyOption);
    }

    private void jsonAssertEquals(String expectedString, Object jsonObject)
            throws JSONException, JsonProcessingException {
        JSONAssert.assertEquals(expectedString, objectMapper.writeValueAsString(jsonObject), true);
    }
}
