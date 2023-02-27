
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

import com.bytechef.hermes.component.util.HttpClientUtils;
import com.bytechef.hermes.definition.Property;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.bytechef.hermes.component.definition.ComponentDSL.fileEntry;
import static com.bytechef.hermes.definition.DefinitionDSL.array;
import static com.bytechef.hermes.definition.DefinitionDSL.bool;
import static com.bytechef.hermes.definition.DefinitionDSL.integer;
import static com.bytechef.hermes.definition.DefinitionDSL.object;
import static com.bytechef.hermes.definition.DefinitionDSL.oneOf;
import static com.bytechef.hermes.definition.DefinitionDSL.option;
import static com.bytechef.hermes.definition.DefinitionDSL.show;
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
    public static final String HEADER_PARAMETERS = "headerParameters";
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
                .label("JSON")
                .description("Body Parameters to send.")
                .displayOption(show(BODY_CONTENT_TYPE, HttpClientUtils.BodyContentType.JSON.name()))
                .additionalProperties(oneOf())
                .placeholder("Add Parameter"),
            object(BODY_CONTENT)
                .label("XML")
                .description("XML content to send.")
                .displayOption(show(BODY_CONTENT_TYPE, HttpClientUtils.BodyContentType.XML.name()))
                .additionalProperties(oneOf())
                .placeholder("Add Parameter"),
            object(BODY_CONTENT)
                .label("Form Data")
                .description("Body parameters to send.")
                .displayOption(show(BODY_CONTENT_TYPE, HttpClientUtils.BodyContentType.FORM_DATA.name()))
                .placeholder("Add Parameter")
                .additionalProperties(oneOf().types(string(), fileEntry())),
            object(BODY_CONTENT)
                .label("Form URL-Encoded")
                .description("Body parameters to send.")
                .displayOption(show(BODY_CONTENT_TYPE, HttpClientUtils.BodyContentType.FORM_URL_ENCODED.name()))
                .placeholder("Add Parameter")
                .additionalProperties(string()),
            string(BODY_CONTENT)
                .label("Raw")
                .description("The raw text to send.")
                .displayOption(show(BODY_CONTENT_TYPE, HttpClientUtils.BodyContentType.RAW.name())),
            fileEntry(BODY_CONTENT)
                .label("Binary")
                .description("The object property which contains a reference to the file to upload.")
                .displayOption(show(BODY_CONTENT_TYPE, HttpClientUtils.BodyContentType.BINARY.name()))));

    public static final List<Property<?>> OUTPUT_PROPERTIES = Collections.unmodifiableList(
        Arrays.asList(
            object().properties(oneOf("body").types(array(), object()), object("headers"), integer("status"))
                .displayOption(show(
                    RESPONSE_FORMAT,
                    List.of(HttpClientUtils.ResponseFormat.JSON.name(), HttpClientUtils.ResponseFormat.XML.name()),
                    FULL_RESPONSE,
                    List.of(false))),
            oneOf()
                .types(array(), object())
                .displayOption(show(
                    RESPONSE_FORMAT,
                    List.of(HttpClientUtils.ResponseFormat.JSON.name(), HttpClientUtils.ResponseFormat.XML.name()),
                    FULL_RESPONSE,
                    List.of(true))),
            string().displayOption(
                show(RESPONSE_FORMAT, List.of(HttpClientUtils.ResponseFormat.TEXT.name()), FULL_RESPONSE,
                    List.of(true))),
            object().properties(string("body"), object("headers"), integer("status"))
                .displayOption(
                    show(RESPONSE_FORMAT, List.of(HttpClientUtils.ResponseFormat.TEXT.name()), FULL_RESPONSE,
                        List.of(false))),
            fileEntry()
                .displayOption(
                    show(RESPONSE_FORMAT, List.of(HttpClientUtils.ResponseFormat.BINARY.name()), FULL_RESPONSE,
                        List.of(true))),
            object().properties(fileEntry("body"), object("headers"), integer("status"))
                .displayOption(
                    show(RESPONSE_FORMAT, List.of(HttpClientUtils.ResponseFormat.BINARY.name()), FULL_RESPONSE,
                        List.of(false)))));

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
                        HttpClientUtils.ResponseFormat.JSON.name(),
                        "The response is automatically converted to object/array."),
                    option(
                        "XML",
                        HttpClientUtils.ResponseFormat.XML.name(),
                        "The response is automatically converted to object/array."),
                    option("Text", HttpClientUtils.ResponseFormat.TEXT.name(), "The response is returned as a text."),
                    option(
                        "File", HttpClientUtils.ResponseFormat.BINARY.name(),
                        "The response is returned as a file object."))
                .defaultValue(HttpClientUtils.ResponseFormat.JSON.name()),
            string(RESPONSE_FILENAME)
                .label("Response Filename")
                .description("The name of the file if the response is returned as a file object.")
                .displayOption(show(RESPONSE_FORMAT, HttpClientUtils.ResponseFormat.BINARY.name())),

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
                .additionalProperties(string())));
}
