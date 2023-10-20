/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.worker.config;

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.discovery.metadata.ServiceMetadataRegistry;
import com.bytechef.hermes.component.registry.domain.ComponentDefinition;
import com.bytechef.hermes.component.registry.service.ComponentDefinitionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@DependsOn("taskWorkerConfiguration")
@Configuration
public class ComponentMetadataRegistryConfiguration implements InitializingBean {

    private final ComponentDefinitionService componentDefinitionService;
    private final ObjectMapper objectMapper;
    private final ServiceMetadataRegistry serviceMetadataRegistry;

    @SuppressFBWarnings("EI2")
    public ComponentMetadataRegistryConfiguration(
        @Qualifier("componentDefinitionService") ComponentDefinitionService componentDefinitionService,
        ObjectMapper objectMapper, ServiceMetadataRegistry serviceMetadataRegistry) {

        this.componentDefinitionService = componentDefinitionService;
        this.objectMapper = objectMapper;
        this.serviceMetadataRegistry = serviceMetadataRegistry;
    }

    @Override
    public void afterPropertiesSet() {
        List<ComponentDefinition> componentDefinitions = componentDefinitionService.getComponentDefinitions();

        serviceMetadataRegistry.registerMetadata(
            Map.of(
                "components",
                JsonUtils.write(
                    componentDefinitions.stream()
                        .map(componentDefinition -> Map.of("name", componentDefinition.getName()))
                        .toList(),
                    objectMapper)));
    }
}
