
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
import com.bytechef.commons.util.IconUtils;
import com.bytechef.hermes.task.dispatcher.definition.TaskDispatcherDefinition;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public class TaskDispatcherDefinitionDTO {

    private final String description;
    private final String icon;
    private final String name;
    private final PropertyDTO outputSchema;
    private final List<? extends PropertyDTO> properties;
    private final ResourcesDTO resources;
    private final List<? extends PropertyDTO> taskProperties;
    private final String title;
    private final int version;

    public TaskDispatcherDefinitionDTO(String name) {
        this.description = null;
        this.icon = null;
        this.properties = List.of();
        this.resources = null;
        this.name = name;
        this.outputSchema = null;
        this.taskProperties = List.of();
        this.title = null;
        this.version = 0;
    }

    public TaskDispatcherDefinitionDTO(TaskDispatcherDefinition taskDispatcherDefinition) {
        this.description = OptionalUtils.orElse(taskDispatcherDefinition.getDescription(), null);
        this.icon = OptionalUtils.mapOrElse(taskDispatcherDefinition.getIcon(), IconUtils::readIcon, null);
        this.name = taskDispatcherDefinition.getName();
        this.outputSchema = OptionalUtils.mapOrElse(
            taskDispatcherDefinition.getOutputSchema(), PropertyDTO::toPropertyDTO, null);
        this.properties = CollectionUtils.map(
            OptionalUtils.orElse(taskDispatcherDefinition.getProperties(), List.of()), PropertyDTO::toPropertyDTO);
        this.resources = OptionalUtils.mapOrElse(taskDispatcherDefinition.getResources(), ResourcesDTO::new, null);
        this.taskProperties = CollectionUtils.map(
            OptionalUtils.orElse(taskDispatcherDefinition.getTaskProperties(), List.of()),
            valueProperty -> (ValuePropertyDTO<?>) PropertyDTO.toPropertyDTO(valueProperty));
        this.title = OptionalUtils.orElse(taskDispatcherDefinition.getTitle(), taskDispatcherDefinition.getName());
        this.version = taskDispatcherDefinition.getVersion();
    }

    public Optional<String> getDescription() {
        return Optional.ofNullable(description);
    }

    public Optional<String> getIcon() {
        return Optional.ofNullable(icon);
    }

    public String getName() {
        return name;
    }

    public Optional<PropertyDTO> getOutputSchema() {
        return Optional.ofNullable(outputSchema);
    }

    public List<? extends PropertyDTO> getProperties() {
        return properties;
    }

    public Optional<ResourcesDTO> getResources() {
        return Optional.ofNullable(resources);
    }

    public List<? extends PropertyDTO> getTaskProperties() {
        return taskProperties;
    }

    public String getTitle() {
        return title;
    }

    public int getVersion() {
        return version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof TaskDispatcherDefinitionDTO that))
            return false;
        return version == that.version && Objects.equals(description, that.description)
            && Objects.equals(icon, that.icon) && Objects.equals(name, that.name)
            && Objects.equals(outputSchema, that.outputSchema) && Objects.equals(properties, that.properties)
            && Objects.equals(resources, that.resources) && Objects.equals(taskProperties, that.taskProperties)
            && Objects.equals(title, that.title);
    }

    @Override
    public int hashCode() {
        return Objects.hash(description, icon, name, outputSchema, properties, resources, taskProperties, title,
            version);
    }

    @Override
    public String toString() {
        return "TaskDispatcherDefinitionDTO{" +
            "description='" + description + '\'' +
            ", icon='" + icon + '\'' +
            ", name='" + name + '\'' +
            ", outputSchema=" + outputSchema +
            ", properties=" + properties +
            ", resources=" + resources +
            ", taskProperties=" + taskProperties +
            ", title='" + title + '\'' +
            ", version=" + version +
            '}';
    }
}
