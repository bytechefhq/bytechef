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

package com.bytechef.platform.configuration.facade;

import com.bytechef.evaluator.Evaluator;
import com.bytechef.platform.component.domain.ArrayProperty;
import com.bytechef.platform.component.domain.ObjectProperty;
import com.bytechef.platform.domain.BaseProperty;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Resolves the required properties of a workflow node that are visible under their display conditions but have no
 * value.
 *
 * @author Ivica Cardic
 */
final class RequiredPropertyUtils {

    private RequiredPropertyUtils() {
    }

    static Set<String> getMissingRequiredProperties(
        List<?> properties, Map<String, ?> parameterMap, Evaluator evaluator) {

        Set<String> missingRequiredProperties = new HashSet<>();

        checkRequiredProperties(
            properties, parameterMap, "", missingRequiredProperties, parameterMap, List.of(), evaluator);

        return missingRequiredProperties;
    }

    static boolean evaluate(String displayCondition, Map<String, ?> inputParameters, Evaluator evaluator) {
        try {
            Map<String, Object> result = evaluator.evaluate(
                Map.of("displayCondition", "=" + displayCondition), inputParameters, true);

            Object displayConditionResult = result.get("displayCondition");

            return displayConditionResult instanceof Boolean bool && bool;
        } catch (RuntimeException runtimeException) {
            return false;
        }
    }

    static String replaceIndexes(String expression, List<Integer> indexes) {
        Integer lastIndex = null;

        for (Integer index : indexes) {
            expression = expression.replaceFirst("\\[index]", "[" + index + "]");
            lastIndex = index;
        }

        if (lastIndex != null && expression.contains("[index]")) {
            expression = expression.replace("[index]", "[" + lastIndex + "]");
        }

        return expression;
    }

    private static void checkRequiredProperties(
        List<?> properties, Map<?, ?> parameterMap, String prefix, Set<String> missingRequiredProperties,
        Map<String, ?> rootParameterMap, List<Integer> indexes, Evaluator evaluator) {

        for (Object prop : properties) {
            if (!(prop instanceof BaseProperty property)) {
                continue;
            }

            if (isHidden(property, rootParameterMap, indexes, evaluator)) {
                continue;
            }

            String propertyName = property.getName();

            String propertyPath = prefix.isEmpty() ? propertyName : prefix + "." + propertyName;

            if (property.getRequired() && isRequiredValueMissing(parameterMap, propertyName)) {
                missingRequiredProperties.add(propertyPath);
            } else if (parameterMap.containsKey(propertyName)) {
                // Check nested properties
                if (property instanceof ObjectProperty objectProperty) {
                    List<?> nestedProperties = objectProperty.getProperties();

                    if (nestedProperties != null && !nestedProperties.isEmpty()) {
                        Object value = parameterMap.get(propertyName);

                        if (value instanceof Map) {
                            checkRequiredProperties(
                                nestedProperties, (Map<?, ?>) value, propertyPath, missingRequiredProperties,
                                rootParameterMap, indexes, evaluator);
                        }
                    }
                } else if (property instanceof ArrayProperty arrayProperty) {
                    List<?> items = arrayProperty.getItems();
                    if (items != null && !items.isEmpty()) {
                        Object value = parameterMap.get(propertyName);

                        if (value instanceof List<?> list) {
                            for (int i = 0; i < list.size(); i++) {
                                Object item = list.get(i);

                                if (item instanceof Map) {
                                    List<Integer> itemIndexes = new ArrayList<>(indexes);

                                    itemIndexes.add(i);

                                    checkRequiredProperties(
                                        items, (Map<?, ?>) item, propertyPath + "[" + i + "]",
                                        missingRequiredProperties, rootParameterMap, itemIndexes, evaluator);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private static boolean isHidden(
        BaseProperty property, Map<String, ?> rootParameterMap, List<Integer> indexes, Evaluator evaluator) {

        String displayCondition = property.getDisplayCondition();

        if (displayCondition == null || displayCondition.isEmpty()) {
            return false;
        }

        if (displayCondition.contains("[index]")) {
            displayCondition = replaceIndexes(displayCondition, indexes);
        }

        return !evaluate(displayCondition, rootParameterMap, evaluator);
    }

    private static boolean isRequiredValueMissing(Map<?, ?> parameterMap, String propertyName) {
        if (!parameterMap.containsKey(propertyName)) {
            return true;
        }

        Object value = parameterMap.get(propertyName);

        return value == null || (value instanceof String string && string.isBlank());
    }
}
