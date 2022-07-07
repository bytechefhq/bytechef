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

package com.bytechef.task.handler.jsonfile.v1_0;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

/**
 * @author Ivica Cardic
 */
public class JsonFileTaskDescriptorHandlerTest {

    private final ObjectMapper objectMapper = new ObjectMapper() {
        {
            setSerializationInclusion(JsonInclude.Include.NON_NULL);
        }
    };

    @Test
    public void testGetJSONFileTaskDescriptor() throws JsonProcessingException {
        JSONAssert.assertEquals(
                """
            {
              "description": "Reads and writes data from a JSON file.",
              "displayName": "JSON File",
              "name": "jsonFile",
              "operations": [
                {
                  "description": "Reads data from a JSON file.",
                  "name": "read",
                  "inputs": [
                    {
                      "description": "The file type to choose.",
                      "displayName": "File Type",
                      "name": "fileType",
                      "required": true,
                      "options": [
                        {
                          "name": "JSON",
                          "value": "JSON"
                        },
                        {
                          "name": "JSON Line",
                          "value": "JSONL"
                        }
                      ],
                      "defaultValue": "JSON",
                      "type": "STRING"
                    },
                    {
                      "description": "The object property which contains a reference to the JSON file to read from.",
                      "displayName": "File",
                      "name": "fileEntry",
                      "required": true,
                      "type": "OBJECT",
                      "properties": [
                        {
                          "name": "extension",
                          "required": true,
                          "type": "STRING"
                        },
                        {
                          "name": "mimeType",
                          "required": true,
                          "type": "STRING"
                        },
                        {
                          "name": "name",
                          "required": true,
                          "type": "STRING"
                        },
                        {
                          "name": "url",
                          "required": true,
                          "type": "STRING"
                        }
                      ]
                    },
                    {
                      "description": "The object input is array?",
                      "displayName": "Is Array",
                      "name": "isArray",
                      "defaultValue": true,
                      "type": "BOOLEAN"
                    },
                    {
                      "displayName": "Options",
                      "placeholder": "Add Option",
                      "type": "OPTION",
                      "options": [
                        {
                          "description": "The path where the array is e.g 'data'. Leave blank to use the top level object.",
                          "displayOption": {
                            "showWhen": {
                              "isArray": {
                                "values": [
                                  true
                                ]
                              }
                            }
                          },
                          "displayName": "Path",
                          "name": "path",
                          "type": "STRING"
                        },
                        {
                          "description": "The amount of child elements to return in a page.",
                          "displayOption": {
                            "showWhen": {
                              "isArray": {
                                "values": [
                                  true
                                ]
                              }
                            }
                          },
                          "displayName": "Page Size",
                          "name": "pageSize",
                          "type": "INTEGER"
                        },
                        {
                          "description": "The page number to get.",
                          "displayOption": {
                            "showWhen": {
                              "isArray": {
                                "values": [
                                  true
                                ]
                              }
                            }
                          },
                          "displayName": "Page Number",
                          "name": "pageNumber",
                          "type": "INTEGER"
                        }
                      ]
                    }
                  ],
                  "outputs": [
                    {
                      "type": "ARRAY"
                    },
                    {
                      "type": "OBJECT"
                    }
                  ],
                  "displayName": "Read from file"
                },
                {
                  "description": "Writes the data to a JSON file.",
                  "name": "write",
                  "inputs": [
                    {
                      "description": "The file type to choose.",
                      "displayName": "File Type",
                      "name": "fileType",
                      "required": true,
                      "options": [
                        {
                          "name": "JSON",
                          "value": "JSON"
                        },
                        {
                          "name": "JSON Line",
                          "value": "JSONL"
                        }
                      ],
                      "defaultValue": "JSON",
                      "type": "STRING"
                    },
                    {
                      "description": "The data to write to the file.",
                      "displayName": "Source",
                      "name": "source",
                      "required": true,
                      "type": "ANY",
                      "types": [
                        {
                          "type": "ARRAY"
                        },
                        {
                          "type": "OBJECT"
                        }
                      ]
                    },
                    {
                      "description": "File name to set for binary data. By default, \\"file.json\\" will be used.",
                      "displayName": "File Name",
                      "name": "fileName",
                      "defaultValue": "file.json",
                      "type": "STRING"
                    }
                  ],
                  "outputs": [
                    {
                      "type": "OBJECT",
                      "properties": [
                        {
                          "name": "extension",
                          "required": true,
                          "type": "STRING"
                        },
                        {
                          "name": "mimeType",
                          "required": true,
                          "type": "STRING"
                        },
                        {
                          "name": "name",
                          "required": true,
                          "type": "STRING"
                        },
                        {
                          "name": "url",
                          "required": true,
                          "type": "STRING"
                        }
                      ]
                    }
                  ],
                  "displayName": "Write to file"
                }
              ],
              "version": 1
            }
            """,
                objectMapper.writeValueAsString(new JsonFileTaskDescriptorHandler().getTaskDescriptor()),
                true);
    }
}
