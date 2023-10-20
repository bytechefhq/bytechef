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
    public void testHttpClientTaskSpecification() throws JsonProcessingException {
        JSONAssert.assertEquals(
            """
            {
                "authentication":{
                    "credentials":[
                        {
                            "name":"httpBasicAuth",
                            "displayOption":{
                                "show":{
                                    "authenticationType":["HTTP_BASIC_AUTH"]
                                }
                            }
                        },
                        {
                            name: 'httpHeaderAuth',
                            displayOption: {
                                show: {
                                    authenticationType: [
                                        "HTTP_HEADER_AUTH",
                                    ],
                                },
                            },
                        },
                        {
                            name: 'httpQueryAuth',
                            displayOption: {
                                show: {
                                    authenticationType: [
                                        "HTTP_QUERY_AUTH",
                                    ],
                                },
                            },
                        },
                        {
                            "name":"httpDigestAuth",
                            "displayOption":{
                                "show":{
                                    "authenticationType":["HTTP_DIGEST_AUTH"]
                                }
                            }
                        },
                        {
                            "name":"oAuth2Auth",
                            "displayOption":{
                                "show":{
                                    "authenticationType":["OAUTH2"]
                                }
                            }
                        }
                    ],
                    properties:[
                         {
                            "displayName":"Authentication Type",
                            "name":"authenticationType",
                            "type":"SELECT",
                            "options":[
                                {
                                    "value":"HTTP_BASIC_AUTH",
                                    "name":"Basic Auth"
                                },
                                {
                                    "value":"HTTP_DIGEST_AUTH",
                                    "name":"Digest Auth"
                                },
                                {
                                    "value":"HTTP_HEADER_AUTH",
                                    "name":"Header Auth"
                                },
                                 {
                                    "value":"HTTP_QUERY_AUTH",
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
                "description":"Makes an HTTP request and returns the response data.",
                "displayName":"HTTP Client",
                "name":"httpClient",
                "properties":[
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
                        "description":"The URI to make the request to",
                        "displayName":"URI",
                        "name":"uri",
                        "required":true,
                        "type":"STRING",
                        "placeholder":"https://example.com/index.html"
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
                                "name":"JSON",
                                "value":"JSON",
                                "description": "The response is automatically converted to object/array."
                            },
                             {
                                "name":"XML",
                                "value":"XML",
                                "description": "The response is automatically converted to object/array."
                            },
                            {
                                "name":"Text",
                                "value":"TEXT",
                                 "description": "The response is returned as a text."
                            },
                             {
                                "name":"File",
                                "value":"FILE",
                                "description": "The response is returned as a file object."
                            },
                        ]
                    },
                    {
                        "defaultValue":"",
                        "description":"The name of the file if the response is returned as a file object.",
                        "displayName":"Response File Name",
                        "displayOption":{
                            "show":{
                                "responseFormat":["FILE"]
                            }
                        },
                        "name":"responseFileName",
                        "type":"STRING",
                    },
                    {
                        "defaultValue":false,
                        "description":"If the header, query and/or body parameters should be set via the key-value pair in UI or as an object/JSON string based).",
                        "displayName":"RAW Parameters",
                        "name":"rawParameters",
                        "type":"BOOLEAN"
                    },
                    {
                        "displayName":"Options",
                        "name":"options",
                        "type":"COLLECTION",
                        "options":[
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
                                        "name":"Raw",
                                        "value":"RAW"
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
                                "description":"Mime-Type to use when sending raw body content.",
                                "displayName":"Mime Type",
                                "displayOption":{
                                    "show":{
                                        "bodyContentType": ["RAW"],
                                        "requestMethod":["PATCH","POST","PUT"]
                                    }
                                },
                                "name":"mimeType",
                                placeholder: "text/xml",
                                "type":"STRING",
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
                                "placeholder":"https://myproxy:3128"
                            },
                            {
                                "defaultValue":1000,
                                "description":"Time in ms to wait for the server to send a response before aborting the request.",
                                "displayName":"Timeout",
                                "name":"timeout",
                                "type":"INTEGER",
                                "typeOption":{
                                    "minValue":1
                                }
                            }
                        ],
                        "placeholder":"Add Option"
                    },
                    {
                        "description":"Header parameters to send as an object/JSON string.",
                        "displayName":"Header Parameters",
                        "displayOption":{
                            "show":{
                                "rawParameters":[true]
                            }
                        },
                        "name":"headerParameters",
                        "type":"JSON"
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
                        "name":"headerParameters",
                        "type":"COLLECTION",
                        "typeOption":{
                            "multipleValues":true
                        },
                        "options":[
                            {
                                "displayName":"Parameter",
                                "name":"parameter",
                                "type":"GROUP",
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
                        "description":"Query parameters to send as an object/JSON string.",
                        "displayName":"Query Parameters",
                        "displayOption":{
                            "show":{
                                "rawParameters":[true]
                            }
                        },
                        "name":"queryParameters",
                        "type":"JSON"
                    },
                    {
                        "defaultValue":"",
                        "description":"Query parameters to send.",
                        "displayName":"Query Parameters",
                        "displayOption":{
                            "show":{
                                "rawParameters":[false]
                            }
                        },
                        "name":"queryParameters",
                        "type":"COLLECTION",
                        "typeOption":{
                            "multipleValues":true
                        },
                        "options":[
                            {
                                "displayName":"Parameter",
                                "name":"parameter",
                                "type":"GROUP",
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
                        "description":"Body parameters to send as an object/JSON string.",
                        "displayName":"Body Parameters",
                        "displayOption":{
                            "show":{
                                "rawParameters":[true],
                                 "bodyContentType":["JSON", "FORM_DATA", "FORM_URLENCODED", "RAW"],
                                "requestMethod":["PATCH","POST","PUT"]
                            }
                        },
                        "name":"bodyParameters",
                        "type":"JSON"
                    },
                    {
                        "defaultValue":"",
                        "description":"Body parameters to send.",
                        "displayName":"Body Parameters",
                        "displayOption":{
                            "show":{
                                "rawParameters":[false],
                                "bodyContentType":["JSON", "FORM_DATA", "FORM_URLENCODED", "RAW"],
                                "requestMethod":["PATCH","POST","PUT"]
                            }
                        },
                        "name":"bodyParameters",
                        "type":"COLLECTION",
                        "typeOption":{
                            "multipleValues":true
                        },
                        "options":[
                            {
                                "displayName":"Parameter",
                                "name":"parameter",
                                "type":"GROUP",
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
