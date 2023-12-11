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

package com.bytechef.hermes.task.dispatcher.definition;

import com.bytechef.hermes.definition.DefinitionDSL;
import com.bytechef.hermes.definition.DefinitionDSL.ModifiableProperty.ModifiableInputProperty;
import com.bytechef.hermes.definition.DefinitionDSL.ModifiableProperty.ModifiableObjectProperty;
import com.bytechef.hermes.definition.DefinitionDSL.ModifiableProperty.ModifiableOutputProperty;
import com.bytechef.hermes.definition.DefinitionDSL.ModifiableProperty.ModifiableValueProperty;
import com.bytechef.hermes.definition.Help;
import com.bytechef.hermes.definition.Property.InputProperty;
import com.bytechef.hermes.definition.Property.OutputProperty;
import com.bytechef.hermes.definition.Property.ValueProperty;
import com.bytechef.hermes.definition.Resources;
import com.bytechef.hermes.task.dispatcher.definition.OutputSchemaDataSource.OutputSchemaFunction;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * @author Ivica Cardic
 */
public final class TaskDispatcherDSL extends DefinitionDSL {

    public static ModifiableTaskDispatcherDefinition taskDispatcher(String name) {
        return new ModifiableTaskDispatcherDefinition(name);
    }

    public static ModifiableObjectProperty task() {
        return task(null);
    }

    public static ModifiableObjectProperty task(String name) {
        return buildObject(name, "The task or task dispatcher to use.", "TASK");
    }

    public static final class ModifiableTaskDispatcherDefinition implements TaskDispatcherDefinition {

        private String description;
        private Help help;
        private String icon;
        private final String name;
        private ModifiableOutputProperty<?> outputSchemaProperty;
        private OutputSchemaFunction outputSchemaFunction;
        private List<? extends ModifiableInputProperty> properties;
        private Resources resources;
        private List<? extends ModifiableValueProperty<?, ?>> taskProperties;
        private String title;
        private List<? extends ModifiableValueProperty<?, ?>> variableProperties;
        private int version = 1;

        private ModifiableTaskDispatcherDefinition(String name) {
            this.name = name;
        }

        public ModifiableTaskDispatcherDefinition description(String description) {
            this.description = description;

            return this;
        }

        public ModifiableTaskDispatcherDefinition help(Help help) {
            this.help = help;

            return this;
        }

        public ModifiableTaskDispatcherDefinition icon(String icon) {
            this.icon = icon;

            return this;
        }

        public <P extends ModifiableOutputProperty<?>> ModifiableTaskDispatcherDefinition outputSchema(
            P property) {

            this.outputSchemaProperty = Objects.requireNonNull(property);

            return this;
        }

        public ModifiableTaskDispatcherDefinition outputSchema(OutputSchemaFunction outputSchema) {
            this.outputSchemaFunction = outputSchema;

            return this;
        }

        public ModifiableTaskDispatcherDefinition resources(String documentationUrl) {
            this.resources = new ResourcesImpl(documentationUrl, null, null);

            return this;
        }

        public ModifiableTaskDispatcherDefinition resources(String documentationUrl, List<String> categories) {
            this.resources = new ResourcesImpl(documentationUrl, null, null);

            return this;
        }

        public ModifiableTaskDispatcherDefinition resources(
            String documentationUrl, List<String> categories, Map<String, String> additionalUrls) {

            this.resources = new ResourcesImpl(documentationUrl, categories, additionalUrls);

            return this;
        }

        @SafeVarargs
        public final <P extends ModifiableInputProperty> ModifiableTaskDispatcherDefinition properties(
            P... properties) {

            if (properties != null) {
                this.properties = List.of(properties);
            }

            return this;
        }

        @SafeVarargs
        public final <P extends ModifiableValueProperty<?, ?>> ModifiableTaskDispatcherDefinition taskProperties(
            P... taskProperties) {

            this.taskProperties = List.of(taskProperties);

            return this;
        }

        public ModifiableTaskDispatcherDefinition title(String title) {
            this.title = title;

            return this;
        }

        public ModifiableTaskDispatcherDefinition version(int version) {
            this.version = version;

            return this;
        }

        @SafeVarargs
        public final <P extends ModifiableValueProperty<?, ?>> ModifiableTaskDispatcherDefinition variableProperties(
            P... variableProperties) {

            this.variableProperties = List.of(variableProperties);

            return this;
        }

        @Override
        public Optional<String> getDescription() {
            return Optional.ofNullable(description);
        }

        @Override
        public Optional<Help> getHelp() {
            return Optional.ofNullable(help);
        }

        @Override
        public Optional<String> getIcon() {
            return Optional.ofNullable(icon);
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Optional<OutputProperty<?>> getOutputSchema() {
            return Optional.ofNullable(outputSchemaProperty);
        }

        @Override
        public Optional<OutputSchemaDataSource> getOutputSchemaDataSource() {
            return Optional.ofNullable(
                outputSchemaFunction == null ? null : new OutputSchemaDataSourceImpl(outputSchemaFunction));
        }

        @Override
        public Optional<List<? extends InputProperty>> getProperties() {
            return Optional.ofNullable(properties);
        }

        @Override
        public Optional<Resources> getResources() {
            return Optional.ofNullable(resources);
        }

        @Override
        public Optional<List<? extends ValueProperty<?>>> getTaskProperties() {
            return Optional.ofNullable(taskProperties);
        }

        @Override
        public Optional<String> getTitle() {
            return Optional.ofNullable(title);
        }

        @Override
        public Optional<List<? extends ValueProperty<?>>> getVariableProperties() {
            return Optional.ofNullable(variableProperties);
        }

        @Override
        public int getVersion() {
            return version;
        }
    }

    private record OutputSchemaDataSourceImpl(OutputSchemaFunction outputSchema) implements OutputSchemaDataSource {

        @Override
        public OutputSchemaFunction getOutputSchema() {
            return outputSchema;
        }
    }
}
