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
 * @author Ivica Cardic
 * @version ee
 */
@AutoConfiguration
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
public class AiEvalExperimentRetryConfiguration {

    private static final Logger log = LoggerFactory.getLogger(AiEvalExperimentRetryConfiguration.class);

    private static final String RETRY_METRIC = "bytechef_ai_eval_experiment_retry_attempts";

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
