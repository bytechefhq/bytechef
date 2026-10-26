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
 * @version ee
 *
 * @author Ivica Cardic
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
