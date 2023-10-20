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

package com.bytechef.component.http.client;

import static com.bytechef.component.http.client.constants.HttpClientConstants.ACCESS_TOKEN;
import static com.bytechef.component.http.client.constants.HttpClientConstants.ADD_TO;
import static com.bytechef.component.http.client.constants.HttpClientConstants.ALLOW_UNAUTHORIZED_CERTS;
import static com.bytechef.component.http.client.constants.HttpClientConstants.BODY_CONTENT_TYPE;
import static com.bytechef.component.http.client.constants.HttpClientConstants.BODY_PARAMETERS;
import static com.bytechef.component.http.client.constants.HttpClientConstants.DELETE;
import static com.bytechef.component.http.client.constants.HttpClientConstants.FOLLOW_ALL_REDIRECTS;
import static com.bytechef.component.http.client.constants.HttpClientConstants.FOLLOW_REDIRECT;
import static com.bytechef.component.http.client.constants.HttpClientConstants.FULL_RESPONSE;
import static com.bytechef.component.http.client.constants.HttpClientConstants.GET;
import static com.bytechef.component.http.client.constants.HttpClientConstants.HEAD;
import static com.bytechef.component.http.client.constants.HttpClientConstants.HEADER_PARAMETERS;
import static com.bytechef.component.http.client.constants.HttpClientConstants.HEADER_PREFIX;
import static com.bytechef.component.http.client.constants.HttpClientConstants.HTTP_CLIENT;
import static com.bytechef.component.http.client.constants.HttpClientConstants.IGNORE_RESPONSE_CODE;
import static com.bytechef.component.http.client.constants.HttpClientConstants.KEY;
import static com.bytechef.component.http.client.constants.HttpClientConstants.MIME_TYPE;
import static com.bytechef.component.http.client.constants.HttpClientConstants.PARAMETER;
import static com.bytechef.component.http.client.constants.HttpClientConstants.PASSWORD;
import static com.bytechef.component.http.client.constants.HttpClientConstants.PATCH;
import static com.bytechef.component.http.client.constants.HttpClientConstants.POST;
import static com.bytechef.component.http.client.constants.HttpClientConstants.PROXY;
import static com.bytechef.component.http.client.constants.HttpClientConstants.PUT;
import static com.bytechef.component.http.client.constants.HttpClientConstants.QUERY_PARAMETERS;
import static com.bytechef.component.http.client.constants.HttpClientConstants.RESPONSE_FILENAME;
import static com.bytechef.component.http.client.constants.HttpClientConstants.RESPONSE_FORMAT;
import static com.bytechef.component.http.client.constants.HttpClientConstants.SEND_FILE;
import static com.bytechef.component.http.client.constants.HttpClientConstants.TIMEOUT;
import static com.bytechef.component.http.client.constants.HttpClientConstants.TOKEN;
import static com.bytechef.component.http.client.constants.HttpClientConstants.URI;
import static com.bytechef.component.http.client.constants.HttpClientConstants.USERNAME;
import static com.bytechef.component.http.client.constants.HttpClientConstants.VALUE;
import static com.bytechef.hermes.component.ComponentDSL.action;
import static com.bytechef.hermes.component.ComponentDSL.any;
import static com.bytechef.hermes.component.ComponentDSL.array;
import static com.bytechef.hermes.component.ComponentDSL.bool;
import static com.bytechef.hermes.component.ComponentDSL.createComponent;
import static com.bytechef.hermes.component.ComponentDSL.createConnection;
import static com.bytechef.hermes.component.ComponentDSL.display;
import static com.bytechef.hermes.component.ComponentDSL.fileEntry;
import static com.bytechef.hermes.component.ComponentDSL.hideWhen;
import static com.bytechef.hermes.component.ComponentDSL.integer;
import static com.bytechef.hermes.component.ComponentDSL.object;
import static com.bytechef.hermes.component.ComponentDSL.showWhen;
import static com.bytechef.hermes.component.ComponentDSL.string;
import static com.bytechef.hermes.component.constants.ComponentConstants.FILE_ENTRY;

import com.bytechef.component.http.client.constants.HttpClientConstants;
import com.bytechef.component.http.client.constants.HttpClientConstants.ApiTokenLocation;
import com.bytechef.component.http.client.constants.HttpClientConstants.AuthType;
import com.bytechef.component.http.client.constants.HttpClientConstants.BodyContentType;
import com.bytechef.component.http.client.constants.HttpClientConstants.RequestMethod;
import com.bytechef.component.http.client.constants.HttpClientConstants.ResponseFormat;
import com.bytechef.hermes.component.ComponentDSL;
import com.bytechef.hermes.component.ComponentHandler;
import com.bytechef.hermes.component.Context;
import com.bytechef.hermes.component.ExecutionParameters;
import com.bytechef.hermes.component.definition.ComponentDefinition;
import com.bytechef.hermes.component.exception.ActionExecutionException;
import com.bytechef.hermes.definition.Property;
import org.apache.commons.lang3.ArrayUtils;

/**
 * @author Ivica Cardic
 */
public class HttpClientComponentHandler implements ComponentHandler {

    private static final Property[] COMMON_PROPERTIES = {
        //
        // General properties
        //
        string(URI)
                .label("URI")
                .description("The URI to make the request to")
                .placeholder("https://example.com/index.html")
                .defaultValue("")
                .required(true),
        bool(ALLOW_UNAUTHORIZED_CERTS)
                .label("Allow Unauthorized Certs")
                .description("Download the response even if SSL certificate validation is not possible.")
                .defaultValue(false),
        string(RESPONSE_FORMAT)
                .label("Response Format")
                .description("The format in which the data gets returned from the URL.")
                .options(
                        ComponentDSL.option(
                                "JSON",
                                ResponseFormat.JSON.name(),
                                "The response is automatically converted to object/array."),
                        ComponentDSL.option(
                                "XML",
                                ResponseFormat.XML.name(),
                                "The response is automatically converted to object/array."),
                        ComponentDSL.option("Text", ResponseFormat.TEXT.name(), "The response is returned as a text."),
                        ComponentDSL.option(
                                "File", ResponseFormat.FILE.name(), "The response is returned as a file object."))
                .defaultValue(ResponseFormat.JSON.name()),
        string(RESPONSE_FILENAME)
                .label("Response Filename")
                .description("The name of the file if the response is returned as a file object.")
                .displayOption(showWhen(RESPONSE_FORMAT).in(ResponseFormat.FILE.name())),
        //
        // Header properties
        //

        array(HEADER_PARAMETERS)
                .label("Header Parameters")
                .description("Header parameters to send.")
                .defaultValue("")
                .placeholder("Add Parameter")
                .items(object(PARAMETER)
                        .label("Parameter")
                        .properties(
                                string(KEY)
                                        .label("Key")
                                        .description("The key of the parameter.")
                                        .defaultValue(""),
                                string(VALUE)
                                        .label("Value")
                                        .description("The value of the parameter.")
                                        .defaultValue(""))),
        //
        // Query parameters properties
        //

        array(QUERY_PARAMETERS)
                .label("Query Parameters")
                .description("Query parameters to send.")
                .defaultValue("")
                .placeholder("Add Parameter")
                .items(object(PARAMETER)
                        .label("Parameter")
                        .properties(
                                string(KEY)
                                        .label("Key")
                                        .description("The key of the parameter.")
                                        .defaultValue(""),
                                string(VALUE)
                                        .label("Value")
                                        .description("The value of the parameter.")
                                        .defaultValue(""))),
    };

    private static final Property[] BODY_CONTENT_PROPERTIES = new Property[] {
        bool(SEND_FILE)
                .label("Send File")
                .description("Send file instead of body parameters.")
                .displayOption(showWhen(BODY_CONTENT_TYPE)
                        .in(BodyContentType.JSON.name(), BodyContentType.RAW.name(), BodyContentType.XML.name()))
                .defaultValue(false),
        object(BODY_PARAMETERS)
                .label("Body Parameters")
                .description("Body parameters to send.")
                .displayOption(showWhen(BODY_CONTENT_TYPE).eq(BodyContentType.JSON.name()))
                .additionalProperties(true)
                .placeholder("Add Parameter"),
        array(BODY_PARAMETERS)
                .label("Body Parameters")
                .description("Body parameters to send.")
                .displayOption(showWhen(BODY_CONTENT_TYPE).eq(BodyContentType.FORM_DATA.name()))
                .defaultValue("")
                .placeholder("Add Parameter")
                .items(object(PARAMETER)
                        .label("Parameter")
                        .properties(
                                string(KEY)
                                        .label("Key")
                                        .description("The key of the parameter.")
                                        .defaultValue(""),
                                string(VALUE)
                                        .label("Value")
                                        .description("The value of the parameter.")
                                        .defaultValue(""))),
        array(BODY_PARAMETERS)
                .label("Body Parameters")
                .description("Body parameters to send.")
                .displayOption(showWhen(BODY_CONTENT_TYPE).eq(BodyContentType.FORM_URLENCODED.name()))
                .defaultValue("")
                .placeholder("Add Parameter")
                .items(object(PARAMETER)
                        .label("Parameter")
                        .properties(
                                string(KEY)
                                        .label("Key")
                                        .description("The key of the parameter.")
                                        .defaultValue(""),
                                any(VALUE)
                                        .label("Value")
                                        .description("The value of the parameter.")
                                        .types(ComponentDSL.string(), ComponentDSL.fileEntry()))),
        string(BODY_PARAMETERS)
                .label("Raw")
                .description("The raw text to send.")
                .displayOption(showWhen(BODY_CONTENT_TYPE).eq(BodyContentType.RAW.name())),
        fileEntry(FILE_ENTRY)
                .label("File")
                .description("The object property which contains a reference to the file with data to upload.")
                .displayOption(
                        hideWhen(SEND_FILE).eq(false),
                        showWhen(BODY_CONTENT_TYPE)
                                .in(
                                        BodyContentType.BINARY.name(),
                                        BodyContentType.JSON.name(),
                                        BodyContentType.RAW.name(),
                                        BodyContentType.XML.name())),
    };

    private static final Property[] OUTPUTS_PROPERTIES = {
        ComponentDSL.any()
                .types(ComponentDSL.array(), ComponentDSL.object())
                .displayOption(showWhen(RESPONSE_FORMAT).in(ResponseFormat.JSON.name(), ResponseFormat.XML.name())),
        ComponentDSL.string().displayOption(showWhen(RESPONSE_FORMAT).eq(ResponseFormat.TEXT.name())),
        ComponentDSL.fileEntry().displayOption(showWhen(RESPONSE_FORMAT).eq(ResponseFormat.FILE.name())),
    };

    private ComponentDefinition componentDefinition = createComponent(HTTP_CLIENT)
            .display(display("HTTP Client").description("Makes an HTTP request and returns the response data."))
            .connections(
                    createConnection(AuthType.API_KEY.name().toLowerCase())
                            .display(display("API Key"))
                            .properties(
                                    string(KEY).label("Key").required(true).defaultValue(HttpClientConstants.API_TOKEN),
                                    string(VALUE).label("Value").required(true),
                                    string(ADD_TO)
                                            .label("Add to")
                                            .required(true)
                                            .options(
                                                    ComponentDSL.option("Header", ApiTokenLocation.HEADER.name()),
                                                    ComponentDSL.option(
                                                            "QueryParams", ApiTokenLocation.QUERY_PARAMS.name()))
                                            .required(true)),
                    createConnection(AuthType.BEARER_TOKEN.name().toLowerCase())
                            .display(display("Bearer Token"))
                            .properties(string(TOKEN).label("Token").required(true)),
                    createConnection(AuthType.BASIC_AUTH.name().toLowerCase())
                            .display(display("Basic Auth"))
                            .properties(
                                    string(USERNAME).label("Username").required(true),
                                    string(PASSWORD).label("Password").required(true)),
                    createConnection(AuthType.DIGEST_AUTH.name().toLowerCase())
                            .display(display("Digest Auth"))
                            .properties(
                                    string(USERNAME).label("Username").required(true),
                                    string(PASSWORD).label("Password").required(true)),
                    createConnection(AuthType.OAUTH2.name().toLowerCase())
                            .display(display("OAuth2"))
                            .properties(
                                    string(ACCESS_TOKEN).label("Access Token").required(true),
                                    string(HEADER_PREFIX)
                                            .label("Header Prefix")
                                            .required(true)
                                            .defaultValue("Bearer")))
            .actions(
                    action(GET)
                            .display(display("GET").description("The request method to use."))
                            .inputs(
                                    //
                                    // Common properties
                                    //

                                    COMMON_PROPERTIES)
                            .outputSchema(
                                    ComponentDSL.any()
                                            .types(ComponentDSL.array(), ComponentDSL.object())
                                            .displayOption(showWhen(RESPONSE_FORMAT)
                                                    .in(ResponseFormat.JSON.name(), ResponseFormat.XML.name())),
                                    ComponentDSL.string()
                                            .displayOption(
                                                    showWhen(RESPONSE_FORMAT).eq(ResponseFormat.TEXT.name())),
                                    ComponentDSL.fileEntry()
                                            .displayOption(
                                                    showWhen(RESPONSE_FORMAT).eq(ResponseFormat.FILE.name())))
                            .performFunction(this::performGet),
                    action(POST)
                            .display(display("POST").description("The request method to use."))
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

                                    options(true)))
                            .outputSchema(
                                    ComponentDSL.any()
                                            .types(ComponentDSL.array(), ComponentDSL.object())
                                            .displayOption(showWhen(RESPONSE_FORMAT)
                                                    .in(ResponseFormat.JSON.name(), ResponseFormat.XML.name())),
                                    ComponentDSL.string()
                                            .displayOption(
                                                    showWhen(RESPONSE_FORMAT).eq(ResponseFormat.TEXT.name())),
                                    ComponentDSL.fileEntry()
                                            .displayOption(
                                                    showWhen(RESPONSE_FORMAT).eq(ResponseFormat.FILE.name())))
                            .performFunction(this::performPost),
                    action(PUT)
                            .display(display("PUT").description("The request method to use."))
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

                                    options(true)))
                            .outputSchema(OUTPUTS_PROPERTIES)
                            .performFunction(this::performPut),
                    action(PATCH)
                            .display(display("PATCH").description("The request method to use."))
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

                                    options(true)))
                            .outputSchema(OUTPUTS_PROPERTIES)
                            .performFunction(this::performPatch),
                    action(DELETE)
                            .display(display("DELETE").description("The request method to use."))
                            .inputs(ArrayUtils.addAll(
                                    //
                                    // Common properties
                                    //

                                    COMMON_PROPERTIES,
                                    //
                                    // Options
                                    //

                                    options(false)))
                            .outputSchema(OUTPUTS_PROPERTIES)
                            .performFunction(this::performDelete),
                    action(HEAD)
                            .display(display("HEAD").description("The request method to use."))
                            .inputs(ArrayUtils.addAll(
                                    //
                                    // Common properties
                                    //

                                    COMMON_PROPERTIES,
                                    //
                                    // Options
                                    //

                                    options(false)))
                            .outputSchema(OUTPUTS_PROPERTIES)
                            .performFunction(this::performHead));

    private static Property.OptionProperty options(boolean includeBodyContentProperties) {
        return ComponentDSL.options()
                .label("Options")
                .placeholder("Add Option")
                .options(
                        includeBodyContentProperties
                                ? string(BODY_CONTENT_TYPE)
                                        .label("Body Content Type")
                                        .description("Content-Type to use when sending body parameters.")
                                        .options(
                                                ComponentDSL.option("JSON", BodyContentType.JSON.name()),
                                                ComponentDSL.option("Raw", BodyContentType.RAW.name()),
                                                ComponentDSL.option("Form-Data", BodyContentType.FORM_DATA.name()),
                                                ComponentDSL.option(
                                                        "Form-Urlencoded", BodyContentType.FORM_URLENCODED.name()),
                                                ComponentDSL.option("Binary", BodyContentType.BINARY.name()),
                                                ComponentDSL.option("XML", BodyContentType.XML.name()))
                                        .defaultValue("JSON")
                                : null,
                        includeBodyContentProperties
                                ? string(MIME_TYPE)
                                        .label("Mime Type")
                                        .description("Mime-Type to use when sending raw body content.")
                                        .displayOption(
                                                showWhen(BODY_CONTENT_TYPE).in(BodyContentType.RAW.name()))
                                        .placeholder("text/xml")
                                : null,
                        bool(FULL_RESPONSE)
                                .label("Full Response")
                                .description("Returns the full response data instead of only the body.")
                                .defaultValue(false),
                        bool(FOLLOW_ALL_REDIRECTS)
                                .label("Follow All Redirects")
                                .description("Follow non-GET HTTP 3xx redirects.")
                                .defaultValue(false),
                        bool(FOLLOW_REDIRECT)
                                .label("Follow GET Redirect")
                                .description("Follow GET HTTP 3xx redirects.")
                                .defaultValue(false),
                        bool(IGNORE_RESPONSE_CODE)
                                .label("Ignore Response Code")
                                .description("Succeeds also when the status code is not 2xx.")
                                .defaultValue(false),
                        string(PROXY)
                                .label("Proxy")
                                .description("HTTP proxy to use.")
                                .placeholder("https://myproxy:3128")
                                .defaultValue(""),
                        integer(TIMEOUT)
                                .label("Timeout")
                                .description(
                                        "Time in ms to wait for the server to send a response before aborting the request.")
                                .defaultValue(1000)
                                .minValue(1));
    }

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }

    protected Object performDelete(Context context, ExecutionParameters executionParameters) {
        return send(context, executionParameters, RequestMethod.DELETE);
    }

    protected Object performGet(Context context, ExecutionParameters executionParameters) {
        return send(context, executionParameters, RequestMethod.GET);
    }

    protected Object performHead(Context context, ExecutionParameters executionParameters) {
        return send(context, executionParameters, RequestMethod.HEAD);
    }

    protected Object performPatch(Context context, ExecutionParameters executionParameters) {
        return send(context, executionParameters, RequestMethod.PATCH);
    }

    protected Object performPost(Context context, ExecutionParameters executionParameters) {
        return send(context, executionParameters, RequestMethod.POST);
    }

    protected Object performPut(Context context, ExecutionParameters executionParameters) {
        return send(context, executionParameters, RequestMethod.PUT);
    }

    private Object send(Context context, ExecutionParameters executionParameters, RequestMethod patch) {
        HttpClientHelper httpClientHelper = new HttpClientHelper(context);

        try {
            return httpClientHelper.send(executionParameters, patch);
        } catch (Exception exception) {
            throw new ActionExecutionException("Unable to send payload", exception);
        }
    }
}
