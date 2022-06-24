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

package com.bytechef.hermes.descriptor.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONParser;

/**
 * @author Ivica Cardic
 */
public class TaskDescriptorTest {

    private final ObjectMapper objectMapper = new ObjectMapper() {
        {
            registerModule(new JavaTimeModule());

            setSerializationInclusion(JsonInclude.Include.NON_NULL);
        }
    };

    @Test
    public void testDisplayOption() throws JsonProcessingException, JSONException {
        DisplayOption displayOption = DisplayOption.build(List.of(DSL.hideWhen("name")));

        jsonAssertEquals(
                """
            {
                "hideWhen":{"name":{}}
            }
            """, displayOption);

        displayOption = DisplayOption.build(List.of(DSL.hideWhen("name").in(true, false)));

        jsonAssertEquals(
                """
        {
            "hideWhen":{"name":{"values": [true, false]}}
        }
        """,
                displayOption);

        displayOption = DisplayOption.build(List.of(DSL.hideWhen("name").in(1, 2)));

        jsonAssertEquals(
                """
            {
                "hideWhen":{"name":{"values": [1,2]}}
            }
            """,
                displayOption);

        displayOption = DisplayOption.build(List.of(DSL.hideWhen("name").in(1L, 2L)));

        jsonAssertEquals(
                """
            {
                "hideWhen":{"name":{"values": [1,2]}}
            }
            """,
                displayOption);

        displayOption = DisplayOption.build(List.of(DSL.hideWhen("name").in(1F, 2F)));

        jsonAssertEquals(
                """
        {
            "hideWhen":{"name":{"values": [1.0,2.0]}}
        }
        """,
                displayOption);

        displayOption = DisplayOption.build(List.of(DSL.hideWhen("name").in(1D, 2D)));

        jsonAssertEquals(
                """
        {
            "hideWhen":{"name":{"values": [1.0,2.0]}}
        }
        """,
                displayOption);

        displayOption = DisplayOption.build(List.of(DSL.hideWhen("name").in("value1", "value2")));

        jsonAssertEquals(
                """
        {
            "hideWhen":{"name":{"values": ["value1","value2"]}}
        }
        """,
                displayOption);

        displayOption = DisplayOption.build(List.of(DSL.hideWhen("name1").eq(1)));

        jsonAssertEquals(
                """
        {
         "hideWhen":{"name1":{"values": [1]}}
        }
        """, displayOption);

        displayOption = DisplayOption.build(
                List.of(DSL.hideWhen("name1").eq(1), DSL.hideWhen("name2").eq(2)));

        jsonAssertEquals(
                """
        {
            "hideWhen":{"name1":{"values": [1]},"name2":{"values": [2]}}
        }
        """,
                displayOption);

        displayOption = DisplayOption.build(List.of(DSL.showWhen("name").in(true, false)));

        jsonAssertEquals(
                """
        {
            "showWhen":{"name":{"values": [true, false]}}
        }
        """,
                displayOption);

        displayOption = DisplayOption.build(List.of(DSL.showWhen("name").in(1, 2)));

        jsonAssertEquals(
                """
            {
                "showWhen":{"name":{"values": [1,2]}}
            }
            """,
                displayOption);

        displayOption = DisplayOption.build(List.of(DSL.showWhen("name").in(1L, 2L)));

        jsonAssertEquals(
                """
            {
                "showWhen":{"name":{"values": [1,2]}}
            }
            """,
                displayOption);

        displayOption = DisplayOption.build(List.of(DSL.showWhen("name").in(1F, 2F)));

        jsonAssertEquals(
                """
        {
            "showWhen":{"name":{"values": [1.0,2.0]}}
        }
        """,
                displayOption);

        displayOption = DisplayOption.build(List.of(DSL.showWhen("name").in(1D, 2D)));

        jsonAssertEquals(
                """
        {
            "showWhen":{"name":{"values": [1.0,2.0]}}
        }
        """,
                displayOption);

        displayOption = DisplayOption.build(List.of(DSL.showWhen("name").in("value1", "value2")));

        jsonAssertEquals(
                """
        {
            "showWhen":{"name":{"values": ["value1","value2"]}}
        }
        """,
                displayOption);

        displayOption = DisplayOption.build(List.of(DSL.showWhen("name1").eq(1)));

        jsonAssertEquals(
                """
        {
         "showWhen":{"name1":{"values": [1]}}
        }
        """, displayOption);

        displayOption = DisplayOption.build(
                List.of(DSL.showWhen("name1").eq(1), DSL.showWhen("name2").eq(2)));

        jsonAssertEquals(
                """
        {
            "showWhen":{"name1":{"values": [1]},"name2":{"values": [2]}}
        }
        """,
                displayOption);
    }

    @Test
    public void testAnyTaskProperty() throws JsonProcessingException, JSONException {
        TaskProperty<?> taskProperty = DSL.ANY_PROPERTY("name")
                .description("description")
                .displayName("displayName")
                .placeholder("placeholder")
                .required(true)
                .types(DSL.STRING_PROPERTY());

        jsonAssertEquals(
                """
        {
            "description":"description",
            "displayName":"displayName",
            "name":"name",
            "placeholder":"placeholder",
            "required":true,
            "types":[{"type":"STRING"}]
        }
        """,
                taskProperty);
    }

    @Test
    public void testArrayTaskProperty() throws JsonProcessingException, JSONException {
        TaskProperty<?> taskProperty = DSL.ARRAY_PROPERTY("name")
                .defaultValue(1, 2)
                .description("description")
                .displayName("displayName")
                .placeholder("placeholder")
                .required(true)
                .items(DSL.STRING_PROPERTY());

        jsonAssertEquals(
                """
        {
            "defaultValue":[1,2],
            "description":"description",
            "displayName":"displayName",
            "items":[{"type":"STRING"}],
            "name":"name",
            "placeholder":"placeholder",
            "required":true,
            "type":"ARRAY"
        }
        """,
                taskProperty);
    }

    @Test
    public void testBooleanTaskProperty() throws JsonProcessingException, JSONException {
        TaskProperty<?> taskProperty = DSL.BOOLEAN_PROPERTY("name")
                .defaultValue(true)
                .description("description")
                .displayName("displayName")
                .placeholder("placeholder")
                .required(true);

        jsonAssertEquals(
                """
        {
            "defaultValue":true,
            "description":"description",
            "displayName":"displayName",
            "name":"name",
            "placeholder":"placeholder",
            "required":true,
            "type":"BOOLEAN"
        }
        """,
                taskProperty);
    }

    @Test
    public void testDateTimeTaskProperty() throws JsonProcessingException, JSONException {
        TaskProperty<?> taskProperty = DSL.DATE_TIME_PROPERTY("name")
                .defaultValue(LocalDateTime.MIN)
                .description("description")
                .displayName("displayName")
                .placeholder("placeholder")
                .required(true);

        jsonAssertEquals(
                """
            {
                "defaultValue":[-999999999,1,1,0,0],
                "description":"description",
                "displayName":"displayName",
                "name":"name",
                "placeholder":"placeholder",
                "required":true,
                "type":"DATE_TIME"
            }
            """,
                taskProperty);
    }

    @Test
    public void testIntegerTaskProperty() throws JsonProcessingException, JSONException {
        TaskProperty<?> taskProperty = DSL.INTEGER_PROPERTY("name")
                .defaultValue(2)
                .description("description")
                .displayName("displayName")
                .placeholder("placeholder")
                .required(true);

        jsonAssertEquals(
                """
            {
                "defaultValue":2,
                "description":"description",
                "displayName":"displayName",
                "name":"name",
                "placeholder":"placeholder",
                "required":true,
                "type":"INTEGER"
            }
            """,
                taskProperty);
    }

    @Test
    public void testNullTaskProperty() throws JsonProcessingException, JSONException {
        TaskProperty<?> taskProperty = DSL.NULL_PROPERTY("name")
                .description("description")
                .displayName("displayName")
                .placeholder("placeholder")
                .required(true);

        jsonAssertEquals(
                """
            {
                "description":"description",
                "displayName":"displayName",
                "name":"name",
                "placeholder":"placeholder",
                "required":true,
                "type":"NULL"
            }
            """,
                taskProperty);
    }

    @Test
    public void testNumberTaskProperty() throws JsonProcessingException, JSONException {
        TaskProperty<?> taskProperty = DSL.NUMBER_PROPERTY("name")
                .defaultValue(2)
                .description("description")
                .displayName("displayName")
                .options(DSL.option("option1", 1), DSL.option("option2", 2))
                .placeholder("placeholder")
                .required(true);

        jsonAssertEquals(
                """
            {
                "defaultValue":2,
                "description":"description",
                "displayName":"displayName",
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
                taskProperty);
    }

    @Test
    public void testObjectTaskProperty() throws JsonProcessingException, JSONException {
        TaskProperty<?> taskProperty = DSL.OBJECT_PROPERTY("name")
                .defaultValue(List.of(Map.of("key", Map.of("key1", "value1"))))
                .description("description")
                .displayName("displayName")
                .placeholder("placeholder")
                .required(true);

        jsonAssertEquals(
                """
            {
                "defaultValue":[{"key":{"key1":"value1"}}],
                "description":"description",
                "displayName":"displayName",
                "name":"name",
                "placeholder":"placeholder",
                "required":true,
                "type":"OBJECT"
            }
            """,
                taskProperty);
    }

    @Test
    public void testTaskOperation() throws JsonProcessingException, JSONException {
        TaskOperation taskOperation =
                DSL.OPERATION("name").description("description").displayName("displayName");

        jsonAssertEquals(
                """
            {
                "description":"description",
                "displayName":"displayName",
                "name":"name"
            }
            """,
                taskOperation);
    }

    @Test
    public void testOptionTaskProperty() throws JsonProcessingException, JSONException {
        TaskProperty<?> taskProperty = DSL.OPTIONS()
                .description("description")
                .displayName("displayName")
                .options(DSL.STRING_PROPERTY("stringProperty"))
                .placeholder("placeholder");

        jsonAssertEquals(
                """
            {
                "description":"description",
                "displayName":"displayName",
                "options": [
                    {
                        "name":"stringProperty",
                        "type":"STRING"
                    }
                ],
                "placeholder":"placeholder"
            }
            """,
                taskProperty);
    }

    @Test
    public void testStringTaskProperty() throws JsonProcessingException, JSONException {
        TaskProperty<?> taskProperty = DSL.STRING_PROPERTY("name")
                .defaultValue("defaultValue")
                .description("description")
                .displayName("displayName")
                .placeholder("placeholder")
                .required(true);

        jsonAssertEquals(
                """
            {
                "defaultValue":"defaultValue",
                "description":"description",
                "displayName":"displayName",
                "name":"name",
                "placeholder":"placeholder",
                "required":true,
                "type":"STRING"
            }
            """,
                taskProperty);
    }

    @Test
    public void testTaskDescriptor() throws JsonProcessingException, JSONException {
        TaskDescriptor taskDescriptor = DSL.createTaskDescriptor("name")
                .displayName("displayName")
                .description("description")
                .subtitle("subtitle")
                .authEnricher()
                .icon("icon")
                .version(1);

        jsonAssertEquals(
                """
            {
                "authEnricher":{options:[]},
                "description":"description",
                "displayName":"displayName",
                "name":"name",
                "icon":"icon",
                "subtitle":"subtitle",
                "version":1.0
            }
            """,
                taskDescriptor);
    }

    @Test
    public void testTaskPropertyOption() throws JsonProcessingException, JSONException {
        TaskPropertyOption taskPropertyOption = DSL.option("name", 1);

        jsonAssertEquals(
                """
            {
                "name":"name",
                "value":1
            }
            """,
                taskPropertyOption);

        taskPropertyOption = DSL.option("name", "value");

        jsonAssertEquals(
                """
            {
                "name":"name",
                "value":"value"
            }
            """,
                taskPropertyOption);

        taskPropertyOption = DSL.option("name", 1, "description");

        jsonAssertEquals(
                """
            {
                "name":"name",
                "value":1,
                "description":"description"
            }
            """,
                taskPropertyOption);

        taskPropertyOption = DSL.option("name", "value", "description");

        jsonAssertEquals(
                """
            {
                "name":"name",
                "value":"value",
                "description":"description"
            }
            """,
                taskPropertyOption);
    }

    private void assertEquals(String expectedString, Object object) throws JsonProcessingException {
        Assertions.assertEquals(expectedString, objectMapper.writeValueAsString(object));
    }

    private void jsonAssertEquals(String expectedString, Object jsonObject)
            throws JSONException, JsonProcessingException {
        JSONAssert.assertEquals(
                expectedString, (JSONObject) JSONParser.parseJSON(objectMapper.writeValueAsString(jsonObject)), true);
    }
}
