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
import com.bytechef.ee.automation.ai.gateway.facade.AiGatewayFacade;
import com.bytechef.ee.platform.ai.eval.dataset.service.AiEvalDatasetItemService;
import com.bytechef.ee.platform.ai.eval.experiment.service.AiEvalExperimentRunService;
import com.bytechef.ee.platform.ai.eval.experiment.service.AiEvalExperimentService;
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
                mock(AiEvalExperimentRunService.class),
                mock(AiEvalExperimentService.class),
                mock(WorkspaceAiEvalExperimentService.class),
                mock(AiGatewayFacade.class),
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
