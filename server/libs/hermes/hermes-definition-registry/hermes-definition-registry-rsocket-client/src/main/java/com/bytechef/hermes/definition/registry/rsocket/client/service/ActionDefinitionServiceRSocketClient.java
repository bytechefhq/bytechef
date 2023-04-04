
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

package com.bytechef.hermes.definition.registry.rsocket.client.service;

import com.bytechef.commons.rsocket.util.RSocketUtils;
import com.bytechef.hermes.definition.registry.dto.ActionDefinitionDTO;
import com.bytechef.hermes.definition.registry.service.ActionDefinitionService;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.messaging.rsocket.RSocketRequester;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class ActionDefinitionServiceRSocketClient implements ActionDefinitionService {

    private static final String WORKER_SERVICE_APP = "worker-service-app";

    private final DiscoveryClient discoveryClient;
    private final RSocketRequester.Builder rSocketRequesterBuilder;

    public ActionDefinitionServiceRSocketClient(
        DiscoveryClient discoveryClient, RSocketRequester.Builder rSocketRequesterBuilder) {

        this.discoveryClient = discoveryClient;
        this.rSocketRequesterBuilder = rSocketRequesterBuilder;
    }

    @Override
    public Mono<ActionDefinitionDTO> getComponentActionDefinitionMono(
        String componentName, int componentVersion, String actionName) {

        return getRSocketRequester(componentName)
            .route("ActionDefinitionService.getComponentActionDefinition")
            .data(
                Map.of("componentName", componentName, "componentVersion", componentVersion, "actionName", actionName))
            .retrieveMono(ActionDefinitionDTO.class);
    }

    @Override
    public Mono<List<ActionDefinitionDTO>> getComponentActionDefinitionsMono(
        String componentName, int componentVersion) {

        return getRSocketRequester(componentName)
            .route("ActionDefinitionService.getComponentActionDefinitions")
            .data(
                Map.of("componentName", componentName, "componentVersion", componentVersion))
            .retrieveMono(new ParameterizedTypeReference<>() {});
    }

    private RSocketRequester getRSocketRequester(String componentName) {
        return RSocketUtils.getRSocketRequester(
            discoveryClient.getInstances(WORKER_SERVICE_APP), componentName, rSocketRequesterBuilder);
    }
}
