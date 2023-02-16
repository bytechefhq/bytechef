
/*
 * Copyright 2021 <your company/name>.
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

import com.bytechef.discovery.metadata.ServiceMetadataRegistry;
import com.bytechef.hermes.component.ComponentDefinitionFactory;
import com.bytechef.hermes.component.definition.Authorization;
import com.bytechef.hermes.component.definition.ConnectionDefinition;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
@DependsOn("workerConfiguration")
@Configuration
@EnableDiscoveryClient
public class DiscoveryClientConfiguration implements InitializingBean {

    private final List<ComponentDefinitionFactory> componentDefinitionFactories;
    private final ObjectMapper objectMapper;
    private final ServiceMetadataRegistry serviceMetadataRegistry;

    @SuppressFBWarnings("EI2")
    public DiscoveryClientConfiguration(
        List<ComponentDefinitionFactory> componentDefinitionFactories,
        ObjectMapper objectMapper, ServiceMetadataRegistry serviceMetadataRegistry) {

        this.componentDefinitionFactories = componentDefinitionFactories;
        this.objectMapper = objectMapper;
        this.serviceMetadataRegistry = serviceMetadataRegistry;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        serviceMetadataRegistry.registerMetadata(
            Map.of(
                "components",
                objectMapper.writeValueAsString(componentDefinitionFactories.stream()
                    .map(ComponentDefinitionFactory::getDefinition)
                    .map(componentDefinition -> {
                        Map<String, Object> map = new HashMap<>();

                        map.put("name", componentDefinition.getName());

                        if (componentDefinition.getConnection() != null) {
                            ConnectionDefinition connectionDefinition = componentDefinition.getConnection();

                            List<? extends Authorization> authorizations = connectionDefinition.getAuthorizations();

                            map.put("connection", Map.of("authorizations", authorizations.stream()
                                .map(authorization -> Map.of("name", authorization.getName(), "type",
                                    authorization.getType()))
                                .toList()));
                        }

                        return map;
                    })
                    .toList())));
    }
}
