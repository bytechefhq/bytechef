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
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.commons.util.OptionalUtils;
import edu.umd.cs.findbugs.annotations.Nullable;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public class TaskDispatcherDefinition {

    private String description;
    private Help help;
    private String icon;
    private String name;
    private OutputSchema outputSchema;
    private boolean outputSchemaDataSource;
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
        com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDefinition taskDispatcherDefinition) {

        this.description = OptionalUtils.orElse(taskDispatcherDefinition.getDescription(), null);
        this.help = OptionalUtils.mapOrElse(taskDispatcherDefinition.getHelp(), Help::new, null);
        this.icon = OptionalUtils.mapOrElse(taskDispatcherDefinition.getIcon(), IconUtils::readIcon, null);
        this.name = taskDispatcherDefinition.getName();
        this.outputSchema =
            OptionalUtils.mapOrElse(
                taskDispatcherDefinition.getOutputSchema(), TaskDispatcherDefinition::toOutputSchema, null);
        this.outputSchemaDataSource = OptionalUtils.mapOrElse(
            taskDispatcherDefinition.getOutputSchemaFunction(), outputSchemaDataSource -> true, false);
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof TaskDispatcherDefinition that)) {
            return false;
        }

        return outputSchemaDataSource == that.outputSchemaDataSource && version == that.version
            && Objects.equals(description, that.description) && Objects.equals(help, that.help)
            && Objects.equals(icon, that.icon) && Objects.equals(name, that.name)
            && Objects.equals(outputSchema, that.outputSchema) && Objects.equals(properties, that.properties)
            && Objects.equals(resources, that.resources) && Objects.equals(taskProperties, that.taskProperties)
            && Objects.equals(title, that.title) && Objects.equals(variableProperties, that.variableProperties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            description, help, icon, name, outputSchema, outputSchemaDataSource, properties, resources, taskProperties,
            title, variableProperties, version);
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
    public OutputSchema getOutputSchema() {
        return outputSchema;
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

    public List<? extends Property> getVariableProperties() {
        return variableProperties;
    }

    public int getVersion() {
        return version;
    }

    public boolean isOutputSchemaDataSource() {
        return outputSchemaDataSource;
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

    private static OutputSchema toOutputSchema(
        com.bytechef.platform.workflow.task.dispatcher.definition.OutputSchema outputSchema) {

        Object sampleOutput = outputSchema.sampleOutput();

        if (sampleOutput == null) {
            sampleOutput = getSampleOutput(outputSchema.definition());
        }

        if (sampleOutput instanceof String string) {
            try {
                sampleOutput = JsonUtils.read(string);
            } catch (Exception e) {
                //
            }
        }

        return new OutputSchema(Property.toProperty(outputSchema.definition()), sampleOutput);
    }

    private static Object getSampleOutput(
        com.bytechef.platform.workflow.task.dispatcher.definition.Property.ValueProperty<?> definitionProperty) {

        return switch (definitionProperty) {
            case com.bytechef.platform.workflow.task.dispatcher.definition.Property.ArrayProperty p -> {
                List<Object> items = new ArrayList<>();

                List<? extends com.bytechef.platform.workflow.task.dispatcher.definition.Property.ValueProperty<?>> properties =
                    OptionalUtils.orElse(p.getItems(), List.of());

                if (!properties.isEmpty()) {
                    items.add(getSampleOutput(properties.getFirst()));
                }

                yield items;
            }
            case com.bytechef.platform.workflow.task.dispatcher.definition.Property.BooleanProperty p -> true;
            case com.bytechef.platform.workflow.task.dispatcher.definition.Property.DateProperty p -> LocalDate.now();
            case com.bytechef.platform.workflow.task.dispatcher.definition.Property.DateTimeProperty p ->
                LocalDateTime.now();
            case com.bytechef.platform.workflow.task.dispatcher.definition.Property.FileEntryProperty p ->
                Map.of(
                    "extension", "sampleExtension", "mimeType", "sampleMimeType", "name", "sampleName", "url",
                    "file:///tmp/fileName.txt");
            case com.bytechef.platform.workflow.task.dispatcher.definition.Property.IntegerProperty p -> 57;
            case com.bytechef.platform.workflow.task.dispatcher.definition.Property.NullProperty p -> null;
            case com.bytechef.platform.workflow.task.dispatcher.definition.Property.NumberProperty p -> 23.34;
            case com.bytechef.platform.workflow.task.dispatcher.definition.Property.ObjectProperty p -> {
                Map<String, Object> map = new HashMap<>();

                for (com.bytechef.platform.workflow.task.dispatcher.definition.Property.ValueProperty<?> property : OptionalUtils
                    .orElse(p.getProperties(), List.of())) {

                    map.put(property.getName(), getSampleOutput(property));
                }

                yield map;
            }
            case com.bytechef.platform.workflow.task.dispatcher.definition.Property.StringProperty p ->
                "sample " + p.getName();
            case com.bytechef.platform.workflow.task.dispatcher.definition.Property.TimeProperty p -> LocalTime.now();
            default -> throw new IllegalArgumentException(
                "Definition %s is not allowed".formatted(definitionProperty.getName()));
        };
    }
}
