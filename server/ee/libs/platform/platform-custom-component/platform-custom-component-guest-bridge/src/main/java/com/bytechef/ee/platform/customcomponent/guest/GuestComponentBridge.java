/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.customcomponent.guest;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.definition.DynamicOptionsProperty;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.OptionsProperty;
import com.bytechef.component.definition.Property;
import com.bytechef.component.definition.TriggerDefinition;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Entry points invoked by the host over polyglot interop. Both methods exchange plain JSON strings so the host↔guest
 * interop surface stays minimal: the definition JSON is
 * {@code {name, title, description, version, actions: [{name, title, description, properties: [property...]}],
 * triggers: [{name, title, description, type, properties}], unsupported: [string...]}} where each property is
 * {@code {name, type, controlType, label, description, placeholder, displayCondition, required, advancedOption,
 * hidden, expressionEnabled, defaultValue, exampleValue, options, minValue, maxValue, minLength, maxLength, regex,
 * languageId, minItems, maxItems, multipleValues, properties, items, additionalProperties}} (absent keys omitted).
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class GuestComponentBridge {

    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder()
        .findAndAddModules()
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .build();

    /**
     * Test seam: plain-JVM unit tests set this to a fake so the Espresso-only polyglot import is bypassed. Production
     * code never writes it.
     */
    @SuppressFBWarnings("UWF_UNWRITTEN_FIELD")
    static HostBridge hostBridgeOverride;

    private GuestComponentBridge() {
    }

    public static String describeComponent(String implClassName) {
        try {
            ComponentDefinition componentDefinition = loadDefinition(implClassName);

            List<String> unsupported = new ArrayList<>();

            Map<String, Object> component = new LinkedHashMap<>();

            component.put("name", componentDefinition.getName());
            componentDefinition.getTitle()
                .ifPresent(title -> component.put("title", title));
            componentDefinition.getDescription()
                .ifPresent(description -> component.put("description", description));
            component.put("version", componentDefinition.getVersion());

            if (componentDefinition.getConnection()
                .isPresent()) {

                Map<String, Object> connection = toConnectionMap(
                    componentDefinition.getConnection()
                        .get(),
                    unsupported);

                if (connection != null) {
                    component.put("connection", connection);
                }
            }

            if (!componentDefinition.getClusterElements()
                .isEmpty()) {

                unsupported.add("clusterElements");
            }

            if (componentDefinition.getUnifiedApi()
                .isPresent()) {

                unsupported.add("unifiedApi");
            }

            List<Map<String, Object>> actions = new ArrayList<>();

            for (ActionDefinition actionDefinition : componentDefinition.getActions()) {

                Map<String, Object> action = new LinkedHashMap<>();

                action.put("name", actionDefinition.getName());
                actionDefinition.getTitle()
                    .ifPresent(title -> action.put("title", title));
                actionDefinition.getDescription()
                    .ifPresent(description -> action.put("description", description));
                action.put(
                    "properties",
                    toPropertyMaps(
                        actionDefinition.getProperties(),
                        unsupported, "action " + actionDefinition.getName()));

                actions.add(action);
            }

            component.put("actions", actions);

            List<Map<String, Object>> triggers = new ArrayList<>();

            for (TriggerDefinition triggerDefinition : componentDefinition.getTriggers()) {

                Map<String, Object> trigger = new LinkedHashMap<>();

                trigger.put("name", triggerDefinition.getName());
                triggerDefinition.getTitle()
                    .ifPresent(title -> trigger.put("title", title));
                triggerDefinition.getDescription()
                    .ifPresent(description -> trigger.put("description", description));

                TriggerDefinition.TriggerType triggerType = triggerDefinition.getType();

                trigger.put("type", triggerType.name());
                trigger.put(
                    "properties",
                    toPropertyMaps(
                        triggerDefinition.getProperties(),
                        unsupported, "trigger " + triggerDefinition.getName()));

                triggers.add(trigger);

                unsupported.add("trigger " + triggerDefinition.getName() + " execution");
            }

            component.put("triggers", triggers);
            component.put("unsupported", unsupported);

            return OBJECT_MAPPER.writeValueAsString(component);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String executeActionPerform(
        String implClassName, String actionName, String inputParametersJson, String connectionParametersJson,
        String traceId) {

        try {
            ComponentDefinition componentDefinition = loadDefinition(implClassName);

            ActionDefinition actionDefinition = componentDefinition.getActions()
                .stream()
                .filter(action -> actionName.equals(action.getName()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Action name=%s not found".formatted(actionName)));

            ActionDefinition.BasePerformFunction basePerformFunction = actionDefinition.getPerform()
                .orElseThrow(() -> new IllegalArgumentException(
                    "Action name=%s does not define a perform function".formatted(actionName)));

            if (!(basePerformFunction instanceof ActionDefinition.PerformFunction performFunction)) {
                throw new UnsupportedOperationException(
                    "Only standard perform functions are supported in the Espresso custom component sandbox; set " +
                        "bytechef.component.custom-component.java-loader=class-loader to use the in-process loader");
            }

            GuestParameters inputParameters = new GuestParameters(readMap(inputParametersJson));
            GuestParameters connectionParameters = new GuestParameters(readMap(connectionParametersJson));

            GuestActionContext guestActionContext = new GuestActionContext(resolveHostBridge(), traceId);

            Object result = performFunction.apply(inputParameters, connectionParameters, guestActionContext);

            return OBJECT_MAPPER.writeValueAsString(result);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Serializes a connection definition's STATIC shape. Seam functions are invoked with {@code null} arguments to
     * extract their constants — a lambda that actually reads its parameters (a dynamic URL) throws, and the whole
     * connection is reported unsupported instead: functions cannot cross the guest boundary as data. Custom
     * {@code apply}/{@code acquire} functions are unsupported for the same reason. The broad catch is that fallback
     * contract — ANY extraction failure downgrades to unsupported rather than failing the describe.
     */
    @SuppressFBWarnings("REC_CATCH_EXCEPTION")
    private static Map<String, Object> toConnectionMap(
        com.bytechef.component.definition.ConnectionDefinition connectionDefinition, List<String> unsupported) {

        try {
            Map<String, Object> connection = new LinkedHashMap<>();

            if (connectionDefinition.getBaseUri()
                .isPresent()) {

                connection.put(
                    "baseUri",
                    connectionDefinition.getBaseUri()
                        .get()
                        .apply(null, null));
            }

            List<Map<String, Object>> authorizations = new ArrayList<>();

            for (com.bytechef.component.definition.Authorization authorization : connectionDefinition
                .getAuthorizations()) {

                if (authorization.getApply()
                    .isPresent()
                    || authorization.getAcquire()
                        .isPresent()) {

                    throw new UnsupportedOperationException(
                        "Custom apply/acquire functions cannot cross the guest boundary");
                }

                Map<String, Object> authorizationMap = new LinkedHashMap<>();

                com.bytechef.component.definition.Authorization.AuthorizationType authorizationType =
                    authorization.getType();

                authorizationMap.put("type", authorizationType.name());

                if (authorization.getAuthorizationUrl()
                    .isPresent()) {

                    authorizationMap.put(
                        "authorizationUrl",
                        authorization.getAuthorizationUrl()
                            .get()
                            .apply(null, null));
                }

                if (authorization.getTokenUrl()
                    .isPresent()) {

                    authorizationMap.put(
                        "tokenUrl",
                        authorization.getTokenUrl()
                            .get()
                            .apply(null, null));
                }

                if (authorization.getRefreshUrl()
                    .isPresent()) {

                    authorizationMap.put(
                        "refreshUrl",
                        authorization.getRefreshUrl()
                            .get()
                            .apply(null, null));
                }

                if (authorization.getScopes()
                    .isPresent()) {

                    Map<String, Boolean> scopes = authorization.getScopes()
                        .get()
                        .apply(null, null);

                    authorizationMap.put(
                        "scopes",
                        scopes.entrySet()
                            .stream()
                            .filter(Map.Entry::getValue)
                            .map(Map.Entry::getKey)
                            .toList());
                }

                List<Map<String, Object>> propertyMaps = toPropertyMaps(
                    authorization.getProperties(), unsupported,
                    "connection authorization " + authorizationType.name());

                if (!propertyMaps.isEmpty()) {
                    authorizationMap.put("properties", propertyMaps);
                }

                authorizations.add(authorizationMap);
            }

            connection.put("authorizations", authorizations);

            List<Map<String, Object>> connectionPropertyMaps = toPropertyMaps(
                connectionDefinition.getProperties(), unsupported, "connection");

            if (!connectionPropertyMaps.isEmpty()) {
                connection.put("properties", connectionPropertyMaps);
            }

            return connection;
        } catch (Exception e) {
            unsupported.add("connection");

            return null;
        }
    }

    private static ComponentDefinition loadDefinition(String implClassName) throws ReflectiveOperationException {
        Class<?> handlerClass = Class.forName(implClassName);

        ComponentHandler componentHandler = (ComponentHandler) handlerClass.getDeclaredConstructor()
            .newInstance();

        return componentHandler.getDefinition();
    }

    private static HostBridge resolveHostBridge() {
        if (hostBridgeOverride != null) {
            return hostBridgeOverride;
        }

        Object imported = com.oracle.truffle.espresso.polyglot.Polyglot.importObject("byteChefHostBridge");

        return new ForeignHostBridge(imported);
    }

    /**
     * {@link HostBridge} view over the foreign (host) callback object; every call goes through the Espresso guest
     * interop API since foreign objects cannot be cast to guest interfaces directly.
     */
    private record ForeignHostBridge(Object foreignObject) implements HostBridge {

        @Override
        public String httpExecute(String requestJson) {
            try {
                Object result = com.oracle.truffle.espresso.polyglot.Interop.invokeMember(
                    foreignObject, "httpExecute", requestJson);

                return com.oracle.truffle.espresso.polyglot.Interop.asString(result);
            } catch (com.oracle.truffle.espresso.polyglot.InteropException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public boolean isEditorEnvironment() {
            try {
                Object result = com.oracle.truffle.espresso.polyglot.Interop.invokeMember(
                    foreignObject, "isEditorEnvironment");

                return com.oracle.truffle.espresso.polyglot.Interop.asBoolean(result);
            } catch (com.oracle.truffle.espresso.polyglot.InteropException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void log(String level, String message, String exceptionMessage) {
            try {
                com.oracle.truffle.espresso.polyglot.Interop.invokeMember(
                    foreignObject, "log", level, message, exceptionMessage);
            } catch (com.oracle.truffle.espresso.polyglot.InteropException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, ?> readMap(String json) throws Exception {
        if (json == null || json.isBlank()) {
            return Map.of();
        }

        Map<String, ?> map = OBJECT_MAPPER.readValue(json, Map.class);

        return map == null ? Map.of() : map;
    }

    private static List<Map<String, Object>> toPropertyMaps(
        List<? extends Property> properties, List<String> unsupported, String owner) {

        List<Map<String, Object>> propertyMaps = new ArrayList<>();

        for (Property property : properties) {
            Map<String, Object> propertyMap = toPropertyMap(property, unsupported, owner);

            if (propertyMap != null) {
                propertyMaps.add(propertyMap);
            }
        }

        return propertyMaps;
    }

    private static Map<String, Object> toPropertyMap(Property property, List<String> unsupported, String owner) {
        Property.Type propertyType = property.getType();

        if (propertyType == Property.Type.DYNAMIC_PROPERTIES) {
            unsupported.add("%s dynamic properties %s".formatted(owner, property.getName()));

            return null;
        }

        Map<String, Object> propertyMap = new LinkedHashMap<>();

        propertyMap.put("name", property.getName());
        propertyMap.put("type", propertyType.name());

        property.getDescription()
            .ifPresent(description -> propertyMap.put("description", description));
        property.getDisplayCondition()
            .ifPresent(displayCondition -> propertyMap.put("displayCondition", displayCondition));

        propertyMap.put("required", property.getRequired());

        propertyMap.put("advancedOption", property.getAdvancedOption());
        propertyMap.put("hidden", property.getHidden());
        propertyMap.put("expressionEnabled", property.getExpressionEnabled());

        if (property instanceof Property.ValueProperty<?> valueProperty) {
            valueProperty.getLabel()
                .ifPresent(label -> propertyMap.put("label", label));
            valueProperty.getPlaceholder()
                .ifPresent(placeholder -> propertyMap.put("placeholder", placeholder));
            valueProperty.getDefaultValue()
                .ifPresent(defaultValue -> propertyMap.put("defaultValue", defaultValue));
            valueProperty.getExampleValue()
                .ifPresent(exampleValue -> propertyMap.put("exampleValue", exampleValue));

            Property.ControlType controlType = valueProperty.getControlType();

            if (controlType != null) {
                propertyMap.put("controlType", controlType.name());
            }
        }

        if (property instanceof OptionsProperty<?> optionsProperty) {
            var options = optionsProperty.getOptions();

            if (!options.isEmpty()) {
                propertyMap.put("options", toOptionMaps(options));
            }
        }

        if (property instanceof DynamicOptionsProperty<?> dynamicOptionsProperty
            && dynamicOptionsProperty.getOptionsDataSource()
                .isPresent()) {

            unsupported.add("%s dynamic options for property %s".formatted(owner, property.getName()));
        }

        if (property instanceof Property.StringProperty stringProperty) {
            stringProperty.getMinLength()
                .ifPresent(minLength -> propertyMap.put("minLength", minLength));
            stringProperty.getMaxLength()
                .ifPresent(maxLength -> propertyMap.put("maxLength", maxLength));
            stringProperty.getRegex()
                .ifPresent(regex -> propertyMap.put("regex", regex));
            stringProperty.getLanguageId()
                .ifPresent(languageId -> propertyMap.put("languageId", languageId));
        } else if (property instanceof Property.IntegerProperty integerProperty) {
            integerProperty.getMinValue()
                .ifPresent(minValue -> propertyMap.put("minValue", minValue));
            integerProperty.getMaxValue()
                .ifPresent(maxValue -> propertyMap.put("maxValue", maxValue));
        } else if (property instanceof Property.NumberProperty numberProperty) {
            numberProperty.getMinValue()
                .ifPresent(minValue -> propertyMap.put("minValue", minValue));
            numberProperty.getMaxValue()
                .ifPresent(maxValue -> propertyMap.put("maxValue", maxValue));
        } else if (property instanceof Property.ObjectProperty objectProperty) {
            List<? extends Property.ValueProperty<?>> properties = objectProperty.getProperties();

            if (!properties.isEmpty()) {
                propertyMap.put("properties", toPropertyMaps(properties, unsupported, owner));
            }

            List<? extends Property.ValueProperty<?>> additionalProperties = objectProperty.getAdditionalProperties();

            if (!additionalProperties.isEmpty()) {
                propertyMap.put("additionalProperties", toPropertyMaps(additionalProperties, unsupported, owner));
            }
        } else if (property instanceof Property.ArrayProperty arrayProperty) {
            List<? extends Property.ValueProperty<?>> items = arrayProperty.getItems();

            if (!items.isEmpty()) {
                propertyMap.put("items", toPropertyMaps(items, unsupported, owner));
            }

            arrayProperty.getMinItems()
                .ifPresent(minItems -> propertyMap.put("minItems", minItems));
            arrayProperty.getMaxItems()
                .ifPresent(maxItems -> propertyMap.put("maxItems", maxItems));
            propertyMap.put("multipleValues", arrayProperty.getMultipleValues());
        } else if (property instanceof Property.FileEntryProperty fileEntryProperty) {
            propertyMap.put("properties", toPropertyMaps(fileEntryProperty.getProperties(), unsupported, owner));
        }

        return propertyMap;
    }

    private static List<Map<String, Object>> toOptionMaps(List<? extends Option<?>> options) {
        List<Map<String, Object>> optionMaps = new ArrayList<>();

        for (Option<?> option : options) {
            Map<String, Object> optionMap = new LinkedHashMap<>();

            optionMap.put("label", option.getLabel());
            optionMap.put("value", option.getValue());

            option.getDescription()
                .ifPresent(description -> optionMap.put("description", description));

            optionMaps.add(optionMap);
        }

        return optionMaps;
    }
}
