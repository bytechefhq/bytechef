/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.bytechef.ee.platform.ai.gateway.service.AiGatewayProviderService;
import com.bytechef.ee.platform.ai.model.catalog.service.AiModelService;
import com.bytechef.platform.ai.model.catalog.ModelCatalog;
import com.bytechef.platform.ai.model.catalog.config.ModelCatalogConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * Pins that {@link AiModelCatalogReconcilerConfiguration} and {@link ModelCatalogConfiguration} wire together cleanly
 * in a single Spring context: the reconciler, its scheduler, and the {@link ModelCatalog} bean must each resolve
 * exactly once.
 *
 * <p>
 * Two prior code reviews flagged the absence of a context test for this wiring as a deferred minor. That gap is exactly
 * why a genuine duplicate registration of the {@code aiModelCatalogReconciler} bean reached the final {@code check}
 * gate undetected — the only thing exercising this wiring at all was {@code AiGatewayIntTestConfiguration}'s full
 * 14-class, Testcontainers-backed integration suite in the automation module, and even that duplicate came from an
 * unrelated test's nested {@code @Configuration} class sitting in a component-scanned package, not from this
 * configuration itself. A lightweight context test at this level closes that coverage gap without needing a database.
 *
 * @author Ivica Cardic
 * @version ee
 */
class AiModelCatalogReconcilerConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withPropertyValues("bytechef.edition=ee", "bytechef.ai.gateway.enabled=true")
        .withBean(AiModelService.class, () -> mock(AiModelService.class))
        .withBean(AiGatewayProviderService.class, () -> mock(AiGatewayProviderService.class))
        .withConfiguration(AutoConfigurations.of(
            AiModelCatalogReconcilerConfiguration.class, ModelCatalogConfiguration.class));

    @Test
    void testReconcilerAndModelCatalogBeansEachResolveExactlyOnce() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(AiModelCatalogReconciler.class);
            assertThat(context).hasSingleBean(AiModelCatalogReconcilerScheduler.class);
            assertThat(context).hasSingleBean(ModelCatalog.class);
        });
    }
}
