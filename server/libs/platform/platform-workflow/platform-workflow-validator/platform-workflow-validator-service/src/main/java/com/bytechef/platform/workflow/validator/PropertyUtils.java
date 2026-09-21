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
    public static boolean isNestedTaskProperty(PropertyInfo propertyInfo) {
        if ("TASK".equalsIgnoreCase(propertyInfo.type())) {
            return true;
        }

        List<PropertyInfo> nestedPropertyInfos = propertyInfo.nestedProperties();

        if (!"ARRAY".equalsIgnoreCase(propertyInfo.type()) || nestedPropertyInfos == null ||
            nestedPropertyInfos.size() != 1) {

            return false;
        }

        PropertyInfo nestedPropertyInfo = nestedPropertyInfos.getFirst();

        return "TASK".equalsIgnoreCase(nestedPropertyInfo.type());
    }

    public static boolean checkPropertyExists(PropertyInfo outputInfo, String propertyName) {
        return checkPropertyExistsRecursive(outputInfo, propertyName.split("\\."));
    }

    /**
     * Gets the type of a property from the given PropertyInfo structure.
     */
    public static @Nullable String getPropertyType(PropertyInfo outputInfo, String propertyName) {
        PropertyInfo propertyInfo = getProperty(outputInfo, propertyName);

        return propertyInfo == null ? null : propertyInfo.type();
    }

    public static @Nullable PropertyInfo getProperty(PropertyInfo outputInfo, String propertyName) {
        return getPropertyRecursive(outputInfo, propertyName.split("\\."));
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

    private static @Nullable PropertyInfo getPropertyRecursive(PropertyInfo outputInfo, String[] propertyPath) {
        if (propertyPath.length == 0) {
            return outputInfo;
        }

        String currentProperty = propertyPath[0];

        if (currentProperty.contains("[") && currentProperty.endsWith("]")) {
            return getArrayProperty(outputInfo, currentProperty, propertyPath);
        }

        if (currentProperty.equals(outputInfo.name())) {
            return getCurrentProperty(outputInfo, propertyPath);
        }

        return getNestedProperty(outputInfo, currentProperty, propertyPath);
    }

    private static @Nullable PropertyInfo getArrayProperty(
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
                        return propertyInfo;
                    }

                    String[] remainingPath = createRemainingPath(propertyPath);

                    return getPropertyRecursive(propertyInfo, remainingPath);
                }
            }
        }

        return null;
    }

    private static @Nullable PropertyInfo getCurrentProperty(PropertyInfo outputPropertyInfo, String[] propertyPath) {
        if (propertyPath.length == 1) {
            return outputPropertyInfo;
        }

        if (outputPropertyInfo.nestedProperties() != null) {
            String[] remainingPath = createRemainingPath(propertyPath);

            for (PropertyInfo propertyInfo : outputPropertyInfo.nestedProperties()) {
                PropertyInfo result = getPropertyRecursive(propertyInfo, remainingPath);

                if (result != null) {
                    return result;
                }
            }
        }

        return null;
    }

    @Nullable
    private static PropertyInfo getNestedProperty(
        PropertyInfo outputPropertyInfo, String currentProperty, String[] propertyPath) {

        if (outputPropertyInfo.nestedProperties() != null) {
            for (PropertyInfo propertyInfo : outputPropertyInfo.nestedProperties()) {
                if (currentProperty.equals(propertyInfo.name())) {
                    if (propertyPath.length == 1) {
                        return propertyInfo;
                    }

                    if (propertyInfo.nestedProperties() != null) {
                        String[] remainingPath = createRemainingPath(propertyPath);

                        for (PropertyInfo deepNestedProp : propertyInfo.nestedProperties()) {
                            PropertyInfo result = getPropertyRecursive(deepNestedProp, remainingPath);

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
