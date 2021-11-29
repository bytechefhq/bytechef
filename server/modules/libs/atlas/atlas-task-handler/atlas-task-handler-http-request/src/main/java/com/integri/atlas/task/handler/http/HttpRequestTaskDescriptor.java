/*
 * Copyright 2016-2018 the original author or authors.
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
 *
 * Modifications copyright (C) 2021 <your company/name>
 */

package com.integri.atlas.task.handler.http;

import static com.integri.atlas.engine.core.task.description.TaskAuthentication.authentication;
import static com.integri.atlas.engine.core.task.description.TaskAuthentication.credential;
import static com.integri.atlas.engine.core.task.description.TaskDescription.task;
import static com.integri.atlas.engine.core.task.description.TaskParameterValue.parameterValues;
import static com.integri.atlas.engine.core.task.description.TaskProperty.BOOLEAN_PROPERTY;
import static com.integri.atlas.engine.core.task.description.TaskProperty.COLLECTION_PROPERTY;
import static com.integri.atlas.engine.core.task.description.TaskProperty.GROUP_PROPERTY;
import static com.integri.atlas.engine.core.task.description.TaskProperty.NUMBER_PROPERTY;
import static com.integri.atlas.engine.core.task.description.TaskProperty.SELECT_PROPERTY;
import static com.integri.atlas.engine.core.task.description.TaskProperty.STRING_PROPERTY;
import static com.integri.atlas.engine.core.task.description.TaskProperty.minValue;
import static com.integri.atlas.engine.core.task.description.TaskProperty.multipleValues;
import static com.integri.atlas.engine.core.task.description.TaskProperty.show;
import static com.integri.atlas.engine.core.task.description.TaskPropertyOption.option;

import com.integri.atlas.engine.core.task.TaskDescriptor;
import com.integri.atlas.engine.core.task.description.TaskDescription;
import org.springframework.stereotype.Component;

@Component
public class HttpRequestTaskDescriptor implements TaskDescriptor {

    private static final TaskDescription TASK_DESCRIPTION = task("httpRequest")
        .displayName("HTTP Request")
        .description("Makes an HTTP request and returns the response data")
        .authentication(
            authentication()
                .properties(
                    SELECT_PROPERTY("authenticationType")
                        .displayName("Authentication Type")
                        .options(
                            option("basicAuth", "Basic Auth"),
                            option("digestAuth", "Digest Auth"),
                            option("headerAuth", "Header Auth"),
                            option("oAuth2", "OAuth2"),
                            option("", "None")
                        )
                )
                .credentials(
                    credential("httpBasicAuth").required(true).displayOption(show("authentication", "basicAuth")),
                    credential("httpDigestAuth").required(true).displayOption(show("authentication", "digestAuth")),
                    credential("oAuthApi").required(true).displayOption(show("authentication", "oAuth2"))
                )
        )
        .properties(
            //
            // General
            //

            SELECT_PROPERTY("requestMethod")
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
            SELECT_PROPERTY("responseFormat")
                .displayName("Response Format")
                .options(option("File", "file"), option("JSON", "json"), option("String", "string"))
                .description("The format in which the data gets returned from the URL.")
                .defaultValue("json"),
            STRING_PROPERTY("dataPropertyName")
                .displayName("Property Name")
                .displayOption(show("responseFormat", "json", "string"))
                .description("Name of the property to which to write the response data.")
                .defaultValue("data")
                .required(true),
            STRING_PROPERTY("dataPropertyName")
                .displayName("Binary Property")
                .displayOption(show("responseFormat", "file"))
                .description("Name of the binary property to which to write the data of the read file.")
                .defaultValue("data")
                .required(true),
            STRING_PROPERTY("statusPropertyName")
                .displayName("Status Name")
                .description("Name of the property to which to write the response status.")
                .defaultValue("status"),
            COLLECTION_PROPERTY("options")
                .displayName("Options")
                .defaultValue("data")
                .placeholder("Add Option")
                .options(
                    BOOLEAN_PROPERTY("rawParameters")
                        .displayName("RAW Parameters")
                        .description(
                            "If the query and/or body parameters should be set via the key-value pair UI or RAW."
                        )
                        .defaultValue(false),
                    SELECT_PROPERTY("bodyContentType")
                        .displayName("Body Content Type")
                        .displayOption(show("requestMethod", "PATCH", "POST", "PUT"))
                        .description("Content-Type to use when sending body parameters.")
                        .options(
                            option("RAW", "raw"),
                            option("Form-Data", "form-data"),
                            option("Form-Urlencoded", "form-urlencoded"),
                            option("Binary", "binary")
                        )
                        .defaultValue("raw"),
                    SELECT_PROPERTY("rawMimeType")
                        .displayName("Raw Mime Type")
                        .displayOption(
                            show(
                                "bodyContentType",
                                parameterValues("raw"),
                                "requestMethod",
                                parameterValues("PATCH", "POST", "PUT")
                            )
                        )
                        .description("Mime-Type to use when sending body as raw content.")
                        .options(
                            option("JSON", "json"),
                            option("Text", "text"),
                            option("HTML", "html"),
                            option("JavaScript", "javascript"),
                            option("XML", "xml")
                        )
                        .defaultValue("json"),
                    BOOLEAN_PROPERTY("fullResponse")
                        .displayName("Full Response")
                        .description("Returns the full reponse data instead of only the body.")
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
                        .description("Succeeds also when status code is not 2xx.")
                        .defaultValue(false),
                    STRING_PROPERTY("proxy")
                        .displayName("Proxy")
                        .description("HTTP proxy to use.")
                        .placeholder("http://myproxy:3128")
                        .defaultValue(""),
                    NUMBER_PROPERTY("timeout")
                        .displayName("Timeout")
                        .description("Time in ms to wait for the server to send response before aborting the request.")
                        .defaultValue(1000)
                        .typeOption(minValue(1))
                ),
            //
            // Header Parameters
            //

            STRING_PROPERTY("headerParametersRaw")
                .displayName("Header Parameters")
                .displayOption(show("rawParameters", true))
                .description("Header parameters as RAW.")
                .defaultValue(""),
            COLLECTION_PROPERTY("headerParametersKeyValue")
                .displayName("Header Parameters")
                .displayOption(show("rawParameters", parameterValues(false)))
                .description("Header parameters to send.")
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
                                .description("Name of the parameter.")
                                .defaultValue("")
                        )
                ),
            //
            // Query Parameters
            //

            STRING_PROPERTY("queryParametersRaw")
                .displayName("Query Parameters")
                .displayOption(show("rawParameters", true))
                .description("Query parameters as RAW.")
                .defaultValue(""),
            COLLECTION_PROPERTY("queryParametersKeyValue")
                .displayName("Header Parameters")
                .displayOption(show("rawParameters", parameterValues(false)))
                .description("Query parameters to send.")
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
                                .description("Name of the parameter.")
                                .defaultValue("")
                        )
                ),
            //
            // Body Content
            //

            STRING_PROPERTY("binaryPropertyName")
                .displayName("Binary Property")
                .displayOption(
                    show(
                        "rawParameters",
                        parameterValues(true),
                        "bodyContentType",
                        parameterValues("binary"),
                        "requestMethod",
                        parameterValues("PATCH", "POST", "PUT")
                    )
                )
                .description("Name of the binary property which contains the data for the file to be uploaded.")
                .defaultValue("data")
                .required(true),
            STRING_PROPERTY("bodyParametersRaw")
                .displayName("Body Parameters")
                .displayOption(
                    show(
                        "rawParameters",
                        parameterValues(true),
                        "bodyContentType",
                        parameterValues("raw", "form-data", "form-urlencoded"),
                        "requestMethod",
                        parameterValues("PATCH", "POST", "PUT")
                    )
                )
                .description("Body parameters as RAW.")
                .defaultValue(""),
            COLLECTION_PROPERTY("bodyParametersKeyValue")
                .displayName("Body Parameters")
                .displayOption(
                    show(
                        "rawParameters",
                        parameterValues(false),
                        "bodyContentType",
                        parameterValues("raw", "form-data", "form-urlencoded"),
                        "rawMimeType",
                        parameterValues("json"),
                        "requestMethod",
                        parameterValues("PATCH", "POST", "PUT")
                    )
                )
                .description("Body parameters to send.")
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
                                .description("Name of the parameter.")
                                .defaultValue("")
                        )
                )
        );

    @Override
    public TaskDescription getDescription() {
        return TASK_DESCRIPTION;
    }
}
