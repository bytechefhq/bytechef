
/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.worker.config;

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.discovery.metadata.ServiceMetadataRegistry;
import com.bytechef.hermes.component.registry.domain.ComponentDefinition;
import com.bytechef.hermes.component.registry.service.ComponentDefinitionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import java.util.List;
import java.util.Map;

/**
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
