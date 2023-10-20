/*
 * Copyright 2021 <your company/name>.
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
 */

package com.integri.atlas.task.handler.json.converter;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.integri.atlas.task.definition.TaskDeclaration;
import com.integri.atlas.task.definition.dsl.TaskSpecification;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONParser;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
public class JSONConverterTaskDeclarationTest {

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
                "displayName":"JSON Converter",
                "name":"jsonConverter",
                "properties":[
                    {
                        "defaultValue":"FROM_JSON",
                        "description":"The operation to perform.",
                        "displayName":"Operation",
                        "name":"operation",
                        "required":true,
                        "type":"SELECT",
                        "options":[
                            {
                                "name":"Convert from JSON string",
                                "value":"FROM_JSON",
                                "description": "Converts the JSON string to object/array."
                            },
                            {
                                "name":"Convert to JSON string",
                                "value":"TO_JSON",
                                 "description": "Writes the object/array to a JSON string."
                            }
                        ]
                    },
                    {
                        "description":"JSON string to convert to the data.",
                        "displayName":"Input",
                        "displayOption":{
                            "show":{
                                "operation":["FROM_JSON"]
                            }
                        },
                        "name":"input",
                        "required":true,
                        "type":"STRING"
                    },
                        {
                        "description":"The data to convert to JSON string.",
                        "displayName":"Input",
                        "displayOption":{
                            "show":{
                                "operation":["TO_JSON"]
                            }
                        },
                        "name":"input",
                        "required":true,
                        "type":"JSON"
                    },
                ],
                "version":1.0
            }
            """,
            (JSONObject) JSONParser.parseJSON(
                objectMapper.writeValueAsString(JSONConverterTaskDeclaration.TASK_SPECIFICATION)
            ),
            true
        );
    }
}
