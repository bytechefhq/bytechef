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

package com.bytechef.commons.util;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;

/**
 * @author Ivica Cardic
 */
public class PrivateAddressGuardedHttpClient extends HttpClient {

    private static final String AUTHORIZATION = "Authorization";
    private static final int MAX_REDIRECTS = 5;

    private final Set<String> allowedPrivateHosts;
    private final HttpClient delegate;

    public PrivateAddressGuardedHttpClient(HttpClient.Builder builder, Set<String> allowedPrivateHosts) {
        this.allowedPrivateHosts = Set.copyOf(allowedPrivateHosts);
        this.delegate = builder.followRedirects(Redirect.NEVER)
            .build();
    }

    public static void replaceHttpClient(Object owner, String fieldName, Set<String> allowedPrivateHosts) {
        Field field = findField(owner.getClass(), fieldName);

        try {
            field.setAccessible(true);

            HttpClient httpClient = (HttpClient) field.get(owner);

            HttpClient.Builder builder = newBuilder()
                .version(httpClient.version());

            httpClient.connectTimeout()
                .ifPresent(builder::connectTimeout);

            field.set(owner, new PrivateAddressGuardedHttpClient(builder, allowedPrivateHosts));
        } catch (ReflectiveOperationException | RuntimeException exception) {
            throw new IllegalStateException(
                "Failed to replace the HTTP client of " + owner.getClass()
                    .getName(),
                exception);
        }
    }

    @Override
    public Optional<Authenticator> authenticator() {
        return delegate.authenticator();
    }

    @Override
    public boolean awaitTermination(Duration duration) throws InterruptedException {
        return delegate.awaitTermination(duration);
    }

    @Override
    public void close() {
        delegate.close();
    }

    @Override
    public Optional<Duration> connectTimeout() {
        return delegate.connectTimeout();
    }

    @Override
    public Optional<CookieHandler> cookieHandler() {
        return delegate.cookieHandler();
    }

    @Override
    public Optional<Executor> executor() {
        return delegate.executor();
    }

    @Override
    public Redirect followRedirects() {
        return Redirect.NORMAL;
    }

    @Override
    public boolean isTerminated() {
        return delegate.isTerminated();
    }

    @Override
    public Optional<ProxySelector> proxy() {
        return delegate.proxy();
    }

    @Override
    public <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler)
        throws IOException, InterruptedException {

        HttpRequest currentRequest = request;

        for (int redirectCount = 0;; redirectCount++) {
            validate(currentRequest.uri());

            HttpResponse<T> response = delegate.send(currentRequest, responseBodyHandler);

            Optional<URI> location = getRedirectLocation(currentRequest.uri(), response);

            if (location.isEmpty()) {
                return response;
            }

            if (redirectCount >= MAX_REDIRECTS) {
                throw new IOException("Too many redirects for " + request.uri());
            }

            currentRequest = createRedirectRequest(currentRequest, response.statusCode(), location.get());
        }
    }

    @Override
    public <T> CompletableFuture<HttpResponse<T>> sendAsync(
        HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                return send(request, responseBodyHandler);
            } catch (IOException ioException) {
                throw new CompletionException(ioException);
            } catch (InterruptedException interruptedException) {
                Thread.currentThread()
                    .interrupt();

                throw new CompletionException(interruptedException);
            }
        }, runnable -> Thread.ofVirtual()
            .start(runnable));
    }

    @Override
    public <T> CompletableFuture<HttpResponse<T>> sendAsync(
        HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler,
        HttpResponse.PushPromiseHandler<T> pushPromiseHandler) {

        return sendAsync(request, responseBodyHandler);
    }

    @Override
    public void shutdown() {
        delegate.shutdown();
    }

    @Override
    public void shutdownNow() {
        delegate.shutdownNow();
    }

    @Override
    public SSLContext sslContext() {
        return delegate.sslContext();
    }

    @Override
    public SSLParameters sslParameters() {
        return delegate.sslParameters();
    }

    @Override
    public Version version() {
        return delegate.version();
    }

    private static HttpRequest createRedirectRequest(HttpRequest request, int statusCode, URI location) {
        URI uri = request.uri();

        boolean sameOrigin = Objects.equals(uri.getScheme(), location.getScheme()) &&
            Objects.equals(uri.getRawAuthority(), location.getRawAuthority());

        HttpRequest.Builder builder = HttpRequest.newBuilder(
            request, (name, value) -> sameOrigin || !AUTHORIZATION.equalsIgnoreCase(name))
            .uri(location);

        if (statusCode == 303 || ((statusCode == 301 || statusCode == 302) && !"GET".equals(request.method()))) {
            builder.GET();
        }

        return builder.build();
    }

    private static Field findField(Class<?> type, String fieldName) {
        for (Class<?> currentType = type; currentType != null; currentType = currentType.getSuperclass()) {
            for (Field field : currentType.getDeclaredFields()) {
                if (field.getName()
                    .equals(fieldName) && HttpClient.class.isAssignableFrom(field.getType())) {

                    return field;
                }
            }
        }

        throw new IllegalStateException(
            "%s has no HttpClient field named %s".formatted(type.getName(), fieldName));
    }

    private static <T> Optional<URI> getRedirectLocation(URI uri, HttpResponse<T> response) {
        int statusCode = response.statusCode();

        if (statusCode != 301 && statusCode != 302 && statusCode != 303 && statusCode != 307 && statusCode != 308) {
            return Optional.empty();
        }

        HttpHeaders headers = response.headers();

        return headers.firstValue("Location")
            .map(location -> uri.resolve(location.strip()))
            .filter(location -> !isDowngrade(uri, location));
    }

    private static boolean isDowngrade(URI uri, URI location) {
        return "https".equalsIgnoreCase(uri.getScheme()) && "http".equalsIgnoreCase(location.getScheme());
    }

    private void validate(URI uri) throws IOException {
        try {
            UrlValidatorUtils.validate(uri.toString(), allowedPrivateHosts);
        } catch (UrlValidationException urlValidationException) {
            throw new IOException(urlValidationException.getMessage(), urlValidationException);
        }
    }
}
