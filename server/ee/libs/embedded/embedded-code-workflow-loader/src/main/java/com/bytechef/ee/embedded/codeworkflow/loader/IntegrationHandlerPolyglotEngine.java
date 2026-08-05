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
import com.bytechef.workflow.definition.ConnectionRequirement;
import com.bytechef.workflow.definition.Input;
import com.bytechef.workflow.definition.Output;
import com.bytechef.workflow.definition.Parameter;
import com.bytechef.workflow.definition.TaskContext;
import com.bytechef.workflow.definition.TaskDefinition;
import com.bytechef.workflow.definition.TriggerDefinition;
import com.bytechef.workflow.definition.WorkflowDefinition;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Function;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.PolyglotAccess;
import org.graalvm.polyglot.TypeLiteral;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.io.IOAccess;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class IntegrationHandlerPolyglotEngine {

    private static final String HOST_BRIDGE_BINDING_NAME = "byteChefCodeWorkflowHostBridge";
    private static final String GUEST_TASK_CONTEXT_CLASS_NAME = "com.bytechef.workflow.guest.GuestTaskContext";

    private static Engine engine;

    // A dedicated engine for the strict-sandbox perform contexts. GraalVM caches host-access interop info
    // per engine, so building a strict `HostAccess.NONE` context and the permissive definition-loading
    // context on the SAME engine corrupts that cache; keeping them on separate engines avoids the clash
    // while leaving definition loading (`getContext()`/`engine`) untouched.
    private static Engine performEngine;

    static IntegrationHandler load(String languageId, String script) {
        if (engine == null) {
            engine = Engine.create();
        }

        try (Context polyglotContext = getContext()) {
            Value value = polyglotContext.eval(languageId, script);

            String componentName = Objects.requireNonNull(getMember(value, "componentName"));

            Value componentVersionValue = value.getMember("componentVersion");

            int componentVersion = componentVersionValue.asInt();

            String description = getMember(value, "description");
            String version = getMember(value, "version");

            List<WorkflowDefinition> workflows = getWorkflows(
                value, new TypeLiteral<List<Map<String, Object>>>() {})
                    .stream()
                    .map(workflow -> (WorkflowDefinition) new PolyglotWorkflowDefinition(
                        (String) workflow.get("name"), (String) workflow.get("label"),
                        (String) workflow.get("description"),
                        toTaskDefinitions(
                            (String) workflow.get("name"), (List<?>) workflow.get("tasks"), languageId, script)))
                    .toList();

            return () -> new PolyglotIntegrationDefinition(
                componentName, componentVersion, description, version, workflows);
        }
    }

    static IntegrationHandler loadJava(Path jarPath) {
        if (engine == null) {
            engine = Engine.create();
        }

        String implClassName = readServiceImplementationClassName(jarPath);

        try (Context polyglotContext = getJavaContext(jarPath)) {
            Value integrationHandler = newGuestInstance(polyglotContext, implClassName);

            Value integrationDefinition = integrationHandler.invokeMember("getDefinition");

            String componentName = Objects.requireNonNull(
                asString(integrationDefinition.invokeMember("getComponentName")));

            Value componentVersionValue = integrationDefinition.invokeMember("getComponentVersion");

            int componentVersion = componentVersionValue.asInt();

            String description = asString(unwrapOptional(integrationDefinition.invokeMember("getDescription")));
            String version = asString(integrationDefinition.invokeMember("getVersion"));

            List<WorkflowDefinition> workflows = new ArrayList<>();

            Value workflowsValue = unwrapOptional(integrationDefinition.invokeMember("getWorkflows"));

            if (workflowsValue != null) {
                for (int workflowIndex = 0; workflowIndex < sizeOf(workflowsValue); workflowIndex++) {
                    Value workflow = workflowsValue.invokeMember("get", workflowIndex);

                    String workflowName = asString(workflow.invokeMember("getName"));

                    workflows.add(
                        new PolyglotWorkflowDefinition(
                            workflowName, asString(unwrapOptional(workflow.invokeMember("getLabel"))),
                            asString(unwrapOptional(workflow.invokeMember("getDescription"))),
                            toJavaTaskDefinitions(workflowName, workflow, jarPath, implClassName)));
                }
            }

            return () -> new PolyglotIntegrationDefinition(
                componentName, componentVersion, description, version, workflows);
        }
    }

    @SuppressWarnings("unchecked")
    private static Object executePerform(
        String workflowName, String taskName, String languageId, String script, TaskContext taskContext) {

        if (performEngine == null) {
            performEngine = Engine.create();
        }

        try (Context polyglotContext = PolyglotSandbox.newContext(performEngine, languageId)) {
            Value value = polyglotContext.eval(languageId, script);

            List<Map<String, Object>> workflows = getWorkflows(value, new TypeLiteral<>() {});

            List<Map<String, Object>> tasks = (List<Map<String, Object>>) workflows.stream()
                .filter(workflow -> workflowName.equals(workflow.get("name")))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Workflow name=%s not found".formatted(workflowName)))
                .get("tasks");

            for (Map<String, Object> task : tasks) {
                if (taskName.equals(task.get("name"))) {
                    Function<Object[], Object> perform = (Function<Object[], Object>) task.get("perform");

                    Object result = perform.apply(new Object[] {
                        toGuestContext(taskContext, languageId)
                    });

                    // The guest function's return value may be a live view backed by the polyglot context (e.g. a
                    // JS object mapped to a PolyglotMap), which becomes unusable once the context closes below.
                    // Copy it into plain Java collections while the context is still open.
                    return PolyglotValues.copyFromPolyglotContext(result);
                }
            }

            throw new IllegalArgumentException("Task name=%s not found".formatted(taskName));
        }
    }

    /**
     * Builds the guest-facing {@code context} argument handed to a code workflow task's {@code perform} function:
     * {@code component} exposes the shared component proxy chain
     * ({@code context.component.<componentName>.<actionName>(input, connectionName)}), {@code connection(name)} returns
     * a wired connection's parameters, and {@code log} delegates to {@link TaskContext#log}. Both dispatch through the
     * {@link TaskContext} the engine received at perform time; a {@code null} context (a legacy zero-argument
     * invocation) fails only when the guest actually uses one of them.
     */
    private static ProxyObject toGuestContext(TaskContext taskContext, String languageId) {
        ComponentActionInvoker componentActionInvoker = (componentName, actionName, input, connectionName) -> {
            if (taskContext == null) {
                throw new IllegalStateException("A TaskContext is not available");
            }

            return taskContext.component(componentName, actionName, input, connectionName);
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

                    taskContext.log(arguments[0].asString(), arguments[1].asString());

                    return null;
                }));
    }

    private static Object executeJavaPerform(
        Path jarPath, String implClassName, String workflowName, String taskName, TaskContext taskContext) {

        try (Context polyglotContext = getJavaContext(jarPath)) {
            Value polyglotBindings = polyglotContext.getPolyglotBindings();

            polyglotBindings.putMember(HOST_BRIDGE_BINDING_NAME, new CodeWorkflowHostBridge(taskContext));

            Value integrationHandler = newGuestInstance(polyglotContext, implClassName);

            Value integrationDefinition = integrationHandler.invokeMember("getDefinition");

            Value workflowsValue = unwrapOptional(integrationDefinition.invokeMember("getWorkflows"));

            if (workflowsValue != null) {
                for (int workflowIndex = 0; workflowIndex < sizeOf(workflowsValue); workflowIndex++) {
                    Value workflow = workflowsValue.invokeMember("get", workflowIndex);

                    if (!workflowName.equals(asString(workflow.invokeMember("getName")))) {
                        continue;
                    }

                    Value tasksValue = unwrapOptional(workflow.invokeMember("getTasks"));

                    if (tasksValue == null) {
                        break;
                    }

                    for (int taskIndex = 0; taskIndex < sizeOf(tasksValue); taskIndex++) {
                        Value task = tasksValue.invokeMember("get", taskIndex);

                        if (taskName.equals(asString(task.invokeMember("getName")))) {
                            Value performFunction = task.invokeMember("getPerform");

                            Value guestTaskContext = newGuestInstance(polyglotContext, GUEST_TASK_CONTEXT_CLASS_NAME);

                            return toHostValue(performFunction.invokeMember("apply", guestTaskContext));
                        }
                    }
                }
            }

            throw new IllegalArgumentException(
                "Workflow name=%s, task name=%s not found".formatted(workflowName, taskName));
        }
    }

    private static List<TaskDefinition> toJavaTaskDefinitions(
        String workflowName, Value workflow, Path jarPath, String implClassName) {

        Value tasksValue = unwrapOptional(workflow.invokeMember("getTasks"));

        if (tasksValue == null) {
            return List.of();
        }

        List<TaskDefinition> taskDefinitions = new ArrayList<>();

        for (int taskIndex = 0; taskIndex < sizeOf(tasksValue); taskIndex++) {
            Value task = tasksValue.invokeMember("get", taskIndex);

            taskDefinitions.add(
                new JavaTaskDefinition(
                    workflowName, asString(task.invokeMember("getName")),
                    asString(unwrapOptional(task.invokeMember("getLabel"))),
                    asString(unwrapOptional(task.invokeMember("getDescription"))),
                    toJavaConnectionRequirements(unwrapOptional(task.invokeMember("getConnections"))), jarPath,
                    implClassName));
        }

        return taskDefinitions;
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

    private static Context getContext() {
        return Context.newBuilder()
            .engine(engine)
            .build();
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
        String serviceEntryName = "META-INF/services/" + IntegrationHandler.class.getName();

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

    private static String getMember(Value value, String name) {
        value = value.getMember(name);

        return value == null ? null : value.as(String.class);
    }

    private static <T> T getWorkflows(Value value, TypeLiteral<T> typeLiteral) {
        return value.getMember("workflows")
            .as(typeLiteral);
    }

    private static List<TaskDefinition> toTaskDefinitions(
        String workflowName, List<?> tasks, String languageId, String script) {

        if (tasks == null) {
            return List.of();
        }

        return tasks.stream()
            .map(task -> (Map<?, ?>) task)
            .map(task -> (TaskDefinition) new PolyglotTaskDefinition(
                workflowName, (String) task.get("name"), (String) task.get("label"), (String) task.get("description"),
                toConnectionRequirements(task.get("connections")), languageId, script))
            .toList();
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
        public Optional<List<? extends Parameter>> getParameters() {
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
        public Optional<List<? extends Parameter>> getParameters() {
            return Optional.empty();
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

    private record PolyglotWorkflowDefinition(
        String name, String label, String description, List<TaskDefinition> taskDefinitions)
        implements WorkflowDefinition {

        @Override
        public Optional<String> getDescription() {
            return Optional.ofNullable(description);
        }

        @Override
        public Optional<List<? extends Input>> getInputs() {
            return Optional.empty();
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
            return Optional.empty();
        }

        @Override
        public Optional<List<? extends TaskDefinition>> getTasks() {
            return Optional.ofNullable(taskDefinitions);
        }

        @Override
        public Optional<List<? extends TriggerDefinition>> getTriggers() {
            return Optional.empty();
        }
    }
}
