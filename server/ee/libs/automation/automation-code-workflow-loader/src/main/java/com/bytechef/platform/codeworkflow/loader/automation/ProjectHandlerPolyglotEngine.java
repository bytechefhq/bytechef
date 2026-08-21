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
import com.bytechef.workflow.definition.CompositeTaskDefinition.Type;
import com.bytechef.workflow.definition.ConnectionRequirement;
import com.bytechef.workflow.definition.Input;
import com.bytechef.workflow.definition.Output;
import com.bytechef.workflow.definition.TaskContext;
import com.bytechef.workflow.definition.TaskDefinition;
import com.bytechef.workflow.definition.TriggerDefinition;
import com.bytechef.workflow.definition.WorkflowDefinition;
import com.bytechef.workflow.definition.WorkflowTaskDefinition;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.Function;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.PolyglotAccess;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.io.IOAccess;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class ProjectHandlerPolyglotEngine {

    private static final String HOST_BRIDGE_BINDING_NAME = "byteChefCodeWorkflowHostBridge";
    private static final String GUEST_TASK_CONTEXT_CLASS_NAME = "com.bytechef.workflow.guest.GuestTaskContext";

    private static Engine engine;

    /**
     * Evaluates the code workflow's definition script. The script is user-supplied and its top level runs in full here,
     * so it is loaded through the same strict sandbox its task performs use rather than a permissive context of its own
     * - the ceilings apply to declaring a workflow, not only to running one.
     */
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

    static ProjectHandler loadJava(Path jarPath) {
        if (engine == null) {
            engine = Engine.create();
        }

        String implClassName = readServiceImplementationClassName(jarPath);

        try (Context polyglotContext = getJavaContext(jarPath)) {
            Value projectHandler = newGuestInstance(polyglotContext, implClassName);

            Value projectDefinition = projectHandler.invokeMember("getDefinition");

            String name = Objects.requireNonNull(asString(projectDefinition.invokeMember("getName")));
            String description = asString(unwrapOptional(projectDefinition.invokeMember("getDescription")));
            String version = asString(projectDefinition.invokeMember("getVersion"));

            List<WorkflowDefinition> workflows = new ArrayList<>();

            Value workflowsValue = projectDefinition.invokeMember("getWorkflows");

            for (int workflowIndex = 0; workflowIndex < sizeOf(workflowsValue); workflowIndex++) {
                Value workflow = workflowsValue.invokeMember("get", workflowIndex);

                String workflowName = asString(workflow.invokeMember("getName"));

                workflows.add(
                    new PolyglotWorkflowDefinition(
                        workflowName, asString(unwrapOptional(workflow.invokeMember("getLabel"))),
                        asString(unwrapOptional(workflow.invokeMember("getDescription"))),
                        toJavaTaskDefinitions(workflowName, workflow, jarPath, implClassName),
                        toJavaInputs(workflow), toJavaOutputs(workflow), toJavaTriggers(workflow)));
            }

            return () -> new PolyglotProjectDefinition(name, description, version, workflows);
        }
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

            // The guest function's return value may be a live view backed by the polyglot context (e.g. a JS object
            // mapped to a PolyglotMap), which becomes unusable once the context closes below. Copy it into plain
            // Java collections while the context is still open.
            return PolyglotValues.copyFromPolyglotContext(result);
        });
    }

    /**
     * Finds a task's guest map by name, descending into groups. Only an entry carrying a {@code perform} matches — a
     * group shares the namespace but performs no work of its own.
     */
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

    /**
     * Builds the guest-facing {@code context} argument handed to a code workflow task's {@code perform} function:
     * {@code component} exposes the shared component proxy chain
     * ({@code context.component.<componentName>.<actionName>(input, connectionName, clusterElements)}), {@code input()}
     * / {@code input(name)} return the workflow's inputs and prior task outputs, {@code connection(name)} returns a
     * wired connection's parameters, and {@code log} delegates to {@link TaskContext#log}. Both dispatch through the
     * {@link TaskContext} the engine received at perform time; a {@code null} context (a legacy zero-argument
     * invocation) fails only when the guest actually uses one of them.
     */
    private static ProxyObject toGuestContext(TaskContext taskContext, String languageId) {
        ComponentActionInvoker componentActionInvoker =
            (componentName, actionName, input, connectionName, clusterElements) -> {
                if (taskContext == null) {
                    throw new IllegalStateException("A TaskContext is not available");
                }

                return taskContext.component(componentName, actionName, input, connectionName, clusterElements);
            };

        // Component and action existence is validated host-side when an invocation dispatches through the
        // TaskContext, so the guest-facing catalog answers existence checks optimistically.
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

                    // Zero arguments hands back the whole snapshot; one argument reads a single entry, failing on an
                    // unknown name rather than yielding an undefined the guest would only notice much later.
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

    private static Object executeJavaPerform(
        Path jarPath, String implClassName, String workflowName, String taskName, TaskContext taskContext) {

        try (Context polyglotContext = getJavaContext(jarPath)) {
            Value polyglotBindings = polyglotContext.getPolyglotBindings();

            polyglotBindings.putMember(HOST_BRIDGE_BINDING_NAME, new CodeWorkflowHostBridge(taskContext));

            Value projectHandler = newGuestInstance(polyglotContext, implClassName);

            Value projectDefinition = projectHandler.invokeMember("getDefinition");

            Value workflowsValue = projectDefinition.invokeMember("getWorkflows");

            for (int workflowIndex = 0; workflowIndex < sizeOf(workflowsValue); workflowIndex++) {
                Value workflow = workflowsValue.invokeMember("get", workflowIndex);

                if (!workflowName.equals(asString(workflow.invokeMember("getName")))) {
                    continue;
                }

                Value tasksValue = unwrapOptional(workflow.invokeMember("getTasks"));

                if (tasksValue == null) {
                    break;
                }

                Value task = findJavaTask(tasksValue, taskName);

                if (task != null) {
                    Value performFunction = task.invokeMember("getPerform");

                    Value guestTaskContext = newGuestInstance(polyglotContext, GUEST_TASK_CONTEXT_CLASS_NAME);

                    return toHostValue(performFunction.invokeMember("apply", guestTaskContext));
                }
            }

            throw new IllegalArgumentException(
                "Workflow name=%s, task name=%s not found".formatted(workflowName, taskName));
        }
    }

    private static List<WorkflowTaskDefinition> toJavaTaskDefinitions(
        String workflowName, Value workflow, Path jarPath, String implClassName) {

        Value tasksValue = unwrapOptional(workflow.invokeMember("getTasks"));

        if (tasksValue == null) {
            return List.of();
        }

        List<WorkflowTaskDefinition> taskDefinitions = new ArrayList<>();

        for (int taskIndex = 0; taskIndex < sizeOf(tasksValue); taskIndex++) {
            Value task = tasksValue.invokeMember("get", taskIndex);

            // Only a composite declares getBranches, so it discriminates the two entry kinds without the guest and
            // host having to agree on a type string.
            if (task.hasMember("getBranches")) {
                taskDefinitions.add(toJavaCompositeTaskDefinition(workflowName, task, jarPath, implClassName));
            } else {
                taskDefinitions.add(toJavaTaskDefinition(workflowName, task, jarPath, implClassName));
            }
        }

        return taskDefinitions;
    }

    private static CompositeTaskDefinition toJavaCompositeTaskDefinition(
        String workflowName, Value task, Path jarPath, String implClassName) {

        Type type = CompositeTaskDefinition.Type.valueOf(asString(task.invokeMember("getType")));

        List<TaskDefinition> tasks = new ArrayList<>();
        List<List<TaskDefinition>> branches = new ArrayList<>();

        Value tasksValue = task.invokeMember("getTasks");

        for (int taskIndex = 0; taskIndex < sizeOf(tasksValue); taskIndex++) {
            tasks.add(
                toJavaTaskDefinition(workflowName, tasksValue.invokeMember("get", taskIndex), jarPath, implClassName));
        }

        Value branchesValue = task.invokeMember("getBranches");

        for (int branchIndex = 0; branchIndex < sizeOf(branchesValue); branchIndex++) {
            Value branchValue = branchesValue.invokeMember("get", branchIndex);

            List<TaskDefinition> branchTasks = new ArrayList<>();

            for (int taskIndex = 0; taskIndex < sizeOf(branchValue); taskIndex++) {
                branchTasks.add(
                    toJavaTaskDefinition(
                        workflowName, branchValue.invokeMember("get", taskIndex), jarPath, implClassName));
            }

            branches.add(branchTasks);
        }

        return new JavaCompositeTaskDefinition(
            asString(task.invokeMember("getName")), asString(unwrapOptional(task.invokeMember("getLabel"))),
            asString(unwrapOptional(task.invokeMember("getDescription"))), type, tasks, branches);
    }

    private static TaskDefinition toJavaTaskDefinition(
        String workflowName, Value task, Path jarPath, String implClassName) {

        return new JavaTaskDefinition(
            workflowName, asString(task.invokeMember("getName")),
            asString(unwrapOptional(task.invokeMember("getLabel"))),
            asString(unwrapOptional(task.invokeMember("getDescription"))),
            toJavaConnectionRequirements(unwrapOptional(task.invokeMember("getConnections"))), jarPath,
            implClassName);
    }

    /**
     * Finds a task by name among a workflow's entries, descending into groups. A group declares getBranches and no
     * perform of its own, so only leaves match.
     */
    private static Value findJavaTask(Value tasksValue, String taskName) {
        for (int taskIndex = 0; taskIndex < sizeOf(tasksValue); taskIndex++) {
            Value task = tasksValue.invokeMember("get", taskIndex);

            if (!task.hasMember("getBranches")) {
                if (taskName.equals(asString(task.invokeMember("getName")))) {
                    return task;
                }

                continue;
            }

            Value nestedTask = findJavaTask(task.invokeMember("getTasks"), taskName);

            if (nestedTask != null) {
                return nestedTask;
            }

            Value branchesValue = task.invokeMember("getBranches");

            for (int branchIndex = 0; branchIndex < sizeOf(branchesValue); branchIndex++) {
                Value branchTask = findJavaTask(branchesValue.invokeMember("get", branchIndex), taskName);

                if (branchTask != null) {
                    return branchTask;
                }
            }
        }

        return null;
    }

    private static List<ConnectionRequirement> toJavaConnectionRequirements(Value connectionsValue) {
        if (connectionsValue == null) {
            return null;
        }

        List<ConnectionRequirement> connectionRequirements = new ArrayList<>();

        for (int connectionIndex = 0; connectionIndex < sizeOf(connectionsValue); connectionIndex++) {
            Value connection = connectionsValue.invokeMember("get", connectionIndex);

            Value componentVersionValue = connection.invokeMember("getComponentVersion");

            Value isPresentValue = componentVersionValue.invokeMember("isPresent");

            Integer componentVersion = isPresentValue.asBoolean()
                ? componentVersionValue.invokeMember("getAsInt")
                    .asInt()
                : null;

            connectionRequirements.add(
                new PolyglotConnectionRequirement(
                    asString(connection.invokeMember("getComponentName")), componentVersion,
                    asString(connection.invokeMember("getName"))));
        }

        return connectionRequirements;
    }

    private static Context getJavaContext(Path jarPath) {
        try {
            // allowExperimentalOptions is required for java.Polyglot, which exposes the guest polyglot API the
            // bridge uses to import the host callback object; it does not widen host access.
            return Context.newBuilder("java")
                .engine(engine)
                .allowCreateThread(true)
                .allowExperimentalOptions(true)
                .allowNativeAccess(true)
                .allowIO(IOAccess.ALL)
                .allowPolyglotAccess(PolyglotAccess.ALL)
                .option("java.Polyglot", "true")
                .option("java.Classpath", jarPath + File.pathSeparator + GuestSdkClasspath.get())
                .build();
        } catch (RuntimeException e) {
            throw new IllegalStateException(
                "Failed to create a GraalVM Espresso context. Embedded Espresso (org.graalvm.polyglot:java) supports " +
                    "linux (amd64, aarch64) and darwin-amd64 as of GraalVM 25; it cannot boot on this platform.",
                e);
        }
    }

    private static Value newGuestInstance(Context polyglotContext, String implClassName) {
        Value bindings = polyglotContext.getBindings("java");

        Value handlerClass = bindings.getMember(implClassName);

        if (handlerClass == null) {
            throw new IllegalStateException(
                "Class %s is not present on the guest classpath".formatted(implClassName));
        }

        return handlerClass.newInstance();
    }

    private static String readServiceImplementationClassName(Path jarPath) {
        String serviceEntryName = "META-INF/services/" + ProjectHandler.class.getName();

        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            JarEntry jarEntry = jarFile.getJarEntry(serviceEntryName);

            if (jarEntry == null) {
                throw new IllegalArgumentException(
                    "Jar %s is missing the service registration %s".formatted(jarPath, serviceEntryName));
            }

            try (InputStream inputStream = jarFile.getInputStream(jarEntry)) {
                String serviceEntryContent = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

                return serviceEntryContent.lines()
                    .map(String::trim)
                    .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                        "Service registration %s in jar %s is empty".formatted(serviceEntryName, jarPath)));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String asString(Value value) {
        return value == null || value.isNull() ? null : value.asString();
    }

    private static int sizeOf(Value listValue) {
        Value sizeValue = listValue.invokeMember("size");

        return sizeValue.asInt();
    }

    private static Value unwrapOptional(Value optionalValue) {
        if (optionalValue == null || optionalValue.isNull()) {
            return null;
        }

        Value unwrapped = optionalValue.invokeMember("orElse", (Object) null);

        return unwrapped == null || unwrapped.isNull() ? null : unwrapped;
    }

    private static Object toHostValue(Value value) {
        if (value == null || value.isNull()) {
            return null;
        }

        if (value.isBoolean()) {
            return value.asBoolean();
        }

        if (value.isNumber()) {
            return value.as(Number.class);
        }

        if (value.isString()) {
            return value.asString();
        }

        throw new IllegalStateException(
            "A Java code workflow perform must return null, a boolean, a number or a string, got: " + value);
    }

    private static String getMember(Value value, String name) {
        value = value.getMember(name);

        return value == null ? null : value.as(String.class);
    }

    /**
     * Walks the guest definition's {@code workflows} member into host-native collections.
     *
     * <p>
     * Walked rather than mapped with {@code Value.as(TypeLiteral)}: the perform contexts this runs in are built under a
     * {@link org.graalvm.polyglot.SandboxPolicy} that forbids host object mappings of mutable target types. The walk
     * keeps a task's {@code perform} member callable.
     */
    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> getWorkflows(Value value) {
        return (List<Map<String, Object>>) PolyglotValues.copyToJavaValue(value.getMember("workflows"));
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

    /**
     * Reads a group of tasks the engine runs concurrently: {@code type: "parallel"} with a {@code tasks} list, or
     * {@code type: "forkJoin"} with a {@code branches} list of task lists.
     *
     * <p>
     * A group carries no {@code perform} of its own, and a group inside a group is rejected — the dispatchers support
     * it, but the semantics of a branch fanning out again while its siblings run have not been worked through.
     */
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

    /**
     * Task names share one namespace across the whole workflow, nesting included: a name is what the engine keys a
     * task's output by and what {@code context.input(name)} looks up, so a duplicate would make one task's output
     * unreachable.
     */
    private static void addName(Set<String> names, String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Every task must declare a name");
        }

        if (!names.add(name)) {
            throw new IllegalArgumentException("Task name %s is declared more than once".formatted(name));
        }
    }

    /**
     * Reads a task's declared connections. Two shapes are accepted: a LIST of
     * {@code {componentName, componentVersion?, name}} entries, and a MAP keyed by connection name whose values are
     * {@code {componentName, componentVersion?}} — the latter mirrors the shape the generated workflow definition
     * carries, so sources written against it parse too.
     */
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
    private record JavaCompositeTaskDefinition(
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

    private record JavaTaskDefinition(
        String workflowName, String name, String label, String description,
        List<ConnectionRequirement> connections, Path jarPath, String implClassName)
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
            return Optional.empty();
        }

        @Override
        public PerformFunction getPerform() {
            return new PerformFunction() {

                @Override
                public Object apply() {
                    return executeJavaPerform(jarPath, implClassName, workflowName, name, null);
                }

                @Override
                public Object apply(TaskContext taskContext) {
                    return executeJavaPerform(jarPath, implClassName, workflowName, name, taskContext);
                }
            };
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

    /**
     * Reads a workflow's declared inputs. Absent means the workflow declares none — the platform then has no contract
     * to prompt for or validate against, which is what every code workflow looked like before inputs could be declared
     * at all.
     */
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

    /**
     * Reads a workflow's declared outputs. An entry names either a {@code task} whose output is the value — the only
     * form that reaches a task name a {@code ${...}} expression cannot — or a literal/expression {@code value}.
     */
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

    private static List<Input> toJavaInputs(Value workflow) {
        Value inputsValue = unwrapOptional(workflow.invokeMember("getInputs"));

        if (inputsValue == null) {
            return null;
        }

        List<Input> inputs = new ArrayList<>();

        for (int inputIndex = 0; inputIndex < sizeOf(inputsValue); inputIndex++) {
            Value input = inputsValue.invokeMember("get", inputIndex);

            inputs.add(
                new PolyglotInput(
                    asString(input.invokeMember("getName")), asString(input.invokeMember("getLabel")),
                    asString(input.invokeMember("getType")),
                    input.invokeMember("isRequired")
                        .asBoolean()));
        }

        return inputs;
    }

    private static List<Output> toJavaOutputs(Value workflow) {
        Value outputsValue = unwrapOptional(workflow.invokeMember("getOutputs"));

        if (outputsValue == null) {
            return null;
        }

        List<Output> outputs = new ArrayList<>();

        for (int outputIndex = 0; outputIndex < sizeOf(outputsValue); outputIndex++) {
            Value output = outputsValue.invokeMember("get", outputIndex);

            Value value = output.invokeMember("getValue");

            outputs.add(
                new PolyglotOutput(
                    asString(output.invokeMember("getName")), asString(output.invokeMember("getTask")),
                    value.isNull() ? null : value.as(Object.class)));
        }

        return outputs;
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

    /**
     * Reads a workflow's declared triggers. A trigger names a component trigger the platform already provides — it is
     * not guest code — so only its type and parameters cross from the source.
     */
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
                        // The guest map is a live view of the polyglot context, which closes before the definition is
                        // used, so copy it into plain Java collections now.
                        ? (Map<String, ?>) PolyglotValues.copyFromPolyglotContext(parameterMap)
                        : Map.of()));
        }

        return triggerDefinitions;
    }

    private static List<TriggerDefinition> toJavaTriggers(Value workflow) {
        Value triggersValue = unwrapOptional(workflow.invokeMember("getTriggers"));

        if (triggersValue == null) {
            return null;
        }

        List<TriggerDefinition> triggers = new ArrayList<>();

        for (int triggerIndex = 0; triggerIndex < sizeOf(triggersValue); triggerIndex++) {
            Value trigger = triggersValue.invokeMember("get", triggerIndex);

            triggers.add(
                new PolyglotTrigger(
                    asString(trigger.invokeMember("getName")), asString(trigger.invokeMember("getType")),
                    trigger.invokeMember("getParameters")
                        .as(Map.class)));
        }

        return triggers;
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
