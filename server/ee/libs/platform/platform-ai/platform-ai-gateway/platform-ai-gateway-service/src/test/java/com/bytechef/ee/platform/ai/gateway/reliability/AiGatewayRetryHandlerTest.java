/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.reliability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayModelDeployment;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Flux;

/**
 * @author Ivica Cardic
 * @version ee
 */
@ExtendWith(MockitoExtension.class)
class AiGatewayRetryHandlerTest {

    @Mock
    private AiGatewayCooldownTracker cooldownTracker;

    private AiGatewayRetryHandler retryHandler;

    @BeforeEach
    void setUp() {
        retryHandler = new AiGatewayRetryHandlerImpl(cooldownTracker);
    }

    @Test
    void testExecuteWithRetrySucceedsOnFirstAttempt() {
        AiGatewayModelDeployment deployment = createDeployment(1L);

        when(cooldownTracker.isCooledDown(1L)).thenReturn(false);

        String result = retryHandler.executeWithRetry(
            List.of(deployment), deploymentArg -> "success");

        assertEquals("success", result);

        verify(cooldownTracker).recordSuccess(1L);
    }

    @Test
    void testExecuteWithRetryFallsToNextDeployment() {
        AiGatewayModelDeployment firstDeployment = createDeployment(1L);
        AiGatewayModelDeployment secondDeployment = createDeployment(2L);

        when(cooldownTracker.isCooledDown(1L)).thenReturn(false);
        when(cooldownTracker.isCooledDown(2L)).thenReturn(false);

        String result = retryHandler.executeWithRetry(
            List.of(firstDeployment, secondDeployment), deploymentArg -> {
                if (deploymentArg.getId()
                    .equals(1L)) {
                    throw new RuntimeException("first deployment failed");
                }

                return "success from second";
            });

        assertEquals("success from second", result);

        verify(cooldownTracker).recordSuccess(2L);
    }

    @Test
    void testExecuteWithRetryThrowsWhenAllDeploymentsFail() {
        AiGatewayModelDeployment firstDeployment = createDeployment(1L);
        AiGatewayModelDeployment secondDeployment = createDeployment(2L);

        when(cooldownTracker.isCooledDown(1L)).thenReturn(false);
        when(cooldownTracker.isCooledDown(2L)).thenReturn(false);

        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> retryHandler.executeWithRetry(
                List.of(firstDeployment, secondDeployment), deploymentArg -> {
                    throw new RuntimeException("deployment failed");
                }));

        assertEquals("All deployments failed after retries", exception.getMessage());
    }

    @Test
    void testExecuteWithRetryPreservesAllDeploymentFailuresAsSuppressed() {
        // Without addSuppressed, only the LAST deployment's exception is retained on the terminal throw; the
        // prior N-1 are gone except for log lines, which scroll out of structured-log retention quickly.
        // addSuppressed keeps the full picture attached to the terminal exception so an operator looking at
        // the failure can see every deployment that was tried.
        AiGatewayModelDeployment firstDeployment = createDeployment(1L);
        AiGatewayModelDeployment secondDeployment = createDeployment(2L);
        AiGatewayModelDeployment thirdDeployment = createDeployment(3L);

        when(cooldownTracker.isCooledDown(1L)).thenReturn(false);
        when(cooldownTracker.isCooledDown(2L)).thenReturn(false);
        when(cooldownTracker.isCooledDown(3L)).thenReturn(false);

        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> retryHandler.executeWithRetry(
                List.of(firstDeployment, secondDeployment, thirdDeployment), deploymentArg -> {
                    throw new RuntimeException("deployment " + deploymentArg.getId() + " failed");
                }));

        // Cause is the LAST deployment's terminal exception (preserves original wrapping behaviour).
        org.assertj.core.api.Assertions.assertThat(exception.getCause())
            .hasMessageContaining("deployment 3 failed");

        // Suppressed list carries the FIRST + SECOND deployments' terminal exceptions in order, so an
        // operator iterating exception.getSuppressed() sees all three deployments without grep'ing logs.
        Throwable[] suppressed = exception.getSuppressed();

        org.assertj.core.api.Assertions.assertThat(suppressed)
            .hasSize(2);
        org.assertj.core.api.Assertions.assertThat(suppressed[0])
            .hasMessageContaining("deployment 1 failed");
        org.assertj.core.api.Assertions.assertThat(suppressed[1])
            .hasMessageContaining("deployment 2 failed");
    }

    @Test
    void testExecuteWithRetryThrowsImmediatelyOnNonRetryableException() {
        AiGatewayModelDeployment deployment = createDeployment(1L);

        when(cooldownTracker.isCooledDown(1L)).thenReturn(false);

        assertThrows(
            IllegalArgumentException.class,
            () -> retryHandler.executeWithRetry(
                List.of(deployment), deploymentArg -> {
                    throw new IllegalArgumentException("bad argument");
                }));

        verify(cooldownTracker, never()).recordFailure(1L);
        verify(cooldownTracker, never()).recordSuccess(1L);
    }

    @Test
    void testExecuteWithRetryRetriesOnIllegalStateException() {
        AiGatewayModelDeployment deployment = createDeployment(1L);

        when(cooldownTracker.isCooledDown(1L)).thenReturn(false);

        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> retryHandler.executeWithRetry(
                List.of(deployment), deploymentArg -> {
                    throw new IllegalStateException("illegal state");
                }));

        assertEquals("All deployments failed after retries", exception.getMessage());
    }

    @Test
    void testExecuteWithRetryThrowsImmediatelyOnNullPointerException() {
        AiGatewayModelDeployment deployment = createDeployment(1L);

        when(cooldownTracker.isCooledDown(1L)).thenReturn(false);

        assertThrows(
            NullPointerException.class,
            () -> retryHandler.executeWithRetry(
                List.of(deployment), deploymentArg -> {
                    throw new NullPointerException("null value");
                }));

        verify(cooldownTracker, never()).recordFailure(1L);
        verify(cooldownTracker, never()).recordSuccess(1L);
    }

    @Test
    void testExecuteWithRetryThrowsWhenAllDeploymentsAreCooledDown() {
        AiGatewayModelDeployment firstDeployment = createDeployment(1L);
        AiGatewayModelDeployment secondDeployment = createDeployment(2L);

        when(cooldownTracker.isCooledDown(1L)).thenReturn(true);
        when(cooldownTracker.isCooledDown(2L)).thenReturn(true);

        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> retryHandler.executeWithRetry(
                List.of(firstDeployment, secondDeployment), deploymentArg -> "should not reach"));

        assertEquals("All deployments are cooled down", exception.getMessage());
    }

    @Test
    void testExecuteStreamWithRetryFailsOverBeforeFirstToken() {
        AiGatewayModelDeployment firstDeployment = createDeployment(1L);
        AiGatewayModelDeployment secondDeployment = createDeployment(2L);

        List<String> emitted = retryHandler.<String>executeStreamWithRetry(
            List.of(firstDeployment, secondDeployment),
            deployment -> deployment.getId()
                .equals(1L)
                    ? Flux.error(new RuntimeException("cold start"))
                    : Flux.just("a", "b"))
            .collectList()
            .block();

        assertEquals(List.of("a", "b"), emitted);
    }

    @Test
    void testExecuteStreamWithRetryDoesNotFailOverAfterFirstToken() {
        AiGatewayModelDeployment firstDeployment = createDeployment(1L);
        AiGatewayModelDeployment secondDeployment = createDeployment(2L);

        List<String> emitted = new ArrayList<>();

        assertThrows(
            RuntimeException.class,
            () -> retryHandler.<String>executeStreamWithRetry(
                List.of(firstDeployment, secondDeployment),
                deployment -> deployment.getId()
                    .equals(1L)
                        ? Flux.just("a")
                            .concatWith(Flux.error(new RuntimeException("mid-stream failure")))
                        : Flux.just("b"))
                .doOnNext(emitted::add)
                .blockLast());

        // The first token streamed, so the mid-stream error must propagate: no retry, no failover to "b", and no
        // replay of "a".
        assertEquals(List.of("a"), emitted);
    }

    @Test
    void testExecuteStreamWithRetryPropagatesWhenAllDeploymentsFailBeforeToken() {
        AiGatewayModelDeployment firstDeployment = createDeployment(1L);
        AiGatewayModelDeployment secondDeployment = createDeployment(2L);

        assertThrows(
            IllegalStateException.class,
            () -> retryHandler.<String>executeStreamWithRetry(
                List.of(firstDeployment, secondDeployment),
                deployment -> Flux.error(new RuntimeException("down")))
                .collectList()
                .block());
    }

    private AiGatewayModelDeployment createDeployment(Long id) {
        AiGatewayModelDeployment deployment = new AiGatewayModelDeployment(1L, 100L);

        ReflectionTestUtils.setField(deployment, "id", id);

        return deployment;
    }
}
