/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.reliability;

import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayModelDeployment;
import com.bytechef.ee.platform.ai.gateway.util.AiGatewayThrowables;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

/**
 * @author Ivica Cardic
 * @version ee
 */
@Component
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
public class AiGatewayRetryHandlerImpl implements AiGatewayRetryHandler {

    private static final long BASE_BACKOFF_MS = 500;
    private static final int DEFAULT_MAX_RETRIES = 2;
    private static final Logger log = LoggerFactory.getLogger(AiGatewayRetryHandlerImpl.class);
    private static final long MAX_BACKOFF_MS = 2000;

    private final AiGatewayCooldownTracker cooldownTracker;
    private final RetryTemplate retryTemplate;

    public AiGatewayRetryHandlerImpl(AiGatewayCooldownTracker cooldownTracker) {
        this.cooldownTracker = cooldownTracker;
        this.retryTemplate = buildRetryTemplate();
    }

    @Override
    public <T> T executeWithRetry(
        List<AiGatewayModelDeployment> deployments,
        Function<AiGatewayModelDeployment, T> action) {

        List<AiGatewayModelDeployment> availableDeployments = getAvailableDeployments(deployments);

        // Accumulate every deployment's terminal exception so the IllegalStateException at the bottom carries
        // the full multi-deployment failure picture via addSuppressed. Without this, only the LAST deployment's
        // exception survives — the prior N-1 are gone except for their warn log lines, which scroll out of
        // structured-log retention quickly.
        List<Exception> deploymentFailures = new ArrayList<>();

        for (AiGatewayModelDeployment deployment : availableDeployments) {
            try {
                return retryTemplate.execute(context -> {
                    try {
                        T result = action.apply(deployment);

                        cooldownTracker.recordSuccess(deployment.getId());

                        return result;
                    } catch (RuntimeException exception) {
                        if (isNonRetryable(exception)) {
                            throw exception;
                        }

                        cooldownTracker.recordFailure(deployment.getId());

                        // Pass exception as the trailing arg so the stack trace is preserved. Without it, a
                        // failure that recovers on a different deployment would lose the original cause forever
                        // (lastException only surfaces if every deployment fails) — exactly the scenario where
                        // operators most need the trace.
                        log.warn("Deployment {} attempt {} failed",
                            deployment.getId(), context.getRetryCount() + 1, exception);

                        throw exception;
                    }
                });
            } catch (Exception exception) {
                if (exception instanceof RuntimeException runtimeException && isNonRetryable(runtimeException)) {
                    throw runtimeException;
                }

                deploymentFailures.add(exception);

                log.warn(
                    "Deployment {} exhausted retries, trying next deployment", deployment.getId(), exception);
            }
        }

        Exception lastException = deploymentFailures.isEmpty()
            ? null
            : deploymentFailures.get(deploymentFailures.size() - 1);

        IllegalStateException terminal = new IllegalStateException(
            "All deployments failed after retries", lastException);

        for (int failureIndex = 0; failureIndex < deploymentFailures.size() - 1; failureIndex++) {
            terminal.addSuppressed(deploymentFailures.get(failureIndex));
        }

        throw terminal;
    }

    @Override
    public <T> Flux<T> executeStreamWithRetry(
        List<AiGatewayModelDeployment> deployments,
        Function<AiGatewayModelDeployment, Flux<T>> action) {

        List<AiGatewayModelDeployment> availableDeployments = getAvailableDeployments(deployments);

        return tryDeploymentStream(availableDeployments, 0, action, new ArrayList<>());
    }

    private RetryTemplate buildRetryTemplate() {
        RetryTemplate template = new RetryTemplate();

        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(
            DEFAULT_MAX_RETRIES + 1,
            Map.of(RuntimeException.class, true),
            true,
            false) {

            @Override
            public boolean canRetry(org.springframework.retry.RetryContext context) {
                Throwable lastThrowable = context.getLastThrowable();

                if (lastThrowable instanceof RuntimeException runtimeException &&
                    isNonRetryable(runtimeException)) {

                    return false;
                }

                return super.canRetry(context);
            }
        };

        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();

        backOffPolicy.setInitialInterval(BASE_BACKOFF_MS);
        backOffPolicy.setMaxInterval(MAX_BACKOFF_MS);
        backOffPolicy.setMultiplier(2.0);

        template.setRetryPolicy(retryPolicy);
        template.setBackOffPolicy(backOffPolicy);

        return template;
    }

    private List<AiGatewayModelDeployment> getAvailableDeployments(
        List<AiGatewayModelDeployment> deployments) {

        List<AiGatewayModelDeployment> availableDeployments = deployments.stream()
            .filter(deployment -> !cooldownTracker.isCooledDown(deployment.getId()))
            .toList();

        if (availableDeployments.isEmpty()) {
            throw new IllegalStateException("All deployments are cooled down");
        }

        return availableDeployments;
    }

    private boolean isNonRetryable(RuntimeException exception) {
        if (exception instanceof HttpClientErrorException httpClientErrorException) {
            int statusCode = httpClientErrorException.getStatusCode()
                .value();

            return !AiGatewayRetryableStatuses.TRANSIENT_RETRYABLE_4XX.contains(statusCode);
        }

        return exception instanceof IllegalArgumentException ||
            exception instanceof NullPointerException ||
            exception instanceof SecurityException ||
            exception instanceof ClassCastException ||
            exception instanceof UnsupportedOperationException ||
            exception instanceof IndexOutOfBoundsException ||
            exception instanceof NonTransientAiException;
    }

    private <T> Flux<T> tryDeploymentStream(
        List<AiGatewayModelDeployment> deployments,
        int deploymentIndex,
        Function<AiGatewayModelDeployment, Flux<T>> action,
        List<Throwable> deploymentFailures) {

        if (deploymentIndex >= deployments.size()) {
            // Mirror the synchronous path: surface the LAST deployment's exception as the cause and chain the rest
            // via addSuppressed. Without this, every prior deployment's exception is dropped on the floor and the
            // terminal IllegalStateException carries no diagnostic content — operators triaging a streaming-chat
            // outage would see only the last deployment's stack with no clue about the earlier deployments.
            Throwable lastFailure = deploymentFailures.isEmpty()
                ? null
                : deploymentFailures.get(deploymentFailures.size() - 1);

            IllegalStateException terminal = new IllegalStateException(
                "All deployments failed after retries", lastFailure);

            for (int failureIndex = 0; failureIndex < deploymentFailures.size() - 1; failureIndex++) {
                terminal.addSuppressed(deploymentFailures.get(failureIndex));
            }

            return Flux.error(terminal);
        }

        AiGatewayModelDeployment deployment = deployments.get(deploymentIndex);

        // Streaming failover is PRE-FIRST-TOKEN ONLY. Once an element has been emitted, the bytes have been flushed to
        // the SSE client and cannot be retracted, so neither same-deployment retry nor cross-deployment failover may
        // re-subscribe (that would replay already-sent tokens). This flag gates both.
        AtomicBoolean emitted = new AtomicBoolean(false);

        return Mono.defer(() -> Mono.just(deployment))
            .flatMapMany(action::apply)
            .doOnNext(element -> emitted.set(true))
            .retryWhen(
                Retry.backoff(DEFAULT_MAX_RETRIES, java.time.Duration.ofMillis(BASE_BACKOFF_MS))
                    .maxBackoff(java.time.Duration.ofMillis(MAX_BACKOFF_MS))
                    // Pure predicate: reactor is free to call a filter multiple times, so side effects like
                    // cooldownTracker.recordFailure MUST live in doBeforeRetry below — where Reactor guarantees
                    // exactly one invocation per retry. Do not retry once streaming has begun.
                    .filter(throwable -> !emitted.get() && !(throwable instanceof RuntimeException runtimeException
                        && isNonRetryable(runtimeException)))
                    .doBeforeRetry(retrySignal -> {
                        cooldownTracker.recordFailure(deployment.getId());

                        Throwable failure = retrySignal.failure();

                        log.warn("Streaming deployment {} retry due to: {}",
                            deployment.getId(), AiGatewayThrowables.summarize(failure), failure);
                    })
                    .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                        cooldownTracker.recordFailure(deployment.getId());

                        return retrySignal.failure();
                    }))
            .doOnComplete(() -> cooldownTracker.recordSuccess(deployment.getId()))
            .onErrorResume(exception -> {
                // Once streaming has begun, or the error is non-retryable, propagate terminally instead of failing
                // over to another deployment.
                if (emitted.get() ||
                    (exception instanceof RuntimeException runtimeException && isNonRetryable(runtimeException))) {

                    return Flux.error(exception);
                }

                deploymentFailures.add(exception);

                log.warn(
                    "Deployment {} exhausted retries, trying next deployment", deployment.getId(), exception);

                return tryDeploymentStream(deployments, deploymentIndex + 1, action, deploymentFailures);
            });
    }
}
