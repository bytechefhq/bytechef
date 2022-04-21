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

package com.integri.atlas.task.handler.xml.file;

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
public class XMLFileTaskDeclarationTest {

    private final ObjectMapper objectMapper = new ObjectMapper() {
        {
            setSerializationInclusion(JsonInclude.Include.NON_NULL);
        }
    };

    @Test
    public void testXMLFileTaskSpecification() throws JsonProcessingException {
        JSONAssert.assertEquals(
            """
            {
                "description":"Reads and writes data from a XML file",
                "displayName":"XML File",
                "name":"xmlFile",
                "properties":[
                    {
                        "defaultValue":"READ",
                        "description":"The operation to perform.",
                        "displayName":"Operation",
                        "name":"operation",
                        "required":true,
                        "type":"SELECT",
                        "options":[
                            {
                                "name":"Read from file",
                                "value":"READ",
                                "description": "Reads data from a XML file."
                            },
                            {
                                "name":"Write to file",
                                "value":"WRITE",
                                 "description": "Writes the data to a XML file."
                            }
                        ]
                    },
                    {
                        "description":"The object property which contains a reference to the XML file to read from.",
                        "displayName":"File",
                        "displayOption":{
                            "show":{
                                "operation":["READ"]
                            }
                        },
                        "name":"fileEntry",
                        "required":true,
                        "type":"FILE_ENTRY"
                    },
                    {
                        "description":"Data to write to the file.",
                        "displayName":"JSON object or array of items",
                        "displayOption":{
                            "show":{
                                "operation":["WRITE"]
                            }
                        },
                        "name":"items",
                        "required":true,
                        "type":"JSON"
                    },
                    {
                        "defaultValue":true,
                        "description":"The input JSON is array?",
                        "displayName":"Is Array",
                        "displayOption":{
                            "show":{
                                "operation":["READ"]
                            }
                        },
                        "name":"isArray",
                        "type":"BOOLEAN"
                    },
                    {
                        "displayName":"Options",
                        "name":"options",
                        "type":"COLLECTION",
                        "options":[
                            {
                                "defaultValue":"",
                                "description":"File name to set for binary data. By default, \\"file.xml\\" will be used.",
                                "displayName":"File Name",
                                "displayOption":{
                                    "show":{
                                        "operation":["WRITE"]
                                    }
                                },
                                "name":"fileName",
                                "type":"STRING"
                            },
                            {
                                "description":"The range to read from the XML array.",
                                "displayName":"Range",
                                "displayOption":{
                                    "show":{
                                        "isArray":[true],
                                        "operation":["READ"]
                                    }
                                },
                                "name":"range",
                                "type":"GROUP",
                                "groupProperties":[
                                    {
                                        "displayName":"The start index of the XML array",
                                        "name":"startIndex",
                                        "type":"NUMBER"
                                    },
                                    {
                                        "displayName":"The end index of the XML array",
                                        "name":"endIndex","type":"NUMBER"
                                    }
                                ]
                            },
                        ],
                        "placeholder":"Add Option"
                    }
                ],
                "version":1.0
            }
            """,
            (JSONObject) JSONParser.parseJSON(
                objectMapper.writeValueAsString(XMLFileTaskDeclaration.TASK_SPECIFICATION)
            ),
            true
        );
    }
}
