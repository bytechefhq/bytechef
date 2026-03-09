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

package com.bytechef.platform.workflow.validator;

import com.bytechef.platform.workflow.validator.model.PropertyInfo;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Centralized utility class for property navigation, finding, and path operations. Consolidates property-related
 * operations scattered across multiple classes.
 *
 * @author Marko Kriskovic
 */
class PropertyUtils {

    private PropertyUtils() {
    }

    /**
     * Builds a property path by combining parent path and property name.
     */
    public static String buildPropertyPath(@Nullable String parentPath, String propertyName) {
        if (parentPath == null || parentPath.isEmpty()) {
            return propertyName;
        }

        return parentPath + "." + propertyName;
    }

    /**
     * Extracts the property name from the end of a property path.
     */
    public static String extractPropertyNameFromPath(String propertyPath) {
        if (propertyPath.contains(".")) {
            String[] parts = propertyPath.split("\\.");

            return parts[parts.length - 1];
        }

        return propertyPath;
    }

    /**
     * Finds a property by name in a PropertyInfo structure.
     */
    public static @Nullable PropertyInfo findPropertyByName(@Nullable PropertyInfo parentProperty, String targetName) {
        if (parentProperty == null || parentProperty.nestedProperties() == null) {
            return null;
        }

        if (targetName.equals(parentProperty.name())) {
            return parentProperty;
        }

        for (PropertyInfo nested : parentProperty.nestedProperties()) {
            if (targetName.equals(nested.name())) {
                return nested;
            }
        }

        return null;
    }

    /**
     * Checks if a property exists in the given PropertyInfo structure.
     */
    public static boolean checkPropertyExists(PropertyInfo outputInfo, String propertyName) {
        return checkPropertyExistsRecursive(outputInfo, propertyName.split("\\."));
    }

    /**
     * Gets the type of a property from the given PropertyInfo structure.
     */
    public static @Nullable String getPropertyType(PropertyInfo outputInfo, String propertyName) {
        return getPropertyTypeRecursive(outputInfo, propertyName.split("\\."));
    }

    private static boolean checkPropertyExistsRecursive(PropertyInfo outputInfo, String[] propertyPath) {
        if (propertyPath.length == 0) {
            return true;
        }

        String currentProperty = propertyPath[0];

        if (currentProperty.contains("[") && currentProperty.endsWith("]")) {
            return checkArrayPropertyExists(outputInfo, currentProperty, propertyPath);
        }

        if (currentProperty.equals(outputInfo.name())) {
            return checkCurrentPropertyExists(outputInfo, propertyPath);
        }

        return checkNestedPropertyExists(outputInfo, currentProperty, propertyPath);
    }

    private static boolean checkArrayPropertyExists(
        PropertyInfo outputInfo, String currentProperty, String[] propertyPath) {

        String arrayName = currentProperty.substring(0, currentProperty.indexOf('['));

        List<PropertyInfo> propertyInfos = outputInfo.nestedProperties();

        boolean anyMatch = propertyInfos != null &&
            propertyInfos
                .stream()
                .anyMatch(prop -> arrayName.equals(prop.name()));

        if (arrayName.equals(outputInfo.name()) || anyMatch) {
            PropertyInfo arrayPropertyInfo = findArrayProperty(outputInfo, arrayName);

            if (arrayPropertyInfo != null && "ARRAY".equals(arrayPropertyInfo.type())) {
                List<PropertyInfo> propertyInfos1 = arrayPropertyInfo.nestedProperties();

                if (propertyInfos1 != null && !propertyInfos1.isEmpty()) {

                    PropertyInfo propertyInfo = propertyInfos1.getFirst();

                    if (propertyPath.length == 1) {
                        return true;
                    }

                    String[] remainingPath = createRemainingPath(propertyPath);

                    return checkPropertyExistsRecursive(propertyInfo, remainingPath);
                }
            }
        }

        return false;
    }

    private static @Nullable PropertyInfo findArrayProperty(PropertyInfo outputInfo, String arrayName) {
        if (arrayName.equals(outputInfo.name())) {
            return outputInfo;
        } else if (outputInfo.nestedProperties() != null) {
            return outputInfo.nestedProperties()
                .stream()
                .filter(prop -> arrayName.equals(prop.name()))
                .findFirst()
                .orElse(null);
        }

        return null;
    }

    private static boolean checkCurrentPropertyExists(PropertyInfo outputInfo, String[] propertyPath) {
        if (propertyPath.length == 1) {
            return true;
        }

        if (outputInfo.nestedProperties() != null) {
            String[] remainingPath = createRemainingPath(propertyPath);

            for (PropertyInfo nestedProp : outputInfo.nestedProperties()) {
                if (checkPropertyExistsRecursive(nestedProp, remainingPath)) {
                    return true;
                }
            }
        }

        return false;
    }

    private static boolean checkNestedPropertyExists(
        PropertyInfo outputPropertyInfo, String currentProperty, String[] propertyPath) {

        if (outputPropertyInfo.nestedProperties() != null) {
            for (PropertyInfo nestedProp : outputPropertyInfo.nestedProperties()) {
                if (currentProperty.equals(nestedProp.name())) {
                    if (propertyPath.length == 1) {
                        return true;
                    }

                    if (nestedProp.nestedProperties() != null) {
                        String[] remainingPath = createRemainingPath(propertyPath);

                        for (PropertyInfo deepNestedProp : nestedProp.nestedProperties()) {
                            if (checkPropertyExistsRecursive(deepNestedProp, remainingPath)) {
                                return true;
                            }
                        }
                    }

                    return false;
                }
            }
        }
        return false;
    }

    private static @Nullable String getPropertyTypeRecursive(PropertyInfo outputInfo, String[] propertyPath) {
        if (propertyPath.length == 0) {
            return outputInfo.type();
        }

        String currentProperty = propertyPath[0];

        if (currentProperty.contains("[") && currentProperty.endsWith("]")) {
            return getArrayPropertyType(outputInfo, currentProperty, propertyPath);
        }

        if (currentProperty.equals(outputInfo.name())) {
            return getCurrentPropertyType(outputInfo, propertyPath);
        }

        return getNestedPropertyType(outputInfo, currentProperty, propertyPath);
    }

    private static @Nullable String getArrayPropertyType(
        PropertyInfo outputPropertyInfo, String currentProperty, String[] propertyPath) {

        String arrayName = currentProperty.substring(0, currentProperty.indexOf('['));

        List<PropertyInfo> propertyInfos = outputPropertyInfo.nestedProperties();

        boolean anyMatch = propertyInfos != null &&
            propertyInfos
                .stream()
                .anyMatch(prop -> arrayName.equals(prop.name()));

        if (arrayName.equals(outputPropertyInfo.name()) || anyMatch) {
            PropertyInfo arrayPropertyInfo = findArrayProperty(outputPropertyInfo, arrayName);

            if (arrayPropertyInfo != null && "ARRAY".equals(arrayPropertyInfo.type())) {
                List<PropertyInfo> nestedPropertyInfos = arrayPropertyInfo.nestedProperties();

                if (nestedPropertyInfos != null && !nestedPropertyInfos.isEmpty()) {
                    PropertyInfo propertyInfo = nestedPropertyInfos.getFirst();

                    if (propertyPath.length == 1) {
                        return propertyInfo.type();
                    }

                    String[] remainingPath = createRemainingPath(propertyPath);

                    return getPropertyTypeRecursive(propertyInfo, remainingPath);
                }
            }
        }

        return null;
    }

    private static @Nullable String getCurrentPropertyType(PropertyInfo outputPropertyInfo, String[] propertyPath) {
        if (propertyPath.length == 1) {
            return outputPropertyInfo.type();
        }

        if (outputPropertyInfo.nestedProperties() != null) {
            String[] remainingPath = createRemainingPath(propertyPath);

            for (PropertyInfo propertyInfo : outputPropertyInfo.nestedProperties()) {
                String result = getPropertyTypeRecursive(propertyInfo, remainingPath);

                if (result != null) {
                    return result;
                }
            }
        }

        return null;
    }

    @Nullable
    private static String getNestedPropertyType(
        PropertyInfo outputPropertyInfo, String currentProperty, String[] propertyPath) {

        if (outputPropertyInfo.nestedProperties() != null) {
            for (PropertyInfo propertyInfo : outputPropertyInfo.nestedProperties()) {
                if (currentProperty.equals(propertyInfo.name())) {
                    if (propertyPath.length == 1) {
                        return propertyInfo.type();
                    }

                    if (propertyInfo.nestedProperties() != null) {
                        String[] remainingPath = createRemainingPath(propertyPath);

                        for (PropertyInfo deepNestedProp : propertyInfo.nestedProperties()) {
                            String result = getPropertyTypeRecursive(deepNestedProp, remainingPath);

                            if (result != null) {
                                return result;
                            }
                        }
                    }

                    return null;
                }
            }
        }

        return null;
    }

    private static String[] createRemainingPath(String[] propertyPath) {
        String[] remainingPath = new String[propertyPath.length - 1];

        System.arraycopy(propertyPath, 1, remainingPath, 0, propertyPath.length - 1);

        return remainingPath;
    }
}
