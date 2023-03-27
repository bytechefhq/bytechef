
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
import com.bytechef.hermes.task.dispatcher.definition.TaskDispatcherDefinition;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author Ivica Cardic
 */
public class TaskDispatcherDefinitionServiceImpl implements TaskDispatcherDefinitionService {

    private final List<TaskDispatcherDefinition> taskDispatcherDefinitions;

    @SuppressFBWarnings("EI2")
    public TaskDispatcherDefinitionServiceImpl(List<TaskDispatcherDefinition> taskDispatcherDefinitions) {
        this.taskDispatcherDefinitions = taskDispatcherDefinitions;
    }

    @Override
    public Mono<TaskDispatcherDefinition> getTaskDispatcherDefinitionMono(String name, Integer version) {
        return Mono.just(
            CollectionUtils.findFirst(
                taskDispatcherDefinitions,
                taskDispatcherDefinition -> name.equalsIgnoreCase(taskDispatcherDefinition.getName())
                    && version == taskDispatcherDefinition.getVersion()));
    }

    @Override
    public Mono<List<TaskDispatcherDefinition>> getTaskDispatcherDefinitionsMono() {
        return Mono.just(new ArrayList<>(taskDispatcherDefinitions));
    }

    @Override
    public Mono<List<TaskDispatcherDefinition>> getTaskDispatcherDefinitionsMono(String name) {
        return Mono.just(
            CollectionUtils.filter(
                taskDispatcherDefinitions,
                taskDispatcherDefinition -> Objects.equals(taskDispatcherDefinition.getName(), name)));
    }
}
