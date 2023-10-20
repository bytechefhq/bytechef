
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

import com.bytechef.hermes.definition.registry.rsocket.client.facade.ComponentDefinitionFacadeRSocketClient;
import com.bytechef.hermes.definition.registry.rsocket.client.facade.TriggerDefinitionFacadeRSocketClient;
import com.bytechef.hermes.definition.registry.rsocket.client.service.ActionDefinitionServiceRSocketClient;
import com.bytechef.hermes.definition.registry.rsocket.client.service.ComponentDefinitionServiceRSocketClient;
import com.bytechef.hermes.definition.registry.rsocket.client.service.ConnectionDefinitionServiceRSocketClient;
import com.bytechef.hermes.definition.registry.rsocket.client.service.TaskDispatcherDefinitionServiceRSocketClient;
import com.bytechef.hermes.definition.registry.rsocket.client.service.TriggerDefinitionServiceRSocketClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.rsocket.RSocketRequester;

/**
 * @author Ivica Cardic
 */
@Configuration
public class DefinitionRegistryConfiguration {

    @Bean
    ActionDefinitionServiceRSocketClient actionDefinitionServiceRSocketClient(
        DiscoveryClient discoveryClient,
        @Qualifier("workerRSocketRequesterBuilder") RSocketRequester.Builder rSocketRequesterBuilder) {

        return new ActionDefinitionServiceRSocketClient(discoveryClient, rSocketRequesterBuilder);
    }

    @Bean
    ComponentDefinitionFacadeRSocketClient componentDefinitionFacadeRSocketClient(
        DiscoveryClient discoveryClient,
        @Qualifier("workerRSocketRequesterBuilder") RSocketRequester.Builder rSocketRequesterBuilder) {

        return new ComponentDefinitionFacadeRSocketClient(discoveryClient, rSocketRequesterBuilder);
    }

    @Bean
    ComponentDefinitionServiceRSocketClient componentDefinitionServiceRSocketClient(
        DiscoveryClient discoveryClient,
        @Qualifier("workerRSocketRequesterBuilder") RSocketRequester.Builder rSocketRequesterBuilder) {

        return new ComponentDefinitionServiceRSocketClient(discoveryClient, rSocketRequesterBuilder);
    }

    @Bean
    ConnectionDefinitionServiceRSocketClient connectionDefinitionServiceRSocketClient(
        DiscoveryClient discoveryClient,
        @Qualifier("workerRSocketRequesterBuilder") RSocketRequester.Builder rSocketRequesterBuilder) {

        return new ConnectionDefinitionServiceRSocketClient(discoveryClient, rSocketRequesterBuilder);
    }

    @Bean
    TaskDispatcherDefinitionServiceRSocketClient taskDispatcherDefinitionServiceRSocketClient(
        @Qualifier("coordinatorRSocketRequester") RSocketRequester rSocketRequester) {

        return new TaskDispatcherDefinitionServiceRSocketClient(rSocketRequester);
    }

    @Bean
    TriggerDefinitionFacadeRSocketClient triggerDefinitionFacadeRSocketClient(
        DiscoveryClient discoveryClient,
        @Qualifier("workerRSocketRequesterBuilder") RSocketRequester.Builder rSocketRequesterBuilder) {

        return new TriggerDefinitionFacadeRSocketClient(discoveryClient, rSocketRequesterBuilder);
    }
    @Bean
    TriggerDefinitionServiceRSocketClient triggerDefinitionServiceRSocketClient(
        DiscoveryClient discoveryClient,
        @Qualifier("workerRSocketRequesterBuilder") RSocketRequester.Builder rSocketRequesterBuilder) {

        return new TriggerDefinitionServiceRSocketClient(discoveryClient, rSocketRequesterBuilder);
    }
}
