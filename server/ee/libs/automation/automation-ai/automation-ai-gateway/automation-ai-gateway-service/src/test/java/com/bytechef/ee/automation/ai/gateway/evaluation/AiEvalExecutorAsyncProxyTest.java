/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.evaluation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ee.automation.ai.eval.service.WorkspaceAiEvalRuleService;
import com.bytechef.ee.automation.ai.eval.service.WorkspaceAiEvalScoreService;
import com.bytechef.ee.automation.ai.observability.service.WorkspaceAiObservabilityTraceService;
import com.bytechef.ee.platform.ai.eval.service.AiEvalExecutionService;
import com.bytechef.ee.platform.ai.eval.service.AiEvalRuleService;
import com.bytechef.ee.platform.ai.eval.service.AiEvalScoreConfigService;
import com.bytechef.ee.platform.ai.gateway.provider.AiGatewayChatModelFactory;
import com.bytechef.ee.platform.ai.gateway.service.AiGatewayProviderService;
import com.bytechef.ee.platform.ai.observability.service.AiObservabilitySpanService;
import com.bytechef.ee.platform.ai.observability.service.AiObservabilityTraceService;
import com.bytechef.platform.ai.guardrails.AiGuardrailsAdvisorProvider;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.concurrent.TenantTaskDecorator;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.scheduling.annotation.AsyncAnnotationBeanPostProcessor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @author Ivica Cardic
 * @version ee
 */
class AiEvalExecutorAsyncProxyTest {

    private static final long TRACE_ID = 42L;

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
        TenantContext.resetCurrentTenantId();
    }

    @Test
    void testAiEvalExecutorIsAopProxyWhenEnableAsyncIsActive() {
        try (AnnotationConfigApplicationContext context = createContext(false)) {
            AiEvalExecutor aiEvalExecutor = context.getBean(AiEvalExecutor.class);

            assertThat(AopUtils.isAopProxy(aiEvalExecutor)).isTrue();
        }
    }

    @Test
    void testEvaluateTraceRunsOnTheWorkerExecutorWithTheCallerSecurityContext() throws InterruptedException {
        try (AnnotationConfigApplicationContext context = createContext(true)) {
            assertCallerContextPropagated(
                context, aiEvalExecutor -> aiEvalExecutor.evaluateTrace(TRACE_ID, 7L), "eval-worker-");
        }
    }

    @Test
    void testEvaluateTraceForRuleRunsOnTheWorkerExecutorWithTheCallerSecurityContext() throws InterruptedException {
        try (AnnotationConfigApplicationContext context = createContext(true)) {
            assertCallerContextPropagated(
                context, aiEvalExecutor -> aiEvalExecutor.evaluateTraceForRule(TRACE_ID, 3L), "eval-worker-");
        }
    }

    @Test
    void testFallbackExecutorPropagatesSecurityAndTenantContextWithoutWorkerExecutor()
        throws InterruptedException {

        try (AnnotationConfigApplicationContext context = createContext(false)) {
            assertCallerContextPropagated(
                context, aiEvalExecutor -> aiEvalExecutor.evaluateTrace(TRACE_ID, 7L), "ai-eval-");
        }
    }

    private void assertCallerContextPropagated(
        AnnotationConfigApplicationContext context, Consumer<AiEvalExecutor> invocation,
        String expectedThreadPrefix) throws InterruptedException {

        AiObservabilityTraceService aiObservabilityTraceService = context.getBean(AiObservabilityTraceService.class);

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Authentication> observedAuthentication = new AtomicReference<>();
        AtomicReference<String> observedThreadName = new AtomicReference<>();
        AtomicReference<String> observedTenantId = new AtomicReference<>();

        when(aiObservabilityTraceService.getTrace(anyLong())).thenAnswer(invocationOnMock -> {
            SecurityContext securityContext = SecurityContextHolder.getContext();

            observedAuthentication.set(securityContext.getAuthentication());

            Thread currentThread = Thread.currentThread();

            observedThreadName.set(currentThread.getName());
            observedTenantId.set(TenantContext.getCurrentTenantId());

            latch.countDown();

            throw new IllegalStateException("stop after capture");
        });

        Authentication callerAuthentication = UsernamePasswordAuthenticationToken.authenticated(
            "admin@localhost.com", null, List.of());

        SecurityContext callerContext = SecurityContextHolder.createEmptyContext();

        callerContext.setAuthentication(callerAuthentication);

        SecurityContextHolder.setContext(callerContext);

        TenantContext.setCurrentTenantId("tenant_a");

        invocation.accept(context.getBean(AiEvalExecutor.class));

        assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();

        Thread callerThread = Thread.currentThread();

        assertThat(observedThreadName.get()).isNotEqualTo(callerThread.getName())
            .startsWith(expectedThreadPrefix);
        assertThat(observedAuthentication.get()).isSameAs(callerAuthentication);
        assertThat(observedTenantId.get()).isEqualTo("tenant_a");
    }

    private static AnnotationConfigApplicationContext createContext(boolean withWorkerExecutor) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

        context.getEnvironment()
            .getPropertySources()
            .addFirst(new MapPropertySource(
                "test", Map.of("bytechef.edition", "ee", "bytechef.ai.gateway.enabled", "true")));

        context.register(AsyncProxyTestConfiguration.class, AiEvalExecutorAsyncConfiguration.class);

        if (withWorkerExecutor) {
            context.registerBean(
                AsyncAnnotationBeanPostProcessor.DEFAULT_TASK_EXECUTOR_BEAN_NAME, AsyncTaskExecutor.class,
                () -> createExecutor("eval-default-"));
            context.registerBean(
                "workerExecutor", AsyncTaskExecutor.class, () -> createExecutor("eval-worker-"));
        }

        context.refresh();

        return context;
    }

    private static AsyncTaskExecutor createExecutor(String threadNamePrefix) {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor(threadNamePrefix);

        executor.setTaskDecorator(new TenantTaskDecorator());

        return executor;
    }

    @EnableAsync
    static class AsyncProxyTestConfiguration {

        @Bean
        AiObservabilityTraceService aiObservabilityTraceService() {
            return mock(AiObservabilityTraceService.class);
        }

        @Bean
        AiEvalExecutor aiEvalExecutor(AiObservabilityTraceService aiObservabilityTraceService) {
            return new AiEvalExecutor(
                staticGuardrailsAdvisorProvider(),
                mock(AiEvalExecutionService.class),
                mock(AiEvalRuleService.class),
                mock(AiEvalScoreConfigService.class),
                mock(WorkspaceAiEvalScoreService.class),
                mock(WorkspaceAiEvalRuleService.class),
                mock(AiGatewayChatModelFactory.class),
                mock(AiGatewayProviderService.class),
                aiObservabilityTraceService, mock(WorkspaceAiObservabilityTraceService.class),
                mock(AiObservabilitySpanService.class),
                staticMeterRegistryProvider());
        }

        @SuppressWarnings("unchecked")
        private static ObjectProvider<MeterRegistry> staticMeterRegistryProvider() {
            ObjectProvider<MeterRegistry> provider = mock(ObjectProvider.class);

            when(provider.getIfAvailable()).thenReturn(new SimpleMeterRegistry());

            return provider;
        }
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<AiGuardrailsAdvisorProvider> staticGuardrailsAdvisorProvider() {
        return mock(ObjectProvider.class);
    }
}
