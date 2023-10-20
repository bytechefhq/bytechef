
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

package com.bytechef.hermes.definition.registry.rsocket.controller.service;

import com.bytechef.hermes.definition.registry.dto.TaskDispatcherDefinitionDTO;
import com.bytechef.hermes.definition.registry.service.TaskDispatcherDefinitionService;
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
@ConditionalOnProperty(prefix = "spring", name = "application.name", havingValue = "coordinator-service-app")
public class TaskDispatcherDefinitionServiceRSocketController {

    private final TaskDispatcherDefinitionService taskDispatcherDefinitionService;

    public TaskDispatcherDefinitionServiceRSocketController(
        TaskDispatcherDefinitionService taskDispatcherDefinitionService) {

        this.taskDispatcherDefinitionService = taskDispatcherDefinitionService;
    }

    @MessageMapping("TaskDispatcherDefinitionService.getTaskDispatcherDefinition")
    public Mono<TaskDispatcherDefinitionDTO> getTaskDispatcherDefinitions(Map<String, Object> map) {
        return Mono.just(
            taskDispatcherDefinitionService.getTaskDispatcherDefinition(
                (String) map.get("name"), (Integer) map.get("version")));
    }

    @MessageMapping("TaskDispatcherDefinitionService.getTaskDispatcherDefinitions")
    public Mono<List<TaskDispatcherDefinitionDTO>> getTaskDispatcherDefinitions() {
        return Mono.just(taskDispatcherDefinitionService.getTaskDispatcherDefinitions());
    }

    @MessageMapping("TaskDispatcherDefinitionService.getComponentDefinitionsForName")
    public Mono<List<TaskDispatcherDefinitionDTO>> getTaskDispatcherDefinitions(String name) {
        return Mono.just(taskDispatcherDefinitionService.getTaskDispatcherDefinitions(name));
    }
}
