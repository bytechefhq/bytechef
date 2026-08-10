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
 * Pins that {@link AiModelFacadeImpl#reconcileCatalog()} is genuinely dispatched off the caller's thread when
 * {@link EnableAsync} is active, following the {@code AiEvalExecutorAsyncProxyTest} pattern from the sibling
 * {@code platform-ai-gateway-service} module. {@code @Async} only fires through a Spring AOP proxy: a self-invocation
 * or a bean obtained by {@code new} would silently make {@code reconcileCatalog()} run synchronously on the caller's
 * thread, reintroducing the finding this test was added for — a cold-instance reconcile (lazy 3.6 MB catalog parse plus
 * up to ~250 writes per enabled provider) blocking the HTTP request thread.
 *
 * <p>
 * Uses a direct {@link AnnotationConfigApplicationContext} rather than a {@code @SpringBootTest} slice to keep the
 * assertion focused on the AOP wiring, and asserts on actual dispatch timing (the call returns while the reconciler is
 * still blocked) rather than merely {@link AopUtils#isAopProxy}, since a proxy could exist without the {@code @Async}
 * advisor actually intercepting this specific method.
 *
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

    /**
     * Deliberately carries no {@code @Configuration}/{@code @TestConfiguration} stereotype. This class lives in
     * {@code com.bytechef.ee.automation.ai.gateway.facade}, a package {@code AiGatewayIntTestConfiguration}
     * component-scans for its 14-class integration suite; a stereotype annotation here would make that scan pick up
     * this class too and register its mock {@code aiModelCatalogReconciler} bean alongside the real one from
     * {@code AiModelCatalogReconcilerConfiguration}, which collide on bean name and abort context startup for every
     * integration test in the module. {@code @TestConfiguration} does not help either — Spring Boot only excludes
     * {@code @TestConfiguration} classes from the component scan performed for the test class actually under execution,
     * not from an unrelated {@code @ComponentScan} like this one. Explicit registration via
     * {@code new AnnotationConfigApplicationContext(AsyncProxyTestConfiguration.class)} below does not need a
     * stereotype: {@code @Bean} methods on an explicitly registered class are processed as a "lite" configuration class
     * regardless.
     */
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
