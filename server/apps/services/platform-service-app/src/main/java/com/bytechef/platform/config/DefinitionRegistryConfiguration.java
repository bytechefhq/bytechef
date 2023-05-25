
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

package com.bytechef.platform.config;

import com.bytechef.hermes.definition.registry.service.web.rest.client.facade.ActionDefinitionFacadeClient;
import com.bytechef.hermes.definition.registry.service.web.rest.client.facade.ComponentDefinitionFacadeClient;
import com.bytechef.hermes.definition.registry.service.web.rest.client.facade.TriggerDefinitionFacadeClient;
import com.bytechef.hermes.definition.registry.service.web.rest.client.service.ActionDefinitionServiceClient;
import com.bytechef.hermes.definition.registry.service.web.rest.client.service.ComponentDefinitionServiceClient;
import com.bytechef.hermes.definition.registry.service.web.rest.client.service.ConnectionDefinitionServiceClient;
import com.bytechef.hermes.definition.registry.service.web.rest.client.service.TaskDispatcherDefinitionServiceClient;
import com.bytechef.hermes.definition.registry.service.web.rest.client.service.TriggerDefinitionServiceClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * @author Ivica Cardic
 */
@Configuration
public class DefinitionRegistryConfiguration {

    @Bean
    ActionDefinitionFacadeClient actionDefinitionFacadeClient(
        DiscoveryClient discoveryClient) {

        return new ActionDefinitionFacadeClient(discoveryClient);
    }

    @Bean
    ActionDefinitionServiceClient actionDefinitionServiceClient(
        DiscoveryClient discoveryClient) {

        return new ActionDefinitionServiceClient(discoveryClient);
    }

    @Bean
    ComponentDefinitionFacadeClient componentDefinitionFacadeClient(DiscoveryClient discoveryClient) {
        return new ComponentDefinitionFacadeClient(discoveryClient);
    }

    @Bean
    ComponentDefinitionServiceClient componentDefinitionServiceClient(
        DiscoveryClient discoveryClient) {

        return new ComponentDefinitionServiceClient(discoveryClient);
    }

    @Bean
    ConnectionDefinitionServiceClient connectionDefinitionServiceClient(
        DiscoveryClient discoveryClient) {

        return new ConnectionDefinitionServiceClient(discoveryClient);
    }

    @Bean
    TaskDispatcherDefinitionServiceClient taskDispatcherDefinitionServiceClient(
        @Qualifier("coordinatorWebClientBuilder") WebClient.Builder coordinatorWebClientBuilder) {

        return new TaskDispatcherDefinitionServiceClient(coordinatorWebClientBuilder);
    }

    @Bean
    TriggerDefinitionFacadeClient triggerDefinitionFacadeClient(DiscoveryClient discoveryClient) {
        return new TriggerDefinitionFacadeClient(discoveryClient);
    }

    @Bean
    TriggerDefinitionServiceClient triggerDefinitionServiceClient(DiscoveryClient discoveryClient) {
        return new TriggerDefinitionServiceClient(discoveryClient);
    }
}
