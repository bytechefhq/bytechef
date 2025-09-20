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

package com.bytechef.ai.mcp.tool.platform.validator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for validating individual fields and their properties. Contains validation logic for required fields,
 * field types, and field patterns.
 */
public class FieldValidator {

    private static final Pattern TYPE_PATTERN = Pattern.compile("^[a-zA-Z0-9]+/v[0-9]+(/[a-zA-Z0-9]+)?$");
    private static final String REQUIRED_MARKER = "(required)";

    private FieldValidator() {
        // Utility class
    }

    /**
     * Checks if a string value is a data pill expression (e.g., "${taskName.property}").
     */
    private static boolean isDataPillExpression(String value) {
        return value != null && value.matches("\\$\\{[^}]+}");
    }

    /**
     * Validates that a required string field exists and is of correct type.
     */
    public static void validateRequiredStringField(JsonNode node, String fieldName, StringBuilder errors) {
        if (!node.has(fieldName)) {
            ValidationErrorBuilder.appendWithNewline(errors, "Missing required field: " + fieldName);
        } else if (!node.get(fieldName)
            .isTextual()) {
            ValidationErrorBuilder.appendWithNewline(errors, "Field '" + fieldName + "' must be a string");
        }
    }

    /**
     * Validates that a required array field exists and is of correct type.
     */
    public static void validateRequiredArrayField(JsonNode node, String fieldName, StringBuilder errors) {
        if (!node.has(fieldName)) {
            ValidationErrorBuilder.appendWithNewline(errors, "Missing required field: " + fieldName);
        } else if (!node.get(fieldName)
            .isArray()) {
            ValidationErrorBuilder.appendWithNewline(errors, "Field '" + fieldName + "' must be an array");
        }
    }

    /**
     * Validates that a required object field exists and is of correct type.
     */
    public static void validateRequiredObjectField(JsonNode node, String fieldName, StringBuilder errors) {
        if (!node.has(fieldName)) {
            ValidationErrorBuilder.appendWithNewline(errors, "Missing required field: " + fieldName);
        } else if (!node.get(fieldName)
            .isObject()) {
            ValidationErrorBuilder.appendWithNewline(errors, "Field '" + fieldName + "' must be an object");
        }
    }

    /**
     * Validates workflow triggers field structure and constraints.
     */
    public static void validateWorkflowTriggers(JsonNode workflowNode, StringBuilder errors) {
        if (!workflowNode.has("triggers")) {
            ValidationErrorBuilder.appendWithNewline(errors, "Missing required field: triggers");
        } else {
            JsonNode triggers = workflowNode.get("triggers");
            if (!triggers.isArray()) {
                ValidationErrorBuilder.appendWithNewline(errors, "Field 'triggers' must be an array");
            } else if (triggers.size() > 1) {
                ValidationErrorBuilder.appendWithNewline(errors, "Field 'triggers' must contain one or less objects");
            } else if (triggers.size() == 1 && !triggers.get(0)
                .isObject()) {
                ValidationErrorBuilder.appendWithNewline(errors, "Trigger must be an object");
            }
        }
    }

    /**
     * Validates task type field against required pattern.
     */
    public static void validateTaskType(JsonNode taskNode, StringBuilder errors) {
        if (!taskNode.has("type")) {
            ValidationErrorBuilder.appendWithNewline(errors, "Missing required field: type");
        } else if (!taskNode.get("type")
            .isTextual()) {
            ValidationErrorBuilder.appendWithNewline(errors, "Field 'type' must be a string");
        } else {
            String typeValue = taskNode.get("type")
                .asText();
            if (!TYPE_PATTERN.matcher(typeValue)
                .matches()) {
                ValidationErrorBuilder.appendWithNewline(errors,
                    "Field 'type' must match pattern: (alphanumeric)+/v(numeric)+(/(alphanumeric)+)?");
            }
        }
    }

    /**
     * Validates a property definition and its value, handling required properties and type checking.
     */
    public static void validateStringTypeDefinition(
        JsonNode currentNode, String propertyName, String propertyDef,
        String propertyPath, StringBuilder errors, StringBuilder warnings,
        String originalTaskDefinition, String originalCurrentParams) {
        if (propertyDef.contains("@")) {
            try {
                boolean shouldInclude = WorkflowParser.extractAndEvaluateCondition(propertyDef,
                    WorkflowParser.parseJsonString(originalCurrentParams));
                if (!shouldInclude) {
                    return; // Skip validation if condition is false
                }
            } catch (Exception e) {
                errors.append(e.getMessage());
            }
        }

        boolean isRequired = propertyDef.contains(REQUIRED_MARKER);
        String expectedType = propertyDef.replace(REQUIRED_MARKER, "")
            .trim();

        // Remove inline conditions from type definition
        expectedType = expectedType.replaceAll("@[^@]+@", "")
            .trim();

        if (isRequired && !currentNode.has(propertyName)) {
            ValidationErrorBuilder.appendWithNewline(errors, ValidationErrorBuilder.missingProperty(propertyPath));
        } else if (currentNode.has(propertyName)) {
            JsonNode currentValue = currentNode.get(propertyName);

            if ("object".equalsIgnoreCase(expectedType) && currentValue.isObject()) {
                return;
            }

            // Inline type validation to avoid wrapper function
            // Skip type validation for data pill expressions
            if (currentValue.isTextual() && isDataPillExpression(currentValue.asText())) {
                // Data pill expressions will be validated separately by validateTaskDataPills
                return;
            }

            // Handle format validation for date/time types
            if (currentValue.isTextual() && isDateTimeType(expectedType)) {
                String formatError = validateDateTimeTypeWithError(currentValue.asText(), expectedType, propertyPath);
                if (formatError != null) {
                    ValidationErrorBuilder.append(errors, formatError);
                }
            } else if (!isTypeValid(currentValue, expectedType)) {
                String actualType = JsonUtils.getJsonNodeType(currentValue);
                ValidationErrorBuilder.append(errors,
                    ValidationErrorBuilder.typeError(propertyPath, expectedType, actualType));
            }
        }
    }

    /**
     * Validates array property and its elements.
     */
    public static void validateArrayProperty(
        JsonNode currentNode, String propertyName, JsonNode defValue, String propertyPath, StringBuilder errors) {
        if (!currentNode.has(propertyName))
            return;

        JsonNode currentValue = currentNode.get(propertyName);
        if (!currentValue.isArray()) {
            String actualType = JsonUtils.getJsonNodeType(currentValue);
            ValidationErrorBuilder.append(errors, ValidationErrorBuilder.typeError(propertyPath, "array", actualType));
            return;
        }

        JsonNode firstElement = defValue.get(0);
        boolean isUnionType = false;

        // Union type: array definition like ["string", "integer"]
        // Object type: array definition like [{"name": "string", "age": "integer"}]
        if (firstElement.isTextual()) {
            // Check if all elements are simple text types (union)
            isUnionType = true;
            for (int i = 0; i < defValue.size(); i++) {
                if (!defValue.get(i)
                    .isTextual()) {
                    isUnionType = false;
                    break;
                }
            }
        }

        if (isUnionType) {
            // Array of simple types (union type)
            validateUnionArrayElements(currentValue, defValue, propertyPath, errors);
        } else {
            // Array of objects
            JsonNode arrayElementDef = defValue.get(0);
            validateObjectArrayElements(currentValue, arrayElementDef, propertyPath, errors);
        }
    }

    /**
     * Validates array elements that can be one of multiple simple types (union types).
     */
    private static void validateUnionArrayElements(
        JsonNode arrayValue, JsonNode allowedTypes, String propertyPath, StringBuilder errors) {
        for (int i = 0; i < arrayValue.size(); i++) {
            JsonNode element = arrayValue.get(i);
            boolean matchesAnyType = false;

            // Check if element matches any of the allowed types
            for (int j = 0; j < allowedTypes.size(); j++) {
                String allowedType = allowedTypes.get(j)
                    .asText();
                if (isTypeValid(element, allowedType)) {
                    matchesAnyType = true;
                    break;
                }
            }

            if (!matchesAnyType) {
                String elementValue = WorkflowParser.formatElementValue(element);
                String propertyName = PropertyUtils.extractPropertyNameFromPath(propertyPath);
                String actualType = JsonUtils.getJsonNodeType(element);

                // Build expected types string
                StringBuilder expectedTypes = new StringBuilder();
                for (int j = 0; j < allowedTypes.size(); j++) {
                    if (j > 0)
                        expectedTypes.append(" or ");
                    expectedTypes.append(allowedTypes.get(j)
                        .asText());
                }

                ValidationErrorBuilder.append(errors, ValidationErrorBuilder.arrayElementError(
                    elementValue, propertyName, expectedTypes.toString(), actualType));
            }
        }
    }

    /**
     * Validates array elements that are objects with defined properties.
     */
    private static void validateObjectArrayElements(
        JsonNode arrayValue, JsonNode objectDef, String propertyPath, StringBuilder errors) {
        for (int i = 0; i < arrayValue.size(); i++) {
            JsonNode element = arrayValue.get(i);
            String elementPath = propertyPath + "[" + i + "]";

            if (!element.isObject()) {
                String actualType = JsonUtils.getJsonNodeType(element);
                ValidationErrorBuilder.append(errors,
                    ValidationErrorBuilder.typeError(elementPath, "object", actualType));
                continue;
            }

            // Validate each required property in the object
            objectDef.fieldNames()
                .forEachRemaining(fieldName -> {
                    JsonNode fieldDef = objectDef.get(fieldName);
                    String fieldPath = elementPath + "." + fieldName;

                    if (fieldDef.isTextual()) {
                        String fieldDefText = fieldDef.asText();
                        boolean isRequired = fieldDefText.contains("(required)");

                        if (isRequired && !element.has(fieldName)) {
                            ValidationErrorBuilder.append(errors, "Missing required property: " + fieldPath);
                        } else if (element.has(fieldName)) {
                            String expectedType = fieldDefText.replace("(required)", "")
                                .trim();
                            JsonNode actualValue = element.get(fieldName);

                            if (!isTypeValid(actualValue, expectedType)) {
                                String actualType = JsonUtils.getJsonNodeType(actualValue);
                                ValidationErrorBuilder.append(errors,
                                    ValidationErrorBuilder.typeError(fieldPath, expectedType, actualType));
                            }
                        }
                    }
                });
        }
    }

    /**
     * Validates array elements that are objects and generates warnings for undefined properties.
     */
    public static void validateObjectArrayElementsWithWarnings(
        JsonNode arrayValue, JsonNode objectDef, String propertyPath, StringBuilder errors, StringBuilder warnings) {
        // Get the root parameters for display condition evaluation
        JsonNode rootParameters = findRootParameters(arrayValue, propertyPath);

        for (int i = 0; i < arrayValue.size(); i++) {
            JsonNode element = arrayValue.get(i);
            String elementPath = propertyPath + "[" + i + "]";

            if (!element.isObject()) {
                String actualType = JsonUtils.getJsonNodeType(element);
                ValidationErrorBuilder.append(errors,
                    ValidationErrorBuilder.typeError(elementPath, "object", actualType));
                continue;
            }

            // Check for extra properties (warnings) - considering display conditions
            final int currentIndex = i;
            element.fieldNames()
                .forEachRemaining(fieldName -> {
                    JsonNode fieldDef = objectDef.get(fieldName);
                    if (fieldDef == null) {
                        // Property not defined in schema at all
                        ValidationErrorBuilder.append(warnings, "Property '" + propertyPath + "[index]." + fieldName
                            + "' is not defined in task definition");
                    } else if (fieldDef.isTextual()) {
                        // Property is defined but check if it should be visible based on display condition
                        String fieldDefText = fieldDef.asText();
                        if (hasDisplayCondition(fieldDefText)) {
                            String condition = extractDisplayCondition(fieldDefText);
                            String resolvedCondition = replaceIndexPlaceholder(condition, currentIndex);

                            boolean shouldShowProperty = WorkflowParser
                                .extractAndEvaluateCondition("@" + resolvedCondition + "@", rootParameters);
                            if (!shouldShowProperty) {
                                // Property exists but display condition is false - generate warning
                                ValidationErrorBuilder.append(warnings, "Property '" + propertyPath + "["
                                    + currentIndex + "]." + fieldName + "' is not defined in task definition");
                            }
                        }
                    }
                });

            // Validate each property in the object definition
            objectDef.fieldNames()
                .forEachRemaining(fieldName -> {
                    JsonNode fieldDef = objectDef.get(fieldName);
                    String fieldPath = elementPath + "." + fieldName;

                    if (fieldDef.isTextual()) {
                        String fieldDefText = fieldDef.asText();
                        boolean isRequired = fieldDefText.contains("(required)");
                        boolean shouldValidateProperty = true;

                        // Check display condition
                        if (hasDisplayCondition(fieldDefText)) {
                            String condition = extractDisplayCondition(fieldDefText);
                            String resolvedCondition = replaceIndexPlaceholder(condition, currentIndex);

                            try {
                                shouldValidateProperty = WorkflowParser
                                    .extractAndEvaluateCondition("@" + resolvedCondition + "@", rootParameters);
                            } catch (Exception e) {
                                // If condition evaluation fails, assume property should be validated
                                shouldValidateProperty = true;
                            }
                        }

                        if (shouldValidateProperty) {
                            // Property should be validated based on display condition
                            if (isRequired && !element.has(fieldName)) {
                                ValidationErrorBuilder.append(errors, "Missing required property: " + fieldPath);
                            } else if (element.has(fieldName)) {
                                String expectedType = fieldDefText.replaceAll("@[^@]*@", "")
                                    .replace("(required)", "")
                                    .trim();
                                JsonNode actualValue = element.get(fieldName);

                                if (!isTypeValid(actualValue, expectedType)) {
                                    String actualType = JsonUtils.getJsonNodeType(actualValue);
                                    ValidationErrorBuilder.append(errors,
                                        ValidationErrorBuilder.typeError(fieldPath, expectedType, actualType));
                                }
                            }
                        }
                    }
                });
        }
    }

    /**
     * Checks if a field definition has a display condition.
     */
    private static boolean hasDisplayCondition(String fieldDefText) {
        return fieldDefText.contains("@") && fieldDefText.matches(".*@[^@]+@.*");
    }

    /**
     * Extracts the display condition from a field definition text.
     */
    private static String extractDisplayCondition(String fieldDefText) {
        Pattern pattern = Pattern.compile("@([^@]+)@");
        Matcher matcher = pattern.matcher(fieldDefText);
        if (matcher.find()) {
            return matcher.group(1)
                .trim();
        }
        return "";
    }

    /**
     * Replaces [index] placeholder in conditions with actual index.
     */
    private static String replaceIndexPlaceholder(String condition, int index) {
        return condition.replace("[index]", "[" + index + "]");
    }

    /**
     * Finds the root parameters node for display condition evaluation.
     */
    private static JsonNode findRootParameters(JsonNode arrayValue, String propertyPath) {
        // For array validation, we need to construct the full parameter structure
        // to evaluate conditions like "items[0].age >= 18"

        // Extract the array property name from the path (e.g., "items" from "items")
        String arrayPropertyName = propertyPath;

        // Create a root node that contains the array
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode rootNode = mapper.createObjectNode();
        rootNode.set(arrayPropertyName, arrayValue);

        return rootNode;
    }

    /**
     * Checks if a JsonNode matches the expected type.
     */
    private static boolean isTypeValid(JsonNode node, String expectedType) {
        // Null values are allowed for all types except when explicitly expecting null type
        if (node.isNull() && !"null".equalsIgnoreCase(expectedType)) {
            return true;
        }

        return switch (expectedType.toLowerCase()) {
            case "string" -> node.isTextual();
            case "float" -> node.isFloatingPointNumber();
            case "integer" -> node.isIntegralNumber();
            case "number" -> node.isNumber();
            case "boolean" -> node.isBoolean();
            case "array" -> node.isArray();
            case "object" -> node.isObject();
            case "null" -> node.isNull();
            case "date" -> node.isTextual() && validateDateFormat(node.asText());
            case "time" -> node.isTextual() && validateTimeFormat(node.asText());
            case "date_time" -> node.isTextual() && validateDateTimeFormat(node.asText());
            default -> true;
        };
    }

    /**
     * Validates DATE format (yyyy-MM-dd).
     */
    private static boolean validateDateFormat(String dateValue) {
        if (dateValue == null || dateValue.trim()
            .isEmpty()) {
            return false;
        }

        // Check format pattern first
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
            if (month < 1 || month > 12)
                return false;
            if (day < 1 || day > 31)
                return false;

            // Check days in month
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
     * Validates TIME format (hh:mm:ss).
     */
    private static boolean validateTimeFormat(String timeValue) {
        if (timeValue == null || timeValue.trim()
            .isEmpty()) {
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

            return hours >= 0 && hours <= 23 &&
                minutes >= 0 && minutes <= 59 &&
                seconds >= 0 && seconds <= 59;
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            return false;
        }
    }

    /**
     * Validates DATE_TIME format (yyyy-MM-ddThh:mm:ss).
     */
    private static boolean validateDateTimeFormat(String dateTimeValue) {
        if (dateTimeValue == null || dateTimeValue.trim()
            .isEmpty()) {
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
     * Checks if a year is a leap year.
     */
    private static boolean isLeapYear(int year) {
        return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0);
    }

    /**
     * Checks if the expected type is a date/time type that needs format validation.
     */
    private static boolean isDateTimeType(String expectedType) {
        String lowerType = expectedType.toLowerCase();
        return "date".equals(lowerType) || "time".equals(lowerType) || "date_time".equals(lowerType);
    }

    /**
     * Validates date/time format and returns specific error message if invalid.
     */
    private static String validateDateTimeTypeWithError(String value, String expectedType, String propertyPath) {
        String lowerType = expectedType.toLowerCase();

        switch (lowerType) {
            case "date":
                if (!value.matches("\\d{4}-\\d{2}-\\d{2}")) {
                    return "Property '" + propertyPath
                        + "' is in incorrect date format. Format should be in: 'yyyy-MM-dd'";
                }
                if (!validateDateFormat(value)) {
                    return "Property '" + propertyPath + "' is in incorrect date format. Impossible date: " + value;
                }
                break;

            case "time":
                if (!value.matches("\\d{2}:\\d{2}:\\d{2}")) {
                    return "Property '" + propertyPath
                        + "' is in incorrect time format. Format should be in: 'hh:mm:ss'";
                }
                if (!validateTimeFormat(value)) {
                    return "Property '" + propertyPath + "' is in incorrect time format. Impossible time: " + value;
                }
                break;

            case "date_time":
                if (!value.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}")) {
                    return "Property '" + propertyPath
                        + "' has incorrect type. Format should be in: 'yyyy-MM-ddThh:mm:ss'";
                }
                if (!validateDateTimeFormat(value)) {
                    return "Property '" + propertyPath
                        + "' has incorrect type. Format should be in: 'yyyy-MM-ddThh:mm:ss'";
                }
                break;
            default:
        }

        return null; // No error
    }

    /**
     * Validates missing object properties that have required fields.
     */
    public static void
        validateMissingObjectWithRequiredFields(JsonNode defValue, String propertyPath, StringBuilder errors) {
        defValue.fieldNames()
            .forEachRemaining(fieldName -> {
                JsonNode fieldDef = defValue.get(fieldName);
                String fullFieldPath = PropertyUtils.buildPropertyPath(propertyPath, fieldName);

                if (fieldDef.isTextual() && fieldDef.asText()
                    .contains(REQUIRED_MARKER)) {
                    ValidationErrorBuilder.appendWithNewline(errors,
                        ValidationErrorBuilder.missingProperty(fullFieldPath));
                } else if (fieldDef.isObject()) {
                    validateMissingObjectWithRequiredFields(fieldDef, fullFieldPath, errors);
                }
            });
    }
}
