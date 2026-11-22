/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.codeworkflow.loader;

import com.bytechef.platform.component.polyglot.ComponentActionInvoker;
import com.bytechef.platform.component.polyglot.ComponentCatalog;
import com.bytechef.platform.component.polyglot.ComponentProxyObject;
import com.bytechef.platform.component.polyglot.GuestFunctions;
import com.bytechef.platform.component.polyglot.PolyglotSandbox;
import com.bytechef.platform.component.polyglot.PolyglotValues;
import com.bytechef.workflow.definition.ComponentTaskDefinition;
import com.bytechef.workflow.definition.CompositeTaskDefinition;
import com.bytechef.workflow.definition.ConnectionRequirement;
import com.bytechef.workflow.definition.Input;
import com.bytechef.workflow.definition.LeafTaskDefinition;
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
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public abstract class AbstractHandlerPolyglotEngine<H> {

    private static final Pattern COMPONENT_TASK_TYPE_PATTERN = Pattern.compile("[^/\\s]+/v[1-9]\\d{0,8}/[^/\\s]+");

    public H loadHandler(String languageId, String script) {
        return PolyglotSandbox.call(languageId, polyglotContext -> {
            Value value = polyglotContext.eval(languageId, script);

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

            return createHandler(value, workflows);
        });
    }

    protected abstract H createHandler(Value value, List<WorkflowDefinition> workflows);

    protected static String getMember(Value value, String name) {
        value = getValue(value, name);

        return value == null ? null : value.as(String.class);
    }

    protected static Value getValue(Value value, String name) {
        if (value.hasHashEntries()) {
            return value.hasHashEntry(name) ? value.getHashValue(name) : null;
        }

        return value.getMember(name);
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

            if (GuestFunctions.getRequiredParameterCount(polyglotContext, languageId, performFunction, 2) >= 3) {
                Object guestConnection = PolyglotValues.copyToGuestMemberAndHashMap(connection, languageId);

                result = performFunction.execute(guestInput, guestConnection, guestContext);
            } else {
                result = performFunction.execute(guestInput, guestContext);
            }

            return PolyglotValues.copyFromPolyglotContext(PolyglotValues.copyToJavaValue(result));
        });
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

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> getWorkflows(Value value) {
        Value workflows = getValue(value, "workflows");

        if (workflows == null || workflows.isNull()) {
            return List.of();
        }

        return (List<Map<String, Object>>) PolyglotValues.copyToJavaValue(workflows);
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
            } else if (isCompositeType(String.valueOf(type))) {
                taskDefinitions.add(
                    toCompositeTaskDefinition(workflowName, task, String.valueOf(type), languageId, script, names));
            } else {
                taskDefinitions.add(toComponentTaskDefinition(task, String.valueOf(type), names));
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

        List<LeafTaskDefinition> tasks = new ArrayList<>();
        List<List<LeafTaskDefinition>> branches = new ArrayList<>();

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

                List<LeafTaskDefinition> branchTaskDefinitions = new ArrayList<>();

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

    private static boolean isCompositeType(String type) {
        String normalizedType = normalizeCompositeType(type);

        return "parallel".equalsIgnoreCase(normalizedType) || "forkjoin".equalsIgnoreCase(normalizedType);
    }

    private static String normalizeCompositeType(String type) {
        return type.replace("-", "")
            .replace("_", "");
    }

    private static CompositeTaskDefinition.Type toCompositeType(String name, String type) {
        String normalizedType = normalizeCompositeType(type);

        if ("parallel".equalsIgnoreCase(normalizedType)) {
            return CompositeTaskDefinition.Type.PARALLEL;
        }

        if ("forkjoin".equalsIgnoreCase(normalizedType)) {
            return CompositeTaskDefinition.Type.FORK_JOIN;
        }

        throw new IllegalArgumentException(
            "Task %s declares type %s; a task's type may only be parallel or forkJoin".formatted(name, type));
    }

    @SuppressWarnings("unchecked")
    private static ComponentTaskDefinition toComponentTaskDefinition(
        Map<?, ?> task, String type, Set<String> names) {

        String name = (String) task.get("name");

        addName(names, name);

        Matcher matcher = COMPONENT_TASK_TYPE_PATTERN.matcher(type);

        if (!matcher.matches()) {
            throw new IllegalArgumentException(
                ("Task %s declares type %s; a task's type may only be parallel or forkJoin, or a component action " +
                    "written <component>/v<version>/<action>, e.g. apiPlatform/v1/responseToAPIRequest")
                        .formatted(name, type));
        }

        if (task.get("perform") != null) {
            throw new IllegalArgumentException(
                "Task %s runs component action %s, so it cannot declare a perform".formatted(name, type));
        }

        if (task.get("connections") != null) {
            throw new IllegalArgumentException(
                ("Task %s runs component action %s; its connection comes from the component through the " +
                    "deployment, so it cannot declare connections").formatted(name, type));
        }

        if (task.get("tasks") != null || task.get("branches") != null) {
            throw new IllegalArgumentException(
                ("Task %s runs component action %s, so it cannot declare tasks or branches; only a parallel or " +
                    "forkJoin task groups other tasks").formatted(name, type));
        }

        Object parameters = task.get("parameters");
        Map<String, ?> componentParameters = Map.of();

        if (parameters instanceof Map<?, ?> parameterMap) {
            componentParameters = (Map<String, ?>) PolyglotValues.copyFromPolyglotContext(parameterMap);
        } else if (parameters != null) {
            throw new IllegalArgumentException("Task %s must declare parameters as a map".formatted(name));
        }

        return new PolyglotComponentTaskDefinition(
            name, (String) task.get("label"), (String) task.get("description"), type, componentParameters);
    }

    private static LeafTaskDefinition toNestedTaskDefinition(
        String workflowName, Object nestedTask, String languageId, String script, Set<String> names) {

        Map<?, ?> task = (Map<?, ?>) nestedTask;

        Object type = task.get("type");

        if (type == null) {
            return toLeafTaskDefinition(workflowName, task, languageId, script, names);
        }

        if (isCompositeType(String.valueOf(type))) {
            throw new IllegalArgumentException(
                "Task %s is nested inside a group, and a group inside a group is not supported"
                    .formatted(task.get("name")));
        }

        return toComponentTaskDefinition(task, String.valueOf(type), names);
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
        String name, String label, String description, Type type, List<LeafTaskDefinition> tasks,
        List<List<LeafTaskDefinition>> branches)
        implements CompositeTaskDefinition {

        @Override
        public List<? extends List<? extends LeafTaskDefinition>> getBranches() {
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
        public List<? extends LeafTaskDefinition> getTasks() {
            return tasks;
        }

        @Override
        public Type getType() {
            return type;
        }
    }

    @SuppressFBWarnings("EI")
    private record PolyglotComponentTaskDefinition(
        String name, String label, String description, String type, Map<String, ?> parameters)
        implements ComponentTaskDefinition {

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
        public Map<String, ?> getParameters() {
            return parameters;
        }

        @Override
        public String getType() {
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
