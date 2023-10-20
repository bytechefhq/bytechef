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

package com.integri.atlas.task.handler.xml.converter;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONParser;

/**
 * @author Ivica Cardic
 */
public class XMLConverterTaskDefinitionTest {

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
                "description":"Converts between XML string and object/array.",
                "displayName":"XML Converter",
                "name":"xmlConverter",
                "properties":[
                    {
                        "defaultValue":"FROM_XML",
                        "description":"The operation to perform.",
                        "displayName":"Operation",
                        "name":"operation",
                        "required":true,
                        "type":"SELECT",
                        "options":[
                            {
                                "name":"Convert from XML string",
                                "value":"FROM_XML",
                                "description": "Converts the XML string to object/array."
                            },
                            {
                                "name":"Convert to XML string",
                                "value":"TO_XML",
                                 "description": "Writes the object/array to a XML string."
                            }
                        ]
                    },
                    {
                        "description":"XML string to convert to the data.",
                        "displayName":"Input",
                        "displayOption":{
                            "show":{
                                "operation":["FROM_XML"]
                            }
                        },
                        "name":"input",
                        "required":true,
                        "type":"STRING"
                    },
                        {
                        "description":"The data to convert to XML string.",
                        "displayName":"Input",
                        "displayOption":{
                            "show":{
                                "operation":["TO_XML"]
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
                objectMapper.writeValueAsString(XMLConverterTaskDefinition.TASK_SPECIFICATION)
            ),
            true
        );
    }
}
