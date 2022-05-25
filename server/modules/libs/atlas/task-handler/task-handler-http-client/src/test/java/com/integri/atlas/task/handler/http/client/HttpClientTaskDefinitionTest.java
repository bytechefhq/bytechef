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
    public void testHttpClientTaskDefinition() throws JsonProcessingException {
        JSONAssert.assertEquals(
            """
            {
              "auth": {
                "options": [
                  {
                    "name": "API Key",
                    "value": "HTTP_API_KEY"
                  },
                  {
                    "name": "Bearer Token",
                    "value": "HTTP_BEARER_TOKEN"
                  },
                  {
                    "name": "Basic Auth",
                    "value": "HTTP_BASIC_AUTH"
                  },
                  {
                    "name": "Digest Auth",
                    "value": "HTTP_DIGEST_AUTH"
                  },
                  {
                    "name": "OAuth2",
                    "value": "OAUTH2"
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
                      "required": true,
                      "defaultValue": "",
                      "type": "STRING"
                    },
                    {
                      "description": "Download the response even if SSL certificate validation is not possible.",
                      "displayName": "Allow Unauthorized Certs",
                      "name": "allowUnauthorizedCerts",
                      "defaultValue": false,
                      "type": "BOOLEAN"
                    },
                    {
                      "description": "The format in which the data gets returned from the URL.",
                      "displayName": "Response Format",
                      "name": "responseFormat",
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
                      "defaultValue": "JSON",
                      "type": "STRING"
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
                      "defaultValue": [
                        ""
                      ],
                      "type": "ARRAY",
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
                              "defaultValue": "",
                              "type": "STRING"
                            },
                            {
                              "description": "The value of the parameter.",
                              "displayName": "Value",
                              "name": "value",
                              "defaultValue": "",
                              "type": "STRING"
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
                      "defaultValue": [
                        ""
                      ],
                      "type": "ARRAY",
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
                              "defaultValue": "",
                              "type": "STRING"
                            },
                            {
                              "description": "The value of the parameter.",
                              "displayName": "Value",
                              "name": "value",
                              "defaultValue": "",
                              "type": "STRING"
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
                      "required": true,
                      "defaultValue": "",
                      "type": "STRING"
                    },
                    {
                      "description": "Download the response even if SSL certificate validation is not possible.",
                      "displayName": "Allow Unauthorized Certs",
                      "name": "allowUnauthorizedCerts",
                      "defaultValue": false,
                      "type": "BOOLEAN"
                    },
                    {
                      "description": "The format in which the data gets returned from the URL.",
                      "displayName": "Response Format",
                      "name": "responseFormat",
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
                      "defaultValue": "JSON",
                      "type": "STRING"
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
                      "defaultValue": [
                        ""
                      ],
                      "type": "ARRAY",
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
                              "defaultValue": "",
                              "type": "STRING"
                            },
                            {
                              "description": "The value of the parameter.",
                              "displayName": "Value",
                              "name": "value",
                              "defaultValue": "",
                              "type": "STRING"
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
                      "defaultValue": [
                        ""
                      ],
                      "type": "ARRAY",
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
                              "defaultValue": "",
                              "type": "STRING"
                            },
                            {
                              "description": "The value of the parameter.",
                              "displayName": "Value",
                              "name": "value",
                              "defaultValue": "",
                              "type": "STRING"
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
                              "JSON",
                              "FORM_DATA",
                              "FORM_URLENCODED"
                            ]
                          }
                        }
                      },
                      "displayName": "Body Parameters",
                      "name": "bodyParameters",
                      "placeholder": "Add Parameter",
                      "defaultValue": [
                        ""
                      ],
                      "type": "ARRAY",
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
                              "defaultValue": "",
                              "type": "STRING"
                            },
                            {
                              "description": "The value of the parameter.",
                              "displayName": "Value",
                              "name": "value",
                              "defaultValue": "",
                              "type": "STRING"
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
                        "showWhen": {
                          "bodyContentType": {
                            "values": [
                              "BINARY"
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
                      "displayName": "Options",
                      "placeholder": "Add Option",
                      "options": [
                        {
                          "description": "Content-Type to use when sending body parameters.",
                          "displayName": "Body Content Type",
                          "name": "bodyContentType",
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
                            }
                          ],
                          "defaultValue": "JSON",
                          "type": "STRING"
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
                          "defaultValue": false,
                          "type": "BOOLEAN"
                        },
                        {
                          "description": "Follow non-GET HTTP 3xx redirects.",
                          "displayName": "Follow All Redirects",
                          "name": "followAllRedirects",
                          "defaultValue": false,
                          "type": "BOOLEAN"
                        },
                        {
                          "description": "Follow GET HTTP 3xx redirects.",
                          "displayName": "Follow GET Redirect",
                          "name": "followRedirect",
                          "defaultValue": false,
                          "type": "BOOLEAN"
                        },
                        {
                          "description": "Succeeds also when the status code is not 2xx.",
                          "displayName": "Ignore Response Code",
                          "name": "ignoreResponseCode",
                          "defaultValue": false,
                          "type": "BOOLEAN"
                        },
                        {
                          "description": "HTTP proxy to use.",
                          "displayName": "Proxy",
                          "name": "proxy",
                          "placeholder": "https://myproxy:3128",
                          "defaultValue": "",
                          "type": "STRING"
                        },
                        {
                          "description": "Time in ms to wait for the server to send a response before aborting the request.",
                          "displayName": "Timeout",
                          "name": "timeout",
                          "defaultValue": 1000,
                          "type": "INTEGER",
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
                      "required": true,
                      "defaultValue": "",
                      "type": "STRING"
                    },
                    {
                      "description": "Download the response even if SSL certificate validation is not possible.",
                      "displayName": "Allow Unauthorized Certs",
                      "name": "allowUnauthorizedCerts",
                      "defaultValue": false,
                      "type": "BOOLEAN"
                    },
                    {
                      "description": "The format in which the data gets returned from the URL.",
                      "displayName": "Response Format",
                      "name": "responseFormat",
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
                      "defaultValue": "JSON",
                      "type": "STRING"
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
                      "defaultValue": [
                        ""
                      ],
                      "type": "ARRAY",
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
                              "defaultValue": "",
                              "type": "STRING"
                            },
                            {
                              "description": "The value of the parameter.",
                              "displayName": "Value",
                              "name": "value",
                              "defaultValue": "",
                              "type": "STRING"
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
                      "defaultValue": [
                        ""
                      ],
                      "type": "ARRAY",
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
                              "defaultValue": "",
                              "type": "STRING"
                            },
                            {
                              "description": "The value of the parameter.",
                              "displayName": "Value",
                              "name": "value",
                              "defaultValue": "",
                              "type": "STRING"
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
                              "JSON",
                              "FORM_DATA",
                              "FORM_URLENCODED"
                            ]
                          }
                        }
                      },
                      "displayName": "Body Parameters",
                      "name": "bodyParameters",
                      "placeholder": "Add Parameter",
                      "defaultValue": [
                        ""
                      ],
                      "type": "ARRAY",
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
                              "defaultValue": "",
                              "type": "STRING"
                            },
                            {
                              "description": "The value of the parameter.",
                              "displayName": "Value",
                              "name": "value",
                              "defaultValue": "",
                              "type": "STRING"
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
                        "showWhen": {
                          "bodyContentType": {
                            "values": [
                              "BINARY"
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
                      "displayName": "Options",
                      "placeholder": "Add Option",
                      "options": [
                        {
                          "description": "Content-Type to use when sending body parameters.",
                          "displayName": "Body Content Type",
                          "name": "bodyContentType",
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
                            }
                          ],
                          "defaultValue": "JSON",
                          "type": "STRING"
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
                          "defaultValue": false,
                          "type": "BOOLEAN"
                        },
                        {
                          "description": "Follow non-GET HTTP 3xx redirects.",
                          "displayName": "Follow All Redirects",
                          "name": "followAllRedirects",
                          "defaultValue": false,
                          "type": "BOOLEAN"
                        },
                        {
                          "description": "Follow GET HTTP 3xx redirects.",
                          "displayName": "Follow GET Redirect",
                          "name": "followRedirect",
                          "defaultValue": false,
                          "type": "BOOLEAN"
                        },
                        {
                          "description": "Succeeds also when the status code is not 2xx.",
                          "displayName": "Ignore Response Code",
                          "name": "ignoreResponseCode",
                          "defaultValue": false,
                          "type": "BOOLEAN"
                        },
                        {
                          "description": "HTTP proxy to use.",
                          "displayName": "Proxy",
                          "name": "proxy",
                          "placeholder": "https://myproxy:3128",
                          "defaultValue": "",
                          "type": "STRING"
                        },
                        {
                          "description": "Time in ms to wait for the server to send a response before aborting the request.",
                          "displayName": "Timeout",
                          "name": "timeout",
                          "defaultValue": 1000,
                          "type": "INTEGER",
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
                      "required": true,
                      "defaultValue": "",
                      "type": "STRING"
                    },
                    {
                      "description": "Download the response even if SSL certificate validation is not possible.",
                      "displayName": "Allow Unauthorized Certs",
                      "name": "allowUnauthorizedCerts",
                      "defaultValue": false,
                      "type": "BOOLEAN"
                    },
                    {
                      "description": "The format in which the data gets returned from the URL.",
                      "displayName": "Response Format",
                      "name": "responseFormat",
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
                      "defaultValue": "JSON",
                      "type": "STRING"
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
                      "defaultValue": [
                        ""
                      ],
                      "type": "ARRAY",
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
                              "defaultValue": "",
                              "type": "STRING"
                            },
                            {
                              "description": "The value of the parameter.",
                              "displayName": "Value",
                              "name": "value",
                              "defaultValue": "",
                              "type": "STRING"
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
                      "defaultValue": [
                        ""
                      ],
                      "type": "ARRAY",
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
                              "defaultValue": "",
                              "type": "STRING"
                            },
                            {
                              "description": "The value of the parameter.",
                              "displayName": "Value",
                              "name": "value",
                              "defaultValue": "",
                              "type": "STRING"
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
                              "JSON",
                              "FORM_DATA",
                              "FORM_URLENCODED"
                            ]
                          }
                        }
                      },
                      "displayName": "Body Parameters",
                      "name": "bodyParameters",
                      "placeholder": "Add Parameter",
                      "defaultValue": [
                        ""
                      ],
                      "type": "ARRAY",
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
                              "defaultValue": "",
                              "type": "STRING"
                            },
                            {
                              "description": "The value of the parameter.",
                              "displayName": "Value",
                              "name": "value",
                              "defaultValue": "",
                              "type": "STRING"
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
                        "showWhen": {
                          "bodyContentType": {
                            "values": [
                              "BINARY"
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
                      "displayName": "Options",
                      "placeholder": "Add Option",
                      "options": [
                        {
                          "description": "Content-Type to use when sending body parameters.",
                          "displayName": "Body Content Type",
                          "name": "bodyContentType",
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
                            }
                          ],
                          "defaultValue": "JSON",
                          "type": "STRING"
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
                          "defaultValue": false,
                          "type": "BOOLEAN"
                        },
                        {
                          "description": "Follow non-GET HTTP 3xx redirects.",
                          "displayName": "Follow All Redirects",
                          "name": "followAllRedirects",
                          "defaultValue": false,
                          "type": "BOOLEAN"
                        },
                        {
                          "description": "Follow GET HTTP 3xx redirects.",
                          "displayName": "Follow GET Redirect",
                          "name": "followRedirect",
                          "defaultValue": false,
                          "type": "BOOLEAN"
                        },
                        {
                          "description": "Succeeds also when the status code is not 2xx.",
                          "displayName": "Ignore Response Code",
                          "name": "ignoreResponseCode",
                          "defaultValue": false,
                          "type": "BOOLEAN"
                        },
                        {
                          "description": "HTTP proxy to use.",
                          "displayName": "Proxy",
                          "name": "proxy",
                          "placeholder": "https://myproxy:3128",
                          "defaultValue": "",
                          "type": "STRING"
                        },
                        {
                          "description": "Time in ms to wait for the server to send a response before aborting the request.",
                          "displayName": "Timeout",
                          "name": "timeout",
                          "defaultValue": 1000,
                          "type": "INTEGER",
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
                      "required": true,
                      "defaultValue": "",
                      "type": "STRING"
                    },
                    {
                      "description": "Download the response even if SSL certificate validation is not possible.",
                      "displayName": "Allow Unauthorized Certs",
                      "name": "allowUnauthorizedCerts",
                      "defaultValue": false,
                      "type": "BOOLEAN"
                    },
                    {
                      "description": "The format in which the data gets returned from the URL.",
                      "displayName": "Response Format",
                      "name": "responseFormat",
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
                      "defaultValue": "JSON",
                      "type": "STRING"
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
                      "defaultValue": [
                        ""
                      ],
                      "type": "ARRAY",
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
                              "defaultValue": "",
                              "type": "STRING"
                            },
                            {
                              "description": "The value of the parameter.",
                              "displayName": "Value",
                              "name": "value",
                              "defaultValue": "",
                              "type": "STRING"
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
                      "defaultValue": [
                        ""
                      ],
                      "type": "ARRAY",
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
                              "defaultValue": "",
                              "type": "STRING"
                            },
                            {
                              "description": "The value of the parameter.",
                              "displayName": "Value",
                              "name": "value",
                              "defaultValue": "",
                              "type": "STRING"
                            }
                          ]
                        }
                      ]
                    },
                    {
                      "displayName": "Options",
                      "placeholder": "Add Option",
                      "options": [
                        {
                          "description": "Returns the full response data instead of only the body.",
                          "displayName": "Full Response",
                          "name": "fullResponse",
                          "defaultValue": false,
                          "type": "BOOLEAN"
                        },
                        {
                          "description": "Follow non-GET HTTP 3xx redirects.",
                          "displayName": "Follow All Redirects",
                          "name": "followAllRedirects",
                          "defaultValue": false,
                          "type": "BOOLEAN"
                        },
                        {
                          "description": "Follow GET HTTP 3xx redirects.",
                          "displayName": "Follow GET Redirect",
                          "name": "followRedirect",
                          "defaultValue": false,
                          "type": "BOOLEAN"
                        },
                        {
                          "description": "Succeeds also when the status code is not 2xx.",
                          "displayName": "Ignore Response Code",
                          "name": "ignoreResponseCode",
                          "defaultValue": false,
                          "type": "BOOLEAN"
                        },
                        {
                          "description": "HTTP proxy to use.",
                          "displayName": "Proxy",
                          "name": "proxy",
                          "placeholder": "https://myproxy:3128",
                          "defaultValue": "",
                          "type": "STRING"
                        },
                        {
                          "description": "Time in ms to wait for the server to send a response before aborting the request.",
                          "displayName": "Timeout",
                          "name": "timeout",
                          "defaultValue": 1000,
                          "type": "INTEGER",
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
                      "required": true,
                      "defaultValue": "",
                      "type": "STRING"
                    },
                    {
                      "description": "Download the response even if SSL certificate validation is not possible.",
                      "displayName": "Allow Unauthorized Certs",
                      "name": "allowUnauthorizedCerts",
                      "defaultValue": false,
                      "type": "BOOLEAN"
                    },
                    {
                      "description": "The format in which the data gets returned from the URL.",
                      "displayName": "Response Format",
                      "name": "responseFormat",
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
                      "defaultValue": "JSON",
                      "type": "STRING"
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
                      "defaultValue": [
                        ""
                      ],
                      "type": "ARRAY",
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
                              "defaultValue": "",
                              "type": "STRING"
                            },
                            {
                              "description": "The value of the parameter.",
                              "displayName": "Value",
                              "name": "value",
                              "defaultValue": "",
                              "type": "STRING"
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
                      "defaultValue": [
                        ""
                      ],
                      "type": "ARRAY",
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
                              "defaultValue": "",
                              "type": "STRING"
                            },
                            {
                              "description": "The value of the parameter.",
                              "displayName": "Value",
                              "name": "value",
                              "defaultValue": "",
                              "type": "STRING"
                            }
                          ]
                        }
                      ]
                    },
                    {
                      "displayName": "Options",
                      "placeholder": "Add Option",
                      "options": [
                        {
                          "description": "Returns the full response data instead of only the body.",
                          "displayName": "Full Response",
                          "name": "fullResponse",
                          "defaultValue": false,
                          "type": "BOOLEAN"
                        },
                        {
                          "description": "Follow non-GET HTTP 3xx redirects.",
                          "displayName": "Follow All Redirects",
                          "name": "followAllRedirects",
                          "defaultValue": false,
                          "type": "BOOLEAN"
                        },
                        {
                          "description": "Follow GET HTTP 3xx redirects.",
                          "displayName": "Follow GET Redirect",
                          "name": "followRedirect",
                          "defaultValue": false,
                          "type": "BOOLEAN"
                        },
                        {
                          "description": "Succeeds also when the status code is not 2xx.",
                          "displayName": "Ignore Response Code",
                          "name": "ignoreResponseCode",
                          "defaultValue": false,
                          "type": "BOOLEAN"
                        },
                        {
                          "description": "HTTP proxy to use.",
                          "displayName": "Proxy",
                          "name": "proxy",
                          "placeholder": "https://myproxy:3128",
                          "defaultValue": "",
                          "type": "STRING"
                        },
                        {
                          "description": "Time in ms to wait for the server to send a response before aborting the request.",
                          "displayName": "Timeout",
                          "name": "timeout",
                          "defaultValue": 1000,
                          "type": "INTEGER",
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
                  "displayName": "HEAD"
                }
              ],
              "version": 1
            }
                """,
            (JSONObject) JSONParser.parseJSON(
                objectMapper.writeValueAsString(new HttpClientTaskDefinitionHandler().getTaskDefinition())
            ),
            true
        );
    }
}
