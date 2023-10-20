
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

package com.bytechef.component.httpclient;

import static com.bytechef.component.httpclient.constants.HttpClientConstants.ALLOW_UNAUTHORIZED_CERTS;
import static com.bytechef.component.httpclient.constants.HttpClientConstants.BODY;
import static com.bytechef.component.httpclient.constants.HttpClientConstants.BODY_CONTENT_TYPE;
import static com.bytechef.component.httpclient.constants.HttpClientConstants.BODY_PARAMETERS;
import static com.bytechef.component.httpclient.constants.HttpClientConstants.DELETE;
import static com.bytechef.component.httpclient.constants.HttpClientConstants.FILE_ENTRY;
import static com.bytechef.component.httpclient.constants.HttpClientConstants.FOLLOW_ALL_REDIRECTS;
import static com.bytechef.component.httpclient.constants.HttpClientConstants.FOLLOW_REDIRECT;
import static com.bytechef.component.httpclient.constants.HttpClientConstants.FULL_RESPONSE;
import static com.bytechef.component.httpclient.constants.HttpClientConstants.GET;
import static com.bytechef.component.httpclient.constants.HttpClientConstants.HEAD;
import static com.bytechef.component.httpclient.constants.HttpClientConstants.HEADER_PARAMETERS;
import static com.bytechef.component.httpclient.constants.HttpClientConstants.HTTP_CLIENT;
import static com.bytechef.component.httpclient.constants.HttpClientConstants.IGNORE_RESPONSE_CODE;
import static com.bytechef.component.httpclient.constants.HttpClientConstants.RAW_CONTENT_MIME_TYPE;
import static com.bytechef.component.httpclient.constants.HttpClientConstants.PATCH;
import static com.bytechef.component.httpclient.constants.HttpClientConstants.POST;
import static com.bytechef.component.httpclient.constants.HttpClientConstants.PROXY;
import static com.bytechef.component.httpclient.constants.HttpClientConstants.PUT;
import static com.bytechef.component.httpclient.constants.HttpClientConstants.QUERY_PARAMETERS;
import static com.bytechef.component.httpclient.constants.HttpClientConstants.RESPONSE_FILENAME;
import static com.bytechef.component.httpclient.constants.HttpClientConstants.RESPONSE_FORMAT;
import static com.bytechef.component.httpclient.constants.HttpClientConstants.TIMEOUT;
import static com.bytechef.component.httpclient.constants.HttpClientConstants.URI;
import static com.bytechef.hermes.component.constants.ComponentConstants.ADD_TO;
import static com.bytechef.hermes.component.constants.ComponentConstants.API_TOKEN;
import static com.bytechef.hermes.component.constants.ComponentConstants.AUTHORIZATION_URL;
import static com.bytechef.hermes.component.constants.ComponentConstants.BASE_URI;
import static com.bytechef.hermes.component.constants.ComponentConstants.CLIENT_ID;
import static com.bytechef.hermes.component.constants.ComponentConstants.CLIENT_SECRET;
import static com.bytechef.hermes.component.constants.ComponentConstants.KEY;
import static com.bytechef.hermes.component.constants.ComponentConstants.PASSWORD;
import static com.bytechef.hermes.component.constants.ComponentConstants.SCOPES;
import static com.bytechef.hermes.component.constants.ComponentConstants.TOKEN;
import static com.bytechef.hermes.component.constants.ComponentConstants.TOKEN_URL;
import static com.bytechef.hermes.component.constants.ComponentConstants.USERNAME;
import static com.bytechef.hermes.component.constants.ComponentConstants.VALUE;
import static com.bytechef.hermes.component.definition.ComponentDSL.action;
import static com.bytechef.hermes.component.definition.ComponentDSL.array;
import static com.bytechef.hermes.component.definition.ComponentDSL.authorization;
import static com.bytechef.hermes.component.definition.ComponentDSL.bool;
import static com.bytechef.hermes.component.definition.ComponentDSL.component;
import static com.bytechef.hermes.component.definition.ComponentDSL.connection;
import static com.bytechef.hermes.component.definition.ComponentDSL.display;
import static com.bytechef.hermes.component.definition.ComponentDSL.fileEntry;
import static com.bytechef.hermes.component.definition.ComponentDSL.integer;
import static com.bytechef.hermes.component.definition.ComponentDSL.object;
import static com.bytechef.hermes.component.definition.ComponentDSL.show;
import static com.bytechef.hermes.component.definition.ComponentDSL.string;
import static com.bytechef.hermes.definition.DefinitionDSL.oneOf;
import static com.bytechef.hermes.definition.DefinitionDSL.option;

import com.bytechef.hermes.component.ComponentHandler;
import com.bytechef.hermes.component.Context;
import com.bytechef.hermes.component.ExecutionParameters;
import com.bytechef.hermes.component.FileEntry;
import com.bytechef.hermes.component.definition.Authorization.ApiTokenLocation;
import com.bytechef.hermes.component.definition.Authorization.AuthorizationType;
import com.bytechef.hermes.component.definition.ComponentDefinition;
import com.bytechef.hermes.component.utils.HttpClientUtils;
import com.bytechef.hermes.component.utils.HttpClientUtils.BodyContentType;
import com.bytechef.hermes.component.utils.HttpClientUtils.ResponseFormat;
import com.bytechef.hermes.definition.Property;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * @author Ivica Cardic
 */
public class HttpClientComponentHandler implements ComponentHandler {

    private static final Property<?>[] COMMON_PROPERTIES = {
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
                option(
                    "JSON",
                    ResponseFormat.JSON.name(),
                    "The response is automatically converted to object/array."),
                option(
                    "XML",
                    ResponseFormat.XML.name(),
                    "The response is automatically converted to object/array."),
                option("Text", ResponseFormat.TEXT.name(), "The response is returned as a text."),
                option(
                    "File", ResponseFormat.FILE.name(), "The response is returned as a file object."))
            .defaultValue(ResponseFormat.JSON.name()),
        string(RESPONSE_FILENAME)
            .label("Response Filename")
            .description("The name of the file if the response is returned as a file object.")
            .displayOption(show(RESPONSE_FORMAT, ResponseFormat.FILE.name())),
        //
        // Header properties
        //

        object(HEADER_PARAMETERS)
            .label("Header Parameters")
            .description("Header parameters to send.")
            .placeholder("Add Parameter")
            .additionalProperties(string()),

        //
        // Query parameters properties
        //

        object(QUERY_PARAMETERS)
            .label("Query Parameters")
            .description("Query parameters to send.")
            .placeholder("Add Parameter")
            .additionalProperties(string())
    };

    private static final Property<?>[] BODY_CONTENT_PROPERTIES = new Property[] {
        object(BODY_PARAMETERS)
            .label("Body Parameters")
            .description("Body Parameters to send.")
            .displayOption(show(BODY_CONTENT_TYPE, BodyContentType.JSON.name()))
            .additionalProperties(oneOf())
            .placeholder("Add Parameter"),
        object(BODY_PARAMETERS)
            .label("XML")
            .description("XML content to send.")
            .displayOption(show(BODY_CONTENT_TYPE, BodyContentType.XML.name()))
            .additionalProperties(oneOf())
            .placeholder("Add Parameter"),
        object(BODY_PARAMETERS)
            .label("Body Parameters")
            .description("Body parameters to send.")
            .displayOption(show(BODY_CONTENT_TYPE, BodyContentType.FORM_DATA.name()))
            .placeholder("Add Parameter")
            .additionalProperties(oneOf().types(string(), fileEntry())),
        object(BODY_PARAMETERS)
            .label("Body Parameters")
            .description("Body parameters to send.")
            .displayOption(show(BODY_CONTENT_TYPE, BodyContentType.FORM_URLENCODED.name()))
            .placeholder("Add Parameter")
            .additionalProperties(string()),
        string(BODY)
            .label("Body")
            .description("The raw text to send.")
            .displayOption(show(BODY_CONTENT_TYPE, BodyContentType.RAW.name())),
        fileEntry(FILE_ENTRY)
            .label("File")
            .description("The object property which contains a reference to the file to upload.")
            .displayOption(show(BODY_CONTENT_TYPE, BodyContentType.BINARY.name()))
    };

    private static final Property<?>[] OUTPUT_PROPERTIES = {
        object().properties(oneOf("body").types(array(), object()), object("headers"), integer("status"))
            .displayOption(show(
                RESPONSE_FORMAT,
                List.of(ResponseFormat.JSON.name(), ResponseFormat.XML.name()),
                FULL_RESPONSE,
                List.of(false))),
        oneOf()
            .types(array(), object())
            .displayOption(show(
                RESPONSE_FORMAT,
                List.of(ResponseFormat.JSON.name(), ResponseFormat.XML.name()),
                FULL_RESPONSE,
                List.of(true))),
        string().displayOption(
            show(RESPONSE_FORMAT, List.of(ResponseFormat.TEXT.name()), FULL_RESPONSE, List.of(true))),
        object().properties(string("body"), object("headers"), integer("status"))
            .displayOption(
                show(RESPONSE_FORMAT, List.of(ResponseFormat.TEXT.name()), FULL_RESPONSE, List.of(false))),
        fileEntry()
            .displayOption(
                show(RESPONSE_FORMAT, List.of(ResponseFormat.FILE.name()), FULL_RESPONSE, List.of(true))),
        object().properties(fileEntry("body"), object("headers"), integer("status"))
            .displayOption(
                show(RESPONSE_FORMAT, List.of(ResponseFormat.FILE.name()), FULL_RESPONSE, List.of(false)))
    };

    private final ComponentDefinition componentDefinition = component(HTTP_CLIENT)
        .display(display("HTTP Client").description("Makes an HTTP request and returns the response data."))
        .connection(connection()
            .properties(string(BASE_URI).label("Base URI"))
            .authorizations(
                authorization(AuthorizationType.API_KEY.name()
                    .toLowerCase(), AuthorizationType.API_KEY)
                        .display(display("API Key"))
                        .properties(
                            string(KEY)
                                .label("Key")
                                .required(true)
                                .defaultValue(API_TOKEN),
                            string(VALUE).label("Value")
                                .required(true),
                            string(ADD_TO)
                                .label("Add to")
                                .required(true)
                                .options(
                                    option(
                                        "Header", ApiTokenLocation.HEADER.name()),
                                    option(
                                        "QueryParams",
                                        ApiTokenLocation.QUERY_PARAMETERS.name()))),
                authorization(
                    AuthorizationType.BEARER_TOKEN
                        .name()
                        .toLowerCase(),
                    AuthorizationType.BEARER_TOKEN)
                        .display(display("Bearer Token"))
                        .properties(string(TOKEN).label("Token")
                            .required(true)),
                authorization(
                    AuthorizationType.BASIC_AUTH.name()
                        .toLowerCase(),
                    AuthorizationType.BASIC_AUTH)
                        .display(display("Basic Auth"))
                        .properties(
                            string(USERNAME).label("Username")
                                .required(true),
                            string(PASSWORD).label("Password")
                                .required(true)),
                authorization(
                    AuthorizationType.DIGEST_AUTH.name()
                        .toLowerCase(),
                    AuthorizationType.DIGEST_AUTH)
                        .display(display("Digest Auth"))
                        .properties(
                            string(USERNAME).label("Username")
                                .required(true),
                            string(PASSWORD).label("Password")
                                .required(true)),
                authorization(
                    AuthorizationType.OAUTH2_AUTHORIZATION_CODE
                        .name()
                        .toLowerCase(),
                    AuthorizationType.OAUTH2_AUTHORIZATION_CODE)
                        .display(display("OAuth2 Authorization code"))
                        .properties(
                            string(AUTHORIZATION_URL)
                                .label("Authorization URL")
                                .required(true),
                            string(TOKEN_URL).label("Token URL")
                                .required(true),
                            array(SCOPES).label("Scopes")
                                .items(string()),
                            string(CLIENT_ID).label("Client Id")
                                .required(true),
                            string(CLIENT_SECRET)
                                .label("Client Secret")
                                .required(true)),
                authorization(
                    AuthorizationType.OAUTH2_CLIENT_CREDENTIALS
                        .name()
                        .toLowerCase(),
                    AuthorizationType.OAUTH2_CLIENT_CREDENTIALS)
                        .display(display("OAuth2 Client Credentials"))
                        .properties(
                            string(TOKEN_URL).label("Token URL")
                                .required(true),
                            array(SCOPES).label("Scopes")
                                .items(string()),
                            string(CLIENT_ID).label("Client Id")
                                .required(true),
                            string(CLIENT_SECRET)
                                .label("Client Secret")
                                .required(true))))
        .actions(
            action(GET)
                .display(display("GET").description("The request method to use."))
                .properties(
                    //
                    // Common properties
                    //

                    COMMON_PROPERTIES)
                .output(
                    oneOf()
                        .types(array(), object())
                        .displayOption(
                            show(
                                RESPONSE_FORMAT,
                                List.of(ResponseFormat.JSON.name(), ResponseFormat.XML.name()))),
                    string().displayOption(show(RESPONSE_FORMAT, ResponseFormat.TEXT.name())),
                    fileEntry().displayOption(show(RESPONSE_FORMAT, ResponseFormat.FILE.name())))
                .perform(this::performGet),
            action(POST)
                .display(display("POST").description("The request method to use."))
                .properties(ArrayUtils.addAll(
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
                .output(
                    oneOf().types(array(), object())
                        .displayOption(show(
                            RESPONSE_FORMAT,
                            List.of(ResponseFormat.JSON.name(), ResponseFormat.XML.name()))),
                    string().displayOption(show(RESPONSE_FORMAT, ResponseFormat.TEXT.name())),
                    fileEntry().displayOption(show(RESPONSE_FORMAT, ResponseFormat.FILE.name())))
                .perform(this::performPost),
            action(PUT)
                .display(display("PUT").description("The request method to use."))
                .properties(ArrayUtils.addAll(
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
                .output(OUTPUT_PROPERTIES)
                .perform(this::performPut),
            action(PATCH)
                .display(display("PATCH").description("The request method to use."))
                .properties(ArrayUtils.addAll(
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
                .output(OUTPUT_PROPERTIES)
                .perform(this::performPatch),
            action(DELETE)
                .display(display("DELETE").description("The request method to use."))
                .properties(ArrayUtils.addAll(
                    //
                    // Common properties
                    //

                    COMMON_PROPERTIES,
                    //
                    // Options
                    //

                    options(false)))
                .output(OUTPUT_PROPERTIES)
                .perform(this::performDelete),
            action(HEAD)
                .display(display("HEAD").description("The request method to use."))
                .properties(ArrayUtils.addAll(
                    //
                    // Common properties
                    //

                    COMMON_PROPERTIES,
                    //
                    // Options
                    //

                    options(false)))
                .output(OUTPUT_PROPERTIES)
                .perform(this::performHead));

    private static Property<?>[] options(boolean includeBodyContentProperties) {
        List<Property<?>> properties = new ArrayList<>();

        if (includeBodyContentProperties) {
            properties.add(string(BODY_CONTENT_TYPE)
                .label("Body Content Type")
                .description("Content-Type to use when sending body parameters.")
                .options(
                    option("None", ""),
                    option("JSON", BodyContentType.JSON.name()),
                    option("Form-Data", BodyContentType.FORM_DATA.name()),
                    option("Form-Urlencoded", BodyContentType.FORM_URLENCODED.name()),
                    option("Raw", BodyContentType.RAW.name()),
                    option("Binary", BodyContentType.BINARY.name()))
                .defaultValue("NONE")
                .advancedOption(true));
        }

        if (includeBodyContentProperties) {
            properties.add(string(RAW_CONTENT_MIME_TYPE)
                .label("Content Type")
                .description("Mime-Type to use when sending raw body content.")
                .displayOption(show(BODY_CONTENT_TYPE, BodyContentType.RAW.name()))
                .defaultValue("text/plain")
                .placeholder("text/plain")
                .advancedOption(true));
        }

        properties.addAll(List.of(
            bool(FULL_RESPONSE)
                .label("Full Response")
                .description("Returns the full response data instead of only the body.")
                .defaultValue(false)
                .advancedOption(true),
            bool(FOLLOW_ALL_REDIRECTS)
                .label("Follow All Redirects")
                .description("Follow non-GET HTTP 3xx redirects.")
                .defaultValue(false)
                .advancedOption(true),
            bool(FOLLOW_REDIRECT)
                .label("Follow GET Redirect")
                .description("Follow GET HTTP 3xx redirects.")
                .defaultValue(false)
                .advancedOption(true),
            bool(IGNORE_RESPONSE_CODE)
                .label("Ignore Response Code")
                .description("Succeeds also when the status code is not 2xx.")
                .defaultValue(false)
                .advancedOption(true),
            string(PROXY)
                .label("Proxy")
                .description("HTTP proxy to use.")
                .placeholder("https://myproxy:3128")
                .defaultValue("")
                .advancedOption(true),
            integer(TIMEOUT)
                .label("Timeout")
                .description(
                    "Time in ms to wait for the server to send a response before aborting the request.")
                .defaultValue(1000)
                .minValue(1)
                .advancedOption(true)));

        return properties.toArray(new Property[0]);
    }

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }

    protected Object performDelete(Context context, ExecutionParameters executionParameters) {
        return execute(context, executionParameters, HttpClientUtils.RequestMethod.DELETE);
    }

    protected Object performGet(Context context, ExecutionParameters executionParameters) {
        return execute(context, executionParameters, HttpClientUtils.RequestMethod.GET);
    }

    protected Object performHead(Context context, ExecutionParameters executionParameters) {
        return execute(context, executionParameters, HttpClientUtils.RequestMethod.HEAD);
    }

    protected Object performPatch(Context context, ExecutionParameters executionParameters) {
        return execute(context, executionParameters, HttpClientUtils.RequestMethod.PATCH);
    }

    protected Object performPost(Context context, ExecutionParameters executionParameters) {
        return execute(context, executionParameters, HttpClientUtils.RequestMethod.POST);
    }

    protected Object performPut(Context context, ExecutionParameters executionParameters) {
        return execute(context, executionParameters, HttpClientUtils.RequestMethod.PUT);
    }

    private Object execute(
        Context context, ExecutionParameters executionParameters, HttpClientUtils.RequestMethod requestMethod) {
        HttpClientUtils.Payload payload = null;

        BodyContentType bodyContentType = executionParameters.containsKey(BODY_CONTENT_TYPE)
            ? BodyContentType.valueOf(StringUtils.upperCase(executionParameters.getString(BODY_CONTENT_TYPE)))
            : null;

        if (executionParameters.containsKey(BODY_PARAMETERS)) {
            if (bodyContentType == BodyContentType.FORM_DATA) {
                payload = HttpClientUtils.Payload.of(
                    executionParameters.getMap(BODY_PARAMETERS, List.of(FileEntry.class), Map.of()), bodyContentType);
            } else {
                payload = HttpClientUtils.Payload.of(
                    executionParameters.getMap(BODY_PARAMETERS, Map.of()), bodyContentType);
            }
        } else if (executionParameters.containsKey(BODY)) {
            payload = HttpClientUtils.Payload.of(
                executionParameters.getString(BODY),
                executionParameters.getString(RAW_CONTENT_MIME_TYPE, "text/plain"));
        } else if (executionParameters.containsKey(FILE_ENTRY)) {
            payload = HttpClientUtils.Payload.of(executionParameters.get(FILE_ENTRY, FileEntry.class));
        }

        return HttpClientUtils.executor()
            .configuration(HttpClientUtils.Configuration.builder()
                .allowUnauthorizedCerts(executionParameters.getBoolean(ALLOW_UNAUTHORIZED_CERTS, false))
                .filename(executionParameters.getString(RESPONSE_FILENAME))
                .followAllRedirects(executionParameters.getBoolean(FOLLOW_ALL_REDIRECTS, false))
                .followRedirect(executionParameters.getBoolean(FOLLOW_REDIRECT, false))
                .fullResponse(executionParameters.getBoolean(FULL_RESPONSE, false))
                .proxy(executionParameters.getString(PROXY))
                .responseFormat(
                    executionParameters.containsKey(RESPONSE_FORMAT)
                        ? ResponseFormat.valueOf(executionParameters.getString(RESPONSE_FORMAT))
                        : null)
                .timeout(Duration.ofMillis(executionParameters.getInteger(TIMEOUT, 10000)))
                .build())
            .exchange(executionParameters.getRequiredString(URI), requestMethod)
            .headers(executionParameters.getMap(HEADER_PARAMETERS))
            .queryParameters(executionParameters.getMap(QUERY_PARAMETERS))
            .payload(payload)
            .execute(context);
    }
}
