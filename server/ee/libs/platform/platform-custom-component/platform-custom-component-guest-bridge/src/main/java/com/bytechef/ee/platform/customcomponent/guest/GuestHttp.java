/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.customcomponent.guest;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Guest-side {@link Context.Http} implementation. The request is accumulated into a JSON-serializable map and shipped
 * to the host through {@link HostBridge#httpExecute(String)}; the host executes it with the live host
 * {@code ActionContext} so platform proxy/authorization behavior applies, then returns
 * {@code {statusCode, headers, body}} JSON.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class GuestHttp implements Context.Http {

    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder()
        .findAndAddModules()
        .build();

    private final HostBridge hostBridge;

    GuestHttp(HostBridge hostBridge) {
        this.hostBridge = hostBridge;
    }

    @Override
    public Executor delete(String url) {
        return new GuestExecutor(url, RequestMethod.DELETE);
    }

    @Override
    public Executor exchange(String url, RequestMethod requestMethod) {
        return new GuestExecutor(url, requestMethod);
    }

    @Override
    public Executor head(String url) {
        return new GuestExecutor(url, RequestMethod.HEAD);
    }

    @Override
    public Executor get(String url) {
        return new GuestExecutor(url, RequestMethod.GET);
    }

    @Override
    public Executor patch(String url) {
        return new GuestExecutor(url, RequestMethod.PATCH);
    }

    @Override
    public Executor post(String url) {
        return new GuestExecutor(url, RequestMethod.POST);
    }

    @Override
    public Executor put(String url) {
        return new GuestExecutor(url, RequestMethod.PUT);
    }

    private class GuestExecutor implements Executor {

        private Body body;
        private Configuration configuration = Configuration.newConfiguration()
            .build();
        private final Map<String, List<String>> headers = new LinkedHashMap<>();
        private final Map<String, List<String>> queryParameters = new LinkedHashMap<>();
        private final RequestMethod requestMethod;
        private final String url;

        private GuestExecutor(String url, RequestMethod requestMethod) {
            this.url = url;
            this.requestMethod = requestMethod;
        }

        @Override
        public Executor configuration(Configuration.ConfigurationBuilder configurationBuilder) {
            this.configuration = configurationBuilder.build();

            return this;
        }

        @Override
        public Executor header(String name, String value) {
            headers.put(name, List.of(value));

            return this;
        }

        @Override
        public Executor headers(Map<String, List<String>> headers) {
            this.headers.putAll(headers);

            return this;
        }

        @Override
        public Executor queryParameter(String name, String value) {
            queryParameters.put(name, List.of(value));

            return this;
        }

        @Override
        public Executor queryParameters(Map<String, List<String>> queryParameters) {
            this.queryParameters.putAll(queryParameters);

            return this;
        }

        @Override
        public Executor queryParameters(Object... keyValueArray) {
            for (int index = 0; index < keyValueArray.length; index += 2) {
                Object value = keyValueArray[index + 1];

                if (keyValueArray[index] != null && value != null) {
                    queryParameters.put(String.valueOf(keyValueArray[index]), List.of(String.valueOf(value)));
                }
            }

            return this;
        }

        @Override
        public Executor body(Body body) {
            this.body = body;

            return this;
        }

        @Override
        public Response execute() {
            Map<String, Object> request = new HashMap<>();

            request.put("method", requestMethod.name());
            request.put("url", url);
            request.put("headers", headers);
            request.put("queryParameters", queryParameters);

            if (body != null) {
                BodyContentType bodyContentType = body.getContentType();

                if (bodyContentType == BodyContentType.BINARY || bodyContentType == BodyContentType.FORM_DATA) {
                    throw new UnsupportedOperationException(
                        "HTTP body content type %s is not supported in the Espresso custom component sandbox; set "
                            .formatted(bodyContentType) +
                            "bytechef.component.custom-component.java-loader=class-loader to use the in-process " +
                            "loader");
                }

                Map<String, Object> bodyMap = new HashMap<>();

                bodyMap.put("content", body.getContent());
                bodyMap.put("contentType", bodyContentType.name());
                bodyMap.put("mimeType", body.getMimeType());

                request.put("body", bodyMap);
            }

            Map<String, Object> configurationMap = new HashMap<>();

            configurationMap.put("allowUnauthorizedCerts", configuration.isAllowUnauthorizedCerts());
            configurationMap.put("disableAuthorization", configuration.isDisableAuthorization());
            configurationMap.put("followAllRedirects", configuration.isFollowAllRedirects());
            configurationMap.put("followRedirect", configuration.isFollowRedirect());

            ResponseType responseType = configuration.getResponseType();

            configurationMap.put(
                "responseType", responseType == null ? null
                    : responseType.getType()
                        .name());

            Duration timeout = configuration.getTimeout();

            configurationMap.put("timeoutMillis", timeout == null ? null : timeout.toMillis());

            request.put("configuration", configurationMap);

            String responseJson = hostBridge.httpExecute(writeJson(request));

            return new GuestResponse(readJsonMap(responseJson));
        }
    }

    private record GuestResponse(Map<String, ?> response) implements Response {

        @Override
        @SuppressWarnings("unchecked")
        public Map<String, List<String>> getHeaders() {
            Map<String, List<String>> responseHeaders = (Map<String, List<String>>) response.get("headers");

            return responseHeaders == null ? Map.of() : responseHeaders;
        }

        @Override
        public Object getBody() {
            return response.get("body");
        }

        @Override
        public <T> T getBody(Class<T> valueType) {
            return OBJECT_MAPPER.convertValue(response.get("body"), valueType);
        }

        @Override
        public <T> T getBody(TypeReference<T> valueTypeRef) {
            TypeFactory typeFactory = OBJECT_MAPPER.getTypeFactory();

            return OBJECT_MAPPER.convertValue(response.get("body"), typeFactory.constructType(valueTypeRef.getType()));
        }

        @Override
        public String getFirstHeader(String name) {
            List<String> values = getHeaders().get(name);

            return values == null || values.isEmpty() ? null : values.getFirst();
        }

        @Override
        public List<String> getHeader(String name) {
            List<String> values = getHeaders().get(name);

            return values == null ? List.of() : values;
        }

        @Override
        public int getStatusCode() {
            Number statusCode = (Number) response.get("statusCode");

            return statusCode.intValue();
        }
    }

    private static String writeJson(Object value) {
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (Exception e) {
            throw new UncheckedIOException(new java.io.IOException(e));
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, ?> readJsonMap(String json) {
        try {
            return OBJECT_MAPPER.readValue(json, Map.class);
        } catch (Exception e) {
            throw new UncheckedIOException(new java.io.IOException(e));
        }
    }
}
