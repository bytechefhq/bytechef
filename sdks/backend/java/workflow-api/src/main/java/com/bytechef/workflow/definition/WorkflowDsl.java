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

package com.bytechef.workflow.definition;

import java.util.List;
import java.util.Optional;

/**
 * @author Ivica Cardic
 */
public class WorkflowDsl {

    public static ModifiableWorkflowDefinition workflow(String name) {
        return new ModifiableWorkflowDefinition(name);
    }

    public static ModifiableTaskDefinition task(String name) {
        return new ModifiableTaskDefinition(name);
    }

    public static ModifiableTriggerDefinition trigger(String name, String type) {
        return new ModifiableTriggerDefinition(name, type);
    }

    public static ModifiableInput input() {
        return new ModifiableInput();
    }

    public static ModifiableOutput output() {
        return new ModifiableOutput();
    }

    public static class ModifiableInput implements Input {

    }

    public static class ModifiableOutput implements Output {

    }

    public static class ModifiableParameter implements Parameter {

    }

    public static class ModifiableTaskDefinition implements TaskDefinition {

        private String description;

        private String label;
        private final String name;
        private List<ModifiableParameter> parameters;
        private PerformFunction performFunction;

        public ModifiableTaskDefinition(String name) {
            this.name = name;
        }

        public ModifiableTaskDefinition description(String description) {
            this.description = description;

            return this;
        }

        public ModifiableTaskDefinition label(String label) {
            this.label = label;

            return this;
        }

        public ModifiableTaskDefinition parameters(ModifiableParameter... parameters) {
            this.parameters = List.of(parameters);

            return this;
        }

        public ModifiableTaskDefinition perform(PerformFunction perform) {
            this.performFunction = perform;

            return this;
        }

        @Override
        public Optional<String> getDescription() {
            return Optional.ofNullable(description);
        }

        @Override
        public Optional<String> getLabel() {
            return Optional.ofNullable(label);
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Optional<List<? extends Parameter>> getParameters() {
            return Optional.ofNullable(parameters);
        }

        @Override
        public PerformFunction getPerform() {
            return performFunction;
        }

    }

    public static class ModifiableTriggerDefinition implements TriggerDefinition {

        private final String name;

        private List<ModifiableParameter> parameters;
        private final String type;

        public ModifiableTriggerDefinition(String name, String type) {
            this.name = name;
            this.type = type;
        }

        public ModifiableTriggerDefinition parameters(ModifiableParameter... parameters) {
            this.parameters = List.of(parameters);

            return this;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public List<? extends Parameter> getParameters() {
            return parameters;
        }

        @Override
        public String getType() {
            return type;
        }

    }

    public static class ModifiableWorkflowDefinition implements WorkflowDefinition {

        private String description;
        private String label;
        private List<Input> inputs;
        private final String name;
        private List<Output> outputs;
        private List<TaskDefinition> tasks;
        private List<TriggerDefinition> triggers;

        public ModifiableWorkflowDefinition(String name) {
            this.name = name;
        }

        public ModifiableWorkflowDefinition description(String description) {
            this.description = description;

            return this;
        }

        public ModifiableWorkflowDefinition inputs(ModifiableInput... inputs) {
            this.inputs = List.of(inputs);

            return this;
        }

        public ModifiableWorkflowDefinition label(String label) {
            this.label = label;

            return this;
        }

        public ModifiableWorkflowDefinition outputs(ModifiableOutput... outputs) {
            this.outputs = List.of(outputs);

            return this;
        }

        public ModifiableWorkflowDefinition tasks(ModifiableTaskDefinition... tasks) {
            this.tasks = List.of(tasks);

            return this;
        }

        public ModifiableWorkflowDefinition triggers(ModifiableTriggerDefinition... triggers) {
            this.triggers = List.of(triggers);

            return this;
        }

        @Override
        public Optional<String> getDescription() {
            return Optional.ofNullable(description);
        }

        @Override
        public Optional<List<? extends Input>> getInputs() {
            return Optional.ofNullable(inputs);
        }

        @Override
        public Optional<String> getLabel() {
            return Optional.ofNullable(label);
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Optional<List<? extends Output>> getOutputs() {
            return Optional.ofNullable(outputs);
        }

        @Override
        public Optional<List<? extends TaskDefinition>> getTasks() {
            return Optional.ofNullable(tasks);
        }

        @Override
        public Optional<List<? extends TriggerDefinition>> getTriggers() {
            return Optional.ofNullable(triggers);
        }
    }
}
