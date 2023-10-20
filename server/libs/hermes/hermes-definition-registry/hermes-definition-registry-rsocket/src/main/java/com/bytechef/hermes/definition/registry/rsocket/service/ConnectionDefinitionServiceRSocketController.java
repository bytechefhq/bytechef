
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

import com.bytechef.hermes.component.definition.ConnectionDefinition;
import com.bytechef.hermes.definition.registry.service.ConnectionDefinitionService;
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
public class ConnectionDefinitionServiceRSocketController {

    private final ConnectionDefinitionService connectionDefinitionService;

    public ConnectionDefinitionServiceRSocketController(ConnectionDefinitionService connectionDefinitionService) {
        this.connectionDefinitionService = connectionDefinitionService;
    }

    @MessageMapping("Service.getComponentConnectionDefinitions")
    public Mono<List<ConnectionDefinition>> getComponentConnectionDefinitions(String componentName) {
        return connectionDefinitionService.getConnectionDefinitionsMono(componentName);
    }

    @MessageMapping("Service.getConnectionDefinition")
    public Mono<ConnectionDefinition> getConnectionDefinition(Map<String, Object> map) {
        return connectionDefinitionService.getConnectionDefinitionMono(
            (String) map.get("componentName"), (Integer) map.get("componentVersion"));
    }

    @MessageMapping("Service.getConnectionDefinitions")
    public Mono<List<ConnectionDefinition>> getConnectionDefinitions() {
        return connectionDefinitionService.getConnectionDefinitionsMono();
    }
}
