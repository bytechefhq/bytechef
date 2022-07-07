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

package com.bytechef.task.handler.httpclient.v1_0;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

/**
 * @author Ivica Cardic
 */
public class HttpClientTaskDescriptorHandlerTest {

    private final ObjectMapper objectMapper = new ObjectMapper() {
        {
            setSerializationInclusion(JsonInclude.Include.NON_NULL);
        }
    };

    @Test
    public void testGetHTTPClientTaskDescriptor() throws JsonProcessingException {
        JSONAssert.assertEquals(
                """
                {
                  "auth": {
                    "options": [
                      {
                        "value": "api_key"
                      },
                      {
                        "value": "bearer_token"
                      },
                      {
                        "value": "basic_auth"
                      },
                      {
                        "value": "digest_auth"
                      },
                      {
                        "value": "oauth2"
                      }
                    ]
                  },
                  "description": "Makes an HTTP request and returns the response data.",
                  "displayName": "HTTP Client",
                  "name": "httpClient",
                  "operations": [
                    {
                      "description": "The request method to use.",
                      "name": "get",
                      "inputs": [
                        {
                          "description": "The URI to make the request to",
                          "displayName": "URI",
                          "name": "uri",
                          "placeholder": "https://example.com/index.html",
                          "type": "STRING",
                          "required": true,
                          "defaultValue": ""
                        },
                        {
                          "description": "Download the response even if SSL certificate validation is not possible.",
                          "displayName": "Allow Unauthorized Certs",
                          "name": "allowUnauthorizedCerts",
                          "type": "BOOLEAN",
                          "defaultValue": false
                        },
                        {
                          "description": "The format in which the data gets returned from the URL.",
                          "displayName": "Response Format",
                          "name": "responseFormat",
                          "type": "STRING",
                          "options": [
                            {
                              "description": "The response is automatically converted to object/array.",
                              "name": "JSON",
                              "value": "JSON"
                            },
                            {
                              "description": "The response is automatically converted to object/array.",
                              "name": "XML",
                              "value": "XML"
                            },
                            {
                              "description": "The response is returned as a text.",
                              "name": "Text",
                              "value": "TEXT"
                            },
                            {
                              "description": "The response is returned as a file object.",
                              "name": "File",
                              "value": "FILE"
                            }
                          ],
                          "defaultValue": "JSON"
                        },
                        {
                          "description": "The name of the file if the response is returned as a file object.",
                          "displayOption": {
                            "showWhen": {
                              "responseFormat": {
                                "values": [
                                  "FILE"
                                ]
                              }
                            }
                          },
                          "displayName": "Response File Name",
                          "name": "responseFileName",
                          "type": "STRING"
                        },
                        {
                          "description": "Header parameters to send.",
                          "displayName": "Header Parameters",
                          "name": "headerParameters",
                          "placeholder": "Add Parameter",
                          "type": "ARRAY",
                          "defaultValue": [
                            ""
                          ],
                          "items": [
                            {
                              "displayName": "Parameter",
                              "name": "parameter",
                              "type": "OBJECT",
                              "properties": [
                                {
                                  "description": "The key of the parameter.",
                                  "displayName": "Key",
                                  "name": "key",
                                  "type": "STRING",
                                  "defaultValue": ""
                                },
                                {
                                  "description": "The value of the parameter.",
                                  "displayName": "Value",
                                  "name": "value",
                                  "type": "STRING",
                                  "defaultValue": ""
                                }
                              ]
                            }
                          ]
                        },
                        {
                          "description": "Query parameters to send.",
                          "displayName": "Query Parameters",
                          "name": "queryParameters",
                          "placeholder": "Add Parameter",
                          "type": "ARRAY",
                          "defaultValue": [
                            ""
                          ],
                          "items": [
                            {
                              "displayName": "Parameter",
                              "name": "parameter",
                              "type": "OBJECT",
                              "properties": [
                                {
                                  "description": "The key of the parameter.",
                                  "displayName": "Key",
                                  "name": "key",
                                  "type": "STRING",
                                  "defaultValue": ""
                                },
                                {
                                  "description": "The value of the parameter.",
                                  "displayName": "Value",
                                  "name": "value",
                                  "type": "STRING",
                                  "defaultValue": ""
                                }
                              ]
                            }
                          ]
                        }
                      ],
                      "outputs": [
                        {
                          "displayOption": {
                            "showWhen": {
                              "responseFormat": {
                                "values": [
                                  "JSON",
                                  "XML"
                                ]
                              }
                            }
                          },
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
                          "displayOption": {
                            "showWhen": {
                              "responseFormat": {
                                "values": [
                                  "TEXT"
                                ]
                              }
                            }
                          },
                          "type": "STRING"
                        },
                        {
                          "displayOption": {
                            "showWhen": {
                              "responseFormat": {
                                "values": [
                                  "FILE"
                                ]
                              }
                            }
                          },
                          "type": "OBJECT",
                          "properties": [
                            {
                              "name": "extension",
                              "type": "STRING",
                              "required": true
                            },
                            {
                              "name": "mimeType",
                              "type": "STRING",
                              "required": true
                            },
                            {
                              "name": "name",
                              "type": "STRING",
                              "required": true
                            },
                            {
                              "name": "url",
                              "type": "STRING",
                              "required": true
                            }
                          ]
                        }
                      ],
                      "displayName": "GET"
                    },
                    {
                      "description": "The request method to use.",
                      "name": "post",
                      "inputs": [
                        {
                          "description": "The URI to make the request to",
                          "displayName": "URI",
                          "name": "uri",
                          "placeholder": "https://example.com/index.html",
                          "type": "STRING",
                          "required": true,
                          "defaultValue": ""
                        },
                        {
                          "description": "Download the response even if SSL certificate validation is not possible.",
                          "displayName": "Allow Unauthorized Certs",
                          "name": "allowUnauthorizedCerts",
                          "type": "BOOLEAN",
                          "defaultValue": false
                        },
                        {
                          "description": "The format in which the data gets returned from the URL.",
                          "displayName": "Response Format",
                          "name": "responseFormat",
                          "type": "STRING",
                          "options": [
                            {
                              "description": "The response is automatically converted to object/array.",
                              "name": "JSON",
                              "value": "JSON"
                            },
                            {
                              "description": "The response is automatically converted to object/array.",
                              "name": "XML",
                              "value": "XML"
                            },
                            {
                              "description": "The response is returned as a text.",
                              "name": "Text",
                              "value": "TEXT"
                            },
                            {
                              "description": "The response is returned as a file object.",
                              "name": "File",
                              "value": "FILE"
                            }
                          ],
                          "defaultValue": "JSON"
                        },
                        {
                          "description": "The name of the file if the response is returned as a file object.",
                          "displayOption": {
                            "showWhen": {
                              "responseFormat": {
                                "values": [
                                  "FILE"
                                ]
                              }
                            }
                          },
                          "displayName": "Response File Name",
                          "name": "responseFileName",
                          "type": "STRING"
                        },
                        {
                          "description": "Header parameters to send.",
                          "displayName": "Header Parameters",
                          "name": "headerParameters",
                          "placeholder": "Add Parameter",
                          "type": "ARRAY",
                          "defaultValue": [
                            ""
                          ],
                          "items": [
                            {
                              "displayName": "Parameter",
                              "name": "parameter",
                              "type": "OBJECT",
                              "properties": [
                                {
                                  "description": "The key of the parameter.",
                                  "displayName": "Key",
                                  "name": "key",
                                  "type": "STRING",
                                  "defaultValue": ""
                                },
                                {
                                  "description": "The value of the parameter.",
                                  "displayName": "Value",
                                  "name": "value",
                                  "type": "STRING",
                                  "defaultValue": ""
                                }
                              ]
                            }
                          ]
                        },
                        {
                          "description": "Query parameters to send.",
                          "displayName": "Query Parameters",
                          "name": "queryParameters",
                          "placeholder": "Add Parameter",
                          "type": "ARRAY",
                          "defaultValue": [
                            ""
                          ],
                          "items": [
                            {
                              "displayName": "Parameter",
                              "name": "parameter",
                              "type": "OBJECT",
                              "properties": [
                                {
                                  "description": "The key of the parameter.",
                                  "displayName": "Key",
                                  "name": "key",
                                  "type": "STRING",
                                  "defaultValue": ""
                                },
                                {
                                  "description": "The value of the parameter.",
                                  "displayName": "Value",
                                  "name": "value",
                                  "type": "STRING",
                                  "defaultValue": ""
                                }
                              ]
                            }
                          ]
                        },
                        {
                          "description": "Send file instead of body parameters.",
                          "displayOption": {
                            "showWhen": {
                              "bodyContentType": {
                                "values": [
                                  "JSON",
                                  "RAW",
                                  "XML"
                                ]
                              }
                            }
                          },
                          "displayName": "Send File",
                          "name": "sendFile",
                          "type": "BOOLEAN",
                          "defaultValue": false
                        },
                        {
                          "description": "Body parameters to send.",
                          "displayOption": {
                            "showWhen": {
                              "bodyContentType": {
                                "values": [
                                  "JSON"
                                ]
                              }
                            }
                          },
                          "displayName": "Body Parameters",
                          "name": "bodyParameters",
                          "placeholder": "Add Parameter",
                          "type": "OBJECT",
                          "additionalProperties": true
                        },
                        {
                          "description": "Body parameters to send.",
                          "displayOption": {
                            "showWhen": {
                              "bodyContentType": {
                                "values": [
                                  "FORM_DATA"
                                ]
                              }
                            }
                          },
                          "displayName": "Body Parameters",
                          "name": "bodyParameters",
                          "placeholder": "Add Parameter",
                          "type": "ARRAY",
                          "defaultValue": [
                            ""
                          ],
                          "items": [
                            {
                              "displayName": "Parameter",
                              "name": "parameter",
                              "type": "OBJECT",
                              "properties": [
                                {
                                  "description": "The key of the parameter.",
                                  "displayName": "Key",
                                  "name": "key",
                                  "type": "STRING",
                                  "defaultValue": ""
                                },
                                {
                                  "description": "The value of the parameter.",
                                  "displayName": "Value",
                                  "name": "value",
                                  "type": "STRING",
                                  "defaultValue": ""
                                }
                              ]
                            }
                          ]
                        },
                        {
                          "description": "Body parameters to send.",
                          "displayOption": {
                            "showWhen": {
                              "bodyContentType": {
                                "values": [
                                  "FORM_URLENCODED"
                                ]
                              }
                            }
                          },
                          "displayName": "Body Parameters",
                          "name": "bodyParameters",
                          "placeholder": "Add Parameter",
                          "type": "ARRAY",
                          "defaultValue": [
                            ""
                          ],
                          "items": [
                            {
                              "displayName": "Parameter",
                              "name": "parameter",
                              "type": "OBJECT",
                              "properties": [
                                {
                                  "description": "The key of the parameter.",
                                  "displayName": "Key",
                                  "name": "key",
                                  "type": "STRING",
                                  "defaultValue": ""
                                },
                                {
                                  "description": "The value of the parameter.",
                                  "displayName": "Value",
                                  "name": "value",
                                  "type": "ANY",
                                  "types": [
                                    {
                                      "type": "STRING"
                                    },
                                    {
                                      "type": "OBJECT",
                                      "properties": [
                                        {
                                          "name": "extension",
                                          "type": "STRING",
                                          "required": true
                                        },
                                        {
                                          "name": "mimeType",
                                          "type": "STRING",
                                          "required": true
                                        },
                                        {
                                          "name": "name",
                                          "type": "STRING",
                                          "required": true
                                        },
                                        {
                                          "name": "url",
                                          "type": "STRING",
                                          "required": true
                                        }
                                      ]
                                    }
                                  ]
                                }
                              ]
                            }
                          ]
                        },
                        {
                          "description": "The raw text to send.",
                          "displayOption": {
                            "showWhen": {
                              "bodyContentType": {
                                "values": [
                                  "RAW"
                                ]
                              }
                            }
                          },
                          "displayName": "Raw",
                          "name": "bodyParameters",
                          "type": "STRING"
                        },
                        {
                          "description": "The object property which contains a reference to the file with data to upload.",
                          "displayOption": {
                            "hideWhen": {
                              "sendFile": {
                                "values": [
                                  false
                                ]
                              }
                            },
                            "showWhen": {
                              "bodyContentType": {
                                "values": [
                                  "BINARY",
                                  "JSON",
                                  "RAW",
                                  "XML"
                                ]
                              }
                            }
                          },
                          "displayName": "File",
                          "name": "fileEntry",
                          "type": "OBJECT",
                          "properties": [
                            {
                              "name": "extension",
                              "type": "STRING",
                              "required": true
                            },
                            {
                              "name": "mimeType",
                              "type": "STRING",
                              "required": true
                            },
                            {
                              "name": "name",
                              "type": "STRING",
                              "required": true
                            },
                            {
                              "name": "url",
                              "type": "STRING",
                              "required": true
                            }
                          ]
                        },
                        {
                          "displayName": "Options",
                          "placeholder": "Add Option",
                          "type": "OPTION",
                          "options": [
                            {
                              "description": "Content-Type to use when sending body parameters.",
                              "displayName": "Body Content Type",
                              "name": "bodyContentType",
                              "type": "STRING",
                              "options": [
                                {
                                  "name": "JSON",
                                  "value": "JSON"
                                },
                                {
                                  "name": "Raw",
                                  "value": "RAW"
                                },
                                {
                                  "name": "Form-Data",
                                  "value": "FORM_DATA"
                                },
                                {
                                  "name": "Form-Urlencoded",
                                  "value": "FORM_URLENCODED"
                                },
                                {
                                  "name": "Binary",
                                  "value": "BINARY"
                                },
                                {
                                  "name": "XML",
                                  "value": "XML"
                                }
                              ],
                              "defaultValue": "JSON"
                            },
                            {
                              "description": "Mime-Type to use when sending raw body content.",
                              "displayOption": {
                                "showWhen": {
                                  "bodyContentType": {
                                    "values": [
                                      "RAW"
                                    ]
                                  }
                                }
                              },
                              "displayName": "Mime Type",
                              "name": "mimeType",
                              "placeholder": "text/xml",
                              "type": "STRING"
                            },
                            {
                              "description": "Returns the full response data instead of only the body.",
                              "displayName": "Full Response",
                              "name": "fullResponse",
                              "type": "BOOLEAN",
                              "defaultValue": false
                            },
                            {
                              "description": "Follow non-GET HTTP 3xx redirects.",
                              "displayName": "Follow All Redirects",
                              "name": "followAllRedirects",
                              "type": "BOOLEAN",
                              "defaultValue": false
                            },
                            {
                              "description": "Follow GET HTTP 3xx redirects.",
                              "displayName": "Follow GET Redirect",
                              "name": "followRedirect",
                              "type": "BOOLEAN",
                              "defaultValue": false
                            },
                            {
                              "description": "Succeeds also when the status code is not 2xx.",
                              "displayName": "Ignore Response Code",
                              "name": "ignoreResponseCode",
                              "type": "BOOLEAN",
                              "defaultValue": false
                            },
                            {
                              "description": "HTTP proxy to use.",
                              "displayName": "Proxy",
                              "name": "proxy",
                              "placeholder": "https://myproxy:3128",
                              "type": "STRING",
                              "defaultValue": ""
                            },
                            {
                              "description": "Time in ms to wait for the server to send a response before aborting the request.",
                              "displayName": "Timeout",
                              "name": "timeout",
                              "type": "INTEGER",
                              "defaultValue": 1000,
                              "minValue": 1
                            }
                          ]
                        }
                      ],
                      "outputs": [
                        {
                          "displayOption": {
                            "showWhen": {
                              "responseFormat": {
                                "values": [
                                  "JSON",
                                  "XML"
                                ]
                              }
                            }
                          },
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
                          "displayOption": {
                            "showWhen": {
                              "responseFormat": {
                                "values": [
                                  "TEXT"
                                ]
                              }
                            }
                          },
                          "type": "STRING"
                        },
                        {
                          "displayOption": {
                            "showWhen": {
                              "responseFormat": {
                                "values": [
                                  "FILE"
                                ]
                              }
                            }
                          },
                          "type": "OBJECT",
                          "properties": [
                            {
                              "name": "extension",
                              "type": "STRING",
                              "required": true
                            },
                            {
                              "name": "mimeType",
                              "type": "STRING",
                              "required": true
                            },
                            {
                              "name": "name",
                              "type": "STRING",
                              "required": true
                            },
                            {
                              "name": "url",
                              "type": "STRING",
                              "required": true
                            }
                          ]
                        }
                      ],
                      "displayName": "POST"
                    },
                    {
                      "description": "The request method to use.",
                      "name": "put",
                      "inputs": [
                        {
                          "description": "The URI to make the request to",
                          "displayName": "URI",
                          "name": "uri",
                          "placeholder": "https://example.com/index.html",
                          "type": "STRING",
                          "required": true,
                          "defaultValue": ""
                        },
                        {
                          "description": "Download the response even if SSL certificate validation is not possible.",
                          "displayName": "Allow Unauthorized Certs",
                          "name": "allowUnauthorizedCerts",
                          "type": "BOOLEAN",
                          "defaultValue": false
                        },
                        {
                          "description": "The format in which the data gets returned from the URL.",
                          "displayName": "Response Format",
                          "name": "responseFormat",
                          "type": "STRING",
                          "options": [
                            {
                              "description": "The response is automatically converted to object/array.",
                              "name": "JSON",
                              "value": "JSON"
                            },
                            {
                              "description": "The response is automatically converted to object/array.",
                              "name": "XML",
                              "value": "XML"
                            },
                            {
                              "description": "The response is returned as a text.",
                              "name": "Text",
                              "value": "TEXT"
                            },
                            {
                              "description": "The response is returned as a file object.",
                              "name": "File",
                              "value": "FILE"
                            }
                          ],
                          "defaultValue": "JSON"
                        },
                        {
                          "description": "The name of the file if the response is returned as a file object.",
                          "displayOption": {
                            "showWhen": {
                              "responseFormat": {
                                "values": [
                                  "FILE"
                                ]
                              }
                            }
                          },
                          "displayName": "Response File Name",
                          "name": "responseFileName",
                          "type": "STRING"
                        },
                        {
                          "description": "Header parameters to send.",
                          "displayName": "Header Parameters",
                          "name": "headerParameters",
                          "placeholder": "Add Parameter",
                          "type": "ARRAY",
                          "defaultValue": [
                            ""
                          ],
                          "items": [
                            {
                              "displayName": "Parameter",
                              "name": "parameter",
                              "type": "OBJECT",
                              "properties": [
                                {
                                  "description": "The key of the parameter.",
                                  "displayName": "Key",
                                  "name": "key",
                                  "type": "STRING",
                                  "defaultValue": ""
                                },
                                {
                                  "description": "The value of the parameter.",
                                  "displayName": "Value",
                                  "name": "value",
                                  "type": "STRING",
                                  "defaultValue": ""
                                }
                              ]
                            }
                          ]
                        },
                        {
                          "description": "Query parameters to send.",
                          "displayName": "Query Parameters",
                          "name": "queryParameters",
                          "placeholder": "Add Parameter",
                          "type": "ARRAY",
                          "defaultValue": [
                            ""
                          ],
                          "items": [
                            {
                              "displayName": "Parameter",
                              "name": "parameter",
                              "type": "OBJECT",
                              "properties": [
                                {
                                  "description": "The key of the parameter.",
                                  "displayName": "Key",
                                  "name": "key",
                                  "type": "STRING",
                                  "defaultValue": ""
                                },
                                {
                                  "description": "The value of the parameter.",
                                  "displayName": "Value",
                                  "name": "value",
                                  "type": "STRING",
                                  "defaultValue": ""
                                }
                              ]
                            }
                          ]
                        },
                        {
                          "description": "Send file instead of body parameters.",
                          "displayOption": {
                            "showWhen": {
                              "bodyContentType": {
                                "values": [
                                  "JSON",
                                  "RAW",
                                  "XML"
                                ]
                              }
                            }
                          },
                          "displayName": "Send File",
                          "name": "sendFile",
                          "type": "BOOLEAN",
                          "defaultValue": false
                        },
                        {
                          "description": "Body parameters to send.",
                          "displayOption": {
                            "showWhen": {
                              "bodyContentType": {
                                "values": [
                                  "JSON"
                                ]
                              }
                            }
                          },
                          "displayName": "Body Parameters",
                          "name": "bodyParameters",
                          "placeholder": "Add Parameter",
                          "type": "OBJECT",
                          "additionalProperties": true
                        },
                        {
                          "description": "Body parameters to send.",
                          "displayOption": {
                            "showWhen": {
                              "bodyContentType": {
                                "values": [
                                  "FORM_DATA"
                                ]
                              }
                            }
                          },
                          "displayName": "Body Parameters",
                          "name": "bodyParameters",
                          "placeholder": "Add Parameter",
                          "type": "ARRAY",
                          "defaultValue": [
                            ""
                          ],
                          "items": [
                            {
                              "displayName": "Parameter",
                              "name": "parameter",
                              "type": "OBJECT",
                              "properties": [
                                {
                                  "description": "The key of the parameter.",
                                  "displayName": "Key",
                                  "name": "key",
                                  "type": "STRING",
                                  "defaultValue": ""
                                },
                                {
                                  "description": "The value of the parameter.",
                                  "displayName": "Value",
                                  "name": "value",
                                  "type": "STRING",
                                  "defaultValue": ""
                                }
                              ]
                            }
                          ]
                        },
                        {
                          "description": "Body parameters to send.",
                          "displayOption": {
                            "showWhen": {
                              "bodyContentType": {
                                "values": [
                                  "FORM_URLENCODED"
                                ]
                              }
                            }
                          },
                          "displayName": "Body Parameters",
                          "name": "bodyParameters",
                          "placeholder": "Add Parameter",
                          "type": "ARRAY",
                          "defaultValue": [
                            ""
                          ],
                          "items": [
                            {
                              "displayName": "Parameter",
                              "name": "parameter",
                              "type": "OBJECT",
                              "properties": [
                                {
                                  "description": "The key of the parameter.",
                                  "displayName": "Key",
                                  "name": "key",
                                  "type": "STRING",
                                  "defaultValue": ""
                                },
                                {
                                  "description": "The value of the parameter.",
                                  "displayName": "Value",
                                  "name": "value",
                                  "type": "ANY",
                                  "types": [
                                    {
                                      "type": "STRING"
                                    },
                                    {
                                      "type": "OBJECT",
                                      "properties": [
                                        {
                                          "name": "extension",
                                          "type": "STRING",
                                          "required": true
                                        },
                                        {
                                          "name": "mimeType",
                                          "type": "STRING",
                                          "required": true
                                        },
                                        {
                                          "name": "name",
                                          "type": "STRING",
                                          "required": true
                                        },
                                        {
                                          "name": "url",
                                          "type": "STRING",
                                          "required": true
                                        }
                                      ]
                                    }
                                  ]
                                }
                              ]
                            }
                          ]
                        },
                        {
                          "description": "The raw text to send.",
                          "displayOption": {
                            "showWhen": {
                              "bodyContentType": {
                                "values": [
                                  "RAW"
                                ]
                              }
                            }
                          },
                          "displayName": "Raw",
                          "name": "bodyParameters",
                          "type": "STRING"
                        },
                        {
                          "description": "The object property which contains a reference to the file with data to upload.",
                          "displayOption": {
                            "hideWhen": {
                              "sendFile": {
                                "values": [
                                  false
                                ]
                              }
                            },
                            "showWhen": {
                              "bodyContentType": {
                                "values": [
                                  "BINARY",
                                  "JSON",
                                  "RAW",
                                  "XML"
                                ]
                              }
                            }
                          },
                          "displayName": "File",
                          "name": "fileEntry",
                          "type": "OBJECT",
                          "properties": [
                            {
                              "name": "extension",
                              "type": "STRING",
                              "required": true
                            },
                            {
                              "name": "mimeType",
                              "type": "STRING",
                              "required": true
                            },
                            {
                              "name": "name",
                              "type": "STRING",
                              "required": true
                            },
                            {
                              "name": "url",
                              "type": "STRING",
                              "required": true
                            }
                          ]
                        },
                        {
                          "displayName": "Options",
                          "placeholder": "Add Option",
                          "type": "OPTION",
                          "options": [
                            {
                              "description": "Content-Type to use when sending body parameters.",
                              "displayName": "Body Content Type",
                              "name": "bodyContentType",
                              "type": "STRING",
                              "options": [
                                {
                                  "name": "JSON",
                                  "value": "JSON"
                                },
                                {
                                  "name": "Raw",
                                  "value": "RAW"
                                },
                                {
                                  "name": "Form-Data",
                                  "value": "FORM_DATA"
                                },
                                {
                                  "name": "Form-Urlencoded",
                                  "value": "FORM_URLENCODED"
                                },
                                {
                                  "name": "Binary",
                                  "value": "BINARY"
                                },
                                {
                                  "name": "XML",
                                  "value": "XML"
                                }
                              ],
                              "defaultValue": "JSON"
                            },
                            {
                              "description": "Mime-Type to use when sending raw body content.",
                              "displayOption": {
                                "showWhen": {
                                  "bodyContentType": {
                                    "values": [
                                      "RAW"
                                    ]
                                  }
                                }
                              },
                              "displayName": "Mime Type",
                              "name": "mimeType",
                              "placeholder": "text/xml",
                              "type": "STRING"
                            },
                            {
                              "description": "Returns the full response data instead of only the body.",
                              "displayName": "Full Response",
                              "name": "fullResponse",
                              "type": "BOOLEAN",
                              "defaultValue": false
                            },
                            {
                              "description": "Follow non-GET HTTP 3xx redirects.",
                              "displayName": "Follow All Redirects",
                              "name": "followAllRedirects",
                              "type": "BOOLEAN",
                              "defaultValue": false
                            },
                            {
                              "description": "Follow GET HTTP 3xx redirects.",
                              "displayName": "Follow GET Redirect",
                              "name": "followRedirect",
                              "type": "BOOLEAN",
                              "defaultValue": false
                            },
                            {
                              "description": "Succeeds also when the status code is not 2xx.",
                              "displayName": "Ignore Response Code",
                              "name": "ignoreResponseCode",
                              "type": "BOOLEAN",
                              "defaultValue": false
                            },
                            {
                              "description": "HTTP proxy to use.",
                              "displayName": "Proxy",
                              "name": "proxy",
                              "placeholder": "https://myproxy:3128",
                              "type": "STRING",
                              "defaultValue": ""
                            },
                            {
                              "description": "Time in ms to wait for the server to send a response before aborting the request.",
                              "displayName": "Timeout",
                              "name": "timeout",
                              "type": "INTEGER",
                              "defaultValue": 1000,
                              "minValue": 1
                            }
                          ]
                        }
                      ],
                      "outputs": [
                        {
                          "displayOption": {
                            "showWhen": {
                              "responseFormat": {
                                "values": [
                                  "JSON",
                                  "XML"
                                ]
                              }
                            }
                          },
                          "name": "",
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
                          "displayOption": {
                            "showWhen": {
                              "responseFormat": {
                                "values": [
                                  "TEXT"
                                ]
                              }
                            }
                          },
                          "type": "STRING"
                        },
                        {
                          "displayOption": {
                            "showWhen": {
                              "responseFormat": {
                                "values": [
                                  "FILE"
                                ]
                              }
                            }
                          },
                          "type": "OBJECT",
                          "properties": [
                            {
                              "name": "extension",
                              "type": "STRING",
                              "required": true
                            },
                            {
                              "name": "mimeType",
                              "type": "STRING",
                              "required": true
                            },
                            {
                              "name": "name",
                              "type": "STRING",
                              "required": true
                            },
                            {
                              "name": "url",
                              "type": "STRING",
                              "required": true
                            }
                          ]
                        }
                      ],
                      "displayName": "PUT"
                    },
                    {
                      "description": "The request method to use.",
                      "name": "patch",
                      "inputs": [
                        {
                          "description": "The URI to make the request to",
                          "displayName": "URI",
                          "name": "uri",
                          "placeholder": "https://example.com/index.html",
                          "type": "STRING",
                          "required": true,
                          "defaultValue": ""
                        },
                        {
                          "description": "Download the response even if SSL certificate validation is not possible.",
                          "displayName": "Allow Unauthorized Certs",
                          "name": "allowUnauthorizedCerts",
                          "type": "BOOLEAN",
                          "defaultValue": false
                        },
                        {
                          "description": "The format in which the data gets returned from the URL.",
                          "displayName": "Response Format",
                          "name": "responseFormat",
                          "type": "STRING",
                          "options": [
                            {
                              "description": "The response is automatically converted to object/array.",
                              "name": "JSON",
                              "value": "JSON"
                            },
                            {
                              "description": "The response is automatically converted to object/array.",
                              "name": "XML",
                              "value": "XML"
                            },
                            {
                              "description": "The response is returned as a text.",
                              "name": "Text",
                              "value": "TEXT"
                            },
                            {
                              "description": "The response is returned as a file object.",
                              "name": "File",
                              "value": "FILE"
                            }
                          ],
                          "defaultValue": "JSON"
                        },
                        {
                          "description": "The name of the file if the response is returned as a file object.",
                          "displayOption": {
                            "showWhen": {
                              "responseFormat": {
                                "values": [
                                  "FILE"
                                ]
                              }
                            }
                          },
                          "displayName": "Response File Name",
                          "name": "responseFileName",
                          "type": "STRING"
                        },
                        {
                          "description": "Header parameters to send.",
                          "displayName": "Header Parameters",
                          "name": "headerParameters",
                          "placeholder": "Add Parameter",
                          "type": "ARRAY",
                          "defaultValue": [
                            ""
                          ],
                          "items": [
                            {
                              "displayName": "Parameter",
                              "name": "parameter",
                              "type": "OBJECT",
                              "properties": [
                                {
                                  "description": "The key of the parameter.",
                                  "displayName": "Key",
                                  "name": "key",
                                  "type": "STRING",
                                  "defaultValue": ""
                                },
                                {
                                  "description": "The value of the parameter.",
                                  "displayName": "Value",
                                  "name": "value",
                                  "type": "STRING",
                                  "defaultValue": ""
                                }
                              ]
                            }
                          ]
                        },
                        {
                          "description": "Query parameters to send.",
                          "displayName": "Query Parameters",
                          "name": "queryParameters",
                          "placeholder": "Add Parameter",
                          "type": "ARRAY",
                          "defaultValue": [
                            ""
                          ],
                          "items": [
                            {
                              "displayName": "Parameter",
                              "name": "parameter",
                              "type": "OBJECT",
                              "properties": [
                                {
                                  "description": "The key of the parameter.",
                                  "displayName": "Key",
                                  "name": "key",
                                  "type": "STRING",
                                  "defaultValue": ""
                                },
                                {
                                  "description": "The value of the parameter.",
                                  "displayName": "Value",
                                  "name": "value",
                                  "type": "STRING",
                                  "defaultValue": ""
                                }
                              ]
                            }
                          ]
                        },
                        {
                          "description": "Send file instead of body parameters.",
                          "displayOption": {
                            "showWhen": {
                              "bodyContentType": {
                                "values": [
                                  "JSON",
                                  "RAW",
                                  "XML"
                                ]
                              }
                            }
                          },
                          "displayName": "Send File",
                          "name": "sendFile",
                          "type": "BOOLEAN",
                          "defaultValue": false
                        },
                        {
                          "description": "Body parameters to send.",
                          "displayOption": {
                            "showWhen": {
                              "bodyContentType": {
                                "values": [
                                  "JSON"
                                ]
                              }
                            }
                          },
                          "displayName": "Body Parameters",
                          "name": "bodyParameters",
                          "placeholder": "Add Parameter",
                          "type": "OBJECT",
                          "additionalProperties": true
                        },
                        {
                          "description": "Body parameters to send.",
                          "displayOption": {
                            "showWhen": {
                              "bodyContentType": {
                                "values": [
                                  "FORM_DATA"
                                ]
                              }
                            }
                          },
                          "displayName": "Body Parameters",
                          "name": "bodyParameters",
                          "placeholder": "Add Parameter",
                          "type": "ARRAY",
                          "defaultValue": [
                            ""
                          ],
                          "items": [
                            {
                              "displayName": "Parameter",
                              "name": "parameter",
                              "type": "OBJECT",
                              "properties": [
                                {
                                  "description": "The key of the parameter.",
                                  "displayName": "Key",
                                  "name": "key",
                                  "type": "STRING",
                                  "defaultValue": ""
                                },
                                {
                                  "description": "The value of the parameter.",
                                  "displayName": "Value",
                                  "name": "value",
                                  "type": "STRING",
                                  "defaultValue": ""
                                }
                              ]
                            }
                          ]
                        },
                        {
                          "description": "Body parameters to send.",
                          "displayOption": {
                            "showWhen": {
                              "bodyContentType": {
                                "values": [
                                  "FORM_URLENCODED"
                                ]
                              }
                            }
                          },
                          "displayName": "Body Parameters",
                          "name": "bodyParameters",
                          "placeholder": "Add Parameter",
                          "type": "ARRAY",
                          "defaultValue": [
                            ""
                          ],
                          "items": [
                            {
                              "displayName": "Parameter",
                              "name": "parameter",
                              "type": "OBJECT",
                              "properties": [
                                {
                                  "description": "The key of the parameter.",
                                  "displayName": "Key",
                                  "name": "key",
                                  "type": "STRING",
                                  "defaultValue": ""
                                },
                                {
                                  "description": "The value of the parameter.",
                                  "displayName": "Value",
                                  "name": "value",
                                  "type": "ANY",
                                  "types": [
                                    {
                                      "type": "STRING"
                                    },
                                    {
                                      "type": "OBJECT",
                                      "properties": [
                                        {
                                          "name": "extension",
                                          "type": "STRING",
                                          "required": true
                                        },
                                        {
                                          "name": "mimeType",
                                          "type": "STRING",
                                          "required": true
                                        },
                                        {
                                          "name": "name",
                                          "type": "STRING",
                                          "required": true
                                        },
                                        {
                                          "name": "url",
                                          "type": "STRING",
                                          "required": true
                                        }
                                      ]
                                    }
                                  ]
                                }
                              ]
                            }
                          ]
                        },
                        {
                          "description": "The raw text to send.",
                          "displayOption": {
                            "showWhen": {
                              "bodyContentType": {
                                "values": [
                                  "RAW"
                                ]
                              }
                            }
                          },
                          "displayName": "Raw",
                          "name": "bodyParameters",
                          "type": "STRING"
                        },
                        {
                          "description": "The object property which contains a reference to the file with data to upload.",
                          "displayOption": {
                            "hideWhen": {
                              "sendFile": {
                                "values": [
                                  false
                                ]
                              }
                            },
                            "showWhen": {
                              "bodyContentType": {
                                "values": [
                                  "BINARY",
                                  "JSON",
                                  "RAW",
                                  "XML"
                                ]
                              }
                            }
                          },
                          "displayName": "File",
                          "name": "fileEntry",
                          "type": "OBJECT",
                          "properties": [
                            {
                              "name": "extension",
                              "type": "STRING",
                              "required": true
                            },
                            {
                              "name": "mimeType",
                              "type": "STRING",
                              "required": true
                            },
                            {
                              "name": "name",
                              "type": "STRING",
                              "required": true
                            },
                            {
                              "name": "url",
                              "type": "STRING",
                              "required": true
                            }
                          ]
                        },
                        {
                          "displayName": "Options",
                          "placeholder": "Add Option",
                          "type": "OPTION",
                          "options": [
                            {
                              "description": "Content-Type to use when sending body parameters.",
                              "displayName": "Body Content Type",
                              "name": "bodyContentType",
                              "type": "STRING",
                              "options": [
                                {
                                  "name": "JSON",
                                  "value": "JSON"
                                },
                                {
                                  "name": "Raw",
                                  "value": "RAW"
                                },
                                {
                                  "name": "Form-Data",
                                  "value": "FORM_DATA"
                                },
                                {
                                  "name": "Form-Urlencoded",
                                  "value": "FORM_URLENCODED"
                                },
                                {
                                  "name": "Binary",
                                  "value": "BINARY"
                                },
                                {
                                  "name": "XML",
                                  "value": "XML"
                                }
                              ],
                              "defaultValue": "JSON"
                            },
                            {
                              "description": "Mime-Type to use when sending raw body content.",
                              "displayOption": {
                                "showWhen": {
                                  "bodyContentType": {
                                    "values": [
                                      "RAW"
                                    ]
                                  }
                                }
                              },
                              "displayName": "Mime Type",
                              "name": "mimeType",
                              "placeholder": "text/xml",
                              "type": "STRING"
                            },
                            {
                              "description": "Returns the full response data instead of only the body.",
                              "displayName": "Full Response",
                              "name": "fullResponse",
                              "type": "BOOLEAN",
                              "defaultValue": false
                            },
                            {
                              "description": "Follow non-GET HTTP 3xx redirects.",
                              "displayName": "Follow All Redirects",
                              "name": "followAllRedirects",
                              "type": "BOOLEAN",
                              "defaultValue": false
                            },
                            {
                              "description": "Follow GET HTTP 3xx redirects.",
                              "displayName": "Follow GET Redirect",
                              "name": "followRedirect",
                              "type": "BOOLEAN",
                              "defaultValue": false
                            },
                            {
                              "description": "Succeeds also when the status code is not 2xx.",
                              "displayName": "Ignore Response Code",
                              "name": "ignoreResponseCode",
                              "type": "BOOLEAN",
                              "defaultValue": false
                            },
                            {
                              "description": "HTTP proxy to use.",
                              "displayName": "Proxy",
                              "name": "proxy",
                              "placeholder": "https://myproxy:3128",
                              "type": "STRING",
                              "defaultValue": ""
                            },
                            {
                              "description": "Time in ms to wait for the server to send a response before aborting the request.",
                              "displayName": "Timeout",
                              "name": "timeout",
                              "type": "INTEGER",
                              "defaultValue": 1000,
                              "minValue": 1
                            }
                          ]
                        }
                      ],
                      "outputs": [
                        {
                          "displayOption": {
                            "showWhen": {
                              "responseFormat": {
                                "values": [
                                  "JSON",
                                  "XML"
                                ]
                              }
                            }
                          },
                          "name": "",
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
                          "displayOption": {
                            "showWhen": {
                              "responseFormat": {
                                "values": [
                                  "TEXT"
                                ]
                              }
                            }
                          },
                          "type": "STRING"
                        },
                        {
                          "displayOption": {
                            "showWhen": {
                              "responseFormat": {
                                "values": [
                                  "FILE"
                                ]
                              }
                            }
                          },
                          "type": "OBJECT",
                          "properties": [
                            {
                              "name": "extension",
                              "type": "STRING",
                              "required": true
                            },
                            {
                              "name": "mimeType",
                              "type": "STRING",
                              "required": true
                            },
                            {
                              "name": "name",
                              "type": "STRING",
                              "required": true
                            },
                            {
                              "name": "url",
                              "type": "STRING",
                              "required": true
                            }
                          ]
                        }
                      ],
                      "displayName": "PATCH"
                    },
                    {
                      "description": "The request method to use.",
                      "name": "delete",
                      "inputs": [
                        {
                          "description": "The URI to make the request to",
                          "displayName": "URI",
                          "name": "uri",
                          "placeholder": "https://example.com/index.html",
                          "type": "STRING",
                          "required": true,
                          "defaultValue": ""
                        },
                        {
                          "description": "Download the response even if SSL certificate validation is not possible.",
                          "displayName": "Allow Unauthorized Certs",
                          "name": "allowUnauthorizedCerts",
                          "type": "BOOLEAN",
                          "defaultValue": false
                        },
                        {
                          "description": "The format in which the data gets returned from the URL.",
                          "displayName": "Response Format",
                          "name": "responseFormat",
                          "type": "STRING",
                          "options": [
                            {
                              "description": "The response is automatically converted to object/array.",
                              "name": "JSON",
                              "value": "JSON"
                            },
                            {
                              "description": "The response is automatically converted to object/array.",
                              "name": "XML",
                              "value": "XML"
                            },
                            {
                              "description": "The response is returned as a text.",
                              "name": "Text",
                              "value": "TEXT"
                            },
                            {
                              "description": "The response is returned as a file object.",
                              "name": "File",
                              "value": "FILE"
                            }
                          ],
                          "defaultValue": "JSON"
                        },
                        {
                          "description": "The name of the file if the response is returned as a file object.",
                          "displayOption": {
                            "showWhen": {
                              "responseFormat": {
                                "values": [
                                  "FILE"
                                ]
                              }
                            }
                          },
                          "displayName": "Response File Name",
                          "name": "responseFileName",
                          "type": "STRING"
                        },
                        {
                          "description": "Header parameters to send.",
                          "displayName": "Header Parameters",
                          "name": "headerParameters",
                          "placeholder": "Add Parameter",
                          "type": "ARRAY",
                          "defaultValue": [
                            ""
                          ],
                          "items": [
                            {
                              "displayName": "Parameter",
                              "name": "parameter",
                              "type": "OBJECT",
                              "properties": [
                                {
                                  "description": "The key of the parameter.",
                                  "displayName": "Key",
                                  "name": "key",
                                  "type": "STRING",
                                  "defaultValue": ""
                                },
                                {
                                  "description": "The value of the parameter.",
                                  "displayName": "Value",
                                  "name": "value",
                                  "type": "STRING",
                                  "defaultValue": ""
                                }
                              ]
                            }
                          ]
                        },
                        {
                          "description": "Query parameters to send.",
                          "displayName": "Query Parameters",
                          "name": "queryParameters",
                          "placeholder": "Add Parameter",
                          "type": "ARRAY",
                          "defaultValue": [
                            ""
                          ],
                          "items": [
                            {
                              "displayName": "Parameter",
                              "name": "parameter",
                              "type": "OBJECT",
                              "properties": [
                                {
                                  "description": "The key of the parameter.",
                                  "displayName": "Key",
                                  "name": "key",
                                  "type": "STRING",
                                  "defaultValue": ""
                                },
                                {
                                  "description": "The value of the parameter.",
                                  "displayName": "Value",
                                  "name": "value",
                                  "type": "STRING",
                                  "defaultValue": ""
                                }
                              ]
                            }
                          ]
                        },
                        {
                          "displayName": "Options",
                          "placeholder": "Add Option",
                          "type": "OPTION",
                          "options": [
                            {
                              "description": "Returns the full response data instead of only the body.",
                              "displayName": "Full Response",
                              "name": "fullResponse",
                              "type": "BOOLEAN",
                              "defaultValue": false
                            },
                            {
                              "description": "Follow non-GET HTTP 3xx redirects.",
                              "displayName": "Follow All Redirects",
                              "name": "followAllRedirects",
                              "type": "BOOLEAN",
                              "defaultValue": false
                            },
                            {
                              "description": "Follow GET HTTP 3xx redirects.",
                              "displayName": "Follow GET Redirect",
                              "name": "followRedirect",
                              "type": "BOOLEAN",
                              "defaultValue": false
                            },
                            {
                              "description": "Succeeds also when the status code is not 2xx.",
                              "displayName": "Ignore Response Code",
                              "name": "ignoreResponseCode",
                              "type": "BOOLEAN",
                              "defaultValue": false
                            },
                            {
                              "description": "HTTP proxy to use.",
                              "displayName": "Proxy",
                              "name": "proxy",
                              "placeholder": "https://myproxy:3128",
                              "type": "STRING",
                              "defaultValue": ""
                            },
                            {
                              "description": "Time in ms to wait for the server to send a response before aborting the request.",
                              "displayName": "Timeout",
                              "name": "timeout",
                              "type": "INTEGER",
                              "defaultValue": 1000,
                              "minValue": 1
                            }
                          ]
                        }
                      ],
                      "outputs": [
                        {
                          "displayOption": {
                            "showWhen": {
                              "responseFormat": {
                                "values": [
                                  "JSON",
                                  "XML"
                                ]
                              }
                            }
                          },
                          "name": "",
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
                          "displayOption": {
                            "showWhen": {
                              "responseFormat": {
                                "values": [
                                  "TEXT"
                                ]
                              }
                            }
                          },
                          "type": "STRING"
                        },
                        {
                          "displayOption": {
                            "showWhen": {
                              "responseFormat": {
                                "values": [
                                  "FILE"
                                ]
                              }
                            }
                          },
                          "type": "OBJECT",
                          "properties": [
                            {
                              "name": "extension",
                              "type": "STRING",
                              "required": true
                            },
                            {
                              "name": "mimeType",
                              "type": "STRING",
                              "required": true
                            },
                            {
                              "name": "name",
                              "type": "STRING",
                              "required": true
                            },
                            {
                              "name": "url",
                              "type": "STRING",
                              "required": true
                            }
                          ]
                        }
                      ],
                      "displayName": "DELETE"
                    },
                    {
                      "description": "The request method to use.",
                      "name": "head",
                      "inputs": [
                        {
                          "description": "The URI to make the request to",
                          "displayName": "URI",
                          "name": "uri",
                          "placeholder": "https://example.com/index.html",
                          "type": "STRING",
                          "required": true,
                          "defaultValue": ""
                        },
                        {
                          "description": "Download the response even if SSL certificate validation is not possible.",
                          "displayName": "Allow Unauthorized Certs",
                          "name": "allowUnauthorizedCerts",
                          "type": "BOOLEAN",
                          "defaultValue": false
                        },
                        {
                          "description": "The format in which the data gets returned from the URL.",
                          "displayName": "Response Format",
                          "name": "responseFormat",
                          "type": "STRING",
                          "options": [
                            {
                              "description": "The response is automatically converted to object/array.",
                              "name": "JSON",
                              "value": "JSON"
                            },
                            {
                              "description": "The response is automatically converted to object/array.",
                              "name": "XML",
                              "value": "XML"
                            },
                            {
                              "description": "The response is returned as a text.",
                              "name": "Text",
                              "value": "TEXT"
                            },
                            {
                              "description": "The response is returned as a file object.",
                              "name": "File",
                              "value": "FILE"
                            }
                          ],
                          "defaultValue": "JSON"
                        },
                        {
                          "description": "The name of the file if the response is returned as a file object.",
                          "displayOption": {
                            "showWhen": {
                              "responseFormat": {
                                "values": [
                                  "FILE"
                                ]
                              }
                            }
                          },
                          "displayName": "Response File Name",
                          "name": "responseFileName",
                          "type": "STRING"
                        },
                        {
                          "description": "Header parameters to send.",
                          "displayName": "Header Parameters",
                          "name": "headerParameters",
                          "placeholder": "Add Parameter",
                          "type": "ARRAY",
                          "defaultValue": [
                            ""
                          ],
                          "items": [
                            {
                              "displayName": "Parameter",
                              "name": "parameter",
                              "type": "OBJECT",
                              "properties": [
                                {
                                  "description": "The key of the parameter.",
                                  "displayName": "Key",
                                  "name": "key",
                                  "type": "STRING",
                                  "defaultValue": ""
                                },
                                {
                                  "description": "The value of the parameter.",
                                  "displayName": "Value",
                                  "name": "value",
                                  "type": "STRING",
                                  "defaultValue": ""
                                }
                              ]
                            }
                          ]
                        },
                        {
                          "description": "Query parameters to send.",
                          "displayName": "Query Parameters",
                          "name": "queryParameters",
                          "placeholder": "Add Parameter",
                          "type": "ARRAY",
                          "defaultValue": [
                            ""
                          ],
                          "items": [
                            {
                              "displayName": "Parameter",
                              "name": "parameter",
                              "type": "OBJECT",
                              "properties": [
                                {
                                  "description": "The key of the parameter.",
                                  "displayName": "Key",
                                  "name": "key",
                                  "type": "STRING",
                                  "defaultValue": ""
                                },
                                {
                                  "description": "The value of the parameter.",
                                  "displayName": "Value",
                                  "name": "value",
                                  "type": "STRING",
                                  "defaultValue": ""
                                }
                              ]
                            }
                          ]
                        },
                        {
                          "displayName": "Options",
                          "placeholder": "Add Option",
                          "type": "OPTION",
                          "options": [
                            {
                              "description": "Returns the full response data instead of only the body.",
                              "displayName": "Full Response",
                              "name": "fullResponse",
                              "type": "BOOLEAN",
                              "defaultValue": false
                            },
                            {
                              "description": "Follow non-GET HTTP 3xx redirects.",
                              "displayName": "Follow All Redirects",
                              "name": "followAllRedirects",
                              "type": "BOOLEAN",
                              "defaultValue": false
                            },
                            {
                              "description": "Follow GET HTTP 3xx redirects.",
                              "displayName": "Follow GET Redirect",
                              "name": "followRedirect",
                              "type": "BOOLEAN",
                              "defaultValue": false
                            },
                            {
                              "description": "Succeeds also when the status code is not 2xx.",
                              "displayName": "Ignore Response Code",
                              "name": "ignoreResponseCode",
                              "type": "BOOLEAN",
                              "defaultValue": false
                            },
                            {
                              "description": "HTTP proxy to use.",
                              "displayName": "Proxy",
                              "name": "proxy",
                              "placeholder": "https://myproxy:3128",
                              "type": "STRING",
                              "defaultValue": ""
                            },
                            {
                              "description": "Time in ms to wait for the server to send a response before aborting the request.",
                              "displayName": "Timeout",
                              "name": "timeout",
                              "type": "INTEGER",
                              "defaultValue": 1000,
                              "minValue": 1
                            }
                          ]
                        }
                      ],
                      "outputs": [
                        {
                          "displayOption": {
                            "showWhen": {
                              "responseFormat": {
                                "values": [
                                  "JSON",
                                  "XML"
                                ]
                              }
                            }
                          },
                          "name": "",
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
                          "displayOption": {
                            "showWhen": {
                              "responseFormat": {
                                "values": [
                                  "TEXT"
                                ]
                              }
                            }
                          },
                          "type": "STRING"
                        },
                        {
                          "displayOption": {
                            "showWhen": {
                              "responseFormat": {
                                "values": [
                                  "FILE"
                                ]
                              }
                            }
                          },
                          "type": "OBJECT",
                          "properties": [
                            {
                              "name": "extension",
                              "type": "STRING",
                              "required": true
                            },
                            {
                              "name": "mimeType",
                              "type": "STRING",
                              "required": true
                            },
                            {
                              "name": "name",
                              "type": "STRING",
                              "required": true
                            },
                            {
                              "name": "url",
                              "type": "STRING",
                              "required": true
                            }
                          ]
                        }
                      ],
                      "displayName": "HEAD"
                    }
                  ],
                  "version": 1
                }
                """,
                objectMapper.writeValueAsString(new HttpClientTaskDescriptorHandler().getTaskDescriptor()),
                true);
    }
}
