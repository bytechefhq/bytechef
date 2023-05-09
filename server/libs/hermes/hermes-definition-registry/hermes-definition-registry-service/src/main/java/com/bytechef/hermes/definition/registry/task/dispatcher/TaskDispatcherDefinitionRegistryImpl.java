
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

package com.bytechef.hermes.definition.registry.task.dispatcher;

import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.hermes.task.dispatcher.TaskDispatcherDefinitionFactory;
import com.bytechef.hermes.task.dispatcher.definition.TaskDispatcherDefinition;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.List;
import java.util.Objects;

/**
 * @author Ivica Cardic
 */
public class TaskDispatcherDefinitionRegistryImpl implements TaskDispatcherDefinitionRegistry {

    private final List<TaskDispatcherDefinition> taskDispatcherDefinitions;

    @SuppressFBWarnings("EI")
    public TaskDispatcherDefinitionRegistryImpl(
        List<TaskDispatcherDefinitionFactory> taskDispatcherDefinitionFactories) {

        this.taskDispatcherDefinitions = taskDispatcherDefinitionFactories.stream()
            .map(TaskDispatcherDefinitionFactory::getDefinition)
            .sorted((o1, o2) -> {
                String o1Name = o1.getName();

                return o1Name.compareTo(o2.getName());
            })
            .toList();
    }

    @Override
    public TaskDispatcherDefinition getTaskDispatcherDefinition(String name, Integer version) {
        return CollectionUtils.getFirst(
            taskDispatcherDefinitions,
            taskDispatcherDefinition -> name.equalsIgnoreCase(taskDispatcherDefinition.getName())
                && version == taskDispatcherDefinition.getVersion());
    }

    @Override
    @SuppressFBWarnings("EI")
    public List<TaskDispatcherDefinition> getTaskDispatcherDefinitions() {
        return taskDispatcherDefinitions;
    }

    @Override
    public List<TaskDispatcherDefinition> getTaskDispatcherDefinitions(String name) {
        return taskDispatcherDefinitions.stream()
            .filter(taskDispatcherDefinition -> Objects.equals(taskDispatcherDefinition.getName(), name))
            .toList();
    }
}
