/*
 * Copyright 2025 ByteChef
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

package com.bytechef.embedded.integration.definition;

import com.bytechef.workflow.definition.WorkflowDefinition;
import java.util.List;
import java.util.Optional;

/**
 * @author Ivica Cardic
 */
public class IntegrationDsl {

    public static ModifiableIntegrationDefinition integration(String componentName, int componentVersion) {
        return new ModifiableIntegrationDefinition(componentName, componentVersion);
    }

    public static class ModifiableIntegrationDefinition implements IntegrationDefinition {

        private String category;
        private final String componentName;
        private final int componentVersion;
        private String description;
        private boolean multipleInstances;
        private List<String> tags;
        private String version;
        private List<WorkflowDefinition> workflows;

        public ModifiableIntegrationDefinition(String componentName, int componentVersion) {
            this.componentName = componentName;
            this.componentVersion = componentVersion;
            this.version = "0.0.1";
        }

        public ModifiableIntegrationDefinition category(String category) {
            this.category = category;

            return this;
        }

        public ModifiableIntegrationDefinition description(String description) {
            this.description = description;

            return this;
        }

        public ModifiableIntegrationDefinition multipleInstances(boolean multipleInstances) {
            this.multipleInstances = multipleInstances;

            return this;
        }

        public ModifiableIntegrationDefinition tags(String... tags) {
            this.tags = List.of(tags);

            return this;
        }

        public ModifiableIntegrationDefinition version(String version) {
            this.version = version;

            return this;
        }

        public ModifiableIntegrationDefinition workflows(WorkflowDefinition... workflows) {
            this.workflows = List.of(workflows);

            return this;
        }

        @Override
        public Optional<String> getCategory() {
            return Optional.ofNullable(category);
        }

        @Override
        public String getComponentName() {
            return componentName;
        }

        @Override
        public int getComponentVersion() {
            return componentVersion;
        }

        @Override
        public Optional<String> getDescription() {
            return Optional.ofNullable(description);
        }

        @Override
        public boolean isMultipleInstances() {
            return multipleInstances;
        }

        @Override
        public Optional<List<String>> getTags() {
            return Optional.ofNullable(tags);
        }

        @Override
        public String getVersion() {
            return version;
        }

        @Override
        public Optional<List<WorkflowDefinition>> getWorkflows() {
            return Optional.ofNullable(workflows);
        }
    }
}
