/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.customcomponent.loader;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.definition.ConnectionDefinition;
import com.bytechef.component.definition.Help;
import com.bytechef.component.definition.OutputDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property;
import com.bytechef.component.definition.Resources;
import com.bytechef.component.definition.TriggerDefinition;
import com.bytechef.component.definition.UnifiedApiDefinition;
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
class ComponentHandlerPolyglotEngine {

    private static Engine engine;

    static ComponentHandler load(String languageId, String script) {
        if (engine == null) {
            engine = Engine.create();
        }

        try (Context polyglotContext = getContext()) {
            Value value = polyglotContext.eval(languageId, script);

            String name = Objects.requireNonNull(getMember(value, "name", String.class));
            String title = getMember(value, "title", String.class);
            String description = getMember(value, "description", String.class);
            int version = Objects.requireNonNull(getMember(value, "version", Integer.class));
            List<Map<String, Object>> actions = getMember(value, "actions", new TypeLiteral<>() {});

            List<ActionDefinition> actionDefinitions = toActionDefinitions(actions, languageId, script);

            return () -> new PolyglotComponentDefinition(name, title, description, version, actionDefinitions);
        }
    }

    @SuppressWarnings({
        "PMD.UnusedFormalParameter", "unchecked"
    })
    private static Object executePerform(
        String actionName, Parameters inputParameters, Parameters connectionParameters, ActionContext context,
        String languageId, String script) {

        try (Context polyglotContext = getContext()) {
            Value value = polyglotContext.eval(languageId, script);

            List<Map<String, Object>> tasks = getMember(value, "actions", new TypeLiteral<>() {});

            for (Map<String, Object> task : tasks) {
                if (actionName.equals(task.get("name"))) {
                    Function<Object[], Object> perform = (Function<Object[], Object>) task.get("perform");

                    return perform.apply(null);
                }
            }

            throw new IllegalArgumentException("Action name=%s not found".formatted(actionName));
        }
    }

    private static Context getContext() {
        return Context.newBuilder()
            .engine(engine)
            .build();
    }

    private static <T> T getMember(Value value, String name, Class<T> valueClass) {
        value = value.getMember(name);

        return value == null ? null : value.as(valueClass);
    }

    private static <T> T getMember(Value value, String name, TypeLiteral<T> typeLiteral) {
        Value member = value.getMember(name);

        return member.as(typeLiteral);
    }

    private static List<ActionDefinition> toActionDefinitions(
        List<Map<String, Object>> actions, String languageId, String script) {

        if (actions == null) {
            return List.of();
        }

        return actions.stream()
            .map(task -> (ActionDefinition) new PolyglotActionDefinition(
                (String) task.get("name"), (String) task.get("title"), (String) task.get("description"), languageId,
                script))
            .toList();
    }

    private record PolyglotActionDefinition(
        String name, String title, String description, String languageId, String script) implements ActionDefinition {

        @Override
        public Optional<Boolean> getBatch() {
            return Optional.empty();
        }

        @Override
        public Optional<BeforeResumeFunction> getBeforeResume() {
            return Optional.empty();
        }

        @Override
        public Optional<BeforeSuspendConsumer> getBeforeSuspend() {
            return Optional.empty();
        }

        @Override
        public Optional<BeforeTimeoutResumeFunction> getBeforeTimeoutResume() {
            return Optional.empty();
        }

        @Override
        public Optional<Boolean> getDeprecated() {
            return Optional.empty();
        }

        @Override
        public Optional<String> getDescription() {
            return Optional.ofNullable(description);
        }

        @Override
        public Optional<BasePerformFunction> getPerform() {
            return Optional.of(
                (PerformFunction) (inputParameters, connectionParameters, context) -> executePerform(
                    name, inputParameters, connectionParameters, context, languageId, script));
        }

        @Override
        public Optional<ProcessErrorResponseFunction> getProcessErrorResponse() {
            return Optional.empty();
        }

        @Override
        public Optional<Help> getHelp() {
            return Optional.empty();
        }

        @Override
        public Optional<Map<String, Object>> getMetadata() {
            return Optional.empty();
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Optional<OutputDefinition> getOutputDefinition() {
            return Optional.empty();
        }

        @Override
        public Optional<List<? extends Property>> getProperties() {
            return Optional.empty();
        }

        @Override
        public Optional<ResumePerformFunction> getResumePerform() {
            return Optional.empty();
        }

        @Override
        public Optional<SuspendPerformFunction> getSuspendPerform() {
            return Optional.empty();
        }

        @Override
        public Optional<String> getTitle() {
            return Optional.ofNullable(title);
        }

        @Override
        public Optional<WorkflowNodeDescriptionFunction> getWorkflowNodeDescription() {
            return Optional.empty();
        }
    }

    private record PolyglotComponentDefinition(
        String name, String title, String description, int version, List<ActionDefinition> actions)
        implements ComponentDefinition {

        @Override
        public Optional<List<ActionDefinition>> getActions() {
            return Optional.ofNullable(actions);
        }

        @Override
        public Optional<List<ComponentCategory>> getComponentCategories() {
            return Optional.empty();
        }

        @Override
        public Optional<List<ClusterElementDefinition<?>>> getClusterElements() {
            return Optional.empty();
        }

        @Override
        public Optional<ConnectionDefinition> getConnection() {
            return Optional.empty();
        }

        @Override
        public Optional<Boolean> getCustomAction() {
            return Optional.empty();
        }

        @Override
        public Optional<Help> getCustomActionHelp() {
            return Optional.empty();
        }

        @Override
        public Optional<String> getDescription() {
            return Optional.ofNullable(description);
        }

        @Override
        public Optional<String> getIcon() {
            return Optional.empty();
        }

        @Override
        public Optional<Map<String, Object>> getMetadata() {
            return Optional.empty();
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Optional<Resources> getResources() {
            return Optional.empty();
        }

        @Override
        public Optional<List<String>> getTags() {
            return Optional.empty();
        }

        @Override
        public Optional<String> getTitle() {
            return Optional.ofNullable(title);
        }

        @Override
        public Optional<List<TriggerDefinition>> getTriggers() {
            return Optional.empty();
        }

        @Override
        public Optional<UnifiedApiDefinition> getUnifiedApi() {
            return Optional.empty();
        }

        @Override
        public int getVersion() {
            return version;
        }
    }
}
