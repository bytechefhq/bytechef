/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.catalog;

import com.bytechef.ee.platform.ai.gateway.service.AiGatewayProviderService;
import com.bytechef.ee.platform.ai.model.catalog.service.AiModelService;
import com.bytechef.platform.ai.model.catalog.ModelCatalog;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
public class AiModelCatalogReconcilerConfiguration {

    @Bean
    AiModelCatalogReconciler aiModelCatalogReconciler(
        AiModelService aiModelService, AiGatewayProviderService aiGatewayProviderService,
        ModelCatalog modelCatalog) {

        return new AiModelCatalogReconcilerImpl(aiModelService, aiGatewayProviderService, modelCatalog);
    }

    @Bean
    AiModelCatalogReconcilerScheduler aiModelCatalogReconcilerScheduler(
        AiModelCatalogReconciler reconciler) {

        return new AiModelCatalogReconcilerScheduler(reconciler);
    }
}
