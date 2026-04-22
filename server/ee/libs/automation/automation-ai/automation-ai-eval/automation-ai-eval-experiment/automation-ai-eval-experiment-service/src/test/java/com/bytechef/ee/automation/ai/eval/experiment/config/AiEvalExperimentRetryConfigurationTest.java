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
 * Pins the retry observability contract: every transient failure must increment the
 * {@code bytechef_ai_eval_experiment_retry_attempts{outcome=failure}} counter, and an exhausted retry budget must add a
 * separate {@code outcome=exhausted} increment. Without these signals, an operator cannot tell "single-attempt failure"
 * apart from "retried 3× and the upstream is hard-down" — both look identical in
 * {@code AiEvalExperimentExecutor.replayItem}'s catch block.
 *
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

        // SimpleRetryPolicy(maxAttempts=3) gives 3 total attempts. Each attempt that throws fires onError;
        // the listener increments outcome=failure per attempt. When the last attempt also throws, the
        // template's close() fires with the throwable — listener also increments outcome=exhausted.
        assertThatThrownBy(() -> template.execute(context -> {
            throw new IOException("simulated transient I/O");
        })).isInstanceOf(IOException.class);

        assertThat(registry.counter(RETRY_METRIC, "outcome", "failure", "exception", "IOException")
            .count())
                .as("one outcome=failure increment per attempt that threw a retryable exception")
                .isEqualTo(3.0);

        // The exhausted counter is now tagged with the boundary exception type for symmetry with the
        // per-attempt failure counter — a "1xx exhausted Foo, 2xx exhausted Bar" dashboard line is
        // operationally far more useful than an untagged total. Updated to query with the matching tag.
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

        // IllegalArgumentException is a RuntimeException — the SimpleRetryPolicy mapping pins
        // RuntimeException -> false explicitly, so it must NOT be retried.
        assertThatThrownBy(() -> template.execute(context -> {
            throw new IllegalArgumentException("not retryable");
        })).isInstanceOf(IllegalArgumentException.class);

        // Fail-fast contract: the failure counter must increment exactly ONCE, not 3×, because the
        // SimpleRetryPolicy returns false for RuntimeException → no retries are scheduled. A regression
        // that re-introduces the retry-all default would push this to 3.0 and the test fails.
        // The exception tag is bucketed: IllegalArgumentException is not a retryable upstream-network class so
        // it falls into the "OTHER" cardinality-safe bucket rather than appearing as its own series.
        assertThat(registry.counter(RETRY_METRIC, "outcome", "failure", "exception", "OTHER")
            .count())
                .as("programmer errors must fail fast — exactly one failure increment, no retries attempted")
                .isEqualTo(1.0);
    }

    @Test
    void testAllowlistedExceptionsBucketIntoTheirOwnTagNotOther() {
        // Positive companion to testProgrammerErrorsAreNotRetried: an allowlisted upstream-network class
        // (SocketTimeoutException is in EXCEPTION_TAG_ALLOWLIST) must surface under its own tag, NOT the
        // catch-all OTHER bucket. A regression that drops the allowlist or otherwise collapses everything
        // into OTHER would silently kill the dashboard breakdown — the OTHER-only assertion above would
        // still pass, but every retryable network failure would be lumped together with programmer errors.
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
