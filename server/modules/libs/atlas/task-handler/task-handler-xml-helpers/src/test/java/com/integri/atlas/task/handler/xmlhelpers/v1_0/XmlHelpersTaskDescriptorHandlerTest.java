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

package com.integri.atlas.task.handler.xmlhelpers.v1_0;

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
public class XmlHelpersTaskDescriptorHandlerTest {

    private final ObjectMapper objectMapper = new ObjectMapper() {
        {
            setSerializationInclusion(JsonInclude.Include.NON_NULL);
        }
    };

    @Test
    public void testGetFileTaskDescriptor() throws JsonProcessingException {
        JSONAssert.assertEquals(
            """
            {
              "description": "Converts between XML string and object/array.",
              "displayName": "XML Helpers",
              "name": "xmlHelpers",
              "operations": [
                {
                  "description": "Converts the XML string to object/array.",
                  "name": "parse",
                  "inputs": [
                    {
                      "description": "The XML string to convert to the data.",
                      "displayName": "Source",
                      "name": "source",
                      "required": true,
                      "type": "STRING"
                    }
                  ],
                  "outputs": [
                    {
                      "type": "OBJECT"
                    }
                  ],
                  "displayName": "Convert from XML string"
                },
                {
                  "description": "Writes the object/array to a XML string.",
                  "name": "stringify",
                  "inputs": [
                    {
                      "description": "The data to convert to XML string.",
                      "displayName": "Source",
                      "name": "source",
                      "required": true,
                      "types": [
                        {
                          "type": "ARRAY"
                        },
                        {
                          "type": "OBJECT"
                        }
                      ]
                    }
                  ],
                  "outputs": [
                    {
                      "type": "STRING"
                    }
                  ],
                  "displayName": "Convert to XML string"
                }
              ],
              "version": 1
            }
            """,
            (JSONObject) JSONParser.parseJSON(
                objectMapper.writeValueAsString(new XmlHelpersTaskDescriptorHandler().getTaskDescriptor())
            ),
            true
        );
    }
}
