/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.evaluation;

import static org.assertj.core.api.Assertions.assertThat;
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
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Pins that {@link AiEvalExecutor} is wrapped by Spring's {@code @Async} proxy when {@link EnableAsync} is active. Unit
 * tests that instantiate the executor with {@code new} bypass the proxy entirely, so a future change that loses
 * {@code @EnableAsync} on the gateway service module's configuration, drops the {@code @Async} annotation from the
 * public {@code evaluateTrace} overloads, or refactors the cross-bean call into an in-class
 * {@code this.evaluateTrace(...)} self-invocation would silently make eval execute synchronously on the live-trace
 * ingest thread — blocking downstream OTLP ingest and inflating tail latency.
 *
 * <p>
 * Uses a direct {@link AnnotationConfigApplicationContext} (no Spring Boot test slice) to keep the assertion focused on
 * AOP wiring; pulling in the full {@code @SpringBootTest} infrastructure would dilute the failure signal — a regression
 * here would be drowned by unrelated context startup errors.
 *
 * @author Ivica Cardic
 * @version ee
 */
class AiEvalExecutorAsyncProxyTest {

    @Test
    void testAiEvalExecutorIsAopProxyWhenEnableAsyncIsActive() {
        try (AnnotationConfigApplicationContext context =
            new AnnotationConfigApplicationContext(AsyncProxyTestConfiguration.class)) {

            AiEvalExecutor aiEvalExecutor = context.getBean(AiEvalExecutor.class);

            // AopUtils.isAopProxy returns true for both JDK dynamic proxies and CGLIB subclass proxies; either
            // is fine here — Spring picks based on whether AiEvalExecutor implements an interface (it does
            // not, so CGLIB is used in practice). The load-bearing assertion is that SOME proxy wraps the
            // bean, otherwise @Async is dead and eval runs on the caller's thread.
            assertThat(AopUtils.isAopProxy(aiEvalExecutor))
                .as("AiEvalExecutor must be wrapped by Spring's AOP proxy so @Async on evaluateTrace fires; " +
                    "a missing @EnableAsync, removed @Async annotation, or refactor exposing a raw bean " +
                    "would silently make eval run on the caller's thread")
                .isTrue();
        }
    }

    /**
     * Deliberately carries no {@code @Configuration}/{@code @TestConfiguration} stereotype. This class lives in
     * {@code com.bytechef.ee.automation.ai.gateway.evaluation}, a package {@code AiGatewayIntTestConfiguration}
     * component-scans for its integration suite; a stereotype annotation here would make that scan pick up this class
     * too and register its mock {@code aiEvalExecutor} bean into every integration test's context. That has not yet
     * collided with a same-named production bean the way the sibling {@code
     * AiModelFacadeAsyncProxyTest.AsyncProxyTestConfiguration} did (see that class's Javadoc for the mechanism and the
     * {@code BeanDefinitionOverrideException} it caused), but it is the identical latent risk and fixed the same way.
     * Explicit registration via {@code new AnnotationConfigApplicationContext(
     * AsyncProxyTestConfiguration.class)} below does not need a stereotype: {@code @Bean} methods on an explicitly
     * registered class are processed as a "lite" configuration class regardless.
     */
    @EnableAsync
    static class AsyncProxyTestConfiguration {

        @Bean
        AiEvalExecutor aiEvalExecutor() {
            // Mocked dependencies are sufficient — we don't invoke any method, only assert the bean is
            // proxied. @Async annotations are detected by class scan, not by call execution.
            return new AiEvalExecutor(
                mock(AiEvalExecutionService.class),
                mock(AiEvalRuleService.class),
                mock(AiEvalScoreConfigService.class),
                mock(WorkspaceAiEvalScoreService.class),
                mock(WorkspaceAiEvalRuleService.class),
                mock(AiGatewayChatModelFactory.class),
                mock(AiGatewayProviderService.class),
                mock(AiObservabilityTraceService.class), mock(WorkspaceAiObservabilityTraceService.class),
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
}
