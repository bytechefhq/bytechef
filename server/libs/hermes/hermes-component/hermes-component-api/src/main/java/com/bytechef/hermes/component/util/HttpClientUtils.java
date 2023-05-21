
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

package com.bytechef.hermes.component.util;

import com.bytechef.hermes.component.Context.FileEntry;
import com.bytechef.hermes.component.exception.ComponentExecutionException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;

/**
 * @author Matija Petanjek
 * @author Ivica Cardic
 */
public final class HttpClientUtils {

    /**
     *
     */
    public enum BodyContentType {
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
    public enum ResponseFormat {
        BINARY,
        JSON,
        TEXT,
        XML,
    }

    /**
     *
     */
    public enum RequestMethod {
        DELETE,
        GET,
        HEAD,
        PATCH,
        POST,
        PUT,
    }

    static HttpClientExecutor httpClientExecutor;

    static {
        ServiceLoader<HttpClientExecutor> loader = ServiceLoader.load(HttpClientExecutor.class);

        httpClientExecutor = loader.findFirst()
            .orElse(null);

        if (httpClientExecutor == null) {
            System.err.println("HttpClientExecutor instance is not available");
        }
    }

    private HttpClientUtils() {
    }

    /**
     *
     * @param allowUnauthorizedCerts
     * @return
     */
    public static Configuration allowUnauthorizedCerts(boolean allowUnauthorizedCerts) {
        Configuration configuration = new Configuration();

        configuration.allowUnauthorizedCerts = allowUnauthorizedCerts;

        return configuration;
    }

    /**
     *
     * @param followAllRedirects
     * @return
     */
    public static Configuration followAllRedirects(boolean followAllRedirects) {
        Configuration configuration = new Configuration();

        configuration.followAllRedirects = followAllRedirects;

        return configuration;
    }

    /**
     *
     * @param filename
     * @return
     */
    public static Configuration filename(String filename) {
        Configuration configuration = new Configuration();

        configuration.filename = filename;

        return configuration;
    }

    /**
     *
     * @param followRedirect
     * @return
     */
    public static Configuration followRedirect(boolean followRedirect) {
        Configuration configuration = new Configuration();

        configuration.followRedirect = followRedirect;

        return configuration;
    }

    /**
     *
     * @param proxy
     * @return
     */
    public static Configuration proxy(String proxy) {
        Configuration configuration = new Configuration();

        configuration.proxy = proxy;

        return configuration;
    }

    /**
     *
     * @param responseFormat
     * @return
     */
    public static Configuration responseFormat(ResponseFormat responseFormat) {
        Configuration configuration = new Configuration();

        configuration.responseFormat = responseFormat;

        return configuration;
    }

    /**
     *
     * @param timeout
     * @return
     */
    public static Configuration timeout(Duration timeout) {
        Configuration configuration = new Configuration();

        configuration.timeout = timeout;

        return configuration;
    }

    /**
     *
     * @param uri
     * @return
     */
    public static Executor delete(String uri) {
        return new Executor(uri, RequestMethod.DELETE);
    }

    /**
     *
     * @param uri
     * @param requestMethod
     * @return
     */
    public static Executor exchange(String uri, RequestMethod requestMethod) {
        return new Executor(uri, requestMethod);
    }

    /**
     *
     * @param uri
     * @return
     */
    public static Executor head(String uri) {
        return new Executor(uri, RequestMethod.HEAD);
    }

    /**
     *
     * @param uri
     * @return
     */
    public static Executor get(String uri) {
        return new Executor(uri, RequestMethod.GET);
    }

    /**
     *
     * @param uri
     * @return
     */
    public static Executor patch(String uri) {
        return new Executor(uri, RequestMethod.PATCH);
    }

    /**
     *
     * @param uri
     * @return
     */
    public static Executor post(String uri) {
        return new Executor(uri, RequestMethod.POST);
    }

    /**
     *
     * @param uri
     * @return
     */
    public static Executor put(String uri) {
        return new Executor(uri, RequestMethod.PUT);
    }

    /**
     *
     */
    @SuppressFBWarnings("EI")
    public record Response(Map<String, List<String>> headers, Object body, int statusCode) {
    }

    /**
     *
     */
    public static class Configuration {

        private boolean allowUnauthorizedCerts;
        private String filename;
        private boolean followAllRedirects;
        private boolean followRedirect;
        private String proxy;
        private ResponseFormat responseFormat;
        private Duration timeout = Duration.ofMillis(1000);

        private Configuration() {
        }

        /**
         *
         * @return
         */
        public static Configuration configuration() {
            return new Configuration();
        }

        /**
         *
         * @param allowUnauthorizedCerts
         * @return
         */
        public Configuration allowUnauthorizedCerts(boolean allowUnauthorizedCerts) {
            this.allowUnauthorizedCerts = allowUnauthorizedCerts;

            return this;
        }

        /**
         *
         * @param followAllRedirects
         * @return
         */
        public Configuration followAllRedirects(boolean followAllRedirects) {
            this.followAllRedirects = followAllRedirects;

            return this;
        }

        /**
         *
         * @param filename
         * @return
         */
        public Configuration filename(String filename) {
            this.filename = filename;

            return this;
        }

        /**
         *
         * @param followRedirect
         * @return
         */
        public Configuration followRedirect(boolean followRedirect) {
            this.followRedirect = followRedirect;

            return this;
        }

        /**
         *
         * @param proxy
         * @return
         */
        public Configuration proxy(String proxy) {
            this.proxy = proxy;

            return this;
        }

        /**
         *
         * @param responseFormat
         * @return
         */
        public Configuration responseFormat(ResponseFormat responseFormat) {
            this.responseFormat = responseFormat;

            return this;
        }

        /**
         *
         * @param timeout
         * @return
         */
        public Configuration timeout(Duration timeout) {
            this.timeout = timeout;

            return this;
        }

        /**
         *
         * @return
         */
        public boolean isAllowUnauthorizedCerts() {
            return allowUnauthorizedCerts;
        }

        /**
         *
         * @return
         */
        public boolean isFollowAllRedirects() {
            return followAllRedirects;
        }

        /**
         *
         * @return
         */
        public boolean isFollowRedirect() {
            return followRedirect;
        }

        /**
         *
         * @return
         */
        public String getFilename() {
            return filename;
        }

        /**
         *
         * @return
         */
        public ResponseFormat getResponseFormat() {
            return responseFormat;
        }

        /**
         *
         * @return
         */
        public String getProxy() {
            return proxy;
        }

        /**
         *
         * @return
         */
        public Duration getTimeout() {
            return timeout;
        }
    }

    /**
     *
     */
    public static class Executor {

        private Configuration configuration = new Configuration();
        private Map<String, List<String>> headers = Collections.emptyMap();
        private Map<String, List<String>> queryParameters = Collections.emptyMap();
        private Body body;
        private RequestMethod requestMethod;
        private String url;

        private Executor(String url, RequestMethod requestMethod) {
            this.url = Objects.requireNonNull(url);
            this.requestMethod = Objects.requireNonNull(requestMethod);
        }

        /**
         *
         * @param configuration
         * @return
         */
        public Executor configuration(Configuration configuration) {
            this.configuration = Objects.requireNonNull(configuration);

            return this;
        }

        /**
         *
         * @param headers
         * @return
         */
        public Executor headers(Map<String, List<String>> headers) {
            if (headers != null) {
                this.headers = new HashMap<>(headers);
            }

            return this;
        }

        /**
         *
         * @param queryParameters
         * @return
         */
        public Executor queryParameters(Map<String, List<String>> queryParameters) {
            if (queryParameters != null) {
                this.queryParameters = new HashMap<>(queryParameters);
            }

            return this;
        }

        /**
         *
         * @param body
         * @return
         */
        public Executor body(Body body) {
            this.body = body;

            return this;
        }

        /**
         *
         * @return
         */
        public Response execute() {
            try {
                return httpClientExecutor.execute(url, headers, queryParameters, body, configuration, requestMethod);
            } catch (Exception e) {
                throw new ComponentExecutionException("Unable to execute HTTP request", e);
            }
        }
    }

    /**
     *
     */
    public static class Body {

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

    interface HttpClientExecutor {

        Response execute(
            String urlString, Map<String, List<String>> headers, Map<String, List<String>> queryParameters,
            Body body, Configuration configuration, RequestMethod requestMethod) throws Exception;
    }
}
