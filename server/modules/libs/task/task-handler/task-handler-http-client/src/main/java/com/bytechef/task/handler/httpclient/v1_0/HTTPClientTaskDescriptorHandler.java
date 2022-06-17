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

import static com.bytechef.hermes.descriptor.domain.DSL.ANY_PROPERTY;
import static com.bytechef.hermes.descriptor.domain.DSL.ARRAY_PROPERTY;
import static com.bytechef.hermes.descriptor.domain.DSL.BOOLEAN_PROPERTY;
import static com.bytechef.hermes.descriptor.domain.DSL.FILE_ENTRY_PROPERTY;
import static com.bytechef.hermes.descriptor.domain.DSL.INTEGER_PROPERTY;
import static com.bytechef.hermes.descriptor.domain.DSL.OBJECT_PROPERTY;
import static com.bytechef.hermes.descriptor.domain.DSL.OPERATION;
import static com.bytechef.hermes.descriptor.domain.DSL.STRING_PROPERTY;
import static com.bytechef.hermes.descriptor.domain.DSL.createTaskDescriptor;
import static com.bytechef.hermes.descriptor.domain.DSL.option;
import static com.bytechef.hermes.descriptor.domain.DSL.showWhen;
import static com.bytechef.task.handler.httpclient.HTTPClientTaskConstants.*;
import static com.bytechef.task.handler.httpclient.HTTPClientTaskConstants.ALLOW_UNAUTHORIZED_CERTS;
import static com.bytechef.task.handler.httpclient.HTTPClientTaskConstants.AuthType.API_KEY;
import static com.bytechef.task.handler.httpclient.HTTPClientTaskConstants.AuthType.BASIC_AUTH;
import static com.bytechef.task.handler.httpclient.HTTPClientTaskConstants.AuthType.BEARER_TOKEN;
import static com.bytechef.task.handler.httpclient.HTTPClientTaskConstants.AuthType.DIGEST_AUTH;
import static com.bytechef.task.handler.httpclient.HTTPClientTaskConstants.AuthType.OAUTH2;
import static com.bytechef.task.handler.httpclient.HTTPClientTaskConstants.BODY_CONTENT_TYPE;
import static com.bytechef.task.handler.httpclient.HTTPClientTaskConstants.BODY_PARAMETERS;
import static com.bytechef.task.handler.httpclient.HTTPClientTaskConstants.BodyContentType;
import static com.bytechef.task.handler.httpclient.HTTPClientTaskConstants.FILE_ENTRY;
import static com.bytechef.task.handler.httpclient.HTTPClientTaskConstants.FOLLOW_ALL_REDIRECTS;
import static com.bytechef.task.handler.httpclient.HTTPClientTaskConstants.FOLLOW_REDIRECT;
import static com.bytechef.task.handler.httpclient.HTTPClientTaskConstants.FULL_RESPONSE;
import static com.bytechef.task.handler.httpclient.HTTPClientTaskConstants.HEADER_PARAMETERS;
import static com.bytechef.task.handler.httpclient.HTTPClientTaskConstants.HTTP_CLIENT;
import static com.bytechef.task.handler.httpclient.HTTPClientTaskConstants.IGNORE_RESPONSE_CODE;
import static com.bytechef.task.handler.httpclient.HTTPClientTaskConstants.KEY;
import static com.bytechef.task.handler.httpclient.HTTPClientTaskConstants.MIME_TYPE;
import static com.bytechef.task.handler.httpclient.HTTPClientTaskConstants.PARAMETER;
import static com.bytechef.task.handler.httpclient.HTTPClientTaskConstants.PROPERTY_QUERY_PARAMETERS;
import static com.bytechef.task.handler.httpclient.HTTPClientTaskConstants.PROXY;
import static com.bytechef.task.handler.httpclient.HTTPClientTaskConstants.RESPONSE_FILE_NAME;
import static com.bytechef.task.handler.httpclient.HTTPClientTaskConstants.RESPONSE_FORMAT;
import static com.bytechef.task.handler.httpclient.HTTPClientTaskConstants.ResponseFormat;
import static com.bytechef.task.handler.httpclient.HTTPClientTaskConstants.TIMEOUT;
import static com.bytechef.task.handler.httpclient.HTTPClientTaskConstants.URI;
import static com.bytechef.task.handler.httpclient.HTTPClientTaskConstants.VALUE;

import com.bytechef.hermes.descriptor.domain.DSL;
import com.bytechef.hermes.descriptor.domain.TaskDescriptor;
import com.bytechef.hermes.descriptor.domain.TaskProperty;
import com.bytechef.hermes.descriptor.handler.TaskDescriptorHandler;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class HTTPClientTaskDescriptorHandler implements TaskDescriptorHandler {

    private static final TaskProperty[] COMMON_PROPERTIES = {
        //
        // General properties
        //
        STRING_PROPERTY(URI)
                .displayName("URI")
                .description("The URI to make the request to")
                .placeholder("https://example.com/index.html")
                .defaultValue("")
                .required(true),
        BOOLEAN_PROPERTY(ALLOW_UNAUTHORIZED_CERTS)
                .displayName("Allow Unauthorized Certs")
                .description("Download the response even if SSL certificate validation is not possible.")
                .defaultValue(false),
        STRING_PROPERTY(RESPONSE_FORMAT)
                .displayName("Response Format")
                .description("The format in which the data gets returned from the URL.")
                .options(
                        option(
                                "JSON",
                                ResponseFormat.JSON.name(),
                                "The response is automatically converted to object/array."),
                        option(
                                "XML",
                                ResponseFormat.XML.name(),
                                "The response is automatically converted to object/array."),
                        option("Text", ResponseFormat.TEXT.name(), "The response is returned as a text."),
                        option("File", ResponseFormat.FILE.name(), "The response is returned as a file object."))
                .defaultValue(ResponseFormat.JSON.name()),
        STRING_PROPERTY(RESPONSE_FILE_NAME)
                .displayName("Response File Name")
                .description("The name of the file if the response is returned as a file object.")
                .displayOption(showWhen(RESPONSE_FORMAT).in(ResponseFormat.FILE.name())),
        //
        // Header properties
        //

        ARRAY_PROPERTY(HEADER_PARAMETERS)
                .displayName("Header Parameters")
                .description("Header parameters to send.")
                .defaultValue("")
                .placeholder("Add Parameter")
                .items(OBJECT_PROPERTY(PARAMETER)
                        .displayName("Parameter")
                        .properties(
                                STRING_PROPERTY(KEY)
                                        .displayName("Key")
                                        .description("The key of the parameter.")
                                        .defaultValue(""),
                                STRING_PROPERTY(VALUE)
                                        .displayName("Value")
                                        .description("The value of the parameter.")
                                        .defaultValue(""))),
        //
        // Query parameters properties
        //

        ARRAY_PROPERTY(PROPERTY_QUERY_PARAMETERS)
                .displayName("Query Parameters")
                .description("Query parameters to send.")
                .defaultValue("")
                .placeholder("Add Parameter")
                .items(OBJECT_PROPERTY(PARAMETER)
                        .displayName("Parameter")
                        .properties(
                                STRING_PROPERTY(KEY)
                                        .displayName("Key")
                                        .description("The key of the parameter.")
                                        .defaultValue(""),
                                STRING_PROPERTY(VALUE)
                                        .displayName("Value")
                                        .description("The value of the parameter.")
                                        .defaultValue(""))),
    };

    private static final TaskProperty[] BODY_CONTENT_PROPERTIES = {
        ARRAY_PROPERTY(BODY_PARAMETERS)
                .displayName("Body Parameters")
                .description("Body parameters to send.")
                .displayOption(showWhen(BODY_CONTENT_TYPE)
                        .in(
                                BodyContentType.JSON.name(),
                                BodyContentType.FORM_DATA.name(),
                                BodyContentType.FORM_URLENCODED.name()))
                .defaultValue("")
                .placeholder("Add Parameter")
                .items(OBJECT_PROPERTY(PARAMETER)
                        .displayName("Parameter")
                        .properties(
                                STRING_PROPERTY(KEY)
                                        .displayName("Key")
                                        .description("The key of the parameter.")
                                        .defaultValue(""),
                                STRING_PROPERTY(VALUE)
                                        .displayName("Value")
                                        .description("The value of the parameter.")
                                        .defaultValue(""))),
        STRING_PROPERTY(BODY_PARAMETERS)
                .displayName("Raw")
                .description("The raw text to send.")
                .displayOption(showWhen(BODY_CONTENT_TYPE).eq(BodyContentType.RAW.name())),
        FILE_ENTRY_PROPERTY(FILE_ENTRY)
                .displayName("File")
                .description("The object property which contains a reference to the file with data to upload.")
                .displayOption(showWhen(BODY_CONTENT_TYPE).eq(BodyContentType.BINARY.name())),
    };

    private static final TaskProperty[] OUTPUTS_PROPERTIES = {
        ANY_PROPERTY("")
                .types(ARRAY_PROPERTY(), OBJECT_PROPERTY())
                .displayOption(showWhen(RESPONSE_FORMAT).in(ResponseFormat.JSON.name(), ResponseFormat.XML.name())),
        STRING_PROPERTY().displayOption(showWhen(RESPONSE_FORMAT).eq(ResponseFormat.TEXT.name())),
        FILE_ENTRY_PROPERTY().displayOption(showWhen(RESPONSE_FORMAT).eq(ResponseFormat.FILE.name())),
    };

    private static final TaskDescriptor TASK_DESCRIPTOR = createTaskDescriptor(HTTP_CLIENT)
            .displayName("HTTP Client")
            .description("Makes an HTTP request and returns the response data.")
            .version(VERSION_1_0)
            .auth(
                    option(API_KEY.name().toLowerCase()),
                    option(BEARER_TOKEN.name().toLowerCase()),
                    option(BASIC_AUTH.name().toLowerCase()),
                    option(DIGEST_AUTH.name().toLowerCase()),
                    option(OAUTH2.name().toLowerCase()))
            .operations(
                    OPERATION(GET)
                            .displayName("GET")
                            .description("The request method to use.")
                            .inputs(
                                    //
                                    // Common properties
                                    //

                                    COMMON_PROPERTIES)
                            .outputs(
                                    ANY_PROPERTY()
                                            .types(ARRAY_PROPERTY(), OBJECT_PROPERTY())
                                            .displayOption(showWhen(RESPONSE_FORMAT)
                                                    .in(ResponseFormat.JSON.name(), ResponseFormat.XML.name())),
                                    STRING_PROPERTY()
                                            .displayOption(
                                                    showWhen(RESPONSE_FORMAT).eq(ResponseFormat.TEXT.name())),
                                    FILE_ENTRY_PROPERTY()
                                            .displayOption(
                                                    showWhen(RESPONSE_FORMAT).eq(ResponseFormat.FILE.name()))),
                    OPERATION(POST)
                            .displayName("POST")
                            .description("The request method to use.")
                            .inputs(ArrayUtils.addAll(
                                    ArrayUtils.addAll(
                                            //
                                            // Common properties
                                            //

                                            COMMON_PROPERTIES,
                                            //
                                            // Body Content properties
                                            //

                                            BODY_CONTENT_PROPERTIES),
                                    //
                                    // Options
                                    //

                                    OPTIONS(true)))
                            .outputs(
                                    ANY_PROPERTY()
                                            .types(ARRAY_PROPERTY(), OBJECT_PROPERTY())
                                            .displayOption(showWhen(RESPONSE_FORMAT)
                                                    .in(ResponseFormat.JSON.name(), ResponseFormat.XML.name())),
                                    STRING_PROPERTY()
                                            .displayOption(
                                                    showWhen(RESPONSE_FORMAT).eq(ResponseFormat.TEXT.name())),
                                    FILE_ENTRY_PROPERTY()
                                            .displayOption(
                                                    showWhen(RESPONSE_FORMAT).eq(ResponseFormat.FILE.name()))),
                    OPERATION(PUT)
                            .displayName("PUT")
                            .description("The request method to use.")
                            .inputs(ArrayUtils.addAll(
                                    ArrayUtils.addAll(
                                            //
                                            // Common properties
                                            //

                                            COMMON_PROPERTIES,
                                            //
                                            // Body Content properties
                                            //

                                            BODY_CONTENT_PROPERTIES),
                                    //
                                    // Options
                                    //

                                    OPTIONS(true)))
                            .outputs(OUTPUTS_PROPERTIES),
                    OPERATION(PATCH)
                            .displayName("PATCH")
                            .description("The request method to use.")
                            .inputs(ArrayUtils.addAll(
                                    ArrayUtils.addAll(
                                            //
                                            // Common properties
                                            //

                                            COMMON_PROPERTIES,
                                            //
                                            // Body Content properties
                                            //

                                            BODY_CONTENT_PROPERTIES),
                                    //
                                    // Options
                                    //

                                    OPTIONS(true)))
                            .outputs(OUTPUTS_PROPERTIES),
                    OPERATION(DELETE)
                            .displayName("DELETE")
                            .description("The request method to use.")
                            .inputs(ArrayUtils.addAll(
                                    //
                                    // Common properties
                                    //

                                    COMMON_PROPERTIES,
                                    //
                                    // Options
                                    //

                                    OPTIONS(false)))
                            .outputs(OUTPUTS_PROPERTIES),
                    OPERATION(HEAD)
                            .displayName("HEAD")
                            .description("The request method to use.")
                            .inputs(ArrayUtils.addAll(
                                    //
                                    // Common properties
                                    //

                                    COMMON_PROPERTIES,
                                    //
                                    // Options
                                    //

                                    OPTIONS(false)))
                            .outputs(OUTPUTS_PROPERTIES));

    @Override
    public TaskDescriptor getTaskDescriptor() {
        return TASK_DESCRIPTOR;
    }

    private static TaskProperty.OptionTaskProperty OPTIONS(boolean includeBodyContentProperties) {
        return DSL.OPTIONS()
                .displayName("Options")
                .placeholder("Add Option")
                .options(
                        includeBodyContentProperties
                                ? STRING_PROPERTY(BODY_CONTENT_TYPE)
                                        .displayName("Body Content Type")
                                        .description("Content-Type to use when sending body parameters.")
                                        .options(
                                                option("JSON", BodyContentType.JSON.name()),
                                                option("Raw", BodyContentType.RAW.name()),
                                                option("Form-Data", BodyContentType.FORM_DATA.name()),
                                                option("Form-Urlencoded", BodyContentType.FORM_URLENCODED.name()),
                                                option("Binary", BodyContentType.BINARY.name()))
                                        .defaultValue("JSON")
                                : null,
                        includeBodyContentProperties
                                ? STRING_PROPERTY(MIME_TYPE)
                                        .displayName("Mime Type")
                                        .description("Mime-Type to use when sending raw body content.")
                                        .displayOption(
                                                showWhen(BODY_CONTENT_TYPE).in(BodyContentType.RAW.name()))
                                        .placeholder("text/xml")
                                : null,
                        BOOLEAN_PROPERTY(FULL_RESPONSE)
                                .displayName("Full Response")
                                .description("Returns the full response data instead of only the body.")
                                .defaultValue(false),
                        BOOLEAN_PROPERTY(FOLLOW_ALL_REDIRECTS)
                                .displayName("Follow All Redirects")
                                .description("Follow non-GET HTTP 3xx redirects.")
                                .defaultValue(false),
                        BOOLEAN_PROPERTY(FOLLOW_REDIRECT)
                                .displayName("Follow GET Redirect")
                                .description("Follow GET HTTP 3xx redirects.")
                                .defaultValue(false),
                        BOOLEAN_PROPERTY(IGNORE_RESPONSE_CODE)
                                .displayName("Ignore Response Code")
                                .description("Succeeds also when the status code is not 2xx.")
                                .defaultValue(false),
                        STRING_PROPERTY(PROXY)
                                .displayName("Proxy")
                                .description("HTTP proxy to use.")
                                .placeholder("https://myproxy:3128")
                                .defaultValue(""),
                        INTEGER_PROPERTY(TIMEOUT)
                                .displayName("Timeout")
                                .description(
                                        "Time in ms to wait for the server to send a response before aborting the request.")
                                .defaultValue(1000)
                                .minValue(1));
    }
}
