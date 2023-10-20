
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

import com.bytechef.hermes.component.definition.Context.FileEntry;
import com.bytechef.hermes.component.exception.ComponentExecutionException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

/**
 * @author Matija Petanjek
 * @author Ivica Cardic
 */
public final class HttpClientUtils {

    private static final Logger logger = LoggerFactory.getLogger(HttpClientUtils.class);

    static HttpClientExecutor httpClientExecutor;

    static {
        try {
            ServiceLoader<HttpClientExecutor> loader = ServiceLoader.load(HttpClientExecutor.class);

            httpClientExecutor = loader.findFirst()
                .orElse(null);
        } catch (ServiceConfigurationError e) {
            if (logger.isDebugEnabled()) {
                logger.debug(e.getMessage(), e);
            }
        }

        if (httpClientExecutor == null && logger.isWarnEnabled()) {
            logger.warn("HttpClientExecutor instance is not available");
        }
    }

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
    public enum ResponseType {
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

    private HttpClientUtils() {
    }

    /**
     *
     * @param allowUnauthorizedCerts
     * @return
     */
    public static Configuration.ConfigurationBuilder allowUnauthorizedCerts(boolean allowUnauthorizedCerts) {
        Configuration.ConfigurationBuilder configurationBuilder = new Configuration.ConfigurationBuilder();

        configurationBuilder.allowUnauthorizedCerts = allowUnauthorizedCerts;

        return configurationBuilder;
    }

    /**
     *
     * @param followAllRedirects
     * @return
     */
    public static Configuration.ConfigurationBuilder followAllRedirects(boolean followAllRedirects) {
        Configuration.ConfigurationBuilder configurationBuilder = new Configuration.ConfigurationBuilder();

        configurationBuilder.followAllRedirects = followAllRedirects;

        return configurationBuilder;
    }

    /**
     *
     * @param filename
     * @return
     */
    public static Configuration.ConfigurationBuilder filename(String filename) {
        Configuration.ConfigurationBuilder configurationBuilder = new Configuration.ConfigurationBuilder();

        configurationBuilder.filename = filename;

        return configurationBuilder;
    }

    /**
     *
     * @param followRedirect
     * @return
     */
    public static Configuration.ConfigurationBuilder followRedirect(boolean followRedirect) {
        Configuration.ConfigurationBuilder configurationBuilder = new Configuration.ConfigurationBuilder();

        configurationBuilder.followRedirect = followRedirect;

        return configurationBuilder;
    }

    /**
     *
     * @param proxy
     * @return
     */
    public static Configuration.ConfigurationBuilder proxy(String proxy) {
        Configuration.ConfigurationBuilder configurationBuilder = new Configuration.ConfigurationBuilder();

        configurationBuilder.proxy = proxy;

        return configurationBuilder;
    }

    /**
     *
     * @param responseType
     * @return
     */
    public static Configuration.ConfigurationBuilder responseType(ResponseType responseType) {
        Configuration.ConfigurationBuilder configurationBuilder = new Configuration.ConfigurationBuilder();

        configurationBuilder.responseType = responseType;

        return configurationBuilder;
    }

    /**
     *
     * @param timeout
     * @return
     */
    public static Configuration.ConfigurationBuilder timeout(Duration timeout) {
        Configuration.ConfigurationBuilder configurationBuilder = new Configuration.ConfigurationBuilder();

        configurationBuilder.timeout = timeout;

        return configurationBuilder;
    }

    /**
     *
     * @param url
     * @return
     */
    public static Executor delete(String url) {
        return new Executor(url, RequestMethod.DELETE);
    }

    /**
     *
     * @param url
     * @param requestMethod
     * @return
     */
    public static Executor exchange(String url, RequestMethod requestMethod) {
        return new Executor(url, requestMethod);
    }

    /**
     *
     * @param url
     * @return
     */
    public static Executor head(String url) {
        return new Executor(url, RequestMethod.HEAD);
    }

    /**
     *
     * @param url
     * @return
     */
    public static Executor get(String url) {
        return new Executor(url, RequestMethod.GET);
    }

    /**
     *
     * @param url
     * @return
     */
    public static Executor patch(String url) {
        return new Executor(url, RequestMethod.PATCH);
    }

    /**
     *
     * @param url
     * @return
     */
    public static Executor post(String url) {
        return new Executor(url, RequestMethod.POST);
    }

    /**
     *
     * @param url
     * @return
     */
    public static Executor put(String url) {
        return new Executor(url, RequestMethod.PUT);
    }

    /**
     *
     */
    @SuppressFBWarnings("EI")
    public record Response(Map<String, List<String>> headers, Object body, int statusCode) {

        @SuppressWarnings("unchecked")
        public <T> T getBody() {
            return (T) body;
        }
    }

    public static class Configuration {

        private boolean allowUnauthorizedCerts;
        private String filename;
        private boolean followAllRedirects;
        private boolean followRedirect;
        private String proxy;
        private ResponseType responseType;
        private Duration timeout;

        Configuration() {
        }

        public static Configuration.ConfigurationBuilder newConfiguration() {
            return new Configuration.ConfigurationBuilder();
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

        public static final class ConfigurationBuilder {

            private boolean allowUnauthorizedCerts;
            private String filename;
            private boolean followAllRedirects;
            private boolean followRedirect;
            private String proxy;
            private ResponseType responseType;
            private Duration timeout = Duration.ofMillis(1000);

            private ConfigurationBuilder() {
            }

            public Configuration.ConfigurationBuilder allowUnauthorizedCerts(boolean allowUnauthorizedCerts) {
                this.allowUnauthorizedCerts = allowUnauthorizedCerts;
                return this;
            }

            public Configuration.ConfigurationBuilder filename(String filename) {
                this.filename = filename;
                return this;
            }

            public Configuration.ConfigurationBuilder followAllRedirects(boolean followAllRedirects) {
                this.followAllRedirects = followAllRedirects;
                return this;
            }

            public Configuration.ConfigurationBuilder followRedirect(boolean followRedirect) {
                this.followRedirect = followRedirect;
                return this;
            }

            public Configuration.ConfigurationBuilder proxy(String proxy) {
                this.proxy = proxy;
                return this;
            }

            public Configuration.ConfigurationBuilder responseType(ResponseType responseType) {
                this.responseType = responseType;
                return this;
            }

            public Configuration.ConfigurationBuilder timeout(Duration timeout) {
                this.timeout = timeout;
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

                return configuration;
            }
        }
    }

    /**
     *
     */
    public static class Executor {

        private Configuration configuration = new Configuration();
        private Map<String, List<String>> headers = new HashMap<>();
        private Map<String, List<String>> queryParameters = new HashMap<>();
        private Body body;
        private final RequestMethod requestMethod;
        private final String url;

        private Executor(String url, RequestMethod requestMethod) {
            this.url = Objects.requireNonNull(url);
            this.requestMethod = Objects.requireNonNull(requestMethod);
        }

        /**
         *
         * @param configurationBuilder
         * @return
         */
        public Executor configuration(Configuration.ConfigurationBuilder configurationBuilder) {
            this.configuration = Objects.requireNonNull(configurationBuilder)
                .build();

            return this;
        }

        public Executor header(String name, String value) {
            headers.put(Objects.requireNonNull(name), List.of(Objects.requireNonNull(value)));

            return this;
        }

        /**
         *
         * @param headers
         * @return
         */
        public Executor headers(Map<String, List<String>> headers) {
            this.headers = new HashMap<>(Objects.requireNonNull(headers));

            return this;
        }

        public Executor queryParameter(String name, String value) {
            queryParameters.put(Objects.requireNonNull(name), List.of(Objects.requireNonNull(value)));

            return this;
        }

        /**
         *
         * @param queryParameters
         * @return
         */
        public Executor queryParameters(Map<String, List<String>> queryParameters) {
            this.queryParameters = new HashMap<>(Objects.requireNonNull(queryParameters));

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
