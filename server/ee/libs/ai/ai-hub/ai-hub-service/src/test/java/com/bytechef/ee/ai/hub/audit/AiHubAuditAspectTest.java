/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.audit.AuditCaptureFailedException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.lang.reflect.Method;
import java.util.Map;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationContext;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class AiHubAuditAspectTest {

    private final ApplicationContext applicationContext = mock(ApplicationContext.class);
    private final AiHubAuditPublisher publisher = mock(AiHubAuditPublisher.class);
    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

    private AiHubAuditAspect newAspect() {
        @SuppressWarnings("unchecked")
        ObjectProvider<MeterRegistry> provider = mock(ObjectProvider.class);

        when(provider.getIfAvailable()).thenReturn(meterRegistry);

        return new AiHubAuditAspect(applicationContext, publisher, provider);
    }

    @Test
    void testAuditEvaluatesSpelDataAndPublishes() throws NoSuchMethodException {
        AiHubAuditAspect aspect = newAspect();

        AuditAiHub annotation = annotation(
            AiHubAuditEvent.AI_HUB_PERSONAL_AGENT_CREATED, false,
            entry("workspaceId", "#workspaceId"),
            entry("agentName", "#result.toString()"));

        JoinPoint joinPoint = joinPointFor(
            ExampleService.class, "create", new Class[] {
                long.class, String.class
            },
            new Object[] {
                42L, "ignored"
            });

        aspect.audit(joinPoint, annotation, "ResearchBot");

        ArgumentCaptor<Map<String, Object>> dataCaptor = ArgumentCaptor.forClass(Map.class);

        verify(publisher).publish(eq(AiHubAuditEvent.AI_HUB_PERSONAL_AGENT_CREATED), dataCaptor.capture());

        assertThat(dataCaptor.getValue())
            .containsEntry("workspaceId", "42")
            .containsEntry("agentName", "ResearchBot");
    }

    @Test
    void testStrictAuditRethrowsOnEvaluationFailure() throws NoSuchMethodException {
        AiHubAuditAspect aspect = newAspect();

        AuditAiHub annotation = annotation(
            AiHubAuditEvent.AI_HUB_PERSONAL_AGENT_DELETED, false,
            entry("workspaceId", "#nonexistent.boom()"));

        JoinPoint joinPoint = joinPointFor(
            ExampleService.class, "delete", new Class[] {
                long.class
            },
            new Object[] {
                42L
            });

        assertThatThrownBy(() -> aspect.audit(joinPoint, annotation, null))
            .isInstanceOf(AuditCaptureFailedException.class);

        Counter counter = meterRegistry.find("bytechef_ai_hub_audit_failed")
            .counter();

        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    void testNonStrictAuditAbsorbsEvaluationFailure() throws NoSuchMethodException {
        AiHubAuditAspect aspect = newAspect();

        AuditAiHub annotation = annotation(
            AiHubAuditEvent.AI_HUB_PERSONAL_AGENT_UPDATED, false,
            entry("workspaceId", "#nonexistent.boom()"));

        JoinPoint joinPoint = joinPointFor(
            ExampleService.class, "update", new Class[] {
                long.class
            },
            new Object[] {
                42L
            });

        aspect.audit(joinPoint, annotation, null);

        verify(publisher, org.mockito.Mockito.never()).publish(any(), any());

        Counter counter = meterRegistry.find("bytechef_ai_hub_audit_failed")
            .counter();

        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    private JoinPoint joinPointFor(Class<?> type, String name, Class<?>[] paramTypes, Object[] args)
        throws NoSuchMethodException {

        Method method = type.getDeclaredMethod(name, paramTypes);
        MethodSignature methodSignature = mock(MethodSignature.class);

        when(methodSignature.getMethod()).thenReturn(method);
        when(methodSignature.toShortString()).thenReturn(type.getSimpleName() + "." + name);

        JoinPoint joinPoint = mock(JoinPoint.class);

        when(joinPoint.getSignature()).thenReturn((Signature) methodSignature);
        when(joinPoint.getArgs()).thenReturn(args);

        return joinPoint;
    }

    private AuditAiHub annotation(
        AiHubAuditEvent event, boolean establishCorrelation, AuditAiHub.AuditData... data) {

        AuditAiHub annotation = mock(AuditAiHub.class);

        when(annotation.event()).thenReturn(event);
        when(annotation.establishCorrelation()).thenReturn(establishCorrelation);
        when(annotation.data()).thenReturn(data);

        return annotation;
    }

    private AuditAiHub.AuditData entry(String key, String expression) {
        AuditAiHub.AuditData entry = mock(AuditAiHub.AuditData.class);

        when(entry.key()).thenReturn(key);
        when(entry.value()).thenReturn(expression);

        return entry;
    }

    @SuppressWarnings("unused")
    private static final class ExampleService {

        String create(long workspaceId, String name) {
            return name;
        }

        void delete(long agentId) {
        }

        void update(long agentId) {
        }
    }
}
