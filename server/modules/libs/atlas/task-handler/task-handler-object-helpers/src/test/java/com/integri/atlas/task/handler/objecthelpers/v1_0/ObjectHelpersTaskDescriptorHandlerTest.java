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

package com.integri.atlas.task.handler.objecthelpers.v1_0;

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
public class ObjectHelpersTaskDescriptorHandlerTest {

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
              "description": "Converts between JSON string and object/array.",
              "displayName": "Object Helpers",
              "name": "objectHelpers",
              "operations": [
                {
                  "description": "Converts the JSON string to object/array.",
                  "name": "parse",
                  "inputs": [
                    {
                      "description": "The JSON string to convert to the data.",
                      "displayName": "Source",
                      "name": "source",
                      "required": true,
                      "type": "STRING"
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
                  "displayName": "Convert from JSON string"
                },
                {
                  "description": "Writes the object/array to a JSON string.",
                  "name": "stringify",
                  "inputs": [
                    {
                      "description": "The data to convert to JSON string.",
                      "displayName": "Source",
                      "name": "source",
                      "required": true,
                      "type": "OBJECT",
                      "additionalProperties": true
                    }
                  ],
                  "outputs": [
                    {
                      "type": "STRING"
                    }
                  ],
                  "displayName": "Convert to JSON string"
                }
              ],
              "version": 1
            }
            """,
            (JSONObject) JSONParser.parseJSON(
                objectMapper.writeValueAsString(new ObjectHelpersTaskDescriptorHandler().getTaskDescriptor())
            ),
            true
        );
    }
}
