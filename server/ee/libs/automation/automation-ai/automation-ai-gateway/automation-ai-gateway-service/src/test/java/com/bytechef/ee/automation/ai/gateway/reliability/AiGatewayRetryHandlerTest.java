/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.reliability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayModelDeployment;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @version ee
 */
@ExtendWith(MockitoExtension.class)
class AiGatewayRetryHandlerTest {

    @Mock
    private AiGatewayCooldownTracker cooldownTracker;

    private AiGatewayRetryHandler retryHandler;

    @BeforeEach
    void setUp() {
        retryHandler = new AiGatewayRetryHandler(cooldownTracker);
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

    private AiGatewayModelDeployment createDeployment(Long id) {
        AiGatewayModelDeployment deployment = new AiGatewayModelDeployment(1L, 100L);

        ReflectionTestUtils.setField(deployment, "id", id);

        return deployment;
    }
}
