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

package com.bytechef.platform.component.definition;

import static com.bytechef.platform.component.definition.WebhookResponse.Type.BINARY;
import static com.bytechef.platform.component.definition.WebhookResponse.Type.JSON;
import static com.bytechef.platform.component.definition.WebhookResponse.Type.NO_DATA;
import static com.bytechef.platform.component.definition.WebhookResponse.Type.RAW;
import static com.bytechef.platform.component.definition.WebhookResponse.Type.REDIRECT;

import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.HttpStatus;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * @author Ivica Cardic
 */
public class WebhookResponse {

    public enum Type {
        JSON, RAW, BINARY, REDIRECT, NO_DATA
    }

    private Object body;
    private Map<String, String> headers = Map.of();
    private int statusCode;
    private Type type;

    private WebhookResponse() {
    }

    private WebhookResponse(Type type, Object body, Map<String, String> headers, int statusCode) {
        this.type = type;
        this.body = body;
        this.headers = Collections.unmodifiableMap(headers);
        this.statusCode = statusCode;
    }

    public static WebhookResponse json(Object body) {
        return new WebhookResponse(JSON, body, Map.of(), HttpStatus.OK.getValue());
    }

    public static WebhookResponse json(Object body, Map<String, String> headers) {
        return new WebhookResponse(JSON, body, headers, HttpStatus.OK.getValue());
    }

    public static WebhookResponse json(Object body, int statusCode) {
        return new WebhookResponse(JSON, body, Map.of(), statusCode);
    }

    public static WebhookResponse json(Object body, HttpStatus status) {
        return new WebhookResponse(JSON, body, Map.of(), status.getValue());
    }

    public static WebhookResponse json(Object body, Map<String, String> headers, int statusCode) {
        return new WebhookResponse(JSON, body, headers, statusCode);
    }

    public static WebhookResponse json(Object body, Map<String, String> headers, HttpStatus status) {
        return new WebhookResponse(JSON, body, headers, status.getValue());
    }

    public static WebhookResponse raw(String body) {
        return new WebhookResponse(RAW, body, Map.of(), HttpStatus.OK.getValue());
    }

    public static WebhookResponse raw(String body, Map<String, String> headers) {
        return new WebhookResponse(RAW, body, headers, HttpStatus.OK.getValue());
    }

    public static WebhookResponse raw(String body, int status) {
        return new WebhookResponse(RAW, body, Map.of(), status);
    }

    public static WebhookResponse raw(String body, HttpStatus status) {
        return new WebhookResponse(RAW, body, Map.of(), status.getValue());
    }

    public static WebhookResponse raw(String body, Map<String, String> headers, int statusCode) {
        return new WebhookResponse(RAW, body, headers, statusCode);
    }

    public static WebhookResponse raw(String body, Map<String, String> headers, HttpStatus status) {
        return new WebhookResponse(RAW, body, headers, status.getValue());
    }

    public static WebhookResponse binary(FileEntry body) {
        return new WebhookResponse(BINARY, body, Map.of(), HttpStatus.OK.getValue());
    }

    public static WebhookResponse binary(FileEntry body, Map<String, String> headers) {
        return new WebhookResponse(BINARY, body, headers, HttpStatus.OK.getValue());
    }

    public static WebhookResponse binary(FileEntry body, Map<String, String> headers, int statusCode) {
        return new WebhookResponse(BINARY, body, headers, statusCode);
    }

    public static WebhookResponse binary(FileEntry body, Map<String, String> headers, HttpStatus status) {
        return new WebhookResponse(BINARY, body, headers, status.getValue());
    }

    public static WebhookResponse redirect(String url) {
        return new WebhookResponse(REDIRECT, url, Map.of(), HttpStatus.FOUND.getValue());
    }

    public static WebhookResponse noData(Map<String, String> headers) {
        return new WebhookResponse(NO_DATA, null, headers, 200);
    }

    public static WebhookResponse noData(Map<String, String> headers, int statusCode) {
        return new WebhookResponse(NO_DATA, null, headers, statusCode);
    }

    public static WebhookResponse noData(Map<String, String> headers, HttpStatus status) {
        return new WebhookResponse(NO_DATA, null, headers, status.getValue());
    }

    public Object getBody() {
        return body;
    }

    public Map<String, String> getHeaders() {
        return Collections.unmodifiableMap(headers);
    }

    public int getStatusCode() {
        return statusCode;
    }

    public Type getType() {
        return type;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }

        var that = (WebhookResponse) obj;

        return Objects.equals(this.body, that.body) && Objects.equals(this.headers, that.headers) &&
            this.statusCode == that.statusCode && Objects.equals(this.type, that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, body, headers, statusCode);
    }

    @Override
    public String toString() {
        return "WebhookResponse[" +
            "type=" + type + ", " +
            "body=" + body + ", " +
            "headers=" + headers + ", " +
            "statusCode=" + statusCode + ']';
    }
}
