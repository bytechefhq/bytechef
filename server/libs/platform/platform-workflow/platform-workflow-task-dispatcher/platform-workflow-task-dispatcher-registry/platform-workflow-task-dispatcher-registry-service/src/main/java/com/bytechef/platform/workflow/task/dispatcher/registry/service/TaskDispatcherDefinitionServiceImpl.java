/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.platform.workflow.task.dispatcher.registry.service;

import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.platform.workflow.task.dispatcher.definition.OutputSchemaFunction;
import com.bytechef.platform.workflow.task.dispatcher.registry.TaskDispatcherDefinitionRegistry;
import com.bytechef.platform.workflow.task.dispatcher.registry.domain.OutputSchema;
import com.bytechef.platform.workflow.task.dispatcher.registry.domain.Property;
import com.bytechef.platform.workflow.task.dispatcher.registry.domain.TaskDispatcherDefinition;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

/**
 * @author Ivica Cardic
 */
@Service
public class TaskDispatcherDefinitionServiceImpl implements TaskDispatcherDefinitionService {

    private final TaskDispatcherDefinitionRegistry taskDispatcherDefinitionRegistry;

    @SuppressFBWarnings("EI2")
    public TaskDispatcherDefinitionServiceImpl(TaskDispatcherDefinitionRegistry taskDispatcherDefinitionRegistry) {
        this.taskDispatcherDefinitionRegistry = taskDispatcherDefinitionRegistry;
    }

    @Override
    public OutputSchema executeOutputSchema(
        @NonNull String name, int version, @NonNull Map<String, Object> inputParameters) {

        OutputSchemaFunction outputSchemaFunction = getOutputSchemaFunction(name, version);

        try {
            com.bytechef.platform.workflow.task.dispatcher.definition.OutputSchema outputSchema =
                outputSchemaFunction.apply(inputParameters);

            return new OutputSchema(Property.toProperty(outputSchema.definition()), outputSchema.sampleOutput());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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

    private OutputSchemaFunction getOutputSchemaFunction(String name, int version) {
        com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDefinition taskDispatcherDefinition =
            taskDispatcherDefinitionRegistry.getTaskDispatcherDefinition(name, version);

        return OptionalUtils.get(taskDispatcherDefinition.getOutputSchemaFunction());
    }
}
