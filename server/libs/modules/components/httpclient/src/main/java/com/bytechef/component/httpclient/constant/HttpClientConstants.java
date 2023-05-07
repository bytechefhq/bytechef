
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

package com.bytechef.component.httpclient.constant;

import com.bytechef.hermes.definition.Property;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.bytechef.hermes.component.definition.ComponentDSL.fileEntry;
import static com.bytechef.hermes.component.util.HttpClientUtils.BodyContentType;
import static com.bytechef.hermes.component.util.HttpClientUtils.ResponseFormat;
import static com.bytechef.hermes.definition.DefinitionDSL.array;
import static com.bytechef.hermes.definition.DefinitionDSL.bool;
import static com.bytechef.hermes.definition.DefinitionDSL.integer;
import static com.bytechef.hermes.definition.DefinitionDSL.object;
import static com.bytechef.hermes.definition.DefinitionDSL.oneOf;
import static com.bytechef.hermes.definition.DefinitionDSL.option;
import static com.bytechef.hermes.definition.DefinitionDSL.string;

/**
 * @author Ivica Cardic
 */
public class HttpClientConstants {

    public static final String ALLOW_UNAUTHORIZED_CERTS = "allowUnauthorizedCerts";
    public static final String BODY_CONTENT = "bodyContent";
    public static final String BODY_CONTENT_MIME_TYPE = "bodyContentMimeType";
    public static final String BODY_CONTENT_TYPE = "bodyContentType";
    public static final String DELETE = "delete";
    public static final String FOLLOW_ALL_REDIRECTS = "followAllRedirects";
    public static final String FOLLOW_REDIRECT = "followRedirect";
    public static final String FULL_RESPONSE = "fullResponse";
    public static final String HEAD = "head";
    public static final String HEADERS = "headers";
    public static final String HTTP_CLIENT = "httpClient";
    public static final String GET = "get";
    public static final String IGNORE_RESPONSE_CODE = "ignoreResponseCode";
    public static final String PATCH = "patch";
    public static final String POST = "post";
    public static final String PROXY = "proxy";
    public static final String PUT = "put";
    public static final String QUERY_PARAMETERS = "queryParameters";
    public static final String RESPONSE_FILENAME = "responseFilename";
    public static final String RESPONSE_FORMAT = "responseFormat";
    public static final String TIMEOUT = "timeout";
    public static final String URI = "uri";

    public static final List<Property<?>> BODY_CONTENT_PROPERTIES = Collections.unmodifiableList(
        Arrays.asList(
            object(BODY_CONTENT)
                .label("Body Content - JSON")
                .description("Body Parameters to send.")
                .displayCondition("%s === '%s'".formatted(BODY_CONTENT_TYPE, BodyContentType.JSON.name()))
                .additionalProperties(oneOf())
                .placeholder("Add Parameter"),
            object(BODY_CONTENT)
                .label("Body Content - XML")
                .description("XML content to send.")
                .displayCondition("%s === '%s'".formatted(BODY_CONTENT_TYPE, BodyContentType.XML.name()))
                .placeholder("Add Parameter"),
            object(BODY_CONTENT)
                .label("Body Content - Form Data")
                .description("Body parameters to send.")
                .displayCondition("%s === '%s'".formatted(BODY_CONTENT_TYPE, BodyContentType.FORM_DATA.name()))
                .placeholder("Add Parameter")
                .additionalProperties(oneOf().types(string(), fileEntry())),
            object(BODY_CONTENT)
                .label("Body Content - Form URL-Encoded")
                .description("Body parameters to send.")
                .displayCondition("%s === '%s'".formatted(BODY_CONTENT_TYPE, BodyContentType.FORM_URL_ENCODED.name()))
                .placeholder("Add Parameter")
                .additionalProperties(string()),
            string(BODY_CONTENT)
                .label("Body Content - Raw")
                .description("The raw text to send.")
                .displayCondition("%s === '%s'".formatted(BODY_CONTENT_TYPE, BodyContentType.RAW.name())),
            fileEntry(BODY_CONTENT)
                .label("Body Content - Binary")
                .description("The object property which contains a reference to the file to upload.")
                .displayCondition("%s === '%s'".formatted(BODY_CONTENT_TYPE, BodyContentType.BINARY.name()))));

    public static final List<Property<?>> OUTPUT_PROPERTIES = Collections.unmodifiableList(
        Arrays.asList(
            object().properties(oneOf("body").types(array(), object()), object("headers"), integer("status"))
                .displayCondition(
                    "['%s'].includes('%s') && %s === false".formatted(
                        ResponseFormat.JSON.name(), RESPONSE_FORMAT, FULL_RESPONSE)),
            object().properties(object("body"), object("headers"), integer("status"))
                .displayCondition(
                    "['%s'].includes('%s') && %s === false".formatted(
                        ResponseFormat.XML.name(), RESPONSE_FORMAT, FULL_RESPONSE)),
            oneOf()
                .types(array(), object())
                .displayCondition(
                    "['%s'].includes('%s') && %s === true".formatted(
                        ResponseFormat.JSON.name(), RESPONSE_FORMAT, FULL_RESPONSE)),
            object()
                .displayCondition(
                    "['%s'].includes('%s') && %s === true".formatted(
                        ResponseFormat.XML.name(), RESPONSE_FORMAT, FULL_RESPONSE)),
            string().displayCondition(
                "%s === '%s' && %s === true".formatted(RESPONSE_FORMAT, ResponseFormat.TEXT.name(), FULL_RESPONSE)),
            object().properties(string("body"), object("headers"), integer("status"))
                .displayCondition(
                    "%s === '%s' && %s === false".formatted(
                        RESPONSE_FORMAT, ResponseFormat.TEXT.name(), FULL_RESPONSE)),
            fileEntry()
                .displayCondition(
                    "%s === '%s' && %s === true".formatted(
                        RESPONSE_FORMAT, ResponseFormat.BINARY.name(), FULL_RESPONSE)),
            object().properties(fileEntry("body"), object("headers"), integer("status"))
                .displayCondition(
                    "%s === '%s' && %s === false".formatted(
                        RESPONSE_FORMAT, ResponseFormat.BINARY.name(), FULL_RESPONSE))));

    public static final List<Property<?>> COMMON_PROPERTIES = Collections.unmodifiableList(
        Arrays.asList(

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
                        "File", ResponseFormat.BINARY.name(),
                        "The response is returned as a file object."))
                .defaultValue(ResponseFormat.JSON.name()),
            string(RESPONSE_FILENAME)
                .label("Response Filename")
                .description("The name of the file if the response is returned as a file object.")
                .displayCondition("%s === '%s'".formatted(RESPONSE_FORMAT, ResponseFormat.BINARY.name())),

            //
            // Header properties
            //

            object(HEADERS)
                .label("Headers")
                .description("Headers to send.")
                .placeholder("Add header")
                .additionalProperties(string()),

            //
            // Query parameters properties
            //

            object(QUERY_PARAMETERS)
                .label("Query Parameters")
                .description("Query parameters to send.")
                .placeholder("Add parameter")
                .additionalProperties(string())));
}
