/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.eval.experiment.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.io.IOException;
import java.net.SocketTimeoutException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.retry.support.RetryTemplate;

/**
 * @author Ivica Cardic
 * @version ee
 */
class AiEvalExperimentRetryConfigurationTest {

    private static final String RETRY_METRIC = "bytechef_ai_eval_experiment_retry_attempts";

    @Test
    void testRetryListenerIncrementsFailureCounterPerAttemptAndExhaustedCounterOnGiveUp() {
        MeterRegistry registry = new SimpleMeterRegistry();
        AiEvalExperimentRetryConfiguration configuration = new AiEvalExperimentRetryConfiguration();

        RetryTemplate template = configuration.chatCompletionRetryTemplate(staticProvider(registry));

        assertThatThrownBy(() -> template.execute(context -> {
            throw new IOException("simulated transient I/O");
        })).isInstanceOf(IOException.class);

        assertThat(registry.counter(RETRY_METRIC, "outcome", "failure", "exception", "IOException")
            .count())
                .as("one outcome=failure increment per attempt that threw a retryable exception")
                .isEqualTo(3.0);

        assertThat(registry.counter(RETRY_METRIC, "outcome", "exhausted", "exception", "IOException")
            .count())
                .as("one outcome=exhausted increment when the retry budget is used up")
                .isEqualTo(1.0);
    }

    @Test
    void testRetryListenerDoesNotIncrementExhaustedWhenCallSucceedsOnFirstAttempt() {
        MeterRegistry registry = new SimpleMeterRegistry();
        AiEvalExperimentRetryConfiguration configuration = new AiEvalExperimentRetryConfiguration();

        RetryTemplate template = configuration.chatCompletionRetryTemplate(staticProvider(registry));

        String result = template.execute(context -> "ok");

        assertThat(result).isEqualTo("ok");
        assertThat(registry.counter(RETRY_METRIC, "outcome", "failure", "exception", "IOException")
            .count()).isZero();
        assertThat(registry.counter(RETRY_METRIC, "outcome", "exhausted", "exception", "IOException")
            .count())
                .as("a single-attempt success must NOT look like an exhausted retry — that's the bug this fix prevents")
                .isZero();
    }

    @Test
    void testProgrammerErrorsAreNotRetried() {
        MeterRegistry registry = new SimpleMeterRegistry();
        AiEvalExperimentRetryConfiguration configuration = new AiEvalExperimentRetryConfiguration();

        RetryTemplate template = configuration.chatCompletionRetryTemplate(staticProvider(registry));

        assertThatThrownBy(() -> template.execute(context -> {
            throw new IllegalArgumentException("not retryable");
        })).isInstanceOf(IllegalArgumentException.class);

        assertThat(registry.counter(RETRY_METRIC, "outcome", "failure", "exception", "OTHER")
            .count())
                .as("programmer errors must fail fast — exactly one failure increment, no retries attempted")
                .isEqualTo(1.0);
    }

    @Test
    void testAllowlistedExceptionsBucketIntoTheirOwnTagNotOther() {
        MeterRegistry registry = new SimpleMeterRegistry();
        AiEvalExperimentRetryConfiguration configuration = new AiEvalExperimentRetryConfiguration();

        RetryTemplate template = configuration.chatCompletionRetryTemplate(staticProvider(registry));

        assertThatThrownBy(() -> template.execute(context -> {
            throw new SocketTimeoutException("simulated timeout");
        })).isInstanceOf(SocketTimeoutException.class);

        assertThat(registry.counter(RETRY_METRIC, "outcome", "failure", "exception", "SocketTimeoutException")
            .count())
                .as("allowlisted exception must surface under its typed tag")
                .isEqualTo(3.0);
        assertThat(registry.counter(RETRY_METRIC, "outcome", "failure", "exception", "OTHER")
            .count())
                .as("allowlisted exception must NOT also leak into the OTHER bucket")
                .isZero();
    }

    private static ObjectProvider<MeterRegistry> staticProvider(MeterRegistry registry) {
        return new ObjectProvider<>() {

            @Override
            public MeterRegistry getObject() {
                return registry;
            }

            @Override
            public MeterRegistry getObject(Object... args) {
                return registry;
            }

            @Override
            public MeterRegistry getIfAvailable() {
                return registry;
            }

            @Override
            public MeterRegistry getIfUnique() {
                return registry;
            }
        };
    }
}
