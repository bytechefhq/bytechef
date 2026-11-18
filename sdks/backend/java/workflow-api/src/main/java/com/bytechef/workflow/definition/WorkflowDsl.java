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

import java.util.AbstractMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;

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

    public static ModifiableCompositeTaskDefinition parallel(String name) {
        return new ModifiableCompositeTaskDefinition(name, CompositeTaskDefinition.Type.PARALLEL);
    }

    public static ModifiableCompositeTaskDefinition forkJoin(String name) {
        return new ModifiableCompositeTaskDefinition(name, CompositeTaskDefinition.Type.FORK_JOIN);
    }

    public static List<ModifiableTaskDefinition> branch(ModifiableTaskDefinition... tasks) {
        return List.of(tasks);
    }

    public static ModifiableTriggerDefinition trigger(String name, String type) {
        return new ModifiableTriggerDefinition(name, type);
    }

    public static ModifiableInput input(String name) {
        return new ModifiableInput(name);
    }

    public static ModifiableOutput output(String name) {
        return new ModifiableOutput(name);
    }

    public static ModifiableConnectionRequirement connection(String componentName, String name) {
        return new ModifiableConnectionRequirement(componentName, null, name);
    }

    public static ModifiableConnectionRequirement connection(String componentName, int componentVersion, String name) {
        return new ModifiableConnectionRequirement(componentName, componentVersion, name);
    }

    public static ModifiableClusterElements clusterElements() {
        return new ModifiableClusterElements();
    }

    public static ModifiableClusterElement clusterElement(String type) {
        return new ModifiableClusterElement(type);
    }

    public static class ModifiableClusterElements extends AbstractMap<String, Object> {

        private final Map<String, Object> clusterElements = new LinkedHashMap<>();

        private ModifiableClusterElements() {
        }

        public ModifiableClusterElements element(String clusterElementType, ModifiableClusterElement clusterElement) {
            clusterElements.put(clusterElementType, clusterElement);

            return this;
        }

        public ModifiableClusterElements elements(
            String clusterElementType, ModifiableClusterElement... clusterElements) {

            this.clusterElements.put(clusterElementType, List.of(clusterElements));

            return this;
        }

        @Override
        public Set<Entry<String, Object>> entrySet() {
            return clusterElements.entrySet();
        }
    }

    public static class ModifiableClusterElement extends AbstractMap<String, Object> {

        private static final String CONNECTION = "connection";
        private static final String NAME = "name";
        private static final String PARAMETERS = "parameters";
        private static final String TYPE = "type";

        private final Map<String, Object> clusterElement = new LinkedHashMap<>();
        private final Map<String, Object> parameters = new LinkedHashMap<>();

        private ModifiableClusterElement(String type) {
            clusterElement.put(TYPE, type);
        }

        public ModifiableClusterElement connection(String connectionName) {
            clusterElement.put(CONNECTION, connectionName);

            return this;
        }

        public ModifiableClusterElement name(String name) {
            clusterElement.put(NAME, name);

            return this;
        }

        public ModifiableClusterElement parameter(String name, Object value) {
            parameters.put(name, value);

            clusterElement.put(PARAMETERS, parameters);

            return this;
        }

        public ModifiableClusterElement parameters(Map<String, ?> parameters) {
            this.parameters.putAll(parameters);

            clusterElement.put(PARAMETERS, this.parameters);

            return this;
        }

        @Override
        public Set<Entry<String, Object>> entrySet() {
            return clusterElement.entrySet();
        }
    }

    public static class ModifiableConnectionRequirement implements ConnectionRequirement {

        private final String componentName;
        private final Integer componentVersion;
        private final String name;

        private ModifiableConnectionRequirement(String componentName, Integer componentVersion, String name) {
            this.componentName = componentName;
            this.componentVersion = componentVersion;
            this.name = name;
        }

        @Override
        public String getComponentName() {
            return componentName;
        }

        @Override
        public OptionalInt getComponentVersion() {
            return componentVersion == null ? OptionalInt.empty() : OptionalInt.of(componentVersion);
        }

        @Override
        public String getName() {
            return name;
        }

    }

    public static class ModifiableInput implements Input {

        private String label;
        private final String name;
        private boolean required;
        private String type = "STRING";

        private ModifiableInput(String name) {
            this.name = name;
        }

        public ModifiableInput label(String label) {
            this.label = label;

            return this;
        }

        public ModifiableInput required(boolean required) {
            this.required = required;

            return this;
        }

        public ModifiableInput type(String type) {
            this.type = type;

            return this;
        }

        @Override
        public String getLabel() {
            return label;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getType() {
            return type;
        }

        @Override
        public boolean isRequired() {
            return required;
        }

    }

    public static class ModifiableOutput implements Output {

        private final String name;
        private String task;
        private Object value;

        private ModifiableOutput(String name) {
            this.name = name;
        }

        public ModifiableOutput task(String task) {
            this.task = task;

            return this;
        }

        public ModifiableOutput value(Object value) {
            this.value = value;

            return this;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getTask() {
            return task;
        }

        @Override
        public Object getValue() {
            return value;
        }

    }

    public static class ModifiableTaskDefinition implements TaskDefinition {

        private List<ModifiableConnectionRequirement> connections;
        private String description;

        private String label;
        private final String name;
        private Map<String, ?> parameters;
        private PerformFunction performFunction;

        public ModifiableTaskDefinition(String name) {
            this.name = name;
        }

        public ModifiableTaskDefinition connections(ModifiableConnectionRequirement... connections) {
            this.connections = List.of(connections);

            return this;
        }

        public ModifiableTaskDefinition description(String description) {
            this.description = description;

            return this;
        }

        public ModifiableTaskDefinition label(String label) {
            this.label = label;

            return this;
        }

        public ModifiableTaskDefinition parameters(Map<String, ?> parameters) {
            this.parameters = Map.copyOf(parameters);

            return this;
        }

        public ModifiableTaskDefinition perform(PerformFunction perform) {
            this.performFunction = perform;

            return this;
        }

        public ModifiableTaskDefinition perform(ContextPerformFunction perform) {
            this.performFunction = new PerformFunction() {

                @Override
                public Object apply() {
                    return null;
                }

                @Override
                public Object apply(TaskContext context) throws Exception {
                    return perform.apply(context);
                }
            };

            return this;
        }

        @Override
        public Optional<List<? extends ConnectionRequirement>> getConnections() {
            return Optional.ofNullable(connections);
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
        public Optional<Map<String, ?>> getParameters() {
            return Optional.ofNullable(parameters);
        }

        @Override
        public PerformFunction getPerform() {
            return performFunction;
        }

    }

    public static class ModifiableCompositeTaskDefinition implements CompositeTaskDefinition {

        private List<List<ModifiableTaskDefinition>> branches = List.of();
        private String description;
        private String label;
        private final String name;
        private List<ModifiableTaskDefinition> tasks = List.of();
        private final Type type;

        private ModifiableCompositeTaskDefinition(String name, Type type) {
            this.name = name;
            this.type = type;
        }

        @SafeVarargs
        public final ModifiableCompositeTaskDefinition branches(List<ModifiableTaskDefinition>... branches) {
            this.branches = List.of(branches);

            return this;
        }

        public ModifiableCompositeTaskDefinition description(String description) {
            this.description = description;

            return this;
        }

        public ModifiableCompositeTaskDefinition label(String label) {
            this.label = label;

            return this;
        }

        public ModifiableCompositeTaskDefinition tasks(ModifiableTaskDefinition... tasks) {
            this.tasks = List.of(tasks);

            return this;
        }

        @Override
        public List<? extends List<? extends TaskDefinition>> getBranches() {
            return branches;
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
        public List<? extends TaskDefinition> getTasks() {
            return tasks;
        }

        @Override
        public Type getType() {
            return type;
        }

    }

    public static class ModifiableTriggerDefinition implements TriggerDefinition {

        private final String name;

        private Map<String, ?> parameters = Map.of();
        private final String type;

        public ModifiableTriggerDefinition(String name, String type) {
            this.name = name;
            this.type = type;
        }

        public ModifiableTriggerDefinition parameters(Map<String, ?> parameters) {
            this.parameters = Map.copyOf(parameters);

            return this;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Map<String, ?> getParameters() {
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
        private List<WorkflowTaskDefinition> tasks;
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

        public ModifiableWorkflowDefinition tasks(WorkflowTaskDefinition... tasks) {
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
        public Optional<List<? extends WorkflowTaskDefinition>> getTasks() {
            return Optional.ofNullable(tasks);
        }

        @Override
        public Optional<List<? extends TriggerDefinition>> getTriggers() {
            return Optional.ofNullable(triggers);
        }
    }
}
