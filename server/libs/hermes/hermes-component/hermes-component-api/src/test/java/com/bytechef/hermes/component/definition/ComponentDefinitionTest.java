
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

import static com.bytechef.hermes.component.constant.Version.VERSION_1;
import static com.bytechef.hermes.component.definition.ComponentDSL.display;

import com.bytechef.hermes.definition.Option;
import com.bytechef.hermes.definition.Property;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
    public void testActionDefinition() throws JSONException, JsonProcessingException {
        ActionDefinition action = ComponentDSL.action("name")
            .display(display("label").description("description"));

        jsonAssertEquals(
            """
                {"display":{"description":"description","label":"label"},"name":"name"}
                """,
            action);
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
                    "controlType": "JSON_BUILDER",
                    "defaultValue":[1,2],
                    "description":"description",
                    "label":"label",
                    "items":[{"controlType":"INPUT_TEXT","type":"STRING"}],
                     "multipleValues": true,
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
                    "controlType": "CHECKBOX",
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
                    "controlType": "DATE",
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
                    "controlType": "DATE_TIME",
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
                    "controlType": "INPUT_INTEGER",
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
                    "controlType": "SELECT",
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
                    "controlType": "JSON_BUILDER",
                    "defaultValue":{"key":{"key1":"value1"}},
                    "description":"description",
                    "label":"label",
                    "multipleValues": true,
                    "name":"name",
                    "placeholder":"placeholder",
                    "required":true,
                    "type":"OBJECT"
                }
                """,
            property);
    }

    @Test
    public void testOneOfProperty() throws JSONException, JsonProcessingException {
        Property<Property.OneOfProperty> property = ComponentDSL.oneOf("name")
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
                    "types":[{"controlType":"INPUT_TEXT","type":"STRING"}],
                    "type": "ONE_OF"
                }
                """,
            property);
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
                    "controlType": "INPUT_TEXT",
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
        componentDefinition = ComponentDSL.component("name")
            .display(display("label")
                .description("description")
                .subtitle("subtitle")
                .icon("icon"))
            .version(VERSION_1);

        jsonAssertEquals(
            """
                        {"display":{"description":"description","icon":"icon","label":"label","subtitle":"subtitle"},"name":"name","version":1.0}
                """,
            componentDefinition);
    }

    @Test
    public void testPropertyOption() throws JSONException, JsonProcessingException {
        Option option = ComponentDSL.option("name", 1);

        jsonAssertEquals(
            """
                {
                    "name":"name",
                    "value":1
                }
                """,
            option);

        option = ComponentDSL.option("name", "value");

        jsonAssertEquals(
            """
                {
                    "name":"name",
                    "value":"value"
                }
                """,
            option);

        option = ComponentDSL.option("name", 1, "description");

        jsonAssertEquals(
            """
                {
                    "name":"name",
                    "value":1,
                    "description":"description"
                }
                """,
            option);

        option = ComponentDSL.option("name", "value", "description");

        jsonAssertEquals(
            """
                {
                    "name":"name",
                    "value":"value",
                    "description":"description"
                }
                """,
            option);
    }

    private void jsonAssertEquals(String expectedString, Object jsonObject)
        throws JSONException, JsonProcessingException {
        JSONAssert.assertEquals(expectedString, objectMapper.writeValueAsString(jsonObject), true);
    }
}
