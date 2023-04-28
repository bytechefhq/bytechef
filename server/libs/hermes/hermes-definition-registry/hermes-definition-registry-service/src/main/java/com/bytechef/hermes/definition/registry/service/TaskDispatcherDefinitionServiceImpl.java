
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

package com.bytechef.hermes.definition.registry.service;

import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.hermes.definition.registry.dto.TaskDispatcherDefinitionDTO;
import com.bytechef.hermes.definition.registry.util.DefinitionUtils;
import com.bytechef.hermes.task.dispatcher.definition.TaskDispatcherDefinition;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;

/**
 * @author Ivica Cardic
 */
public class TaskDispatcherDefinitionServiceImpl implements TaskDispatcherDefinitionService {

    private final List<TaskDispatcherDefinition> taskDispatcherDefinitions;

    @SuppressFBWarnings("EI2")
    public TaskDispatcherDefinitionServiceImpl(List<TaskDispatcherDefinition> taskDispatcherDefinitions) {
        this.taskDispatcherDefinitions = taskDispatcherDefinitions.stream()
            .sorted((o1, o2) -> {
                String o1Name = o1.getName();

                return o1Name.compareTo(o2.getName());
            })
            .toList();
    }

    @Override
    public Mono<TaskDispatcherDefinitionDTO> getTaskDispatcherDefinitionMono(String name, Integer version) {
        return Mono.just(
            toTaskDispatcherDefinitionDTO(
                CollectionUtils.getFirst(
                    taskDispatcherDefinitions,
                    taskDispatcherDefinition -> name.equalsIgnoreCase(taskDispatcherDefinition.getName())
                        && version == taskDispatcherDefinition.getVersion())));
    }

    @Override
    public Mono<List<TaskDispatcherDefinitionDTO>> getTaskDispatcherDefinitionsMono() {
        return Mono.just(CollectionUtils.map(taskDispatcherDefinitions, this::toTaskDispatcherDefinitionDTO));
    }

    @Override
    public Mono<List<TaskDispatcherDefinitionDTO>> getTaskDispatcherDefinitionsMono(String name) {
        return Mono.just(
            CollectionUtils.map(
                CollectionUtils.filter(
                    taskDispatcherDefinitions,
                    taskDispatcherDefinition -> Objects.equals(taskDispatcherDefinition.getName(), name)),
                this::toTaskDispatcherDefinitionDTO));
    }

    private TaskDispatcherDefinitionDTO toTaskDispatcherDefinitionDTO(
        TaskDispatcherDefinition taskDispatcherDefinition) {

        return new TaskDispatcherDefinitionDTO(
            OptionalUtils.orElse(taskDispatcherDefinition.getDescription(), null),
            DefinitionUtils.readIcon(taskDispatcherDefinition.getIcon()), taskDispatcherDefinition.getName(),
            taskDispatcherDefinition.getOutputSchema(), taskDispatcherDefinition.getProperties(),
            taskDispatcherDefinition.getResources(), taskDispatcherDefinition.getTaskProperties(),
            taskDispatcherDefinition.getTitle(), taskDispatcherDefinition.getVersion());
    }

}
