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

import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.ResponseType;

import com.bytechef.component.definition.Property;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author Ivica Cardic
 */
public class HttpClientComponentConstants {

    public static final String ALLOW_UNAUTHORIZED_CERTS = "allowUnauthorizedCerts";
    public static final String BODY = "body";
    public static final String BODY_CONTENT = "bodyContent";
    public static final String BODY_CONTENT_MIME_TYPE = "bodyContentMimeType";
    public static final String BODY_CONTENT_TYPE = "bodyContentType";
    public static final String FOLLOW_ALL_REDIRECTS = "followAllRedirects";
    public static final String FOLLOW_REDIRECT = "followRedirect";
    public static final String FULL_RESPONSE = "fullResponse";
    public static final String HEADERS = "headers";
    public static final String IGNORE_RESPONSE_CODE = "ignoreResponseCode";
    public static final String PROXY = "proxy";
    public static final String QUERY_PARAMETERS = "queryParameters";
    public static final String RESPONSE_FILENAME = "responseFilename";
    public static final String RESPONSE_FORMAT = "responseType";
    public static final String TIMEOUT = "timeout";
    public static final String URI = "uri";

    public static final List<? extends Property> COMMON_PROPERTIES = Collections.unmodifiableList(
        Arrays.asList(

            //
            // General properties
            //

            string(URI)
                .label("URI")
                .description(
                    "The URI to make the request to. If HTTP Client Connection defines Base URI, then this value is appended to it.")
                .exampleValue("/")
                .placeholder("https://example.com/index.html")
                .defaultValue("")
                .required(true),
            bool(ALLOW_UNAUTHORIZED_CERTS)
                .label("Allow Unauthorized Certs")
                .description("Download the response even if SSL certificate validation is not possible.")
                .defaultValue(false)
                .advancedOption(true),
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
                        ResponseType.XML.name(),
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
