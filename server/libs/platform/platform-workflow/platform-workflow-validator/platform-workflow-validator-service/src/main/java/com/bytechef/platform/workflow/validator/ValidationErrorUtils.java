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

import java.util.Collection;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Utility class for building validation messages with consistent formatting.
 *
 * @author Marko Kriskovic
 */
class ValidationErrorUtils {

    private ValidationErrorUtils() {
    }

    /**
     * Creates an error message for a value that is not one of the property's fixed options.
     */
    public static String invalidOptionValue(String propertyPath, String value, List<Object> optionValues) {
        return "Property '" + propertyPath + "' has value " + value + " that is not one of the allowed options: " +
            optionValues.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(", "));
    }

    /**
     * Creates a type error message (handles property, object, array types).
     */
    public static String typeError(String propertyPath, String expectedType, String actualType) {
        return "Property '" + propertyPath + "' has incorrect type. Expected: " + expectedType + ", but got: " +
            actualType;
    }

    /**
     * Creates an array element type error message.
     */
    public static String arrayElementError(
        String elementValue, String propertyName, String expectedType, String actualType) {

        return "Value " + elementValue + " has incorrect type in property '" + propertyName + "'. Expected: " +
            expectedType + ", but got: " + actualType;
    }

    /**
     * Creates a missing cluster element error message.
     */
    public static String missingClusterElement(String elementKey, String taskName) {
        return "Cluster element '" + elementKey + "' is missing from task " + taskName;
    }

    public static String invalidConnection(String connectionName) {
        return "Invalid connection: " + connectionName;
    }

    /**
     * Creates a missing property error message.
     */
    public static String missingConnection(String componentTitle) {
        return "Missing required connection: " + componentTitle;
    }

    public static String missingProperty(String propertyPath) {
        return "Missing required property: " + propertyPath;
    }

    public static String missingResource(String propertyPath, String reason) {
        return "Resource referenced by property '" + propertyPath + "' is not available: " + reason;
    }

    public static String resourceCheckFailed(String propertyPath, String reason) {
        return "Could not verify the resource referenced by property '" + propertyPath + "': " + reason;
    }

    /**
     * Creates property not defined a warning message.
     */
    public static String notDefined(String propertyPath) {
        return "Property '" + propertyPath + "' is not defined in task definition";
    }

    /**
     * Creates an undefined cluster element warning message.
     */
    public static String unknownClusterElementType(String elementName, String elementType) {
        return "Cluster element '" + elementName + "' has unknown type " + elementType +
            "; the type must be componentName/vN/clusterElementName of a defined cluster element";
    }

    public static String clusterElementInWrongSlot(
        String elementName, String elementType, String slotKey, Collection<String> slotKeys) {

        return "Cluster element '" + elementName + "' of type " + elementType + " cannot be placed under '" + slotKey +
            "'; it belongs under " + String.join(" or ", new TreeSet<>(slotKeys));
    }

    public static String undefinedClusterElementSlot(
        String elementKey, String taskType, Collection<String> clusterElementKeys) {

        return "Cluster element slot '" + elementKey + "' does not exist on " + taskType + "; its slots are " +
            String.join(", ", new TreeSet<>(clusterElementKeys));
    }
}
