/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.platform.ai.tool.util;

import com.bytechef.component.definition.ComponentDsl.ModifiableValueProperty;
import com.bytechef.definition.BaseProperty.BaseValueProperty;
import com.bytechef.platform.component.constant.WorkflowConstants;
import com.bytechef.platform.component.definition.PropertyFactory;
import com.bytechef.platform.component.domain.ObjectProperty;
import com.bytechef.platform.component.domain.Property;
import com.bytechef.platform.component.domain.ValueProperty;
import com.bytechef.platform.util.SchemaUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Ivica Cardic
 */
public final class ToolParameterDefaultsUtils {

    private static final Logger log = LoggerFactory.getLogger(ToolParameterDefaultsUtils.class);

    private ToolParameterDefaultsUtils() {
    }

    public static Map<String, Object> collectPropertyDefaults(List<? extends Property> properties) {
        Map<String, Object> propertyDefaults = new LinkedHashMap<>();

        for (Property property : properties) {
            String name = property.getName();

            if (name == null || !(property instanceof ValueProperty<?> valueProperty)) {
                continue;
            }

            Object defaultValue = valueProperty.getDefaultValue();

            if (isExpression(defaultValue)) {
                defaultValue = null;
            }

            if (property instanceof ObjectProperty objectProperty) {
                defaultValue = getObjectDefaultValue(objectProperty, defaultValue);
            }

            if (defaultValue != null) {
                propertyDefaults.put(name, defaultValue);
            }
        }

        return propertyDefaults;
    }

    public static Map<String, Object> collectTriggerInputDefaults(@Nullable Map<String, ?> triggerParameters) {
        if (triggerParameters == null) {
            return Map.of();
        }

        Object inputSchemaValue = triggerParameters.get(WorkflowConstants.INPUT_SCHEMA);

        if (!(inputSchemaValue instanceof String inputSchema) || inputSchema.isBlank()) {
            return Map.of();
        }

        Property property;

        try {
            BaseValueProperty<?> inputProperty =
                SchemaUtils.getJsonSchemaProperty(inputSchema, PropertyFactory.JSON_SCHEMA_PROPERTY_FACTORY);

            if (!(inputProperty instanceof ModifiableValueProperty<?, ?> modifiableValueProperty)) {
                return Map.of();
            }

            property = Property.toProperty(modifiableValueProperty);
        } catch (RuntimeException exception) {
            log.warn("Unable to parse the callable trigger input schema", exception);

            return Map.of();
        }

        if (property instanceof ObjectProperty objectProperty) {
            return collectPropertyDefaults(objectProperty.getProperties());
        }

        return collectPropertyDefaults(List.of(property));
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> mergeParameters(
        Map<String, ?> baseParameters, Map<String, ?> overridingParameters) {

        Map<String, Object> mergedParameters = copyMap(baseParameters);

        for (Map.Entry<String, ?> entry : overridingParameters.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            Object baseValue = mergedParameters.get(key);

            if (value == null && baseValue != null) {
                continue;
            }

            if (baseValue instanceof Map<?, ?> baseMap && value instanceof Map<?, ?> overridingMap) {
                value = mergeParameters((Map<String, ?>) baseMap, (Map<String, ?>) overridingMap);
            }

            mergedParameters.put(key, value);
        }

        return mergedParameters;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> removeClearedDefaults(
        Map<String, ?> defaultParameters, Map<String, ?> savedParameters) {

        Map<String, Object> remainingDefaults = copyMap(defaultParameters);

        for (Map.Entry<String, ?> entry : savedParameters.entrySet()) {
            String key = entry.getKey();
            Object savedValue = entry.getValue();

            if (savedValue == null) {
                remainingDefaults.remove(key);

                continue;
            }

            if (savedValue instanceof Map<?, ?> savedMap &&
                remainingDefaults.get(key) instanceof Map<?, ?> defaultMap) {

                remainingDefaults.put(
                    key, removeClearedDefaults((Map<String, ?>) defaultMap, (Map<String, ?>) savedMap));
            }
        }

        return remainingDefaults;
    }

    private static Map<String, Object> copyMap(Map<?, ?> map) {
        Map<String, Object> copiedMap = new LinkedHashMap<>();

        for (Map.Entry<?, ?> entry : map.entrySet()) {
            copiedMap.put(String.valueOf(entry.getKey()), copyValue(entry.getValue()));
        }

        return copiedMap;
    }

    private static @Nullable Object copyValue(@Nullable Object value) {
        if (value instanceof Map<?, ?> map) {
            return copyMap(map);
        }

        if (value instanceof Collection<?> collection) {
            List<Object> copiedList = new ArrayList<>();

            for (Object item : collection) {
                copiedList.add(copyValue(item));
            }

            return copiedList;
        }

        return value;
    }

    private static @Nullable Object getObjectDefaultValue(
        ObjectProperty objectProperty, @Nullable Object defaultValue) {

        Map<String, Object> nestedDefaults = collectPropertyDefaults(objectProperty.getProperties());

        if (defaultValue instanceof Map<?, ?> defaultValueMap) {
            Map<String, Object> objectDefaults = new LinkedHashMap<>(nestedDefaults);

            for (Map.Entry<?, ?> entry : defaultValueMap.entrySet()) {
                objectDefaults.put(String.valueOf(entry.getKey()), entry.getValue());
            }

            return objectDefaults;
        }

        if (defaultValue == null && !nestedDefaults.isEmpty()) {
            return nestedDefaults;
        }

        return defaultValue;
    }

    private static boolean isExpression(@Nullable Object value) {
        return value instanceof String string && string.startsWith("=");
    }
}
