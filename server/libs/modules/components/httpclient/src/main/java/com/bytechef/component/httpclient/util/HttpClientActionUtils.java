
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

package com.bytechef.component.httpclient.util;

import com.bytechef.hermes.component.definition.Context;
import com.bytechef.hermes.component.definition.Context.FileEntry;
import com.bytechef.hermes.component.definition.Context.Http;
import com.bytechef.hermes.component.definition.ParameterMap;
import com.bytechef.hermes.component.definition.Context.Http.BodyContentType;
import com.bytechef.hermes.component.definition.Context.Http.Body;
import com.bytechef.hermes.component.definition.Context.Http.RequestMethod;
import com.bytechef.hermes.component.definition.Context.Http.ResponseType;

import com.bytechef.hermes.definition.DefinitionDSL.ModifiableProperty.ModifiableInputProperty;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.bytechef.component.httpclient.constant.HttpClientConstants.ALLOW_UNAUTHORIZED_CERTS;
import static com.bytechef.component.httpclient.constant.HttpClientConstants.BODY_CONTENT;
import static com.bytechef.component.httpclient.constant.HttpClientConstants.BODY_CONTENT_MIME_TYPE;
import static com.bytechef.component.httpclient.constant.HttpClientConstants.BODY_CONTENT_TYPE;
import static com.bytechef.component.httpclient.constant.HttpClientConstants.FOLLOW_ALL_REDIRECTS;
import static com.bytechef.component.httpclient.constant.HttpClientConstants.FOLLOW_REDIRECT;
import static com.bytechef.component.httpclient.constant.HttpClientConstants.FULL_RESPONSE;
import static com.bytechef.component.httpclient.constant.HttpClientConstants.HEADERS;
import static com.bytechef.component.httpclient.constant.HttpClientConstants.IGNORE_RESPONSE_CODE;
import static com.bytechef.component.httpclient.constant.HttpClientConstants.PROXY;
import static com.bytechef.component.httpclient.constant.HttpClientConstants.QUERY_PARAMETERS;
import static com.bytechef.component.httpclient.constant.HttpClientConstants.RESPONSE_FILENAME;
import static com.bytechef.component.httpclient.constant.HttpClientConstants.RESPONSE_FORMAT;
import static com.bytechef.component.httpclient.constant.HttpClientConstants.TIMEOUT;
import static com.bytechef.component.httpclient.constant.HttpClientConstants.URI;
import static com.bytechef.hermes.definition.DefinitionDSL.bool;
import static com.bytechef.hermes.definition.DefinitionDSL.integer;
import static com.bytechef.hermes.definition.DefinitionDSL.option;
import static com.bytechef.hermes.definition.DefinitionDSL.string;

/**
 * @author Ivica Cardic
 */
public class HttpClientActionUtils {

    public static List<? extends ModifiableInputProperty> options(boolean includeBodyContentProperties) {
        List<ModifiableInputProperty> properties = new ArrayList<>();

        if (includeBodyContentProperties) {
            properties.add(string(BODY_CONTENT_TYPE)
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
            properties.add(string(BODY_CONTENT_MIME_TYPE)
                .label("Content Type")
                .description("Mime-Type to use when sending raw body content.")
                .displayCondition(
                    "['%s', '%s'].includes('%s')".formatted(
                        Http.BodyContentType.BINARY.name(),
                        Http.BodyContentType.RAW.name(),
                        BODY_CONTENT_TYPE))
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

        return properties;
    }

    @SuppressWarnings({
        "rawtypes", "unchecked"
    })
    public static Object execute(ParameterMap inputParameters, RequestMethod requestMethod, Context context) {
        Http.Response response =
            context.http(http -> http.exchange(inputParameters.getRequiredString(URI), requestMethod))
                .configuration(
                    Http.allowUnauthorizedCerts(inputParameters.getBoolean(ALLOW_UNAUTHORIZED_CERTS, false))
                        .filename(inputParameters.getString(RESPONSE_FILENAME))
                        .followAllRedirects(inputParameters.getBoolean(FOLLOW_ALL_REDIRECTS, false))
                        .followRedirect(inputParameters.getBoolean(FOLLOW_REDIRECT, false))
                        .proxy(inputParameters.getString(PROXY))
                        .responseType(getResponseType(inputParameters))
                        .timeout(Duration.ofMillis(inputParameters.getInteger(TIMEOUT, 10000))))
                .headers((Map) inputParameters.getMap(HEADERS, List.class))
                .queryParameters((Map) inputParameters.getMap(QUERY_PARAMETERS, List.class))
                .body(getPayload(inputParameters, getBodyContentType(inputParameters)))
                .execute();

        if (inputParameters.getBoolean(FULL_RESPONSE, false)) {
            return response;
        } else {
            return response.getBody();
        }
    }

    @SafeVarargs
    public static ModifiableInputProperty[] toArray(List<? extends ModifiableInputProperty>... propertiesArray) {
        List<? super ModifiableInputProperty> allProperties = new ArrayList<>();

        for (List<? extends ModifiableInputProperty> properties : propertiesArray) {
            allProperties.addAll(properties);
        }

        return allProperties.toArray(ModifiableInputProperty[]::new);
    }

    private static BodyContentType getBodyContentType(ParameterMap inputParameters) {
        String bodyContentTypeParameter = inputParameters.getString(BODY_CONTENT_TYPE);

        return bodyContentTypeParameter == null
            ? null
            : Http.BodyContentType.valueOf(bodyContentTypeParameter.toUpperCase());
    }

    private static Body getPayload(ParameterMap inputParameters, BodyContentType bodyContentType) {
        Body body = null;

        if (inputParameters.containsKey(BODY_CONTENT)) {
            if (bodyContentType == Http.BodyContentType.BINARY) {
                body = Http.Body.of(
                    inputParameters.getRequired(BODY_CONTENT, FileEntry.class),
                    inputParameters.getString(BODY_CONTENT_MIME_TYPE));
            } else if (bodyContentType == Http.BodyContentType.FORM_DATA) {
                body = Http.Body.of(
                    inputParameters.getMap(BODY_CONTENT, List.of(FileEntry.class), Map.of()),
                    bodyContentType);
            } else if (bodyContentType == Http.BodyContentType.FORM_URL_ENCODED) {
                body = Http.Body.of(inputParameters.getMap(BODY_CONTENT, Map.of()), bodyContentType);
            } else if (bodyContentType == Http.BodyContentType.JSON || bodyContentType == Http.BodyContentType.XML) {
                body = Http.Body.of(
                    inputParameters.getMap(BODY_CONTENT, Map.of()), bodyContentType);
            } else {
                body = Http.Body.of(
                    inputParameters.getString(BODY_CONTENT),
                    inputParameters.getString(BODY_CONTENT_MIME_TYPE, "text/plain"));
            }
        }

        return body;
    }

    private static ResponseType getResponseType(ParameterMap inputParameters) {
        return inputParameters.containsKey(RESPONSE_FORMAT)
            ? Http.ResponseType.valueOf(inputParameters.getString(RESPONSE_FORMAT))
            : null;
    }
}
