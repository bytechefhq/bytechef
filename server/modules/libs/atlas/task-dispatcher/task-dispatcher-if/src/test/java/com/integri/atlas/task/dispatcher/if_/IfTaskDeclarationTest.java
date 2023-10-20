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
public class IfTaskDeclarationTest {

    private final ObjectMapper objectMapper = new ObjectMapper() {
        {
            setSerializationInclusion(JsonInclude.Include.NON_NULL);
        }
    };

    @Test
    public void testIfTaskSpecification() throws JsonProcessingException, JSONException {
        JSONAssert.assertEquals(
            """
            {
                "description":"Directs a stream based on true/false results of comparisons",
                "displayName":"If",
                "name":"if",
                "properties":[
                     {
                        "defaultValue":false,
                        "description":"If the conditions should be set via the key-value pair in UI or as an raw expression).",
                        "displayName":"RAW Conditions",
                        "name":"rawConditions",
                        "type":"BOOLEAN"
                    },
                    {
                        "description":"The conditions expressed as an expression.",
                        "displayName":"Conditions",
                        "displayOption":{
                            "show":{
                                "rawConditions":[true]
                            }
                        },
                        "name":"conditions",
                        "type":"STRING"
                    },
                    {
                        "description":"The type of values to compare.",
                        "displayName":"Conditions",
                        "displayOption":{
                            "show":{
                                "rawConditions":[false]
                            }
                        },
                        "name":"conditions",
                        "type":"COLLECTION",
                        "typeOption":{
                            "multipleValues":true
                        },
                        "options":[
                            {
                                "displayName":"Boolean",
                                "name":"boolean",
                                "type":"GROUP",
                                "groupProperties":[
                                    {
                                        "defaultValue":false,
                                        "description":"The boolean value to compare with the second one.",
                                        "displayName":"Value 1",
                                        "name":"value1",
                                        "type":"BOOLEAN"
                                    },
                                    {
                                        "defaultValue":"EQUALS",
                                        "description":"Compare operation to decide where to map data.",
                                        "displayName":"Operation",
                                        "name":"operation",
                                        "type":"SELECT",
                                        "options":[
                                            {
                                                "name":"Equals",
                                                "value":"EQUALS"
                                            },
                                            {
                                                "name":"Not Equals",
                                                "value":"NOT_EQUALS"
                                            }
                                        ]
                                    },
                                    {
                                        "defaultValue":false,
                                        "description":"The boolean value to compare with the first one.",
                                        "displayName":"Value 2",
                                        "name":"value2",
                                        "type":"BOOLEAN"
                                    }
                                ]
                            },
                            {
                                "displayName":"Date & Time",
                                "name":"dateTime",
                                "type":"GROUP",
                                "groupProperties":[
                                    {
                                        "defaultValue":"",
                                        "description":"The date & time value to compare with the second one.",
                                        "displayName":"Value 1",
                                        "name":"value1",
                                        "type":"DATE_TIME"
                                    },
                                    {
                                        "defaultValue":"AFTER",
                                        "description":"Compare operation to decide where to map data.",
                                        "displayName":"Operation",
                                        "name":"operation",
                                        "type":"SELECT",
                                        "options":[
                                            {
                                                "name":"After",
                                                "value":"AFTER"
                                            },
                                            {
                                                "name":"Before","value":"BEFORE"
                                            }
                                        ]
                                    },
                                    {
                                        "defaultValue":"",
                                        "description":"The date & time value to compare with the first one.",
                                        "displayName":"Value 2",
                                        "name":"value2",
                                        "type":"DATE_TIME"
                                    }
                                ]
                            },
                            {
                                "displayName":"Number",
                                "name":"number",
                                "type":"GROUP",
                                "groupProperties":[
                                    {
                                        "defaultValue":0,
                                        "description":"The number value to compare with the second one.",
                                        "displayName":"Value 1",
                                        "name":"value1",
                                        "type":"NUMBER"
                                    },
                                    {
                                        "defaultValue":"LESS",
                                        "description":"Compare operation to decide where to map data.",
                                        "displayName":"Operation",
                                        "name":"operation",
                                        "type":"SELECT",
                                        "options":[
                                            {
                                                "name":"Less",
                                                "value":"LESS"
                                            },
                                            {
                                                "name":"Less or Equals",
                                                "value":"LESS_EQUALS"
                                            },
                                            {
                                                "name":"Equals",
                                                "value":"EQUALS"
                                            },
                                            {
                                                "name":"Not Equals",
                                                "value":"NOT_EQUALS"
                                            },
                                            {
                                                "name":"Greater",
                                                "value":"GREATER"
                                            },
                                            {
                                                "name":"Greater or Equals",
                                                "value":"GREATER_EQUALS"
                                            },
                                            {
                                                "name":"Empty",
                                                "value":"EMPTY"
                                            }
                                        ]
                                    },
                                    {
                                        "defaultValue":0,
                                        "description":"The number value to compare with the first one.",
                                        "displayName":"Value 2",
                                        "displayOption":{
                                            "hide":{
                                                "operation":[]
                                            }
                                        },
                                        "name":"value2",
                                        "type":"NUMBER"
                                    }
                                ]
                            },
                            {
                                "displayName":"String",
                                "name":"string",
                                "type":"GROUP",
                                "groupProperties":[
                                    {
                                        "defaultValue":"",
                                        "description":"The string value to compare with the second one.",
                                        "displayName":"Value 1",
                                        "name":"value1",
                                        "type":"STRING"
                                    },
                                    {
                                        "defaultValue":"EQUALS",
                                        "description":"Compare operation to decide where to map data.",
                                        "displayName":"Operation",
                                        "name":"operation",
                                        "type":"SELECT",
                                        "options":[
                                            {
                                                "name":"Equals",
                                                "value":"EQUALS"
                                            },
                                            {
                                                "name":"Not Equals",
                                                "value":"NOT_EQUALS"
                                            },
                                            {
                                                "name":"Contains",
                                                "value":"CONTAINS"
                                            },
                                            {
                                                "name":"Not Contains",
                                                "value":"NOT_CONTAINS"
                                            },
                                            {
                                                "name":"Starts With",
                                                "value":"STARTS_WITH"
                                            },
                                            {
                                                "name":"Ends With",
                                                "value":"ENDS_WITH"
                                            },
                                            {
                                                "name":"Regex",
                                                "value":"REGEX"
                                            },
                                            {
                                                "name":"Empty",
                                                "value":"EMPTY"
                                            }
                                        ]
                                    },
                                    {
                                        "defaultValue":"",
                                        "description":"The string value to compare with the first one.",
                                        "displayName":"Value 2",
                                        "displayOption":{
                                            "hide":{
                                                "operation":["EMPTY","REGEX"]
                                            }
                                        },
                                        "name":"value2",
                                        "type":"STRING"
                                    },
                                    {
                                        "defaultValue":"",
                                        "description":"The regex value to compare with the first one.",
                                        "displayName":"Regex",
                                        "displayOption":{
                                            "show":{
                                                "operation":["REGEX"]
                                            }
                                        },
                                        "name":"value2",
                                        "type":"STRING",
                                        "placeholder":"/text/i"
                                    }
                                ]
                            }
                        ],
                        "placeholder":"Add Condition"
                    },
                    {
                        "defaultValue":"ALL",
                        "description":"If multiple conditions are set, this setting decides if it is true as soon as ANY condition\\n matches or only if ALL are met.\\n",
                        "displayName":"Combine",
                        "displayOption":{
                            "show":{
                                "rawConditions":[false]
                            }
                        },
                        "name":"combineOperation",
                        "type":"SELECT",
                        "options":[
                            {
                                "name":"All",
                                "value":"ALL",
                                "description":"Only if all conditions are met, the workflow goes into \\"true\\" branch."
                            },
                            {
                                "name":"Any",
                                "value":"ANY",
                                "description":"If any condition is met, the workflow goes into \\"true\\" branch."
                            }
                        ]
                    }
                ],
                "version":1.0
            }
                """,
            (JSONObject) JSONParser.parseJSON(objectMapper.writeValueAsString(IfTaskDeclaration.TASK_SPECIFICATION)),
            true
        );
    }
}
