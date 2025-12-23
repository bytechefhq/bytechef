/*
 * Copyright 2025 ByteChef
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

package com.bytechef.component.http.client.util;

import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.date;
import static com.bytechef.component.definition.ComponentDsl.dateTime;
import static com.bytechef.component.definition.ComponentDsl.fileEntry;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.nullable;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.time;
import static com.bytechef.component.definition.Context.Http.ResponseType;
import static com.bytechef.component.http.client.constant.HttpClientConstants.ALLOW_UNAUTHORIZED_CERTS;
import static com.bytechef.component.http.client.constant.HttpClientConstants.BODY;
import static com.bytechef.component.http.client.constant.HttpClientConstants.BODY_CONTENT;
import static com.bytechef.component.http.client.constant.HttpClientConstants.BODY_CONTENT_MIME_TYPE;
import static com.bytechef.component.http.client.constant.HttpClientConstants.BODY_CONTENT_TYPE;
import static com.bytechef.component.http.client.constant.HttpClientConstants.FOLLOW_ALL_REDIRECTS;
import static com.bytechef.component.http.client.constant.HttpClientConstants.FOLLOW_REDIRECT;
import static com.bytechef.component.http.client.constant.HttpClientConstants.FULL_RESPONSE;
import static com.bytechef.component.http.client.constant.HttpClientConstants.HEADERS;
import static com.bytechef.component.http.client.constant.HttpClientConstants.IGNORE_RESPONSE_CODE;
import static com.bytechef.component.http.client.constant.HttpClientConstants.PROXY;
import static com.bytechef.component.http.client.constant.HttpClientConstants.QUERY_PARAMETERS;
import static com.bytechef.component.http.client.constant.HttpClientConstants.RESPONSE_CONTENT_TYPE;
import static com.bytechef.component.http.client.constant.HttpClientConstants.RESPONSE_FILENAME;
import static com.bytechef.component.http.client.constant.HttpClientConstants.RESPONSE_FORMAT;
import static com.bytechef.component.http.client.constant.HttpClientConstants.TIMEOUT;
import static com.bytechef.component.http.client.constant.HttpClientConstants.URI;

import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.BodyContentType;
import com.bytechef.component.definition.Context.Http.RequestMethod;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property;
import com.bytechef.component.definition.TypeReference;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 * @author Igor Beslic
 */
public class HttpClientActionUtils {

    public static List<? extends Property> options(boolean includeBodyContentProperties) {
        List<Property> properties = new ArrayList<>();

        if (includeBodyContentProperties) {
            properties.add(
                object(BODY)
                    .label("Body")
                    .description("The body of the request.")
                    .properties(
                        string(BODY_CONTENT_TYPE)
                            .label("Body Content Type")
                            .description("Content-Type to use when sending body parameters.")
                            .options(
                                option("None", ""),
                                option("JSON", BodyContentType.JSON.name()),
                                option("XML", BodyContentType.XML.name()),
                                option("Form-Data", BodyContentType.FORM_DATA.name()),
                                option("Form-Urlencoded", BodyContentType.FORM_URL_ENCODED.name()),
                                option("Raw", BodyContentType.RAW.name()),
                                option("Binary", BodyContentType.BINARY.name())),
                        object(BODY_CONTENT)
                            .label("Body Content - JSON")
                            .description("Body Parameters to send.")
                            .displayCondition(
                                "%s.%s == '%s'".formatted(BODY, BODY_CONTENT_TYPE, BodyContentType.JSON.name()))
                            .additionalProperties(
                                array(), bool(), date(), dateTime(), integer(), nullable(), number(), object(),
                                string(), time())
                            .placeholder("Add Parameter"),
                        object(BODY_CONTENT)
                            .label("Body Content - XML")
                            .description("XML content to send.")
                            .displayCondition(
                                "%s.%s == '%s'".formatted(BODY, BODY_CONTENT_TYPE, BodyContentType.XML.name()))
                            .placeholder("Add Parameter"),
                        object(BODY_CONTENT)
                            .label("Body Content - Form Data")
                            .description("Body parameters to send.")
                            .displayCondition(
                                "%s.%s == '%s'".formatted(BODY, BODY_CONTENT_TYPE, BodyContentType.FORM_DATA.name()))
                            .placeholder("Add Parameter")
                            .additionalProperties(string(), fileEntry()),
                        object(BODY_CONTENT)
                            .label("Body Content - Form URL-Encoded")
                            .description("Body parameters to send.")
                            .displayCondition(
                                "%s.%s == '%s'".formatted(
                                    BODY, BODY_CONTENT_TYPE, BodyContentType.FORM_URL_ENCODED.name()))
                            .placeholder("Add Parameter")
                            .additionalProperties(string()),
                        string(BODY_CONTENT)
                            .label("Body Content - Raw")
                            .description("The raw text to send.")
                            .displayCondition(
                                "%s.%s == '%s'".formatted(BODY, BODY_CONTENT_TYPE, BodyContentType.RAW.name())),
                        fileEntry(BODY_CONTENT)
                            .label("Body Content - Binary")
                            .description("The object property which contains a reference to the file to upload.")
                            .displayCondition(
                                "%s.%s == '%s'".formatted(BODY, BODY_CONTENT_TYPE, BodyContentType.BINARY.name())),
                        string(BODY_CONTENT_MIME_TYPE)
                            .label("Content Type")
                            .description("Mime-Type to use when sending binary body content.")
                            .displayCondition(
                                "'%s' == %s.%s".formatted(Http.BodyContentType.BINARY.name(), BODY, BODY_CONTENT_TYPE))
                            .placeholder("text/plain"),
                        string(BODY_CONTENT_MIME_TYPE)
                            .label("Content Type")
                            .description("Mime-Type to use when sending raw body content.")
                            .displayCondition(
                                "'%s' == %s.%s".formatted(Http.BodyContentType.RAW.name(), BODY, BODY_CONTENT_TYPE))
                            .defaultValue("text/plain")
                            .placeholder("text/plain")));
        }

        properties.addAll(
            List.of(
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
                    .advancedOption(true),
                integer(TIMEOUT)
                    .label("Timeout")
                    .description(
                        "Time in ms to wait for the server to send a response before aborting the request.")
                    .defaultValue(1000)
                    .minValue(1)
                    .advancedOption(true)));

        return properties;
    }

    @SuppressWarnings({
        "rawtypes", "unchecked"
    })
    public static Object execute(Parameters inputParameters, RequestMethod requestMethod, Context context) {
        Http.Response response =
            context
                .http(http -> http.exchange(inputParameters.getRequiredString(URI), requestMethod))
                .configuration(
                    Http.allowUnauthorizedCerts(inputParameters.getBoolean(ALLOW_UNAUTHORIZED_CERTS, false))
                        .filename(inputParameters.getString(RESPONSE_FILENAME))
                        .followAllRedirects(inputParameters.getBoolean(FOLLOW_ALL_REDIRECTS, false))
                        .followRedirect(inputParameters.getBoolean(FOLLOW_REDIRECT, false))
                        .proxy(inputParameters.getString(PROXY))
                        .responseType(getResponseType(inputParameters))
                        .timeout(Duration.ofMillis(inputParameters.getInteger(TIMEOUT, 10000))))
                .headers((Map) inputParameters.getMap(HEADERS, List.class, Collections.emptyMap()))
                .queryParameters((Map) inputParameters.getMap(QUERY_PARAMETERS, List.class, Collections.emptyMap()))
                .body(getBody(inputParameters, getBodyContentType(inputParameters)))
                .execute();

        if (inputParameters.getBoolean(FULL_RESPONSE, false)) {
            return response;
        } else {
            return response.getBody();
        }
    }

    public static ActionDefinition.PerformFunction getPerform(RequestMethod requestMethod) {
        return (inputParameters, connectionParameters, context) -> execute(
            inputParameters, requestMethod, context);
    }

    @SafeVarargs
    public static Property[] toArray(List<? extends Property>... propertiesArray) {
        List<? super Property> allProperties = new ArrayList<>();

        for (List<? extends Property> properties : propertiesArray) {
            allProperties.addAll(properties);
        }

        return allProperties.toArray(Property[]::new);
    }

    private static BodyContentType getBodyContentType(Parameters inputParameters) {
        if (!inputParameters.containsKey(BODY)) {
            return null;
        }

        String bodyContentTypeParameter = inputParameters.getFromPath(BODY + "." + BODY_CONTENT_TYPE, String.class);

        return bodyContentTypeParameter == null
            ? null
            : Http.BodyContentType.valueOf(bodyContentTypeParameter.toUpperCase());
    }

    private static Body getBody(Parameters inputParameters, BodyContentType bodyContentType) {
        Body body = null;

        if (inputParameters.containsKey(BODY)) {
            String bodyContentPath = BODY + "." + BODY_CONTENT;
            String bodyContentMimeTypePath = BODY + "." + BODY_CONTENT_MIME_TYPE;

            if (bodyContentType == Http.BodyContentType.BINARY) {
                body = Http.Body.of(
                    inputParameters.getRequiredFromPath(bodyContentPath, FileEntry.class),
                    inputParameters.getFromPath(bodyContentMimeTypePath, String.class));
            } else if (bodyContentType == Http.BodyContentType.FORM_DATA) {
                body = Http.Body.of(
                    inputParameters.getMapFromPath(bodyContentPath, List.of(FileEntry.class), Map.of()),
                    bodyContentType);
            } else if (bodyContentType == Http.BodyContentType.FORM_URL_ENCODED) {
                body = Http.Body.of(
                    inputParameters.getFromPath(bodyContentPath, new TypeReference<Map<String, ?>>() {}, Map.of()),
                    bodyContentType);
            } else if (bodyContentType == Http.BodyContentType.JSON || bodyContentType == Http.BodyContentType.XML) {
                body = Http.Body.of(
                    inputParameters.getFromPath(bodyContentPath, new TypeReference<Map<String, ?>>() {}, Map.of()),
                    bodyContentType);
            } else {
                body = Http.Body.of(
                    inputParameters.getFromPath(bodyContentPath, String.class),
                    inputParameters.getFromPath(bodyContentMimeTypePath, String.class, "text/plain"));
            }
        }

        return body;
    }

    private static ResponseType getResponseType(Parameters inputParameters) {
        if (inputParameters.containsKey(RESPONSE_FORMAT)) {
            String responseFormat = inputParameters.getString(RESPONSE_FORMAT);

            if (responseFormat.equals(String.valueOf(ResponseType.BINARY.getType()))) {
                String responseContentType = inputParameters.getString(RESPONSE_CONTENT_TYPE);

                return ResponseType.binary(responseContentType);
            } else {
                return Http.ResponseType.valueOf(responseFormat);
            }
        } else {
            return ResponseType.JSON;
        }
    }
}
