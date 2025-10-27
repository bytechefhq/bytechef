/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.worker.config;

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.ee.discovery.metadata.ServiceMetadataRegistry;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
@DependsOn("taskWorkerConfiguration")
public class ComponentMetadataRegistryConfiguration {

    private final ComponentDefinitionService componentDefinitionService;
    private final ServiceMetadataRegistry serviceMetadataRegistry;

    public ComponentMetadataRegistryConfiguration(
        @Qualifier("componentDefinitionService") ComponentDefinitionService componentDefinitionService,
        ServiceMetadataRegistry serviceMetadataRegistry) {

        this.componentDefinitionService = componentDefinitionService;
        this.serviceMetadataRegistry = serviceMetadataRegistry;
    }

    @Bean
    ApplicationRunner componentMetadataRegistryApplicationRunner() {
        return args -> {
            List<ComponentDefinition> componentDefinitions = componentDefinitionService.getComponentDefinitions();

            serviceMetadataRegistry.registerMetadata(
                Map.of(
                    "components",
                    JsonUtils.write(
                        componentDefinitions.stream()
                            .map(componentDefinition -> Map.of("name", componentDefinition.getName()))
                            .toList())));
        };
    }
}
