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

package com.bytechef.hermes.task.dispatcher.registry.domain;

import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.IconUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.hermes.registry.domain.Help;
import com.bytechef.hermes.registry.domain.Resources;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public class TaskDispatcherDefinition {

    private String description;
    private Help help;
    private String icon;
    private String name;
    private Property outputSchema;
    private List<? extends Property> properties;
    private Resources resources;
    private List<? extends Property> taskProperties;
    private String title;
    private List<? extends Property> variableProperties;
    private int version;

    private TaskDispatcherDefinition() {
    }

    public TaskDispatcherDefinition(String name) {
        this.properties = List.of();
        this.name = name;
        this.taskProperties = List.of();
    }

    public TaskDispatcherDefinition(
        com.bytechef.hermes.task.dispatcher.definition.TaskDispatcherDefinition taskDispatcherDefinition) {

        this.description = OptionalUtils.orElse(taskDispatcherDefinition.getDescription(), null);
        this.help = OptionalUtils.mapOrElse(taskDispatcherDefinition.getHelp(), Help::new, null);
        this.icon = OptionalUtils.mapOrElse(taskDispatcherDefinition.getIcon(), IconUtils::readIcon, null);
        this.name = taskDispatcherDefinition.getName();
        this.outputSchema = OptionalUtils.mapOrElse(
            taskDispatcherDefinition.getOutputSchema(), Property::toProperty, null);
        this.properties = CollectionUtils.map(
            OptionalUtils.orElse(taskDispatcherDefinition.getProperties(), List.of()), Property::toProperty);
        this.resources = OptionalUtils.mapOrElse(taskDispatcherDefinition.getResources(), Resources::new, null);
        this.taskProperties = CollectionUtils.map(
            OptionalUtils.orElse(taskDispatcherDefinition.getTaskProperties(), List.of()),
            valueProperty -> (ValueProperty<?>) Property.toProperty(valueProperty));
        this.title = OptionalUtils.orElse(taskDispatcherDefinition.getTitle(), taskDispatcherDefinition.getName());
        this.variableProperties = CollectionUtils.map(
            OptionalUtils.orElse(taskDispatcherDefinition.getVariableProperties(), List.of()),
            valueProperty -> (ValueProperty<?>) Property.toProperty(valueProperty));
        this.version = taskDispatcherDefinition.getVersion();
    }

    public Optional<String> getDescription() {
        return Optional.ofNullable(description);
    }

    public Help getHelp() {
        return help;
    }

    public Optional<String> getIcon() {
        return Optional.ofNullable(icon);
    }

    public String getName() {
        return name;
    }

    public Optional<Property> getOutputSchema() {
        return Optional.ofNullable(outputSchema);
    }

    public List<? extends Property> getProperties() {
        return properties;
    }

    public Optional<Resources> getResources() {
        return Optional.ofNullable(resources);
    }

    public List<? extends Property> getTaskProperties() {
        return taskProperties;
    }

    public String getTitle() {
        return title;
    }

    public List<? extends Property> getVariableProperties() {
        return variableProperties;
    }

    public int getVersion() {
        return version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof TaskDispatcherDefinition that))
            return false;
        return version == that.version && Objects.equals(description, that.description)
            && Objects.equals(help, that.help) && Objects.equals(icon, that.icon) && Objects.equals(name, that.name)
            && Objects.equals(outputSchema, that.outputSchema) && Objects.equals(properties, that.properties)
            && Objects.equals(resources, that.resources) && Objects.equals(taskProperties, that.taskProperties)
            && Objects.equals(title, that.title) && Objects.equals(variableProperties, that.variableProperties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(description, help, icon, name, outputSchema, properties, resources, taskProperties, title,
            variableProperties, version);
    }

    @Override
    public String toString() {
        return "TaskDispatcherDefinition{" +
            "description='" + description + '\'' +
            ", help=" + help +
            ", icon='" + icon + '\'' +
            ", name='" + name + '\'' +
            ", outputSchema=" + outputSchema +
            ", properties=" + properties +
            ", resources=" + resources +
            ", taskProperties=" + taskProperties +
            ", title='" + title + '\'' +
            ", variableProperties=" + variableProperties +
            ", version=" + version +
            '}';
    }
}
