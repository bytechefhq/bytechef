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

import static com.integri.atlas.task.definition.dsl.TaskCredential.credential;
import static com.integri.atlas.task.definition.dsl.TaskParameterValue.parameterValues;
import static com.integri.atlas.task.definition.dsl.TaskProperty.BOOLEAN_PROPERTY;
import static com.integri.atlas.task.definition.dsl.TaskProperty.COLLECTION_PROPERTY;
import static com.integri.atlas.task.definition.dsl.TaskProperty.GROUP_PROPERTY;
import static com.integri.atlas.task.definition.dsl.TaskProperty.INTEGER_PROPERTY;
import static com.integri.atlas.task.definition.dsl.TaskProperty.JSON_PROPERTY;
import static com.integri.atlas.task.definition.dsl.TaskProperty.SELECT_PROPERTY;
import static com.integri.atlas.task.definition.dsl.TaskProperty.STRING_PROPERTY;
import static com.integri.atlas.task.definition.dsl.TaskProperty.minValue;
import static com.integri.atlas.task.definition.dsl.TaskProperty.multipleValues;
import static com.integri.atlas.task.definition.dsl.TaskProperty.show;
import static com.integri.atlas.task.definition.dsl.TaskPropertyOption.option;
import static com.integri.atlas.task.definition.dsl.TaskSpecification.create;
import static com.integri.atlas.task.definition.dsl.TaskSpecification.credentials;
import static com.integri.atlas.task.handler.http.client.HttpClientTaskConstants.AuthenticationType;
import static com.integri.atlas.task.handler.http.client.HttpClientTaskConstants.BodyContentType;
import static com.integri.atlas.task.handler.http.client.HttpClientTaskConstants.PROPERTY_ALLOW_UNAUTHORIZED_CERTS;
import static com.integri.atlas.task.handler.http.client.HttpClientTaskConstants.PROPERTY_AUTHENTICATION_TYPE;
import static com.integri.atlas.task.handler.http.client.HttpClientTaskConstants.PROPERTY_BODY_CONTENT_TYPE;
import static com.integri.atlas.task.handler.http.client.HttpClientTaskConstants.PROPERTY_BODY_PARAMETERS;
import static com.integri.atlas.task.handler.http.client.HttpClientTaskConstants.PROPERTY_FILE_ENTRY;
import static com.integri.atlas.task.handler.http.client.HttpClientTaskConstants.PROPERTY_FOLLOW_ALL_REDIRECTS;
import static com.integri.atlas.task.handler.http.client.HttpClientTaskConstants.PROPERTY_FOLLOW_REDIRECT;
import static com.integri.atlas.task.handler.http.client.HttpClientTaskConstants.PROPERTY_FULL_RESPONSE;
import static com.integri.atlas.task.handler.http.client.HttpClientTaskConstants.PROPERTY_HEADER_PARAMETERS;
import static com.integri.atlas.task.handler.http.client.HttpClientTaskConstants.PROPERTY_HTTP_BASIC_AUTH;
import static com.integri.atlas.task.handler.http.client.HttpClientTaskConstants.PROPERTY_HTTP_DIGEST_AUTH;
import static com.integri.atlas.task.handler.http.client.HttpClientTaskConstants.PROPERTY_HTTP_HEADER_AUTH;
import static com.integri.atlas.task.handler.http.client.HttpClientTaskConstants.PROPERTY_HTTP_QUERY_AUTH;
import static com.integri.atlas.task.handler.http.client.HttpClientTaskConstants.PROPERTY_IGNORE_RESPONSE_CODE;
import static com.integri.atlas.task.handler.http.client.HttpClientTaskConstants.PROPERTY_MIME_TYPE;
import static com.integri.atlas.task.handler.http.client.HttpClientTaskConstants.PROPERTY_NAME;
import static com.integri.atlas.task.handler.http.client.HttpClientTaskConstants.PROPERTY_O_AUTH_2_AUTH;
import static com.integri.atlas.task.handler.http.client.HttpClientTaskConstants.PROPERTY_PARAMETER;
import static com.integri.atlas.task.handler.http.client.HttpClientTaskConstants.PROPERTY_PROPERTY_URI;
import static com.integri.atlas.task.handler.http.client.HttpClientTaskConstants.PROPERTY_PROXY;
import static com.integri.atlas.task.handler.http.client.HttpClientTaskConstants.PROPERTY_QUERY_PARAMETERS;
import static com.integri.atlas.task.handler.http.client.HttpClientTaskConstants.PROPERTY_RAW_PARAMETERS;
import static com.integri.atlas.task.handler.http.client.HttpClientTaskConstants.PROPERTY_REQUEST_METHOD;
import static com.integri.atlas.task.handler.http.client.HttpClientTaskConstants.PROPERTY_RESPONSE_FILE_NAME;
import static com.integri.atlas.task.handler.http.client.HttpClientTaskConstants.PROPERTY_RESPONSE_FORMAT;
import static com.integri.atlas.task.handler.http.client.HttpClientTaskConstants.PROPERTY_TIMEOUT;
import static com.integri.atlas.task.handler.http.client.HttpClientTaskConstants.PROPERTY_VALUE;
import static com.integri.atlas.task.handler.http.client.HttpClientTaskConstants.RequestMethod;
import static com.integri.atlas.task.handler.http.client.HttpClientTaskConstants.ResponseFormat;
import static com.integri.atlas.task.handler.http.client.HttpClientTaskConstants.TASK_HTTP_CLIENT;

import com.integri.atlas.task.definition.TaskDeclaration;
import com.integri.atlas.task.definition.dsl.TaskSpecification;
import org.springframework.stereotype.Component;

@Component
public class HttpClientTaskDeclaration implements TaskDeclaration {

    public static final TaskSpecification TASK_SPECIFICATION = TaskSpecification
        .create(TASK_HTTP_CLIENT)
        .displayName("HTTP Client")
        .description("Makes an HTTP request and returns the response data.")
        .credentials(
            credential(PROPERTY_HTTP_BASIC_AUTH)
                .required(true)
                .displayOption(show(PROPERTY_AUTHENTICATION_TYPE, AuthenticationType.BASIC.name())),
            credential(PROPERTY_HTTP_DIGEST_AUTH)
                .required(true)
                .displayOption(show(PROPERTY_AUTHENTICATION_TYPE, AuthenticationType.DIGEST.name())),
            credential(PROPERTY_HTTP_HEADER_AUTH)
                .required(true)
                .displayOption(show(PROPERTY_AUTHENTICATION_TYPE, AuthenticationType.HEADER.name())),
            credential(PROPERTY_O_AUTH_2_AUTH)
                .required(true)
                .displayOption(show(PROPERTY_AUTHENTICATION_TYPE, AuthenticationType.OAUTH_2.name()))
        )
        .properties(
            //
            // General properties
            //

            SELECT_PROPERTY(PROPERTY_AUTHENTICATION_TYPE)
                .displayName("Authentication Type")
                .options(
                    option("Basic Auth", AuthenticationType.BASIC.name()),
                    option("Digest Auth", AuthenticationType.DIGEST.name()),
                    option("Header Auth", AuthenticationType.HEADER.name()),
                    option("Query Auth", AuthenticationType.QUERY.name()),
                    option("OAuth2", AuthenticationType.OAUTH_2.name()),
                    option("None", "")
                ),
            SELECT_PROPERTY(PROPERTY_REQUEST_METHOD)
                .displayName("Request Method")
                .options(
                    option("DELETE", RequestMethod.DELETE.name()),
                    option("GET", RequestMethod.GET.name()),
                    option("HEAD", RequestMethod.HEAD.name()),
                    option("PATCH", RequestMethod.PATCH.name()),
                    option("POST", RequestMethod.POST.name()),
                    option("PUT", RequestMethod.PUT.name())
                )
                .description("The request method to use.")
                .defaultValue(RequestMethod.GET.name()),
            STRING_PROPERTY(PROPERTY_PROPERTY_URI)
                .displayName("URI")
                .description("The URI to make the request to")
                .placeholder("https://example.com/index.html")
                .defaultValue("")
                .required(true),
            BOOLEAN_PROPERTY(PROPERTY_ALLOW_UNAUTHORIZED_CERTS)
                .displayName("Allow Unauthorized Certs")
                .description("Download the response even if SSL certificate validation is not possible.")
                .defaultValue(false),
            SELECT_PROPERTY(PROPERTY_RESPONSE_FORMAT)
                .displayName("Response Format")
                .description("The format in which the data gets returned from the URL.")
                .options(
                    option(
                        "JSON",
                        ResponseFormat.JSON.name(),
                        "The response is automatically converted to object/array."
                    ),
                    option(
                        "XML",
                        ResponseFormat.XML.name(),
                        "The response is automatically converted to object/array."
                    ),
                    option("Text", ResponseFormat.TEXT.name(), "The response is returned as a text."),
                    option("File", ResponseFormat.FILE.name(), "The response is returned as a file object.")
                )
                .defaultValue(ResponseFormat.JSON.name()),
            STRING_PROPERTY(PROPERTY_RESPONSE_FILE_NAME)
                .displayName("Response File Name")
                .description("The name of the file if the response is returned as a file object.")
                .displayOption(show(PROPERTY_RESPONSE_FORMAT, parameterValues("FILE")))
                .defaultValue(""),
            BOOLEAN_PROPERTY(PROPERTY_RAW_PARAMETERS)
                .displayName("RAW Parameters")
                .description(
                    "If the header, query and/or body parameters should be set via the key-value pair in UI or as an object/JSON string based)."
                )
                .defaultValue(false),
            COLLECTION_PROPERTY("options")
                .displayName("Options")
                .placeholder("Add Option")
                .options(
                    SELECT_PROPERTY(PROPERTY_BODY_CONTENT_TYPE)
                        .displayName("Body Content Type")
                        .description("Content-Type to use when sending body parameters.")
                        .displayOption(show(PROPERTY_REQUEST_METHOD, "PATCH", "POST", "PUT"))
                        .options(
                            option("JSON", BodyContentType.JSON.name()),
                            option("Raw", BodyContentType.RAW.name()),
                            option("Form-Data", BodyContentType.FORM_DATA.name()),
                            option("Form-Urlencoded", BodyContentType.FORM_URLENCODED.name()),
                            option("Binary", BodyContentType.BINARY.name())
                        )
                        .defaultValue("JSON"),
                    STRING_PROPERTY(PROPERTY_MIME_TYPE)
                        .displayName("Mime Type")
                        .description("Mime-Type to use when sending raw body content.")
                        .displayOption(
                            show(
                                "requestMethod",
                                parameterValues(
                                    RequestMethod.PATCH.name(),
                                    RequestMethod.POST.name(),
                                    RequestMethod.PUT.name()
                                ),
                                "bodyContentType",
                                parameterValues(BodyContentType.RAW.name())
                            )
                        )
                        .placeholder("text/xml"),
                    BOOLEAN_PROPERTY(PROPERTY_FULL_RESPONSE)
                        .displayName("Full Response")
                        .description("Returns the full response data instead of only the body.")
                        .defaultValue(false),
                    BOOLEAN_PROPERTY(PROPERTY_FOLLOW_ALL_REDIRECTS)
                        .displayName("Follow All Redirects")
                        .description("Follow non-GET HTTP 3xx redirects.")
                        .defaultValue(false),
                    BOOLEAN_PROPERTY(PROPERTY_FOLLOW_REDIRECT)
                        .displayName("Follow GET Redirect")
                        .description("Follow GET HTTP 3xx redirects.")
                        .defaultValue(false),
                    BOOLEAN_PROPERTY(PROPERTY_IGNORE_RESPONSE_CODE)
                        .displayName("Ignore Response Code")
                        .description("Succeeds also when the status code is not 2xx.")
                        .defaultValue(false),
                    STRING_PROPERTY(PROPERTY_PROXY)
                        .displayName("Proxy")
                        .description("HTTP proxy to use.")
                        .placeholder("https://myproxy:3128")
                        .defaultValue(""),
                    INTEGER_PROPERTY(PROPERTY_TIMEOUT)
                        .displayName("Timeout")
                        .description(
                            "Time in ms to wait for the server to send a response before aborting the request."
                        )
                        .defaultValue(1000)
                        .typeOption(minValue(1))
                ),
            //
            // Header properties
            //

            JSON_PROPERTY(PROPERTY_HEADER_PARAMETERS)
                .displayName("Header Parameters")
                .description("Header parameters to send as an object/JSON string.")
                .displayOption(show(PROPERTY_RAW_PARAMETERS, true)),
            COLLECTION_PROPERTY(PROPERTY_HEADER_PARAMETERS)
                .displayName("Header Parameters")
                .description("Header parameters to send.")
                .displayOption(show("rawParameters", parameterValues(false)))
                .defaultValue("")
                .placeholder("Add Parameter")
                .typeOption(multipleValues(true))
                .options(
                    GROUP_PROPERTY(PROPERTY_PARAMETER)
                        .displayName("Parameter")
                        .groupProperties(
                            STRING_PROPERTY(PROPERTY_NAME)
                                .displayName("Name")
                                .description("Name of the parameter.")
                                .defaultValue(""),
                            STRING_PROPERTY(PROPERTY_VALUE)
                                .displayName("Value")
                                .description("Value of the parameter.")
                                .defaultValue("")
                        )
                ),
            //
            // Query properties
            //

            JSON_PROPERTY(PROPERTY_QUERY_PARAMETERS)
                .displayName("Query Parameters")
                .description("Query parameters to send as an object/JSON string.")
                .displayOption(show("rawParameters", true)),
            COLLECTION_PROPERTY(PROPERTY_QUERY_PARAMETERS)
                .displayName("Query Parameters")
                .description("Query parameters to send.")
                .displayOption(show("rawParameters", parameterValues(false)))
                .defaultValue("")
                .placeholder("Add Parameter")
                .typeOption(multipleValues(true))
                .options(
                    GROUP_PROPERTY(PROPERTY_PARAMETER)
                        .displayName("Parameter")
                        .groupProperties(
                            STRING_PROPERTY(PROPERTY_NAME)
                                .displayName("Name")
                                .description("Name of the parameter.")
                                .defaultValue(""),
                            STRING_PROPERTY(PROPERTY_VALUE)
                                .displayName("Value")
                                .description("Value of the parameter.")
                                .defaultValue("")
                        )
                ),
            //
            // Body Content properties
            //

            JSON_PROPERTY(PROPERTY_BODY_PARAMETERS)
                .displayName("Body Parameters")
                .description("Body parameters to send as an object/JSON string.")
                .displayOption(
                    show(
                        PROPERTY_RAW_PARAMETERS,
                        parameterValues(true),
                        PROPERTY_BODY_CONTENT_TYPE,
                        parameterValues(
                            BodyContentType.JSON.name(),
                            BodyContentType.FORM_DATA.name(),
                            BodyContentType.FORM_URLENCODED.name(),
                            BodyContentType.RAW.name()
                        ),
                        PROPERTY_REQUEST_METHOD,
                        parameterValues(RequestMethod.PATCH.name(), RequestMethod.POST.name(), RequestMethod.PUT.name())
                    )
                ),
            COLLECTION_PROPERTY(PROPERTY_BODY_PARAMETERS)
                .displayName("Body Parameters")
                .description("Body parameters to send.")
                .displayOption(
                    show(
                        PROPERTY_RAW_PARAMETERS,
                        parameterValues(false),
                        PROPERTY_BODY_CONTENT_TYPE,
                        parameterValues(
                            BodyContentType.JSON.name(),
                            BodyContentType.FORM_DATA.name(),
                            BodyContentType.FORM_URLENCODED.name(),
                            BodyContentType.RAW.name()
                        ),
                        PROPERTY_REQUEST_METHOD,
                        parameterValues(RequestMethod.PATCH.name(), RequestMethod.POST.name(), RequestMethod.PUT.name())
                    )
                )
                .defaultValue("")
                .placeholder("Add Parameter")
                .typeOption(multipleValues(true))
                .options(
                    GROUP_PROPERTY(PROPERTY_PARAMETER)
                        .displayName("Parameter")
                        .groupProperties(
                            STRING_PROPERTY(PROPERTY_NAME)
                                .displayName("Name")
                                .description("Name of the parameter.")
                                .defaultValue(""),
                            STRING_PROPERTY(PROPERTY_VALUE)
                                .displayName("Value")
                                .description("Value of the parameter.")
                                .defaultValue("")
                        )
                ),
            JSON_PROPERTY(PROPERTY_FILE_ENTRY)
                .displayName("File")
                .description("The object property which contains a reference to the file with data to upload.")
                .displayOption(
                    show(
                        PROPERTY_BODY_CONTENT_TYPE,
                        parameterValues(BodyContentType.BINARY.name()),
                        PROPERTY_REQUEST_METHOD,
                        parameterValues(RequestMethod.PATCH.name(), RequestMethod.POST.name(), RequestMethod.PUT.name())
                    )
                )
        );

    @Override
    public TaskSpecification getSpecification() {
        return TASK_SPECIFICATION;
    }
}
