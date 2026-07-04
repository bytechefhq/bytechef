/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.customcomponent.loader;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context.Http;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.graalvm.polyglot.HostAccess;

/**
 * Host callback object handed to the GraalVM Espresso guest through the polyglot bindings. The
 * {@code @HostAccess.Export} methods below are the ONLY host surface reachable from sandboxed custom component code —
 * keep this set minimal and treat every argument as attacker-controlled. HTTP requests execute through the live host
 * {@link ActionContext} so platform proxy/authorization behavior applies.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class HostContextBridge {

    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder()
        .findAndAddModules()
        .build();

    private final ActionContext actionContext;

    HostContextBridge(ActionContext actionContext) {
        this.actionContext = actionContext;
    }

    @HostAccess.Export
    @SuppressWarnings("unchecked")
    public String httpExecute(String requestJson) {
        try {
            Map<String, ?> request = OBJECT_MAPPER.readValue(requestJson, Map.class);

            Http.Response response = actionContext.http(http -> executeRequest(http, request));

            Map<String, Object> responseMap = new LinkedHashMap<>();

            responseMap.put("statusCode", response.getStatusCode());
            responseMap.put("headers", response.getHeaders());
            responseMap.put("body", response.getBody());

            return OBJECT_MAPPER.writeValueAsString(responseMap);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @HostAccess.Export
    public boolean isEditorEnvironment() {
        return actionContext.isEditorEnvironment();
    }

    @HostAccess.Export
    public void log(String level, String message, String exceptionMessage) {
        String fullMessage = exceptionMessage == null ? message : message + ": " + exceptionMessage;

        actionContext.log(log -> {
            switch (level) {
                case "TRACE" -> log.trace(fullMessage);
                case "DEBUG" -> log.debug(fullMessage);
                case "WARN" -> log.warn(fullMessage);
                case "ERROR" -> log.error(fullMessage);
                default -> log.info(fullMessage);
            }
        });
    }

    @SuppressWarnings("unchecked")
    private static Http.Response executeRequest(Http http, Map<String, ?> request) {
        String url = (String) request.get("url");

        Http.RequestMethod requestMethod = Http.RequestMethod.valueOf((String) request.get("method"));

        Http.Executor executor = http.exchange(url, requestMethod);

        Map<String, List<String>> headers = (Map<String, List<String>>) request.get("headers");

        if (headers != null && !headers.isEmpty()) {
            executor = executor.headers(headers);
        }

        Map<String, List<String>> queryParameters = (Map<String, List<String>>) request.get("queryParameters");

        if (queryParameters != null && !queryParameters.isEmpty()) {
            executor = executor.queryParameters(queryParameters);
        }

        Map<String, ?> body = (Map<String, ?>) request.get("body");

        if (body != null) {
            executor = executor.body(toBody(body));
        }

        Map<String, ?> configuration = (Map<String, ?>) request.get("configuration");

        if (configuration != null) {
            executor = executor.configuration(toConfigurationBuilder(configuration));
        }

        return executor.execute();
    }

    @SuppressWarnings("unchecked")
    private static Http.Body toBody(Map<String, ?> body) {
        Object content = body.get("content");

        Http.BodyContentType bodyContentType = Http.BodyContentType.valueOf((String) body.get("contentType"));

        return switch (bodyContentType) {
            case JSON, FORM_URL_ENCODED, FORM_DATA -> {
                if (content instanceof List<?> listContent) {
                    yield Http.Body.of(listContent, bodyContentType);
                }

                yield Http.Body.of((Map<String, ?>) content, bodyContentType);
            }
            case RAW, XML -> {
                String mimeType = (String) body.get("mimeType");

                yield mimeType == null
                    ? Http.Body.of(String.valueOf(content), bodyContentType)
                    : Http.Body.of(String.valueOf(content), mimeType);
            }
            case BINARY -> throw new UnsupportedOperationException(
                "BINARY HTTP bodies are not supported in the Espresso custom component sandbox");
        };
    }

    private static Http.Configuration.ConfigurationBuilder toConfigurationBuilder(Map<String, ?> configuration) {
        Http.Configuration.ConfigurationBuilder configurationBuilder = Http.Configuration.newConfiguration();

        if (Boolean.TRUE.equals(configuration.get("allowUnauthorizedCerts"))) {
            configurationBuilder.allowUnauthorizedCerts(true);
        }

        if (Boolean.TRUE.equals(configuration.get("disableAuthorization"))) {
            configurationBuilder.disableAuthorization(true);
        }

        if (Boolean.TRUE.equals(configuration.get("followAllRedirects"))) {
            configurationBuilder.followAllRedirects(true);
        }

        if (Boolean.TRUE.equals(configuration.get("followRedirect"))) {
            configurationBuilder.followRedirect(true);
        }

        String responseType = (String) configuration.get("responseType");

        if (responseType != null) {
            configurationBuilder.responseType(Http.ResponseType.valueOf(responseType));
        }

        Number timeoutMillis = (Number) configuration.get("timeoutMillis");

        if (timeoutMillis != null) {
            configurationBuilder.timeout(Duration.ofMillis(timeoutMillis.longValue()));
        }

        return configurationBuilder;
    }
}
