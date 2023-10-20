
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
import com.bytechef.hermes.definition.Property;
import com.bytechef.hermes.definition.Resources;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.bytechef.hermes.task.dispatcher.constant.TaskDispatcherConstants.Versions.VERSION_1;

/**
 * @author Ivica Cardic
 */
public final class TaskDispatcherDSL extends DefinitionDSL {

    public static ModifiableTaskDispatcherDefinition taskDispatcher(String name) {
        return new ModifiableTaskDispatcherDefinition(name);
    }

    public static ModifiableProperty.ModifiableObjectProperty task() {
        return task(null);
    }

    public static ModifiableProperty.ModifiableObjectProperty task(String name) {
        return buildObject(name, "The task or task dispatcher to use.", "TASK");
    }

    public static final class ModifiableTaskDispatcherDefinition implements TaskDispatcherDefinition {

        private String description;
        private String icon;
        private String name;
        private List<Property<? extends Property<?>>> outputSchema;
        private List<Property<?>> properties;
        private Resources resources;
        private List<Property<?>> taskProperties;
        private String title;
        private int version = VERSION_1;

        private ModifiableTaskDispatcherDefinition() {
        }

        private ModifiableTaskDispatcherDefinition(String name) {
            this.name = name;
        }

        public ModifiableTaskDispatcherDefinition description(String description) {
            this.description = description;

            return this;
        }

        public ModifiableTaskDispatcherDefinition display(ModifiableResources resources) {
            this.resources = resources;

            return this;
        }

        public ModifiableTaskDispatcherDefinition icon(String icon) {
            this.icon = icon;

            return this;
        }

        public <P extends Property<?>> ModifiableTaskDispatcherDefinition outputSchema(P... outputSchema) {
            if (outputSchema != null) {
                this.outputSchema = List.of(outputSchema);
            }

            return this;
        }

        public <P extends Property<?>> ModifiableTaskDispatcherDefinition properties(P... properties) {
            if (properties != null) {
                this.properties = List.of(properties);
            }

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

        public <P extends Property<?>> ModifiableTaskDispatcherDefinition taskProperties(P... taskProperties) {
            this.taskProperties = List.of(taskProperties);

            return this;
        }

        @Override
        public Optional<String> getDescription() {
            return Optional.ofNullable(description);
        }

        @Override
        public String getIcon() {
            return icon;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public List<Property<? extends Property<?>>> getOutputSchema() {
            return outputSchema;
        }

        @Override
        public List<Property<?>> getProperties() {
            return properties;
        }

        @Override
        @SuppressFBWarnings("EI")
        public Resources getResources() {
            return resources;
        }

        @Override
        public String getTitle() {
            return Objects.requireNonNullElseGet(title, () -> name);
        }

        @Override
        public int getVersion() {
            return version;
        }

        @Override
        public List<Property<?>> getTaskProperties() {
            return taskProperties;
        }
    }
}
