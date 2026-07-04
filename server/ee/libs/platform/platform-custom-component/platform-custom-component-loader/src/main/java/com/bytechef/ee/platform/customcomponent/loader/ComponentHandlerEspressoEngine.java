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
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableArrayProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableBooleanProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableIntegerProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableNumberProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableObjectProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableStringProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableTriggerDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableValueProperty;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.platform.component.definition.AbstractComponentDefinitionWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.PolyglotAccess;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.io.IOAccess;

/**
 * Loads and executes Java custom components inside a GraalVM Espresso guest JVM. The guest side of the bridge (see the
 * {@code platform-custom-component-guest-bridge} module) walks the component definition and returns it as one JSON
 * string; this class rebuilds a host-side {@link ComponentDefinition} with {@link ComponentDsl} and routes every action
 * perform back into a fresh guest context, exposing the live host {@link ActionContext} through the narrow
 * {@link HostContextBridge} callback.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class ComponentHandlerEspressoEngine {

    private static final String GUEST_BRIDGE_CLASS_NAME =
        "com.bytechef.ee.platform.customcomponent.guest.GuestComponentBridge";
    private static final String HOST_BRIDGE_BINDING_NAME = "byteChefHostBridge";

    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder()
        .findAndAddModules()
        .build();

    private static Engine engine;

    static ComponentHandler load(Path jarPath) {
        String implClassName = readServiceImplementationClassName(jarPath);
        String icon = readIcon(jarPath);

        String definitionJson;

        try (Context polyglotContext = getJavaContext(jarPath)) {
            Value bindings = polyglotContext.getBindings("java");

            Value bridgeClass = bindings.getMember(GUEST_BRIDGE_CLASS_NAME);

            if (bridgeClass == null) {
                throw new IllegalStateException(
                    "Guest bridge class %s is not present on the guest classpath".formatted(GUEST_BRIDGE_CLASS_NAME));
            }

            Value definitionValue = bridgeClass.invokeMember("describeComponent", implClassName);

            definitionJson = definitionValue.asString();
        }

        ComponentDefinition componentDefinition = toComponentDefinition(
            definitionJson, jarPath, implClassName, icon);

        return () -> componentDefinition;
    }

    @SuppressWarnings("unchecked")
    static ComponentDefinition toComponentDefinition(
        String definitionJson, Path jarPath, String implClassName, String icon) {

        Map<String, ?> component = readMap(definitionJson);

        List<ModifiableActionDefinition> actionDefinitions = new ArrayList<>();

        for (Map<String, ?> action : (List<Map<String, ?>>) component.get("actions")) {
            String actionName = (String) action.get("name");

            ModifiableActionDefinition actionDefinition = ComponentDsl.action(actionName);

            applyIfPresent(action, "title", title -> actionDefinition.title((String) title));
            applyIfPresent(action, "description", description -> actionDefinition.description((String) description));

            actionDefinition.properties(toProperties((List<Map<String, ?>>) action.get("properties")));

            actionDefinition.perform(
                (ActionDefinition.PerformFunction) (
                    inputParameters, connectionParameters,
                    context) -> executePerform(
                        jarPath, implClassName, actionName, inputParameters, connectionParameters, context));

            actionDefinitions.add(actionDefinition);
        }

        List<ModifiableTriggerDefinition> triggerDefinitions = new ArrayList<>();

        for (Map<String, ?> trigger : (List<Map<String, ?>>) component.get("triggers")) {
            ModifiableTriggerDefinition triggerDefinition = ComponentDsl.trigger((String) trigger.get("name"));

            applyIfPresent(trigger, "title", title -> triggerDefinition.title((String) title));
            applyIfPresent(trigger, "description", description -> triggerDefinition.description((String) description));

            triggerDefinition.type(TriggerType.valueOf((String) trigger.get("type")));
            triggerDefinition.properties(
                toProperties((List<Map<String, ?>>) trigger.get("properties")).toArray(Property[]::new));

            triggerDefinitions.add(triggerDefinition);
        }

        ComponentDsl.ModifiableComponentDefinition componentDefinition = ComponentDsl.component(
            (String) component.get("name"));

        applyIfPresent(component, "title", title -> componentDefinition.title((String) title));
        applyIfPresent(component, "description", description -> componentDefinition.description((String) description));

        componentDefinition.version(((Number) component.get("version")).intValue());
        componentDefinition.actions(actionDefinitions);
        componentDefinition.triggers(triggerDefinitions);

        return new IconComponentDefinitionWrapper(componentDefinition, icon);
    }

    private static Object executePerform(
        Path jarPath, String implClassName, String actionName, Parameters inputParameters,
        Parameters connectionParameters, ActionContext context) {

        try (Context polyglotContext = getJavaContext(jarPath)) {
            Value polyglotBindings = polyglotContext.getPolyglotBindings();

            polyglotBindings.putMember(HOST_BRIDGE_BINDING_NAME, new HostContextBridge(context));

            Value bindings = polyglotContext.getBindings("java");

            Value bridgeClass = bindings.getMember(GUEST_BRIDGE_CLASS_NAME);

            Value resultValue = bridgeClass.invokeMember(
                "executeActionPerform", implClassName, actionName,
                writeJson(inputParameters.toMap()), writeJson(connectionParameters.toMap()), traceId(context));

            return readValue(resultValue.asString());
        }
    }

    private static String traceId(ActionContext context) {
        try {
            return context.getTraceId();
        } catch (RuntimeException e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static List<Property> toProperties(List<Map<String, ?>> propertyMaps) {
        List<Property> properties = new ArrayList<>();

        if (propertyMaps == null) {
            return properties;
        }

        for (Map<String, ?> propertyMap : propertyMaps) {
            properties.add(toProperty(propertyMap));
        }

        return properties;
    }

    @SuppressWarnings("unchecked")
    private static Property toProperty(Map<String, ?> propertyMap) {
        String name = (String) propertyMap.get("name");
        String type = (String) propertyMap.get("type");

        ModifiableValueProperty<?, ?> property = switch (type) {
            case "STRING" -> {
                ModifiableStringProperty stringProperty = ComponentDsl.string(name);

                applyIfPresent(
                    propertyMap, "minLength", minLength -> stringProperty.minLength(((Number) minLength).intValue()));
                applyIfPresent(
                    propertyMap, "maxLength", maxLength -> stringProperty.maxLength(((Number) maxLength).intValue()));
                applyIfPresent(propertyMap, "regex", regex -> stringProperty.regex((String) regex));
                applyIfPresent(
                    propertyMap, "defaultValue", defaultValue -> stringProperty.defaultValue((String) defaultValue));
                applyIfPresent(
                    propertyMap, "options",
                    options -> stringProperty.options(toStringOptions((List<Map<String, ?>>) options)));

                yield stringProperty;
            }
            case "INTEGER" -> {
                ModifiableIntegerProperty integerProperty = ComponentDsl.integer(name);

                applyIfPresent(
                    propertyMap, "minValue", minValue -> integerProperty.minValue(((Number) minValue).longValue()));
                applyIfPresent(
                    propertyMap, "maxValue", maxValue -> integerProperty.maxValue(((Number) maxValue).longValue()));
                applyIfPresent(
                    propertyMap, "defaultValue",
                    defaultValue -> integerProperty.defaultValue(((Number) defaultValue).longValue()));
                applyIfPresent(
                    propertyMap, "options",
                    options -> integerProperty.options(toLongOptions((List<Map<String, ?>>) options)));

                yield integerProperty;
            }
            case "NUMBER" -> {
                ModifiableNumberProperty numberProperty = ComponentDsl.number(name);

                applyIfPresent(
                    propertyMap, "minValue", minValue -> numberProperty.minValue(((Number) minValue).doubleValue()));
                applyIfPresent(
                    propertyMap, "maxValue", maxValue -> numberProperty.maxValue(((Number) maxValue).doubleValue()));
                applyIfPresent(
                    propertyMap, "defaultValue",
                    defaultValue -> numberProperty.defaultValue(((Number) defaultValue).doubleValue()));
                applyIfPresent(
                    propertyMap, "options",
                    options -> numberProperty.options(toDoubleOptions((List<Map<String, ?>>) options)));

                yield numberProperty;
            }
            case "BOOLEAN" -> {
                ModifiableBooleanProperty booleanProperty = ComponentDsl.bool(name);

                applyIfPresent(
                    propertyMap, "defaultValue",
                    defaultValue -> booleanProperty.defaultValue((Boolean) defaultValue));

                yield booleanProperty;
            }
            case "DATE" -> ComponentDsl.date(name);
            case "DATE_TIME" -> ComponentDsl.dateTime(name);
            case "TIME" -> ComponentDsl.time(name);
            case "NULL" -> ComponentDsl.nullable(name);
            case "OBJECT" -> {
                ModifiableObjectProperty objectProperty = ComponentDsl.object(name);

                applyIfPresent(
                    propertyMap, "properties", children -> objectProperty.properties(
                        toValueProperties((List<Map<String, ?>>) children)));
                applyIfPresent(
                    propertyMap, "additionalProperties", children -> objectProperty.additionalProperties(
                        toValueProperties((List<Map<String, ?>>) children)));

                yield objectProperty;
            }
            case "ARRAY" -> {
                ModifiableArrayProperty arrayProperty = ComponentDsl.array(name);

                applyIfPresent(
                    propertyMap, "items", items -> arrayProperty.items(
                        toValueProperties((List<Map<String, ?>>) items)));
                applyIfPresent(
                    propertyMap, "minItems", minItems -> arrayProperty.minItems(((Number) minItems).longValue()));
                applyIfPresent(
                    propertyMap, "maxItems", maxItems -> arrayProperty.maxItems(((Number) maxItems).longValue()));
                applyIfPresent(
                    propertyMap, "multipleValues",
                    multipleValues -> arrayProperty.multipleValues((Boolean) multipleValues));

                yield arrayProperty;
            }
            case "FILE_ENTRY" -> ComponentDsl.fileEntry(name);
            default -> throw new IllegalArgumentException("Unsupported property type: " + type);
        };

        applyIfPresent(propertyMap, "label", label -> property.label((String) label));
        applyIfPresent(propertyMap, "description", description -> property.description((String) description));
        applyIfPresent(propertyMap, "placeholder", placeholder -> property.placeholder((String) placeholder));
        applyIfPresent(propertyMap, "required", required -> property.required((Boolean) required));
        applyIfPresent(
            propertyMap, "advancedOption", advancedOption -> property.advancedOption((Boolean) advancedOption));
        applyIfPresent(
            propertyMap, "displayCondition",
            displayCondition -> property.displayCondition((String) displayCondition));
        applyIfPresent(propertyMap, "hidden", hidden -> property.hidden((Boolean) hidden));

        return property;
    }

    private static List<ModifiableValueProperty<?, ?>> toValueProperties(List<Map<String, ?>> propertyMaps) {
        List<ModifiableValueProperty<?, ?>> properties = new ArrayList<>();

        for (Map<String, ?> propertyMap : propertyMaps) {
            properties.add((ModifiableValueProperty<?, ?>) toProperty(propertyMap));
        }

        return properties;
    }

    private static List<Option<String>> toStringOptions(List<Map<String, ?>> optionMaps) {
        List<Option<String>> options = new ArrayList<>();

        for (Map<String, ?> optionMap : optionMaps) {
            options.add(ComponentDsl.option((String) optionMap.get("label"), (String) optionMap.get("value")));
        }

        return options;
    }

    private static List<Option<Long>> toLongOptions(List<Map<String, ?>> optionMaps) {
        List<Option<Long>> options = new ArrayList<>();

        for (Map<String, ?> optionMap : optionMaps) {
            Number value = (Number) optionMap.get("value");

            options.add(ComponentDsl.option((String) optionMap.get("label"), value.longValue()));
        }

        return options;
    }

    @SuppressWarnings("unchecked")
    private static Option<Double>[] toDoubleOptions(List<Map<String, ?>> optionMaps) {
        List<Option<Double>> options = new ArrayList<>();

        for (Map<String, ?> optionMap : optionMaps) {
            Number value = (Number) optionMap.get("value");

            options.add(ComponentDsl.option((String) optionMap.get("label"), value.doubleValue()));
        }

        return options.toArray(Option[]::new);
    }

    private static Context getJavaContext(Path jarPath) {
        if (engine == null) {
            engine = Engine.create();
        }

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
                .option("java.Classpath", jarPath + java.io.File.pathSeparator + GuestBridgeClasspath.get())
                .build();
        } catch (RuntimeException e) {
            throw new IllegalStateException(
                "Failed to create a GraalVM Espresso context. Embedded Espresso (org.graalvm.polyglot:java) supports " +
                    "linux (amd64, aarch64) and darwin-amd64 as of GraalVM 25; it cannot boot on this platform.",
                e);
        }
    }

    private static String readServiceImplementationClassName(Path jarPath) {
        String serviceEntryName = "META-INF/services/" + ComponentHandler.class.getName();

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

    private static String readIcon(Path jarPath) {
        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            JarEntry jarEntry = jarFile.getJarEntry("assets/sample.svg");

            if (jarEntry == null) {
                return null;
            }

            try (InputStream inputStream = jarFile.getInputStream(jarEntry)) {
                return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void applyIfPresent(Map<String, ?> map, String key, java.util.function.Consumer<Object> setter) {
        Object value = map.get(key);

        if (value != null) {
            setter.accept(value);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, ?> readMap(String json) {
        try {
            return OBJECT_MAPPER.readValue(json, Map.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Object readValue(String json) {
        try {
            return OBJECT_MAPPER.readValue(json, Object.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String writeJson(Object value) {
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static class IconComponentDefinitionWrapper extends AbstractComponentDefinitionWrapper {

        private final String icon;

        private IconComponentDefinitionWrapper(ComponentDefinition componentDefinition, String icon) {
            super(componentDefinition);

            this.icon = icon;
        }

        @Override
        public Optional<String> getIcon() {
            return Optional.ofNullable(icon);
        }
    }
}
