
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

package com.bytechef.hermes.definition.registry.rsocket.service;

import com.bytechef.hermes.component.definition.ComponentDefinition;
import com.bytechef.hermes.definition.registry.service.ComponentDefinitionService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnProperty(prefix = "spring", name = "application.name", havingValue = "worker-service-app")
public class ComponentDefinitionServiceRSocketController {

    private final ComponentDefinitionService componentDefinitionService;

    public ComponentDefinitionServiceRSocketController(ComponentDefinitionService componentDefinitionService) {
        this.componentDefinitionService = componentDefinitionService;
    }

    @MessageMapping("Service.getComponentDefinitions")
    public Mono<List<ComponentDefinition>> getComponentDefinitions() {
        return componentDefinitionService.getComponentDefinitionsMono();
    }

    @MessageMapping("Service.getComponentDefinitionsForName")
    public Mono<List<ComponentDefinition>> getComponentDefinitions(String name) {
        return componentDefinitionService.getComponentDefinitionsMono(name);
    }

    @MessageMapping("Service.getComponentDefinition")
    public Mono<ComponentDefinition> getComponentDefinition(Map<String, Object> map) {
        return componentDefinitionService.getComponentDefinitionMono((String) map.get("name"),
            (Integer) map.get("version"));
    }
}
