
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

package com.bytechef.hermes.definition.registry.dto;

import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.hermes.definition.registry.util.DefinitionUtils;
import com.bytechef.hermes.task.dispatcher.definition.TaskDispatcherDefinition;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.List;
import java.util.Optional;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record TaskDispatcherDefinitionDTO(
    Optional<String> description, Optional<String> icon, String name, List<? extends PropertyDTO> outputSchema,
    List<? extends PropertyDTO> properties, Optional<ResourcesDTO> resources,
    List<? extends PropertyDTO> taskProperties, String title, int version) {

    public TaskDispatcherDefinitionDTO(String name) {
        this(
            Optional.empty(), Optional.empty(), name, List.of(), List.of(), Optional.empty(), List.of(),
            null, 1);
    }

    public TaskDispatcherDefinitionDTO(TaskDispatcherDefinition taskDispatcherDefinition) {
        this(
            taskDispatcherDefinition.getDescription(), getIcon(taskDispatcherDefinition),
            taskDispatcherDefinition.getName(),
            CollectionUtils.map(
                OptionalUtils.orElse(taskDispatcherDefinition.getOutputSchema(), List.of()),
                PropertyDTO::toPropertyDTO),
            CollectionUtils.map(
                OptionalUtils.orElse(taskDispatcherDefinition.getProperties(), List.of()), PropertyDTO::toPropertyDTO),
            getResources(taskDispatcherDefinition),
            CollectionUtils.map(
                OptionalUtils.orElse(taskDispatcherDefinition.getTaskProperties(), List.of()),
                PropertyDTO::toPropertyDTO),
            getTitle(taskDispatcherDefinition), taskDispatcherDefinition.getVersion());
    }

    private static Optional<String> getIcon(TaskDispatcherDefinition taskDispatcherDefinition) {
        return taskDispatcherDefinition.getIcon()
            .map(DefinitionUtils::readIcon);
    }

    private static Optional<ResourcesDTO> getResources(TaskDispatcherDefinition taskDispatcherDefinition) {
        return taskDispatcherDefinition.getResources()
            .map(ResourcesDTO::new);
    }

    public static String getTitle(TaskDispatcherDefinition taskDispatcherDefinition) {
        return OptionalUtils.orElse(taskDispatcherDefinition.getTitle(), taskDispatcherDefinition.getName());
    }
}
