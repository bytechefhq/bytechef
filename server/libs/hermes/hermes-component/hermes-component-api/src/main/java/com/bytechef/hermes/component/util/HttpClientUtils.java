
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

/**
 * @author Matija Petanjek
 * @author Ivica Cardic
 */
public final class HttpClientUtils {

    public enum BodyContentType {
        BINARY,
        FORM_DATA,
        FORM_URL_ENCODED,
        JSON,
        RAW,
        XML
    }

    public enum ResponseFormat {
        BINARY,
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

    static HttpClientExecutor httpClientExecutor;

    private HttpClientUtils() {
    }

    public static Configuration allowUnauthorizedCerts(boolean allowUnauthorizedCerts) {
        Configuration configuration = new Configuration();

        configuration.allowUnauthorizedCerts = allowUnauthorizedCerts;

        return configuration;
    }

    public static Configuration followAllRedirects(boolean followAllRedirects) {
        Configuration configuration = new Configuration();

        configuration.followAllRedirects = followAllRedirects;

        return configuration;
    }

    public static Configuration filename(String filename) {
        Configuration configuration = new Configuration();

        configuration.filename = filename;

        return configuration;
    }

    public static Configuration followRedirect(boolean followRedirect) {
        Configuration configuration = new Configuration();

        configuration.followRedirect = followRedirect;

        return configuration;
    }

    public static Configuration proxy(String proxy) {
        Configuration configuration = new Configuration();

        configuration.proxy = proxy;

        return configuration;
    }

    public static Configuration responseFormat(ResponseFormat responseFormat) {
        Configuration configuration = new Configuration();

        configuration.responseFormat = responseFormat;

        return configuration;
    }

    public static Configuration timeout(Duration timeout) {
        Configuration configuration = new Configuration();

        configuration.timeout = timeout;

        return configuration;
    }

    public static Executor delete(String uri) {
        return new Executor().delete(uri);
    }

    public static Executor exchange(String uri, RequestMethod requestMethod) {
        return new Executor().exchange(uri, requestMethod);
    }

    public static Executor head(String uri) {
        return new Executor().head(uri);
    }

    public static Executor get(String uri) {
        return new Executor().get(uri);
    }

    public static Executor patch(String uri) {
        return new Executor().patch(uri);
    }

    public static Executor post(String uri) {
        return new Executor().post(uri);
    }

    public static Executor put(String uri) {
        return new Executor().put(uri);
    }

    @SuppressFBWarnings({
        "EI", "EI2"
    })
    public static class Response {

        private final Object body;
        private final Map<String, List<String>> headers;
        private final int statusCode;

        public Response(Map<String, List<String>> headers, Object body, int statusCode) {
            this.body = body;
            this.headers = headers;
            this.statusCode = statusCode;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }

            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Response response = (Response) o;

            return statusCode == response.statusCode &&
                Objects.equals(body, response.body) && Objects.equals(headers, response.headers);
        }

        @Override
        public int hashCode() {
            return Objects.hash(body, headers, statusCode);
        }

        public Object getBody() {
            return body;
        }

        public Map<String, List<String>> getHeaders() {
            return headers;
        }

        public int getStatusCode() {
            return statusCode;
        }
    }

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

        public static Configuration configuration() {
            return new Configuration();
        }

        public Configuration allowUnauthorizedCerts(boolean allowUnauthorizedCerts) {
            this.allowUnauthorizedCerts = allowUnauthorizedCerts;

            return this;
        }

        public Configuration followAllRedirects(boolean followAllRedirects) {
            this.followAllRedirects = followAllRedirects;

            return this;
        }

        public Configuration filename(String filename) {
            this.filename = filename;

            return this;
        }

        public Configuration followRedirect(boolean followRedirect) {
            this.followRedirect = followRedirect;

            return this;
        }

        public Configuration proxy(String proxy) {
            this.proxy = proxy;

            return this;
        }

        public Configuration responseFormat(ResponseFormat responseFormat) {
            this.responseFormat = responseFormat;

            return this;
        }

        public Configuration timeout(Duration timeout) {
            this.timeout = timeout;

            return this;
        }

        public boolean isAllowUnauthorizedCerts() {
            return allowUnauthorizedCerts;
        }

        public boolean isFollowAllRedirects() {
            return followAllRedirects;
        }

        public boolean isFollowRedirect() {
            return followRedirect;
        }

        public String getFilename() {
            return filename;
        }

        public ResponseFormat getResponseFormat() {
            return responseFormat;
        }

        public String getProxy() {
            return proxy;
        }

        public Duration getTimeout() {
            return timeout;
        }
    }

    public static class Executor {

        private Configuration configuration = new Configuration();
        private Map<String, List<String>> headers = Collections.emptyMap();
        private Map<String, List<String>> queryParameters = Collections.emptyMap();
        private Body body;
        private RequestMethod requestMethod;
        private String url;

        private Executor() {
        }

        public Executor configuration(Configuration configuration) {
            this.configuration = Objects.requireNonNull(configuration);

            return this;
        }

        public Executor delete(String url) {
            this.url = Objects.requireNonNull(url);
            this.requestMethod = RequestMethod.DELETE;

            return this;
        }

        public Executor exchange(String url, RequestMethod requestMethod) {
            this.url = Objects.requireNonNull(url);
            this.requestMethod = Objects.requireNonNull(requestMethod);

            return this;
        }

        public Executor head(String url) {
            this.url = Objects.requireNonNull(url);
            this.requestMethod = RequestMethod.HEAD;

            return this;
        }

        public Executor headers(Map<String, List<String>> headers) {
            if (headers != null) {
                this.headers = new HashMap<>(headers);
            }

            return this;
        }

        public Executor get(String url) {
            this.url = Objects.requireNonNull(url);
            this.requestMethod = RequestMethod.GET;

            return this;
        }

        public Executor queryParameters(Map<String, List<String>> queryParameters) {
            if (queryParameters != null) {
                this.queryParameters = new HashMap<>(queryParameters);
            }

            return this;
        }

        public Executor patch(String url) {
            this.url = Objects.requireNonNull(url);
            this.requestMethod = RequestMethod.PATCH;

            return this;
        }

        public Executor body(Body body) {
            this.body = body;

            return this;
        }

        public Executor post(String url) {
            this.url = Objects.requireNonNull(url);
            this.requestMethod = RequestMethod.POST;

            return this;
        }

        public Executor put(String url) {
            this.url = Objects.requireNonNull(url);
            this.requestMethod = RequestMethod.PUT;

            return this;
        }

        public Response execute() {
            try {
                return httpClientExecutor.execute(url, headers, queryParameters, body, configuration, requestMethod);
            } catch (Exception e) {
                throw new ComponentExecutionException("Unable to execute HTTP request", e);
            }
        }
    }

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

        public static Body of(FileEntry fileEntry) {
            return new Body(fileEntry, BodyContentType.BINARY);
        }

        public static Body of(FileEntry fileEntry, String mimeType) {
            Objects.requireNonNull(fileEntry);

            return new Body(fileEntry, BodyContentType.BINARY, mimeType);
        }

        public static Body of(List<?> list) {
            Objects.requireNonNull(list);

            return new Body(list, BodyContentType.JSON);
        }

        public static Body of(List<?> list, BodyContentType bodyContentType) {
            Objects.requireNonNull(list);
            Objects.requireNonNull(bodyContentType);

            return new Body(list, bodyContentType);
        }

        public static Body of(Map<String, Object> map) {
            Objects.requireNonNull(map);

            return new Body(map, BodyContentType.JSON);
        }

        public static Body of(Map<String, Object> content, BodyContentType bodyContentType) {
            Objects.requireNonNull(content);
            Objects.requireNonNull(bodyContentType);

            return new Body(content, bodyContentType);
        }

        public static Body of(String string) {
            Objects.requireNonNull(string);

            return new Body(string, BodyContentType.RAW, "text/plain");
        }

        public static Body of(String string, String mimeType) {
            Objects.requireNonNull(string);
            Objects.requireNonNull(mimeType);

            return new Body(string, BodyContentType.RAW, mimeType);
        }

        public static Body of(String string, BodyContentType bodyContentType) {
            Objects.requireNonNull(string);
            Objects.requireNonNull(bodyContentType);

            return new Body(string, bodyContentType, null);
        }

        public Object getContent() {
            return content;
        }

        public BodyContentType getContentType() {
            return contentType;
        }

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
