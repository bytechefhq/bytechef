
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
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.hermes.definition.registry.dto.TaskDispatcherDefinitionDTO;
import com.bytechef.hermes.definition.registry.service.TaskDispatcherDefinitionService;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.messaging.rsocket.RSocketRequester;

import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class TaskDispatcherDefinitionServiceRSocketClient implements TaskDispatcherDefinitionService {

    private final RSocketRequester rSocketRequester;

    public TaskDispatcherDefinitionServiceRSocketClient(RSocketRequester rSocketRequester) {
        this.rSocketRequester = rSocketRequester;
    }

    @Override
    public TaskDispatcherDefinitionDTO getTaskDispatcherDefinition(String name, Integer version) {
        return MonoUtils.get(
            rSocketRequester
                .route("TaskDispatcherDefinitionService.getTaskDispatcherDefinition")
                .data(Map.of("name", name, "version", version))
                .retrieveMono(TaskDispatcherDefinitionDTO.class)
                .map(taskDispatcherDefinition -> taskDispatcherDefinition));
    }

    @Override
    public List<TaskDispatcherDefinitionDTO> getTaskDispatcherDefinitions() {
        return MonoUtils.get(
            rSocketRequester
                .route("TaskDispatcherDefinitionService.getTaskDispatcherDefinitions")
                .retrieveMono(
                    new ParameterizedTypeReference<List<TaskDispatcherDefinitionDTO>>() {})
                .map(taskDispatcherDefinitions -> CollectionUtils.map(
                    taskDispatcherDefinitions, taskDispatcherDefinition -> taskDispatcherDefinition)));
    }

    @Override
    public List<TaskDispatcherDefinitionDTO> getTaskDispatcherDefinitions(String name) {
        return MonoUtils.get(
            rSocketRequester
                .route("TaskDispatcherDefinitionService.getComponentDefinitionsForName")
                .data(name)
                .retrieveMono(
                    new ParameterizedTypeReference<List<TaskDispatcherDefinitionDTO>>() {})
                .map(taskDispatcherDefinitions -> CollectionUtils.map(
                    taskDispatcherDefinitions, taskDispatcherDefinition -> taskDispatcherDefinition)));
    }
}
