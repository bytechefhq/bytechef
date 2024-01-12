/*
 * Copyright 2023-present ByteChef Inc.
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

import static com.bytechef.component.definition.ComponentDSL.bool;
import static com.bytechef.component.definition.ComponentDSL.integer;
import static com.bytechef.component.definition.ComponentDSL.option;
import static com.bytechef.component.definition.ComponentDSL.string;

import com.bytechef.component.definition.ActionContext.FileEntry;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.BodyContentType;
import com.bytechef.component.definition.Context.Http.RequestMethod;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property.InputProperty;
import com.bytechef.component.http.client.constant.HttpClientConstants;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class HttpClientActionUtils {

    public static List<? extends InputProperty> options(boolean includeBodyContentProperties) {
        List<InputProperty> properties = new ArrayList<>();

        if (includeBodyContentProperties) {
            properties.add(string(HttpClientConstants.BODY_CONTENT_TYPE)
                .label("Body Content Type")
                .description("Content-Type to use when sending body parameters.")
                .options(
                    option("None", ""),
                    option("JSON", Http.BodyContentType.JSON.name()),
                    option("Form-Data", Http.BodyContentType.FORM_DATA.name()),
                    option("Form-Urlencoded", Http.BodyContentType.FORM_URL_ENCODED.name()),
                    option("Raw", Http.BodyContentType.RAW.name()),
                    option("Binary", Http.BodyContentType.BINARY.name()))
                .defaultValue("")
                .advancedOption(true));
        }

        if (includeBodyContentProperties) {
            properties.add(string(HttpClientConstants.BODY_CONTENT_MIME_TYPE)
                .label("Content Type")
                .description("Mime-Type to use when sending raw body content.")
                .displayCondition(
                    "['%s', '%s'].includes('%s')".formatted(
                        Http.BodyContentType.BINARY.name(),
                        Http.BodyContentType.RAW.name(),
                        HttpClientConstants.BODY_CONTENT_TYPE))
                .defaultValue("text/plain")
                .placeholder("text/plain")
                .advancedOption(true));
        }

        properties.addAll(List.of(
            bool(HttpClientConstants.FULL_RESPONSE)
                .label("Full Response")
                .description("Returns the full response data instead of only the body.")
                .defaultValue(false)
                .advancedOption(true),
            bool(HttpClientConstants.FOLLOW_ALL_REDIRECTS)
                .label("Follow All Redirects")
                .description("Follow non-GET HTTP 3xx redirects.")
                .defaultValue(false)
                .advancedOption(true),
            bool(HttpClientConstants.FOLLOW_REDIRECT)
                .label("Follow GET Redirect")
                .description("Follow GET HTTP 3xx redirects.")
                .defaultValue(false)
                .advancedOption(true),
            bool(HttpClientConstants.IGNORE_RESPONSE_CODE)
                .label("Ignore Response Code")
                .description("Succeeds also when the status code is not 2xx.")
                .defaultValue(false)
                .advancedOption(true),
            string(HttpClientConstants.PROXY)
                .label("Proxy")
                .description("HTTP proxy to use.")
                .placeholder("https://myproxy:3128")
                .defaultValue("")
                .advancedOption(true),
            integer(HttpClientConstants.TIMEOUT)
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
                .http(http -> http.exchange(inputParameters.getRequiredString(HttpClientConstants.URI), requestMethod))
                .configuration(
                    Http.allowUnauthorizedCerts(
                        inputParameters.getBoolean(HttpClientConstants.ALLOW_UNAUTHORIZED_CERTS, false))
                        .filename(inputParameters.getString(HttpClientConstants.RESPONSE_FILENAME))
                        .followAllRedirects(inputParameters.getBoolean(HttpClientConstants.FOLLOW_ALL_REDIRECTS, false))
                        .followRedirect(inputParameters.getBoolean(HttpClientConstants.FOLLOW_REDIRECT, false))
                        .proxy(inputParameters.getString(HttpClientConstants.PROXY))
                        .responseType(getResponseType(inputParameters))
                        .timeout(Duration.ofMillis(inputParameters.getInteger(HttpClientConstants.TIMEOUT, 10000))))
                .headers((Map) inputParameters.getMap(HttpClientConstants.HEADERS, List.class, Collections.emptyMap()))
                .queryParameters((Map) inputParameters.getMap(HttpClientConstants.QUERY_PARAMETERS, List.class,
                    Collections.emptyMap()))
                .body(getPayload(inputParameters, getBodyContentType(inputParameters)))
                .execute();

        if (inputParameters.getBoolean(HttpClientConstants.FULL_RESPONSE, false)) {
            return response;
        } else {
            return response.getBody();
        }
    }

    @SafeVarargs
    public static InputProperty[] toArray(List<? extends InputProperty>... propertiesArray) {
        List<? super InputProperty> allProperties = new ArrayList<>();

        for (List<? extends InputProperty> properties : propertiesArray) {
            allProperties.addAll(properties);
        }

        return allProperties.toArray(InputProperty[]::new);
    }

    private static BodyContentType getBodyContentType(Parameters inputParameters) {
        String bodyContentTypeParameter = inputParameters.getString(HttpClientConstants.BODY_CONTENT_TYPE);

        return bodyContentTypeParameter == null
            ? null
            : Http.BodyContentType.valueOf(bodyContentTypeParameter.toUpperCase());
    }

    private static Body getPayload(Parameters inputParameters, BodyContentType bodyContentType) {
        Body body = null;

        if (inputParameters.containsKey(HttpClientConstants.BODY_CONTENT)) {
            if (bodyContentType == Http.BodyContentType.BINARY) {
                body = Http.Body.of(
                    inputParameters.getRequired(HttpClientConstants.BODY_CONTENT, FileEntry.class),
                    inputParameters.getString(HttpClientConstants.BODY_CONTENT_MIME_TYPE));
            } else if (bodyContentType == Http.BodyContentType.FORM_DATA) {
                body = Http.Body.of(
                    inputParameters.getMap(HttpClientConstants.BODY_CONTENT, List.of(FileEntry.class), Map.of()),
                    bodyContentType);
            } else if (bodyContentType == Http.BodyContentType.FORM_URL_ENCODED) {
                body =
                    Http.Body.of(inputParameters.getMap(HttpClientConstants.BODY_CONTENT, Map.of()), bodyContentType);
            } else if (bodyContentType == Http.BodyContentType.JSON || bodyContentType == Http.BodyContentType.XML) {
                body = Http.Body.of(
                    inputParameters.getMap(HttpClientConstants.BODY_CONTENT, Map.of()), bodyContentType);
            } else {
                body = Http.Body.of(
                    inputParameters.getString(HttpClientConstants.BODY_CONTENT),
                    inputParameters.getString(HttpClientConstants.BODY_CONTENT_MIME_TYPE, "text/plain"));
            }
        }

        return body;
    }

    private static ResponseType getResponseType(Parameters inputParameters) {
        return inputParameters.containsKey(HttpClientConstants.RESPONSE_FORMAT)
            ? Http.ResponseType.valueOf(inputParameters.getString(HttpClientConstants.RESPONSE_FORMAT))
            : null;
    }
}
