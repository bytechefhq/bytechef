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

package com.bytechef.component.http.client.constant;

import static com.bytechef.component.definition.ComponentDSL.array;
import static com.bytechef.component.definition.ComponentDSL.bool;
import static com.bytechef.component.definition.ComponentDSL.date;
import static com.bytechef.component.definition.ComponentDSL.dateTime;
import static com.bytechef.component.definition.ComponentDSL.fileEntry;
import static com.bytechef.component.definition.ComponentDSL.integer;
import static com.bytechef.component.definition.ComponentDSL.nullable;
import static com.bytechef.component.definition.ComponentDSL.number;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.option;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.definition.ComponentDSL.time;
import static com.bytechef.component.definition.Context.Http.BodyContentType;
import static com.bytechef.component.definition.Context.Http.ResponseType;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Property;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author Ivica Cardic
 */
public class HttpClientComponentConstants {

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
    public static final String RESPONSE_FORMAT = "responseType";
    public static final String TIMEOUT = "timeout";
    public static final String URI = "uri";

    public static final List<? extends Property> BODY_CONTENT_PROPERTIES = Collections.unmodifiableList(
        Arrays.asList(
            object(BODY_CONTENT)
                .label("Body Content - JSON")
                .description("Body Parameters to send.")
                .displayCondition("%s == '%s'".formatted(BODY_CONTENT_TYPE, BodyContentType.JSON.name()))
                .additionalProperties(
                    array(), bool(), date(), dateTime(), integer(), nullable(), number(), object(), string(), time())
                .placeholder("Add Parameter")
                .advancedOption(true),
            object(BODY_CONTENT)
                .label("Body Content - XML")
                .description("XML content to send.")
                .displayCondition("%s == '%s'".formatted(BODY_CONTENT_TYPE, BodyContentType.XML.name()))
                .placeholder("Add Parameter")
                .advancedOption(true),
            object(BODY_CONTENT)
                .label("Body Content - Form Data")
                .description("Body parameters to send.")
                .displayCondition("%s == '%s'".formatted(BODY_CONTENT_TYPE, BodyContentType.FORM_DATA.name()))
                .placeholder("Add Parameter")
                .additionalProperties(string(), fileEntry())
                .advancedOption(true),
            object(BODY_CONTENT)
                .label("Body Content - Form URL-Encoded")
                .description("Body parameters to send.")
                .displayCondition("%s == '%s'".formatted(BODY_CONTENT_TYPE, BodyContentType.FORM_URL_ENCODED.name()))
                .placeholder("Add Parameter")
                .additionalProperties(string())
                .advancedOption(true),
            string(BODY_CONTENT)
                .label("Body Content - Raw")
                .description("The raw text to send.")
                .displayCondition("%s == '%s'".formatted(BODY_CONTENT_TYPE, BodyContentType.RAW.name()))
                .advancedOption(true),
            fileEntry(BODY_CONTENT)
                .label("Body Content - Binary")
                .description("The object property which contains a reference to the file to upload.")
                .displayCondition("%s == '%s'".formatted(BODY_CONTENT_TYPE, BodyContentType.BINARY.name()))
                .advancedOption(true)));

    public static final List<? extends Property> COMMON_PROPERTIES = Collections.unmodifiableList(
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
                        ResponseType.JSON.name(),
                        "The response is automatically converted to object/array."),
                    option(
                        "XML",
                        Context.Http.ResponseType.XML.name(),
                        "The response is automatically converted to object/array."),
                    option("Text", ResponseType.TEXT.name(), "The response is returned as a text."),
                    option(
                        "File", ResponseType.BINARY.name(),
                        "The response is returned as a file object."))
                .defaultValue(ResponseType.JSON.name()),
            string(RESPONSE_FILENAME)
                .label("Response Filename")
                .description("The name of the file if the response is returned as a file object.")
                .displayCondition("%s == '%s'".formatted(RESPONSE_FORMAT, ResponseType.BINARY.name())),

            //
            // Header properties
            //

            object(HEADERS)
                .label("Headers")
                .description("Headers to send.")
                .placeholder("Add header")
                .additionalProperties(array().items(string())),

            //
            // Query parameters properties
            //

            object(QUERY_PARAMETERS)
                .label("Query Parameters")
                .description("Query parameters to send.")
                .placeholder("Add parameter")
                .additionalProperties(array().items(string()))));
}
