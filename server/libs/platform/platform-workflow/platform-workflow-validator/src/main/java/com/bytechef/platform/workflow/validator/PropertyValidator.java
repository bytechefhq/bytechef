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

import com.bytechef.commons.util.StringUtils;
import com.bytechef.platform.workflow.validator.model.PropertyInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for orchestrating property-level validation. Handles recursive property validation, extra property
 * detection, and defined property validation.
 *
 * @author Marko Kriskovic
 */
class PropertyValidator {

    private static final Logger logger = LoggerFactory.getLogger(PropertyValidator.class);

    private PropertyValidator() {
    }

    /**
     * Recursively validates properties in current parameters against their definition using PropertyInfo list.
     */
    public static void validatePropertiesFromPropertyInfo(
        JsonNode taskParametersJsonNode, List<PropertyInfo> propertyInfos,
        String path, String originalCurrentParameters, StringBuilder errors, StringBuilder warnings) {

        if (propertyInfos.isEmpty()) {
            // No definition, generate warnings for all properties
            generateWarningsForAllProperties(taskParametersJsonNode, path, warnings);
            return;
        }

        // Track which properties have been validated by at least one definition
        Set<String> validatedProperties = new HashSet<>();

        // First pass: validate properties whose display conditions are true
        for (PropertyInfo propertyInfo : propertyInfos) {
            String fieldName = propertyInfo.name();
            String propertyPath = PropertyUtils.buildPropertyPath(path, fieldName);

            // Evaluate display condition if present
            boolean shouldInclude = true;
            boolean hasInvalidDisplayCondition = false;
            String malformedConditionMessage = null;

            if (propertyInfo.displayCondition() != null && !propertyInfo.displayCondition()
                .isEmpty()) {
                try {
                    shouldInclude = WorkflowUtils.extractAndEvaluateCondition(
                        propertyInfo.displayCondition(),
                        com.bytechef.commons.util.JsonUtils.readTree(originalCurrentParameters));
                } catch (Exception e) {
                    // Any exception during display condition evaluation means we should not include the property
                    shouldInclude = false;

                    // Check if this is truly a malformed condition that should generate a warning
                    String message = e.getMessage();
                    if (message != null && message.startsWith("Invalid logic for display condition:")) {
                        malformedConditionMessage = message;
                        hasInvalidDisplayCondition = true;
                    } else if (logger.isTraceEnabled()) {
                        logger.trace(e.getMessage());
                    }
                }
            }

            // If condition is invalid, add property warnings first, then the condition warning
            if (hasInvalidDisplayCondition) {
                // Check if property exists and warn about it and all nested properties
                if (taskParametersJsonNode.has(fieldName)) {
                    StringUtils.appendWithNewline(ValidationErrorUtils.notDefined(propertyPath), warnings);

                    // Generate warnings for nested properties if this is an object
                    JsonNode valueJsonNode = taskParametersJsonNode.get(fieldName);
                    if (valueJsonNode.isObject()) {
                        generateWarningsForAllProperties(valueJsonNode, propertyPath, warnings);
                    }
                }

                // Now add the malformed condition warning AFTER property warnings
                StringUtils.appendWithNewline(malformedConditionMessage, warnings);
                validatedProperties.add(fieldName); // Mark as processed
            }
            // Display condition is true, validate the property
            else if (shouldInclude) {
                validatedProperties.add(fieldName);
                validatePropertyFromPropertyInfo(
                    taskParametersJsonNode, propertyInfo, propertyPath, originalCurrentParameters, errors, warnings);
            }
        }

        // Second pass: check for properties in parameters that weren't validated by any definition
        Iterator<String> fieldNamesIterator = taskParametersJsonNode.fieldNames();
        fieldNamesIterator.forEachRemaining(fieldName -> {
            if (!validatedProperties.contains(fieldName)) {
                String propertyPath = PropertyUtils.buildPropertyPath(path, fieldName);
                JsonNode valueJsonNode = taskParametersJsonNode.get(fieldName);

                StringUtils.appendWithNewline(ValidationErrorUtils.notDefined(propertyPath), warnings);

                if (valueJsonNode.isObject()) {
                    generateWarningsForAllProperties(valueJsonNode, propertyPath, warnings);
                }
            }
        });
    }

    /**
     * Recursively validates properties in current parameters against their definition.
     */
    public static void validatePropertiesRecursively(
        JsonNode taskParametersJsonNode, JsonNode definitionJsonNode, String path, String originalTaskDefinition,
        String originalCurrentParameters, StringBuilder errors, StringBuilder warnings) {

        validatePropertiesRecursively(
            taskParametersJsonNode, definitionJsonNode, path, originalTaskDefinition, null, originalCurrentParameters,
            errors, warnings);
    }

    /**
     * Recursively validates properties with array support for display conditions.
     */
    public static void validatePropertiesRecursively(
        JsonNode taskParametersJsonNode, JsonNode definitionJsonNode, String path, String originalTaskDefinition,
        @Nullable String originalTaskDefinitionForArrays, String originalCurrentParameters, StringBuilder errors,
        StringBuilder warnings) {

        // Check for extra properties (generate warnings)
        if (isEmptyContainer(definitionJsonNode)) {
            generateWarningsForAllProperties(taskParametersJsonNode, path, warnings);
        } else {
            Iterator<String> fieldNamesIterator = taskParametersJsonNode.fieldNames();

            fieldNamesIterator.forEachRemaining(fieldName -> {
                if (!definitionJsonNode.has(fieldName)) {
                    String propertyPath = PropertyUtils.buildPropertyPath(path, fieldName);
                    JsonNode valueJsonNode = taskParametersJsonNode.get(fieldName);

                    StringUtils.appendWithNewline(ValidationErrorUtils.notDefined(propertyPath), warnings);

                    if (valueJsonNode.isObject()) {
                        generateWarningsForAllProperties(valueJsonNode, propertyPath, warnings);
                    }
                }
            });
        }

        // Validate defined properties
        Iterator<String> fieldNamesIterator = definitionJsonNode.fieldNames();

        fieldNamesIterator.forEachRemaining(fieldName -> {
            JsonNode curDefinitionJsonNode = definitionJsonNode.get(fieldName);
            String propertyPath = PropertyUtils.buildPropertyPath(path, fieldName);

            if (curDefinitionJsonNode.isTextual()) {
                handleTextualProperty(
                    taskParametersJsonNode, fieldName, curDefinitionJsonNode, propertyPath,
                    errors, warnings);
            } else if (curDefinitionJsonNode.isObject()) {
                validateNestedObject(
                    taskParametersJsonNode, fieldName, curDefinitionJsonNode, propertyPath, originalTaskDefinition,
                    originalTaskDefinitionForArrays, originalCurrentParameters, errors, warnings);
            } else if (curDefinitionJsonNode.isArray() && !curDefinitionJsonNode.isEmpty()) {
                if (originalTaskDefinitionForArrays == null) {
                    validateArrayProperty(
                        taskParametersJsonNode, fieldName, curDefinitionJsonNode, propertyPath, errors, warnings);
                } else {
                    validateArrayPropertyWithArraySupport(
                        taskParametersJsonNode, fieldName, curDefinitionJsonNode, propertyPath, errors);
                }
            }
        });
    }

    private static String buildPropertyPath(String parentPath, String propertyName) {
        return parentPath.isEmpty() ? propertyName : parentPath + "." + propertyName;
    }

    /**
     * For array validation, we need to construct the full parameter structure to evaluate conditions like "items[0].age
     * >= 18"
     */
    private static JsonNode createRootParametersJsonNode(JsonNode arrayValue, String propertyPath) {
        ObjectMapper mapper = new ObjectMapper();

        ObjectNode rootNode = mapper.createObjectNode();

        rootNode.set(propertyPath, arrayValue);

        return rootNode;
    }

    private static String formatValue(JsonNode jsonNode) {
        return jsonNode.isTextual() ? "'" + jsonNode.asText() + "'" : jsonNode.toString();
    }

    /**
     * Generates warnings for all properties recursively.
     */
    private static void generateWarningsForAllProperties(JsonNode jsonNode, String path, StringBuilder warnings) {
        Iterator<String> fieldNamesIterator = jsonNode.fieldNames();

        fieldNamesIterator.forEachRemaining(fieldName -> {
            String propertyPath = buildPropertyPath(path, fieldName);
            JsonNode valueJsonNode = jsonNode.get(fieldName);

            StringUtils.appendWithNewline(ValidationErrorUtils.notDefined(propertyPath), warnings);

            if (valueJsonNode.isObject()) {
                generateWarningsForAllProperties(valueJsonNode, propertyPath, warnings);
            }
        });
    }

    /**
     * Handles validation for textual property definitions (like "string (required)").
     */
    private static void handleTextualProperty(
        JsonNode taskParametersJsonNode, String fieldName, JsonNode definitionJsonNode, String propertyPath,
        StringBuilder errors, StringBuilder warnings) {

        String type = definitionJsonNode.asText();

        if (("object".equalsIgnoreCase(type) || "array".equalsIgnoreCase(type)) &&
            taskParametersJsonNode.has(fieldName)) {

            JsonNode valueJsonNode = taskParametersJsonNode.get(fieldName);

            boolean correctType = ("object".equalsIgnoreCase(type) && valueJsonNode.isObject()) ||
                ("array".equalsIgnoreCase(type) && valueJsonNode.isArray());

            if (correctType && "object".equalsIgnoreCase(type)) {
                // Generate warnings for all nested properties since they're not defined
                Iterator<String> fieldNamesIterator = valueJsonNode.fieldNames();

                fieldNamesIterator.forEachRemaining(curFieldName -> {
                    String fieldPath = buildPropertyPath(propertyPath, curFieldName);

                    StringUtils.appendWithNewline(ValidationErrorUtils.notDefined(fieldPath), warnings);
                });

                return;
            }
        }

        validateStringTypeDefinition(taskParametersJsonNode, fieldName, type, propertyPath,
            errors);
    }

    private static boolean isDataPillExpression(@Nullable String value) {
        return value != null && value.matches("\\$\\{[^}]+}");
    }

    private static boolean isDateTimeType(String expectedType) {
        String lowerType = expectedType.toLowerCase();

        return "date".equals(lowerType) || "time".equals(lowerType) || "date_time".equals(lowerType);
    }

    private static boolean isEmptyContainer(JsonNode node) {
        return (node.isObject() && node.isEmpty()) || (node.isArray() && node.isEmpty());
    }

    private static boolean isLeapYear(int year) {
        return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0);
    }

    /**
     * Checks if a JsonNode matches the expected type.
     */
    private static boolean isTypeValid(JsonNode jsonNode, String expectedType) {
        // Null values are allowed for all types except when explicitly expecting the null type
        if (jsonNode.isNull() && !"null".equalsIgnoreCase(expectedType)) {
            return true;
        }

        return switch (expectedType.toLowerCase()) {
            case "string" -> jsonNode.isTextual();
            case "float" -> jsonNode.isFloatingPointNumber();
            case "integer" -> jsonNode.isIntegralNumber();
            case "number" -> jsonNode.isNumber();
            case "boolean" -> jsonNode.isBoolean();
            case "array" -> jsonNode.isArray();
            case "object" -> jsonNode.isObject();
            case "null" -> jsonNode.isNull();
            case "date" -> jsonNode.isTextual() && validateDateFormat(jsonNode.asText());
            case "time" -> jsonNode.isTextual() && validateTimeFormat(jsonNode.asText());
            case "date_time" -> jsonNode.isTextual() && validateDateTimeFormat(jsonNode.asText());
            default -> true;
        };
    }

    /**
     * Replaces [index] placeholder in conditions with actual index.
     */
    private static String replaceIndexPlaceholder(String condition, int index) {
        return condition.replace("[index]", "[" + index + "]");
    }

    /**
     * Validates array property with a warnings support.
     */
    private static void validateArrayProperty(
        JsonNode taskParametersJsonNode, String fieldName, JsonNode definitionJsonNode, String propertyPath,
        StringBuilder errors) {

        validateArrayProperty(taskParametersJsonNode, fieldName, definitionJsonNode, propertyPath, errors, null);
    }

    /**
     * Validates array property and its elements.
     */
    private static void validateArrayProperty(
        JsonNode taskParametersJsonNode, String fieldName, JsonNode definitionJsonNode, String propertyPath,
        StringBuilder errors, @Nullable StringBuilder warnings) {

        if (!taskParametersJsonNode.has(fieldName)) {
            return;
        }

        JsonNode valueJsonNode = taskParametersJsonNode.get(fieldName);

        if (!valueJsonNode.isArray()) {
            String actualType = JsonUtils.getJsonNodeType(valueJsonNode);
            StringUtils.appendWithNewline(ValidationErrorUtils.typeError(propertyPath, "array", actualType), errors);

            return;
        }

        // Check if this is a union type or object array
        JsonNode firstElementJsonNode = definitionJsonNode.get(0);
        boolean isUnionType = false;

        if (firstElementJsonNode.isTextual()) {
            // Check if all elements are simple text types (union)
            isUnionType = true;

            for (int i = 0; i < definitionJsonNode.size(); i++) {
                JsonNode cureElementJsonNode = definitionJsonNode.get(i);

                if (!cureElementJsonNode.isTextual()) {
                    isUnionType = false;

                    break;
                }
            }
        }

        if (warnings == null) {
            if (isUnionType) {
                // Array of simple types (union type)
                validateUnionArrayElements(valueJsonNode, definitionJsonNode, propertyPath, errors);
            } else {
                // Array of objects - use new validation with warnings
                JsonNode arrayElementDefinitionJsonNode = definitionJsonNode.get(0);

                validateObjectArrayElements(valueJsonNode, arrayElementDefinitionJsonNode, propertyPath, errors);
            }
        } else {
            if (isUnionType) {
                // Array of simple types - use existing validation without warnings
                validateArrayProperty(taskParametersJsonNode, fieldName, definitionJsonNode, propertyPath, errors);
            }
        }
    }

    /**
     * Validates array property with array support for display conditions.
     */
    private static void validateArrayPropertyWithArraySupport(
        JsonNode taskParametersJsonNode, String fieldName, JsonNode definitionJsonNode,
        String propertyPath, StringBuilder errors) {

        if (!taskParametersJsonNode.has(fieldName)) {
            return;
        }

        JsonNode valueJsonNode = taskParametersJsonNode.get(fieldName);

        if (!valueJsonNode.isArray()) {
            String actualType = JsonUtils.getJsonNodeType(valueJsonNode);

            StringUtils.appendWithNewline(ValidationErrorUtils.typeError(propertyPath, "array", actualType), errors);

            return;
        }

        // Check if this is a union type (array of simple types) or object array
        boolean isUnionType = false;

        if (!definitionJsonNode.isEmpty()) {
            JsonNode firstElementJsonNode = definitionJsonNode.get(0);

            if (firstElementJsonNode.isTextual()) {
                // Check if all elements are simple text types (union type)
                isUnionType = true;

                for (int i = 0; i < definitionJsonNode.size(); i++) {
                    JsonNode curElementJsonNode = definitionJsonNode.get(i);

                    if (!curElementJsonNode.isTextual()) {
                        isUnionType = false;

                        break;
                    }
                }
            }
        }

        if (isUnionType) {
            // Check if this is a TASK type array first
            if (definitionJsonNode.size() == 1) {
                JsonNode firstElementJsonNode = definitionJsonNode.get(0);

                if (firstElementJsonNode.isTextual() && "task".equalsIgnoreCase(firstElementJsonNode.asText())) {
                    // Handle TASK type arrays specially
                    TaskValidator.validateTaskArray(valueJsonNode, propertyPath, errors);

                    return;
                }
            }
            // Array of simple types - use existing validation without warnings
            validateArrayProperty(taskParametersJsonNode, fieldName, definitionJsonNode, propertyPath, errors);
        } else if (!definitionJsonNode.isEmpty()) {
            JsonNode arrayElementJsonNode = definitionJsonNode.get(0);

            // Check if this is a TASK type array
            if (arrayElementJsonNode.isTextual() && "task".equalsIgnoreCase(arrayElementJsonNode.asText())) {
                // Handle TASK type arrays specially
                TaskValidator.validateTaskArray(valueJsonNode, propertyPath, errors);

                return;
            }

            // Check if this is an array of arrays (nested arrays)
            if (arrayElementJsonNode.isArray()) {
                // Handle array of arrays - validate each sub-array
                for (int i = 0; i < valueJsonNode.size(); i++) {
                    JsonNode subArrayJsonNode = valueJsonNode.get(i);
                    String subArrayPath = propertyPath + "[" + i + "]";

                    if (!subArrayJsonNode.isArray()) {
                        String actualType = JsonUtils.getJsonNodeType(subArrayJsonNode);

                        StringUtils.appendWithNewline(
                            ValidationErrorUtils.typeError(subArrayPath, "array", actualType), errors);
                    }
                }
            }
        }
    }

    /**
     * Validates DATE format (yyyy-MM-dd).
     */
    private static boolean validateDateFormat(@Nullable String dateValue) {
        if (org.apache.commons.lang3.StringUtils.isBlank(dateValue)) {
            return false;
        }

        // Check the format pattern first
        if (!dateValue.matches("\\d{4}-\\d{2}-\\d{2}")) {
            return false;
        }

        // Parse and validate actual date
        try {
            String[] parts = dateValue.split("-");

            int year = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);
            int day = Integer.parseInt(parts[2]);

            // Basic range checks
            if (month < 1 || month > 12) {
                return false;
            }

            if (day < 1 || day > 31) {
                return false;
            }

            // Check days in a month
            int[] daysInMonth = {
                31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31
            };

            // Check for leap year
            if (month == 2 && isLeapYear(year)) {
                daysInMonth[1] = 29;
            }

            return day <= daysInMonth[month - 1];
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            return false;
        }
    }

    /**
     * Validates DATE_TIME format (yyyy-MM-ddThh:mm:ss).
     */
    private static boolean validateDateTimeFormat(@Nullable String dateTimeValue) {
        if (dateTimeValue == null || org.apache.commons.lang3.StringUtils.isBlank(dateTimeValue)) {
            return false;
        }

        // Check format pattern first
        if (!dateTimeValue.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}")) {
            return false;
        }

        // Split date and time parts
        String[] parts = dateTimeValue.split("T");

        if (parts.length != 2) {
            return false;
        }

        return validateDateFormat(parts[0]) && validateTimeFormat(parts[1]);
    }

    /**
     * Validates the date/time format and returns a specific error message if invalid.
     */
    @Nullable
    private static String validateDateTimeTypeWithError(String value, String expectedType, String propertyPath) {
        String lowerType = expectedType.toLowerCase();

        switch (lowerType) {
            case "date":
                if (!value.matches("\\d{4}-\\d{2}-\\d{2}")) {
                    return "Property '" + propertyPath +
                        "' is in incorrect date format. Format should be in: 'yyyy-MM-dd'";
                }

                if (!validateDateFormat(value)) {
                    return "Property '" + propertyPath + "' is in incorrect date format. Impossible date: " + value;
                }

                break;
            case "time":
                if (!value.matches("\\d{2}:\\d{2}:\\d{2}")) {
                    return "Property '" + propertyPath +
                        "' is in incorrect time format. Format should be in: 'hh:mm:ss'";
                }

                if (!validateTimeFormat(value)) {
                    return "Property '" + propertyPath + "' is in incorrect time format. Impossible time: " + value;
                }

                break;

            case "date_time":
                if (!value.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}")) {
                    return "Property '" + propertyPath +
                        "' has incorrect type. Format should be in: 'yyyy-MM-ddThh:mm:ss'";
                }

                if (!validateDateTimeFormat(value)) {
                    return "Property '" + propertyPath +
                        "' has incorrect type. Format should be in: 'yyyy-MM-ddThh:mm:ss'";
                }

                break;
            default:
        }

        return null;
    }

    /**
     * Validates nested object properties with array support.
     */
    private static void validateNestedObject(
        JsonNode taskParametersJsonNode, String fieldName, JsonNode definitionJsonNode, String propertyPath,
        String originalTaskDefinition, @Nullable String originalTaskDefinitionForArrays,
        String originalCurrentParameters, StringBuilder errors, StringBuilder warnings) {

        if (!taskParametersJsonNode.has(fieldName)) {

            return;
        }

        JsonNode valueJsonNode = taskParametersJsonNode.get(fieldName);

        if (valueJsonNode.isObject()) {
            if (originalTaskDefinitionForArrays == null) {
                validatePropertiesRecursively(
                    valueJsonNode, definitionJsonNode, propertyPath, originalTaskDefinition, originalCurrentParameters,
                    errors, warnings);
            } else {
                validatePropertiesRecursively(
                    valueJsonNode, definitionJsonNode, propertyPath, originalTaskDefinition,
                    originalTaskDefinitionForArrays, originalCurrentParameters, errors, warnings);
            }
        }
        else {
            String actualType = JsonUtils.getJsonNodeType(valueJsonNode);

            StringUtils.appendWithNewline(ValidationErrorUtils.typeError(propertyPath, "object", actualType), errors);
        }
    }

    /**
     * Validates array elements that are objects with defined properties.
     */
    private static void validateObjectArrayElements(
        JsonNode arrayValueJsonNode, JsonNode objectDefinitionJsonNode, String propertyPath, StringBuilder errors) {

        for (int i = 0; i < arrayValueJsonNode.size(); i++) {
            JsonNode jsonNode = arrayValueJsonNode.get(i);

            String elementPath = propertyPath + "[" + i + "]";

            if (!jsonNode.isObject()) {
                String actualType = JsonUtils.getJsonNodeType(jsonNode);

                StringUtils.appendWithNewline(
                    ValidationErrorUtils.typeError(elementPath, "object", actualType), errors);
            } else {
                Iterator<String> fieldNamesIterator = objectDefinitionJsonNode.fieldNames();

                fieldNamesIterator.forEachRemaining(fieldName -> {
                    JsonNode fieldDefinitionJsonNode = objectDefinitionJsonNode.get(fieldName);
                    String fieldPath = elementPath + "." + fieldName;

                    if (fieldDefinitionJsonNode.isTextual()) {
                        String expectedType = fieldDefinitionJsonNode.asText();

                        if (jsonNode.has(fieldName)) {
                            JsonNode valueJsonNode = jsonNode.get(fieldName);

                            if (!isTypeValid(valueJsonNode, expectedType)) {
                                String actualType = JsonUtils.getJsonNodeType(valueJsonNode);

                                StringUtils.appendWithNewline(
                                    ValidationErrorUtils.typeError(fieldPath, expectedType, actualType), errors);
                            }
                        }
                    }
                });
            }
        }
    }

    /**
     * Validates a property definition and its value, handling required properties and type checking.
     */
    private static void validateStringTypeDefinition(
        JsonNode taskParametersJsonNode, String fieldName, String propertyDefinition, String propertyPath,
        StringBuilder errors) {

        if (taskParametersJsonNode.has(fieldName)) {
            JsonNode valueJsonNode = taskParametersJsonNode.get(fieldName);

            if ("object".equalsIgnoreCase(propertyDefinition) && valueJsonNode.isObject()) {
                return;
            }

            // Inline type validation to avoid wrapper function
            if (valueJsonNode.isTextual() && isDataPillExpression(valueJsonNode.asText())) {
                return;
            }

            // Handle format validation for date/time types
            if (valueJsonNode.isTextual() && isDateTimeType(propertyDefinition)) {
                String formatError = validateDateTimeTypeWithError(
                    valueJsonNode.asText(), propertyDefinition, propertyPath);

                if (formatError != null) {
                    StringUtils.appendWithNewline(formatError, errors);
                }
            } else if (!isTypeValid(valueJsonNode, propertyDefinition)) {
                String actualType = JsonUtils.getJsonNodeType(valueJsonNode);

                StringUtils.appendWithNewline(
                    ValidationErrorUtils.typeError(propertyPath, propertyDefinition, actualType), errors);
            }
        }
    }

    /**
     * Validates TIME format (hh:mm:ss).
     */
    private static boolean validateTimeFormat(@Nullable String timeValue) {
        if (timeValue == null || org.apache.commons.lang3.StringUtils.isBlank(timeValue)) {
            return false;
        }

        // Check format pattern first
        if (!timeValue.matches("\\d{2}:\\d{2}:\\d{2}")) {
            return false;
        }

        // Parse and validate actual time
        try {
            String[] parts = timeValue.split(":");

            int hours = Integer.parseInt(parts[0]);
            int minutes = Integer.parseInt(parts[1]);
            int seconds = Integer.parseInt(parts[2]);

            return hours >= 0 && hours <= 23 && minutes >= 0 && minutes <= 59 && seconds >= 0 && seconds <= 59;
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            return false;
        }
    }

    /**
     * Validates array elements that can be one of multiple simple types (union types).
     */
    private static void validateUnionArrayElements(
        JsonNode arrayJsonNode, JsonNode allowedTypesJsonNode, String propertyPath, StringBuilder errors) {

        for (int i = 0; i < arrayJsonNode.size(); i++) {
            JsonNode valueJsonNode = arrayJsonNode.get(i);
            boolean matchesAnyType = false;

            // Check if an element matches any of the allowed types
            for (int j = 0; j < allowedTypesJsonNode.size(); j++) {
                JsonNode allowedTypeJsonNode = allowedTypesJsonNode.get(j);

                String allowedType = allowedTypeJsonNode.asText();

                if (isTypeValid(valueJsonNode, allowedType)) {
                    matchesAnyType = true;

                    break;
                }
            }

            if (!matchesAnyType) {
                String elementValue = formatValue(valueJsonNode);
                String propertyName = PropertyUtils.extractPropertyNameFromPath(propertyPath);
                String actualType = JsonUtils.getJsonNodeType(valueJsonNode);

                // Build expected types string
                StringBuilder expectedTypes = new StringBuilder();

                for (int j = 0; j < allowedTypesJsonNode.size(); j++) {
                    if (j > 0) {
                        expectedTypes.append(" or ");
                    }

                    JsonNode allowedTypeJsonNode = allowedTypesJsonNode.get(j);

                    expectedTypes.append(allowedTypeJsonNode.asText());
                }

                StringUtils.appendWithNewline(
                    ValidationErrorUtils.arrayElementError(
                        elementValue, propertyName, expectedTypes.toString(), actualType),
                    errors);
            }
        }
    }

    /**
     * Recursively reports all nested required properties as missing.
     */
    private static void reportMissingNestedRequiredProperties(
        PropertyInfo propertyInfo, String propertyPath, StringBuilder errors) {

        List<PropertyInfo> nestedProperties = propertyInfo.nestedProperties();
        if (nestedProperties == null || nestedProperties.isEmpty()) {
            return;
        }

        for (PropertyInfo nestedProp : nestedProperties) {
            // Only report required properties that don't have display conditions
            // If a property has a display condition, we can't determine if it should be required
            // without evaluating the condition in the context of actual data
            if (nestedProp.required() && (nestedProp.displayCondition() == null || nestedProp.displayCondition()
                .isEmpty())) {
                String nestedPath = PropertyUtils.buildPropertyPath(propertyPath, nestedProp.name());
                StringUtils.appendWithNewline(ValidationErrorUtils.missingProperty(nestedPath), errors);

                // If this nested property is also an OBJECT, recursively report its nested required properties
                if ("OBJECT".equalsIgnoreCase(nestedProp.type())) {
                    reportMissingNestedRequiredProperties(nestedProp, nestedPath, errors);
                }
            }
        }
    }

    /**
     * Validates a single property using PropertyInfo.
     */
    private static void validatePropertyFromPropertyInfo(
        JsonNode taskParametersJsonNode, PropertyInfo propertyInfo,
        String propertyPath, String originalCurrentParameters, StringBuilder errors, StringBuilder warnings) {

        String fieldName = propertyInfo.name();
        String type = propertyInfo.type();
        boolean isRequired = propertyInfo.required();

        // Check if property exists
        if (!taskParametersJsonNode.has(fieldName)) {
            if (isRequired) {
                StringUtils.appendWithNewline(ValidationErrorUtils.missingProperty(propertyPath), errors);

                // For required objects that are missing, also report all nested required properties
                if ("OBJECT".equalsIgnoreCase(type)) {
                    reportMissingNestedRequiredProperties(propertyInfo, propertyPath, errors);
                }
            }
            return;
        }

        JsonNode valueJsonNode = taskParametersJsonNode.get(fieldName);

        // Skip type validation for data pill expressions
        if (valueJsonNode.isTextual() && isDataPillExpression(valueJsonNode.asText())) {
            return;
        }

        // Validate based on type
        if ("OBJECT".equalsIgnoreCase(type)) {
            validateObjectPropertyFromPropertyInfo(
                valueJsonNode, propertyInfo, propertyPath, originalCurrentParameters, errors, warnings);
        } else if ("ARRAY".equalsIgnoreCase(type)) {
            validateArrayPropertyFromPropertyInfo(
                valueJsonNode, propertyInfo, propertyPath, errors, warnings);
        } else {
            validateSimpleType(valueJsonNode, type, propertyPath, errors);
        }
    }

    /**
     * Validates an object property using PropertyInfo.
     */
    private static void validateObjectPropertyFromPropertyInfo(
        JsonNode valueJsonNode, PropertyInfo propertyInfo,
        String propertyPath, String originalCurrentParameters, StringBuilder errors, StringBuilder warnings) {

        // Null values are allowed for all types
        if (valueJsonNode.isNull()) {
            return;
        }

        if (!valueJsonNode.isObject()) {
            String actualType = JsonUtils.getJsonNodeType(valueJsonNode);
            StringUtils.appendWithNewline(ValidationErrorUtils.typeError(propertyPath, "object", actualType), errors);
            return;
        }

        // Recursively validate nested properties
        List<PropertyInfo> nestedProperties = propertyInfo.nestedProperties();
        if (nestedProperties != null && !nestedProperties.isEmpty()) {
            validatePropertiesFromPropertyInfo(
                valueJsonNode, nestedProperties, propertyPath, originalCurrentParameters, errors, warnings);
        } else {
            // Generate warnings for all nested properties since they're not defined
            Iterator<String> fieldNamesIterator = valueJsonNode.fieldNames();
            fieldNamesIterator.forEachRemaining(curFieldName -> {
                String fieldPath = buildPropertyPath(propertyPath, curFieldName);
                StringUtils.appendWithNewline(ValidationErrorUtils.notDefined(fieldPath), warnings);
            });
        }
    }

    /**
     * Validates an array property using PropertyInfo.
     */
    private static void validateArrayPropertyFromPropertyInfo(
        JsonNode valueJsonNode, PropertyInfo propertyInfo,
        String propertyPath, StringBuilder errors, StringBuilder warnings) {

        // Null values are allowed for all types
        if (valueJsonNode.isNull()) {
            return;
        }

        if (!valueJsonNode.isArray()) {
            String actualType = JsonUtils.getJsonNodeType(valueJsonNode);
            StringUtils.appendWithNewline(ValidationErrorUtils.typeError(propertyPath, "array", actualType), errors);
            return;
        }

        List<PropertyInfo> nestedProperties = propertyInfo.nestedProperties();

        if (nestedProperties == null || nestedProperties.isEmpty()) {
            // No element definition, skip validation
            return;
        }

        // Check if this is a TASK type array
        if (nestedProperties.size() == 1 && "TASK".equalsIgnoreCase(nestedProperties.getFirst()
            .type())) {
            TaskValidator.validateTaskArray(valueJsonNode, propertyPath, errors);
            return;
        }

        // Check if it's a wrapped definition (single entry with nested properties)
        boolean isWrappedDefinition = nestedProperties.size() == 1 &&
            nestedProperties.getFirst()
                .nestedProperties() != null
            &&
            !nestedProperties.getFirst()
                .nestedProperties()
                .isEmpty();

        if (isWrappedDefinition) {
            PropertyInfo wrapperInfo = nestedProperties.getFirst();
            String wrapperType = wrapperInfo.type();

            if ("ARRAY".equalsIgnoreCase(wrapperType)) {
                // Array of arrays - validate each element as an array
                for (int i = 0; i < valueJsonNode.size(); i++) {
                    JsonNode elementJsonNode = valueJsonNode.get(i);
                    String elementPath = propertyPath + "[" + i + "]";

                    // Create a temporary PropertyInfo for the array element
                    PropertyInfo elementInfo =
                        new PropertyInfo(
                            null, "ARRAY", null, false, false, null, wrapperInfo.nestedProperties());

                    validateArrayPropertyFromPropertyInfo(
                        elementJsonNode, elementInfo, elementPath, errors, warnings);
                }
                return;
            } else {
                // Array of objects - extract the nested properties from the wrapper object
                List<PropertyInfo> objectProperties =
                    wrapperInfo.nestedProperties();
                validateObjectArrayFromPropertyInfo(
                    valueJsonNode, objectProperties, propertyPath, errors, warnings);
                return;
            }
        }

        // For non-wrapped definitions, check the actual array content to determine the type
        if (!valueJsonNode.isEmpty()) {
            JsonNode firstElement = valueJsonNode.get(0);
            if (firstElement.isObject()) {
                // Array contains objects, so nestedProperties define object properties
                validateObjectArrayFromPropertyInfo(
                    valueJsonNode, nestedProperties, propertyPath, errors, warnings);
            } else {
                // Array contains primitives, so nestedProperties define union type options
                validateUnionTypeArrayFromPropertyInfo(valueJsonNode, nestedProperties, propertyPath, errors);
            }
        }
    }

    /**
     * Simplifies display conditions for union type validation by removing array path prefixes. Transforms conditions
     * like "conditions[index][index].operation" to just "operation".
     */
    private static List<PropertyInfo> simplifyDisplayConditionsForUnionType(
        List<PropertyInfo> properties, String arrayPath) {

        List<PropertyInfo> simplified = new ArrayList<>();

        for (PropertyInfo prop : properties) {
            String displayCondition = prop.displayCondition();

            // Simplify the display condition if it contains array path references
            if (displayCondition != null && !displayCondition.isEmpty()) {
                String simplifiedCondition = displayCondition;

                // Extract the base array name from arrayPath (e.g., "conditions[0]" -> "conditions")
                String baseArrayName = arrayPath.replaceAll("\\[\\d+]", "");

                // Replace patterns like "baseArrayName[index][index]." with just ""
                // This transforms "conditions[index][index].operation" to "operation"
                simplifiedCondition = simplifiedCondition.replaceAll(
                    baseArrayName.replaceAll("\\[", "\\\\[")
                        .replaceAll("]", "\\\\]") +
                            "\\[index]\\[index]\\.",
                    "");

                // Also handle single [index] in case of single-level arrays
                simplifiedCondition = simplifiedCondition.replaceAll(
                    baseArrayName.replaceAll("\\[", "\\\\[")
                        .replaceAll("]", "\\\\]") +
                            "\\[index]\\.",
                    "");

                PropertyInfo simplifiedProp =
                    new PropertyInfo(
                        prop.name(), prop.type(), prop.description(), prop.required(),
                        prop.expressionEnabled(), simplifiedCondition, prop.nestedProperties());
                simplified.add(simplifiedProp);
            } else {
                simplified.add(prop);
            }
        }

        return simplified;
    }

    /**
     * Validates union type object array where each object must match one of several schemas.
     */
    private static void validateUnionTypeObjectArray(
        JsonNode arrayJsonNode, List<PropertyInfo> unionTypes,
        String propertyPath, StringBuilder errors, StringBuilder warnings) {

        for (int i = 0; i < arrayJsonNode.size(); i++) {
            JsonNode elementJsonNode = arrayJsonNode.get(i);
            String elementPath = propertyPath + "[" + i + "]";

            if (!elementJsonNode.isObject()) {
                String actualType = JsonUtils.getJsonNodeType(elementJsonNode);
                StringUtils.appendWithNewline(
                    ValidationErrorUtils.typeError(elementPath, "object", actualType), errors);
                continue;
            }

            // Try to find a matching union type schema
            boolean foundMatch = false;

            for (PropertyInfo unionType : unionTypes) {
                // Try validating against this union type schema
                StringBuilder currentErrors = new StringBuilder();
                StringBuilder currentWarnings = new StringBuilder();

                List<PropertyInfo> schemaProperties = unionType.nestedProperties();
                if (schemaProperties != null && !schemaProperties.isEmpty()) {
                    // For union types in nested arrays, we need to simplify display conditions
                    // Transform schemaProperties to remove array path prefixes from display conditions
                    List<PropertyInfo> simplifiedProperties =
                        simplifyDisplayConditionsForUnionType(schemaProperties, propertyPath);

                    // Evaluate in the context of the current element
                    String elementContext = elementJsonNode.toString();
                    validatePropertiesFromPropertyInfo(
                        elementJsonNode, simplifiedProperties, elementPath, elementContext,
                        currentErrors, currentWarnings);

                    // If no errors, this schema matches
                    if (currentErrors.isEmpty()) {
                        foundMatch = true;
                        // Keep the warnings from the matching schema
                        if (!currentWarnings.isEmpty()) {
                            warnings.append(currentWarnings);
                        }
                        break;
                    }
                }
            }

            // If no schema matched, it's an error
            if (!foundMatch) {
                // Generate a helpful error message listing the possible types
                StringBuilder typeNames = new StringBuilder();
                for (int j = 0; j < unionTypes.size(); j++) {
                    if (j > 0) {
                        typeNames.append(", ");
                    }
                    typeNames.append(unionTypes.get(j)
                        .name());
                }
                StringUtils.appendWithNewline(
                    "Property '" + elementPath + "' does not match any of the expected union types: " + typeNames,
                    errors);
            }
        }
    }

    /**
     * Validates union type array elements using PropertyInfo.
     */
    private static void validateUnionTypeArrayFromPropertyInfo(
        JsonNode arrayJsonNode, List<PropertyInfo> allowedTypes,
        String propertyPath, StringBuilder errors) {

        for (int i = 0; i < arrayJsonNode.size(); i++) {
            JsonNode valueJsonNode = arrayJsonNode.get(i);
            boolean matchesAnyType = false;

            // Check if an element matches any of the allowed types
            for (PropertyInfo typeInfo : allowedTypes) {
                if (isTypeValid(valueJsonNode, typeInfo.type())) {
                    matchesAnyType = true;
                    break;
                }
            }

            if (!matchesAnyType) {
                String elementValue = formatValue(valueJsonNode);
                String propertyName = PropertyUtils.extractPropertyNameFromPath(propertyPath);
                String actualType = JsonUtils.getJsonNodeType(valueJsonNode);

                // Build expected types string (lowercase)
                StringBuilder expectedTypes = new StringBuilder();
                for (int j = 0; j < allowedTypes.size(); j++) {
                    if (j > 0) {
                        expectedTypes.append(" or ");
                    }
                    expectedTypes.append(allowedTypes.get(j)
                        .type()
                        .toLowerCase());
                }

                StringUtils.appendWithNewline(
                    ValidationErrorUtils.arrayElementError(
                        elementValue, propertyName, expectedTypes.toString(), actualType),
                    errors);
            }
        }
    }

    /**
     * Validates object array elements using PropertyInfo.
     */
    private static void validateObjectArrayFromPropertyInfo(
        JsonNode arrayJsonNode, List<PropertyInfo> elementProperties,
        String propertyPath, StringBuilder errors, StringBuilder warnings) {

        // Check if this is a union type array (all elementProperties are OBJECT types with nestedProperties)
        boolean isUnionTypeObjectArray = elementProperties.stream()
            .allMatch(prop -> "OBJECT".equalsIgnoreCase(prop.type()) &&
                prop.nestedProperties() != null &&
                !prop.nestedProperties()
                    .isEmpty());

        if (isUnionTypeObjectArray) {
            // Handle union type object array - each object must match at least one of the schemas
            validateUnionTypeObjectArray(arrayJsonNode, elementProperties, propertyPath, errors, warnings);
            return;
        }

        // Get the root parameters for display condition evaluation
        JsonNode rootParametersJsonNode = createRootParametersJsonNode(arrayJsonNode, propertyPath);

        for (int i = 0; i < arrayJsonNode.size(); i++) {
            JsonNode elementJsonNode = arrayJsonNode.get(i);
            String elementPath = propertyPath + "[" + i + "]";

            if (!elementJsonNode.isObject()) {
                String actualType = JsonUtils.getJsonNodeType(elementJsonNode);
                StringUtils.appendWithNewline(
                    ValidationErrorUtils.typeError(elementPath, "object", actualType), errors);
                continue;
            }

            // Check for extra properties (warnings)
            final int currentIndex = i;
            Iterator<String> fieldNamesIterator = elementJsonNode.fieldNames();

            fieldNamesIterator.forEachRemaining(fieldName -> {
                PropertyInfo matchingProperty = elementProperties.stream()
                    .filter(prop -> fieldName.equals(prop.name()))
                    .findFirst()
                    .orElse(null);

                if (matchingProperty == null) {
                    // Property is not defined in schema at all
                    StringUtils.appendWithNewline(
                        "Property '" + propertyPath + "[index]." + fieldName + "' is not defined in task definition",
                        warnings);
                } else if (matchingProperty.displayCondition() != null && !matchingProperty.displayCondition()
                    .isEmpty()) {
                    // Property is defined but check if it should be visible based on display condition
                    String condition = matchingProperty.displayCondition();
                    String resolvedCondition = replaceIndexPlaceholder(condition, currentIndex);

                    try {
                        boolean shouldShowProperty = WorkflowUtils.extractAndEvaluateCondition(
                            resolvedCondition, rootParametersJsonNode);

                        if (!shouldShowProperty) {
                            // Property exists but display condition is false - generate warning
                            StringUtils.appendWithNewline(
                                "Property '" + propertyPath + "[" + currentIndex + "]." + fieldName +
                                    "' is not defined in task definition",
                                warnings);
                        }
                    } catch (Exception e) {
                        if (logger.isTraceEnabled()) {
                            logger.trace(e.getMessage());
                        }
                    }
                }
            });

            // Validate each property in the element
            for (PropertyInfo propertyInfo : elementProperties) {
                String fieldName = propertyInfo.name();
                String fieldPath = elementPath + "." + fieldName;
                boolean isRequired = propertyInfo.required();

                // Check display condition
                boolean shouldValidateProperty = true;
                if (propertyInfo.displayCondition() != null && !propertyInfo.displayCondition()
                    .isEmpty()) {
                    String condition = propertyInfo.displayCondition();
                    String resolvedCondition = replaceIndexPlaceholder(condition, currentIndex);

                    try {
                        shouldValidateProperty = WorkflowUtils.extractAndEvaluateCondition(
                            resolvedCondition, rootParametersJsonNode);
                    } catch (Exception e) {
                        if (logger.isTraceEnabled()) {
                            logger.trace(e.getMessage());
                        }
                    }
                }

                if (shouldValidateProperty) {
                    // Property should be validated based on the display condition
                    if (isRequired && !elementJsonNode.has(fieldName)) {
                        StringUtils.appendWithNewline("Missing required property: " + fieldPath, errors);
                    } else if (elementJsonNode.has(fieldName)) {
                        JsonNode valueJsonNode = elementJsonNode.get(fieldName);

                        // Skip type validation for data pill expressions
                        if (!valueJsonNode.isTextual() || !isDataPillExpression(valueJsonNode.asText())) {
                            validateSimpleType(valueJsonNode, propertyInfo.type(), fieldPath, errors);
                        }
                    }
                }
            }
        }
    }

    /**
     * Validates simple type (STRING, INTEGER, etc.) using PropertyInfo.
     */
    private static void validateSimpleType(
        JsonNode valueJsonNode, String expectedType, String propertyPath, StringBuilder errors) {

        // Handle format validation for date/time types
        if (valueJsonNode.isTextual() && isDateTimeType(expectedType)) {
            String formatError = validateDateTimeTypeWithError(
                valueJsonNode.asText(), expectedType, propertyPath);

            if (formatError != null) {
                StringUtils.appendWithNewline(formatError, errors);
            }
        } else if (!isTypeValid(valueJsonNode, expectedType)) {
            String actualType = JsonUtils.getJsonNodeType(valueJsonNode);
            // Convert type to lowercase for error messages
            String normalizedExpectedType = expectedType.toLowerCase();
            StringUtils.appendWithNewline(
                ValidationErrorUtils.typeError(propertyPath, normalizedExpectedType, actualType), errors);
        }
    }
}
