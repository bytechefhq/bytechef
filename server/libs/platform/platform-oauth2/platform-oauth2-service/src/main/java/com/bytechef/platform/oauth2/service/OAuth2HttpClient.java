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

package com.bytechef.platform.oauth2.service;

import com.bytechef.commons.util.UrlValidatorUtils;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.oauth2.exception.OAuth2ErrorType;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class OAuth2HttpClient {

    static final int MAX_RESPONSE_BYTES = 64 * 1024;

    private static final Duration TIMEOUT = Duration.ofSeconds(10);
    @SuppressWarnings("PMD.AvoidUsingHardCodedIP")
    private static final Set<String> DEV_HOSTS = Set.of("localhost", "127.0.0.1");

    private final Set<String> allowedHosts;
    private final Duration timeout;

    @Autowired
    public OAuth2HttpClient(ApplicationProperties applicationProperties, Environment environment) {
        this(applicationProperties, environment, TIMEOUT);
    }

    OAuth2HttpClient(ApplicationProperties applicationProperties, Environment environment, Duration timeout) {
        this.timeout = timeout;

        List<String> configuredHosts = applicationProperties.getOauth2()
            .getDiscovery()
            .getAllowedHosts();

        Set<String> hosts = new HashSet<>();

        if (configuredHosts != null) {
            hosts.addAll(configuredHosts);
        }

        if (environment.acceptsProfiles(Profiles.of("dev"))) {
            hosts.addAll(DEV_HOSTS);
        }

        this.allowedHosts = Set.copyOf(hosts);
    }

    public record Response(int statusCode, String body, HttpHeaders headers) {
    }

    public void validateUrl(String url) {
        if (!UrlValidatorUtils.isValid(url, allowedHosts)) {
            throw new ConfigurationException(
                "This server address isn't allowed: " + url, OAuth2ErrorType.URL_NOT_ALLOWED);
        }

        URI uri = URI.create(url);
        String scheme = uri.getScheme()
            .toLowerCase(Locale.ROOT);

        if (!"https".equals(scheme) && !isAllowlisted(uri.getHost())) {
            throw new ConfigurationException(
                "This server address must use https: " + url, OAuth2ErrorType.URL_NOT_ALLOWED);
        }
    }

    public Response probe(String url, String jsonBody) {
        validateUrl(url);

        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
            .timeout(timeout)
            .header("Content-Type", "application/json")
            .header("Accept", "application/json, text/event-stream")
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
            .build();

        return send(request, false);
    }

    public Response get(String url) {
        validateUrl(url);

        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
            .timeout(timeout)
            .header("Accept", "application/json")
            .GET()
            .build();

        return send(request, true);
    }

    public Response post(String url, String contentType, String body, Map<String, String> headers) {
        validateUrl(url);

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(URI.create(url))
            .timeout(timeout)
            .header("Content-Type", contentType)
            .header("Accept", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body));

        headers.forEach(requestBuilder::header);

        return send(requestBuilder.build(), true);
    }

    private Response send(HttpRequest request, boolean readBody) {
        try (HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NEVER)
            .connectTimeout(timeout)
            .build();
            ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor()) {

            Future<Response> future = executorService.submit(() -> exchange(httpClient, request, readBody));

            try {
                return future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
            } catch (TimeoutException timeoutException) {
                future.cancel(true);
                httpClient.shutdownNow();

                throw new ConfigurationException(
                    "Timed out calling " + request.uri(), timeoutException, OAuth2ErrorType.METADATA_NOT_FOUND);
            } catch (ExecutionException executionException) {
                Throwable cause = executionException.getCause();

                if (cause instanceof ConfigurationException configurationException) {
                    throw configurationException;
                }

                throw new ConfigurationException(
                    "Unable to reach " + request.uri() + ": " + cause.getMessage(), cause,
                    OAuth2ErrorType.METADATA_NOT_FOUND);
            } catch (InterruptedException interruptedException) {
                future.cancel(true);
                httpClient.shutdownNow();

                Thread.currentThread()
                    .interrupt();

                throw new ConfigurationException(
                    "Interrupted while calling " + request.uri(), interruptedException,
                    OAuth2ErrorType.METADATA_NOT_FOUND);
            }
        }
    }

    private Response exchange(HttpClient httpClient, HttpRequest request, boolean readBody)
        throws IOException, InterruptedException {

        HttpResponse<InputStream> httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

        try (InputStream inputStream = httpResponse.body()) {
            String body = "";

            if (readBody) {
                byte[] bytes = inputStream.readNBytes(MAX_RESPONSE_BYTES + 1);

                if (bytes.length > MAX_RESPONSE_BYTES) {
                    throw new ConfigurationException(
                        "Response larger than " + MAX_RESPONSE_BYTES + " bytes", OAuth2ErrorType.INVALID_METADATA);
                }

                body = new String(bytes, StandardCharsets.UTF_8);
            }

            return new Response(httpResponse.statusCode(), body, httpResponse.headers());
        }
    }

    private boolean isAllowlisted(String host) {
        return host != null && allowedHosts.stream()
            .anyMatch(allowedHost -> allowedHost.equalsIgnoreCase(host));
    }
}
