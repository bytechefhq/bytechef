/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.reliability;

import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayModelDeployment;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import java.util.List;
import java.util.Map;
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
 * @version ee
 */
@Component
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
public class AiGatewayRetryHandler {

    private static final long BASE_BACKOFF_MS = 500;
    private static final int DEFAULT_MAX_RETRIES = 2;
    private static final Logger logger = LoggerFactory.getLogger(AiGatewayRetryHandler.class);
    private static final long MAX_BACKOFF_MS = 2000;

    private final AiGatewayCooldownTracker cooldownTracker;
    private final RetryTemplate retryTemplate;

    public AiGatewayRetryHandler(AiGatewayCooldownTracker cooldownTracker) {
        this.cooldownTracker = cooldownTracker;
        this.retryTemplate = buildRetryTemplate();
    }

    public <T> T executeWithRetry(
        List<AiGatewayModelDeployment> deployments,
        Function<AiGatewayModelDeployment, T> action) {

        List<AiGatewayModelDeployment> availableDeployments = getAvailableDeployments(deployments);

        Exception lastException = null;

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

                        logger.warn("Deployment {} attempt {} failed: {}",
                            deployment.getId(), context.getRetryCount() + 1, exception.getMessage());

                        throw exception;
                    }
                });
            } catch (Exception exception) {
                if (exception instanceof RuntimeException runtimeException && isNonRetryable(runtimeException)) {
                    throw runtimeException;
                }

                lastException = exception;

                logger.warn("Deployment {} exhausted retries, trying next deployment", deployment.getId());
            }
        }

        throw new IllegalStateException("All deployments failed after retries", lastException);
    }

    public <T> Flux<T> executeStreamWithRetry(
        List<AiGatewayModelDeployment> deployments,
        Function<AiGatewayModelDeployment, Flux<T>> action) {

        List<AiGatewayModelDeployment> availableDeployments = getAvailableDeployments(deployments);

        return tryDeploymentStream(availableDeployments, 0, action);
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
        Function<AiGatewayModelDeployment, Flux<T>> action) {

        if (deploymentIndex >= deployments.size()) {
            return Flux.error(new IllegalStateException("All deployments failed after retries"));
        }

        AiGatewayModelDeployment deployment = deployments.get(deploymentIndex);

        return Mono.defer(() -> Mono.just(deployment))
            .flatMapMany(action::apply)
            .retryWhen(
                Retry.backoff(DEFAULT_MAX_RETRIES, java.time.Duration.ofMillis(BASE_BACKOFF_MS))
                    .maxBackoff(java.time.Duration.ofMillis(MAX_BACKOFF_MS))
                    // Pure predicate: reactor is free to call a filter multiple times, so side effects like
                    // cooldownTracker.recordFailure MUST live in doBeforeRetry below — where Reactor guarantees
                    // exactly one invocation per retry.
                    .filter(throwable -> !(throwable instanceof RuntimeException runtimeException
                        && isNonRetryable(runtimeException)))
                    .doBeforeRetry(retrySignal -> {
                        cooldownTracker.recordFailure(deployment.getId());

                        logger.warn("Streaming deployment {} retry due to: {}",
                            deployment.getId(), retrySignal.failure()
                                .getMessage());
                    })
                    .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                        // Record the terminal failure so the cooldown tracker stays aligned with the non-stream
                        // path's retryTemplate.execute, which records the last failure inside its own catch.
                        cooldownTracker.recordFailure(deployment.getId());

                        return retrySignal.failure();
                    }))
            .doOnComplete(() -> cooldownTracker.recordSuccess(deployment.getId()))
            .onErrorResume(exception -> {
                if (exception instanceof RuntimeException runtimeException && isNonRetryable(runtimeException)) {
                    return Flux.error(exception);
                }

                logger.warn("Deployment {} exhausted retries, trying next deployment", deployment.getId());

                return tryDeploymentStream(deployments, deploymentIndex + 1, action);
            });
    }
}
