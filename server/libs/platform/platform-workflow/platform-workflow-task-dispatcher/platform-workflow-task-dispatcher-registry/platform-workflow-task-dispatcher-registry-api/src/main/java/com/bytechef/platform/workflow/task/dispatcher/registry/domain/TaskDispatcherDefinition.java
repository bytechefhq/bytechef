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

package com.bytechef.platform.workflow.task.dispatcher.registry.domain;

import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.IconUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.platform.registry.util.SchemaUtils;
import edu.umd.cs.findbugs.annotations.Nullable;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Objects;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public class TaskDispatcherDefinition {

    private boolean defaultOutput;
    private String description;
    private Help help;
    private String icon;
    private String name;
    private Output output;
    private boolean outputDefined;
    private List<? extends Property> properties;
    private Resources resources;
    private List<? extends Property> taskProperties;
    private String title;
    private ObjectProperty variableProperties;
    private boolean variablePropertiesDefined;
    private int version;

    private TaskDispatcherDefinition() {
    }

    public TaskDispatcherDefinition(String name) {
        this.properties = List.of();
        this.name = name;
        this.taskProperties = List.of();
    }

    public TaskDispatcherDefinition(
        com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDefinition taskDispatcherDefinition) {

        this.defaultOutput = OptionalUtils.mapOrElse(
            taskDispatcherDefinition.getOutputFunction(), outputFunction -> true, false);
        this.description = OptionalUtils.orElse(taskDispatcherDefinition.getDescription(), null);
        this.help = OptionalUtils.mapOrElse(taskDispatcherDefinition.getHelp(), Help::new, null);
        this.icon = OptionalUtils.mapOrElse(taskDispatcherDefinition.getIcon(), IconUtils::readIcon, null);
        this.name = taskDispatcherDefinition.getName();
        this.output = getOutput(taskDispatcherDefinition);
        this.outputDefined = OptionalUtils.mapOrElse(taskDispatcherDefinition.getOutput(), outputSchema -> true, false);
        this.properties = CollectionUtils.map(
            OptionalUtils.orElse(taskDispatcherDefinition.getProperties(), List.of()), Property::toProperty);
        this.resources = OptionalUtils.mapOrElse(taskDispatcherDefinition.getResources(), Resources::new, null);
        this.taskProperties = CollectionUtils.map(
            OptionalUtils.orElse(taskDispatcherDefinition.getTaskProperties(), List.of()),
            valueProperty -> (Property) Property.toProperty(valueProperty));
        this.title = OptionalUtils.orElse(taskDispatcherDefinition.getTitle(), taskDispatcherDefinition.getName());
        this.variableProperties =
            OptionalUtils.mapOrElse(taskDispatcherDefinition.getVariableProperties(), Property::toProperty, null);
        this.variablePropertiesDefined =
            OptionalUtils.mapOrElse(
                taskDispatcherDefinition.getVariableProperties(), variableProperties -> true, false) ||
                OptionalUtils.mapOrElse(
                    taskDispatcherDefinition.getOutputFunction(), outputSchemaDataSource -> true, false);
        this.version = taskDispatcherDefinition.getVersion();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof TaskDispatcherDefinition that)) {
            return false;
        }

        return defaultOutput == that.defaultOutput && Objects.equals(description, that.description) &&
            Objects.equals(help, that.help) && Objects.equals(icon, that.icon) && Objects.equals(name, that.name) &&
            Objects.equals(output, that.output) && outputDefined == that.outputDefined &&
            Objects.equals(properties, that.properties) && Objects.equals(resources, that.resources) &&
            Objects.equals(taskProperties, that.taskProperties) && Objects.equals(title, that.title) &&
            Objects.equals(variableProperties, that.variableProperties) &&
            Objects.equals(variablePropertiesDefined, that.variablePropertiesDefined) && version == that.version;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            defaultOutput, description, help, icon, name, output, outputDefined, properties, resources, taskProperties,
            title, variableProperties, variablePropertiesDefined, version);
    }

    @Nullable
    public String getDescription() {
        return description;
    }

    @Nullable
    public Help getHelp() {
        return help;
    }

    @Nullable
    public String getIcon() {
        return icon;
    }

    public String getName() {
        return name;
    }

    @Nullable
    public Output getOutput() {
        return output;
    }

    public List<? extends Property> getProperties() {
        return properties;
    }

    @Nullable
    public Resources getResources() {
        return resources;
    }

    public List<? extends Property> getTaskProperties() {
        return taskProperties;
    }

    public String getTitle() {
        return title;
    }

    public ObjectProperty getVariableProperties() {
        return variableProperties;
    }

    public int getVersion() {
        return version;
    }

    public boolean isDynamicOutput() {
        return defaultOutput;
    }

    public boolean isOutputDefined() {
        return outputDefined;
    }

    public boolean isVariablePropertiesDefined() {
        return variablePropertiesDefined;
    }

    private static Output getOutput(
        com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDefinition taskDispatcherDefinition) {

        return OptionalUtils.mapOrElse(
            taskDispatcherDefinition.getOutput(),
            output -> SchemaUtils.toOutput(
                output,
                (property, sampleOutput) -> new Output(
                    Property.toProperty(
                        (com.bytechef.platform.workflow.task.dispatcher.definition.Property) property),
                    sampleOutput)),
            null);
    }

    @Override
    public String toString() {
        return "TaskDispatcherDefinition{" +
            "description='" + description + '\'' +
            ", help=" + help +
            ", icon='" + icon + '\'' +
            ", name='" + name + '\'' +
            ", properties=" + properties +
            ", output=" + output +
            ", outputDefined=" + outputDefined +
            ", outputFunctionDefined=" + defaultOutput +
            ", resources=" + resources +
            ", taskProperties=" + taskProperties +
            ", title='" + title + '\'' +
            ", variablePropertiesDefined=" + variablePropertiesDefined +
            ", variableProperties=" + variableProperties +
            ", version=" + version +
            '}';
    }
}
