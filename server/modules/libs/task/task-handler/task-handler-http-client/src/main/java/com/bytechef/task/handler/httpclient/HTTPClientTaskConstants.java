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

package com.bytechef.task.handler.httpclient;

/**
 * @author Ivica Cardic
 */
public class HTTPClientTaskConstants {

    public static final String URI = "uri";
    public static final String ALLOW_UNAUTHORIZED_CERTS = "allowUnauthorizedCerts";
    public static final String RESPONSE_FORMAT = "responseFormat";
    public static final String RESPONSE_FILE_NAME = "responseFileName";
    public static final String BODY_CONTENT_TYPE = "bodyContentType";
    public static final String MIME_TYPE = "mimeType";
    public static final String FULL_RESPONSE = "fullResponse";
    public static final String FOLLOW_ALL_REDIRECTS = "followAllRedirects";
    public static final String FOLLOW_REDIRECT = "followRedirect";
    public static final String IGNORE_RESPONSE_CODE = "ignoreResponseCode";
    public static final String PROXY = "proxy";
    public static final String TIMEOUT = "timeout";
    public static final String HEADER_PARAMETERS = "headerParameters";
    public static final String PARAMETER = "parameter";
    public static final String KEY = "key";
    public static final String VALUE = "value";
    public static final String QUERY_PARAMETERS = "queryParameters";
    public static final String BODY_PARAMETERS = "bodyParameters";
    public static final String FILE_ENTRY = "fileEntry";
    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";
    public static final String HTTP_CLIENT = "httpClient";
    public static final String TOKEN = "token";
    public static final String ADD_TO = "addTo";
    public static final String GET = "get";
    public static final String POST = "post";
    public static final String PUT = "put";
    public static final String PATCH = "patch";
    public static final String DELETE = "delete";
    public static final String HEAD = "head";
    public static final String SEND_FILE = "sendFile";
    public static final String ACCESS_TOKEN = "accessToken";
    public static final String HEADER_PREFIX = "headerPrefix";
    public static final float VERSION_1_0 = 1.0f;

    public enum ApiTokenLocation {
        HEADER,
        QUERY_PARAMS,
    }

    public enum AuthType {
        API_KEY,
        BASIC_AUTH,
        BEARER_TOKEN,
        DIGEST_AUTH,
        OAUTH2,
    }

    public enum BodyContentType {
        BINARY,
        FORM_DATA,
        FORM_URLENCODED,
        JSON,
        RAW,
        XML
    }

    public enum ResponseFormat {
        FILE,
        JSON,
        TEXT,
        XML,
    }

    public enum RequestMethod {
        DELETE,
        GET,
        HEAD,
        PATCH,
        POST,
        PUT,
    }
}
