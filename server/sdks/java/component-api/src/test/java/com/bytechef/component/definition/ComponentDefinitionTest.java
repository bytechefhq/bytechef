/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.component.definition;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
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
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

            registerModule(new JavaTimeModule());
            registerModule(new Jdk8Module());
        }
    };

    @Test
    public void testActionDefinition() throws JSONException, JsonProcessingException {
        ActionDefinition action = ComponentDSL.action("name")
            .title("title")
            .description("description")
            .perform((inputParameters, connectionParameters, context) -> null);

        jsonAssertEquals(
            """
                {"batch":null,"componentName":null,"componentDescription":null,"componentTitle":null,"componentVersion":0,"deprecated":null,"description":"description","sampleOutput":null,"help":null,"metadata":null,"name":"name","outputSchema":null,"properties":null,"title":"title","editorDescriptionDataSource":null,"perform":{},"outputSchemaDataSource":null,"sampleOutputDataSource":null}
                """,
            action);
    }

    @Test
    public void testArrayProperty() throws JSONException, JsonProcessingException {
        Property property = ComponentDSL.array("name")
            .defaultValue(1, 2)
            .description("description")
            .label("label")
            .placeholder("placeholder")
            .required(true)
            .items(ComponentDSL.integer());

        jsonAssertEquals(
            """
                {"advancedOption":null,"description":"description","displayCondition":null,"expressionEnabled":null,"hidden":null,"label":"label", "maxItems":null,"minItems":null,"metadata":{},"placeholder":"placeholder","required":true,"name":"name","type":"ARRAY","defaultValue":[1,2],"exampleValue":null,"items":[{"advancedOption":null,"description":null,"displayCondition":null,"expressionEnabled":null,"hidden":null,"label":null,"metadata":{},"placeholder":null,"required":null,"name":null,"type":"INTEGER","defaultValue":null,"exampleValue":null,"maxValue":null,"minValue":null,"options":null,"controlType":"INTEGER","optionsDataSource":null}],"multipleValues":null,"options":null,"controlType":"ARRAY_BUILDER","optionsDataSource":null}
                """,
            property);
    }

    @Test
    public void testBooleanProperty() throws JSONException, JsonProcessingException {
        Property property = ComponentDSL.bool("name")
            .defaultValue(true)
            .description("description")
            .label("label")
            .placeholder("placeholder")
            .required(true);

        jsonAssertEquals(
            """
                {"advancedOption":null,"description":"description","displayCondition":null,"expressionEnabled":null,"hidden":null,"label":"label","metadata":{},"placeholder":"placeholder","required":true,"name":"name","type":"BOOLEAN","defaultValue":true,"exampleValue":null,"options":[{"description":null,"displayCondition":null,"label":"True","value":true},{"description":null,"displayCondition":null,"label":"False","value":true}],"controlType":"CHECKBOX"}
                """,
            property);
    }

    @Test
    public void testComponentDefinition() throws JSONException, JsonProcessingException {
        ComponentDefinition componentDefinition = ComponentDSL.component("name")
            .title("title")
            .description("description")
            .icon("icon")
            .version(1);

        jsonAssertEquals(
            """
                {"actions":null,"category":null,"connection":null,"customAction":null,"customActionHelp":null,"description":"description","icon":"icon","tags":null,"allowedConnections":null,"metadata":null,"name":"name","resources":null,"version":1,"title":"title","triggers":null}
                  """,
            componentDefinition);
    }

    @Test
    public void testDateProperty() throws JSONException, JsonProcessingException {
        Property property = ComponentDSL.date("name")
            .defaultValue(LocalDate.MIN)
            .description("description")
            .label("label")
            .placeholder("placeholder")
            .required(true);

        jsonAssertEquals(
            """
                {"advancedOption":null,"description":"description","displayCondition":null,"expressionEnabled":null,"hidden":null,"label":"label","metadata":{},"placeholder":"placeholder","required":true,"name":"name","type":"DATE","defaultValue":"-999999999-01-01","exampleValue":null,"options":null,"controlType":"DATE","optionsDataSource":null}
                """,
            property);
    }

    @Test
    public void testDateTimeProperty() throws JSONException, JsonProcessingException {
        Property property = ComponentDSL.dateTime("name")
            .defaultValue(LocalDateTime.MIN)
            .description("description")
            .label("label")
            .placeholder("placeholder")
            .required(true);

        jsonAssertEquals(
            """
                {"advancedOption":null,"description":"description","displayCondition":null,"expressionEnabled":null,"hidden":null,"label":"label","metadata":{},"placeholder":"placeholder","required":true,"name":"name","type":"DATE_TIME","defaultValue":"-999999999-01-01T00:00:00","exampleValue":null,"options":null,"controlType":"DATE_TIME","optionsDataSource":null}
                 """,
            property);
    }

    @Test
    public void testIntegerProperty() throws JSONException, JsonProcessingException {
        Property property = ComponentDSL.integer("name")
            .defaultValue(2)
            .description("description")
            .label("label")
            .placeholder("placeholder")
            .required(true);

        jsonAssertEquals(
            """
                {"advancedOption":null,"description":"description","displayCondition":null,"expressionEnabled":null,"hidden":null,"label":"label","metadata":{},"placeholder":"placeholder","required":true,"name":"name","type":"INTEGER","defaultValue":2,"exampleValue":null,"maxValue":null,"minValue":null,"options":null,"controlType":"INTEGER","optionsDataSource":null}
                 """,
            property);
    }

    @Test
    public void testNumberProperty() throws JSONException, JsonProcessingException {
        Property property = ComponentDSL.number("name")
            .defaultValue(2)
            .description("description")
            .label("label")
            .options(ComponentDSL.option("option1", 1L), ComponentDSL.option("option2", 2L))
            .placeholder("placeholder")
            .required(true);

        jsonAssertEquals(
            """
                {"advancedOption":null,"description":"description","displayCondition":null,"expressionEnabled":null,"hidden":null,"label":"label","maxNumberPrecision":null,"minNumberPrecision":null,"metadata":{},"placeholder":"placeholder","required":true,"name":"name","type":"NUMBER","defaultValue":2.0,"exampleValue":null,"maxValue":null,"minValue":null,"numberPrecision":null,"options":[{"description":null,"displayCondition":null,"label":"option1","value":1.0},{"description":null,"displayCondition":null,"label":"option2","value":2.0}],"controlType":"SELECT","optionsDataSource":null}
                 """,
            property);
    }

    @Test
    public void testObjectProperty() throws JSONException, JsonProcessingException {
        Property property = ComponentDSL.object("name")
            .defaultValue(Map.of("key", Map.of("key1", "value1")))
            .description("description")
            .label("label")
            .placeholder("placeholder")
            .required(true);

        jsonAssertEquals(
            """
                {"advancedOption":null,"description":"description","displayCondition":null,"expressionEnabled":null,"hidden":null,"label":"label","metadata":{},"placeholder":"placeholder","required":true,"name":"name","type":"OBJECT","defaultValue":{"key":{"key1":"value1"}},"exampleValue":null,"additionalProperties":null,"multipleValues":null,"objectType":null,"options":null,"properties":null,"controlType":"OBJECT_BUILDER","optionsDataSource":null}
                """,
            property);
    }

    @Test
    public void testStringProperty() throws JSONException, JsonProcessingException {
        Property property = ComponentDSL.string("name")
            .defaultValue("defaultValue")
            .description("description")
            .label("label")
            .placeholder("placeholder")
            .required(true);

        jsonAssertEquals(
            """
                {"advancedOption":null,"description":"description","displayCondition":null,"expressionEnabled":null,"hidden":null,"label":"label","metadata":{},"maxLength":null,"minLength":null,"placeholder":"placeholder","required":true,"name":"name","type":"STRING","defaultValue":"defaultValue","exampleValue":null,"controlType":"TEXT","options":null,"optionsDataSource":null}
                """,
            property);
    }

    @Test
    public void testPropertyOption() throws JSONException, JsonProcessingException {
        Option option = ComponentDSL.option("label", 1);

        jsonAssertEquals(
            """
                {"description":null,"displayCondition":null,"label":"label","value":1}
                """,
            option);

        option = ComponentDSL.option("label", "value");

        jsonAssertEquals(
            """
                {"description":null,"displayCondition":null,"label":"label","value":"value"}
                """,
            option);

        option = ComponentDSL.option("label", 1, "description");

        jsonAssertEquals(
            """
                {"description":"description","displayCondition":null,"label":"label","value":1}
                """,
            option);

        option = ComponentDSL.option("label", "value", "description");

        jsonAssertEquals(
            """
                {"description":"description","displayCondition":null,"label":"label","value":"value"}
                """,
            option);
    }

    private void jsonAssertEquals(String expectedString, Object jsonObject)
        throws JSONException, JsonProcessingException {

        String json = objectMapper.writeValueAsString(jsonObject);

        JSONAssert.assertEquals(expectedString, json, true);
    }
}
