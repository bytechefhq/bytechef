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

import static com.bytechef.hermes.component.constants.ComponentConstants.ADD_TO;
import static com.bytechef.hermes.component.constants.ComponentConstants.API_TOKEN;
import static com.bytechef.hermes.component.constants.ComponentConstants.AUTHORIZATION_URL;
import static com.bytechef.hermes.component.constants.ComponentConstants.BASE_URI;
import static com.bytechef.hermes.component.constants.ComponentConstants.CLIENT_ID;
import static com.bytechef.hermes.component.constants.ComponentConstants.CLIENT_SECRET;
import static com.bytechef.hermes.component.constants.ComponentConstants.FILE_ENTRY;
import static com.bytechef.hermes.component.constants.ComponentConstants.KEY;
import static com.bytechef.hermes.component.constants.ComponentConstants.PASSWORD;
import static com.bytechef.hermes.component.constants.ComponentConstants.SCOPES;
import static com.bytechef.hermes.component.constants.ComponentConstants.TOKEN;
import static com.bytechef.hermes.component.constants.ComponentConstants.TOKEN_URL;
import static com.bytechef.hermes.component.constants.ComponentConstants.USERNAME;
import static com.bytechef.hermes.component.constants.ComponentConstants.VALUE;
import static com.bytechef.hermes.component.definition.ComponentDSL.action;
import static com.bytechef.hermes.component.definition.ComponentDSL.any;
import static com.bytechef.hermes.component.definition.ComponentDSL.array;
import static com.bytechef.hermes.component.definition.ComponentDSL.authorization;
import static com.bytechef.hermes.component.definition.ComponentDSL.bool;
import static com.bytechef.hermes.component.definition.ComponentDSL.component;
import static com.bytechef.hermes.component.definition.ComponentDSL.connection;
import static com.bytechef.hermes.component.definition.ComponentDSL.display;
import static com.bytechef.hermes.component.definition.ComponentDSL.fileEntry;
import static com.bytechef.hermes.component.definition.ComponentDSL.hideWhen;
import static com.bytechef.hermes.component.definition.ComponentDSL.integer;
import static com.bytechef.hermes.component.definition.ComponentDSL.object;
import static com.bytechef.hermes.component.definition.ComponentDSL.showWhen;
import static com.bytechef.hermes.component.definition.ComponentDSL.string;
import static com.bytechef.hermes.component.http.client.constants.HttpClientConstants.ALLOW_UNAUTHORIZED_CERTS;
import static com.bytechef.hermes.component.http.client.constants.HttpClientConstants.BODY_CONTENT_TYPE;
import static com.bytechef.hermes.component.http.client.constants.HttpClientConstants.BODY_PARAMETERS;
import static com.bytechef.hermes.component.http.client.constants.HttpClientConstants.DELETE;
import static com.bytechef.hermes.component.http.client.constants.HttpClientConstants.FOLLOW_ALL_REDIRECTS;
import static com.bytechef.hermes.component.http.client.constants.HttpClientConstants.FOLLOW_REDIRECT;
import static com.bytechef.hermes.component.http.client.constants.HttpClientConstants.FULL_RESPONSE;
import static com.bytechef.hermes.component.http.client.constants.HttpClientConstants.GET;
import static com.bytechef.hermes.component.http.client.constants.HttpClientConstants.HEAD;
import static com.bytechef.hermes.component.http.client.constants.HttpClientConstants.HEADER_PARAMETERS;
import static com.bytechef.hermes.component.http.client.constants.HttpClientConstants.IGNORE_RESPONSE_CODE;
import static com.bytechef.hermes.component.http.client.constants.HttpClientConstants.MIME_TYPE;
import static com.bytechef.hermes.component.http.client.constants.HttpClientConstants.PATCH;
import static com.bytechef.hermes.component.http.client.constants.HttpClientConstants.POST;
import static com.bytechef.hermes.component.http.client.constants.HttpClientConstants.PROXY;
import static com.bytechef.hermes.component.http.client.constants.HttpClientConstants.PUT;
import static com.bytechef.hermes.component.http.client.constants.HttpClientConstants.QUERY_PARAMETERS;
import static com.bytechef.hermes.component.http.client.constants.HttpClientConstants.RESPONSE_FILENAME;
import static com.bytechef.hermes.component.http.client.constants.HttpClientConstants.RESPONSE_FORMAT;
import static com.bytechef.hermes.component.http.client.constants.HttpClientConstants.SEND_FILE;
import static com.bytechef.hermes.component.http.client.constants.HttpClientConstants.TIMEOUT;
import static com.bytechef.hermes.component.http.client.constants.HttpClientConstants.URI;

import com.bytechef.hermes.component.ComponentHandler;
import com.bytechef.hermes.component.Context;
import com.bytechef.hermes.component.ExecutionParameters;
import com.bytechef.hermes.component.definition.Authorization.ApiTokenLocation;
import com.bytechef.hermes.component.definition.Authorization.AuthorizationType;
import com.bytechef.hermes.component.definition.ComponentDSL;
import com.bytechef.hermes.component.definition.ComponentDefinition;
import com.bytechef.hermes.component.exception.ActionExecutionException;
import com.bytechef.hermes.component.http.client.HttpClient;
import com.bytechef.hermes.component.http.client.HttpClient.RequestMethod;
import com.bytechef.hermes.component.http.client.constants.HttpClientConstants;
import com.bytechef.hermes.definition.Property;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.ArrayUtils;

/**
 * @author Ivica Cardic
 */
public class HttpClientComponentHandler implements ComponentHandler {

    private static final HttpClient HTTP_CLIENT = new HttpClient();

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
                                HttpClient.ResponseFormat.JSON.name(),
                                "The response is automatically converted to object/array."),
                        ComponentDSL.option(
                                "XML",
                                HttpClient.ResponseFormat.XML.name(),
                                "The response is automatically converted to object/array."),
                        ComponentDSL.option(
                                "Text", HttpClient.ResponseFormat.TEXT.name(), "The response is returned as a text."),
                        ComponentDSL.option(
                                "File",
                                HttpClient.ResponseFormat.FILE.name(),
                                "The response is returned as a file object."))
                .defaultValue(HttpClient.ResponseFormat.JSON.name()),
        string(RESPONSE_FILENAME)
                .label("Response Filename")
                .description("The name of the file if the response is returned as a file object.")
                .displayOption(showWhen(RESPONSE_FORMAT).in(HttpClient.ResponseFormat.FILE.name())),
        //
        // Header properties
        //

        array(HEADER_PARAMETERS)
                .label("Header Parameters")
                .description("Header parameters to send.")
                .defaultValue("")
                .placeholder("Add Parameter")
                .items(object().label("Parameter")
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
                .items(object().label("Parameter")
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
                        .in(
                                HttpClient.BodyContentType.JSON.name(),
                                HttpClient.BodyContentType.RAW.name(),
                                HttpClient.BodyContentType.XML.name()))
                .defaultValue(false),
        object(BODY_PARAMETERS)
                .label("Body Parameters")
                .description("Body parameters to send.")
                .displayOption(showWhen(BODY_CONTENT_TYPE).eq(HttpClient.BodyContentType.JSON.name()))
                .additionalProperties(true)
                .properties(any())
                .placeholder("Add Parameter"),
        array(BODY_PARAMETERS)
                .label("Body Parameters")
                .description("Body parameters to send.")
                .displayOption(showWhen(BODY_CONTENT_TYPE).eq(HttpClient.BodyContentType.FORM_DATA.name()))
                .defaultValue("")
                .placeholder("Add Parameter")
                .items(object().label("Parameter")
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
                .displayOption(showWhen(BODY_CONTENT_TYPE).eq(HttpClient.BodyContentType.FORM_URLENCODED.name()))
                .defaultValue("")
                .placeholder("Add Parameter")
                .items(object().label("Parameter")
                        .properties(
                                string(KEY)
                                        .label("Key")
                                        .description("The key of the parameter.")
                                        .defaultValue(""),
                                any(VALUE)
                                        .label("Value")
                                        .description("The value of the parameter.")
                                        .types(string(), fileEntry()))),
        string(BODY_PARAMETERS)
                .label("Raw")
                .description("The raw text to send.")
                .displayOption(showWhen(BODY_CONTENT_TYPE).eq(HttpClient.BodyContentType.RAW.name())),
        fileEntry(FILE_ENTRY)
                .label("File")
                .description("The object property which contains a reference to the file with data to upload.")
                .displayOption(
                        hideWhen(SEND_FILE).eq(false),
                        showWhen(BODY_CONTENT_TYPE)
                                .in(
                                        HttpClient.BodyContentType.BINARY.name(),
                                        HttpClient.BodyContentType.JSON.name(),
                                        HttpClient.BodyContentType.RAW.name(),
                                        HttpClient.BodyContentType.XML.name())),
    };

    private static final Property<?>[] OUTPUT_PROPERTIES = {
        any().types(array(), object())
                .displayOption(
                        showWhen(RESPONSE_FORMAT)
                                .in(HttpClient.ResponseFormat.JSON.name(), HttpClient.ResponseFormat.XML.name()),
                        showWhen(FULL_RESPONSE).eq(true)),
        object().properties(any("body").types(array(), object()), object("headers"), integer("status"))
                .displayOption(
                        showWhen(RESPONSE_FORMAT)
                                .in(HttpClient.ResponseFormat.JSON.name(), HttpClient.ResponseFormat.XML.name()),
                        showWhen(FULL_RESPONSE).eq(false)),
        string().displayOption(
                        showWhen(RESPONSE_FORMAT).eq(HttpClient.ResponseFormat.TEXT.name()),
                        showWhen(FULL_RESPONSE).eq(true)),
        object().properties(string("body"), object("headers"), integer("status"))
                .displayOption(
                        showWhen(RESPONSE_FORMAT).eq(HttpClient.ResponseFormat.TEXT.name()),
                        showWhen(FULL_RESPONSE).eq(false)),
        fileEntry()
                .displayOption(
                        showWhen(RESPONSE_FORMAT).eq(HttpClient.ResponseFormat.FILE.name()),
                        showWhen(FULL_RESPONSE).eq(true)),
        object().properties(fileEntry("body"), object("headers"), integer("status"))
                .displayOption(
                        showWhen(RESPONSE_FORMAT).eq(HttpClient.ResponseFormat.FILE.name()),
                        showWhen(FULL_RESPONSE).eq(false))
    };

    private ComponentDefinition componentDefinition = component(HttpClientConstants.HTTP_CLIENT)
            .display(display("HTTP Client").description("Makes an HTTP request and returns the response data."))
            .connection(connection()
                    .properties(string(BASE_URI).label("Base URI"))
                    .authorizations(
                            authorization(AuthorizationType.API_KEY.name().toLowerCase(), AuthorizationType.API_KEY)
                                    .display(display("API Key"))
                                    .properties(
                                            string(KEY)
                                                    .label("Key")
                                                    .required(true)
                                                    .defaultValue(API_TOKEN),
                                            string(VALUE).label("Value").required(true),
                                            string(ADD_TO)
                                                    .label("Add to")
                                                    .required(true)
                                                    .options(
                                                            ComponentDSL.option(
                                                                    "Header", ApiTokenLocation.HEADER.name()),
                                                            ComponentDSL.option(
                                                                    "QueryParams",
                                                                    ApiTokenLocation.QUERY_PARAMS.name()))),
                            authorization(
                                            AuthorizationType.BEARER_TOKEN
                                                    .name()
                                                    .toLowerCase(),
                                            AuthorizationType.BEARER_TOKEN)
                                    .display(display("Bearer Token"))
                                    .properties(string(TOKEN).label("Token").required(true)),
                            authorization(
                                            AuthorizationType.BASIC_AUTH.name().toLowerCase(),
                                            AuthorizationType.BASIC_AUTH)
                                    .display(display("Basic Auth"))
                                    .properties(
                                            string(USERNAME).label("Username").required(true),
                                            string(PASSWORD).label("Password").required(true)),
                            authorization(
                                            AuthorizationType.DIGEST_AUTH.name().toLowerCase(),
                                            AuthorizationType.DIGEST_AUTH)
                                    .display(display("Digest Auth"))
                                    .properties(
                                            string(USERNAME).label("Username").required(true),
                                            string(PASSWORD).label("Password").required(true)),
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
                                            string(TOKEN_URL).label("Token URL").required(true),
                                            array(SCOPES).label("Scopes").items(string()),
                                            string(CLIENT_ID).label("Client Id").required(true),
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
                                            string(TOKEN_URL).label("Token URL").required(true),
                                            array(SCOPES).label("Scopes").items(string()),
                                            string(CLIENT_ID).label("Client Id").required(true),
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
                                    array().displayOption(showWhen(RESPONSE_FORMAT)
                                            .in(
                                                    HttpClient.ResponseFormat.JSON.name(),
                                                    HttpClient.ResponseFormat.XML.name())),
                                    object().displayOption(showWhen(RESPONSE_FORMAT)
                                            .in(
                                                    HttpClient.ResponseFormat.JSON.name(),
                                                    HttpClient.ResponseFormat.XML.name())),
                                    string().displayOption(showWhen(RESPONSE_FORMAT)
                                            .eq(HttpClient.ResponseFormat.TEXT.name())),
                                    fileEntry()
                                            .displayOption(showWhen(RESPONSE_FORMAT)
                                                    .eq(HttpClient.ResponseFormat.FILE.name())))
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
                                    array().displayOption(showWhen(RESPONSE_FORMAT)
                                            .in(
                                                    HttpClient.ResponseFormat.JSON.name(),
                                                    HttpClient.ResponseFormat.XML.name())),
                                    object().displayOption(showWhen(RESPONSE_FORMAT)
                                            .in(
                                                    HttpClient.ResponseFormat.JSON.name(),
                                                    HttpClient.ResponseFormat.XML.name())),
                                    string().displayOption(showWhen(RESPONSE_FORMAT)
                                            .eq(HttpClient.ResponseFormat.TEXT.name())),
                                    fileEntry()
                                            .displayOption(showWhen(RESPONSE_FORMAT)
                                                    .eq(HttpClient.ResponseFormat.FILE.name())))
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
                            ComponentDSL.option("JSON", HttpClient.BodyContentType.JSON.name()),
                            ComponentDSL.option("Raw", HttpClient.BodyContentType.RAW.name()),
                            ComponentDSL.option("Form-Data", HttpClient.BodyContentType.FORM_DATA.name()),
                            ComponentDSL.option("Form-Urlencoded", HttpClient.BodyContentType.FORM_URLENCODED.name()),
                            ComponentDSL.option("Binary", HttpClient.BodyContentType.BINARY.name()),
                            ComponentDSL.option("XML", HttpClient.BodyContentType.XML.name()))
                    .defaultValue("JSON"));
        }
        if (includeBodyContentProperties) {
            properties.add(string(MIME_TYPE)
                    .label("Mime Type")
                    .description("Mime-Type to use when sending raw body content.")
                    .displayOption(showWhen(BODY_CONTENT_TYPE).in(HttpClient.BodyContentType.RAW.name()))
                    .placeholder("text/xml"));
        }

        properties.addAll(List.of(
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
                        .minValue(1)));

        return properties.toArray(new Property[0]);
    }

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }

    protected Object performDelete(Context context, ExecutionParameters executionParameters) {
        return execute(context, executionParameters, HttpClient.RequestMethod.DELETE);
    }

    protected Object performGet(Context context, ExecutionParameters executionParameters) {
        return execute(context, executionParameters, HttpClient.RequestMethod.GET);
    }

    protected Object performHead(Context context, ExecutionParameters executionParameters) {
        return execute(context, executionParameters, HttpClient.RequestMethod.HEAD);
    }

    protected Object performPatch(Context context, ExecutionParameters executionParameters) {
        return execute(context, executionParameters, HttpClient.RequestMethod.PATCH);
    }

    protected Object performPost(Context context, ExecutionParameters executionParameters) {
        return execute(context, executionParameters, HttpClient.RequestMethod.POST);
    }

    protected Object performPut(Context context, ExecutionParameters executionParameters) {
        return execute(context, executionParameters, HttpClient.RequestMethod.PUT);
    }

    private Object execute(Context context, ExecutionParameters executionParameters, RequestMethod patch) {
        try {
            return HTTP_CLIENT.execute(context, executionParameters, patch);
        } catch (Exception exception) {
            throw new ActionExecutionException("Unable to send payload", exception);
        }
    }
}
