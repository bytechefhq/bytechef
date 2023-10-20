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

package com.integri.atlas.task.handler.http.client;

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
public class HttpClientTaskDefinitionTest {

    private final ObjectMapper objectMapper = new ObjectMapper() {
        {
            setSerializationInclusion(JsonInclude.Include.NON_NULL);
        }
    };

    @Test
    public void testHttpClientTaskDescription() throws JsonProcessingException {
        JSONAssert.assertEquals(
            """
            {
                "authentication":{
                    "credentials":[
                        {
                            "name":"httpBasicAuth",
                            "required":true,
                            "displayOption":{
                                "show":{
                                    "authenticationType":["BASIC_AUTH"]
                                }
                            }
                        },
                        {
                            "name":"httpDigestAuth",
                            "required":true,
                            "displayOption":{
                                "show":{
                                    "authenticationType":["DIGEST_AUTH"]
                                }
                            }
                        },
                        {
                            name: 'httpHeaderAuth',
                            required: true,
                            displayOption: {
                                show: {
                                    authenticationType: [
                                        "HEADER_AUTH",
                                    ],
                                },
                            },
                        },
                        {
                            "name":"oAuth2Auth",
                            "required":true,
                            "displayOption":{
                                "show":{
                                    "authenticationType":["OAUTH2"]
                                }
                            }
                        }
                    ],
                    "properties":[
                        {
                            "displayName":"Authentication Type",
                            "name":"authenticationType",
                            "type":"SELECT",
                            "options":[
                                {
                                    "value":"BASIC_AUTH",
                                    "name":"Basic Auth"
                                },
                                {
                                    "value":"DIGEST_AUTH",
                                    "name":"Digest Auth"
                                },
                                {
                                    "value":"HEADER_AUTH",
                                    "name":"Header Auth"
                                },
                                 {
                                    "value":"QUERY_AUTH",
                                    "name":"Query Auth"
                                },
                                {
                                    "value":"OAUTH2",
                                    "name":"OAuth2"
                                },
                                {
                                    "value":"",
                                    "name":"None"
                                }
                            ]
                        }
                    ]
                },
                "description":"Makes an HTTP request and returns the response data",
                "displayName":"HTTP Client",
                "name":"httpClient"
                ,"properties":[
                    {
                        "defaultValue":"GET",
                        "description":"The request method to use.",
                        "displayName":"Request Method",
                        "name":"requestMethod",
                        "type":"SELECT",
                        "options":[
                            {
                                "name":"DELETE",
                                "value":"DELETE"
                            },
                            {
                                "name":"GET",
                                "value":"GET"
                            },
                            {
                                "name":"HEAD",
                                "value":"HEAD"
                            },
                            {
                                "name":"PATCH",
                                "value":"PATCH"
                            },
                            {
                                "name":"POST",
                                "value":"POST"
                            },
                            {
                                "name":"PUT",
                                "value":"PUT"
                            }
                        ]
                    },
                    {
                        "defaultValue":"",
                        "description":"The URL to make the request to",
                        "displayName":"URL",
                        "name":"url",
                        "required":true,
                        "type":"STRING",
                        "placeholder":"http://example.com/index.html"
                    },
                    {
                        "defaultValue":false,
                        "description":"Download the response even if SSL certificate validation is not possible.",
                        "displayName":"Allow Unauthorized Certs",
                        "name":"allowUnauthorizedCerts",
                        "type":"BOOLEAN"
                    },
                    {
                        "defaultValue":"JSON",
                        "description":"The format in which the data gets returned from the URL.",
                        "displayName":"Response Format",
                        "name":"responseFormat",
                        "type":"SELECT",
                        "options":[
                            {
                                "name":"Binary",
                                "value":"BINARY"
                            },
                            {
                                "name":"JSON",
                                "value":"JSON"
                            },
                            {
                                "name":"String",
                                "value":"STRING"
                            }
                        ]
                    },
                    {
                        "displayName":"Options",
                        "name":"options",
                        "type":"COLLECTION",
                        "options":[
                            {
                                "defaultValue":false,
                                "description":"If the query and/or body parameters should be set via the key-value pair UI or RAW.",
                                "displayName":"RAW Parameters",
                                "name":"rawParameters",
                                "type":"BOOLEAN"
                            },
                            {
                                "defaultValue":"JSON",
                                "description":"Content-Type to use when sending body parameters.",
                                "displayName":"Body Content Type",
                                "displayOption":{
                                    "show":{
                                        "requestMethod":["PATCH","POST","PUT"]
                                    }
                                },
                                "name":"bodyContentType",
                                "type":"SELECT",
                                "options":[
                                    {
                                        "name":"JSON",
                                        "value":"JSON"
                                    },
                                    {
                                        "name":"Form-Data",
                                        "value":"FORM_DATA"
                                    },
                                    {
                                        "name":"Form-Urlencoded",
                                        "value":"FORM_URLENCODED"
                                    },
                                    {
                                        "name":"Binary",
                                        "value":"BINARY"
                                    }
                                ]
                            },
                            {
                                "defaultValue":false,
                                "description":"Returns the full response data instead of only the body.",
                                "displayName":"Full Response",
                                "name":"fullResponse",
                                "type":"BOOLEAN"
                            },
                            {
                                "defaultValue":false,
                                "description":"Follow non-GET HTTP 3xx redirects.",
                                "displayName":"Follow All Redirects",
                                "name":"followAllRedirects",
                                "type":"BOOLEAN"
                            },
                            {
                                "defaultValue":false,
                                "description":"Follow GET HTTP 3xx redirects.",
                                "displayName":"Follow GET Redirect",
                                "name":"followRedirect",
                                "type":"BOOLEAN"
                            },
                            {
                                "defaultValue":false,
                                "description":"Succeeds also when the status code is not 2xx.",
                                "displayName":"Ignore Response Code",
                                "name":"ignoreResponseCode",
                                "type":"BOOLEAN"
                            },
                            {
                                "defaultValue":"",
                                "description":"HTTP proxy to use.",
                                "displayName":"Proxy",
                                "name":"proxy",
                                "type":"STRING",
                                "placeholder":"http://myproxy:3128"
                            },
                            {
                                "defaultValue":1000,
                                "description":"Time in ms to wait for the server to send a response before aborting the request.",
                                "displayName":"Timeout",
                                "name":"timeout",
                                "type":"NUMBER",
                                "typeOption":{
                                    "minValue":1.0,
                                    "multipleValues":false
                                }
                            }
                        ],
                        "placeholder":"Add Option"
                    },
                    {
                        "defaultValue":"",
                        "description":"Header parameters to send as RAW.",
                        "displayName":"Header Parameters",
                        "displayOption":{
                            "hide":{
                                "rawParameters":[true]
                            }
                        },
                        "name":"headerParametersRaw",
                        "type":"STRING"
                    },
                    {
                        "defaultValue":"",
                        "description":"Header parameters to send.",
                        "displayName":"Header Parameters",
                        "displayOption":{
                            "show":{
                                "rawParameters":[false]
                            }
                        },
                        "name":"headerParametersKeyValue",
                        "type":"COLLECTION",
                        "typeOption":{
                            "multipleValues":true
                        },
                        "options":[
                            {
                                "displayName":"Parameter",
                                "name":"parameter",
                                "type":"COLLECTION",
                                "fields":[
                                    {
                                        "defaultValue":"",
                                        "description":"Name of the parameter.",
                                        "displayName":"Name",
                                        "name":"name",
                                        "type":"STRING"
                                    },
                                    {
                                        "defaultValue":"",
                                        "description":"Value of the parameter.",
                                        "displayName":"Value",
                                        "name":"value","type":"STRING"
                                    }
                                ]
                            }
                        ],
                        "placeholder":"Add Parameter"
                    },
                    {
                        "defaultValue":"",
                        "description":"Query parameters as RAW.",
                        "displayName":"Query Parameters",
                        "displayOption":{
                            "hide":{
                                "rawParameters":[true]
                            }
                        },
                        "name":"queryParametersRaw",
                        "type":"STRING"
                    },
                    {
                        "defaultValue":"",
                        "description":"Query parameters to send.",
                        "displayName":"Header Parameters",
                        "displayOption":{
                            "show":{
                                "rawParameters":[false]
                            }
                        },
                        "name":"queryParametersKeyValue",
                        "type":"COLLECTION",
                        "typeOption":{
                            "multipleValues":true
                        },
                        "options":[
                            {
                                "displayName":"Parameter",
                                "name":"parameter",
                                "type":"COLLECTION",
                                "fields":[
                                    {
                                        "defaultValue":"",
                                        "description":"Name of the parameter.",
                                        "displayName":"Name",
                                        "name":"name",
                                        "type":"STRING"
                                    },
                                    {
                                        "defaultValue":"",
                                        "description":"Value of the parameter.",
                                        "displayName":"Value",
                                        "name":"value",
                                        "type":"STRING"
                                    }
                                ]
                            }
                        ],
                        "placeholder":"Add Parameter"
                    },
                    {
                        "defaultValue":"",
                        "description":"Body parameters as RAW.",
                        "displayName":"Body Parameters",
                        "displayOption":{
                            "show":{
                                "rawParameters":[true],
                                "bodyContentType":["JSON","FORM_DATA","FORM_URLENCODED"],
                                "requestMethod":["PATCH","POST","PUT"]
                            }
                        },
                        "name":"bodyParametersRaw",
                        "type":"STRING"
                    },
                    {
                        "defaultValue":"",
                        "description":"Body parameters to send.",
                        "displayName":"Body Parameters",
                        "displayOption":{
                            "show":{
                                "rawParameters":[false],
                                "bodyContentType":["JSON","FORM_DATA","FORM_URLENCODED"],
                                "requestMethod":["PATCH","POST","PUT"]
                            }
                        },
                        "name":"bodyParametersKeyValue",
                        "type":"COLLECTION",
                        "typeOption":{
                            "multipleValues":true
                        },
                        "options":[
                            {
                                "displayName":"Parameter",
                                "name":"parameter",
                                "type":"COLLECTION",
                                "fields":[
                                    {
                                        "defaultValue":"",
                                        "description":"Name of the parameter.",
                                        "displayName":"Name",
                                        "name":"name",
                                        "type":"STRING"
                                    },
                                    {
                                        "defaultValue":"",
                                        "description":"Value of the parameter.",
                                        "displayName":"Value",
                                        "name":"value",
                                        "type":"STRING"
                                    }
                                ]
                            }
                        ],
                        "placeholder":"Add Parameter"
                    },
                    {
                        "description":"The object property which contains a reference to the file with data to upload.",
                        "displayName":"File",
                        "displayOption":{
                            "show":{
                                "bodyContentType":["BINARY"],
                                "requestMethod":["PATCH","POST","PUT"]
                            }
                        },
                        "name":"fileEntry",
                        "type":"JSON"
                    }
                ],
                "version":1.0
            }
                """,
            (JSONObject) JSONParser.parseJSON(
                objectMapper.writeValueAsString(HttpClientTaskDefinition.TASK_SPECIFICATION)
            ),
            true
        );
    }
}
