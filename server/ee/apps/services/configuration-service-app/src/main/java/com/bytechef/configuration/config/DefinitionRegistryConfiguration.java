
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

package com.bytechef.configuration.config;

import com.bytechef.commons.webclient.DefaultWebClient;
import com.bytechef.commons.webclient.LoadBalancedWebClient;
import com.bytechef.hermes.definition.registry.remote.client.facade.ActionDefinitionFacadeClient;
import com.bytechef.hermes.definition.registry.remote.client.facade.ComponentDefinitionFacadeClient;
import com.bytechef.hermes.definition.registry.remote.client.facade.TriggerDefinitionFacadeClient;
import com.bytechef.hermes.definition.registry.remote.client.service.ActionDefinitionServiceClient;
import com.bytechef.hermes.definition.registry.remote.client.service.ComponentDefinitionServiceClient;
import com.bytechef.hermes.definition.registry.remote.client.service.ConnectionDefinitionServiceClient;
import com.bytechef.hermes.definition.registry.remote.client.service.TaskDispatcherDefinitionServiceClient;
import com.bytechef.hermes.definition.registry.remote.client.service.TriggerDefinitionServiceClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Ivica Cardic
 */
@Configuration
public class DefinitionRegistryConfiguration {

    @Bean
    ActionDefinitionFacadeClient actionDefinitionFacadeClient(
        DefaultWebClient defaultWebClient, DiscoveryClient discoveryClient, ObjectMapper objectMapper) {

        return new ActionDefinitionFacadeClient(defaultWebClient, discoveryClient, objectMapper);
    }

    @Bean
    ActionDefinitionServiceClient actionDefinitionServiceClient(
        DefaultWebClient defaultWebClient, DiscoveryClient discoveryClient, ObjectMapper objectMapper) {

        return new ActionDefinitionServiceClient(defaultWebClient, discoveryClient, objectMapper);
    }

    @Bean
    ComponentDefinitionFacadeClient componentDefinitionFacadeClient(
        DefaultWebClient defaultWebClient, DiscoveryClient discoveryClient, ObjectMapper objectMapper) {

        return new ComponentDefinitionFacadeClient(defaultWebClient, discoveryClient, objectMapper);
    }

    @Bean
    ComponentDefinitionServiceClient componentDefinitionServiceClient(
        DefaultWebClient defaultWebClient, DiscoveryClient discoveryClient, ObjectMapper objectMapper) {

        return new ComponentDefinitionServiceClient(defaultWebClient, discoveryClient, objectMapper);
    }

    @Bean
    ConnectionDefinitionServiceClient connectionDefinitionServiceClient(
        DefaultWebClient defaultWebClient, DiscoveryClient discoveryClient, ObjectMapper objectMapper) {

        return new ConnectionDefinitionServiceClient(defaultWebClient, discoveryClient, objectMapper);
    }

    @Bean
    TaskDispatcherDefinitionServiceClient taskDispatcherDefinitionServiceClient(
        LoadBalancedWebClient loadBalancedWebClient) {

        return new TaskDispatcherDefinitionServiceClient(loadBalancedWebClient);
    }

    @Bean
    TriggerDefinitionFacadeClient triggerDefinitionFacadeClient(
        DefaultWebClient defaultWebClient, DiscoveryClient discoveryClient, ObjectMapper objectMapper) {

        return new TriggerDefinitionFacadeClient(defaultWebClient, discoveryClient, objectMapper);
    }

    @Bean
    TriggerDefinitionServiceClient triggerDefinitionServiceClient(
        DefaultWebClient defaultWebClient, DiscoveryClient discoveryClient, ObjectMapper objectMapper) {

        return new TriggerDefinitionServiceClient(defaultWebClient, discoveryClient, objectMapper);
    }
}
