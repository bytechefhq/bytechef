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
import com.bytechef.component.definition.Authorization;
import com.bytechef.component.definition.Authorization.ApplyResponse;
import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.ComponentDsl.ModifiableAuthorization;
import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableIntegerProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableNumberProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableStringProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableValueProperty;
import com.bytechef.component.definition.ConnectionDefinition;
import com.bytechef.component.definition.Help;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.OutputDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property;
import com.bytechef.component.definition.PropertyGroup;
import com.bytechef.component.definition.Resources;
import com.bytechef.component.definition.TriggerDefinition;
import com.bytechef.component.definition.UnifiedApiDefinition;
import com.bytechef.platform.component.polyglot.PolyglotSandbox;
import com.bytechef.platform.component.polyglot.PolyglotValues;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.TypeLiteral;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class ComponentHandlerPolyglotEngine {

    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder()
        .findAndAddModules()
        .build();

    private static Engine engine;

    // A dedicated engine for the strict-sandbox perform contexts. GraalVM caches host-access interop info
    // per engine, so building a strict `HostAccess.NONE` context and the permissive definition-loading
    // context on the SAME engine corrupts that cache; keeping them on separate engines avoids the clash
    // while leaving definition loading (`getContext()`/`engine`) untouched.
    private static Engine performEngine;

    static ComponentHandler load(String languageId, String script) {
        if (engine == null) {
            engine = Engine.create();
        }

        try (Context polyglotContext = getContext()) {
            Value value = polyglotContext.eval(languageId, script);

            String name = Objects.requireNonNull(getMember(value, "name", String.class));
            String title = getMember(value, "title", String.class);
            String description = getMember(value, "description", String.class);
            String icon = getMember(value, "icon", String.class);
            int version = Objects.requireNonNull(getMember(value, "version", Integer.class));
            List<Map<String, Object>> actions = getMember(value, "actions", new TypeLiteral<>() {});

            List<ActionDefinition> actionDefinitions = toActionDefinitions(actions, languageId, script);

            // A tool IS an action — same properties, output and perform — so an action opts in with `tool: true`
            // and ComponentDsl.tool(...) adapts it into a TOOLS cluster element the AI Agent can call.
            List<ClusterElementDefinition<?>> clusterElementDefinitions = new ArrayList<>();

            for (ActionDefinition actionDefinition : actionDefinitions) {
                if (actionDefinition instanceof PolyglotActionDefinition polyglotActionDefinition &&
                    polyglotActionDefinition.tool()) {

                    clusterElementDefinitions.add(ComponentDsl.tool(actionDefinition));
                }
            }

            Map<String, Object> connectionMap = getMember(value, "connection", new TypeLiteral<>() {});

            ConnectionDefinition connectionDefinition = connectionMap == null
                ? null : toConnectionDefinition(connectionMap, languageId, script);

            return () -> new PolyglotComponentDefinition(
                name, title, description, icon, version, actionDefinitions, clusterElementDefinitions,
                connectionDefinition);
        }
    }

    /**
     * Materializes the declarative {@code connection} member into a host-side {@link ConnectionDefinition}. Static
     * values become constant-returning seam functions — the guest script never executes during an OAuth flow for them;
     * {@code platform-oauth2} runs the whole flow against the constants. Guest-function seam values
     * ({@code authorizationUrl}/{@code tokenUrl}/{@code refreshUrl}/{@code apply}) are wrapped in host functions that
     * re-evaluate the script in a fresh strict-sandbox context per invocation, the same pattern the perform path uses.
     */
    @SuppressWarnings("unchecked")
    private static ConnectionDefinition toConnectionDefinition(
        Map<String, Object> connectionMap, String languageId, String script) {

        ModifiableConnectionDefinition connectionDefinition = ComponentDsl.connection();

        if (connectionMap.get("baseUri") instanceof String baseUri) {
            connectionDefinition.baseUri((connectionParameters, context) -> baseUri);
        }

        List<Property> connectionProperties = ComponentHandlerEspressoEngine.toProperties(
            (List<Map<String, ?>>) connectionMap.get("properties"));

        List<Map<String, Object>> authorizationMaps = (List<Map<String, Object>>) connectionMap.get("authorizations");

        List<ModifiableAuthorization> authorizations = new ArrayList<>();

        if (authorizationMaps != null) {
            for (int authorizationIndex = 0; authorizationIndex < authorizationMaps.size(); authorizationIndex++) {
                authorizations.add(
                    toAuthorization(
                        authorizationMaps.get(authorizationIndex), authorizationIndex, connectionProperties,
                        languageId, script));
            }
        } else if (!connectionProperties.isEmpty()) {
            // Properties without any authorization still need a carrier the platform understands.
            ModifiableAuthorization authorization = ComponentDsl.authorization(
                Authorization.AuthorizationType.CUSTOM);

            authorization.properties(connectionProperties);

            authorizations.add(authorization);
        }

        if (!authorizations.isEmpty()) {
            connectionDefinition.authorizations(authorizations.toArray(ModifiableAuthorization[]::new));
        }

        return connectionDefinition;
    }

    @SuppressWarnings("unchecked")
    private static ModifiableAuthorization toAuthorization(
        Map<String, Object> authorizationMap, int authorizationIndex, List<Property> connectionProperties,
        String languageId, String script) {

        String type = (String) authorizationMap.get("type");

        ModifiableAuthorization authorization = ComponentDsl.authorization(
            Authorization.AuthorizationType.valueOf(type.toUpperCase(Locale.ROOT)));

        Object authorizationUrl = authorizationMap.get("authorizationUrl");

        if (authorizationUrl instanceof String constantUrl) {
            authorization.authorizationUrl((connectionParameters, context) -> constantUrl);
        } else if (authorizationUrl != null) {
            authorization.authorizationUrl((connectionParameters, context) -> executeUrlSeam(
                languageId, script, authorizationIndex, "authorizationUrl", connectionParameters));
        }

        Object tokenUrl = authorizationMap.get("tokenUrl");

        if (tokenUrl instanceof String constantUrl) {
            authorization.tokenUrl((connectionParameters, context) -> constantUrl);
        } else if (tokenUrl != null) {
            authorization.tokenUrl((connectionParameters, context) -> executeUrlSeam(
                languageId, script, authorizationIndex, "tokenUrl", connectionParameters));
        }

        Object refreshUrl = authorizationMap.get("refreshUrl");

        if (refreshUrl instanceof String constantUrl) {
            authorization.refreshUrl((connectionParameters, context) -> constantUrl);
        } else if (refreshUrl != null) {
            authorization.refreshUrl((connectionParameters, context) -> executeUrlSeam(
                languageId, script, authorizationIndex, "refreshUrl", connectionParameters));
        }

        List<String> scopes = (List<String>) authorizationMap.get("scopes");

        if (scopes != null) {
            Map<String, Boolean> scopeMap = new LinkedHashMap<>();

            for (String scope : scopes) {
                scopeMap.put(scope, true);
            }

            authorization.scopes((connectionParameters, context) -> scopeMap);
        }

        if (authorizationMap.get("apply") != null) {
            authorization.apply((connectionParameters, context) -> executeApplySeam(
                languageId, script, authorizationIndex, connectionParameters));
        }

        List<Property> authorizationProperties = ComponentHandlerEspressoEngine.toProperties(
            (List<Map<String, ?>>) authorizationMap.get("properties"));

        if (!authorizationProperties.isEmpty()) {
            authorization.properties(authorizationProperties);
        } else if (!connectionProperties.isEmpty()) {
            authorization.properties(connectionProperties);
        }

        return authorization;
    }

    private static String executeUrlSeam(
        String languageId, String script, int authorizationIndex, String seamName, Parameters connectionParameters) {

        Object result = executeConnectionSeamFunction(
            languageId, script, authorizationIndex, seamName, connectionParameters);

        if (!(result instanceof String string)) {
            throw new IllegalStateException(
                "Connection authorization %s function must return a string, got: %s".formatted(seamName, result));
        }

        return string;
    }

    @SuppressWarnings("unchecked")
    private static ApplyResponse executeApplySeam(
        String languageId, String script, int authorizationIndex, Parameters connectionParameters) {

        Object result = executeConnectionSeamFunction(
            languageId, script, authorizationIndex, "apply", connectionParameters);

        if (!(result instanceof Map)) {
            throw new IllegalStateException(
                "Connection authorization apply function must return an object with headers and/or queryParameters " +
                    "members, got: " + result);
        }

        Map<String, ?> resultMap = (Map<String, ?>) result;

        ApplyResponse applyResponse = ApplyResponse.ofHeaders(toStringMultimap(resultMap.get("headers")));

        Map<String, List<String>> queryParameters = toStringMultimap(resultMap.get("queryParameters"));

        if (queryParameters != null) {
            Map<String, List<String>> applyResponseQueryParameters = applyResponse.getQueryParameters();

            applyResponseQueryParameters.putAll(queryParameters);
        }

        return applyResponse;
    }

    private static Map<String, List<String>> toStringMultimap(Object value) {
        if (value == null) {
            return null;
        }

        if (!(value instanceof Map)) {
            throw new IllegalStateException(
                "Connection authorization apply function members must be objects, got: " + value);
        }

        Map<String, List<String>> stringMultimap = new LinkedHashMap<>();

        for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
            Object entryValue = entry.getValue();

            List<String> values;

            if (entryValue instanceof List<?> list) {
                values = list.stream()
                    .map(String::valueOf)
                    .toList();
            } else {
                values = List.of(String.valueOf(entryValue));
            }

            stringMultimap.put(String.valueOf(entry.getKey()), values);
        }

        return stringMultimap;
    }

    @SuppressWarnings("unchecked")
    private static Object executeConnectionSeamFunction(
        String languageId, String script, int authorizationIndex, String seamName, Parameters connectionParameters) {

        if (performEngine == null) {
            performEngine = Engine.create();
        }

        try (Context polyglotContext = PolyglotSandbox.newContext(performEngine, languageId)) {
            Value value = polyglotContext.eval(languageId, script);

            Map<String, Object> connectionMap = getMember(value, "connection", new TypeLiteral<>() {});

            List<Map<String, Object>> authorizationMaps =
                (List<Map<String, Object>>) connectionMap.get("authorizations");

            Function<Object[], Object> seamFunction = (Function<Object[], Object>) authorizationMaps
                .get(authorizationIndex)
                .get(seamName);

            Object connectionParametersMap = connectionParameters == null
                ? PolyglotValues.copyToGuestValue(Map.of(), languageId)
                : PolyglotValues.copyToGuestValue(connectionParameters.toMap(), languageId);

            Object result = seamFunction.apply(new Object[] {
                connectionParametersMap
            });

            return PolyglotValues.copyFromPolyglotContext(result);
        }
    }

    @SuppressWarnings("unchecked")
    private static Object executePerform(
        String actionName, Parameters inputParameters, Parameters connectionParameters, ActionContext context,
        String languageId, String script) {

        if (performEngine == null) {
            performEngine = Engine.create();
        }

        try (Context polyglotContext = PolyglotSandbox.newContext(performEngine, languageId)) {
            Value value = polyglotContext.eval(languageId, script);

            List<Map<String, Object>> tasks = getMember(value, "actions", new TypeLiteral<>() {});

            for (Map<String, Object> task : tasks) {
                if (actionName.equals(task.get("name"))) {
                    Function<Object[], Object> perform = (Function<Object[], Object>) task.get("perform");

                    Object guestInputParameters = PolyglotValues.copyToGuestValue(
                        inputParameters.toMap(), languageId);
                    Object guestConnectionParameters = PolyglotValues.copyToGuestValue(
                        connectionParameters.toMap(), languageId);
                    ProxyObject guestContext = toGuestContext(context, languageId);

                    Object result = perform.apply(new Object[] {
                        guestInputParameters, guestConnectionParameters, guestContext
                    });

                    // The guest function's return value may be a live view backed by the polyglot context (e.g. a
                    // JS object mapped to a PolyglotMap), which becomes unusable once the context closes below.
                    // Copy it into plain Java collections while the context is still open.
                    return PolyglotValues.copyFromPolyglotContext(result);
                }
            }

            throw new IllegalArgumentException("Action name=%s not found".formatted(actionName));
        }
    }

    /**
     * Builds the guest-facing {@code context} argument handed to a custom component's {@code perform} function. Only
     * {@code http} and {@code log} are exposed — custom components deliberately do not get component-invocation
     * capability.
     */
    private static ProxyObject toGuestContext(ActionContext context, String languageId) {
        HostContextBridge hostContextBridge = new HostContextBridge(context);

        return ProxyObject.fromMap(Map.of(
            "http", (ProxyExecutable) arguments -> executeHttp(hostContextBridge, arguments, languageId),
            "log", (ProxyExecutable) arguments -> executeLog(hostContextBridge, arguments)));
    }

    private static Object executeHttp(HostContextBridge hostContextBridge, Value[] arguments, String languageId) {
        Object request = PolyglotValues.copyToJavaValue(arguments[0]);

        String responseJson = hostContextBridge.httpExecute(toJson(request));

        return PolyglotValues.copyToGuestValue(fromJson(responseJson), languageId);
    }

    private static Object executeLog(HostContextBridge hostContextBridge, Value[] arguments) {
        String level = arguments[0].asString();
        String message = arguments[1].asString();

        hostContextBridge.log(level, message, null);

        return null;
    }

    private static String toJson(Object value) {
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> fromJson(String json) {
        try {
            return OBJECT_MAPPER.readValue(json, Map.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
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

        return member == null ? null : member.as(typeLiteral);
    }

    @SuppressWarnings("unchecked")
    private static List<ActionDefinition> toActionDefinitions(
        List<Map<String, Object>> actions, String languageId, String script) {

        if (actions == null) {
            return List.of();
        }

        return actions.stream()
            .map(task -> (ActionDefinition) new PolyglotActionDefinition(
                (String) task.get("name"), (String) task.get("title"), (String) task.get("description"),
                toActionProperties(
                    (String) task.get("name"), (List<Map<String, ?>>) task.get("properties"), languageId, script),
                toOutputDefinition((Map<String, ?>) task.get("output"), task.get("sampleOutput")),
                Boolean.TRUE.equals(task.get("tool")), languageId, script))
            .toList();
    }

    /**
     * Builds an action's input properties. A property whose {@code options} member is a guest FUNCTION rather than a
     * static list gets a host options function that re-evaluates the script per lookup — the same pattern the
     * connection's url/apply seams use — so a dropdown can be populated from a live API call.
     */
    @SuppressWarnings("unchecked")
    private static List<Property> toActionProperties(
        String actionName, List<Map<String, ?>> propertyMaps, String languageId, String script) {

        if (propertyMaps == null) {
            return List.of();
        }

        List<Property> properties = new ArrayList<>();

        for (Map<String, ?> propertyMap : propertyMaps) {
            Object options = propertyMap.get("options");

            if (options == null || options instanceof List) {
                properties.add(ComponentHandlerEspressoEngine.toValueProperty(propertyMap));

                continue;
            }

            Map<String, Object> staticPropertyMap = new LinkedHashMap<>(propertyMap);

            staticPropertyMap.remove("options");

            properties.add(
                withDynamicOptions(
                    ComponentHandlerEspressoEngine.toValueProperty(staticPropertyMap), actionName,
                    (String) propertyMap.get("name"), languageId, script));
        }

        return properties;
    }

    private static Property withDynamicOptions(
        ModifiableValueProperty<?, ?> property, String actionName, String propertyName, String languageId,
        String script) {

        ActionDefinition.OptionsFunction<String> optionsFunction =
            (inputParameters, connectionParameters, lookupDependsOnPaths, searchText, context) -> executeOptionsSeam(
                languageId, script, actionName, propertyName, inputParameters, connectionParameters, searchText);

        if (property instanceof ModifiableStringProperty stringProperty) {
            return stringProperty.options(optionsFunction);
        }

        if (property instanceof ModifiableIntegerProperty integerProperty) {
            return integerProperty.options(optionsFunction);
        }

        if (property instanceof ModifiableNumberProperty numberProperty) {
            return numberProperty.options(optionsFunction);
        }

        throw new IllegalArgumentException(
            "Property %s of action %s declares an options function, which only STRING, INTEGER and NUMBER properties "
                .formatted(propertyName, actionName) + "support");
    }

    @SuppressWarnings("unchecked")
    private static List<Option<String>> executeOptionsSeam(
        String languageId, String script, String actionName, String propertyName, Parameters inputParameters,
        Parameters connectionParameters, String searchText) {

        if (performEngine == null) {
            performEngine = Engine.create();
        }

        try (Context polyglotContext = PolyglotSandbox.newContext(performEngine, languageId)) {
            Value value = polyglotContext.eval(languageId, script);

            List<Map<String, Object>> actions = getMember(value, "actions", new TypeLiteral<>() {});

            Map<String, Object> action = actions.stream()
                .filter(curAction -> Objects.equals(curAction.get("name"), actionName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Action name=%s not found".formatted(actionName)));

            Map<String, Object> property = ((List<Map<String, Object>>) action.get("properties")).stream()
                .filter(curProperty -> Objects.equals(curProperty.get("name"), propertyName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Property name=%s not found".formatted(propertyName)));

            Function<Object[], Object> optionsFunction = (Function<Object[], Object>) property.get("options");

            Object result = optionsFunction.apply(new Object[] {
                PolyglotValues.copyToGuestValue(inputParameters.toMap(), languageId),
                PolyglotValues.copyToGuestValue(connectionParameters.toMap(), languageId), searchText
            });

            Object options = PolyglotValues.copyFromPolyglotContext(result);

            if (!(options instanceof List)) {
                throw new IllegalStateException(
                    "The options function of property %s must return a list, got: %s".formatted(propertyName, options));
            }

            List<Option<String>> optionList = new ArrayList<>();

            for (Object option : (List<?>) options) {
                if (option instanceof Map<?, ?> optionMap) {
                    Object optionValue = optionMap.get("value");

                    optionList.add(
                        ComponentDsl.option(
                            String.valueOf(optionMap.get("label")),
                            optionValue == null ? null : String.valueOf(optionValue)));
                } else {
                    optionList.add(ComponentDsl.option(String.valueOf(option), String.valueOf(option)));
                }
            }

            return optionList;
        }
    }

    /**
     * Builds an action's output definition from the declarative {@code output} property map (the shape downstream nodes
     * read for their data pills) and/or a {@code sampleOutput} value. Without either, the action publishes no output
     * schema, exactly as before.
     */
    private static OutputDefinition toOutputDefinition(Map<String, ?> outputMap, Object sampleOutput) {
        if (outputMap == null && sampleOutput == null) {
            return null;
        }

        if (outputMap == null) {
            return OutputDefinition.of(PolyglotValues.copyFromPolyglotContext(sampleOutput));
        }

        ModifiableValueProperty<?, ?> outputSchema = ComponentHandlerEspressoEngine.toValueProperty(outputMap);

        if (sampleOutput == null) {
            return OutputDefinition.of(outputSchema);
        }

        return OutputDefinition.of(outputSchema, PolyglotValues.copyFromPolyglotContext(sampleOutput));
    }

    @SuppressFBWarnings("EI")
    private record PolyglotActionDefinition(
        String name, String title, String description, List<Property> properties,
        OutputDefinition outputDefinition, boolean tool, String languageId, String script)
        implements ActionDefinition {

        @Override
        public boolean getBatch() {
            return false;
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
        public boolean getDeprecated() {
            return false;
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
        public Map<String, Object> getMetadata() {
            return Map.of();
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Optional<OutputDefinition> getOutputDefinition() {
            return Optional.ofNullable(outputDefinition);
        }

        @Override
        public List<? extends Property> getProperties() {
            return properties;
        }

        @Override
        public Optional<ResumePerformFunction> getResumePerform() {
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
        String name, String title, String description, String icon, int version, List<ActionDefinition> actions,
        List<ClusterElementDefinition<?>> clusterElements, ConnectionDefinition connectionDefinition)
        implements ComponentDefinition {

        @Override
        public List<ActionDefinition> getActions() {
            return actions == null ? List.of() : actions;
        }

        @Override
        public List<ComponentCategory> getComponentCategories() {
            return List.of();
        }

        @Override
        public List<ClusterElementDefinition<?>> getClusterElements() {
            return clusterElements == null ? List.of() : clusterElements;
        }

        @Override
        public Optional<ConnectionDefinition> getConnection() {
            return Optional.ofNullable(connectionDefinition);
        }

        @Override
        public boolean getCustomAction() {
            return false;
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
            return Optional.ofNullable(icon);
        }

        @Override
        public List<? extends PropertyGroup> getInputs() {
            return List.of();
        }

        @Override
        public Map<String, Object> getMetadata() {
            return Map.of();
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
        public List<String> getTags() {
            return List.of();
        }

        @Override
        public Optional<String> getTitle() {
            return Optional.ofNullable(title);
        }

        @Override
        public List<TriggerDefinition> getTriggers() {
            return List.of();
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
