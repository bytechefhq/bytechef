/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.platform.codeworkflow.loader.automation;

import com.bytechef.automation.project.ProjectHandler;
import com.bytechef.automation.project.definition.ProjectDefinition;
import com.bytechef.workflow.definition.Input;
import com.bytechef.workflow.definition.Output;
import com.bytechef.workflow.definition.Parameter;
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
import java.util.function.Function;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.TypeLiteral;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.io.IOAccess;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class ProjectHandlerPolyglotEngine {

    private static Engine engine;

    static ProjectHandler load(String languageId, String script) {
        if (engine == null) {
            engine = Engine.create();
        }

        try (Context polyglotContext = getContext()) {
            Value value = polyglotContext.eval(languageId, script);

            String name = Objects.requireNonNull(getMember(value, "name"));
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

            return () -> new PolyglotProjectDefinition(name, description, version, workflows);
        }
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
                        toJavaTaskDefinitions(workflowName, workflow, jarPath, implClassName)));
            }

            return () -> new PolyglotProjectDefinition(name, description, version, workflows);
        }
    }

    @SuppressWarnings("unchecked")
    private static Object executePerform(String workflowName, String taskName, String languageId, String script) {
        try (Context polyglotContext = getContext()) {
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

                    return perform.apply(null);
                }
            }

            throw new IllegalArgumentException("Task name=%s not found".formatted(taskName));
        }
    }

    private static Object executeJavaPerform(
        Path jarPath, String implClassName, String workflowName, String taskName) {

        try (Context polyglotContext = getJavaContext(jarPath)) {
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

                for (int taskIndex = 0; taskIndex < sizeOf(tasksValue); taskIndex++) {
                    Value task = tasksValue.invokeMember("get", taskIndex);

                    if (taskName.equals(asString(task.invokeMember("getName")))) {
                        Value performFunction = task.invokeMember("getPerform");

                        return toHostValue(performFunction.invokeMember("apply"));
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
                    asString(unwrapOptional(task.invokeMember("getDescription"))), jarPath, implClassName));
        }

        return taskDefinitions;
    }

    private static Context getContext() {
        return Context.newBuilder()
            .engine(engine)
            .build();
    }

    private static Context getJavaContext(Path jarPath) {
        try {
            return Context.newBuilder("java")
                .engine(engine)
                .allowCreateThread(true)
                .allowNativeAccess(true)
                .allowIO(IOAccess.ALL)
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
                languageId, script))
            .toList();
    }

    private record JavaTaskDefinition(
        String workflowName, String name, String label, String description, Path jarPath, String implClassName)
        implements TaskDefinition {

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
            return () -> executeJavaPerform(jarPath, implClassName, workflowName, name);
        }
    }

    private record PolyglotTaskDefinition(
        String workflowName, String name, String label, String description, String languageId, String script)
        implements TaskDefinition {

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
            return () -> executePerform(workflowName, name, languageId, script);
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
