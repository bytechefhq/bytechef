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

package com.bytechef.component.definition;

import static com.bytechef.component.definition.WebhookResponse.Type.BINARY;
import static com.bytechef.component.definition.WebhookResponse.Type.JSON;
import static com.bytechef.component.definition.WebhookResponse.Type.NO_DATA;
import static com.bytechef.component.definition.WebhookResponse.Type.RAW;
import static com.bytechef.component.definition.WebhookResponse.Type.REDIRECT;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * Represents an HTTP response returned by a webhook action.
 * <p>
 * This class provides factory methods to construct various types of HTTP responses, including JSON, raw text, binary
 * data, redirects, and responses with no body content. Each response can be customized with HTTP headers and status
 * codes.
 * </p>
 *
 * <h2>Usage Examples:</h2>
 *
 * <pre>{@code
 * // Simple JSON response
 * WebhookResponse response = WebhookResponse.json(Map.of("status", "success"));
 *
 * // JSON with custom status code
 * WebhookResponse created = WebhookResponse.json(data, HttpStatus.CREATED);
 *
 * // JSON with headers and status
 * WebhookResponse customJson = WebhookResponse.json(
 *     data,
 *     Map.of("X-Custom-Header", "value"),
 *     HttpStatus.OK);
 *
 * // Raw text response
 * WebhookResponse text = WebhookResponse.raw("Plain text content");
 *
 * // Binary file response
 * WebhookResponse file = WebhookResponse.binary(fileEntry);
 *
 * // Redirect response
 * WebhookResponse redirect = WebhookResponse.redirect("https://example.com");
 *
 * // No content response with headers
 * WebhookResponse noContent = WebhookResponse.noData(
 *     Map.of("X-Request-Id", "123"),
 *     HttpStatus.NO_CONTENT);
 * }</pre>
 *
 * @author Ivica Cardic
 */
public class WebhookResponse {

    /**
     * Defines the type of content in the webhook response body.
     */
    public enum Type {
        /** JSON-formatted content that will be serialized to JSON */
        JSON,
        /** Raw text content (e.g., plain text, HTML, XML) */
        RAW,
        /** Binary file content */
        BINARY,
        /** HTTP redirect to another URL */
        REDIRECT,
        /** No response body content */
        NO_DATA
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

    /**
     * Creates a JSON response with HTTP 200 OK status.
     *
     * @param body the response body that will be serialized to JSON (typically a Map, List, or POJO)
     * @return a new WebhookResponse instance with JSON content type and 200 status
     */
    public static WebhookResponse json(Object body) {
        return new WebhookResponse(JSON, body, Map.of(), HttpStatus.OK.getValue());
    }

    /**
     * Creates a JSON response with custom headers and HTTP 200 OK status.
     *
     * @param body    the response body that will be serialized to JSON
     * @param headers custom HTTP headers to include in the response
     * @return a new WebhookResponse instance with JSON content type, custom headers, and 200 status
     */
    public static WebhookResponse json(Object body, Map<String, String> headers) {
        return new WebhookResponse(JSON, body, headers, HttpStatus.OK.getValue());
    }

    /**
     * Creates a JSON response with a custom status code.
     *
     * @param body       the response body that will be serialized to JSON
     * @param statusCode the HTTP status code (e.g., 201, 400, 500)
     * @return a new WebhookResponse instance with JSON content type and custom status code
     */
    public static WebhookResponse json(Object body, int statusCode) {
        return new WebhookResponse(JSON, body, Map.of(), statusCode);
    }

    /**
     * Creates a JSON response with a custom HTTP status.
     *
     * @param body   the response body that will be serialized to JSON
     * @param status the HTTP status enum value
     * @return a new WebhookResponse instance with JSON content type and custom status
     */
    public static WebhookResponse json(Object body, HttpStatus status) {
        return new WebhookResponse(JSON, body, Map.of(), status.getValue());
    }

    /**
     * Creates a JSON response with custom headers and status code.
     *
     * @param body       the response body that will be serialized to JSON
     * @param headers    custom HTTP headers to include in the response
     * @param statusCode the HTTP status code
     * @return a new WebhookResponse instance with JSON content type, custom headers, and status code
     */
    public static WebhookResponse json(Object body, Map<String, String> headers, int statusCode) {
        return new WebhookResponse(JSON, body, headers, statusCode);
    }

    /**
     * Creates a JSON response with custom headers and HTTP status.
     *
     * @param body    the response body that will be serialized to JSON
     * @param headers custom HTTP headers to include in the response
     * @param status  the HTTP status enum value
     * @return a new WebhookResponse instance with JSON content type, custom headers, and status
     */
    public static WebhookResponse json(Object body, Map<String, String> headers, HttpStatus status) {
        return new WebhookResponse(JSON, body, headers, status.getValue());
    }

    /**
     * Creates a raw text response with HTTP 200 OK status.
     * <p>
     * Use this method for plain text, HTML, XML, or any other string-based content.
     * </p>
     *
     * @param body the raw text content
     * @return a new WebhookResponse instance with raw content type and 200 status
     */
    public static WebhookResponse raw(String body) {
        return new WebhookResponse(RAW, body, Map.of(), HttpStatus.OK.getValue());
    }

    /**
     * Creates a raw text response with custom headers and HTTP 200 OK status.
     *
     * @param body    the raw text content
     * @param headers custom HTTP headers to include in the response
     * @return a new WebhookResponse instance with raw content type, custom headers, and 200 status
     */
    public static WebhookResponse raw(String body, Map<String, String> headers) {
        return new WebhookResponse(RAW, body, headers, HttpStatus.OK.getValue());
    }

    /**
     * Creates a raw text response with a custom status code.
     *
     * @param body   the raw text content
     * @param status the HTTP status code
     * @return a new WebhookResponse instance with raw content type and custom status code
     */
    public static WebhookResponse raw(String body, int status) {
        return new WebhookResponse(RAW, body, Map.of(), status);
    }

    /**
     * Creates a raw text response with a custom HTTP status.
     *
     * @param body   the raw text content
     * @param status the HTTP status enum value
     * @return a new WebhookResponse instance with raw content type and custom status
     */
    public static WebhookResponse raw(String body, HttpStatus status) {
        return new WebhookResponse(RAW, body, Map.of(), status.getValue());
    }

    /**
     * Creates a raw text response with custom headers and status code.
     *
     * @param body       the raw text content
     * @param headers    custom HTTP headers to include in the response
     * @param statusCode the HTTP status code
     * @return a new WebhookResponse instance with raw content type, custom headers, and status code
     */
    public static WebhookResponse raw(String body, Map<String, String> headers, int statusCode) {
        return new WebhookResponse(RAW, body, headers, statusCode);
    }

    /**
     * Creates a raw text response with custom headers and HTTP status.
     *
     * @param body    the raw text content
     * @param headers custom HTTP headers to include in the response
     * @param status  the HTTP status enum value
     * @return a new WebhookResponse instance with raw content type, custom headers, and status
     */
    public static WebhookResponse raw(String body, Map<String, String> headers, HttpStatus status) {
        return new WebhookResponse(RAW, body, headers, status.getValue());
    }

    /**
     * Creates a binary file response with HTTP 200 OK status.
     *
     * @param body the binary file content
     * @return a new WebhookResponse instance with binary content type and 200 status
     */
    public static WebhookResponse binary(FileEntry body) {
        return new WebhookResponse(BINARY, body, Map.of(), HttpStatus.OK.getValue());
    }

    /**
     * Creates a binary file response with custom headers and HTTP 200 OK status.
     *
     * @param body    the binary file content
     * @param headers custom HTTP headers to include in the response (e.g., Content-Type, Content-Disposition)
     * @return a new WebhookResponse instance with binary content type, custom headers, and 200 status
     */
    public static WebhookResponse binary(FileEntry body, Map<String, String> headers) {
        return new WebhookResponse(BINARY, body, headers, HttpStatus.OK.getValue());
    }

    /**
     * Creates a binary file response with custom headers and status code.
     *
     * @param body       the binary file content
     * @param headers    custom HTTP headers to include in the response
     * @param statusCode the HTTP status code
     * @return a new WebhookResponse instance with binary content type, custom headers, and status code
     */
    public static WebhookResponse binary(FileEntry body, Map<String, String> headers, int statusCode) {
        return new WebhookResponse(BINARY, body, headers, statusCode);
    }

    /**
     * Creates a binary file response with custom headers and HTTP status.
     *
     * @param body    the binary file content
     * @param headers custom HTTP headers to include in the response
     * @param status  the HTTP status enum value
     * @return a new WebhookResponse instance with binary content type, custom headers, and status
     */
    public static WebhookResponse binary(FileEntry body, Map<String, String> headers, HttpStatus status) {
        return new WebhookResponse(BINARY, body, headers, status.getValue());
    }

    /**
     * Creates an HTTP redirect response (302 Found).
     *
     * @param url the URL to redirect to
     * @return a new WebhookResponse instance with redirect type and 302 status
     */
    public static WebhookResponse redirect(String url) {
        return new WebhookResponse(REDIRECT, url, Map.of(), HttpStatus.FOUND.getValue());
    }

    /**
     * Creates a response with no body content and custom headers with HTTP 200 OK status.
     * <p>
     * Useful for responses that only need to return headers without a body.
     * </p>
     *
     * @param headers custom HTTP headers to include in the response
     * @return a new WebhookResponse instance with no body, custom headers, and 200 status
     */
    public static WebhookResponse noData(Map<String, String> headers) {
        return new WebhookResponse(NO_DATA, null, headers, 200);
    }

    /**
     * Creates a response with no body content and custom headers with a custom status code.
     * <p>
     * Useful for responses like 204 No Content or 304 Not Modified.
     * </p>
     *
     * @param headers    custom HTTP headers to include in the response
     * @param statusCode the HTTP status code (e.g., 204, 304)
     * @return a new WebhookResponse instance with no body, custom headers, and custom status code
     */
    public static WebhookResponse noData(Map<String, String> headers, int statusCode) {
        return new WebhookResponse(NO_DATA, null, headers, statusCode);
    }

    /**
     * Creates a response with no body content and custom headers with a custom HTTP status.
     *
     * @param headers custom HTTP headers to include in the response
     * @param status  the HTTP status enum value
     * @return a new WebhookResponse instance with no body, custom headers, and custom status
     */
    public static WebhookResponse noData(Map<String, String> headers, HttpStatus status) {
        return new WebhookResponse(NO_DATA, null, headers, status.getValue());
    }

    /**
     * Returns the response body.
     * <p>
     * The body type depends on the response type:
     * </p>
     * <ul>
     * <li>JSON: any object that can be serialized to JSON (Map, List, POJO, etc.)</li>
     * <li>RAW: String containing text content</li>
     * <li>BINARY: FileEntry containing binary file data</li>
     * <li>REDIRECT: String containing the redirect URL</li>
     * <li>NO_DATA: null</li>
     * </ul>
     *
     * @return the response body, or null for NO_DATA responses
     */
    public Object getBody() {
        return body;
    }

    /**
     * Returns an unmodifiable map of HTTP headers.
     *
     * @return immutable map of HTTP header names to values
     */
    public Map<String, String> getHeaders() {
        return Collections.unmodifiableMap(headers);
    }

    /**
     * Returns the HTTP status code.
     *
     * @return the HTTP status code (e.g., 200, 201, 404, 500)
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * Returns the response type.
     *
     * @return the type of content in the response body
     * @see Type
     */
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
