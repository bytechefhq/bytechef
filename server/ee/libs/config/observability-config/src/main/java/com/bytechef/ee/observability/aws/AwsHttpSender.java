/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.observability.aws;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.opentelemetry.common.ComponentLoader;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.common.export.Compressor;
import io.opentelemetry.sdk.common.export.HttpResponse;
import io.opentelemetry.sdk.common.export.HttpSender;
import io.opentelemetry.sdk.common.export.HttpSenderConfig;
import io.opentelemetry.sdk.common.export.HttpSenderProvider;
import io.opentelemetry.sdk.common.export.MessageWriter;
import io.opentelemetry.sdk.common.export.ProxyOptions;
import io.opentelemetry.sdk.common.export.RetryPolicy;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OTLP/HTTP {@link HttpSender} that signs every request with AWS Signature Version 4.
 *
 * <p>
 * The stock OkHttp and JDK senders build the request body internally and never expose it, while the signature must
 * cover the SHA-256 hash of the exact bytes on the wire. This sender therefore owns the whole request: it serializes
 * (and compresses) the export message once, then signs and posts those bytes, re-signing on every retry attempt so the
 * {@code X-Amz-Date} stays fresh and rotated credentials are picked up.
 *
 * <p>
 * Exporters pick their sender through the {@link ComponentLoader}, see {@link #componentLoader}.
 *
 * @version ee
 *
 * @author Igor Beslic
 */
public final class AwsHttpSender implements HttpSender {

    private static final Logger log = LoggerFactory.getLogger(AwsHttpSender.class);

    private static final int MAX_LOGGED_RESPONSE_BODY_LENGTH = 1024;
    private static final Set<Integer> RETRYABLE_STATUS_CODES = Set.of(429, 502, 503, 504);

    private final @Nullable Compressor compressor;
    private final String contentType;
    private final Supplier<AwsSigV4Signer.Credentials> credentialsSupplier;
    private final URI endpoint;
    private final ExecutorService executorService;
    private final Supplier<Map<String, List<String>>> headersSupplier;
    private final HttpClient httpClient;
    private final boolean managedExecutorService;
    private final long maxResponseBodySize;
    private final RetryPolicy retryPolicy;
    private final AwsSigV4Signer signer;
    private final Duration timeout;

    AwsHttpSender(
        HttpSenderConfig httpSenderConfig, AwsSigV4Signer signer,
        Supplier<AwsSigV4Signer.Credentials> credentialsSupplier) {

        ExecutorService configuredExecutorService = httpSenderConfig.getExecutorService();

        this.compressor = httpSenderConfig.getCompressor();
        this.contentType = httpSenderConfig.getContentType();
        this.credentialsSupplier = credentialsSupplier;
        this.endpoint = httpSenderConfig.getEndpoint();
        this.executorService = configuredExecutorService == null
            ? Executors.newThreadPerTaskExecutor(Thread.ofVirtual()
                .name("otlp-aws-sender-", 0)
                .factory())
            : configuredExecutorService;
        this.headersSupplier = httpSenderConfig.getHeadersSupplier();
        this.httpClient = createHttpClient(httpSenderConfig);
        this.managedExecutorService = configuredExecutorService == null;
        this.maxResponseBodySize = httpSenderConfig.getMaxResponseBodySize();
        this.retryPolicy = getRetryPolicy(httpSenderConfig);
        this.signer = signer;
        this.timeout = httpSenderConfig.getTimeout();
    }

    /**
     * Returns a {@link ComponentLoader} to pass to an OTLP/HTTP exporter builder's {@code setComponentLoader} so that
     * exporter, and only that exporter, sends through an {@link AwsHttpSender}. Every other SPI lookup falls through to
     * the regular {@link java.util.ServiceLoader} based loader.
     */
    public static ComponentLoader componentLoader(
        AwsSigV4Signer signer, Supplier<AwsSigV4Signer.Credentials> credentialsSupplier) {

        HttpSenderProvider httpSenderProvider = httpSenderConfig -> new AwsHttpSender(
            httpSenderConfig, signer, credentialsSupplier);
        ComponentLoader serviceLoaderComponentLoader = ComponentLoader.forClassLoader(
            AwsHttpSender.class.getClassLoader());

        return new ComponentLoader() {

            @Override
            public <T> Iterable<T> load(Class<T> spiClass) {
                if (spiClass == HttpSenderProvider.class) {
                    return List.of(spiClass.cast(httpSenderProvider));
                }

                return serviceLoaderComponentLoader.load(spiClass);
            }
        };
    }

    @Override
    public void send(
        @ParametersAreNonnullByDefault MessageWriter messageWriter,
        @ParametersAreNonnullByDefault Consumer<HttpResponse> onResponse,
        @ParametersAreNonnullByDefault Consumer<Throwable> onError) {
        try {
            executorService.execute(() -> {
                HttpResponse httpResponse;

                try {
                    httpResponse = sendWithRetries(serialize(messageWriter));
                } catch (IOException | RuntimeException exception) {
                    onError.accept(exception);

                    return;
                }

                onResponse.accept(httpResponse);
            });
        } catch (RejectedExecutionException rejectedExecutionException) {
            onError.accept(rejectedExecutionException);
        }
    }

    @Override
    public CompletableResultCode shutdown() {
        if (managedExecutorService) {
            executorService.shutdown();
        }

        httpClient.shutdown();

        return CompletableResultCode.ofSuccess();
    }

    private static HttpClient createHttpClient(HttpSenderConfig httpSenderConfig) {
        // HTTP/1.1 keeps the Host header, which is part of the signature, under our control; with HTTP/2 it becomes
        // the :authority pseudo-header
        HttpClient.Builder builder = HttpClient.newBuilder()
            .connectTimeout(httpSenderConfig.getConnectTimeout())
            .version(HttpClient.Version.HTTP_1_1);

        ProxyOptions proxyOptions = httpSenderConfig.getProxyOptions();

        if (proxyOptions != null) {
            builder.proxy(proxyOptions.getProxySelector());
        }

        SSLContext sslContext = httpSenderConfig.getSslContext();

        if (sslContext != null) {
            builder.sslContext(sslContext);
        }

        return builder.build();
    }

    private static boolean isRetryable(IOException ioException, @Nullable RetryPolicy retryPolicy) {
        if (retryPolicy != null && retryPolicy.getRetryExceptionPredicate() != null) {
            return retryPolicy.getRetryExceptionPredicate()
                .test(ioException);
        }

        return !(ioException instanceof SSLException);
    }

    private Map<String, String> getRequestHeaders() {
        Map<String, String> requestHeaders = new LinkedHashMap<>();

        Map<String, List<String>> configuredHeaders = headersSupplier.get();

        if (configuredHeaders != null) {
            configuredHeaders.forEach((name, values) -> requestHeaders.put(name, String.join(",", values)));
        }

        requestHeaders.put("Content-Type", contentType);

        if (compressor != null) {
            requestHeaders.put("Content-Encoding", compressor.getEncoding());
        }

        return requestHeaders;
    }

    private RetryPolicy getRetryPolicy(HttpSenderConfig httpSenderConfig) {
        if (httpSenderConfig.getRetryPolicy() != null) {
            return httpSenderConfig.getRetryPolicy();
        }

        RetryPolicy.RetryPolicyBuilder retryPolicyBuilder = RetryPolicy.builder();

        return retryPolicyBuilder.setMaxAttempts(
            1)
            .setInitialBackoff(
                Duration.ofSeconds(0))
            .build();
    }

    private HttpResponse sendOnce(byte[] body, Duration attemptTimeout) throws IOException {
        Map<String, String> requestHeaders = getRequestHeaders();

        Map<String, String> signatureHeaders = signer.sign(
            "POST", endpoint, requestHeaders, body, credentialsSupplier.get());

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(endpoint)
            .timeout(attemptTimeout)
            .POST(HttpRequest.BodyPublishers.ofByteArray(body));

        requestHeaders.forEach(requestBuilder::header);
        signatureHeaders.forEach(requestBuilder::header);

        java.net.http.HttpResponse<InputStream> response;

        try {
            response = httpClient.send(requestBuilder.build(), BodyHandlers.ofInputStream());
        } catch (InterruptedException interruptedException) {
            Thread.currentThread()
                .interrupt();

            throw new InterruptedIOException("OTLP export to " + endpoint + " was interrupted");
        }

        byte[] responseBody;

        try (InputStream inputStream = response.body()) {
            responseBody = inputStream.readNBytes((int) Math.min(maxResponseBodySize, Integer.MAX_VALUE));
        }

        int statusCode = response.statusCode();

        if (statusCode >= 400 && log.isWarnEnabled()) {
            // AWS explains signature failures in the body, e.g. with the canonical request it expected
            String responseBodyText = new String(responseBody, StandardCharsets.UTF_8);

            log.warn(
                "AWS SigV4 signed OTLP export to {} failed with HTTP status {}: {}", endpoint, statusCode,
                responseBodyText.length() > MAX_LOGGED_RESPONSE_BODY_LENGTH
                    ? responseBodyText.substring(0, MAX_LOGGED_RESPONSE_BODY_LENGTH) : responseBodyText);
        }

        return new SignedHttpResponse(statusCode, responseBody);
    }

    private HttpResponse sendWithRetries(byte[] body) throws IOException {
        int maxAttempts = retryPolicy.getMaxAttempts();
        long totalWaitTimeNanos = 0;
        long waitTimeNanos = retryPolicy.getInitialBackoff()
            .toNanos();

        HttpResponse httpResponse = null;
        IOException ioException = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            if (attempt > 1) {
                waitTimeNanos = nextWaitTimeNanos(waitTimeNanos);

                totalWaitTimeNanos += waitTimeNanos;

                if (totalWaitTimeNanos >= timeout.toNanos()) {
                    break;
                }

                sleep(waitTimeNanos);
            }

            httpResponse = null;
            ioException = null;

            Duration remainingTimeout = Duration.ofNanos(
                Math.max(timeout.toNanos(), TimeUnit.MILLISECONDS.toNanos(1)));

            try {
                httpResponse = sendOnce(body, remainingTimeout);

                if (!RETRYABLE_STATUS_CODES.contains(httpResponse.getStatusCode())) {
                    return httpResponse;
                }
            } catch (IOException exception) {
                if (!isRetryable(exception, retryPolicy)) {
                    throw exception;
                }

                ioException = exception;
            }
        }

        if (httpResponse != null) {
            return httpResponse;
        }

        throw ioException == null ? new IOException("OTLP export to " + endpoint + " timed out") : ioException;
    }

    @SuppressFBWarnings(value = "PREDICTABLE_RANDOM", justification = "Retry wait time, not security sensitive")
    private long nextWaitTimeNanos(long initialWaitTimeNanos) {
        long waitTimeSeedNanos = Math.min(
            initialWaitTimeNanos, retryPolicy.getMaxBackoff()
                .toNanos());

        return (long) (waitTimeSeedNanos * retryPolicy.getBackoffMultiplier()
            * ThreadLocalRandom.current()
                .nextDouble(0.8d, 1.2d));
    }

    private byte[] serialize(MessageWriter messageWriter) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(
            Math.max(messageWriter.getContentLength(), 32));

        if (compressor == null) {
            messageWriter.writeMessage(byteArrayOutputStream);
        } else {
            try (OutputStream compressedOutputStream = compressor.compress(byteArrayOutputStream)) {
                messageWriter.writeMessage(compressedOutputStream);
            }
        }

        return byteArrayOutputStream.toByteArray();
    }

    private static void sleep(long nanos) throws InterruptedIOException {
        try {
            TimeUnit.NANOSECONDS.sleep(nanos);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread()
                .interrupt();

            throw new InterruptedIOException("OTLP export retry backoff was interrupted");
        }
    }

    @SuppressFBWarnings("EI")
    private record SignedHttpResponse(int statusCode, byte[] responseBody) implements HttpResponse {

        @Override
        public int getStatusCode() {
            return statusCode;
        }

        @Override
        public String getStatusMessage() {
            return String.valueOf(statusCode);
        }

        @Override
        public byte[] getResponseBody() {
            return responseBody;
        }
    }
}
