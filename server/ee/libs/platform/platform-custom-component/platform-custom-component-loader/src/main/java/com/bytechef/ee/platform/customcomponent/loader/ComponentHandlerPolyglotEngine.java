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
import com.bytechef.component.definition.ComponentDsl.ModifiableTriggerDefinition;
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
import com.bytechef.component.definition.TriggerDefinition.HttpHeaders;
import com.bytechef.component.definition.TriggerDefinition.HttpParameters;
import com.bytechef.component.definition.TriggerDefinition.PollOutput;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.component.definition.TriggerDefinition.WebhookBody;
import com.bytechef.component.definition.TriggerDefinition.WebhookEnableOutput;
import com.bytechef.component.definition.TriggerDefinition.WebhookMethod;
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

    /**
     * Evaluates the component's definition script. The script is user-supplied and its top level runs in full here, so
     * it is loaded through the same strict sandbox its perform functions use rather than a permissive context of its
     * own - the ceilings apply to declaring a component, not only to running one.
     */
    static ComponentHandler load(String languageId, String script) {
        return PolyglotSandbox.call(languageId, polyglotContext -> {
            Value value = polyglotContext.eval(languageId, script);

            String name = Objects.requireNonNull(getMember(value, "name", String.class));
            String title = getMember(value, "title", String.class);
            String description = getMember(value, "description", String.class);
            String icon = getMember(value, "icon", String.class);
            int version = Objects.requireNonNull(getMember(value, "version", Integer.class));
            List<Map<String, Object>> actions = getMemberTree(value, "actions");

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

            List<Map<String, Object>> triggers = getMemberTree(value, "triggers");

            List<TriggerDefinition> triggerDefinitions = toTriggerDefinitions(triggers, languageId, script);

            Map<String, Object> connectionMap = getMemberTree(value, "connection");

            ConnectionDefinition connectionDefinition = connectionMap == null
                ? null : toConnectionDefinition(connectionMap, languageId, script);

            return () -> new PolyglotComponentDefinition(
                name, title, description, icon, version, actionDefinitions, clusterElementDefinitions,
                triggerDefinitions, connectionDefinition);
        });
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

        return PolyglotSandbox.call(languageId, polyglotContext -> {
            Value value = polyglotContext.eval(languageId, script);

            Map<String, Object> connectionMap = getMemberTree(value, "connection");

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
        });
    }

    @SuppressWarnings("unchecked")
    private static Object executePerform(
        String actionName, Parameters inputParameters, Parameters connectionParameters, ActionContext context,
        String languageId, String script) {

        return PolyglotSandbox.call(languageId, polyglotContext -> {
            Value value = polyglotContext.eval(languageId, script);

            List<Map<String, Object>> tasks = getMemberTree(value, "actions");

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
        });
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

    private static <T> T getMember(Value value, String name, Class<T> valueClass) {
        value = value.getMember(name);

        return value == null ? null : value.as(valueClass);
    }

    /**
     * Walks a guest member into host-native collections.
     *
     * <p>
     * Walked rather than mapped with {@code Value.as(TypeLiteral)}: the perform contexts this runs in are built under a
     * {@link org.graalvm.polyglot.SandboxPolicy} that forbids host object mappings of mutable target types. The walk
     * keeps executable members, such as an action's {@code perform}, callable.
     */
    @SuppressWarnings("unchecked")
    private static <T> T getMemberTree(Value value, String name) {
        Value member = value.getMember(name);

        return member == null ? null : (T) PolyglotValues.copyToJavaValue(member);
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
     * Builds the component's triggers. {@code POLLING} and {@code STATIC_WEBHOOK} are supported: each has a single
     * execution function — {@code poll} and {@code webhookRequest} — which fits the same re-evaluate-per-invocation
     * pattern the action perform uses.
     *
     * <p>
     * {@code DYNAMIC_WEBHOOK} adds {@code webhookEnable} and {@code webhookDisable}: the platform hands the enable
     * function the webhook URL, keeps whatever parameters it returns, and passes them back on every request and on
     * disable. Each is still one function, so all three re-evaluate the script the same way.
     *
     * <p>
     * {@code LISTENER} remains rejected: enabling one opens a live, long-lived listener rather than making a call, and
     * a per-invocation sandbox has nowhere to keep it.
     */
    private static TriggerType toTriggerType(String triggerName, String type) {
        if (type == null || Objects.equals(type, TriggerType.POLLING.name())) {
            return TriggerType.POLLING;
        }

        if (Objects.equals(type, TriggerType.STATIC_WEBHOOK.name())) {
            return TriggerType.STATIC_WEBHOOK;
        }

        if (Objects.equals(type, TriggerType.DYNAMIC_WEBHOOK.name())) {
            return TriggerType.DYNAMIC_WEBHOOK;
        }

        throw new IllegalArgumentException(
            ("Trigger %s declares type %s; script components support POLLING, STATIC_WEBHOOK and DYNAMIC_WEBHOOK "
                + "triggers only — a LISTENER holds a live listener a per-invocation sandbox cannot keep")
                    .formatted(triggerName, type));
    }

    @SuppressWarnings("unchecked")
    private static List<TriggerDefinition> toTriggerDefinitions(
        List<Map<String, Object>> triggers, String languageId, String script) {

        if (triggers == null) {
            return List.of();
        }

        List<TriggerDefinition> triggerDefinitions = new ArrayList<>();

        for (Map<String, Object> trigger : triggers) {
            String triggerName = (String) trigger.get("name");
            String type = (String) trigger.get("type");

            TriggerType triggerType = toTriggerType(triggerName, type);

            ModifiableTriggerDefinition triggerDefinition = ComponentDsl.trigger(triggerName)
                .title((String) trigger.get("title"))
                .description((String) trigger.get("description"))
                .type(triggerType)
                .properties(
                    ComponentHandlerEspressoEngine
                        .toProperties((List<Map<String, ?>>) trigger.get("properties"))
                        .toArray(Property[]::new));

            if (triggerType == TriggerType.DYNAMIC_WEBHOOK) {
                triggerDefinition
                    .webhookEnable(
                        (inputParameters, connectionParameters, webhookUrl, workflowExecutionId, context) -> {
                            Object result = executeTriggerFunction(
                                languageId, script, triggerName, "webhookEnable", inputParameters,
                                connectionParameters, Map.of("webhookUrl", webhookUrl));

                            Map<String, ?> enableOutput = result instanceof Map<?, ?> resultMap
                                ? (Map<String, ?>) resultMap : Map.of();

                            return new WebhookEnableOutput(enableOutput, null);
                        })
                    .webhookDisable(
                        (
                            inputParameters, connectionParameters, webhookEnableOutputParameters, workflowExecutionId,
                            context) -> executeTriggerFunction(
                                languageId, script, triggerName, "webhookDisable", inputParameters,
                                connectionParameters, webhookEnableOutputParameters.toMap()));
            }

            if (triggerType == TriggerType.STATIC_WEBHOOK || triggerType == TriggerType.DYNAMIC_WEBHOOK) {
                triggerDefinition.webhookRequest(
                    (
                        inputParameters, connectionParameters, headers, parameters, body, method,
                        webhookEnableOutputParameters, context) -> executeWebhookRequest(
                            languageId, script, triggerName, inputParameters, connectionParameters, headers,
                            parameters, body, method));
            } else {
                triggerDefinition.poll(
                    (inputParameters, connectionParameters, closureParameters, context) -> executePoll(
                        languageId, script, triggerName, inputParameters, connectionParameters, closureParameters));
            }

            Map<String, ?> outputMap = (Map<String, ?>) trigger.get("output");
            Object sampleOutput = trigger.get("sampleOutput");

            if (outputMap != null && sampleOutput != null) {
                triggerDefinition.output(
                    ComponentDsl.outputSchema(ComponentHandlerEspressoEngine.toValueProperty(outputMap)),
                    ComponentDsl.sampleOutput(PolyglotValues.copyFromPolyglotContext(sampleOutput)));
            } else if (outputMap != null) {
                triggerDefinition.output(
                    ComponentDsl.outputSchema(ComponentHandlerEspressoEngine.toValueProperty(outputMap)));
            } else if (sampleOutput != null) {
                triggerDefinition.output(
                    ComponentDsl.sampleOutput(PolyglotValues.copyFromPolyglotContext(sampleOutput)));
            }

            triggerDefinitions.add(triggerDefinition);
        }

        return triggerDefinitions;
    }

    /**
     * Runs a named guest function on a trigger — the webhook enable/disable pair — with the third argument carrying
     * whatever that call needs: the webhook URL on enable, the parameters enable returned on disable.
     */
    @SuppressWarnings("unchecked")
    private static Object executeTriggerFunction(
        String languageId, String script, String triggerName, String functionName, Parameters inputParameters,
        Parameters connectionParameters, Map<String, ?> arguments) {

        return PolyglotSandbox.call(languageId, polyglotContext -> {
            Value value = polyglotContext.eval(languageId, script);

            Map<String, Object> trigger = findTrigger(value, triggerName);

            Function<Object[], Object> function = (Function<Object[], Object>) trigger.get(functionName);

            if (function == null) {
                throw new IllegalArgumentException(
                    "Trigger %s is a DYNAMIC_WEBHOOK but declares no %s function".formatted(triggerName, functionName));
            }

            Object result = function.apply(new Object[] {
                PolyglotValues.copyToGuestValue(inputParameters.toMap(), languageId),
                PolyglotValues.copyToGuestValue(
                    connectionParameters == null ? Map.of() : connectionParameters.toMap(), languageId),
                PolyglotValues.copyToGuestValue(arguments, languageId)
            });

            return PolyglotValues.copyFromPolyglotContext(result);
        });
    }

    private static Map<String, Object> findTrigger(Value value, String triggerName) {
        List<Map<String, Object>> triggers = getMemberTree(value, "triggers");

        return triggers.stream()
            .filter(trigger -> Objects.equals(trigger.get("name"), triggerName))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Trigger name=%s not found".formatted(triggerName)));
    }

    /**
     * Runs a STATIC_WEBHOOK trigger's {@code webhookRequest}. The guest sees the request as one plain object —
     * {@code {headers, parameters, body, method}} — rather than the host's four separate arguments, so a script reads
     * it the way it would any other map. Whatever it returns becomes the trigger's output.
     */
    @SuppressWarnings("unchecked")
    private static Object executeWebhookRequest(
        String languageId, String script, String triggerName, Parameters inputParameters,
        Parameters connectionParameters, HttpHeaders headers, HttpParameters parameters, WebhookBody body,
        WebhookMethod method) {

        return PolyglotSandbox.call(languageId, polyglotContext -> {
            Value value = polyglotContext.eval(languageId, script);

            Map<String, Object> triggerMap = findTrigger(value, triggerName);

            Function<Object[], Object> webhookRequest =
                (Function<Object[], Object>) triggerMap.get("webhookRequest");

            if (webhookRequest == null) {
                throw new IllegalArgumentException(
                    "Trigger %s is a STATIC_WEBHOOK but declares no webhookRequest function".formatted(triggerName));
            }

            Map<String, Object> request = new LinkedHashMap<>();

            request.put("headers", headers == null ? Map.of() : headers.toMap());
            request.put("parameters", parameters == null ? Map.of() : parameters.toMap());
            request.put("body", body == null ? null : body.getContent());
            request.put("method", method == null ? null : method.name());

            Object result = webhookRequest.apply(new Object[] {
                PolyglotValues.copyToGuestValue(inputParameters.toMap(), languageId),
                PolyglotValues.copyToGuestValue(
                    connectionParameters == null ? Map.of() : connectionParameters.toMap(), languageId),
                PolyglotValues.copyToGuestValue(request, languageId)
            });

            return PolyglotValues.copyFromPolyglotContext(result);
        });
    }

    private static PollOutput executePoll(
        String languageId, String script, String triggerName, Parameters inputParameters,
        Parameters connectionParameters, Parameters closureParameters) {

        return PolyglotSandbox.call(languageId, polyglotContext -> {
            Value value = polyglotContext.eval(languageId, script);

            Map<String, Object> trigger = findTrigger(value, triggerName);

            Function<Object[], Object> poll = (Function<Object[], Object>) trigger.get("poll");

            Object result = poll.apply(new Object[] {
                PolyglotValues.copyToGuestValue(inputParameters.toMap(), languageId),
                PolyglotValues.copyToGuestValue(connectionParameters.toMap(), languageId),
                PolyglotValues.copyToGuestValue(closureParameters.toMap(), languageId)
            });

            Object pollOutput = PolyglotValues.copyFromPolyglotContext(result);

            if (!(pollOutput instanceof Map<?, ?> pollOutputMap)) {
                throw new IllegalStateException(
                    "The poll function of trigger %s must return an object with a records member, got: %s"
                        .formatted(triggerName, pollOutput));
            }

            Object records = pollOutputMap.get("records");
            Object nextClosureParameters = pollOutputMap.get("closureParameters");

            return new PollOutput(
                records instanceof List<?> recordList ? recordList : List.of(),
                nextClosureParameters instanceof Map<?, ?> ? (Map<String, ?>) nextClosureParameters : Map.of(),
                Boolean.TRUE.equals(pollOutputMap.get("pollImmediately")));
        });
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
            // A DYNAMIC_PROPERTIES property produces its whole property list at edit time, so the shape of the form
            // can depend on what the user picked — a record type, a board, a custom-field set.
            if (Objects.equals(propertyMap.get("type"), "DYNAMIC_PROPERTIES")) {
                properties.add(toDynamicProperties(actionName, propertyMap, languageId, script));

                continue;
            }

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

    /**
     * Builds a DYNAMIC_PROPERTIES property whose {@code properties} function runs in a fresh strict sandbox and returns
     * ordinary property maps, which are then materialized the same way static ones are.
     */
    @SuppressWarnings("unchecked")
    private static Property toDynamicProperties(
        String actionName, Map<String, ?> propertyMap, String languageId, String script) {

        String propertyName = (String) propertyMap.get("name");

        ComponentDsl.ModifiableDynamicPropertiesProperty property = ComponentDsl.dynamicProperties(propertyName)
            .properties(
                (ActionDefinition.PropertiesFunction) (
                    inputParameters, connectionParameters, lookupDependsOnPaths,
                    context) -> ComponentHandlerEspressoEngine.toProperties(
                        (List<Map<String, ?>>) executePropertySeam(
                            languageId, script, actionName, propertyName, "properties", inputParameters,
                            connectionParameters))
                        .stream()
                        .map(curProperty -> (Property.ValueProperty<?>) curProperty)
                        .toList());

        if (propertyMap.get("propertiesLookupDependsOn") instanceof List<?> dependsOn) {
            property.propertiesLookupDependsOn(
                dependsOn.stream()
                    .map(String::valueOf)
                    .toArray(String[]::new));
        }

        return property;
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

    /**
     * Runs a property's named guest function — the DYNAMIC_PROPERTIES {@code properties} seam — in a fresh strict
     * sandbox, returning whatever it produced as plain Java collections.
     */
    @SuppressWarnings("unchecked")
    private static Object executePropertySeam(
        String languageId, String script, String actionName, String propertyName, String functionName,
        Parameters inputParameters, Parameters connectionParameters) {

        return PolyglotSandbox.call(languageId, polyglotContext -> {
            Value value = polyglotContext.eval(languageId, script);

            List<Map<String, Object>> actions = getMemberTree(value, "actions");

            Map<String, Object> action = actions.stream()
                .filter(curAction -> Objects.equals(curAction.get("name"), actionName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Action name=%s not found".formatted(actionName)));

            Map<String, Object> property = ((List<Map<String, Object>>) action.get("properties")).stream()
                .filter(curProperty -> Objects.equals(curProperty.get("name"), propertyName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Property name=%s not found".formatted(propertyName)));

            Function<Object[], Object> function = (Function<Object[], Object>) property.get(functionName);

            if (function == null) {
                throw new IllegalArgumentException(
                    "Property %s declares type DYNAMIC_PROPERTIES but no %s function"
                        .formatted(propertyName, functionName));
            }

            Object result = function.apply(new Object[] {
                PolyglotValues.copyToGuestValue(inputParameters.toMap(), languageId),
                PolyglotValues.copyToGuestValue(
                    connectionParameters == null ? Map.of() : connectionParameters.toMap(), languageId)
            });

            return PolyglotValues.copyFromPolyglotContext(result);
        });
    }

    private static List<Option<String>> executeOptionsSeam(
        String languageId, String script, String actionName, String propertyName, Parameters inputParameters,
        Parameters connectionParameters, String searchText) {

        return PolyglotSandbox.call(languageId, polyglotContext -> {
            Value value = polyglotContext.eval(languageId, script);

            List<Map<String, Object>> actions = getMemberTree(value, "actions");

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
        });
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
        List<ClusterElementDefinition<?>> clusterElements, List<TriggerDefinition> triggers,
        ConnectionDefinition connectionDefinition)
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
            return triggers == null ? List.of() : triggers;
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
