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
public class IfTaskDefinitionTest {

    private final ObjectMapper objectMapper = new ObjectMapper() {
        {
            setSerializationInclusion(JsonInclude.Include.NON_NULL);
        }
    };

    @Test
    public void testIfTaskDescription() throws JsonProcessingException, JSONException {
        JSONAssert.assertEquals(
            """
            {
                "description":"Directs a stream based on true/false results of comparisons",
                "displayName":"IF",
                "name":"if",
                "properties":[
                    {
                        "description":"The type of values to compare.",
                        "displayName":"Conditions",
                        "name":"conditions",
                        "type":"COLLECTION",
                        "typeOption":{
                            "multipleValues":true
                        },
                        "options":[
                            {
                                "displayName":"Boolean",
                                "name":"boolean",
                                "type":"COLLECTION",
                                "fields":[
                                    {
                                        "defaultValue":false,
                                        "description":"The boolean value to compare with the second one.",
                                        "displayName":"Value 1",
                                        "name":"value1",
                                        "type":"BOOLEAN"
                                    },
                                    {
                                        "defaultValue":"equal",
                                        "description":"Compare operation to decide where to map data.",
                                        "displayName":"Operation",
                                        "name":"operation",
                                        "type":"SELECT",
                                        "options":[
                                            {
                                                "name":"Equal",
                                                "value":"equal"
                                            },
                                            {
                                                "name":"Not Equal",
                                                "value":"notEqual"
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
                                "type":"COLLECTION",
                                "fields":[
                                    {
                                        "defaultValue":"",
                                        "description":"The date & time value to compare with the second one.",
                                        "displayName":"Value 1",
                                        "name":"value1",
                                        "type":"DATE_TIME"
                                    },
                                    {
                                        "defaultValue":"after",
                                        "description":"Compare operation to decide where to map data.",
                                        "displayName":"Operation",
                                        "name":"operation",
                                        "type":"SELECT",
                                        "options":[
                                            {
                                                "name":"After",
                                                "value":"after"
                                            },
                                            {
                                                "name":"Before","value":"before"
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
                                "type":"COLLECTION",
                                "fields":[
                                    {
                                        "defaultValue":0,
                                        "description":"The number value to compare with the second one.",
                                        "displayName":"Value 1",
                                        "name":"value1",
                                        "type":"NUMBER"
                                    },
                                    {
                                        "defaultValue":"smaller",
                                        "description":"Compare operation to decide where to map data.",
                                        "displayName":"Operation",
                                        "name":"operation",
                                        "type":"SELECT",
                                        "options":[
                                            {
                                                "name":"Smaller",
                                                "value":"smaller"
                                            },
                                            {
                                                "name":"Smaller or Equal",
                                                "value":"smallerEqual"
                                            },
                                            {
                                                "name":"Equal",
                                                "value":"equal"
                                            },
                                            {
                                                "name":"Not Equal",
                                                "value":"notEqual"
                                            },
                                            {
                                                "name":"Larger",
                                                "value":"larger"
                                            },
                                            {
                                                "name":"Larger or Equal",
                                                "value":"largerEqual"
                                            },
                                            {
                                                "name":"Empty",
                                                "value":"empty"
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
                                "type":"COLLECTION",
                                "fields":[
                                    {
                                        "defaultValue":"",
                                        "description":"The string value to compare with the second one.",
                                        "displayName":"Value 1",
                                        "name":"value1",
                                        "type":"STRING"
                                    },
                                    {
                                        "defaultValue":"equal",
                                        "description":"Compare operation to decide where to map data.",
                                        "displayName":"Operation",
                                        "name":"operation",
                                        "type":"SELECT",
                                        "options":[
                                            {
                                                "name":"Equal",
                                                "value":"equal"
                                            },
                                            {
                                                "name":"Not Equal",
                                                "value":"notEqual"
                                            },
                                            {
                                                "name":"Contains",
                                                "value":"contains"
                                            },
                                            {
                                                "name":"Not Contains",
                                                "value":"notContains"
                                            },
                                            {
                                                "name":"Starts With",
                                                "value":"startsWith"
                                            },
                                            {
                                                "name":"Ends With",
                                                "value":"endsWith"
                                            },
                                            {
                                                "name":"Regex",
                                                "value":"regex"
                                            },
                                            {
                                                "name":"Empty",
                                                "value":"empty"
                                            }
                                        ]
                                    },
                                    {
                                        "defaultValue":"",
                                        "description":"The string value to compare with the first one.",
                                        "displayName":"Value 2",
                                        "displayOption":{
                                            "hide":{
                                                "operation":["empty","regex"]
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
                                                "operation":["regex"]
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
                    },
                    {
                        "description":"List of tasks that will be executed when result of resolving all conditions is TRUE",
                        "displayName":"Case TRUE",
                        "name":"caseTrue",
                        "type":"COLLECTION"
                    },
                    {
                        "description":"List of tasks that will be executed when result of resolving all conditions is FALSE",
                        "displayName":"Case FALSE",
                        "name":"caseFalse",
                        "type":"COLLECTION"
                    }
                ],
                "version":1.0
            }
                """,
            (JSONObject) JSONParser.parseJSON(objectMapper.writeValueAsString(IfTaskDefinition.TASK_SPECIFICATION)),
            true
        );
    }
}
