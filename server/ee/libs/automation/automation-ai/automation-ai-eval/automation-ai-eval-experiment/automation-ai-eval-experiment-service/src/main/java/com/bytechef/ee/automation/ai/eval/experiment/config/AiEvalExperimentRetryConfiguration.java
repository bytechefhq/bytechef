/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.eval.experiment.config;

import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.reactive.function.client.WebClientRequestException;

/**
 * Builds the {@link RetryTemplate} that wraps the per-run {@code chatCompletion} call in
 * {@link com.bytechef.ee.automation.ai.eval.experiment.executor.AiEvalExperimentExecutor}. Retries on transient network
 * or upstream-server failures; programmer errors fall through and fail fast.
 *
 * <p>
 * Retryable exception types:
 * <ul>
 * <li>{@link IOException} (and subtypes such as {@link java.net.SocketTimeoutException},
 * {@link java.net.ConnectException}) — raw socket errors when the chat client uses a blocking transport.</li>
 * <li>{@link ResourceAccessException} — Spring's {@code RestTemplate} wrapper for I/O errors.</li>
 * <li>{@link WebClientRequestException} — Spring WebClient's wrapper for connection / DNS / SSL failures (does not
 * include 4xx/5xx responses).</li>
 * <li>{@link HttpServerErrorException} — upstream LLM provider returning 5xx; safe to retry per RFC 9110.</li>
 * </ul>
 *
 * <p>
 * {@link SimpleRetryPolicy} uses most-specific exception matching. Explicitly mapping {@link RuntimeException} to
 * {@code false} ensures programmer-error subtypes ({@link IllegalArgumentException}, {@link NullPointerException},
 * {@link org.springframework.web.client.HttpClientErrorException} for upstream 4xx, etc.) do NOT fall back to the
 * retry-all default list that {@code SimpleRetryPolicy} otherwise applies when no mapping matches.
 *
 * <p>
 * A {@link RetryListener} is wired so each transient failure produces a WARN log + counter increment. Without it,
 * exhausted retries surface as a single line in {@code AiEvalExperimentExecutor.replayItem}'s catch — operators have no
 * way to tell "this failed once" apart from "this was retried 3× with backoff and the upstream is hard-down".
 *
 * @author Ivica Cardic
 * @version ee
 */
@AutoConfiguration
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
public class AiEvalExperimentRetryConfiguration {

    private static final Logger log = LoggerFactory.getLogger(AiEvalExperimentRetryConfiguration.class);

    private static final String RETRY_METRIC = "bytechef_ai_eval_experiment_retry_attempts";

    /**
     * Allowlist of exception class names safe to use as a Micrometer tag without exploding cardinality. Provider SDKs
     * sometimes throw parameterized error classes whose simple name varies per request; tagging by them directly would
     * grow the metric series unboundedly and bring Prometheus / OTLP backends down. Anything not on this list is
     * bucketed as {@code OTHER}.
     *
     * <p>
     * Keep in sync with the {@code SimpleRetryPolicy} map in {@link #chatCompletionRetryTemplate}. Subtypes of
     * retryable parents (e.g. {@code SocketTimeoutException} / {@code SSLHandshakeException} are children of
     * {@code IOException}) need explicit entries — otherwise the policy retries them but the metric collapses them into
     * the {@code OTHER} bucket, which masks DNS / SSL / handshake failures behind a useless catch-all.
     */
    private static final Set<String> EXCEPTION_TAG_ALLOWLIST = Set.of(
        "IOException",
        "SocketTimeoutException",
        "ConnectException",
        "UnknownHostException",
        "NoRouteToHostException",
        "SSLException",
        "SSLHandshakeException",
        "ResourceAccessException",
        "WebClientRequestException",
        "HttpServerErrorException");

    @Bean
    public RetryTemplate chatCompletionRetryTemplate(ObjectProvider<MeterRegistry> meterRegistryProvider) {
        RetryTemplate template = new RetryTemplate();

        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();

        backOffPolicy.setInitialInterval(250L);
        backOffPolicy.setMultiplier(2.0);
        backOffPolicy.setMaxInterval(2_000L);

        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(
            3,
            Map.of(
                IOException.class, true,
                ResourceAccessException.class, true,
                WebClientRequestException.class, true,
                HttpServerErrorException.class, true,
                RuntimeException.class, false));

        template.setBackOffPolicy(backOffPolicy);
        template.setRetryPolicy(retryPolicy);
        template.registerListener(observabilityListener(meterRegistryProvider.getIfAvailable()));

        return template;
    }

    private static RetryListener observabilityListener(MeterRegistry meterRegistry) {
        return new RetryListener() {

            @Override
            public <T, E extends Throwable> void onError(
                RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {

                int attempt = context.getRetryCount();

                log.warn(
                    "chatCompletion retry attempt {} failed: {}",
                    attempt, throwable.getClass()
                        .getSimpleName(),
                    throwable);

                if (meterRegistry != null) {
                    Counter.builder(RETRY_METRIC)
                        .tag("outcome", "failure")
                        .tag("exception", bucketException(throwable))
                        .register(meterRegistry)
                        .increment();
                }
            }

            @Override
            public <T, E extends Throwable> void close(
                RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {

                if (throwable == null) {
                    return;
                }

                int attemptCount = context.getRetryCount();
                String exceptionType = throwable.getClass()
                    .getSimpleName();

                // WARN-level (not ERROR) so the line co-locates with the per-attempt onError WARNs in operator
                // log streams — they are reading the same retry storyline. Without this terminal line, an
                // operator tailing logs sees N identical onError WARNs and no marker that retries were
                // exhausted vs. just transiently slow. The exhaustion counter alone (counter-only) does not
                // give a log-trace that an investigator can correlate to a specific request id; the WARN does.
                log.warn(
                    "chatCompletion retries exhausted after {} attempt(s); final failure: {}",
                    attemptCount, exceptionType, throwable);

                if (meterRegistry != null) {
                    Counter.builder(RETRY_METRIC)
                        .tag("outcome", "exhausted")
                        .tag("exception", bucketException(throwable))
                        .register(meterRegistry)
                        .increment();
                }
            }
        };
    }

    private static String bucketException(Throwable throwable) {
        String simpleName = throwable.getClass()
            .getSimpleName();

        return EXCEPTION_TAG_ALLOWLIST.contains(simpleName) ? simpleName : "OTHER";
    }
}
