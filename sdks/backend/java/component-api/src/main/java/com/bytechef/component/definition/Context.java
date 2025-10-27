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

/**
 * @author Ivica Cardic
 */
public interface Context {

    /**
     * @param convertFunction
     * @param <R>
     * @return
     */
    <R> R convert(ContextFunction<Convert, R> convertFunction);

    /**
     *
     * @param encoderFunction
     * @return
     * @param <R>
     */
    <R> R encoder(ContextFunction<Encoder, R> encoderFunction);

    /**
     * @param fileFunction
     * @param <R>
     * @return
     */
    <R> R file(ContextFunction<File, R> fileFunction);

    /**
     *
     * @param httpFunction
     * @return
     * @param <R>
     */
    <R> R http(ContextFunction<Http, R> httpFunction);

    /**
     * Determines whether the current environment is the editor environment.
     *
     * @return true if the current environment is an editor environment, false otherwise.
     */
    boolean isEditorEnvironment();

    /**
     *
     * @param jsonFunction
     * @return
     * @param <R>
     */
    <R> R json(ContextFunction<Json, R> jsonFunction);

    /**
     *
     * @param logConsumer
     */
    void log(ContextConsumer<Log> logConsumer);

    /**
     *
     * @param mimeTypeContextFunction
     * @return
     * @param <R>
     */
    <R> R mimeType(ContextFunction<MimeType, R> mimeTypeContextFunction);

    /**
     *
     * @param outputSchemaFunction
     * @return
     * @param <R>
     */
    <R> R outputSchema(ContextFunction<OutputSchema, R> outputSchemaFunction);

    /**
     *
     * @param xmlFunction
     * @return
     * @param <R>
     */
    <R> R xml(ContextFunction<Xml, R> xmlFunction);

    /**
     *
     * @param <T>
     */
    @FunctionalInterface
    interface ContextConsumer<T> {

        void accept(T t) throws Exception;
    }

    /**
     *
     * @param <T>
     * @param <R>
     */
    @FunctionalInterface
    interface ContextFunction<T, R> {

        R apply(T t) throws Exception;
    }

    /**
     *
     */
    interface Convert {

        /**
         *
         * @param fromValue
         * @param toValueType
         * @return
         */
        boolean canConvert(Object fromValue, Class<?> toValueType);

        /**
         *
         * @param fromValue
         * @param toValueType
         * @return
         */
        <T> T value(Object fromValue, Class<T> toValueType);

        /**
         *
         * @param fromValue
         * @param toValueType
         * @param includeNulls
         * @return
         */
        <T> T value(Object fromValue, Class<T> toValueType, boolean includeNulls);

        /**
         *
         * @param fromValue
         * @param toValueTypeRef
         * @return
         */
        <T> T value(Object fromValue, TypeReference<T> toValueTypeRef);

        /**
         *
         * @param str
         * @return
         */
        Object string(String str);

    }

    /**
     *
     */
    interface Encoder {

        /**
         *
         * @param string
         * @return
         */
        byte[] base64Decode(String string);

        /**
         *
         * @param bytes
         * @return
         */
        String base64EncodeToString(byte[] bytes);

        /**
         *
         * @param string
         * @return
         */
        byte[] urlDecodeBase64FromString(String string);

        /**
         *
         * @param string
         * @return
         */
        String urlEncode(String string);

    }

    /**
     *
     */
    interface File {

        /**
         *
         * @param fileEntry
         * @return
         */
        long getContentLength(FileEntry fileEntry);

        /**
         *
         * @param fileEntry
         * @return
         */
        InputStream getInputStream(FileEntry fileEntry);

        /**
         *
         * @param fileEntry
         * @return
         */
        OutputStream getOutputStream(FileEntry fileEntry);

        /**
         *
         * @param fileEntry
         * @return
         */
        String readToString(FileEntry fileEntry);

        /**
         *
         * @param fileName
         * @param inputStream
         * @return
         */
        FileEntry storeContent(String fileName, InputStream inputStream) throws IOException;

        /**
         *
         * @param fileName
         * @param data
         * @return
         */
        FileEntry storeContent(String fileName, String data) throws IOException;

        /**
         *
         * @param fileEntry
         * @return
         */
        java.io.File toTempFile(FileEntry fileEntry);

        /**
         *
         * @param fileEntry
         * @return
         */
        Path toTempFilePath(FileEntry fileEntry);

        /**
         * @param fileEntry
         * @return
         */
        byte[] readAllBytes(FileEntry fileEntry) throws IOException;
    }

    /**
     *
     */
    interface Http {

        /**
         *
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
         *
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
         *
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
         *
         * @param url
         * @return
         */
        Executor delete(String url);

        /**
         *
         * @param url
         * @param requestMethod
         * @return
         */
        Executor exchange(String url, RequestMethod requestMethod);

        /**
         *
         * @param url
         * @return
         */
        Executor head(String url);

        /**
         *
         * @param url
         * @return
         */
        Executor get(String url);

        /**
         *
         * @param url
         * @return
         */
        Executor patch(String url);

        /**
         *
         * @param url
         * @return
         */
        Executor post(String url);

        /**
         *
         * @param url
         * @return
         */
        Executor put(String url);

        /**
         *
         * @param allowUnauthorizedCerts
         * @return
         */
        static ConfigurationBuilder allowUnauthorizedCerts(boolean allowUnauthorizedCerts) {
            ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();

            configurationBuilder.allowUnauthorizedCerts = allowUnauthorizedCerts;

            return configurationBuilder;
        }

        /**
         *
         * @param followAllRedirects
         * @return
         */
        static ConfigurationBuilder followAllRedirects(boolean followAllRedirects) {
            ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();

            configurationBuilder.followAllRedirects = followAllRedirects;

            return configurationBuilder;
        }

        /**
         *
         * @param filename
         * @return
         */
        static ConfigurationBuilder filename(String filename) {
            ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();

            configurationBuilder.filename = filename;

            return configurationBuilder;
        }

        /**
         *
         * @param followRedirect
         * @return
         */
        static ConfigurationBuilder followRedirect(boolean followRedirect) {
            ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();

            configurationBuilder.followRedirect = followRedirect;

            return configurationBuilder;
        }

        /**
         *
         * @param proxy
         * @return
         */
        static ConfigurationBuilder proxy(String proxy) {
            ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();

            configurationBuilder.proxy = proxy;

            return configurationBuilder;
        }

        /**
         *
         * @param responseType
         * @return
         */
        static ConfigurationBuilder responseType(ResponseType responseType) {
            ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();

            configurationBuilder.responseType = responseType;

            return configurationBuilder;
        }

        /**
         *
         * @param timeout
         * @return
         */
        static ConfigurationBuilder timeout(Duration timeout) {
            ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();

            configurationBuilder.timeout = timeout;

            return configurationBuilder;
        }

        /**
         *
         * @param disableAuthorization
         * @return
         */
        static ConfigurationBuilder disableAuthorization(boolean disableAuthorization) {
            ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();

            configurationBuilder.disableAuthorization = disableAuthorization;

            return configurationBuilder;
        }

        /**
         *
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
             *
             * @param content
             * @return
             */
            public static Body of(FileEntry content) {
                return new Body(content, BodyContentType.BINARY);
            }

            /**
             *
             * @param content
             * @param mimeType
             * @return
             */
            public static Body of(FileEntry content, String mimeType) {
                Objects.requireNonNull(content);

                return new Body(content, BodyContentType.BINARY, mimeType);
            }

            /**
             *
             * @param content
             * @return
             */
            public static Body of(List<?> content) {
                Objects.requireNonNull(content);

                return new Body(content, BodyContentType.JSON);
            }

            /**
             *
             * @param content
             * @param contentType
             * @return
             */
            public static Body of(List<?> content, BodyContentType contentType) {
                Objects.requireNonNull(content);
                Objects.requireNonNull(contentType);

                return new Body(content, contentType);
            }

            /**
             *
             * @param content
             * @return
             */
            public static Body of(Map<String, ?> content) {
                Objects.requireNonNull(content);

                return new Body(content, BodyContentType.JSON);
            }

            /**
             *
             * @param keyValueArray
             * @return
             */
            public static Body of(Object... keyValueArray) {
                Map<String, ?> content = getStringObjectMap(keyValueArray);

                return new Body(content, BodyContentType.JSON);
            }

            /**
             * @param keyValueArray
             * @return
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
             *
             * @param content
             * @param contentType
             * @return
             */
            public static Body of(Map<String, ?> content, BodyContentType contentType) {
                Objects.requireNonNull(content);
                Objects.requireNonNull(contentType);

                return new Body(content, contentType);
            }

            /**
             *
             * @param content
             * @return
             */
            public static Body of(String content) {
                Objects.requireNonNull(content);

                return new Body(content, BodyContentType.RAW, "text/plain");
            }

            /**
             *
             * @param content
             * @param mimeType
             * @return
             */
            public static Body of(String content, String mimeType) {
                Objects.requireNonNull(content);
                Objects.requireNonNull(mimeType);

                return new Body(content, BodyContentType.RAW, mimeType);
            }

            /**
             *
             * @param content
             * @param contentType
             * @return
             */
            public static Body of(String content, BodyContentType contentType) {
                Objects.requireNonNull(content);
                Objects.requireNonNull(contentType);

                return new Body(content, contentType, null);
            }

            /**
             *
             * @return
             */
            public Object getContent() {
                return content;
            }

            /**
             *
             * @return
             */
            public BodyContentType getContentType() {
                return contentType;
            }

            /**
             *
             * @return
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

            public static ConfigurationBuilder newConfiguration() {
                return new ConfigurationBuilder();
            }

            /**
             * @return
             */
            public boolean isAllowUnauthorizedCerts() {
                return allowUnauthorizedCerts;
            }

            /**
             * @return
             */
            public boolean isFollowAllRedirects() {
                return followAllRedirects;
            }

            /**
             * @return
             */
            public boolean isFollowRedirect() {
                return followRedirect;
            }

            /**
             * @return
             */
            public String getFilename() {
                return filename;
            }

            /**
             * @return
             */
            public ResponseType getResponseType() {
                return responseType;
            }

            /**
             * @return
             */
            public String getProxy() {
                return proxy;
            }

            /**
             * @return
             */
            public Duration getTimeout() {
                return timeout;
            }

            /**
             * @return
             */
            public boolean isDisableAuthorization() {
                return disableAuthorization;
            }

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

                public ConfigurationBuilder allowUnauthorizedCerts(boolean allowUnauthorizedCerts) {
                    this.allowUnauthorizedCerts = allowUnauthorizedCerts;
                    return this;
                }

                public ConfigurationBuilder filename(String filename) {
                    this.filename = filename;
                    return this;
                }

                public ConfigurationBuilder followAllRedirects(boolean followAllRedirects) {
                    this.followAllRedirects = followAllRedirects;
                    return this;
                }

                public ConfigurationBuilder followRedirect(boolean followRedirect) {
                    this.followRedirect = followRedirect;
                    return this;
                }

                public ConfigurationBuilder proxy(String proxy) {
                    this.proxy = proxy;
                    return this;
                }

                public ConfigurationBuilder responseType(ResponseType responseType) {
                    this.responseType = responseType;
                    return this;
                }

                public ConfigurationBuilder timeout(Duration timeout) {
                    this.timeout = timeout;
                    return this;
                }

                public ConfigurationBuilder disableAuthorization(boolean disableAuthorization) {
                    this.disableAuthorization = disableAuthorization;
                    return this;
                }

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
         *
         */
        interface Executor {

            /**
             *
             * @param configurationBuilder
             * @return
             */
            Executor configuration(ConfigurationBuilder configurationBuilder);

            /**
             *
             * @param name
             * @param value
             * @return
             */
            Executor header(String name, String value);

            /**
             *
             * @param headers
             * @return
             */
            Executor headers(Map<String, List<String>> headers);

            /**
             *
             * @param name
             * @param value
             * @return
             */
            Executor queryParameter(String name, String value);

            /**
             *
             * @param queryParameters
             * @return
             */
            Executor queryParameters(Map<String, List<String>> queryParameters);

            /**
             *
             * @param keyValueArray
             * @return
             */
            Executor queryParameters(Object... keyValueArray);

            /**
             *
             * @param body
             * @return
             */
            Executor body(Body body);

            /**
             *
             * @return
             */
            Response execute();
        }

        interface Response {

            Map<String, List<String>> getHeaders();

            Object getBody();

            <T> T getBody(Class<T> valueType);

            <T> T getBody(TypeReference<T> valueTypeRef);

            String getFirstHeader(String name);

            List<String> getHeader(String name);

            int getStatusCode();
        }
    }

    /**
     *
     */
    interface Json {

        /**
         *
         * @param inputStream
         * @return
         */
        Object read(InputStream inputStream);

        /**
         *
         * @param inputStream
         * @param valueType
         * @return
         * @param <T>
         */
        <T> T read(InputStream inputStream, Class<T> valueType);

        /**
         *
         * @param inputStream
         * @param typeReference
         * @return
         * @param <T>
         */
        <T> T read(InputStream inputStream, TypeReference<T> typeReference);

        /**
         *
         * @param inputStream
         * @param path
         * @return
         */
        Object read(InputStream inputStream, String path);

        /**
         *
         * @param inputStream
         * @param path
         * @param valueType
         * @return
         * @param <T>
         */
        <T> T read(InputStream inputStream, String path, Class<T> valueType);

        /**
         *
         * @param inputStream
         * @param path
         * @param typeReference
         * @return
         * @param <T>
         */
        <T> T read(InputStream inputStream, String path, TypeReference<T> typeReference);

        /**
         *
         * @param json
         * @return
         */
        Object read(String json);

        /**
         *
         * @param json
         * @param valueType
         * @return
         * @param <T>
         */
        <T> T read(String json, Class<T> valueType);

        /**
         *
         * @param json
         * @param typeReference
         * @return
         * @param <T>
         */
        <T> T read(String json, TypeReference<T> typeReference);

        /**
         *
         * @param json
         * @param path
         * @return
         */
        Object read(String json, String path);

        /**
         *
         * @param json
         * @param path
         * @param valueType
         * @return
         * @param <T>
         */
        <T> T read(String json, String path, Class<T> valueType);

        /**
         *
         * @param json
         * @param path
         * @param typeReference
         * @return
         * @param <T>
         */
        <T> T read(String json, String path, TypeReference<T> typeReference);

        /**
         *
         * @param inputStream
         * @return
         */
        List<?> readList(InputStream inputStream);

        /**
         *
         * @param inputStream
         * @param elementType
         * @return
         * @param <T>
         */
        <T> List<T> readList(InputStream inputStream, Class<T> elementType);

        /**
         *
         * @param inputStream
         * @param path
         * @return
         */
        List<?> readList(InputStream inputStream, String path);

        /**
         *
         * @param inputStream
         * @param path
         * @param elementType
         * @return
         * @param <T>
         */
        <T> List<T> readList(InputStream inputStream, String path, Class<T> elementType);

        /**
         *
         * @param json
         * @return
         */
        List<?> readList(String json);

        /**
         *
         * @param json
         * @param elementType
         * @return
         * @param <T>
         */
        <T> List<T> readList(String json, Class<T> elementType);

        /**
         *
         * @param json
         * @param path
         * @return
         */
        List<?> readList(String json, String path);

        /**
         *
         * @param json
         * @param path
         * @param elementType
         * @return
         * @param <T>
         */
        <T> List<T> readList(String json, String path, Class<T> elementType);

        /**
         *
         * @param inputStream
         * @param valueType
         * @return
         * @param <V>
         */
        <V> Map<String, V> readMap(InputStream inputStream, Class<V> valueType);

        /**
         *
         * @param inputStream
         * @param path
         * @return
         */
        Map<String, ?> readMap(InputStream inputStream, String path);

        /**
         *
         * @param inputStream
         * @param path
         * @param valueType
         * @return
         * @param <V>
         */
        <V> Map<String, V> readMap(InputStream inputStream, String path, Class<V> valueType);

        /**
         *
         * @param json
         * @return
         */
        Map<String, ?> readMap(String json);

        /**
         *
         * @param json
         * @param valueType
         * @return
         * @param <V>
         */
        <V> Map<String, V> readMap(String json, Class<V> valueType);

        /**
         *
         * @param json
         * @param path
         * @return
         */
        Map<String, ?> readMap(String json, String path);

        /**
         *
         * @param json
         * @param path
         * @param valueType
         * @return
         * @param <V>
         */
        <V> Map<String, V> readMap(String json, String path, Class<V> valueType);

        /**
         *
         * @param inputStream
         * @return
         */
        Stream<Map<String, ?>> stream(InputStream inputStream);

        /**
         *
         * @param object
         * @return
         */
        String write(Object object);
    }

    /**
     *
     */
    interface Log {

        /**
         *
         * @param message
         */
        void debug(String message);

        /**
         *
         * @param format
         * @param args
         */
        void debug(String format, Object... args);

        /**
         *
         * @param message
         * @param exception
         */
        void debug(String message, Exception exception);

        /**
         *
         * @param message
         */
        void error(String message);

        /**
         *
         * @param format
         * @param args
         */
        void error(String format, Object... args);

        /**
         *
         * @param message
         * @param exception
         */
        void error(String message, Exception exception);

        /**
         *
         * @param message
         */
        void info(String message);

        /**
         *
         * @param format
         * @param args
         */
        void info(String format, Object... args);

        /**
         *
         * @param message
         * @param exception
         */
        void info(String message, Exception exception);

        /**
         *
         * @param message
         */
        void warn(String message);

        /**
         *
         * @param format
         * @param args
         */
        void warn(String format, Object... args);

        /**
         *
         * @param message
         * @param exception
         */
        void warn(String message, Exception exception);

        /**
         *
         * @param message
         */
        void trace(String message);

        /**
         *
         * @param format
         * @param args
         */
        void trace(String format, Object... args);

        /**
         *
         * @param message
         * @param exception
         */
        void trace(String message, Exception exception);
    }

    interface MimeType {

        /**
         *
         * @param ext
         */
        String lookupMimeType(String ext);

        /**
         *
         * @param mimeType
         */
        String lookupExt(String mimeType);

    }

    interface OutputSchema {

        ValueProperty<?> getOutputSchema(String jsonSchema);

        ValueProperty<?> getOutputSchema(String propertyName, String jsonSchema);

        ValueProperty<?> getOutputSchema(Object value);
    }

    /**
     *
     */
    interface Xml {

        /**
         *
         * @param inputStream
         * @return
         */
        Map<String, ?> read(InputStream inputStream);

        /**
         *
         * @param inputStream
         * @param valueType
         * @return
         * @param <T>
         */
        <T> Map<String, T> read(InputStream inputStream, Class<T> valueType);

        /**
         *
         * @param inputStream
         * @param valueTypeReference
         * @return
         * @param <T>
         */
        <T> Map<String, T> read(InputStream inputStream, TypeReference<T> valueTypeReference);

        /**
         *
         * @param xml
         * @return
         */
        Map<String, ?> read(String xml);

        /**
         *
         * @param xml
         * @param valueType
         * @return
         * @param <T>
         */
        <T> Map<String, T> read(String xml, Class<T> valueType);

        /**
         *
         * @param xml
         * @param valueTypeReference
         * @return
         * @param <T>
         */
        <T> Map<String, T> read(String xml, TypeReference<T> valueTypeReference);

        /**
         *
         * @param inputStream
         * @param path
         * @return
         */
        List<?> readList(InputStream inputStream, String path);

        /**
         *
         * @param inputStream
         * @param path
         * @param elementType
         * @return
         * @param <T>
         */
        <T> List<T> readList(InputStream inputStream, String path, Class<T> elementType);

        /**
         *
         * @param inputStream
         * @param path
         * @param elementTypeReference
         * @return
         * @param <T>
         */
        <T> List<T> readList(InputStream inputStream, String path, TypeReference<T> elementTypeReference);

        /**
         *
         * @param inputStream
         * @return
         */
        Stream<Map<String, ?>> stream(InputStream inputStream);

        /**
         *
         * @param object
         * @return
         */
        String write(Object object);

        /**
         *
         * @param object
         * @param rootName
         * @return
         */
        String write(Object object, String rootName);
    }
}
