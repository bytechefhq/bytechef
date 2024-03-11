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

package com.bytechef.component.definition;

import com.bytechef.component.definition.Context.Http.Configuration.ConfigurationBuilder;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
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
     *
     * @param httpFunction
     * @return
     * @param <R>
     */
    <R> R http(ContextFunction<Http, R> httpFunction);

    /**
     *
     * @param jsonFunction
     * @return
     * @param <R>
     */
    <R> R json(ContextFunction<Json, R> jsonFunction);

    /**
     *
     * @param logFunction
     */
    void logger(ContextConsumer<Logger> logFunction);

    /**
     *
     * @param outputFunction
     * @return
     */
    com.bytechef.component.definition.Output output(
        ContextFunction<Output, com.bytechef.component.definition.Output> outputFunction);

    /**
     *
     * @param xmlFunction
     * @return
     * @param <R>
     */
    <R> R xml(ContextFunction<Xml, R> xmlFunction);

    /**
     *
     */
    interface File {

        /**
         *
         * @param fileEntry
         * @return
         */
        InputStream getStream(FileEntry fileEntry);

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
        FileEntry storeContent(String fileName, InputStream inputStream)
            throws IOException;

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

    @FunctionalInterface
    interface ContextConsumer<T> {

        void accept(T t) throws Exception;
    }

    @FunctionalInterface
    interface ContextFunction<T, R> {

        R apply(T t) throws Exception;
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
        enum ResponseType {
            BINARY,
            JSON,
            TEXT,
            XML,
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
                Objects.requireNonNull(keyValueArray);

                if (keyValueArray.length % 2 != 0) {
                    throw new IllegalArgumentException();
                }

                HashMap<String, ?> content = IntStream.range(0, keyValueArray.length / 2)
                    .filter(i -> keyValueArray[i * 2] != null && keyValueArray[i * 2 + 1] != null)
                    .collect(
                        HashMap::new,
                        (map, i) -> map.put(String.valueOf(keyValueArray[i * 2]), keyValueArray[i * 2 + 1]),
                        HashMap::putAll);

                return new Body(content, BodyContentType.JSON);
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

        @SuppressFBWarnings("EI")
        interface Response {

            Map<String, List<String>> getHeaders();

            Object getBody();

            <T> T getBody(Class<T> valueType);

            <T> T getBody(TypeReference<T> valueTypeRef);

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
    interface Logger {

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

    /**
     *
     */
    interface Output {

        /**
         *
         * @param value
         * @return
         */
        com.bytechef.component.definition.Output get(Object value);
    }

    /**
     *
     * @param <T>
     */
    abstract class TypeReference<T> implements Comparable<TypeReference<T>> {

        protected final Type type;

        @SuppressFBWarnings("CT_CONSTRUCTOR_THROW")
        protected TypeReference() {
            Type superClass = getClass().getGenericSuperclass();

            if (superClass instanceof Class<?>) { // sanity check, should never happen
                throw new IllegalArgumentException(
                    "Internal error: TypeReference constructed without actual type information");
            }

            type = ((ParameterizedType) superClass).getActualTypeArguments()[0];
        }

        public Type getType() {
            return type;
        }

        /**
         * The only reason we define this method (and require implementation of <code>Comparable</code>) is to prevent
         * constructing a reference without type information.
         */
        @Override
        public int compareTo(TypeReference<T> o) {
            return 0;
        }
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
