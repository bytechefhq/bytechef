
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

package com.bytechef.hermes.task.dispatcher.definition;

import com.bytechef.hermes.definition.DefinitionDSL;
import com.bytechef.hermes.definition.DefinitionDSL.ModifiableProperty.ModifiableObjectProperty;
import com.bytechef.hermes.definition.Property;
import com.bytechef.hermes.definition.Property.InputProperty;
import com.bytechef.hermes.definition.Property.OutputProperty;
import com.bytechef.hermes.definition.Property.ValueProperty;
import com.bytechef.hermes.definition.Resources;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

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

    private static <P extends InputProperty> List<P> checkInputProperties(P[] properties) {
        if (properties != null) {
            for (Property property : properties) {
                String name = property.getName();

                if (name == null || name.isEmpty()) {
                    throw new IllegalArgumentException("Defined properties cannot to have empty names.");
                }
            }

            return Stream.of(properties)
                .distinct()
                .toList();
        }

        return null;
    }

    public static final class ModifiableTaskDispatcherDefinition implements TaskDispatcherDefinition {

        private String description;
        private String icon;
        private final String name;
        private List<? extends OutputProperty<?>> outputSchemaProperties;
        private List<? extends InputProperty> properties;
        private Resources resources;
        private List<? extends ValueProperty<?>> taskProperties;
        private String title;
        private int version = 1;

        private ModifiableTaskDispatcherDefinition(String name) {
            this.name = name;
        }

        public ModifiableTaskDispatcherDefinition description(String description) {
            this.description = description;

            return this;
        }

        public ModifiableTaskDispatcherDefinition icon(String icon) {
            this.icon = icon;

            return this;
        }

        @SafeVarargs
        public final <P extends OutputProperty<?>> ModifiableTaskDispatcherDefinition outputSchema(P... properties) {
            this.outputSchemaProperties = checkPropertyNames(properties == null ? List.of() : List.of(properties));

            return this;
        }

        public ModifiableTaskDispatcherDefinition resources(String documentationUrl) {
            this.resources = new ResourcesImpl(null, null, documentationUrl);

            return this;
        }

        public ModifiableTaskDispatcherDefinition resources(String documentationUrl, List<String> categories) {
            this.resources = new ResourcesImpl(null, null, documentationUrl);

            return this;
        }

        public ModifiableTaskDispatcherDefinition resources(
            String documentationUrl, List<String> categories, Map<String, String> additionalUrls) {

            this.resources = new ResourcesImpl(null, null, documentationUrl);

            return this;
        }

        @SafeVarargs
        public final <P extends InputProperty> ModifiableTaskDispatcherDefinition properties(P... properties) {
            this.properties = checkInputProperties(properties);

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
        public final <P extends ValueProperty<?>> ModifiableTaskDispatcherDefinition taskProperties(
            P... taskProperties) {

            this.taskProperties = List.of(taskProperties);

            return this;
        }

        @Override
        public Optional<String> getDescription() {
            return Optional.ofNullable(description);
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
        public Optional<List<? extends OutputProperty<?>>> getOutputSchema() {
            return Optional.ofNullable(outputSchemaProperties);
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
        public Optional<String> getTitle() {
            return Optional.ofNullable(title);
        }

        @Override
        public int getVersion() {
            return version;
        }

        @Override
        public Optional<List<? extends ValueProperty<?>>> getTaskProperties() {
            return Optional.ofNullable(taskProperties);
        }
    }
}
