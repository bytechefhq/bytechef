/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.platform.codeworkflow.loader.automation;

import com.bytechef.automation.project.ProjectHandler;
import com.bytechef.automation.project.definition.ProjectDefinition;
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
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class ProjectHandlerPolyglotEngine {

    static ProjectHandler load(String languageId, String script) {
        return PolyglotSandbox.call(languageId, polyglotContext -> {
            Value value = polyglotContext.eval(languageId, script);

            String name = Objects.requireNonNull(getMember(value, "name"));
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

            return () -> new PolyglotProjectDefinition(name, description, version, workflows);
        });
    }

    private static Object executePerform(
        String workflowName, String taskName, String languageId, String script, Map<String, ?> input,
        Map<String, ?> connection, TaskContext taskContext) {

        return PolyglotSandbox.call(languageId, polyglotContext -> {
            Value value = polyglotContext.eval(languageId, script);

            Value workflow = findWorkflow(value, workflowName);

            if (workflow == null) {
                throw new IllegalArgumentException("Workflow name=%s not found".formatted(workflowName));
            }

            Value performFunction = findPerformFunction(getValue(workflow, "tasks"), taskName);

            if (performFunction == null) {
                throw new IllegalArgumentException("Task name=%s not found".formatted(taskName));
            }

            Object guestInput = PolyglotValues.copyToGuestValue(input, languageId);
            ProxyObject guestContext = toGuestContext(taskContext, languageId);

            Value result;

            if (getRequiredPerformParameterCount(polyglotContext, languageId, performFunction) >= 3) {
                Object guestConnection = PolyglotValues.copyToGuestMemberAndHashMap(connection, languageId);

                result = performFunction.execute(guestInput, guestConnection, guestContext);
            } else {
                result = performFunction.execute(guestInput, guestContext);
            }

            return PolyglotValues.copyFromPolyglotContext(PolyglotValues.copyToJavaValue(result));
        });
    }

    private static int getRequiredPerformParameterCount(
        Context polyglotContext, String languageId, Value performFunction) {

        String parameterCountFunctionSource = switch (languageId) {
            case "js" -> "(performFunction) => performFunction.length";
            case "python" -> "lambda perform_function: perform_function.__code__.co_argcount - " +
                "len(perform_function.__defaults__ or ())";
            default -> null;
        };

        if (parameterCountFunctionSource == null) {
            return 2;
        }

        Value parameterCountFunction = polyglotContext.eval(languageId, parameterCountFunctionSource);

        Value parameterCount = parameterCountFunction.execute(performFunction);

        return parameterCount.fitsInInt() ? parameterCount.asInt() : 2;
    }

    private static Value findWorkflow(Value value, String workflowName) {
        Value workflows = getValue(value, "workflows");

        if (workflows == null || !workflows.hasArrayElements()) {
            return null;
        }

        for (long index = 0; index < workflows.getArraySize(); index++) {
            Value workflow = workflows.getArrayElement(index);

            if (workflowName.equals(getMember(workflow, "name"))) {
                return workflow;
            }
        }

        return null;
    }

    private static Value findPerformFunction(Value tasks, String taskName) {
        if (tasks == null || !tasks.hasArrayElements()) {
            return null;
        }

        for (long index = 0; index < tasks.getArraySize(); index++) {
            Value task = tasks.getArrayElement(index);

            Value performFunction = getValue(task, "perform");

            if (taskName.equals(getMember(task, "name")) && performFunction != null && !performFunction.isNull()) {
                return performFunction;
            }

            Value nestedPerformFunction = findPerformFunction(getValue(task, "tasks"), taskName);

            if (nestedPerformFunction != null) {
                return nestedPerformFunction;
            }

            Value branches = getValue(task, "branches");

            if (branches == null || !branches.hasArrayElements()) {
                continue;
            }

            for (long branchIndex = 0; branchIndex < branches.getArraySize(); branchIndex++) {
                Value branchPerformFunction = findPerformFunction(branches.getArrayElement(branchIndex), taskName);

                if (branchPerformFunction != null) {
                    return branchPerformFunction;
                }
            }
        }

        return null;
    }

    private static ProxyObject toGuestContext(TaskContext taskContext, String languageId) {
        ComponentActionInvoker componentActionInvoker = taskContext::component;

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
                "log", (ProxyExecutable) arguments -> {
                    taskContext.log(
                        TaskContext.LogLevel.of(arguments[0].asString()), PolyglotValues.toLogMessage(arguments[1]));

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

        return new PolyglotTaskDefinition(
            workflowName, name, (String) task.get("label"), (String) task.get("description"),
            toConnectionRequirements(task.get("connections")), languageId, script);
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
        List<ConnectionRequirement> connections, String languageId, String script)
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
        public PerformFunction getPerform() {
            return (input, connection, taskContext) -> executePerform(
                workflowName, name, languageId, script, input, connection, taskContext);
        }
    }

    private record PolyglotProjectDefinition(
        String name, String description, String version, List<WorkflowDefinition> workflows)
        implements ProjectDefinition {

        @Override
        public Optional<String> getCategory() {
            return Optional.empty();
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
        public String getVersion() {
            return version == null ? "0.0.1" : version;
        }

        @Override
        public List<WorkflowDefinition> getWorkflows() {
            return List.copyOf(workflows);
        }

        @Override
        public Optional<List<String>> getTags() {
            return Optional.empty();
        }
    }

    private static List<Input> toInputs(Object inputs) {
        if (!(inputs instanceof List<?> inputList)) {
            return null;
        }

        List<Input> inputDefinitions = new ArrayList<>();

        for (Object entry : inputList) {
            Map<?, ?> input = (Map<?, ?>) entry;

            Object internalOnly = input.get("internalOnly");
            Object required = input.get("required");

            inputDefinitions.add(
                new PolyglotInput(
                    (String) input.get("name"), (String) input.get("label"),
                    input.get("type") == null ? "STRING" : String.valueOf(input.get("type")),
                    Boolean.TRUE.equals(required), Boolean.TRUE.equals(internalOnly),
                    toComponentInputReference(input)));
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

    private static Input.ComponentInputReference toComponentInputReference(Map<?, ?> input) {
        Object componentName = input.get("componentName");
        Object componentVersion = input.get("componentVersion");
        Object groupName = input.get("groupName");

        if (componentName == null && componentVersion == null && groupName == null) {
            return null;
        }

        if (!(componentName instanceof String) || !(componentVersion instanceof Number number) ||
            !(groupName instanceof String)) {

            throw new IllegalArgumentException(
                ("Input %s must set componentName, componentVersion and groupName together to use a component " +
                    "input").formatted(input.get("name")));
        }

        return new Input.ComponentInputReference((String) componentName, number.intValue(), (String) groupName);
    }

    @SuppressFBWarnings("EI")
    private record PolyglotInput(
        String name, String label, String type, boolean required, boolean internalOnly,
        Input.ComponentInputReference componentInput) implements Input {

        @Override
        public Optional<ComponentInputReference> getComponentInput() {
            return Optional.ofNullable(componentInput);
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
        public boolean isInternalOnly() {
            return internalOnly;
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
