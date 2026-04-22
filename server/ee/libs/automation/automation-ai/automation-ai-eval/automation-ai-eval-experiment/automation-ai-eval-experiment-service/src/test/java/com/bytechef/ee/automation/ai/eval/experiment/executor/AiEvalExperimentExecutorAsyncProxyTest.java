/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.eval.experiment.executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ee.automation.ai.eval.experiment.service.WorkspaceAiEvalExperimentService;
import com.bytechef.ee.automation.ai.gateway.evaluation.AiEvalExecutor;
import com.bytechef.ee.automation.ai.gateway.facade.AiGatewayFacade;
import com.bytechef.ee.automation.ai.observability.service.WorkspaceAiObservabilityTraceService;
import com.bytechef.ee.platform.ai.eval.dataset.service.AiEvalDatasetItemService;
import com.bytechef.ee.platform.ai.eval.experiment.service.AiEvalExperimentRunService;
import com.bytechef.ee.platform.ai.eval.experiment.service.AiEvalExperimentService;
import com.bytechef.ee.platform.ai.observability.service.AiObservabilityTraceService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import tools.jackson.databind.ObjectMapper;

/**
 * Pins that {@link AiEvalExperimentExecutor} is wrapped by Spring's {@code @Async} proxy AND that the {@code execute}
 * method's {@code @Async} annotation is qualified with the {@code aiEvalExperimentTaskExecutor} pool name. A regression
 * that loses {@code @EnableAsync}, drops the {@code @Async} annotation, refactors the cross-bean call into an in-class
 * {@code this.execute(...)} self-invocation, or removes the named-pool qualifier would silently make experiment
 * execution synchronous on the controller request thread (or starve a different pool), defeating the bounded
 * 8-thread/100-queue pool the experiment subsystem is intended to run on.
 *
 * <p>
 * Mirrors {@code AiEvalExecutorAsyncProxyTest}. Uses a direct {@link AnnotationConfigApplicationContext} (no Spring
 * Boot test slice) to keep the assertion focused on AOP wiring; pulling in the full {@code @SpringBootTest}
 * infrastructure would dilute the failure signal.
 *
 * @author Ivica Cardic
 * @version ee
 */
class AiEvalExperimentExecutorAsyncProxyTest {

    private static final String EXPECTED_QUALIFIER = "aiEvalExperimentTaskExecutor";

    @Test
    void testAiEvalExperimentExecutorIsAopProxyWhenEnableAsyncIsActive() {
        try (AnnotationConfigApplicationContext context =
            new AnnotationConfigApplicationContext(AsyncProxyTestConfiguration.class)) {

            AiEvalExperimentExecutor aiEvalExperimentExecutor = context.getBean(AiEvalExperimentExecutor.class);

            assertThat(AopUtils.isAopProxy(aiEvalExperimentExecutor))
                .as("AiEvalExperimentExecutor must be wrapped by Spring's AOP proxy so @Async on execute fires; " +
                    "a missing @EnableAsync, removed @Async annotation, or refactor exposing a raw bean " +
                    "would silently make experiment execution run on the caller's thread")
                .isTrue();
        }
    }

    @Test
    void testExecuteAsyncAnnotationCarriesNamedPoolQualifier() throws NoSuchMethodException {
        Method executeMethod = AiEvalExperimentExecutor.class.getMethod("execute", long.class);

        Async asyncAnnotation = executeMethod.getAnnotation(Async.class);

        assertThat(asyncAnnotation)
            .as("AiEvalExperimentExecutor.execute must carry an @Async annotation; without it the method runs " +
                "synchronously on the caller's thread regardless of @EnableAsync")
            .isNotNull();

        assertThat(asyncAnnotation.value())
            .as("AiEvalExperimentExecutor.execute must qualify @Async with the bounded '%s' pool; an unqualified " +
                "@Async would route to Spring's default pool and the bounded 8-thread/100-queue contract " +
                "the experiment subsystem relies on would be lost",
                EXPECTED_QUALIFIER)
            .isEqualTo(EXPECTED_QUALIFIER);
    }

    @Configuration
    @EnableAsync
    static class AsyncProxyTestConfiguration {

        @Bean
        AiEvalExperimentExecutor aiEvalExperimentExecutor() {
            return new AiEvalExperimentExecutor(
                mock(AiEvalDatasetItemService.class),
                mock(AiEvalExecutor.class),
                mock(AiEvalExperimentRunService.class),
                mock(AiEvalExperimentService.class),
                mock(WorkspaceAiEvalExperimentService.class),
                mock(AiGatewayFacade.class),
                mock(AiObservabilityTraceService.class),
                mock(WorkspaceAiObservabilityTraceService.class),
                mock(RetryTemplate.class),
                mock(ObjectMapper.class),
                staticMeterRegistryProvider(),
                1);
        }

        @SuppressWarnings("unchecked")
        private static ObjectProvider<MeterRegistry> staticMeterRegistryProvider() {
            ObjectProvider<MeterRegistry> provider = mock(ObjectProvider.class);

            when(provider.getIfAvailable()).thenReturn(new SimpleMeterRegistry());

            return provider;
        }
    }
}
