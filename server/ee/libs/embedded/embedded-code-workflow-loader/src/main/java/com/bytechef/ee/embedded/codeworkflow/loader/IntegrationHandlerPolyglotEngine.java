/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.codeworkflow.loader;

import com.bytechef.embedded.integration.IntegrationHandler;
import com.bytechef.embedded.integration.definition.IntegrationDefinition;
import com.bytechef.platform.component.polyglot.ComponentActionInvoker;
import com.bytechef.platform.component.polyglot.ComponentCatalog;
import com.bytechef.platform.component.polyglot.ComponentProxyObject;
import com.bytechef.platform.component.polyglot.PolyglotSandbox;
import com.bytechef.platform.component.polyglot.PolyglotValues;
import com.bytechef.workflow.definition.CompositeTaskDefinition;
import com.bytechef.workflow.definition.ConnectionRequirement;
import com.bytechef.workflow.definition.Input;
import com.bytechef.workflow.definition.Output;
import com.bytechef.workflow.definition.TaskContext;
import com.bytechef.workflow.definition.TaskDefinition;
import com.bytechef.workflow.definition.TriggerDefinition;
import com.bytechef.workflow.definition.WorkflowDefinition;
import com.bytechef.workflow.definition.WorkflowTaskDefinition;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.Function;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class IntegrationHandlerPolyglotEngine {

    static IntegrationHandler load(String languageId, String script) {
        return PolyglotSandbox.call(languageId, polyglotContext -> {
            Value value = polyglotContext.eval(languageId, script);

            String componentName = Objects.requireNonNull(getMember(value, "componentName"));

            Value componentVersionValue = getValue(value, "componentVersion");

            int componentVersion = componentVersionValue.asInt();

            String description = getMember(value, "description");
            String version = getMember(value, "version");

            List<WorkflowDefinition> workflows = getWorkflows(value)
                .stream()
                .map(workflow -> (WorkflowDefinition) new PolyglotWorkflowDefinition(
                    (String) workflow.get("name"), (String) workflow.get("label"),
                    (String) workflow.get("description"),
                    toTaskDefinitions(
                        (String) workflow.get("name"), (List<?>) workflow.get("tasks"), languageId, script),
                    toInputs(workflow.get("inputs")), toOutputs(workflow.get("outputs")),
                    toTriggers(workflow.get("triggers"))))
                .toList();

            return () -> new PolyglotIntegrationDefinition(
                componentName, componentVersion, description, version, workflows);
        });
    }

    @SuppressWarnings("unchecked")
    private static Object executePerform(
        String workflowName, String taskName, String languageId, String script, TaskContext taskContext) {

        return PolyglotSandbox.call(languageId, polyglotContext -> {
            Value value = polyglotContext.eval(languageId, script);

            List<Map<String, Object>> workflows = getWorkflows(value);

            List<Map<String, Object>> tasks = (List<Map<String, Object>>) workflows.stream()
                .filter(workflow -> workflowName.equals(workflow.get("name")))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Workflow name=%s not found".formatted(workflowName)))
                .get("tasks");

            Map<String, Object> task = findTask(tasks, taskName);

            if (task == null) {
                throw new IllegalArgumentException("Task name=%s not found".formatted(taskName));
            }

            Function<Object[], Object> perform = (Function<Object[], Object>) task.get("perform");

            Object result = perform.apply(new Object[] {
                toGuestContext(taskContext, languageId)
            });

            return PolyglotValues.copyFromPolyglotContext(result);
        });
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> findTask(List<?> tasks, String taskName) {
        for (Object entry : tasks) {
            Map<String, Object> task = (Map<String, Object>) entry;

            if (taskName.equals(task.get("name")) && task.get("perform") != null) {
                return task;
            }

            if (task.get("tasks") instanceof List<?> nestedTasks) {
                Map<String, Object> nestedTask = findTask(nestedTasks, taskName);

                if (nestedTask != null) {
                    return nestedTask;
                }
            }

            if (task.get("branches") instanceof List<?> branches) {
                for (Object branch : branches) {
                    if (branch instanceof List<?> branchTasks) {
                        Map<String, Object> branchTask = findTask(branchTasks, taskName);

                        if (branchTask != null) {
                            return branchTask;
                        }
                    }
                }
            }
        }

        return null;
    }

    private static ProxyObject toGuestContext(TaskContext taskContext, String languageId) {
        ComponentActionInvoker componentActionInvoker =
            (componentName, actionName, input, connectionName, clusterElements) -> {
                if (taskContext == null) {
                    throw new IllegalStateException("A TaskContext is not available");
                }

                return taskContext.component(componentName, actionName, input, connectionName, clusterElements);
            };

        ComponentCatalog componentCatalog = new ComponentCatalog() {

            @Override
            public boolean hasComponent(String name) {
                return true;
            }

            @Override
            public boolean hasAction(String componentName, String actionName) {
                return true;
            }
        };

        return ProxyObject.fromMap(
            Map.of(
                "component", new ComponentProxyObject(languageId, componentActionInvoker, componentCatalog),
                "input", (ProxyExecutable) arguments -> {
                    if (taskContext == null) {
                        throw new IllegalStateException("A TaskContext is not available");
                    }

                    if (arguments.length == 0) {
                        return PolyglotValues.copyToGuestValue(taskContext.input(), languageId);
                    }

                    return PolyglotValues.copyToGuestValue(taskContext.input(arguments[0].asString()), languageId);
                },
                "parameters", (ProxyExecutable) arguments -> {
                    if (taskContext == null) {
                        throw new IllegalStateException("A TaskContext is not available");
                    }

                    return PolyglotValues.copyToGuestValue(taskContext.parameters(), languageId);
                },
                "connection", (ProxyExecutable) arguments -> {
                    if (taskContext == null) {
                        throw new IllegalStateException("A TaskContext is not available");
                    }

                    try {
                        return PolyglotValues.copyToGuestValue(
                            taskContext.connection(arguments[0].asString()), languageId);
                    } catch (RuntimeException e) {
                        throw e;
                    } catch (Exception e) {
                        throw new IllegalStateException(e);
                    }
                },
                "log", (ProxyExecutable) arguments -> {
                    if (taskContext == null) {
                        throw new IllegalStateException("A TaskContext is not available");
                    }

                    taskContext.log(TaskContext.LogLevel.of(arguments[0].asString()), arguments[1].asString());

                    return null;
                }));
    }

    private static String getMember(Value value, String name) {
        value = getValue(value, name);

        return value == null ? null : value.as(String.class);
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> getWorkflows(Value value) {
        Value workflows = getValue(value, "workflows");

        if (workflows == null || workflows.isNull()) {
            return List.of();
        }

        return (List<Map<String, Object>>) PolyglotValues.copyToJavaValue(workflows);
    }

    /**
     * Reads a top-level key from a hash (a Python dict) or, failing that, a member (a JavaScript object, a Python
     * object's attribute).
     */
    private static Value getValue(Value value, String name) {
        if (value.hasHashEntries()) {
            return value.hasHashEntry(name) ? value.getHashValue(name) : null;
        }

        return value.getMember(name);
    }

    private static List<WorkflowTaskDefinition> toTaskDefinitions(
        String workflowName, List<?> tasks, String languageId, String script) {

        if (tasks == null) {
            return List.of();
        }

        List<WorkflowTaskDefinition> taskDefinitions = new ArrayList<>();
        Set<String> names = new HashSet<>();

        for (Object entry : tasks) {
            Map<?, ?> task = (Map<?, ?>) entry;

            Object type = task.get("type");

            if (type == null) {
                taskDefinitions.add(toLeafTaskDefinition(workflowName, task, languageId, script, names));
            } else {
                taskDefinitions.add(
                    toCompositeTaskDefinition(workflowName, task, String.valueOf(type), languageId, script, names));
            }
        }

        return taskDefinitions;
    }

    private static CompositeTaskDefinition toCompositeTaskDefinition(
        String workflowName, Map<?, ?> task, String type, String languageId, String script, Set<String> names) {

        String name = (String) task.get("name");

        addName(names, name);

        if (task.get("perform") != null) {
            throw new IllegalArgumentException(
                "Task %s groups other tasks, so it cannot declare a perform of its own".formatted(name));
        }

        CompositeTaskDefinition.Type compositeType = toCompositeType(name, type);

        List<TaskDefinition> tasks = new ArrayList<>();
        List<List<TaskDefinition>> branches = new ArrayList<>();

        if (compositeType == CompositeTaskDefinition.Type.PARALLEL) {
            Object nestedTasks = task.get("tasks");

            if (!(nestedTasks instanceof List<?> nestedTaskList) || nestedTaskList.isEmpty()) {
                throw new IllegalArgumentException("Parallel task %s must declare a non-empty tasks list"
                    .formatted(name));
            }

            for (Object nestedTask : nestedTaskList) {
                tasks.add(toNestedTaskDefinition(workflowName, nestedTask, languageId, script, names));
            }
        } else {
            Object declaredBranches = task.get("branches");

            if (!(declaredBranches instanceof List<?> branchList) || branchList.isEmpty()) {
                throw new IllegalArgumentException("Fork/join task %s must declare a non-empty branches list"
                    .formatted(name));
            }

            for (Object branch : branchList) {
                if (!(branch instanceof List<?> branchTasks) || branchTasks.isEmpty()) {
                    throw new IllegalArgumentException(
                        "Each branch of fork/join task %s must be a non-empty list of tasks".formatted(name));
                }

                List<TaskDefinition> branchTaskDefinitions = new ArrayList<>();

                for (Object branchTask : branchTasks) {
                    branchTaskDefinitions.add(
                        toNestedTaskDefinition(workflowName, branchTask, languageId, script, names));
                }

                branches.add(branchTaskDefinitions);
            }
        }

        return new PolyglotCompositeTaskDefinition(
            name, (String) task.get("label"), (String) task.get("description"), compositeType, tasks, branches);
    }

    private static CompositeTaskDefinition.Type toCompositeType(String name, String type) {
        String normalizedType = type.replace("-", "")
            .replace("_", "");

        if ("parallel".equalsIgnoreCase(normalizedType)) {
            return CompositeTaskDefinition.Type.PARALLEL;
        }

        if ("forkjoin".equalsIgnoreCase(normalizedType)) {
            return CompositeTaskDefinition.Type.FORK_JOIN;
        }

        throw new IllegalArgumentException(
            "Task %s declares type %s; a task's type may only be parallel or forkJoin".formatted(name, type));
    }

    private static TaskDefinition toNestedTaskDefinition(
        String workflowName, Object nestedTask, String languageId, String script, Set<String> names) {

        Map<?, ?> task = (Map<?, ?>) nestedTask;

        if (task.get("type") != null) {
            throw new IllegalArgumentException(
                "Task %s is nested inside a group, and a group inside a group is not supported"
                    .formatted(task.get("name")));
        }

        return toLeafTaskDefinition(workflowName, task, languageId, script, names);
    }

    private static TaskDefinition toLeafTaskDefinition(
        String workflowName, Map<?, ?> task, String languageId, String script, Set<String> names) {

        String name = (String) task.get("name");

        addName(names, name);

        Object parameters = task.get("parameters");

        return new PolyglotTaskDefinition(
            workflowName, name, (String) task.get("label"), (String) task.get("description"),
            toConnectionRequirements(task.get("connections")),
            parameters instanceof Map<?, ?> parameterMap
                ? (Map<String, ?>) PolyglotValues.copyFromPolyglotContext(parameterMap) : null,
            languageId, script);
    }

    private static void addName(Set<String> names, String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Every task must declare a name");
        }

        if (!names.add(name)) {
            throw new IllegalArgumentException("Task name %s is declared more than once".formatted(name));
        }
    }

    private static List<ConnectionRequirement> toConnectionRequirements(Object connections) {
        if (connections == null) {
            return null;
        }

        if (connections instanceof List<?> connectionList) {
            return connectionList.stream()
                .map(connection -> (Map<?, ?>) connection)
                .map(
                    connection -> toConnectionRequirement(
                        (String) connection.get("name"), connection.get("componentName"),
                        connection.get("componentVersion")))
                .toList();
        }

        if (connections instanceof Map<?, ?> connectionMap) {
            List<ConnectionRequirement> connectionRequirements = new ArrayList<>();

            for (Map.Entry<?, ?> entry : connectionMap.entrySet()) {
                Map<?, ?> connection = (Map<?, ?>) entry.getValue();

                connectionRequirements.add(
                    toConnectionRequirement(
                        String.valueOf(entry.getKey()), connection.get("componentName"),
                        connection.get("componentVersion")));
            }

            return connectionRequirements;
        }

        throw new IllegalArgumentException(
            "A task's connections must be a list or a map keyed by connection name, got: " + connections);
    }

    private static ConnectionRequirement toConnectionRequirement(
        String name, Object componentName, Object componentVersion) {

        return new PolyglotConnectionRequirement(
            (String) componentName, componentVersion instanceof Number number ? number.intValue() : null, name);
    }

    private record PolyglotConnectionRequirement(String componentName, Integer componentVersion, String name)
        implements ConnectionRequirement {

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

    @SuppressFBWarnings("EI")
    private record PolyglotCompositeTaskDefinition(
        String name, String label, String description, Type type, List<TaskDefinition> tasks,
        List<List<TaskDefinition>> branches)
        implements CompositeTaskDefinition {

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

    @SuppressFBWarnings("EI")
    private record PolyglotTaskDefinition(
        String workflowName, String name, String label, String description,
        List<ConnectionRequirement> connections, Map<String, ?> parameters, String languageId, String script)
        implements TaskDefinition {

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
            return new PerformFunction() {

                @Override
                public Object apply() {
                    return executePerform(workflowName, name, languageId, script, null);
                }

                @Override
                public Object apply(TaskContext taskContext) {
                    return executePerform(workflowName, name, languageId, script, taskContext);
                }
            };
        }
    }

    private record PolyglotIntegrationDefinition(
        String componentName, int componentVersion, String description, String version,
        List<WorkflowDefinition> workflows)
        implements IntegrationDefinition {

        @Override
        public Optional<String> getCategory() {
            return Optional.empty();
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
            return false;
        }

        @Override
        public Optional<List<String>> getTags() {
            return Optional.empty();
        }

        @Override
        public String getVersion() {
            return version == null ? "0.0.1" : version;
        }

        @Override
        public Optional<List<WorkflowDefinition>> getWorkflows() {
            return Optional.ofNullable(workflows);
        }
    }

    private static List<Input> toInputs(Object inputs) {
        if (!(inputs instanceof List<?> inputList)) {
            return null;
        }

        List<Input> inputDefinitions = new ArrayList<>();

        for (Object entry : inputList) {
            Map<?, ?> input = (Map<?, ?>) entry;

            Object required = input.get("required");

            inputDefinitions.add(
                new PolyglotInput(
                    (String) input.get("name"), (String) input.get("label"),
                    input.get("type") == null ? "STRING" : String.valueOf(input.get("type")),
                    Boolean.TRUE.equals(required)));
        }

        return inputDefinitions;
    }

    private static List<Output> toOutputs(Object outputs) {
        if (!(outputs instanceof List<?> outputList)) {
            return null;
        }

        List<Output> outputDefinitions = new ArrayList<>();

        for (Object entry : outputList) {
            Map<?, ?> output = (Map<?, ?>) entry;

            outputDefinitions.add(
                new PolyglotOutput(
                    (String) output.get("name"), (String) output.get("task"),
                    PolyglotValues.copyFromPolyglotContext(output.get("value"))));
        }

        return outputDefinitions;
    }

    @SuppressFBWarnings("EI")
    private record PolyglotInput(String name, String label, String type, boolean required) implements Input {

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

    @SuppressFBWarnings("EI")
    private record PolyglotOutput(String name, String task, Object value) implements Output {

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

    @SuppressWarnings("unchecked")
    private static List<TriggerDefinition> toTriggers(Object triggers) {
        if (!(triggers instanceof List<?> triggerList)) {
            return null;
        }

        List<TriggerDefinition> triggerDefinitions = new ArrayList<>();

        for (Object entry : triggerList) {
            Map<?, ?> trigger = (Map<?, ?>) entry;

            Object parameters = trigger.get("parameters");

            triggerDefinitions.add(
                new PolyglotTrigger(
                    (String) trigger.get("name"), (String) trigger.get("type"),
                    parameters instanceof Map<?, ?> parameterMap
                        ? (Map<String, ?>) PolyglotValues.copyFromPolyglotContext(parameterMap)
                        : Map.of()));
        }

        return triggerDefinitions;
    }

    @SuppressFBWarnings("EI")
    private record PolyglotTrigger(String name, String type, Map<String, ?> parameters) implements TriggerDefinition {

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

    private record PolyglotWorkflowDefinition(
        String name, String label, String description, List<WorkflowTaskDefinition> taskDefinitions,
        List<Input> inputs, List<Output> outputs, List<TriggerDefinition> triggers)
        implements WorkflowDefinition {

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
            return Optional.ofNullable(taskDefinitions);
        }

        @Override
        public Optional<List<? extends TriggerDefinition>> getTriggers() {
            return Optional.ofNullable(triggers);
        }
    }
}
