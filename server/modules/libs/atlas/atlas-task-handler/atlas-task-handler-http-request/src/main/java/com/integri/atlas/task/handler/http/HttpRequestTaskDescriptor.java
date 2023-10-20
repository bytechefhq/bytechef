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

import static com.integri.atlas.engine.core.task.TaskDescriptor.task;
import static com.integri.atlas.engine.core.task.description.TaskAuthentication.authentication;
import static com.integri.atlas.engine.core.task.description.TaskAuthentication.credential;
import static com.integri.atlas.engine.core.task.description.TaskDescription.property;
import static com.integri.atlas.engine.core.task.description.TaskParameterValue.parameterValues;
import static com.integri.atlas.engine.core.task.description.TaskProperty.minValue;
import static com.integri.atlas.engine.core.task.description.TaskProperty.multipleValues;
import static com.integri.atlas.engine.core.task.description.TaskProperty.properties;
import static com.integri.atlas.engine.core.task.description.TaskProperty.show;
import static com.integri.atlas.engine.core.task.description.TaskPropertyOption.propertyGroup;
import static com.integri.atlas.engine.core.task.description.TaskPropertyOption.propertyOption;
import static com.integri.atlas.engine.core.task.description.TaskPropertyType.*;

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
                    property("authenticationType")
                        .displayName("Authentication Type")
                        .type(SELECT)
                        .propertyOptions(
                            propertyOption("basicAuth", "Basic Auth"),
                            propertyOption("digestAuth", "Digest Auth"),
                            propertyOption("headerAuth", "Header Auth"),
                            propertyOption("oAuth2", "OAuth2"),
                            propertyOption("", "None")
                        )
                )
                .credentials(
                    credential("httpBasicAuth").required(true).displayOption(show("authentication", "basicAuth")),
                    credential("httpDigestAuth").required(true).displayOption(show("authentication", "digestAuth")),
                    credential("oAuthApi").required(true).displayOption(show("authentication", "oAuth2"))
                )
        )
        .properties(
            // General

            property("requestMethod")
                .displayName("Request Method")
                .type(SELECT)
                .propertyOptions(
                    propertyOption("DELETE", "DELETE"),
                    propertyOption("GET", "GET"),
                    propertyOption("HEAD", "HEAD"),
                    propertyOption("PATCH", "PATCH"),
                    propertyOption("POST", "POST"),
                    propertyOption("PUT", "PUT")
                )
                .description("The request method to use.")
                .defaultValue("GET"),
            property("url")
                .displayName("URL")
                .type(STRING)
                .description("The URL to make the request to")
                .placeholder("http://example.com/index.html")
                .defaultValue("")
                .required(true),
            property("allowUnauthorizedCerts")
                .displayName("Allow Unauthorized Certs")
                .type(BOOLEAN)
                .description("Download the response even if SSL certificate validation is not possible.")
                .defaultValue(false),
            property("responseFormat")
                .displayName("Response Format")
                .type(SELECT)
                .propertyOptions(
                    propertyOption("File", "file"),
                    propertyOption("JSON", "json"),
                    propertyOption("String", "string")
                )
                .description("The format in which the data gets returned from the URL.")
                .defaultValue("json"),
            property("dataPropertyName")
                .displayName("Property Name")
                .type(STRING)
                .description("Name of the property to which to write the response data.")
                .defaultValue("data")
                .required(true)
                .displayOption(show("responseFormat", "json", "string")),
            property("dataPropertyName")
                .displayName("Binary Property")
                .type(STRING)
                .description("Name of the binary property to which to write the data of the read file.")
                .defaultValue("data")
                .required(true)
                .displayOption(show("responseFormat", "file")),
            property("statusPropertyName")
                .displayName("Status Name")
                .type(STRING)
                .description("Name of the property to which to write the response status.")
                .defaultValue("status"),
            property("options")
                .displayName("Options")
                .type(COLLECTION)
                .defaultValue("data")
                .placeholder("Add Option")
                .propertyOptions(
                    property("rawParameters")
                        .displayName("RAW Parameters")
                        .type(BOOLEAN)
                        .description(
                            "If the query and/or body parameters should be set via the key-value pair UI or RAW."
                        )
                        .defaultValue(false),
                    property("bodyContentType")
                        .displayName("Body Content Type")
                        .type(SELECT)
                        .description("Content-Type to use when sending body parameters.")
                        .propertyOptions(
                            propertyOption("RAW", "raw"),
                            propertyOption("Form-Data", "form-data"),
                            propertyOption("Form-Urlencoded", "form-urlencoded"),
                            propertyOption("Binary", "binary")
                        )
                        .defaultValue("raw")
                        .displayOption(show("requestMethod", "PATCH", "POST", "PUT")),
                    property("rawMimeType")
                        .displayName("Raw Mime Type")
                        .type(SELECT)
                        .description("Mime-Type to use when sending body as raw content.")
                        // from Postman
                        .propertyOptions(
                            propertyOption("JSON", "json"),
                            propertyOption("Text", "text"),
                            propertyOption("HTML", "html"),
                            propertyOption("JavaScript", "javascript"),
                            propertyOption("XML", "xml")
                        )
                        .defaultValue("json")
                        .displayOption(
                            show(
                                "bodyContentType",
                                parameterValues("raw"),
                                "requestMethod",
                                parameterValues("PATCH", "POST", "PUT")
                            )
                        ),
                    property("followAllRedirects")
                        .displayName("Follow All Redirects")
                        .type(BOOLEAN)
                        .description("Follow non-GET HTTP 3xx redirects.")
                        .defaultValue(false),
                    property("followRedirect")
                        .displayName("Follow GET Redirect")
                        .type(BOOLEAN)
                        .description("Follow GET HTTP 3xx redirects.")
                        .defaultValue(false),
                    property("ignoreResponseCode")
                        .displayName("Ignore Response Code")
                        .type(BOOLEAN)
                        .description("Succeeds also when status code is not 2xx.")
                        .defaultValue(false),
                    //                        property("bodyContentCustomMimeType")
                    //                            .displayName("MIME Type")
                    //                            .type(STRING)
                    //                            .description("Specify the mime type for raw/custom body type.")
                    //                            .defaultValue(false),
                    property("proxy")
                        .displayName("Proxy")
                        .type(STRING)
                        .description("HTTP proxy to use.")
                        .placeholder("http://myproxy:3128")
                        .defaultValue(""),
                    property("timeout")
                        .displayName("Timeout")
                        .type(NUMBER)
                        .description("Time in ms to wait for the server to send response before aborting the request.")
                        .defaultValue(1000)
                        .propertyTypeOption(minValue(1))
                ),
            // Query Parameters

            property("queryParametersRaw")
                .displayName("Query Parameters")
                .type(STRING)
                .description("Query parameters as RAW.")
                .defaultValue("")
                .displayOption(show("rawParameters", true)),
            property("queryParametersKeyValue")
                .displayName("Header Parameters")
                .type(COLLECTION)
                .description("Query parameters to send.")
                .defaultValue("")
                .placeholder("Add Parameter")
                .propertyTypeOption(multipleValues(true))
                .displayOption(show("rawParameters", parameterValues(false)))
                .propertyOptions(
                    propertyGroup(
                        "parameter",
                        "Parameter",
                        properties(
                            property("name")
                                .displayName("Name")
                                .type(STRING)
                                .description("Name of the parameter.")
                                .defaultValue(""),
                            property("value")
                                .displayName("Value")
                                .type(STRING)
                                .description("Name of the parameter.")
                                .defaultValue("")
                        )
                    )
                ),
            // Header Parameters

            property("headerParametersRaw")
                .displayName("Header Parameters")
                .type(STRING)
                .description("Header parameters as RAW.")
                .defaultValue("")
                .displayOption(show("rawParameters", true)),
            property("headerParametersKeyValue")
                .displayName("Header Parameters")
                .type(COLLECTION)
                .description("Header parameters to send.")
                .defaultValue("")
                .placeholder("Add Parameter")
                .propertyTypeOption(multipleValues(true))
                .displayOption(show("rawParameters", parameterValues(false)))
                .propertyOptions(
                    propertyGroup(
                        "parameter",
                        "Parameter",
                        properties(
                            property("name")
                                .displayName("Name")
                                .type(STRING)
                                .description("Name of the parameter.")
                                .defaultValue(""),
                            property("value")
                                .displayName("Value")
                                .type(STRING)
                                .description("Name of the parameter.")
                                .defaultValue("")
                        )
                    )
                ),
            // Body Content

            property("binaryPropertyName")
                .displayName("Binary Property")
                .type(STRING)
                .description("Name of the binary property which contains the data for the file to be uploaded.")
                .defaultValue("data")
                .required(true)
                .displayOption(
                    show(
                        "rawParameters",
                        parameterValues(true),
                        "bodyContentType",
                        parameterValues("binary"),
                        "requestMethod",
                        parameterValues("PATCH", "POST", "PUT")
                    )
                ),
            property("bodyParametersRaw")
                .displayName("Body Parameters")
                .type(STRING)
                .description("Body parameters as RAW.")
                .defaultValue("")
                .displayOption(
                    show(
                        "rawParameters",
                        parameterValues(true),
                        "bodyContentType",
                        parameterValues("raw", "form-data", "form-urlencoded"),
                        "requestMethod",
                        parameterValues("PATCH", "POST", "PUT")
                    )
                ),
            property("bodyParametersKeyValue")
                .displayName("Body Parameters")
                .type(COLLECTION)
                .description("Body parameters to send.")
                .defaultValue("")
                .placeholder("Add Parameter")
                .propertyTypeOption(multipleValues(true))
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
                .propertyOptions(
                    propertyGroup(
                        "parameter",
                        "Parameter",
                        properties(
                            property("name")
                                .displayName("Name")
                                .type(STRING)
                                .description("Name of the parameter.")
                                .defaultValue(""),
                            property("value")
                                .displayName("Value")
                                .type(STRING)
                                .description("Name of the parameter.")
                                .defaultValue("")
                        )
                    )
                )
        );

    @Override
    public TaskDescription getDescription() {
        return TASK_DESCRIPTION;
    }
}
