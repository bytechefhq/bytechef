/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.customcomponent.loader;

import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.ComponentDsl.ModifiableArrayProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableBooleanProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableIntegerProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableNumberProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableObjectProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableStringProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableValueProperty;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Property;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
final class ComponentPropertyConverter {

    private ComponentPropertyConverter() {
    }

    @SuppressWarnings("unchecked")
    static List<Property> toProperties(List<Map<String, ?>> propertyMaps) {
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
    static ModifiableValueProperty<?, ?> toValueProperty(Map<String, ?> propertyMap) {
        return (ModifiableValueProperty<?, ?>) toProperty(propertyMap);
    }

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
        applyIfPresent(
            propertyMap, "expressionEnabled",
            expressionEnabled -> property.expressionEnabled((Boolean) expressionEnabled));

        applyIfPresent(
            propertyMap, "controlType", controlType -> applyControlType(property, (String) controlType));

        applyIfPresent(
            propertyMap, "optionsLookupDependsOn",
            optionsLookupDependsOn -> applyOptionsLookupDependsOn(property, optionsLookupDependsOn));

        return property;
    }

    private static void applyControlType(ModifiableValueProperty<?, ?> property, String controlType) {
        Property.ControlType type = Property.ControlType.valueOf(controlType);

        if (property instanceof ModifiableStringProperty stringProperty) {
            stringProperty.controlType(type);
        }
    }

    private static void applyOptionsLookupDependsOn(
        ModifiableValueProperty<?, ?> property, Object optionsLookupDependsOn) {

        if (!(optionsLookupDependsOn instanceof List<?> dependsOnList)) {
            return;
        }

        String[] dependsOn = dependsOnList.stream()
            .map(String::valueOf)
            .toArray(String[]::new);

        switch (property) {
            case ModifiableStringProperty stringProperty -> stringProperty.optionsLookupDependsOn(dependsOn);
            case ModifiableIntegerProperty integerProperty -> integerProperty.optionsLookupDependsOn(dependsOn);
            case ModifiableNumberProperty numberProperty -> numberProperty.optionsLookupDependsOn(dependsOn);
            case ModifiableArrayProperty arrayProperty -> arrayProperty.optionsLookupDependsOn(dependsOn);
            default -> {
            }
        }
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

    private static void applyIfPresent(Map<String, ?> map, String key, Consumer<Object> setter) {
        Object value = map.get(key);

        if (value != null) {
            setter.accept(value);
        }
    }
}
