
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

import com.bytechef.hermes.definition.registry.domain.TaskDispatcherDefinition;
import com.bytechef.hermes.definition.registry.task.dispatcher.TaskDispatcherDefinitionRegistry;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.List;

/**
 * @author Ivica Cardic
 */
public class TaskDispatcherDefinitionServiceImpl implements TaskDispatcherDefinitionService {

    private final TaskDispatcherDefinitionRegistry taskDispatcherDefinitionRegistry;

    @SuppressFBWarnings("EI2")
    public TaskDispatcherDefinitionServiceImpl(TaskDispatcherDefinitionRegistry taskDispatcherDefinitionRegistry) {
        this.taskDispatcherDefinitionRegistry = taskDispatcherDefinitionRegistry;
    }

    @Override
    public TaskDispatcherDefinition getTaskDispatcherDefinition(String name, Integer version) {
        return new TaskDispatcherDefinition(
            taskDispatcherDefinitionRegistry.getTaskDispatcherDefinition(name, version));
    }

    @Override
    public List<TaskDispatcherDefinition> getTaskDispatcherDefinitions() {
        return taskDispatcherDefinitionRegistry.getTaskDispatcherDefinitions()
            .stream()
            .map(TaskDispatcherDefinition::new)
            .toList();
    }

    @Override
    public List<TaskDispatcherDefinition> getTaskDispatcherDefinitionVersions(String name) {
        return taskDispatcherDefinitionRegistry.getTaskDispatcherDefinitions(name)
            .stream()
            .map(TaskDispatcherDefinition::new)
            .toList();
    }
}
