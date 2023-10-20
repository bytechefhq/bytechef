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

package com.integri.atlas.task.handler.file;

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
public class FileTaskDefinitionTest {

    private final ObjectMapper objectMapper = new ObjectMapper() {
        {
            setSerializationInclusion(JsonInclude.Include.NON_NULL);
        }
    };

    @Test
    public void testFileTaskDefinition() throws JsonProcessingException {
        JSONAssert.assertEquals(
            """
            {
              "description": "Reads and writes data from a file",
              "displayName": "File",
              "name": "file",
              "operations": [
                {
                  "description": "Reads data from a csv file.",
                  "name": "READ",
                  "inputs": [
                    {
                      "description": "The object property which contains a reference to the file to read from.",
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
                    }
                  ],
                  "outputs": [
                    {
                      "type": "STRING"
                    }
                  ],
                  "displayName": "Read from file"
                },
                {
                  "description": "Writes the data to a csv file.",
                  "name": "WRITE",
                  "inputs": [
                    {
                      "description": "String to write to the file.",
                      "displayName": "Content",
                      "name": "content",
                      "required": true,
                      "type": "STRING"
                    },
                    {
                      "description": "File name to set for binary data. By default, \\"file.txt\\" will be used.",
                      "displayName": "File Name",
                      "name": "fileName",
                      "defaultValue": "file.txt",
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
            (JSONObject) JSONParser.parseJSON(
                objectMapper.writeValueAsString(new FileTaskDefinitionHandler().getTaskDefinition())
            ),
            true
        );
    }
}
