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
        ActionDefinition action = ComponentDsl.action("name")
            .title("title")
            .description("description")
            .perform(
                (ActionDefinition.SingleConnectionPerformFunction) (
                    inputParameters, connectionParameters, context) -> null);

        jsonAssertEquals(
            """
                {"batch":null,"deprecated":null,"description":"description","help":null,"metadata":null,"name":"name","outputDefinition":null,"properties":null,"title":"title","processErrorResponse":null,"workflowNodeDescription":null,"perform":{}}
                """,
            action);
    }

    @Test
    public void testArrayProperty() throws JSONException, JsonProcessingException {
        Property property = ComponentDsl.array("name")
            .defaultValue(1, 2)
            .description("description")
            .label("label")
            .placeholder("placeholder")
            .required(true)
            .items(ComponentDsl.integer());

        jsonAssertEquals(
            """
                {"advancedOption":null,"description":"description","displayCondition":null,"expressionEnabled":null,"hidden":null,"label":"label", "maxItems":null,"minItems":null,"metadata":{},"placeholder":"placeholder","required":true,"name":"name","type":"ARRAY","defaultValue":[1,2],"exampleValue":null,"items":[{"advancedOption":null,"description":null,"displayCondition":null,"expressionEnabled":null,"hidden":null,"label":null,"metadata":{},"placeholder":null,"required":null,"name":null,"type":"INTEGER","defaultValue":null,"exampleValue":null,"maxValue":null,"minValue":null,"options":null,"controlType":"INTEGER","optionsDataSource":null}],"multipleValues":null,"options":null,"controlType":"ARRAY_BUILDER","optionsDataSource":null}
                """,
            property);
    }

    @Test
    public void testBooleanProperty() throws JSONException, JsonProcessingException {
        Property property = ComponentDsl.bool("name")
            .defaultValue(true)
            .description("description")
            .label("label")
            .placeholder("placeholder")
            .required(true);

        jsonAssertEquals(
            """
                {"advancedOption":null,"description":"description","displayCondition":null,"expressionEnabled":null,"hidden":null,"label":"label","metadata":{},"placeholder":"placeholder","required":true,"name":"name","type":"BOOLEAN","defaultValue":true,"exampleValue":null,"options":[{"description":null,"label":"True","value":true},{"description":null,"label":"False","value":false}],"controlType":"SELECT"}
                """,
            property);
    }

    @Test
    public void testComponentDefinition() throws JSONException, JsonProcessingException {
        ComponentDefinition componentDefinition = ComponentDsl.component("name")
            .title("title")
            .description("description")
            .icon("icon")
            .version(1);

        jsonAssertEquals(
            """
                {"componentCategories":null,"customAction":null,"customActionHelp":null,"description":"description","icon":"icon","tags":null,"metadata":null,"name":"name","resources":null,"version":1,"title":"title","connection":null,"actions":null,"triggers":null,"unifiedApi":null,"clusterElements":null}
                """,
            componentDefinition);
    }

    @Test
    public void testDateProperty() throws JSONException, JsonProcessingException {
        Property property = ComponentDsl.date("name")
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
        Property property = ComponentDsl.dateTime("name")
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
        Property property = ComponentDsl.integer("name")
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
        Property property = ComponentDsl.number("name")
            .defaultValue(2)
            .description("description")
            .label("label")
            .options(ComponentDsl.option("option1", 1.0), ComponentDsl.option("option2", 2.0))
            .placeholder("placeholder")
            .required(true);

        jsonAssertEquals(
            """
                {"advancedOption":null,"description":"description","displayCondition":null,"expressionEnabled":null,"hidden":null,"label":"label","maxNumberPrecision":null,"minNumberPrecision":null,"metadata":{},"placeholder":"placeholder","required":true,"name":"name","type":"NUMBER","defaultValue":2.0,"exampleValue":null,"maxValue":null,"minValue":null,"numberPrecision":null,"options":[{"description":null,"label":"option1","value":1.0},{"description":null,"label":"option2","value":2.0}],"controlType":"SELECT","optionsDataSource":null}
                """,
            property);
    }

    @Test
    public void testObjectProperty() throws JSONException, JsonProcessingException {
        Property property = ComponentDsl.object("name")
            .defaultValue(Map.of("key", Map.of("key1", "value1")))
            .description("description")
            .label("label")
            .placeholder("placeholder")
            .required(true);

        jsonAssertEquals(
            """
                {"advancedOption":null,"description":"description","displayCondition":null,"expressionEnabled":null,"hidden":null,"metadata":{},"required":true,"name":"name","type":"OBJECT","defaultValue":{"key":{"key1":"value1"}},"exampleValue":null,"label":"label","placeholder":"placeholder","additionalProperties":null,"multipleValues":null,"options":null,"properties":null,"optionsDataSource":null,"controlType":"OBJECT_BUILDER"}
                """,
            property);
    }

    @Test
    public void testStringProperty() throws JSONException, JsonProcessingException {
        Property property = ComponentDsl.string("name")
            .defaultValue("defaultValue")
            .description("description")
            .label("label")
            .placeholder("placeholder")
            .required(true);

        jsonAssertEquals(
            """
                {"advancedOption":null,"description":"description","displayCondition":null,"expressionEnabled":null,"hidden":null,"label":"label","languageId":null,"metadata":{},"maxLength":null,"minLength":null,"placeholder":"placeholder","regex":null,"required":true,"name":"name","type":"STRING","defaultValue":"defaultValue","exampleValue":null,"controlType":"TEXT","options":null,"optionsDataSource":null, optionsLoadedDynamically:null}
                """,
            property);
    }

    @Test
    public void testPropertyOption() throws JSONException, JsonProcessingException {
        Option<?> option = ComponentDsl.option("label", 1);

        jsonAssertEquals(
            """
                {"description":null,"label":"label","value":1}
                """,
            option);

        option = ComponentDsl.option("label", "value");

        jsonAssertEquals(
            """
                {"description":null,"label":"label","value":"value"}
                """,
            option);

        option = ComponentDsl.option("label", 1, "description");

        jsonAssertEquals(
            """
                {"description":"description","label":"label","value":1}
                """,
            option);

        option = ComponentDsl.option("label", "value", "description");

        jsonAssertEquals(
            """
                {"description":"description","label":"label","value":"value"}
                """,
            option);
    }

    private void jsonAssertEquals(String expectedString, Object jsonObject)
        throws JSONException, JsonProcessingException {

        String json = objectMapper.writeValueAsString(jsonObject);

        JSONAssert.assertEquals(expectedString, json, true);
    }
}
