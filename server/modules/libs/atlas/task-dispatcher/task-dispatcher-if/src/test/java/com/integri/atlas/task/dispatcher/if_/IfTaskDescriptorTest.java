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

package com.integri.atlas.task.dispatcher.if_;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONParser;

/**
 * @author Ivica Cardic
 */
public class IfTaskDescriptorTest {

    private final ObjectMapper objectMapper = new ObjectMapper() {
        {
            setSerializationInclusion(JsonInclude.Include.NON_NULL);
        }
    };

    @Test
    public void testIfTaskDescriptor() throws JsonProcessingException, JSONException {
        JSONAssert.assertEquals(
            """
            {
  "description": "Directs a stream based on true/false results of comparisons",
  "displayName": "If",
  "name": "if",
  "operations": [
    {
      "name": "Branch",
      "inputs": [
        {
          "description": "If the conditions should be set via the key-value pair in UI or as an raw expression).",
          "displayName": "RAW Conditions",
          "name": "rawConditions",
          "defaultValue": false,
          "type": "BOOLEAN"
        },
        {
          "description": "The conditions expressed as an expression.",
          "displayOption": {
            "showWhen": {
              "rawConditions": {
                "values": [
                  true
                ]
              }
            }
          },
          "displayName": "Conditions",
          "name": "conditions",
          "type": "STRING"
        },
        {
          "description": "The type of values to compare.",
          "displayOption": {
            "showWhen": {
              "rawConditions": {
                "values": [
                  false
                ]
              }
            }
          },
          "displayName": "Conditions",
          "name": "conditions",
          "placeholder": "Add Condition",
          "options": [
            {
              "name": "Boolean",
              "value": "boolean"
            },
            {
              "name": "Date & Time",
              "value": "dateTime"
            },
            {
              "name": "Number",
              "value": "number"
            },
            {
              "name": "String",
              "value": "string"
            }
          ],
          "type": "ARRAY",
          "items": [
            {
              "displayName": "Boolean",
              "name": "boolean",
              "type": "OBJECT",
              "properties": [
                {
                  "description": "The boolean value to compare with the second one.",
                  "displayName": "Value 1",
                  "name": "value1",
                  "defaultValue": false,
                  "type": "BOOLEAN"
                },
                {
                  "description": "Compare operation to decide where to map data.",
                  "displayName": "Operation",
                  "name": "operation",
                  "options": [
                    {
                      "name": "Equals",
                      "value": "EQUALS"
                    },
                    {
                      "name": "Not Equals",
                      "value": "NOT_EQUALS"
                    }
                  ],
                  "defaultValue": "EQUALS",
                  "type": "STRING"
                },
                {
                  "description": "The boolean value to compare with the first one.",
                  "displayName": "Value 2",
                  "name": "value2",
                  "defaultValue": false,
                  "type": "BOOLEAN"
                }
              ]
            },
            {
              "displayName": "Date & Time",
              "name": "dateTime",
              "type": "OBJECT",
              "properties": [
                {
                  "description": "The date & time value to compare with the second one.",
                  "displayName": "Value 1",
                  "name": "value1",
                  "type": "DATE_TIME"
                },
                {
                  "description": "Compare operation to decide where to map data.",
                  "displayName": "Operation",
                  "name": "operation",
                  "options": [
                    {
                      "name": "After",
                      "value": "AFTER"
                    },
                    {
                      "name": "Before",
                      "value": "BEFORE"
                    }
                  ],
                  "defaultValue": "AFTER",
                  "type": "STRING"
                },
                {
                  "description": "The date & time value to compare with the first one.",
                  "displayName": "Value 2",
                  "name": "value2",
                  "type": "DATE_TIME"
                }
              ]
            },
            {
              "displayName": "Number",
              "name": "number",
              "type": "OBJECT",
              "properties": [
                {
                  "description": "The number value to compare with the second one.",
                  "displayName": "Value 1",
                  "name": "value1",
                  "defaultValue": 0,
                  "type": "NUMBER"
                },
                {
                  "description": "Compare operation to decide where to map data.",
                  "displayName": "Operation",
                  "name": "operation",
                  "options": [
                    {
                      "name": "Less",
                      "value": "LESS"
                    },
                    {
                      "name": "Less or Equals",
                      "value": "LESS_EQUALS"
                    },
                    {
                      "name": "Equals",
                      "value": "EQUALS"
                    },
                    {
                      "name": "Not Equals",
                      "value": "NOT_EQUALS"
                    },
                    {
                      "name": "Greater",
                      "value": "GREATER"
                    },
                    {
                      "name": "Greater or Equals",
                      "value": "GREATER_EQUALS"
                    },
                    {
                      "name": "Empty",
                      "value": "EMPTY"
                    }
                  ],
                  "defaultValue": "LESS",
                  "type": "STRING"
                },
                {
                  "description": "The number value to compare with the first one.",
                  "displayOption": {
                    "hideWhen": {
                      "operation": {}
                    }
                  },
                  "displayName": "Value 2",
                  "name": "value2",
                  "defaultValue": 0,
                  "type": "NUMBER"
                }
              ]
            },
            {
              "displayName": "String",
              "name": "string",
              "type": "OBJECT",
              "properties": [
                {
                  "description": "The string value to compare with the second one.",
                  "displayName": "Value 1",
                  "name": "value1",
                  "defaultValue": "",
                  "type": "STRING"
                },
                {
                  "description": "Compare operation to decide where to map data.",
                  "displayName": "Operation",
                  "name": "operation",
                  "options": [
                    {
                      "name": "Equals",
                      "value": "EQUALS"
                    },
                    {
                      "name": "Not Equals",
                      "value": "NOT_EQUALS"
                    },
                    {
                      "name": "Contains",
                      "value": "CONTAINS"
                    },
                    {
                      "name": "Not Contains",
                      "value": "NOT_CONTAINS"
                    },
                    {
                      "name": "Starts With",
                      "value": "STARTS_WITH"
                    },
                    {
                      "name": "Ends With",
                      "value": "ENDS_WITH"
                    },
                    {
                      "name": "Regex",
                      "value": "REGEX"
                    },
                    {
                      "name": "Empty",
                      "value": "EMPTY"
                    }
                  ],
                  "defaultValue": "EQUALS",
                  "type": "STRING"
                },
                {
                  "description": "The string value to compare with the first one.",
                  "displayOption": {
                    "hideWhen": {
                      "operation": {
                        "values": [
                          "EMPTY",
                          "REGEX"
                        ]
                      }
                    }
                  },
                  "displayName": "Value 2",
                  "name": "value2",
                  "defaultValue": "",
                  "type": "STRING"
                },
                {
                  "description": "The regex value to compare with the first one.",
                  "displayOption": {
                    "showWhen": {
                      "operation": {
                        "values": [
                          "REGEX"
                        ]
                      }
                    }
                  },
                  "displayName": "Regex",
                  "name": "value2",
                  "placeholder": "/text/i",
                  "defaultValue": "",
                  "type": "STRING"
                }
              ]
            }
          ]
        },
        {
          "description": "If multiple conditions are set, this setting decides if it is true as soon as ANY condition\\n matches or only if ALL are met.\\n",
          "displayOption": {
            "showWhen": {
              "rawConditions": {
                "values": [
                  false
                ]
              }
            }
          },
          "displayName": "Combine",
          "name": "combineOperation",
          "options": [
            {
              "description": "Only if all conditions are met, the workflow goes into \\"true\\" branch.",
              "name": "All",
              "value": "ALL"
            },
            {
              "description": "If any condition is met, the workflow goes into \\"true\\" branch.",
              "name": "Any",
              "value": "ANY"
            }
          ],
          "defaultValue": "ALL",
          "type": "STRING"
        }
      ]
    }
  ],
  "version": 1
}
                """,
            (JSONObject) JSONParser.parseJSON(
                objectMapper.writeValueAsString(new IfTaskDescriptorHandler().getTaskDescriptor())
            ),
            true
        );
    }
}
