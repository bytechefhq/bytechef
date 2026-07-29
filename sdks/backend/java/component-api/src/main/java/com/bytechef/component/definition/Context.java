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

import com.bytechef.component.definition.Context.Http.Configuration.ConfigurationBuilder;
import com.bytechef.component.definition.Property.ValueProperty;
import com.bytechef.definition.BaseProperty;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;

/**
 * Provides access to the runtime utilities and services available to components while they execute, including type
 * conversion, encoding, HTTP calls, JSON and XML processing, logging, MIME-type lookup, and output-schema resolution.
 * Each utility is exposed through a function-accepting accessor so that the framework can manage the lifecycle of the
 * underlying utility instance.
 *
 * @author Ivica Cardic
 * @author Igor Beslic
 */
public interface Context {

    /**
     * Applies the given function to a {@link Converter} instance that performs type conversions.
     *
     * @param converterFunction the function to apply to the {@link Converter} utilities
     * @param <R>               the type of the result produced by the function
     * @return the result of applying the function
     */
    <R> R converter(ContextFunction<Converter, R> converterFunction);

    /**
     * Applies the given function to an {@link Encoder} instance that performs Base64 and URL encoding and decoding.
     *
     * @param encoderFunction the function to apply to the {@link Encoder} utilities
     * @param <R>             the type of the result produced by the function
     * @return the result of applying the function
     */
    <R> R encoder(ContextFunction<Encoder, R> encoderFunction);

    /**
     * Applies the given function to an {@link Escaper} instance that performs string escaping.
     *
     * @param escaperFunction the function to apply to the {@link Escaper} utilities
     * @param <R>             the type of the result produced by the function
     * @return the result of applying the function
     */
    <R> R escaper(ContextFunction<Escaper, R> escaperFunction);

    /**
     * Applies the given function to a {@link File} instance that reads and writes file content backed by the platform's
     * file storage.
     *
     * @param fileFunction the function to apply to the {@link File} utilities
     * @param <R>          the type of the result produced by the function
     * @return the result of applying the function
     */
    <R> R file(ContextFunction<File, R> fileFunction);

    /**
     * Applies the given function to an {@link Http} instance used to build and execute HTTP requests.
     *
     * @param httpFunction the function to apply to the {@link Http} utilities
     * @param <R>          the type of the result produced by the function
     * @return the result of applying the function
     */
    <R> R http(ContextFunction<Http, R> httpFunction);

    /**
     * Determines whether the current environment is the editor environment.
     *
     * @return true if the current environment is an editor environment, false otherwise.
     */
    boolean isEditorEnvironment();

    /**
     * Applies the given function to a {@link Json} instance used to read and write JSON.
     *
     * @param jsonFunction the function to apply to the {@link Json} utilities
     * @param <R>          the type of the result produced by the function
     * @return the result of applying the function
     */
    <R> R json(ContextFunction<Json, R> jsonFunction);

    /**
     * Passes a {@link Log} instance to the given consumer, allowing the component to emit log messages.
     *
     * @param logConsumer the consumer that emits log messages using the provided {@link Log} instance
     */
    void log(ContextConsumer<Log> logConsumer);

    /**
     * Applies the given function to a {@link MimeType} instance used to look up MIME types and file extensions.
     *
     * @param mimeTypeFunction the function to apply to the {@link MimeType} utilities
     * @param <R>              the type of the result produced by the function
     * @return the result of applying the function
     */
    <R> R mimeType(ContextFunction<MimeType, R> mimeTypeFunction);

    /**
     * Applies the given function to an {@link OutputSchema} instance used to derive output schemas and sample output.
     *
     * @param outputSchemaFunction the function to apply to the {@link OutputSchema} utilities
     * @param <R>                  the type of the result produced by the function
     * @return the result of applying the function
     */
    <R> R outputSchema(ContextFunction<OutputSchema, R> outputSchemaFunction);

    /**
     * Applies the given function to an {@link Xml} instance used to read and write XML.
     *
     * @param xmlFunction the function to apply to the {@link Xml} utilities
     * @param <R>         the type of the result produced by the function
     * @return the result of applying the function
     */
    <R> R xml(ContextFunction<Xml, R> xmlFunction);

    /**
     * A consumer that accepts a single input and may throw a checked exception.
     *
     * @param <T> the type of the input to the consumer
     */
    @FunctionalInterface
    interface ContextConsumer<T> {

        /**
         * Performs this operation on the given input.
         *
         * @param t the input to consume
         * @throws Exception if an error occurs while consuming the input
         */
        void accept(T t) throws Exception;
    }

    /**
     * A function that accepts a single input, produces a result, and may throw a checked exception.
     *
     * @param <T> the type of the input to the function
     * @param <R> the type of the result of the function
     */
    @FunctionalInterface
    interface ContextFunction<T, R> {

        /**
         * Applies this function to the given input.
         *
         * @param t the input to the function
         * @return the function result
         * @throws Exception if an error occurs while applying the function
         */
        R apply(T t) throws Exception;
    }

    /**
     * Provides type-conversion utilities for coercing values from one type to another.
     */
    interface Converter {

        /**
         * Determines whether a value can be converted to the given target type.
         *
         * @param fromValue   the value to convert
         * @param toValueType the target type
         * @return {@code true} if the value can be converted to the target type, {@code false} otherwise
         */
        boolean canConvert(Object fromValue, Class<?> toValueType);

        /**
         * Converts a value to the given target type.
         *
         * @param <T>         the target type
         * @param fromValue   the value to convert
         * @param toValueType the class representing the target type
         * @return the converted value
         */
        <T> T value(Object fromValue, Class<T> toValueType);

        /**
         * Converts a value to the target type described by the given type reference, preserving generic type
         * information.
         *
         * @param <T>            the target type
         * @param fromValue      the value to convert
         * @param toValueTypeRef the type reference describing the target type
         * @return the converted value
         */
        <T> T value(Object fromValue, TypeReference<T> toValueTypeRef);

        /**
         * Parses the given string into its most appropriate object representation.
         *
         * @param str the string to parse
         * @return the parsed value
         */
        Object string(String str);

    }

    /**
     * Provides Base64 and Base64-URL encoding and decoding utilities.
     */
    interface Encoder {

        /**
         * Decodes a Base64-encoded string into its raw bytes.
         *
         * @param string the Base64-encoded string
         * @return the decoded bytes
         */
        byte[] base64Decode(String string);

        /**
         * Encodes concatenated values as one Base64 encoded value
         *
         * @param values the values to concatenate and encode
         * @return base64 encoded value
         */
        String base64Encode(String... values);

        /**
         * Encodes the given bytes as a Base64 string.
         *
         * @param bytes the bytes to encode
         * @return the Base64-encoded value
         */
        String base64Encode(byte[] bytes);

        /**
         * Decodes parameter value which is Base64 encoded URL encoded value
         *
         * @param value the base64 encoded value
         * @return human understandable string value
         */
        byte[] base64UrlDecode(String value);

        /**
         * Encodes parameter value in URL encoding and applies Base64 encoding to the final result.
         *
         * @param value the value to be encoded
         * @return Base64 encoded value of URL encoded input
         */
        String base64UrlEncode(String value);

        /**
         * Encodes the given bytes as a Base64-URL string.
         *
         * @param bytes the bytes to encode
         * @return the Base64-URL-encoded value
         */
        String base64UrlEncode(byte[] bytes);

    }

    /**
     * Provides string-escaping utilities.
     */
    interface Escaper {

        /**
         * Escapes special characters in the given string so that it is safe to embed within HTML.
         *
         * @param html the string to escape
         * @return the HTML-escaped string
         */
        String escapeHtml(String html);
    }

    /**
     * Provides utilities for reading and writing file content backed by the platform's file storage, operating on
     * {@link FileEntry} references.
     */
    interface File {

        /**
         * Returns the length, in bytes, of the content referenced by the given file entry.
         *
         * @param fileEntry the file entry whose content length is returned
         * @return the content length in bytes
         */
        long getContentLength(FileEntry fileEntry);

        /**
         * Opens an input stream over the content referenced by the given file entry.
         *
         * @param fileEntry the file entry whose content is read
         * @return an input stream over the file content
         */
        InputStream getInputStream(FileEntry fileEntry);

        /**
         * Opens an output stream for writing to the content referenced by the given file entry.
         *
         * @param fileEntry the file entry whose content is written
         * @return an output stream over the file content
         */
        OutputStream getOutputStream(FileEntry fileEntry);

        /**
         * Reads the entire content referenced by the given file entry as a string.
         *
         * @param fileEntry the file entry whose content is read
         * @return the file content as a string
         */
        String readToString(FileEntry fileEntry);

        /**
         * Stores the content read from the given input stream under the specified file name.
         *
         * @param fileName    the name to assign to the stored file
         * @param inputStream the stream supplying the content to store
         * @return a {@link FileEntry} referencing the stored content
         * @throws IOException if an error occurs while storing the content
         */
        FileEntry storeContent(String fileName, InputStream inputStream) throws IOException;

        /**
         * Stores the given string data under the specified file name.
         *
         * @param fileName the name to assign to the stored file
         * @param data     the string content to store
         * @return a {@link FileEntry} referencing the stored content
         * @throws IOException if an error occurs while storing the content
         */
        FileEntry storeContent(String fileName, String data) throws IOException;

        /**
         * Materializes the content referenced by the given file entry into a temporary file on the local filesystem.
         *
         * @param fileEntry the file entry whose content is written to a temporary file
         * @return the temporary {@link java.io.File}
         */
        java.io.File toTempFile(FileEntry fileEntry);

        /**
         * Materializes the content referenced by the given file entry into a temporary file and returns its path.
         *
         * @param fileEntry the file entry whose content is written to a temporary file
         * @return the {@link Path} of the temporary file
         */
        Path toTempFilePath(FileEntry fileEntry);

        /**
         * Reads the entire content referenced by the given file entry into a byte array.
         *
         * @param fileEntry the file entry whose content is read
         * @return the file content as a byte array
         * @throws IOException if an error occurs while reading the content
         */
        byte[] readAllBytes(FileEntry fileEntry) throws IOException;
    }

    /**
     * Provides utilities for building and executing HTTP requests and reading their responses.
     */
    interface Http {

        /**
         * Enumerates the supported content encodings for an HTTP request body.
         */
        enum BodyContentType {
            BINARY,
            FORM_DATA,
            FORM_URL_ENCODED,
            JSON,
            RAW,
            XML
        }

        /**
         * Describes the expected type and content type of an HTTP response, controlling how the response body is
         * deserialized.
         */
        class ResponseType {

            public enum Type {
                BINARY,
                JSON,
                TEXT,
                XML
            }

            public static final ResponseType BINARY = new ResponseType("application/octet-stream", Type.BINARY);
            public static final ResponseType JSON = new ResponseType("application/json", Type.JSON);
            public static final ResponseType TEXT = new ResponseType("text/plain", Type.TEXT);
            public static final ResponseType XML = new ResponseType("application/xml", Type.XML);

            private String contentType;
            private Type type;

            private ResponseType() {
            }

            @Override
            public int hashCode() {
                return Objects.hash(contentType, type);
            }

            @Override
            public boolean equals(Object obj) {
                if (this == obj)
                    return true;
                if (obj == null || getClass() != obj.getClass())
                    return false;
                ResponseType other = (ResponseType) obj;
                return Objects.equals(contentType, other.contentType) && type == other.type;
            }

            private ResponseType(String contentType, Type type) {
                this.contentType = contentType;
                this.type = type;
            }

            public static ResponseType binary(String contentType) {
                return new ResponseType(contentType, Type.BINARY);
            }

            public static ResponseType valueOf(String string) {
                return switch (string) {
                    case "BINARY" -> BINARY;
                    case "JSON" -> JSON;
                    case "TEXT" -> TEXT;
                    case "XML" -> XML;
                    default -> throw new IllegalArgumentException("Unsupported response type: " + string);
                };
            }

            public String getContentType() {
                return contentType;
            }

            public Type getType() {
                return type;
            }
        }

        /**
         * Enumerates the supported HTTP request methods.
         */
        enum RequestMethod {
            DELETE,
            GET,
            HEAD,
            PATCH,
            POST,
            PUT,
        }

        /**
         * Begins building an HTTP {@code DELETE} request to the given URL.
         *
         * @param url the request URL
         * @return an {@link Executor} for configuring and executing the request
         */
        Executor delete(String url);

        /**
         * Begins building an HTTP request to the given URL using the specified request method.
         *
         * @param url           the request URL
         * @param requestMethod the HTTP method to use
         * @return an {@link Executor} for configuring and executing the request
         */
        Executor exchange(String url, RequestMethod requestMethod);

        /**
         * Begins building an HTTP {@code HEAD} request to the given URL.
         *
         * @param url the request URL
         * @return an {@link Executor} for configuring and executing the request
         */
        Executor head(String url);

        /**
         * Begins building an HTTP {@code GET} request to the given URL.
         *
         * @param url the request URL
         * @return an {@link Executor} for configuring and executing the request
         */
        Executor get(String url);

        /**
         * Begins building an HTTP {@code PATCH} request to the given URL.
         *
         * @param url the request URL
         * @return an {@link Executor} for configuring and executing the request
         */
        Executor patch(String url);

        /**
         * Begins building an HTTP {@code POST} request to the given URL.
         *
         * @param url the request URL
         * @return an {@link Executor} for configuring and executing the request
         */
        Executor post(String url);

        /**
         * Begins building an HTTP {@code PUT} request to the given URL.
         *
         * @param url the request URL
         * @return an {@link Executor} for configuring and executing the request
         */
        Executor put(String url);

        /**
         * Creates a configuration builder that controls whether requests may accept unauthorized (untrusted) TLS
         * certificates.
         *
         * @param allowUnauthorizedCerts whether to accept unauthorized TLS certificates
         * @return a {@link ConfigurationBuilder} initialized with the given setting
         */
        static ConfigurationBuilder allowUnauthorizedCerts(boolean allowUnauthorizedCerts) {
            ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();

            configurationBuilder.allowUnauthorizedCerts = allowUnauthorizedCerts;

            return configurationBuilder;
        }

        /**
         * Creates a configuration builder that controls whether requests follow redirects for all request methods.
         *
         * @param followAllRedirects whether to follow redirects for all request methods
         * @return a {@link ConfigurationBuilder} initialized with the given setting
         */
        static ConfigurationBuilder followAllRedirects(boolean followAllRedirects) {
            ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();

            configurationBuilder.followAllRedirects = followAllRedirects;

            return configurationBuilder;
        }

        /**
         * Creates a configuration builder that sets the file name used when the response is stored as a file.
         *
         * @param filename the file name to assign to the response content
         * @return a {@link ConfigurationBuilder} initialized with the given setting
         */
        static ConfigurationBuilder filename(String filename) {
            ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();

            configurationBuilder.filename = filename;

            return configurationBuilder;
        }

        /**
         * Creates a configuration builder that controls whether requests follow a redirect response.
         *
         * @param followRedirect whether to follow a redirect response
         * @return a {@link ConfigurationBuilder} initialized with the given setting
         */
        static ConfigurationBuilder followRedirect(boolean followRedirect) {
            ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();

            configurationBuilder.followRedirect = followRedirect;

            return configurationBuilder;
        }

        /**
         * Creates a configuration builder that routes requests through the given proxy.
         *
         * @param proxy the proxy specification to route requests through
         * @return a {@link ConfigurationBuilder} initialized with the given setting
         */
        static ConfigurationBuilder proxy(String proxy) {
            ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();

            configurationBuilder.proxy = proxy;

            return configurationBuilder;
        }

        /**
         * Creates a configuration builder that sets the expected {@link ResponseType} of the response.
         *
         * @param responseType the expected response type
         * @return a {@link ConfigurationBuilder} initialized with the given setting
         */
        static ConfigurationBuilder responseType(ResponseType responseType) {
            ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();

            configurationBuilder.responseType = responseType;

            return configurationBuilder;
        }

        /**
         * Creates a configuration builder that sets the request timeout.
         *
         * @param timeout the maximum duration to wait for the request to complete
         * @return a {@link ConfigurationBuilder} initialized with the given setting
         */
        static ConfigurationBuilder timeout(Duration timeout) {
            ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();

            configurationBuilder.timeout = timeout;

            return configurationBuilder;
        }

        /**
         * Creates a configuration builder that controls whether the connection's authorization is applied to the
         * request.
         *
         * @param disableAuthorization whether to omit applying the connection's authorization to the request
         * @return a {@link ConfigurationBuilder} initialized with the given setting
         */
        static ConfigurationBuilder disableAuthorization(boolean disableAuthorization) {
            ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();

            configurationBuilder.disableAuthorization = disableAuthorization;

            return configurationBuilder;
        }

        /**
         * Represents the body of an HTTP request, pairing the content with its content type and optional MIME type.
         */
        class Body {

            private final Object content;
            private final BodyContentType contentType;
            private final String mimeType;

            private Body(Object content, BodyContentType contentType) {
                this(content, contentType, null);
            }

            private Body(Object content, BodyContentType contentType, String mimeType) {
                this.content = content;
                this.contentType = contentType;
                this.mimeType = mimeType;
            }

            /**
             * Creates a binary request body from the given file entry.
             *
             * @param content the file entry supplying the binary content
             * @return a new {@link Body} carrying the binary content
             */
            public static Body of(FileEntry content) {
                return new Body(content, BodyContentType.BINARY);
            }

            /**
             * Creates a binary request body from the given file entry with an explicit MIME type.
             *
             * @param content  the file entry supplying the binary content
             * @param mimeType the MIME type of the content
             * @return a new {@link Body} carrying the binary content
             */
            public static Body of(FileEntry content, String mimeType) {
                Objects.requireNonNull(content);

                return new Body(content, BodyContentType.BINARY, mimeType);
            }

            /**
             * Creates a JSON request body from the given list.
             *
             * @param content the list to serialize as the JSON body
             * @return a new {@link Body} carrying the JSON content
             */
            public static Body of(List<?> content) {
                Objects.requireNonNull(content);

                return new Body(content, BodyContentType.JSON);
            }

            /**
             * Creates a request body from the given list using the specified content type.
             *
             * @param content     the list serving as the body content
             * @param contentType the encoding to apply to the body
             * @return a new {@link Body} carrying the content
             */
            public static Body of(List<?> content, BodyContentType contentType) {
                Objects.requireNonNull(content);
                Objects.requireNonNull(contentType);

                return new Body(content, contentType);
            }

            /**
             * Creates a JSON request body from the given map.
             *
             * @param content the map to serialize as the JSON body
             * @return a new {@link Body} carrying the JSON content
             */
            public static Body of(Map<String, ?> content) {
                Objects.requireNonNull(content);

                return new Body(content, BodyContentType.JSON);
            }

            /**
             * Creates a JSON request body from an alternating array of keys and values, where {@code null} keys or
             * values are omitted.
             *
             * @param keyValueArray an even-length array of alternating string keys and their values
             * @return a new {@link Body} carrying the JSON content
             */
            public static Body of(Object... keyValueArray) {
                Map<String, ?> content = getStringObjectMap(keyValueArray);

                return new Body(content, BodyContentType.JSON);
            }

            /**
             * Creates a JSON request body wrapping an alternating array of keys and values under a single top-level
             * name.
             *
             * @param name          the top-level key under which the built map is nested
             * @param keyValueArray an even-length array of alternating string keys and their values
             * @return a new {@link Body} carrying the JSON content
             */
            public static Body of(String name, Object[] keyValueArray) {
                HashMap<String, ?> content = getStringObjectMap(keyValueArray);

                return new Body(Map.of(name, content), BodyContentType.JSON);
            }

            private static HashMap<String, ?> getStringObjectMap(Object... keyValueArray) {
                Objects.requireNonNull(keyValueArray);

                if (keyValueArray.length % 2 != 0) {
                    throw new IllegalArgumentException();
                }

                return IntStream
                    .range(0, keyValueArray.length / 2)
                    .filter(i -> keyValueArray[i * 2] != null && keyValueArray[i * 2 + 1] != null)
                    .collect(
                        HashMap::new,
                        (map, i) -> map.put(String.valueOf(keyValueArray[i * 2]), keyValueArray[i * 2 + 1]),
                        HashMap::putAll);
            }

            /**
             * Creates a request body from the given map using the specified content type.
             *
             * @param content     the map serving as the body content
             * @param contentType the encoding to apply to the body
             * @return a new {@link Body} carrying the content
             */
            public static Body of(Map<String, ?> content, BodyContentType contentType) {
                Objects.requireNonNull(content);
                Objects.requireNonNull(contentType);

                return new Body(content, contentType);
            }

            /**
             * Creates a raw request body from the given string using the {@code text/plain} MIME type.
             *
             * @param content the raw string content
             * @return a new {@link Body} carrying the raw content
             */
            public static Body of(String content) {
                Objects.requireNonNull(content);

                return new Body(content, BodyContentType.RAW, "text/plain");
            }

            /**
             * Creates a raw request body from the given string with an explicit MIME type.
             *
             * @param content  the raw string content
             * @param mimeType the MIME type of the content
             * @return a new {@link Body} carrying the raw content
             */
            public static Body of(String content, String mimeType) {
                Objects.requireNonNull(content);
                Objects.requireNonNull(mimeType);

                return new Body(content, BodyContentType.RAW, mimeType);
            }

            /**
             * Creates a request body from the given string using the specified content type.
             *
             * @param content     the string serving as the body content
             * @param contentType the encoding to apply to the body
             * @return a new {@link Body} carrying the content
             */
            public static Body of(String content, BodyContentType contentType) {
                Objects.requireNonNull(content);
                Objects.requireNonNull(contentType);

                return new Body(content, contentType, null);
            }

            /**
             * Returns the raw content of the request body.
             *
             * @return the body content
             */
            public Object getContent() {
                return content;
            }

            /**
             * Returns the content type describing how the body content is encoded.
             *
             * @return the body content type
             */
            public BodyContentType getContentType() {
                return contentType;
            }

            /**
             * Returns the MIME type of the body content, if one was specified.
             *
             * @return the MIME type, or {@code null} if none was specified
             */
            public String getMimeType() {
                return mimeType;
            }

            @Override
            public String toString() {
                return "Body{" +
                    "content=" + content +
                    ", contentType=" + contentType +
                    ", mimeType='" + mimeType + '\'' +
                    '}';
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) {
                    return true;
                }

                if (!(o instanceof Body that)) {
                    return false;
                }

                return Objects.equals(content, that.content)
                    && Objects.equals(contentType, that.contentType)
                    && Objects.equals(mimeType, that.mimeType);
            }

            @Override
            public int hashCode() {
                return Objects.hash(content, contentType, mimeType);
            }
        }

        /**
         * Holds the settings that customize how an HTTP request is executed, such as timeout, proxy, redirect handling,
         * expected response type, and authorization behavior. Instances are created through a
         * {@link ConfigurationBuilder}.
         */
        class Configuration {

            private boolean allowUnauthorizedCerts;
            private String filename;
            private boolean followAllRedirects;
            private boolean followRedirect;
            private String proxy;
            private ResponseType responseType;
            private Duration timeout;
            private boolean disableAuthorization;

            public Configuration() {
            }

            /**
             * Creates a new, empty {@link ConfigurationBuilder}.
             *
             * @return a new configuration builder
             */
            public static ConfigurationBuilder newConfiguration() {
                return new ConfigurationBuilder();
            }

            /**
             * Returns whether requests may accept unauthorized (untrusted) TLS certificates.
             *
             * @return {@code true} if unauthorized certificates are accepted, {@code false} otherwise
             */
            public boolean isAllowUnauthorizedCerts() {
                return allowUnauthorizedCerts;
            }

            /**
             * Returns whether requests follow redirects for all request methods.
             *
             * @return {@code true} if all redirects are followed, {@code false} otherwise
             */
            public boolean isFollowAllRedirects() {
                return followAllRedirects;
            }

            /**
             * Returns whether requests follow a redirect response.
             *
             * @return {@code true} if redirects are followed, {@code false} otherwise
             */
            public boolean isFollowRedirect() {
                return followRedirect;
            }

            /**
             * Returns the file name assigned to the response content when it is stored as a file.
             *
             * @return the configured file name, or {@code null} if none is set
             */
            public String getFilename() {
                return filename;
            }

            /**
             * Returns the expected {@link ResponseType} of the response.
             *
             * @return the configured response type, or {@code null} if none is set
             */
            public ResponseType getResponseType() {
                return responseType;
            }

            /**
             * Returns the proxy through which requests are routed.
             *
             * @return the configured proxy, or {@code null} if none is set
             */
            public String getProxy() {
                return proxy;
            }

            /**
             * Returns the request timeout.
             *
             * @return the configured timeout, or {@code null} if none is set
             */
            public Duration getTimeout() {
                return timeout;
            }

            /**
             * Returns whether the connection's authorization is omitted from the request.
             *
             * @return {@code true} if authorization is disabled, {@code false} otherwise
             */
            public boolean isDisableAuthorization() {
                return disableAuthorization;
            }

            /**
             * Fluent builder that assembles a {@link Configuration} from individual HTTP request settings.
             */
            public static final class ConfigurationBuilder {

                private boolean allowUnauthorizedCerts;
                private String filename;
                private boolean followAllRedirects;
                private boolean followRedirect;
                private String proxy;
                private ResponseType responseType;
                private Duration timeout;
                private boolean disableAuthorization;

                private ConfigurationBuilder() {
                }

                @Override
                public boolean equals(Object o) {
                    if (this == o) {
                        return true;
                    }

                    if (!(o instanceof ConfigurationBuilder that)) {
                        return false;
                    }

                    return Objects.equals(allowUnauthorizedCerts, that.allowUnauthorizedCerts) &&
                        Objects.equals(followAllRedirects, that.followAllRedirects) &&
                        Objects.equals(followRedirect, that.followRedirect) &&
                        Objects.equals(disableAuthorization, that.disableAuthorization) &&
                        Objects.equals(filename, that.filename) &&
                        Objects.equals(proxy, that.proxy) &&
                        Objects.equals(responseType, that.responseType) &&
                        Objects.equals(timeout, that.timeout);
                }

                @Override
                public int hashCode() {
                    return Objects.hash(
                        allowUnauthorizedCerts, filename, followAllRedirects, followRedirect, proxy, responseType,
                        timeout, disableAuthorization);
                }

                /**
                 * Sets whether requests may accept unauthorized (untrusted) TLS certificates.
                 *
                 * @param allowUnauthorizedCerts whether to accept unauthorized TLS certificates
                 * @return this builder
                 */
                public ConfigurationBuilder allowUnauthorizedCerts(boolean allowUnauthorizedCerts) {
                    this.allowUnauthorizedCerts = allowUnauthorizedCerts;
                    return this;
                }

                /**
                 * Sets the file name assigned to the response content when it is stored as a file.
                 *
                 * @param filename the file name to assign
                 * @return this builder
                 */
                public ConfigurationBuilder filename(String filename) {
                    this.filename = filename;
                    return this;
                }

                /**
                 * Sets whether requests follow redirects for all request methods.
                 *
                 * @param followAllRedirects whether to follow redirects for all request methods
                 * @return this builder
                 */
                public ConfigurationBuilder followAllRedirects(boolean followAllRedirects) {
                    this.followAllRedirects = followAllRedirects;
                    return this;
                }

                /**
                 * Sets whether requests follow a redirect response.
                 *
                 * @param followRedirect whether to follow a redirect response
                 * @return this builder
                 */
                public ConfigurationBuilder followRedirect(boolean followRedirect) {
                    this.followRedirect = followRedirect;
                    return this;
                }

                /**
                 * Sets the proxy through which requests are routed.
                 *
                 * @param proxy the proxy specification
                 * @return this builder
                 */
                public ConfigurationBuilder proxy(String proxy) {
                    this.proxy = proxy;
                    return this;
                }

                /**
                 * Sets the expected {@link ResponseType} of the response.
                 *
                 * @param responseType the expected response type
                 * @return this builder
                 */
                public ConfigurationBuilder responseType(ResponseType responseType) {
                    this.responseType = responseType;
                    return this;
                }

                /**
                 * Sets the request timeout.
                 *
                 * @param timeout the maximum duration to wait for the request to complete
                 * @return this builder
                 */
                public ConfigurationBuilder timeout(Duration timeout) {
                    this.timeout = timeout;
                    return this;
                }

                /**
                 * Sets whether the connection's authorization is omitted from the request.
                 *
                 * @param disableAuthorization whether to omit applying the connection's authorization
                 * @return this builder
                 */
                public ConfigurationBuilder disableAuthorization(boolean disableAuthorization) {
                    this.disableAuthorization = disableAuthorization;
                    return this;
                }

                /**
                 * Builds a {@link Configuration} from the settings accumulated in this builder.
                 *
                 * @return the assembled configuration
                 */
                public Configuration build() {
                    Configuration configuration = new Configuration();

                    configuration.proxy = this.proxy;
                    configuration.followRedirect = this.followRedirect;
                    configuration.timeout = this.timeout;
                    configuration.responseType = this.responseType;
                    configuration.followAllRedirects = this.followAllRedirects;
                    configuration.allowUnauthorizedCerts = this.allowUnauthorizedCerts;
                    configuration.filename = this.filename;
                    configuration.disableAuthorization = this.disableAuthorization;

                    return configuration;
                }
            }
        }

        /**
         * Fluent builder for configuring and executing a single HTTP request, allowing headers, query parameters, a
         * body, and per-request configuration to be attached before the request is sent.
         */
        interface Executor {

            /**
             * Applies the request configuration built by the given builder.
             *
             * @param configurationBuilder the builder holding the request configuration
             * @return this executor
             */
            Executor configuration(ConfigurationBuilder configurationBuilder);

            /**
             * Adds a single HTTP header to the request.
             *
             * @param name  the header name
             * @param value the header value
             * @return this executor
             */
            Executor header(String name, String value);

            /**
             * Adds the given HTTP headers to the request.
             *
             * @param headers a map of header names to their values
             * @return this executor
             */
            Executor headers(Map<String, List<String>> headers);

            /**
             * Adds a single query parameter to the request.
             *
             * @param name  the query parameter name
             * @param value the query parameter value
             * @return this executor
             */
            Executor queryParameter(String name, String value);

            /**
             * Adds the given query parameters to the request.
             *
             * @param queryParameters a map of query parameter names to their values
             * @return this executor
             */
            Executor queryParameters(Map<String, List<String>> queryParameters);

            /**
             * Adds query parameters supplied as an alternating array of names and values.
             *
             * @param keyValueArray an even-length array of alternating parameter names and values
             * @return this executor
             */
            Executor queryParameters(Object... keyValueArray);

            /**
             * Sets the body of the request.
             *
             * @param body the request body
             * @return this executor
             */
            Executor body(Body body);

            /**
             * Executes the configured HTTP request and returns the response.
             *
             * @return the {@link Response} returned by the server
             */
            Response execute();
        }

        /**
         * Represents the response returned by an executed HTTP request, exposing its status code, headers, and body in
         * various deserialized forms.
         */
        interface Response {

            /**
             * Returns all response headers.
             *
             * @return a map of header names to their values
             */
            Map<String, List<String>> getHeaders();

            /**
             * Returns the response body in its default representation.
             *
             * @return the response body
             */
            Object getBody();

            /**
             * Returns the response body deserialized into the given type.
             *
             * @param <T>       the target type
             * @param valueType the class representing the target type
             * @return the deserialized response body
             */
            <T> T getBody(Class<T> valueType);

            /**
             * Returns the response body deserialized into the type described by the given type reference, preserving
             * generic type information.
             *
             * @param <T>          the target type
             * @param valueTypeRef the type reference describing the target type
             * @return the deserialized response body
             */
            <T> T getBody(TypeReference<T> valueTypeRef);

            /**
             * Returns the first value of the header with the given name.
             *
             * @param name the header name
             * @return the first value of the header, or {@code null} if the header is absent
             */
            String getFirstHeader(String name);

            /**
             * Returns all values of the header with the given name.
             *
             * @param name the header name
             * @return the list of values for the header
             */
            List<String> getHeader(String name);

            /**
             * Returns the HTTP status code of the response.
             *
             * @return the HTTP status code
             */
            int getStatusCode();
        }
    }

    /**
     * Provides utilities for reading JSON from strings and input streams into objects, lists, and maps, optionally
     * navigating to a nested path, and for writing objects back to JSON.
     */
    interface Json {

        /**
         * Reads JSON from the given input stream into its default object representation.
         *
         * @param inputStream the input stream supplying the JSON
         * @return the parsed value
         */
        Object read(InputStream inputStream);

        /**
         * Reads JSON from the given input stream and deserializes it into the specified type.
         *
         * @param <T>         the target type
         * @param inputStream the input stream supplying the JSON
         * @param valueType   the class representing the target type
         * @return the deserialized value
         */
        <T> T read(InputStream inputStream, Class<T> valueType);

        /**
         * Reads JSON from the given input stream and deserializes it into the type described by the given type
         * reference.
         *
         * @param <T>           the target type
         * @param inputStream   the input stream supplying the JSON
         * @param typeReference the type reference describing the target type
         * @return the deserialized value
         */
        <T> T read(InputStream inputStream, TypeReference<T> typeReference);

        /**
         * Reads JSON from the given input stream and returns the value located at the given path.
         *
         * @param inputStream the input stream supplying the JSON
         * @param path        the path locating the value to return
         * @return the value at the given path
         */
        Object read(InputStream inputStream, String path);

        /**
         * Reads JSON from the given input stream and deserializes the value at the given path into the specified type.
         *
         * @param <T>         the target type
         * @param inputStream the input stream supplying the JSON
         * @param path        the path locating the value to deserialize
         * @param valueType   the class representing the target type
         * @return the deserialized value at the given path
         */
        <T> T read(InputStream inputStream, String path, Class<T> valueType);

        /**
         * Reads JSON from the given input stream and deserializes the value at the given path into the type described
         * by the given type reference.
         *
         * @param <T>           the target type
         * @param inputStream   the input stream supplying the JSON
         * @param path          the path locating the value to deserialize
         * @param typeReference the type reference describing the target type
         * @return the deserialized value at the given path
         */
        <T> T read(InputStream inputStream, String path, TypeReference<T> typeReference);

        /**
         * Reads the given JSON string into its default object representation.
         *
         * @param json the JSON string
         * @return the parsed value
         */
        Object read(String json);

        /**
         * Reads the given JSON string and deserializes it into the specified type.
         *
         * @param <T>       the target type
         * @param json      the JSON string
         * @param valueType the class representing the target type
         * @return the deserialized value
         */
        <T> T read(String json, Class<T> valueType);

        /**
         * Reads the given JSON string and deserializes it into the type described by the given type reference.
         *
         * @param <T>           the target type
         * @param json          the JSON string
         * @param typeReference the type reference describing the target type
         * @return the deserialized value
         */
        <T> T read(String json, TypeReference<T> typeReference);

        /**
         * Reads the given JSON string and returns the value located at the given path.
         *
         * @param json the JSON string
         * @param path the path locating the value to return
         * @return the value at the given path
         */
        Object read(String json, String path);

        /**
         * Reads the given JSON string and deserializes the value at the given path into the specified type.
         *
         * @param <T>       the target type
         * @param json      the JSON string
         * @param path      the path locating the value to deserialize
         * @param valueType the class representing the target type
         * @return the deserialized value at the given path
         */
        <T> T read(String json, String path, Class<T> valueType);

        /**
         * Reads the given JSON string and deserializes the value at the given path into the type described by the given
         * type reference.
         *
         * @param <T>           the target type
         * @param json          the JSON string
         * @param path          the path locating the value to deserialize
         * @param typeReference the type reference describing the target type
         * @return the deserialized value at the given path
         */
        <T> T read(String json, String path, TypeReference<T> typeReference);

        /**
         * Reads JSON from the given input stream into a list.
         *
         * @param inputStream the input stream supplying the JSON
         * @return the parsed list
         */
        List<?> readList(InputStream inputStream);

        /**
         * Reads JSON from the given input stream into a list whose elements are deserialized into the specified type.
         *
         * @param <T>         the element type
         * @param inputStream the input stream supplying the JSON
         * @param elementType the class representing the element type
         * @return the parsed list
         */
        <T> List<T> readList(InputStream inputStream, Class<T> elementType);

        /**
         * Reads JSON from the given input stream and returns the list located at the given path.
         *
         * @param inputStream the input stream supplying the JSON
         * @param path        the path locating the list to return
         * @return the parsed list at the given path
         */
        List<?> readList(InputStream inputStream, String path);

        /**
         * Reads JSON from the given input stream and returns the list located at the given path with elements
         * deserialized into the specified type.
         *
         * @param <T>         the element type
         * @param inputStream the input stream supplying the JSON
         * @param path        the path locating the list to return
         * @param elementType the class representing the element type
         * @return the parsed list at the given path
         */
        <T> List<T> readList(InputStream inputStream, String path, Class<T> elementType);

        /**
         * Reads the given JSON string into a list.
         *
         * @param json the JSON string
         * @return the parsed list
         */
        List<?> readList(String json);

        /**
         * Reads the given JSON string into a list whose elements are deserialized into the specified type.
         *
         * @param <T>         the element type
         * @param json        the JSON string
         * @param elementType the class representing the element type
         * @return the parsed list
         */
        <T> List<T> readList(String json, Class<T> elementType);

        /**
         * Reads the given JSON string and returns the list located at the given path.
         *
         * @param json the JSON string
         * @param path the path locating the list to return
         * @return the parsed list at the given path
         */
        List<?> readList(String json, String path);

        /**
         * Reads the given JSON string and returns the list located at the given path with elements deserialized into
         * the specified type.
         *
         * @param <T>         the element type
         * @param json        the JSON string
         * @param path        the path locating the list to return
         * @param elementType the class representing the element type
         * @return the parsed list at the given path
         */
        <T> List<T> readList(String json, String path, Class<T> elementType);

        /**
         * Reads JSON from the given input stream into a map whose values are deserialized into the specified type.
         *
         * @param <V>         the value type
         * @param inputStream the input stream supplying the JSON
         * @param valueType   the class representing the value type
         * @return the parsed map
         */
        <V> Map<String, V> readMap(InputStream inputStream, Class<V> valueType);

        /**
         * Reads JSON from the given input stream and returns the map located at the given path.
         *
         * @param inputStream the input stream supplying the JSON
         * @param path        the path locating the map to return
         * @return the parsed map at the given path
         */
        Map<String, ?> readMap(InputStream inputStream, String path);

        /**
         * Reads JSON from the given input stream and returns the map located at the given path with values deserialized
         * into the specified type.
         *
         * @param <V>         the value type
         * @param inputStream the input stream supplying the JSON
         * @param path        the path locating the map to return
         * @param valueType   the class representing the value type
         * @return the parsed map at the given path
         */
        <V> Map<String, V> readMap(InputStream inputStream, String path, Class<V> valueType);

        /**
         * Reads the given JSON string into a map.
         *
         * @param json the JSON string
         * @return the parsed map
         */
        Map<String, ?> readMap(String json);

        /**
         * Reads the given JSON string into a map whose values are deserialized into the specified type.
         *
         * @param <V>       the value type
         * @param json      the JSON string
         * @param valueType the class representing the value type
         * @return the parsed map
         */
        <V> Map<String, V> readMap(String json, Class<V> valueType);

        /**
         * Reads the given JSON string and returns the map located at the given path.
         *
         * @param json the JSON string
         * @param path the path locating the map to return
         * @return the parsed map at the given path
         */
        Map<String, ?> readMap(String json, String path);

        /**
         * Reads the given JSON string and returns the map located at the given path with values deserialized into the
         * specified type.
         *
         * @param <V>       the value type
         * @param json      the JSON string
         * @param path      the path locating the map to return
         * @param valueType the class representing the value type
         * @return the parsed map at the given path
         */
        <V> Map<String, V> readMap(String json, String path, Class<V> valueType);

        /**
         * Reads JSON from the given input stream as a stream of maps, one per element of the top-level array.
         *
         * @param inputStream the input stream supplying the JSON
         * @return a stream of maps
         */
        Stream<Map<String, ?>> stream(InputStream inputStream);

        /**
         * Serializes the given object to a JSON string.
         *
         * @param object the object to serialize
         * @return the JSON representation of the object
         */
        String write(Object object);
    }

    /**
     * Provides logging utilities at the debug, error, info, warn, and trace levels, with support for parameterized
     * messages and exceptions.
     */
    interface Log {

        /**
         * Logs a message at the debug level.
         *
         * @param message the message to log
         */
        void debug(String message);

        /**
         * Logs a parameterized message at the debug level.
         *
         * @param format the message format containing placeholders
         * @param args   the arguments substituted into the format
         */
        void debug(String format, Object... args);

        /**
         * Logs a message and an accompanying exception at the debug level.
         *
         * @param message   the message to log
         * @param exception the exception to log alongside the message
         */
        void debug(String message, Exception exception);

        /**
         * Logs a message at the error level.
         *
         * @param message the message to log
         */
        void error(String message);

        /**
         * Logs a parameterized message at the error level.
         *
         * @param format the message format containing placeholders
         * @param args   the arguments substituted into the format
         */
        void error(String format, Object... args);

        /**
         * Logs a message and an accompanying exception at the error level.
         *
         * @param message   the message to log
         * @param exception the exception to log alongside the message
         */
        void error(String message, Exception exception);

        /**
         * Logs a message at the info level.
         *
         * @param message the message to log
         */
        void info(String message);

        /**
         * Logs a parameterized message at the info level.
         *
         * @param format the message format containing placeholders
         * @param args   the arguments substituted into the format
         */
        void info(String format, Object... args);

        /**
         * Logs a message and an accompanying exception at the info level.
         *
         * @param message   the message to log
         * @param exception the exception to log alongside the message
         */
        void info(String message, Exception exception);

        /**
         * Logs a message at the warn level.
         *
         * @param message the message to log
         */
        void warn(String message);

        /**
         * Logs a parameterized message at the warn level.
         *
         * @param format the message format containing placeholders
         * @param args   the arguments substituted into the format
         */
        void warn(String format, Object... args);

        /**
         * Logs a message and an accompanying exception at the warn level.
         *
         * @param message   the message to log
         * @param exception the exception to log alongside the message
         */
        void warn(String message, Exception exception);

        /**
         * Logs a message at the trace level.
         *
         * @param message the message to log
         */
        void trace(String message);

        /**
         * Logs a parameterized message at the trace level.
         *
         * @param format the message format containing placeholders
         * @param args   the arguments substituted into the format
         */
        void trace(String format, Object... args);

        /**
         * Logs a message and an accompanying exception at the trace level.
         *
         * @param message   the message to log
         * @param exception the exception to log alongside the message
         */
        void trace(String message, Exception exception);
    }

    /**
     * Provides lookups between file extensions and their corresponding MIME types.
     */
    interface MimeType {

        /**
         * Looks up the MIME type associated with the given file extension.
         *
         * @param ext the file extension to look up
         * @return the corresponding MIME type
         */
        String lookupMimeType(String ext);

        /**
         * Looks up the file extension associated with the given MIME type.
         *
         * @param mimeType the MIME type to look up
         * @return the corresponding file extension
         */
        String lookupExt(String mimeType);

    }

    /**
     * Provides utilities for deriving the output schema and sample output of an action or trigger, either from a JSON
     * schema or by inspecting a sample value.
     */
    interface OutputSchema {

        /**
         * Derives an output schema property from the given JSON schema.
         *
         * @param jsonSchema the JSON schema describing the output
         * @return the derived output schema property, or {@code null} if it cannot be derived
         */
        @Nullable
        ValueProperty<?> getOutputSchema(String jsonSchema);

        /**
         * Derives an output schema property with the given name from the given JSON schema.
         *
         * @param propertyName the name to assign to the derived property
         * @param jsonSchema   the JSON schema describing the output
         * @return the derived output schema property, or {@code null} if it cannot be derived
         */
        @Nullable
        ValueProperty<?> getOutputSchema(String propertyName, String jsonSchema);

        /**
         * Derives an output schema property by inspecting the structure of the given sample value.
         *
         * @param value the sample value from which to infer the schema
         * @return the derived output schema property
         */
        ValueProperty<?> getOutputSchema(Object value);

        /**
         * Produces a sample output value consistent with the given property definition.
         *
         * @param definitionProperty the property definition to generate a sample for
         * @return a sample output value, or {@code null} if one cannot be produced
         */
        @Nullable
        Object getSampleOutput(BaseProperty definitionProperty);
    }

    /**
     * Provides utilities for reading XML from strings and input streams into maps and lists, optionally navigating to a
     * nested path, and for writing objects back to XML.
     */
    interface Xml {

        /**
         * Reads XML from the given input stream into a map.
         *
         * @param inputStream the input stream supplying the XML
         * @return the parsed map
         */
        Map<String, ?> read(InputStream inputStream);

        /**
         * Reads XML from the given input stream into a map whose values are deserialized into the specified type.
         *
         * @param <T>         the value type
         * @param inputStream the input stream supplying the XML
         * @param valueType   the class representing the value type
         * @return the parsed map
         */
        <T> Map<String, T> read(InputStream inputStream, Class<T> valueType);

        /**
         * Reads XML from the given input stream into a map whose values are deserialized into the type described by the
         * given type reference.
         *
         * @param <T>                the value type
         * @param inputStream        the input stream supplying the XML
         * @param valueTypeReference the type reference describing the value type
         * @return the parsed map
         */
        <T> Map<String, T> read(InputStream inputStream, TypeReference<T> valueTypeReference);

        /**
         * Reads the given XML string into a map.
         *
         * @param xml the XML string
         * @return the parsed map
         */
        Map<String, ?> read(String xml);

        /**
         * Reads the given XML string into a map whose values are deserialized into the specified type.
         *
         * @param <T>       the value type
         * @param xml       the XML string
         * @param valueType the class representing the value type
         * @return the parsed map
         */
        <T> Map<String, T> read(String xml, Class<T> valueType);

        /**
         * Reads the given XML string into a map whose values are deserialized into the type described by the given type
         * reference.
         *
         * @param <T>                the value type
         * @param xml                the XML string
         * @param valueTypeReference the type reference describing the value type
         * @return the parsed map
         */
        <T> Map<String, T> read(String xml, TypeReference<T> valueTypeReference);

        /**
         * Reads XML from the given input stream and returns the list located at the given path.
         *
         * @param inputStream the input stream supplying the XML
         * @param path        the path locating the list to return
         * @return the parsed list at the given path
         */
        List<?> readList(InputStream inputStream, String path);

        /**
         * Reads XML from the given input stream and returns the list located at the given path with elements
         * deserialized into the specified type.
         *
         * @param <T>         the element type
         * @param inputStream the input stream supplying the XML
         * @param path        the path locating the list to return
         * @param elementType the class representing the element type
         * @return the parsed list at the given path
         */
        <T> List<T> readList(InputStream inputStream, String path, Class<T> elementType);

        /**
         * Reads XML from the given input stream and returns the list located at the given path with elements
         * deserialized into the type described by the given type reference.
         *
         * @param <T>                  the element type
         * @param inputStream          the input stream supplying the XML
         * @param path                 the path locating the list to return
         * @param elementTypeReference the type reference describing the element type
         * @return the parsed list at the given path
         */
        <T> List<T> readList(InputStream inputStream, String path, TypeReference<T> elementTypeReference);

        /**
         * Reads XML from the given input stream as a stream of maps, one per element of the top-level collection.
         *
         * @param inputStream the input stream supplying the XML
         * @return a stream of maps
         */
        Stream<Map<String, ?>> stream(InputStream inputStream);

        /**
         * Serializes the given object to an XML string.
         *
         * @param object the object to serialize
         * @return the XML representation of the object
         */
        String write(Object object);

        /**
         * Serializes the given object to an XML string using the specified root element name.
         *
         * @param object   the object to serialize
         * @param rootName the name of the root XML element
         * @return the XML representation of the object
         */
        String write(Object object, String rootName);
    }
}
