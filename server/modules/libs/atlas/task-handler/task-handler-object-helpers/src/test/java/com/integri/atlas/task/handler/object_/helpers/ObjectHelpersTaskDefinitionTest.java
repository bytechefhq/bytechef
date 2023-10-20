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

package com.integri.atlas.task.handler.object_.helpers;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.integri.atlas.task.handler.object_.helpers.ObjectHelpersTaskDefinition;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONParser;

/**
 * @author Ivica Cardic
 */
public class ObjectHelpersTaskDefinitionTest {

    private final ObjectMapper objectMapper = new ObjectMapper() {
        {
            setSerializationInclusion(JsonInclude.Include.NON_NULL);
        }
    };

    @Test
    public void testFileTaskSpecification() throws JsonProcessingException {
        JSONAssert.assertEquals(
            """
            {
                "description":"Converts between JSON string and object/array.",
                "displayName":"Object Helpers",
                "name":"objectHelpers",
                "properties":[
                    {
                        "defaultValue":"JSON_PARSE",
                        "description":"The operation to perform.",
                        "displayName":"Operation",
                        "name":"operation",
                        "required":true,
                        "type":"SELECT",
                        "options":[
                            {
                                "name":"Convert from JSON string",
                                "value":"JSON_PARSE",
                                "description": "Converts the JSON string to object/array."
                            },
                            {
                                "name":"Convert to JSON string",
                                "value":"JSON_STRINGIFY",
                                 "description": "Writes the object/array to a JSON string."
                            }
                        ]
                    },
                    {
                        "description":"The JSON string to convert to the data.",
                        "displayName":"Source",
                        "displayOption":{
                            "show":{
                                "operation":["JSON_PARSE"]
                            }
                        },
                        "name":"source",
                        "required":true,
                        "type":"STRING"
                    },
                        {
                        "description":"The data to convert to JSON string.",
                        "displayName":"Source",
                        "displayOption":{
                            "show":{
                                "operation":["JSON_STRINGIFY"]
                            }
                        },
                        "name":"source",
                        "required":true,
                        "type":"JSON"
                    },
                ],
                "version":1.0
            }
            """,
            (JSONObject) JSONParser.parseJSON(
                objectMapper.writeValueAsString(ObjectHelpersTaskDefinition.TASK_SPECIFICATION)
            ),
            true
        );
    }
}
