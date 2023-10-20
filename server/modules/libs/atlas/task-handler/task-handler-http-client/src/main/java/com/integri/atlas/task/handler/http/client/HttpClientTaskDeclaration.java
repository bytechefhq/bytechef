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

import static com.integri.atlas.task.definition.dsl.TaskCredential.*;
import static com.integri.atlas.task.definition.dsl.TaskParameterValue.parameterValues;
import static com.integri.atlas.task.definition.dsl.TaskProperty.BOOLEAN_PROPERTY;
import static com.integri.atlas.task.definition.dsl.TaskProperty.COLLECTION_PROPERTY;
import static com.integri.atlas.task.definition.dsl.TaskProperty.FILE_ENTRY_PROPERTY;
import static com.integri.atlas.task.definition.dsl.TaskProperty.GROUP_PROPERTY;
import static com.integri.atlas.task.definition.dsl.TaskProperty.NUMBER_PROPERTY;
import static com.integri.atlas.task.definition.dsl.TaskProperty.OPTION_PROPERTY;
import static com.integri.atlas.task.definition.dsl.TaskProperty.STRING_PROPERTY;
import static com.integri.atlas.task.definition.dsl.TaskProperty.minValue;
import static com.integri.atlas.task.definition.dsl.TaskProperty.multipleValues;
import static com.integri.atlas.task.definition.dsl.TaskProperty.show;
import static com.integri.atlas.task.definition.dsl.TaskPropertyOption.option;

import com.integri.atlas.task.definition.TaskDeclaration;
import com.integri.atlas.task.definition.dsl.TaskSpecification;
import org.springframework.stereotype.Component;

@Component
public class HttpClientTaskDeclaration implements TaskDeclaration {

    public static final TaskSpecification TASK_SPECIFICATION = TaskSpecification
        .create("httpClient")
        .displayName("HTTP Client")
        .description("Makes an HTTP request and returns the response data")
        .credentials(
            credential("httpBasicAuth").required(true).displayOption(show("authenticationType", "BASIC_AUTH")),
            credential("httpDigestAuth").required(true).displayOption(show("authenticationType", "DIGEST_AUTH")),
            credential("httpHeaderAuth").required(true).displayOption(show("authenticationType", "HEADER_AUTH")),
            credential("oAuth2Auth").required(true).displayOption(show("authenticationType", "OAUTH2"))
        )
        .properties(
            //
            // General properties
            //

            OPTION_PROPERTY("authenticationType")
                .displayName("Authentication Type")
                .options(
                    option("Basic Auth", "BASIC_AUTH"),
                    option("Digest Auth", "DIGEST_AUTH"),
                    option("Header Auth", "HEADER_AUTH"),
                    option("Query Auth", "QUERY_AUTH"),
                    option("OAuth2", "OAUTH2"),
                    option("None", "")
                ),
            OPTION_PROPERTY("requestMethod")
                .displayName("Request Method")
                .options(
                    option("DELETE", "DELETE"),
                    option("GET", "GET"),
                    option("HEAD", "HEAD"),
                    option("PATCH", "PATCH"),
                    option("POST", "POST"),
                    option("PUT", "PUT")
                )
                .description("The request method to use.")
                .defaultValue("GET"),
            STRING_PROPERTY("url")
                .displayName("URL")
                .description("The URL to make the request to")
                .placeholder("http://example.com/index.html")
                .defaultValue("")
                .required(true),
            BOOLEAN_PROPERTY("allowUnauthorizedCerts")
                .displayName("Allow Unauthorized Certs")
                .description("Download the response even if SSL certificate validation is not possible.")
                .defaultValue(false),
            OPTION_PROPERTY("responseFormat")
                .displayName("Response Format")
                .description("The format in which the data gets returned from the URL.")
                .options(option("Binary", "BINARY"), option("JSON", "JSON"), option("String", "STRING"))
                .defaultValue("JSON"),
            COLLECTION_PROPERTY("options")
                .displayName("Options")
                .placeholder("Add Option")
                .options(
                    BOOLEAN_PROPERTY("rawParameters")
                        .displayName("RAW Parameters")
                        .description(
                            "If the query and/or body parameters should be set via the key-value pair UI or RAW."
                        )
                        .defaultValue(false),
                    OPTION_PROPERTY("bodyContentType")
                        .displayName("Body Content Type")
                        .description("Content-Type to use when sending body parameters.")
                        .displayOption(show("requestMethod", "PATCH", "POST", "PUT"))
                        .options(
                            option("JSON", "JSON"),
                            option("Form-Data", "FORM_DATA"),
                            option("Form-Urlencoded", "FORM_URLENCODED"),
                            option("Binary", "BINARY")
                        )
                        .defaultValue("JSON"),
                    BOOLEAN_PROPERTY("fullResponse")
                        .displayName("Full Response")
                        .description("Returns the full response data instead of only the body.")
                        .defaultValue(false),
                    BOOLEAN_PROPERTY("followAllRedirects")
                        .displayName("Follow All Redirects")
                        .description("Follow non-GET HTTP 3xx redirects.")
                        .defaultValue(false),
                    BOOLEAN_PROPERTY("followRedirect")
                        .displayName("Follow GET Redirect")
                        .description("Follow GET HTTP 3xx redirects.")
                        .defaultValue(false),
                    BOOLEAN_PROPERTY("ignoreResponseCode")
                        .displayName("Ignore Response Code")
                        .description("Succeeds also when the status code is not 2xx.")
                        .defaultValue(false),
                    STRING_PROPERTY("proxy")
                        .displayName("Proxy")
                        .description("HTTP proxy to use.")
                        .placeholder("http://myproxy:3128")
                        .defaultValue(""),
                    NUMBER_PROPERTY("timeout")
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

            STRING_PROPERTY("headerParametersRaw")
                .displayName("Header Parameters")
                .description("Header parameters to send as RAW.")
                .displayOption(show("rawParameters", true))
                .defaultValue(""),
            COLLECTION_PROPERTY("headerParametersKeyValue")
                .displayName("Header Parameters")
                .description("Header parameters to send.")
                .displayOption(show("rawParameters", parameterValues(false)))
                .defaultValue("")
                .placeholder("Add Parameter")
                .typeOption(multipleValues(true))
                .options(
                    GROUP_PROPERTY("parameter")
                        .displayName("Parameter")
                        .fields(
                            STRING_PROPERTY("name")
                                .displayName("Name")
                                .description("Name of the parameter.")
                                .defaultValue(""),
                            STRING_PROPERTY("value")
                                .displayName("Value")
                                .description("Value of the parameter.")
                                .defaultValue("")
                        )
                ),
            //
            // Query properties
            //

            STRING_PROPERTY("queryParametersRaw")
                .displayName("Query Parameters")
                .description("Query parameters as RAW.")
                .displayOption(show("rawParameters", true))
                .defaultValue(""),
            COLLECTION_PROPERTY("queryParametersKeyValue")
                .displayName("Header Parameters")
                .description("Query parameters to send.")
                .displayOption(show("rawParameters", parameterValues(false)))
                .defaultValue("")
                .placeholder("Add Parameter")
                .typeOption(multipleValues(true))
                .options(
                    GROUP_PROPERTY("parameter")
                        .displayName("Parameter")
                        .fields(
                            STRING_PROPERTY("name")
                                .displayName("Name")
                                .description("Name of the parameter.")
                                .defaultValue(""),
                            STRING_PROPERTY("value")
                                .displayName("Value")
                                .description("Value of the parameter.")
                                .defaultValue("")
                        )
                ),
            //
            // Body Content properties
            //

            STRING_PROPERTY("bodyParametersRaw")
                .displayName("Body Parameters")
                .description("Body parameters as RAW.")
                .displayOption(
                    show(
                        "rawParameters",
                        parameterValues(true),
                        "bodyContentType",
                        parameterValues("JSON", "FORM_DATA", "FORM_URLENCODED"),
                        "requestMethod",
                        parameterValues("PATCH", "POST", "PUT")
                    )
                )
                .defaultValue(""),
            COLLECTION_PROPERTY("bodyParametersKeyValue")
                .displayName("Body Parameters")
                .description("Body parameters to send.")
                .displayOption(
                    show(
                        "rawParameters",
                        parameterValues(false),
                        "bodyContentType",
                        parameterValues("JSON", "FORM_DATA", "FORM_URLENCODED"),
                        "requestMethod",
                        parameterValues("PATCH", "POST", "PUT")
                    )
                )
                .defaultValue("")
                .placeholder("Add Parameter")
                .typeOption(multipleValues(true))
                .options(
                    GROUP_PROPERTY("parameter")
                        .displayName("Parameter")
                        .fields(
                            STRING_PROPERTY("name")
                                .displayName("Name")
                                .description("Name of the parameter.")
                                .defaultValue(""),
                            STRING_PROPERTY("value")
                                .displayName("Value")
                                .description("Value of the parameter.")
                                .defaultValue("")
                        )
                ),
            FILE_ENTRY_PROPERTY("fileEntry")
                .displayName("File")
                .description("The object property which contains a reference to the file with data to upload.")
                .displayOption(
                    show(
                        "bodyContentType",
                        parameterValues("BINARY"),
                        "requestMethod",
                        parameterValues("PATCH", "POST", "PUT")
                    )
                )
        );

    @Override
    public TaskSpecification getSpecification() {
        return TASK_SPECIFICATION;
    }
}
