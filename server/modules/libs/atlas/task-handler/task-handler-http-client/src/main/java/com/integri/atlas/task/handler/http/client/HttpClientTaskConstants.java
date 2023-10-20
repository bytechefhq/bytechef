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

package com.integri.atlas.task.handler.http.client;

/**
 * @author Ivica Cardic
 */
public class HttpClientTaskConstants {

    public static final String PROPERTY_AUTHENTICATION_TYPE = "authenticationType";
    public static final String PROPERTY_REQUEST_METHOD = "requestMethod";
    public static final String PROPERTY_PROPERTY_URI = "uri";
    public static final String PROPERTY_ALLOW_UNAUTHORIZED_CERTS = "allowUnauthorizedCerts";
    public static final String PROPERTY_RESPONSE_FORMAT = "responseFormat";
    public static final String PROPERTY_RESPONSE_FILE_NAME = "responseFileName";
    public static final String PROPERTY_RAW_PARAMETERS = "rawParameters";
    public static final String PROPERTY_BODY_CONTENT_TYPE = "bodyContentType";
    public static final String PROPERTY_MIME_TYPE = "mimeType";
    public static final String PROPERTY_FULL_RESPONSE = "fullResponse";
    public static final String PROPERTY_FOLLOW_ALL_REDIRECTS = "followAllRedirects";
    public static final String PROPERTY_FOLLOW_REDIRECT = "followRedirect";
    public static final String PROPERTY_IGNORE_RESPONSE_CODE = "ignoreResponseCode";
    public static final String PROPERTY_PROXY = "proxy";
    public static final String PROPERTY_TIMEOUT = "timeout";
    public static final String PROPERTY_HEADER_PARAMETERS = "headerParameters";
    public static final String PROPERTY_PARAMETER = "parameter";
    public static final String PROPERTY_NAME = "name";
    public static final String PROPERTY_VALUE = "value";
    public static final String PROPERTY_QUERY_PARAMETERS = "queryParameters";
    public static final String PROPERTY_BODY_PARAMETERS = "bodyParameters";
    public static final String PROPERTY_FILE_ENTRY = "fileEntry";
    public static final String PROPERTY_HTTP_BASIC_AUTH = "httpBasicAuth";
    public static final String PROPERTY_HTTP_DIGEST_AUTH = "httpDigestAuth";
    public static final String PROPERTY_HTTP_BEARER_TOKEN = "httpBearerToken";
    public static final String PROPERTY_HTTP_API_KEY = "httpApiKey";
    public static final String PROPERTY_OAUTH2 = "oAuth2";
    public static final String TASK_HTTP_CLIENT = "httpClient";

    public enum AuthenticationType {
        HTTP_API_KEY,
        HTTP_BASIC_AUTH,
        HTTP_BEARER_TOKEN,
        HTTP_DIGEST_AUTH,
        OAUTH2,
    }

    public enum BodyContentType {
        JSON,
        RAW,
        FORM_DATA,
        FORM_URLENCODED,
        BINARY,
    }

    public enum ResponseFormat {
        JSON,
        XML,
        TEXT,
        FILE,
    }

    public enum RequestMethod {
        PATCH,
        POST,
        PUT,
        GET,
        HEAD,
        DELETE,
    }
}
