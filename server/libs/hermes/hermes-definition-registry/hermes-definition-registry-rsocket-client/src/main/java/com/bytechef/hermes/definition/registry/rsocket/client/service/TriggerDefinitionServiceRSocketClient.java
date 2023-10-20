
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

import com.bytechef.commons.reactor.util.MonoUtils;
import com.bytechef.hermes.definition.registry.dto.TriggerDefinitionDTO;
import com.bytechef.hermes.definition.registry.rsocket.AbstractRSocketClient;
import com.bytechef.hermes.definition.registry.service.TriggerDefinitionService;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.messaging.rsocket.RSocketRequester;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class TriggerDefinitionServiceRSocketClient extends AbstractRSocketClient
    implements TriggerDefinitionService {

    public TriggerDefinitionServiceRSocketClient(
        DiscoveryClient discoveryClient, RSocketRequester.Builder rSocketRequesterBuilder) {

        super(discoveryClient, rSocketRequesterBuilder);
    }

    @Override
    public TriggerDefinitionDTO getTriggerDefinition(String componentName, int componentVersion, String triggerName) {
        return MonoUtils.get(getRSocketRequester(componentName)
            .route("TriggerDefinitionService.getTriggerDefinition")
            .data(
                Map.of(
                    "componentName", componentName, "componentVersion", componentVersion,
                    "triggerName", triggerName))
            .retrieveMono(TriggerDefinitionDTO.class));
    }

    @Override
    public Mono<TriggerDefinitionDTO> getTriggerDefinitionMono(
        String componentName, int componentVersion, String triggerName) {

        return getRSocketRequester(componentName)
            .route("TriggerDefinitionService.getTriggerDefinition")
            .data(
                Map.of(
                    "componentName", componentName, "componentVersion", componentVersion,
                    "triggerName", triggerName))
            .retrieveMono(TriggerDefinitionDTO.class);
    }

    @Override
    public Mono<List<TriggerDefinitionDTO>> getTriggerDefinitions(
        String componentName, int componentVersion) {

        return getRSocketRequester(componentName)
            .route("TriggerDefinitionService.getTriggerDefinitions")
            .data(
                Map.of("componentName", componentName, "componentVersion", componentVersion))
            .retrieveMono(new ParameterizedTypeReference<>() {});
    }
}
