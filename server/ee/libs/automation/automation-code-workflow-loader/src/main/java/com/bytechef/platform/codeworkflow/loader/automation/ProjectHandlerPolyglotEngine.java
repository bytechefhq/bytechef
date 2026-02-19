/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.platform.codeworkflow.loader.automation;

import com.bytechef.workflow.ProjectHandler;
import com.bytechef.workflow.definition.Input;
import com.bytechef.workflow.definition.Output;
import com.bytechef.workflow.definition.Parameter;
import com.bytechef.workflow.definition.ProjectDefinition;
import com.bytechef.workflow.definition.TaskDefinition;
import com.bytechef.workflow.definition.TriggerDefinition;
import com.bytechef.workflow.definition.WorkflowDefinition;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.TypeLiteral;
import org.graalvm.polyglot.Value;

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

    private static Context getContext() {
        return Context.newBuilder()
            .engine(engine)
            .build();
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
            return workflows;
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
