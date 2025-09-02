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

package utils;

import com.bytechef.ai.mcp.tool.automation.ToolUtils;

/**
 * Utility class for navigating and checking properties in PropertyInfo structures. Handles nested property access,
 * array indexing, and type resolution.
 */
public class PropertyNavigator {

    private PropertyNavigator() {
        // Utility class
    }

    /**
     * Checks if a property exists in the given PropertyInfo structure.
     */
    public static boolean checkPropertyExists(ToolUtils.PropertyInfo outputInfo, String propertyName) {
        return checkPropertyExistsRecursive(outputInfo, propertyName.split("\\."));
    }

    /**
     * Gets the type of a property from the given PropertyInfo structure.
     */
    public static String getPropertyType(ToolUtils.PropertyInfo outputInfo, String propertyName) {
        return getPropertyTypeRecursive(outputInfo, propertyName.split("\\."));
    }

    private static boolean checkPropertyExistsRecursive(ToolUtils.PropertyInfo outputInfo, String[] propertyPath) {
        if (propertyPath.length == 0) {
            return true;
        }

        String currentProperty = propertyPath[0];

        // Handle array access like "items[0]"
        if (currentProperty.contains("[") && currentProperty.endsWith("]")) {
            return checkArrayPropertyExists(outputInfo, currentProperty, propertyPath);
        }

        // Check if the current property matches the main property
        if (currentProperty.equals(outputInfo.name())) {
            return checkCurrentPropertyExists(outputInfo, propertyPath);
        }

        // Check nested properties if they exist
        return checkNestedPropertyExists(outputInfo, currentProperty, propertyPath);
    }

    private static boolean
        checkArrayPropertyExists(ToolUtils.PropertyInfo outputInfo, String currentProperty, String[] propertyPath) {
        String arrayName = currentProperty.substring(0, currentProperty.indexOf('['));

        // Check if the array property exists
        if (arrayName.equals(outputInfo.name()) ||
            (outputInfo.nestedProperties() != null &&
                outputInfo.nestedProperties()
                    .stream()
                    .anyMatch(prop -> arrayName.equals(prop.name())))) {

            // Find the array property
            ToolUtils.PropertyInfo arrayProp = findArrayProperty(outputInfo, arrayName);

            if (arrayProp != null && "ARRAY".equals(arrayProp.type()) &&
                arrayProp.nestedProperties() != null && !arrayProp.nestedProperties()
                    .isEmpty()) {

                ToolUtils.PropertyInfo elementType = arrayProp.nestedProperties()
                    .get(0);
                if (propertyPath.length == 1) {
                    return true;
                }

                String[] remainingPath = createRemainingPath(propertyPath, 1);
                return checkPropertyExistsRecursive(elementType, remainingPath);
            }
        }
        return false;
    }

    private static ToolUtils.PropertyInfo findArrayProperty(ToolUtils.PropertyInfo outputInfo, String arrayName) {
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

    private static boolean checkCurrentPropertyExists(ToolUtils.PropertyInfo outputInfo, String[] propertyPath) {
        if (propertyPath.length == 1) {
            return true;
        }

        if (outputInfo.nestedProperties() != null) {
            String[] remainingPath = createRemainingPath(propertyPath, 1);
            for (ToolUtils.PropertyInfo nestedProp : outputInfo.nestedProperties()) {
                if (checkPropertyExistsRecursive(nestedProp, remainingPath)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean
        checkNestedPropertyExists(ToolUtils.PropertyInfo outputInfo, String currentProperty, String[] propertyPath) {
        if (outputInfo.nestedProperties() != null) {
            for (ToolUtils.PropertyInfo nestedProp : outputInfo.nestedProperties()) {
                if (currentProperty.equals(nestedProp.name())) {
                    if (propertyPath.length == 1) {
                        return true;
                    }

                    if (nestedProp.nestedProperties() != null) {
                        String[] remainingPath = createRemainingPath(propertyPath, 1);
                        for (ToolUtils.PropertyInfo deepNestedProp : nestedProp.nestedProperties()) {
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

    private static String getPropertyTypeRecursive(ToolUtils.PropertyInfo outputInfo, String[] propertyPath) {
        if (propertyPath.length == 0) {
            return outputInfo.type();
        }

        String currentProperty = propertyPath[0];

        // Handle array access like "items[0]"
        if (currentProperty.contains("[") && currentProperty.endsWith("]")) {
            return getArrayPropertyType(outputInfo, currentProperty, propertyPath);
        }

        // Check if the current property matches the main property
        if (currentProperty.equals(outputInfo.name())) {
            return getCurrentPropertyType(outputInfo, propertyPath);
        }

        // Check nested properties if they exist
        return getNestedPropertyType(outputInfo, currentProperty, propertyPath);
    }

    private static String
        getArrayPropertyType(ToolUtils.PropertyInfo outputInfo, String currentProperty, String[] propertyPath) {
        String arrayName = currentProperty.substring(0, currentProperty.indexOf('['));

        if (arrayName.equals(outputInfo.name()) ||
            (outputInfo.nestedProperties() != null &&
                outputInfo.nestedProperties()
                    .stream()
                    .anyMatch(prop -> arrayName.equals(prop.name())))) {

            ToolUtils.PropertyInfo arrayProp = findArrayProperty(outputInfo, arrayName);

            if (arrayProp != null && "ARRAY".equals(arrayProp.type()) &&
                arrayProp.nestedProperties() != null && !arrayProp.nestedProperties()
                    .isEmpty()) {

                ToolUtils.PropertyInfo elementType = arrayProp.nestedProperties()
                    .get(0);
                if (propertyPath.length == 1) {
                    return elementType.type();
                }

                String[] remainingPath = createRemainingPath(propertyPath, 1);
                return getPropertyTypeRecursive(elementType, remainingPath);
            }
        }
        return null;
    }

    private static String getCurrentPropertyType(ToolUtils.PropertyInfo outputInfo, String[] propertyPath) {
        if (propertyPath.length == 1) {
            return outputInfo.type();
        }

        if (outputInfo.nestedProperties() != null) {
            String[] remainingPath = createRemainingPath(propertyPath, 1);
            for (ToolUtils.PropertyInfo nestedProp : outputInfo.nestedProperties()) {
                String result = getPropertyTypeRecursive(nestedProp, remainingPath);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    private static String
        getNestedPropertyType(ToolUtils.PropertyInfo outputInfo, String currentProperty, String[] propertyPath) {
        if (outputInfo.nestedProperties() != null) {
            for (ToolUtils.PropertyInfo nestedProp : outputInfo.nestedProperties()) {
                if (currentProperty.equals(nestedProp.name())) {
                    if (propertyPath.length == 1) {
                        return nestedProp.type();
                    }

                    if (nestedProp.nestedProperties() != null) {
                        String[] remainingPath = createRemainingPath(propertyPath, 1);
                        for (ToolUtils.PropertyInfo deepNestedProp : nestedProp.nestedProperties()) {
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

    private static String[] createRemainingPath(String[] propertyPath, int startIndex) {
        String[] remainingPath = new String[propertyPath.length - startIndex];
        System.arraycopy(propertyPath, startIndex, remainingPath, 0, propertyPath.length - startIndex);
        return remainingPath;
    }
}
