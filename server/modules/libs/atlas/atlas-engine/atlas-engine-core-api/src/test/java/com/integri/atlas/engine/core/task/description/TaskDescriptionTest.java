/*
 * Copyright 2016-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Modifications copyright (C) 2021 <your company/name>
 */

package com.integri.atlas.engine.core.task.description;

import static com.integri.atlas.engine.core.task.description.DisplayOption.displayOption;
import static com.integri.atlas.engine.core.task.description.TaskAuthentication.authentication;
import static com.integri.atlas.engine.core.task.description.TaskCredential.credential;
import static com.integri.atlas.engine.core.task.description.TaskDescription.task;
import static com.integri.atlas.engine.core.task.description.TaskParameter.parameter;
import static com.integri.atlas.engine.core.task.description.TaskParameterValue.parameterValue;
import static com.integri.atlas.engine.core.task.description.TaskParameterValue.parameterValues;
import static com.integri.atlas.engine.core.task.description.TaskProperty.SELECT_PROPERTY;
import static com.integri.atlas.engine.core.task.description.TaskPropertyOption.option;
import static com.integri.atlas.engine.core.task.description.TaskPropertyOptionValue.optionValue;
import static com.integri.atlas.engine.core.task.description.TaskPropertyTypeOption.propertyTypeOption;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONParser;

/**
 * @author Ivica Cardic
 */
public class TaskDescriptionTest {

    private final ObjectMapper objectMapper = new ObjectMapper() {
        {
            setSerializationInclusion(JsonInclude.Include.NON_NULL);
        }
    };

    @Test
    public void testDisplayOption() throws JsonProcessingException, JSONException {
        DisplayOption displayOption = displayOption().hide("name");

        jsonAssertEquals("""
            {
                "hide":{"name":[]}
            }
            """, displayOption);

        displayOption = displayOption().hide("name", true, false);

        jsonAssertEquals("""
        {
            "hide":{"name":[true, false]}
        }
        """, displayOption);

        displayOption = displayOption().hide("name", 1, 2);

        jsonAssertEquals(
            """
            {
                "hide":{"name":[1,2]}
            }
            """,
            displayOption
        );

        displayOption = displayOption().hide("name", 1L, 2L);

        jsonAssertEquals(
            """
            {
                "hide":{"name":[1,2]}
            }
            """,
            displayOption
        );

        displayOption = displayOption().hide("name", 1F, 2F);

        jsonAssertEquals("""
        {
            "hide":{"name":[1.0,2.0]}
        }
        """, displayOption);

        displayOption = displayOption().hide("name", 1D, 2D);

        jsonAssertEquals("""
        {
            "hide":{"name":[1.0,2.0]}
        }
        """, displayOption);

        displayOption = displayOption().hide("name", "value1", "value2");

        jsonAssertEquals(
            """
        {
            "hide":{"name":["value1","value2"]}
        }
        """,
            displayOption
        );

        displayOption = displayOption().hide("name1", parameterValues(1));

        jsonAssertEquals("""
        {
         "hide":{"name1":[1]}
        }
        """, displayOption);

        displayOption = displayOption().hide("name1", parameterValues(1), "name2", parameterValues(2));

        jsonAssertEquals("""
        {
            "hide":{"name1":[1],"name2":[2]}
        }
        """, displayOption);

        displayOption =
            displayOption().hide("name1", parameterValues(1), "name2", parameterValues(2), "name3", parameterValues(3));

        jsonAssertEquals(
            """
        {
            "hide":{"name1":[1],"name2":[2],"name3":[3]}
        }
        """,
            displayOption
        );

        displayOption =
            displayOption()
                .hide(
                    "name1",
                    parameterValues(1),
                    "name2",
                    parameterValues(2),
                    "name3",
                    parameterValues(3),
                    "name4",
                    parameterValues(4)
                );

        jsonAssertEquals(
            """
        {
            "hide":{"name1":[1],"name2":[2],"name3":[3],"name4":[4]}
        }
        """,
            displayOption
        );

        displayOption =
            displayOption()
                .hide(
                    "name1",
                    parameterValues(1),
                    "name2",
                    parameterValues(2),
                    "name3",
                    parameterValues(3),
                    "name4",
                    parameterValues(4),
                    "name5",
                    parameterValues(5)
                );

        jsonAssertEquals(
            """
        {
            "hide":{"name1":[1],"name2":[2],"name3":[3],"name4":[4],"name5":[5]}
        }
        """,
            displayOption
        );

        displayOption =
            displayOption()
                .hide(
                    "name1",
                    parameterValues(1),
                    "name2",
                    parameterValues(2),
                    "name3",
                    parameterValues(3),
                    "name4",
                    parameterValues(4),
                    "name5",
                    parameterValues(5),
                    "name6",
                    parameterValues(6)
                );

        jsonAssertEquals(
            """
        {
            "hide":{"name1":[1],"name2":[2],"name3":[3],"name4":[4],"name5":[5],"name6":[6]}
        }
        """,
            displayOption
        );

        displayOption =
            displayOption()
                .hide(
                    "name1",
                    parameterValues(1),
                    "name2",
                    parameterValues(2),
                    "name3",
                    parameterValues(3),
                    "name4",
                    parameterValues(4),
                    "name5",
                    parameterValues(5),
                    "name6",
                    parameterValues(6),
                    "name7",
                    parameterValues(7)
                );

        jsonAssertEquals(
            """
        {
            "hide":{"name1":[1],"name2":[2],"name3":[3],"name4":[4],"name5":[5],"name6":[6],"name7":[7]}
        }
        """,
            displayOption
        );

        displayOption =
            displayOption()
                .hide(
                    "name1",
                    parameterValues(1),
                    "name2",
                    parameterValues(2),
                    "name3",
                    parameterValues(3),
                    "name4",
                    parameterValues(4),
                    "name5",
                    parameterValues(5),
                    "name6",
                    parameterValues(6),
                    "name7",
                    parameterValues(7),
                    "name8",
                    parameterValues(8)
                );

        jsonAssertEquals(
            """
        {
            "hide":{"name1":[1],"name2":[2],"name3":[3],"name4":[4],"name5":[5],"name6":[6],"name7":[7],"name8":[8]}
        }
        """,
            displayOption
        );

        displayOption =
            displayOption()
                .hide(
                    "name1",
                    parameterValues(1),
                    "name2",
                    parameterValues(2),
                    "name3",
                    parameterValues(3),
                    "name4",
                    parameterValues(4),
                    "name5",
                    parameterValues(5),
                    "name6",
                    parameterValues(6),
                    "name7",
                    parameterValues(7),
                    "name8",
                    parameterValues(8),
                    "name9",
                    parameterValues(9)
                );

        jsonAssertEquals(
            """
        {
            "hide":{
                "name1":[1],"name2":[2],"name3":[3],"name4":[4],"name5":[5],"name6":[6],"name7":[7],"name8":[8],
                "name9":[9]
            }
        }
        """,
            displayOption
        );

        displayOption =
            displayOption()
                .hide(
                    "name1",
                    parameterValues(1),
                    "name2",
                    parameterValues(2),
                    "name3",
                    parameterValues(3),
                    "name4",
                    parameterValues(4),
                    "name5",
                    parameterValues(5),
                    "name6",
                    parameterValues(6),
                    "name7",
                    parameterValues(7),
                    "name8",
                    parameterValues(8),
                    "name9",
                    parameterValues(9),
                    "name10",
                    parameterValues(10)
                );

        jsonAssertEquals(
            """
        {
            "hide":{
                "name1":[1],"name2":[2],"name3":[3],"name4":[4],"name5":[5],"name6":[6],"name7":[7],"name8":[8],
                "name9":[9],"name10":[10]
            }
        }
        """,
            displayOption
        );

        displayOption = displayOption().show("name");

        jsonAssertEquals("""
            {
                "show":{"name":[]}
            }
            """, displayOption);

        displayOption = displayOption().show("name", true, false);

        jsonAssertEquals("""
        {
            "show":{"name":[true, false]}
        }
        """, displayOption);

        displayOption = displayOption().show("name", 1, 2);

        jsonAssertEquals(
            """
            {
                "show":{"name":[1,2]}
            }
            """,
            displayOption
        );

        displayOption = displayOption().show("name", 1L, 2L);

        jsonAssertEquals(
            """
            {
                "show":{"name":[1,2]}
            }
            """,
            displayOption
        );

        displayOption = displayOption().show("name", 1F, 2F);

        jsonAssertEquals("""
        {
            "show":{"name":[1.0,2.0]}
        }
        """, displayOption);

        displayOption = displayOption().show("name", 1D, 2D);

        jsonAssertEquals("""
        {
            "show":{"name":[1.0,2.0]}
        }
        """, displayOption);

        displayOption = displayOption().show("name", "value1", "value2");

        jsonAssertEquals(
            """
        {
            "show":{"name":["value1","value2"]}
        }
        """,
            displayOption
        );

        displayOption = displayOption().show("name1", parameterValues(1));

        jsonAssertEquals("""
        {
         "show":{"name1":[1]}
        }
        """, displayOption);

        displayOption = displayOption().show("name1", parameterValues(1), "name2", parameterValues(2));

        jsonAssertEquals("""
        {
            "show":{"name1":[1],"name2":[2]}
        }
        """, displayOption);

        displayOption =
            displayOption().show("name1", parameterValues(1), "name2", parameterValues(2), "name3", parameterValues(3));

        jsonAssertEquals(
            """
        {
            "show":{"name1":[1],"name2":[2],"name3":[3]}
        }
        """,
            displayOption
        );

        displayOption =
            displayOption()
                .show(
                    "name1",
                    parameterValues(1),
                    "name2",
                    parameterValues(2),
                    "name3",
                    parameterValues(3),
                    "name4",
                    parameterValues(4)
                );

        jsonAssertEquals(
            """
        {
            "show":{"name1":[1],"name2":[2],"name3":[3],"name4":[4]}
        }
        """,
            displayOption
        );

        displayOption =
            displayOption()
                .show(
                    "name1",
                    parameterValues(1),
                    "name2",
                    parameterValues(2),
                    "name3",
                    parameterValues(3),
                    "name4",
                    parameterValues(4),
                    "name5",
                    parameterValues(5)
                );

        jsonAssertEquals(
            """
        {
            "show":{"name1":[1],"name2":[2],"name3":[3],"name4":[4],"name5":[5]}
        }
        """,
            displayOption
        );

        displayOption =
            displayOption()
                .show(
                    "name1",
                    parameterValues(1),
                    "name2",
                    parameterValues(2),
                    "name3",
                    parameterValues(3),
                    "name4",
                    parameterValues(4),
                    "name5",
                    parameterValues(5),
                    "name6",
                    parameterValues(6)
                );

        jsonAssertEquals(
            """
        {
            "show":{"name1":[1],"name2":[2],"name3":[3],"name4":[4],"name5":[5],"name6":[6]}
        }
        """,
            displayOption
        );

        displayOption =
            displayOption()
                .show(
                    "name1",
                    parameterValues(1),
                    "name2",
                    parameterValues(2),
                    "name3",
                    parameterValues(3),
                    "name4",
                    parameterValues(4),
                    "name5",
                    parameterValues(5),
                    "name6",
                    parameterValues(6),
                    "name7",
                    parameterValues(7)
                );

        jsonAssertEquals(
            """
        {
            "show":{"name1":[1],"name2":[2],"name3":[3],"name4":[4],"name5":[5],"name6":[6],"name7":[7]}
        }
        """,
            displayOption
        );

        displayOption =
            displayOption()
                .show(
                    "name1",
                    parameterValues(1),
                    "name2",
                    parameterValues(2),
                    "name3",
                    parameterValues(3),
                    "name4",
                    parameterValues(4),
                    "name5",
                    parameterValues(5),
                    "name6",
                    parameterValues(6),
                    "name7",
                    parameterValues(7),
                    "name8",
                    parameterValues(8)
                );

        jsonAssertEquals(
            """
        {
            "show":{"name1":[1],"name2":[2],"name3":[3],"name4":[4],"name5":[5],"name6":[6],"name7":[7],"name8":[8]}
        }
        """,
            displayOption
        );

        displayOption =
            displayOption()
                .show(
                    "name1",
                    parameterValues(1),
                    "name2",
                    parameterValues(2),
                    "name3",
                    parameterValues(3),
                    "name4",
                    parameterValues(4),
                    "name5",
                    parameterValues(5),
                    "name6",
                    parameterValues(6),
                    "name7",
                    parameterValues(7),
                    "name8",
                    parameterValues(8),
                    "name9",
                    parameterValues(9)
                );

        jsonAssertEquals(
            """
        {
            "show":{
                "name1":[1],"name2":[2],"name3":[3],"name4":[4],"name5":[5],"name6":[6],"name7":[7],"name8":[8],
                "name9":[9]
            }
        }
        """,
            displayOption
        );

        displayOption =
            displayOption()
                .show(
                    "name1",
                    parameterValues(1),
                    "name2",
                    parameterValues(2),
                    "name3",
                    parameterValues(3),
                    "name4",
                    parameterValues(4),
                    "name5",
                    parameterValues(5),
                    "name6",
                    parameterValues(6),
                    "name7",
                    parameterValues(7),
                    "name8",
                    parameterValues(8),
                    "name9",
                    parameterValues(9),
                    "name10",
                    parameterValues(10)
                );

        jsonAssertEquals(
            """
        {
            "show":{
                "name1":[1],"name2":[2],"name3":[3],"name4":[4],"name5":[5],"name6":[6],"name7":[7],"name8":[8],
                "name9":[9],"name10":[10]
            }
        }
        """,
            displayOption
        );
    }

    @Test
    public void testTaskAuthentication() throws JsonProcessingException, JSONException {
        TaskAuthentication taskAuthentication = authentication()
            .credentials(credential())
            .properties(TaskProperty.STRING_PROPERTY("name"));

        jsonAssertEquals(
            """
        {
            "credentials":[{}],"properties":[{name:"name",type:STRING}]
        }
        """,
            taskAuthentication
        );
    }

    @Test
    public void testTaskCredential() throws JsonProcessingException, JSONException {
        TaskCredential taskCredential = credential().displayOption(displayOption()).name("name").required(true);

        jsonAssertEquals(
            """
        {
            "name":"name","required":true,"displayOption":{}
        }
        """,
            taskCredential
        );
    }

    @Test
    public void testTaskDescription() throws JsonProcessingException, JSONException {
        TaskDescription taskDescription = task("name")
            .displayName("displayName")
            .description("description")
            .subtitle("subtitle")
            .authentication(authentication().credentials(credential()).properties(TaskProperty.STRING_PROPERTY("name")))
            .properties(TaskProperty.STRING_PROPERTY("name"))
            .icon("icon")
            .version(1);

        jsonAssertEquals(
            """
        {
            "authentication":{
                "credentials":[{}],
                "properties":[{name:"name",type:STRING}]
            },
            "description":"description",
            "displayName":"displayName",
            "name":"name",
            "icon":"icon",
            "properties":[{"name":"name","type":"STRING"}],
            "subtitle":"subtitle",
            "version":1.0
        }
        """,
            taskDescription
        );
    }

    @Test
    public void testTaskParameter() throws JsonProcessingException, JSONException {
        TaskParameter taskParameter = parameter(true);

        assertEquals("true", taskParameter);

        taskParameter = parameter(true, false);

        assertEquals("[true,false]", taskParameter);

        taskParameter = parameter(1);

        assertEquals("1", taskParameter);

        taskParameter = parameter(1, 2);

        assertEquals("[1,2]", taskParameter);

        taskParameter = parameter(1L);

        assertEquals("1", taskParameter);

        taskParameter = parameter(1L, 2L);

        assertEquals("[1,2]", taskParameter);

        taskParameter = parameter(1F);

        assertEquals("1.0", taskParameter);

        taskParameter = parameter(1F, 2F);

        assertEquals("[1.0,2.0]", taskParameter);

        taskParameter = parameter(1D);

        assertEquals("1.0", taskParameter);

        taskParameter = parameter(1D, 2D);

        assertEquals("[1.0,2.0]", taskParameter);

        taskParameter = parameter("parameter", true);

        jsonAssertEquals("""
        {
            "parameter":true
        }
        """, taskParameter);

        taskParameter = parameter("parameter", true, false);

        jsonAssertEquals("""
        {
            "parameter":[true,false]
        }
        """, taskParameter);

        taskParameter = parameter("parameter", 1);

        jsonAssertEquals("""
        {
            "parameter":1
        }
        """, taskParameter);

        taskParameter = parameter("parameter", 1, 2);

        jsonAssertEquals("""
        {
            "parameter":[1,2]
        }
        """, taskParameter);

        taskParameter = parameter("parameter", 1L);

        jsonAssertEquals("""
        {
            "parameter":1
        }
        """, taskParameter);

        taskParameter = parameter("parameter", 1L, 2L);

        jsonAssertEquals("""
        {
            "parameter":[1,2]
        }
        """, taskParameter);

        taskParameter = parameter("parameter", 1F);

        jsonAssertEquals("""
        {
            "parameter":1.0
        }
        """, taskParameter);

        taskParameter = parameter("parameter", 1F, 2F);

        jsonAssertEquals("""
        {
            "parameter":[1.0,2.0]
        }
        """, taskParameter);

        taskParameter = parameter("parameter", 1D);

        jsonAssertEquals("""
        {
            "parameter":1.0
        }
        """, taskParameter);

        taskParameter = parameter("parameter", 1D, 2D);

        jsonAssertEquals("""
        {
            "parameter":[1.0,2.0]
        }
        """, taskParameter);

        taskParameter = parameter("parameter", "value1");

        jsonAssertEquals("""
        {
            "parameter":"value1"
        }
        """, taskParameter);

        taskParameter = parameter("parameter", new String[] { "value1", "value2" });

        jsonAssertEquals("""
        {
            "parameter":["value1","value2"]
        }
        """, taskParameter);

        taskParameter = parameter("parameter1", parameter(1));

        jsonAssertEquals("""
        {
            "parameter1":1
        }
        """, taskParameter);

        taskParameter = parameter("parameter1", parameter("parameter2", 2));

        jsonAssertEquals(
            """
        {
            "parameter1":{
                parameter2: 2
            }
        }
        """,
            taskParameter
        );

        taskParameter = parameter("parameter1", parameterValue(1));

        jsonAssertEquals("""
        {
            "parameter1":1
        }
        """, taskParameter);

        taskParameter = parameter("parameter1", List.of(parameter(1)));

        jsonAssertEquals("""
        {
            "parameter1":[1]
        }
        """, taskParameter);

        taskParameter = parameter("parameter1", parameter(1), "parameter2", parameterValue(2));

        jsonAssertEquals(
            """
        {
             "parameter1":1,
             "parameter2":2
        }
        """,
            taskParameter
        );

        taskParameter = parameter("parameter1", parameter("parameter11", 1, 11), "parameter2", parameterValue(2));

        jsonAssertEquals(
            """
        {
             "parameter1":{
                parameter11: [1, 11]
             },
             "parameter2":2
        }
        """,
            taskParameter
        );

        taskParameter = parameter("parameter1", parameterValue((LocalDateTime) null));

        jsonAssertEquals("""
        {
             "parameter1":"null"
        }
        """, taskParameter);

        taskParameter = parameter("parameter1", parameterValue((JsonNode) null));

        jsonAssertEquals("""
        {
             "parameter1":"null"
        }
        """, taskParameter);

        taskParameter = parameter("parameter1", List.of(parameter(1)), "parameter2", List.of(parameter(2)));

        jsonAssertEquals(
            """
        {
             "parameter1":[1],
             "parameter2":[2]
        }
        """,
            taskParameter
        );

        taskParameter =
            parameter(
                "parameter1",
                parameterValue(1),
                "parameter2",
                parameterValue(2),
                "parameter3",
                parameterValue(3)
            );

        jsonAssertEquals(
            """
        {
             "parameter1":1,
             "parameter2":2,
             "parameter3":3
        }
        """,
            taskParameter
        );

        taskParameter =
            parameter(
                "parameter1",
                List.of(parameter(1)),
                "parameter2",
                List.of(parameter(2)),
                "parameter3",
                List.of(parameter(3))
            );

        jsonAssertEquals(
            """
        {
             "parameter1":[1],
             "parameter2":[2],
             "parameter3":[3]
        }
        """,
            taskParameter
        );

        taskParameter =
            parameter(
                "parameter1",
                parameterValue(1),
                "parameter2",
                parameterValue(2),
                "parameter3",
                parameterValue(3),
                "parameter4",
                parameterValue(4)
            );

        jsonAssertEquals(
            """
        {
             "parameter1":1,
             "parameter2":2,
             "parameter3":3,
             "parameter4":4
        }
        """,
            taskParameter
        );

        taskParameter =
            parameter(
                "parameter1",
                List.of(parameter(1)),
                "parameter2",
                List.of(parameter(2)),
                "parameter3",
                List.of(parameter(3)),
                "parameter4",
                List.of(parameter(4))
            );

        jsonAssertEquals(
            """
        {
             "parameter1":[1],
             "parameter2":[2],
             "parameter3":[3],
             "parameter4":[4]
        }
        """,
            taskParameter
        );

        taskParameter =
            parameter(
                "parameter1",
                parameterValue(1),
                "parameter2",
                parameterValue(2),
                "parameter3",
                parameterValue(3),
                "parameter4",
                parameterValue(4),
                "parameter5",
                parameterValue(5)
            );

        jsonAssertEquals(
            """
        {
             "parameter1":1,
             "parameter2":2,
             "parameter3":3,
             "parameter4":4,
             "parameter5":5
        }
        """,
            taskParameter
        );

        taskParameter =
            parameter(
                "parameter1",
                List.of(parameter(1)),
                "parameter2",
                List.of(parameter(2)),
                "parameter3",
                List.of(parameter(3)),
                "parameter4",
                List.of(parameter(4)),
                "parameter5",
                List.of(parameter(5))
            );

        jsonAssertEquals(
            """
        {
             "parameter1":[1],
             "parameter2":[2],
             "parameter3":[3],
             "parameter4":[4],
             "parameter5":[5]
        }
        """,
            taskParameter
        );

        taskParameter =
            parameter(
                "parameter1",
                parameterValue(1),
                "parameter2",
                parameterValue(2),
                "parameter3",
                parameterValue(3),
                "parameter4",
                parameterValue(4),
                "parameter5",
                parameterValue(5),
                "parameter6",
                parameterValue(6)
            );

        jsonAssertEquals(
            """
        {
             "parameter1":1,
             "parameter2":2,
             "parameter3":3,
             "parameter4":4,
             "parameter5":5,
             "parameter6":6
        }
        """,
            taskParameter
        );

        taskParameter =
            parameter(
                "parameter1",
                List.of(parameter(1)),
                "parameter2",
                List.of(parameter(2)),
                "parameter3",
                List.of(parameter(3)),
                "parameter4",
                List.of(parameter(4)),
                "parameter5",
                List.of(parameter(5)),
                "parameter6",
                List.of(parameter(6))
            );

        jsonAssertEquals(
            """
        {
             "parameter1":[1],
             "parameter2":[2],
             "parameter3":[3],
             "parameter4":[4],
             "parameter5":[5],
             "parameter6":[6]
        }
        """,
            taskParameter
        );

        taskParameter =
            parameter(
                "parameter1",
                parameterValue(1),
                "parameter2",
                parameterValue(2),
                "parameter3",
                parameterValue(3),
                "parameter4",
                parameterValue(4),
                "parameter5",
                parameterValue(5),
                "parameter6",
                parameterValue(6),
                "parameter7",
                parameterValue(7)
            );

        jsonAssertEquals(
            """
        {
             "parameter1":1,
             "parameter2":2,
             "parameter3":3,
             "parameter4":4,
             "parameter5":5,
             "parameter6":6,
             "parameter7":7
        }
        """,
            taskParameter
        );

        taskParameter =
            parameter(
                "parameter1",
                List.of(parameter(1)),
                "parameter2",
                List.of(parameter(2)),
                "parameter3",
                List.of(parameter(3)),
                "parameter4",
                List.of(parameter(4)),
                "parameter5",
                List.of(parameter(5)),
                "parameter6",
                List.of(parameter(6)),
                "parameter7",
                List.of(parameter(7))
            );

        jsonAssertEquals(
            """
        {
             "parameter1":[1],
             "parameter2":[2],
             "parameter3":[3],
             "parameter4":[4],
             "parameter5":[5],
             "parameter6":[6],
             "parameter7":[7]
        }
        """,
            taskParameter
        );

        taskParameter =
            parameter(
                "parameter1",
                parameterValue(1),
                "parameter2",
                parameterValue(2),
                "parameter3",
                parameterValue(3),
                "parameter4",
                parameterValue(4),
                "parameter5",
                parameterValue(5),
                "parameter6",
                parameterValue(6),
                "parameter7",
                parameterValue(7),
                "parameter8",
                parameterValue(8)
            );

        jsonAssertEquals(
            """
        {
             "parameter1":1,
             "parameter2":2,
             "parameter3":3,
             "parameter4":4,
             "parameter5":5,
             "parameter6":6,
             "parameter7":7,
             "parameter8":8
        }
        """,
            taskParameter
        );

        taskParameter =
            parameter(
                "parameter1",
                List.of(parameter(1)),
                "parameter2",
                List.of(parameter(2)),
                "parameter3",
                List.of(parameter(3)),
                "parameter4",
                List.of(parameter(4)),
                "parameter5",
                List.of(parameter(5)),
                "parameter6",
                List.of(parameter(6)),
                "parameter7",
                List.of(parameter(7)),
                "parameter8",
                List.of(parameter(8))
            );

        jsonAssertEquals(
            """
        {
             "parameter1":[1],
             "parameter2":[2],
             "parameter3":[3],
             "parameter4":[4],
             "parameter5":[5],
             "parameter6":[6],
             "parameter7":[7],
             "parameter8":[8]
        }
        """,
            taskParameter
        );

        taskParameter =
            parameter(
                "parameter1",
                parameterValue(1),
                "parameter2",
                parameterValue(2),
                "parameter3",
                parameterValue(3),
                "parameter4",
                parameterValue(4),
                "parameter5",
                parameterValue(5),
                "parameter6",
                parameterValue(6),
                "parameter7",
                parameterValue(7),
                "parameter8",
                parameterValue(8),
                "parameter9",
                parameterValue(9)
            );

        jsonAssertEquals(
            """
        {
             "parameter1":1,
             "parameter2":2,
             "parameter3":3,
             "parameter4":4,
             "parameter5":5,
             "parameter6":6,
             "parameter7":7,
             "parameter8":8,
             "parameter9":9
        }
        """,
            taskParameter
        );

        taskParameter =
            parameter(
                "parameter1",
                List.of(parameter(1)),
                "parameter2",
                List.of(parameter(2)),
                "parameter3",
                List.of(parameter(3)),
                "parameter4",
                List.of(parameter(4)),
                "parameter5",
                List.of(parameter(5)),
                "parameter6",
                List.of(parameter(6)),
                "parameter7",
                List.of(parameter(7)),
                "parameter8",
                List.of(parameter(8)),
                "parameter9",
                List.of(parameter(9))
            );

        jsonAssertEquals(
            """
        {
             "parameter1":[1],
             "parameter2":[2],
             "parameter3":[3],
             "parameter4":[4],
             "parameter5":[5],
             "parameter6":[6],
             "parameter7":[7],
             "parameter8":[8],
             "parameter9":[9]
        }
        """,
            taskParameter
        );

        taskParameter =
            parameter(
                "parameter1",
                parameterValue(1),
                "parameter2",
                parameterValue(2),
                "parameter3",
                parameterValue(3),
                "parameter4",
                parameterValue(4),
                "parameter5",
                parameterValue(5),
                "parameter6",
                parameterValue(6),
                "parameter7",
                parameterValue(7),
                "parameter8",
                parameterValue(8),
                "parameter9",
                parameterValue(9),
                "parameter10",
                parameterValue(10)
            );

        jsonAssertEquals(
            """
        {
             "parameter1":1,
             "parameter2":2,
             "parameter3":3,
             "parameter4":4,
             "parameter5":5,
             "parameter6":6,
             "parameter7":7,
             "parameter8":8,
             "parameter9":9,
             "parameter10":10
        }
        """,
            taskParameter
        );

        taskParameter =
            parameter(
                "parameter1",
                List.of(parameter(1)),
                "parameter2",
                List.of(parameter(2)),
                "parameter3",
                List.of(parameter(3)),
                "parameter4",
                List.of(parameter(4)),
                "parameter5",
                List.of(parameter(5)),
                "parameter6",
                List.of(parameter(6)),
                "parameter7",
                List.of(parameter(7)),
                "parameter8",
                List.of(parameter(8)),
                "parameter9",
                List.of(parameter(9)),
                "parameter10",
                List.of(parameter(10))
            );

        jsonAssertEquals(
            """
        {
             "parameter1":[1],
             "parameter2":[2],
             "parameter3":[3],
             "parameter4":[4],
             "parameter5":[5],
             "parameter6":[6],
             "parameter7":[7],
             "parameter8":[8],
             "parameter9":[9],
             "parameter10":[10]
        }
        """,
            taskParameter
        );

        taskParameter = parameter("parameter1", parameterValue(1), parameterValue(2));

        jsonAssertEquals("""
        {
            parameter1: [1,2]
        }
        """, taskParameter);

        taskParameter = parameter(new String[] { "parameter1", "parameter1", "parameter1" });

        assertEquals("""
            ["parameter1","parameter1","parameter1"]""", taskParameter);

        taskParameter = parameter(parameterValue(1), parameterValue(2), parameterValue(3));

        assertEquals("[1,2,3]", taskParameter);
    }

    @Test
    public void testTaskParameterValue() throws JsonProcessingException {
        TaskParameterValue taskParameterValue = parameterValue(true);

        assertEquals("true", taskParameterValue);

        taskParameterValue = parameterValue(1);

        assertEquals("1", taskParameterValue);

        taskParameterValue = parameterValue(1L);

        assertEquals("1", taskParameterValue);

        taskParameterValue = parameterValue(1F);

        assertEquals("1.0", taskParameterValue);

        taskParameterValue = parameterValue(1D);

        assertEquals("1.0", taskParameterValue);

        taskParameterValue = parameterValue("value");

        assertEquals("""
            "value\"""", taskParameterValue);

        List<TaskParameterValue> taskParameterValues = parameterValues(true, false);

        assertEquals("[true,false]", taskParameterValues);

        taskParameterValues = parameterValues(1, 2);

        assertEquals("[1,2]", taskParameterValues);

        taskParameterValues = parameterValues(1L, 2L);

        assertEquals("[1,2]", taskParameterValues);

        taskParameterValues = parameterValues(1F, 2F);

        assertEquals("[1.0,2.0]", taskParameterValues);

        taskParameterValues = parameterValues(1D, 2D);

        assertEquals("[1.0,2.0]", taskParameterValues);

        taskParameterValues = parameterValues("value1", "value2");

        assertEquals("""
            ["value1","value2"]""", taskParameterValues);
    }

    @Test
    public void testTaskProperty() throws JsonProcessingException, JSONException {
        TaskProperty<?> taskProperty = SELECT_PROPERTY("name")
            .defaultValue(2)
            .description("description")
            .displayName("displayName")
            .displayOption(displayOption())
            .options(option("option1", 1), option("option2", 2))
            .placeholder("placeholder")
            .required(true)
            .typeOption(propertyTypeOption());

        jsonAssertEquals(
            """
        {
            "defaultValue":2,
            "description":"description",
            "displayName":"displayName",
            "displayOption":{},
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
            "type":"SELECT",
            "typeOption":{"multipleValues":false}
        }
        """,
            taskProperty
        );
    }

    @Test
    public void testTaskPropertyOption() throws JsonProcessingException, JSONException {
        TaskPropertyOption taskPropertyOption = option("name", 1);

        jsonAssertEquals(
            """
        {
            "name":"name",
            "value":1
        }
        """,
            taskPropertyOption
        );

        taskPropertyOption = option("name", "value");

        jsonAssertEquals(
            """
        {
            "name":"name",
            "value":"value"
        }
        """,
            taskPropertyOption
        );

        taskPropertyOption = option("name", 1, "description");

        jsonAssertEquals(
            """
        {
            "name":"name",
            "value":1,
            "description":"description"
        }
        """,
            taskPropertyOption
        );

        taskPropertyOption = option("name", "value", "description");

        jsonAssertEquals(
            """
        {
            "name":"name",
            "value":"value",
            "description":"description"
        }
        """,
            taskPropertyOption
        );
    }

    @Test
    public void testTaskPropertyOptionValue() throws JsonProcessingException {
        TaskPropertyOptionValue taskPropertyOptionValue = optionValue(1);

        assertEquals("1", taskPropertyOptionValue);

        taskPropertyOptionValue = optionValue("value");

        assertEquals("\"value\"", taskPropertyOptionValue);
    }

    @Test
    public void testTaskPropertyTypeOption() throws JsonProcessingException, JSONException {
        TaskPropertyTypeOption taskPropertyTypeOption = propertyTypeOption()
            .loadOptionsDependsOn("property1", "property2")
            .loadOptionsMethod("method")
            .maxValue(2)
            .minValue(1)
            .multipleValues(true)
            .multipleValueButtonText("Add")
            .numberPrecision(2);

        jsonAssertEquals(
            """
        {
            "loadOptionsDependsOn":["property1","property2"],
            "loadOptionsMethod":"method",
            "maxValue":2.0,
            "minValue":1.0,
            "multipleValues":true,
            "multipleValueButtonText":"Add",
            "numberPrecision":2
        }
        """,
            taskPropertyTypeOption
        );
    }

    private void assertEquals(String expectedString, Object object) throws JsonProcessingException {
        Assertions.assertEquals(expectedString, objectMapper.writeValueAsString(object));
    }

    private void jsonAssertEquals(String expectedString, Object jsonObject)
        throws JSONException, JsonProcessingException {
        JSONAssert.assertEquals(
            expectedString,
            (JSONObject) JSONParser.parseJSON(objectMapper.writeValueAsString(jsonObject)),
            true
        );
    }
}
