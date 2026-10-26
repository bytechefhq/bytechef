/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import com.bytechef.ee.platform.ai.gateway.catalog.AiModelCatalogReconciler;
import com.bytechef.ee.platform.ai.model.catalog.service.AiModelService;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class AiModelFacadeAsyncProxyTest {

    @Test
    void testReconcileCatalogReturnsBeforeTheReconcilerCompletes() throws InterruptedException {
        try (AnnotationConfigApplicationContext context =
            new AnnotationConfigApplicationContext(AsyncProxyTestConfiguration.class)) {
            AiModelFacade facade = context.getBean(AiModelFacade.class);

            assertThat(AopUtils.isAopProxy(facade))
                .as("AiModelFacadeImpl must be wrapped by Spring's AOP proxy so @Async on "
                    + "reconcileCatalog fires; a missing @EnableAsync or a raw bean would silently make the "
                    + "reconcile run on the caller's thread")
                .isTrue();

            CountDownLatch reconcileStarted = context.getBean("reconcileStarted", CountDownLatch.class);
            CountDownLatch releaseReconcile = context.getBean("releaseReconcile", CountDownLatch.class);

            long startNanos = System.nanoTime();

            facade.reconcileCatalog();

            Duration callDuration = Duration.ofNanos(System.nanoTime() - startNanos);

            assertThat(callDuration)
                .as("reconcileCatalog() must return immediately rather than block on the reconciler")
                .isLessThan(Duration.ofSeconds(2));

            assertThat(reconcileStarted.await(5, TimeUnit.SECONDS))
                .as("the reconciler must actually run, just on a different thread")
                .isTrue();

            releaseReconcile.countDown();
        }
    }

    @EnableAsync
    static class AsyncProxyTestConfiguration {

        @Bean
        CountDownLatch reconcileStarted() {
            return new CountDownLatch(1);
        }

        @Bean
        CountDownLatch releaseReconcile() {
            return new CountDownLatch(1);
        }

        @Bean
        AiModelCatalogReconciler aiModelCatalogReconciler(
            CountDownLatch reconcileStarted, CountDownLatch releaseReconcile) {
            AiModelCatalogReconciler reconciler = mock(AiModelCatalogReconciler.class);

            doAnswer(invocation -> {
                reconcileStarted.countDown();
                releaseReconcile.await(5, TimeUnit.SECONDS);

                return null;
            }).when(reconciler)
                .reconcile();

            return reconciler;
        }

        @Bean
        AiModelFacade aiModelFacade(AiModelCatalogReconciler aiModelCatalogReconciler) {
            return new AiModelFacadeImpl(mock(AiModelService.class), aiModelCatalogReconciler);
        }
    }
}
