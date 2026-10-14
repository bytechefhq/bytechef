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

package com.bytechef.automation.project.definition;

import com.bytechef.workflow.definition.WorkflowDefinition;
import java.util.List;
import java.util.Optional;

/**
 * @author Ivica Cardic
 */
public class ProjectDsl {

    public static ModifiableProjectDefinition project(String name) {
        return new ModifiableProjectDefinition(name);
    }

    public static class ModifiableProjectDefinition implements ProjectDefinition {

        private String category;
        private String description;
        private final String name;
        private List<String> tags;
        private String version;
        private List<WorkflowDefinition> workflows;

        public ModifiableProjectDefinition(String name) {
            this.name = name;
            this.version = "0.0.1";
        }

        public ModifiableProjectDefinition category(String category) {
            this.category = category;

            return this;
        }

        public ModifiableProjectDefinition description(String description) {
            this.description = description;

            return this;
        }

        public ModifiableProjectDefinition tags(String... tags) {
            this.tags = List.of(tags);

            return this;
        }

        public ModifiableProjectDefinition version(String version) {
            this.version = version;

            return this;
        }

        public ModifiableProjectDefinition workflows(WorkflowDefinition... workflows) {
            this.workflows = List.of(workflows);

            return this;
        }

        @Override
        public Optional<String> getCategory() {
            return Optional.ofNullable(category);
        }

        @Override
        public Optional<String> getDescription() {
            return Optional.ofNullable(description);
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public List<WorkflowDefinition> getWorkflows() {
            return workflows;
        }

        @Override
        public Optional<List<String>> getTags() {
            return Optional.ofNullable(tags);
        }

        @Override
        public String getVersion() {
            return version;
        }
    }
}
