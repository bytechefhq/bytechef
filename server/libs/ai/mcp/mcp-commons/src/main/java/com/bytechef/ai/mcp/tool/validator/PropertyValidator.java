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

package com.bytechef.ai.mcp.tool.validator;

import com.bytechef.ai.mcp.tool.util.JsonUtils;
import com.bytechef.ai.mcp.tool.util.PropertyUtils;
import com.bytechef.ai.mcp.tool.util.ValidationErrorUtils;
import com.bytechef.ai.mcp.tool.util.WorkflowUtils;
import com.bytechef.commons.util.StringUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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

    private static final String REQUIRED_MARKER = "(required)";

    private PropertyValidator() {
    }

    /**
     * Recursively validates properties in current parameters against their definition.
     */
    public static void validatePropertiesRecursively(
        JsonNode currentJsonNode, JsonNode definitionValueJsonNode, String path, StringBuilder errors,
        StringBuilder warnings, String originalTaskDefinition, String originalCurrentParams) {

        // Check for extra properties (generate warnings)
        if (isEmptyContainer(definitionValueJsonNode)) {
            generateWarningsForAllProperties(currentJsonNode, path, warnings);
        } else {
            Iterator<String> stringIterator = currentJsonNode.fieldNames();

            stringIterator.forEachRemaining(fieldName -> {
                if (!definitionValueJsonNode.has(fieldName)) {
                    String propertyPath = PropertyUtils.buildPropertyPath(path, fieldName);
                    JsonNode valueJsonNode = currentJsonNode.get(fieldName);

                    StringUtils.appendWithNewline(warnings, ValidationErrorUtils.notDefined(propertyPath));

                    if (valueJsonNode.isObject()) {
                        generateWarningsForAllProperties(valueJsonNode, propertyPath, warnings);
                    }
                }
            });
        }

        // Validate defined properties
        Iterator<String> stringIterator = definitionValueJsonNode.fieldNames();

        stringIterator.forEachRemaining(fieldName -> {
            JsonNode valueJsonNode = definitionValueJsonNode.get(fieldName);

            String propertyPath = PropertyUtils.buildPropertyPath(path, fieldName);

            if (valueJsonNode.isTextual()) {
                handleTextualProperty(
                    currentJsonNode, fieldName, valueJsonNode, propertyPath, errors, warnings, originalCurrentParams);
            } else if (valueJsonNode.isObject()) {
                validateNestedObject(
                    currentJsonNode, fieldName, valueJsonNode, propertyPath, errors, warnings, originalTaskDefinition,
                    originalCurrentParams);
            } else if (valueJsonNode.isArray() && !valueJsonNode.isEmpty()) {
                validateArrayProperty(currentJsonNode, fieldName, valueJsonNode, propertyPath, errors, warnings);
            }
        });
    }

    /**
     * Recursively validates properties with array support for display conditions.
     */
    public static void validatePropertiesRecursively(
        JsonNode currentJsonNode, JsonNode definitionValueJsonNode, String path, StringBuilder errors,
        StringBuilder warnings, String originalTaskDefinition, String originalTaskDefinitionForArrays,
        String originalCurrentParams) {

        // Check for extra properties (generate warnings)
        if (isEmptyContainer(definitionValueJsonNode)) {
            generateWarningsForAllProperties(currentJsonNode, path, warnings);
        } else {
            Iterator<String> stringIterator = currentJsonNode.fieldNames();

            stringIterator.forEachRemaining(fieldName -> {
                if (!definitionValueJsonNode.has(fieldName)) {
                    String propertyPath = PropertyUtils.buildPropertyPath(path, fieldName);
                    JsonNode currentValueJsonNode = currentJsonNode.get(fieldName);

                    StringUtils.appendWithNewline(warnings, ValidationErrorUtils.notDefined(propertyPath));

                    if (currentValueJsonNode.isObject()) {
                        generateWarningsForAllProperties(currentValueJsonNode, propertyPath, warnings);
                    }
                }
            });
        }

        // Validate defined properties
        Iterator<String> stringIterator = definitionValueJsonNode.fieldNames();

        stringIterator.forEachRemaining(propertyName -> {
            JsonNode valueJsonNode = definitionValueJsonNode.get(propertyName);
            String propertyPath = PropertyUtils.buildPropertyPath(path, propertyName);

            if (valueJsonNode.isTextual()) {
                handleTextualProperty(
                    currentJsonNode, propertyName, valueJsonNode, propertyPath, errors, warnings,
                    originalCurrentParams);
            } else if (valueJsonNode.isObject()) {
                validateNestedObject(
                    currentJsonNode, propertyName, valueJsonNode, propertyPath, errors, warnings,
                    originalTaskDefinition, originalTaskDefinitionForArrays, originalCurrentParams);
            } else if (valueJsonNode.isArray() && !valueJsonNode.isEmpty()) {
                validateArrayPropertyWithArraySupport(
                    currentJsonNode, propertyName, valueJsonNode, propertyPath, errors, warnings,
                    originalTaskDefinitionForArrays);
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

    /**
     * Extracts the display condition from a field definition text.
     */
    private static String extractDisplayCondition(String fieldDefText) {
        Pattern pattern = Pattern.compile("@([^@]+)@");
        Matcher matcher = pattern.matcher(fieldDefText);

        if (matcher.find()) {
            return org.apache.commons.lang3.StringUtils.trim(matcher.group(1));
        }

        return "";
    }

    private static String formatElementValue(JsonNode element) {
        return element.isTextual() ? "'" + element.asText() + "'" : element.toString();
    }

    /**
     * Generates warnings for all properties recursively.
     */
    private static void generateWarningsForAllProperties(
        JsonNode currentJsonNode, String path, StringBuilder warnings) {

        Iterator<String> stringIterator = currentJsonNode.fieldNames();

        stringIterator.forEachRemaining(fieldName -> {
            String propertyPath = buildPropertyPath(path, fieldName);
            JsonNode valueJsonNode = currentJsonNode.get(fieldName);

            StringUtils.appendWithNewline(warnings, ValidationErrorUtils.notDefined(propertyPath));

            if (valueJsonNode.isObject()) {
                generateWarningsForAllProperties(valueJsonNode, propertyPath, warnings);
            }
        });
    }

    /**
     * Handles validation for textual property definitions (like "string (required)").
     */
    private static void handleTextualProperty(
        JsonNode currentJsonNode, String propertyName, JsonNode definitionValueJsonNode, String propertyPath,
        StringBuilder errors, StringBuilder warnings, String originalCurrentParams) {

        String text = definitionValueJsonNode.asText();

        String definitionText = org.apache.commons.lang3.StringUtils.trim(text.replace("(required)", ""));

        if (("object".equalsIgnoreCase(definitionText) || "array".equalsIgnoreCase(definitionText)) &&
            currentJsonNode.has(propertyName)) {

            JsonNode currentValueJsonNode = currentJsonNode.get(propertyName);

            boolean correctType = ("object".equalsIgnoreCase(definitionText) && currentValueJsonNode.isObject()) ||
                ("array".equalsIgnoreCase(definitionText) && currentValueJsonNode.isArray());

            if (correctType && "object".equalsIgnoreCase(definitionText)) {
                // Generate warnings for all nested properties since they're not defined
                Iterator<String> stringIterator = currentValueJsonNode.fieldNames();

                stringIterator.forEachRemaining(fieldName -> {
                    String fieldPath = buildPropertyPath(propertyPath, fieldName);
                    StringUtils.appendWithNewline(warnings, ValidationErrorUtils.notDefined(fieldPath));
                });

                return;
            }
        }

        validateStringTypeDefinition(currentJsonNode, propertyName, text, propertyPath, errors, originalCurrentParams);
    }

    /**
     * Checks if a field definition has a display condition.
     */
    private static boolean hasDisplayCondition(String fieldDefinitionText) {
        return fieldDefinitionText.contains("@") && fieldDefinitionText.matches(".*@[^@]+@.*");
    }

    private static boolean isDataPillExpression(String value) {
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
        // Null values are allowed for all types except when explicitly expecting null type
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
        JsonNode currentJsonNode, String propertyName, JsonNode definitionValueJsonNode, String propertyPath,
        StringBuilder errors, StringBuilder warnings) {

        if (!currentJsonNode.has(propertyName)) {
            return;
        }

        JsonNode currentValueJsonNode = currentJsonNode.get(propertyName);

        if (!currentValueJsonNode.isArray()) {
            String actualType = JsonUtils.getJsonNodeType(currentValueJsonNode);
            StringUtils.appendWithNewline(errors, ValidationErrorUtils.typeError(propertyPath, "array", actualType));

            return;
        }

        // Check if this is a union type or object array
        JsonNode firstElementJsonNode = definitionValueJsonNode.get(0);

        boolean isUnionType = false;

        if (firstElementJsonNode.isTextual()) {
            // Check if all elements are simple text types (union)
            isUnionType = true;
            for (int i = 0; i < definitionValueJsonNode.size(); i++) {
                if (!definitionValueJsonNode.get(i)
                    .isTextual()) {
                    isUnionType = false;
                    break;
                }
            }
        }

        if (isUnionType) {
            // Array of simple types - use existing validation without warnings
            validateArrayProperty(currentJsonNode, propertyName, definitionValueJsonNode, propertyPath, errors);
        } else {
            // Array of objects - use new validation with warnings
            JsonNode arrayElementDefinitionJsonNode = definitionValueJsonNode.get(0);
            validateObjectArrayElementsWithWarnings(
                currentValueJsonNode, arrayElementDefinitionJsonNode, propertyPath, errors, warnings);
        }
    }

    /**
     * Validates array property and its elements.
     */
    private static void validateArrayProperty(
        JsonNode currentNode, String propertyName, JsonNode defValueJsonNode, String propertyPath,
        StringBuilder errors) {

        if (!currentNode.has(propertyName))
            return;

        JsonNode currentValue = currentNode.get(propertyName);

        if (!currentValue.isArray()) {
            String actualType = JsonUtils.getJsonNodeType(currentValue);

            StringUtils.appendWithNewline(errors, ValidationErrorUtils.typeError(propertyPath, "array", actualType));

            return;
        }

        JsonNode firstElementJsonNode = defValueJsonNode.get(0);
        boolean isUnionType = false;

        // Union type: array definition like ["string", "integer"]
        // Object type: array definition like [{"name": "string", "age": "integer"}]
        if (firstElementJsonNode.isTextual()) {
            // Check if all elements are simple text types (union)
            isUnionType = true;

            for (int i = 0; i < defValueJsonNode.size(); i++) {
                JsonNode jsonNode = defValueJsonNode.get(i);

                if (!jsonNode.isTextual()) {
                    isUnionType = false;

                    break;
                }
            }
        }

        if (isUnionType) {
            // Array of simple types (union type)
            validateUnionArrayElements(currentValue, defValueJsonNode, propertyPath, errors);
        } else {
            // Array of objects
            JsonNode arrayElementDefJsonNode = defValueJsonNode.get(0);

            validateObjectArrayElements(currentValue, arrayElementDefJsonNode, propertyPath, errors);
        }
    }

    /**
     * Validates array property with array support for display conditions.
     */
    private static void validateArrayPropertyWithArraySupport(
        JsonNode currentJsonNode, String propertyName, JsonNode definitionValueJsonNode, String propertyPath,
        StringBuilder errors, StringBuilder warnings, String originalTaskDefinitionForArrays) {

        if (!currentJsonNode.has(propertyName)) {
            return;
        }

        JsonNode valueJsonNode = currentJsonNode.get(propertyName);

        if (!valueJsonNode.isArray()) {
            String actualType = JsonUtils.getJsonNodeType(valueJsonNode);

            StringUtils.appendWithNewline(errors, ValidationErrorUtils.typeError(propertyPath, "array", actualType));

            return;
        }

        // Check if this is a union type (array of simple types) or object array
        boolean isUnionType = false;

        if (!definitionValueJsonNode.isEmpty()) {
            JsonNode firstElementJsonNode = definitionValueJsonNode.get(0);

            if (firstElementJsonNode.isTextual()) {
                // Check if all elements are simple text types (union type)
                isUnionType = true;

                for (int i = 0; i < definitionValueJsonNode.size(); i++) {
                    JsonNode jsonNode = definitionValueJsonNode.get(i);

                    if (!jsonNode.isTextual()) {
                        isUnionType = false;

                        break;
                    }
                }
            }
        }

        if (isUnionType) {
            // Check if this is a TASK type array first
            if (definitionValueJsonNode.size() == 1) {
                JsonNode jsonNode = definitionValueJsonNode.get(0);

                if (jsonNode.isTextual() && "task".equalsIgnoreCase(jsonNode.asText())) {
                    // Handle TASK type arrays specially
                    TaskValidator.validateTaskArray(valueJsonNode, propertyPath, errors);

                    return;
                }
            }
            // Array of simple types - use existing validation without warnings
            validateArrayProperty(currentJsonNode, propertyName, definitionValueJsonNode, propertyPath, errors);
        } else if (!definitionValueJsonNode.isEmpty()) {
            JsonNode arrayElementJsonNode = definitionValueJsonNode.get(0);

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
                            errors, ValidationErrorUtils.typeError(subArrayPath, "array", actualType));

                        continue;
                    }

                    // Recursively validate the sub-array using the array element definition
                    try {
                        JsonNode originalTaskDefinitionJsonNode = com.bytechef.commons.util.JsonUtils.readTree(
                            originalTaskDefinitionForArrays);
                        JsonNode originalParametersJsonNode = originalTaskDefinitionJsonNode.get("parameters");
                        JsonNode originalArrayDefinitionJsonNode = WorkflowUtils.getNestedField(
                            originalParametersJsonNode, propertyName);

                        if (originalArrayDefinitionJsonNode != null && originalArrayDefinitionJsonNode.isArray() &&
                            !originalArrayDefinitionJsonNode.isEmpty()) {

                            JsonNode originalSubArrayElementDefinitionJsonNode = originalArrayDefinitionJsonNode.get(0);
                            validateDiscriminatedUnionArray(
                                subArrayJsonNode, originalSubArrayElementDefinitionJsonNode, subArrayPath, errors,
                                warnings);
                        } else {
                            // Fallback to processed definition
                            validateDiscriminatedUnionArray(
                                subArrayJsonNode, arrayElementJsonNode, subArrayPath, errors, warnings);
                        }
                    } catch (Exception e) {
                        // Fallback to processed definition
                        validateDiscriminatedUnionArray(
                            subArrayJsonNode, arrayElementJsonNode, subArrayPath, errors, warnings);
                    }
                }
            } else {
                // Array of objects - use the original task definition to get array element definition with display
                // conditions
                try {
                    JsonNode originalTaskDefNode = com.bytechef.commons.util.JsonUtils.readTree(
                        originalTaskDefinitionForArrays);
                    JsonNode originalParametersNode = originalTaskDefNode.get("parameters");
                    JsonNode originalArrayDef = WorkflowUtils.getNestedField(originalParametersNode, propertyName);

                    if (originalArrayDef != null && originalArrayDef.isArray() && !originalArrayDef.isEmpty()) {
                        JsonNode originalArrayElementDef = originalArrayDef.get(0);

                        validateObjectArrayElementsWithWarnings(
                            valueJsonNode, originalArrayElementDef, propertyPath, errors, warnings);
                    } else {
                        // Fallback to processed definition
                        validateObjectArrayElementsWithWarnings(
                            valueJsonNode, arrayElementJsonNode, propertyPath, errors, warnings);
                    }
                } catch (Exception e) {
                    // Fallback to processed definition
                    validateObjectArrayElementsWithWarnings(
                        valueJsonNode, arrayElementJsonNode, propertyPath, errors, warnings);
                }
            }
        }
    }

    /**
     * Validates DATE format (yyyy-MM-dd).
     */
    private static boolean validateDateFormat(String dateValue) {
        if (dateValue == null || dateValue.trim()
            .isEmpty()) {
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
    private static boolean validateDateTimeFormat(String dateTimeValue) {
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
     * Validates an array that contains objects matching different schemas based on a discriminator field (like "type").
     */
    private static void validateDiscriminatedUnionArray(
        JsonNode arrayValueJsonNode, JsonNode unionDefinitionJsonNode, String propertyPath, StringBuilder errors,
        StringBuilder warnings) {

        for (int i = 0; i < arrayValueJsonNode.size(); i++) {
            JsonNode elementJsonNode = arrayValueJsonNode.get(i);
            String elementPath = propertyPath + "[" + i + "]";

            if (!elementJsonNode.isObject()) {
                String actualType = JsonUtils.getJsonNodeType(elementJsonNode);

                StringUtils.appendWithNewline(
                    errors, ValidationErrorUtils.typeError(elementPath, "object", actualType));

                continue;
            }

            // Find the matching schema based on a discriminator field (typically "type")
            if (elementJsonNode.has("type")) {
                JsonNode typeJsonNode = elementJsonNode.get("type");

                String typeValue = typeJsonNode.asText();

                // Look for a schema that matches this type
                if (unionDefinitionJsonNode.has(typeValue)) {
                    JsonNode schema = unionDefinitionJsonNode.get(typeValue);

                    if (schema.isObject()) {
                        // Create a single-element array for validation
                        ArrayNode singleElementArray = JsonNodeFactory.instance.arrayNode();

                        singleElementArray.add(elementJsonNode);

                        // Validate this element against the matching schema
                        validateObjectArrayElementsWithWarnings(
                            singleElementArray, schema, elementPath.substring(0, elementPath.lastIndexOf('[')), errors,
                            warnings);
                    }
                }
            }
        }
    }

    /**
     * Validates missing object properties that have required fields.
     */
    private static void validateMissingObjectWithRequiredFields(
        JsonNode definitionJsonNode, String propertyPath, StringBuilder errors) {

        Iterator<String> stringIterator = definitionJsonNode.fieldNames();

        stringIterator.forEachRemaining(fieldName -> {
            JsonNode valueJsonNode = definitionJsonNode.get(fieldName);
            String fullFieldPath = PropertyUtils.buildPropertyPath(propertyPath, fieldName);

            String text = valueJsonNode.asText();

            if (valueJsonNode.isTextual() && text.contains(REQUIRED_MARKER)) {
                StringUtils.appendWithNewline(
                    errors, ValidationErrorUtils.missingProperty(fullFieldPath));
            } else if (valueJsonNode.isObject()) {
                validateMissingObjectWithRequiredFields(valueJsonNode, fullFieldPath, errors);
            }
        });
    }

    /**
     * Validates nested object properties and handles type mismatches.
     */
    private static void validateNestedObject(
        JsonNode currentJsonNode, String propertyName, JsonNode definitionValueJsonNode, String propertyPath,
        StringBuilder errors, StringBuilder warnings, String originalTaskDefinition, String originalCurrentParams) {

        if (!currentJsonNode.has(propertyName)) {
            validateMissingObjectWithRequiredFields(definitionValueJsonNode, propertyPath, errors);
            return;
        }

        JsonNode valueJsonNode = currentJsonNode.get(propertyName);

        if (valueJsonNode.isObject()) {
            validatePropertiesRecursively(
                valueJsonNode, definitionValueJsonNode, propertyPath, errors, warnings, originalTaskDefinition,
                originalCurrentParams);
        } else {
            String actualType = JsonUtils.getJsonNodeType(valueJsonNode);

            StringUtils.appendWithNewline(errors, ValidationErrorUtils.typeError(propertyPath, "object", actualType));
        }
    }

    /**
     * Validates nested object properties with array support.
     */
    private static void validateNestedObject(
        JsonNode currentJsonNode, String propertyName, JsonNode definitionValueJsonNode, String propertyPath,
        StringBuilder errors, StringBuilder warnings, String originalTaskDefinition,
        String originalTaskDefinitionForArrays, String originalCurrentParams) {

        if (!currentJsonNode.has(propertyName)) {
            validateMissingObjectWithRequiredFields(definitionValueJsonNode, propertyPath, errors);

            return;
        }

        JsonNode currentValueJsonNode = currentJsonNode.get(propertyName);
        if (currentValueJsonNode.isObject()) {
            validatePropertiesRecursively(
                currentValueJsonNode, definitionValueJsonNode, propertyPath, errors, warnings, originalTaskDefinition,
                originalTaskDefinitionForArrays, originalCurrentParams);
        } else {
            String actualType = JsonUtils.getJsonNodeType(currentValueJsonNode);

            StringUtils.appendWithNewline(errors, ValidationErrorUtils.typeError(propertyPath, "object", actualType));
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
                    errors, ValidationErrorUtils.typeError(elementPath, "object", actualType));

                continue;
            }

            // Validate each required property in the object
            Iterator<String> stringIterator = objectDefinitionJsonNode.fieldNames();

            stringIterator.forEachRemaining(fieldName -> {
                JsonNode fieldDefJsonNode = objectDefinitionJsonNode.get(fieldName);
                String fieldPath = elementPath + "." + fieldName;

                if (fieldDefJsonNode.isTextual()) {
                    String fieldDefText = fieldDefJsonNode.asText();

                    boolean isRequired = fieldDefText.contains("(required)");

                    if (isRequired && !jsonNode.has(fieldName)) {
                        StringUtils.appendWithNewline(errors, "Missing required property: " + fieldPath);
                    } else if (jsonNode.has(fieldName)) {
                        JsonNode actualValue = jsonNode.get(fieldName);
                        String expectedType = org.apache.commons.lang3.StringUtils.trim(
                            fieldDefText.replace("(required)", ""));

                        if (!isTypeValid(actualValue, expectedType)) {
                            String actualType = JsonUtils.getJsonNodeType(actualValue);

                            StringUtils.appendWithNewline(
                                errors, ValidationErrorUtils.typeError(fieldPath, expectedType, actualType));
                        }
                    }
                }
            });
        }
    }

    /**
     * Validates array elements that are objects and generates warnings for undefined properties.
     */
    private static void validateObjectArrayElementsWithWarnings(
        JsonNode arrayValueJsonNode, JsonNode objectDefinitionJsonNode, String propertyPath, StringBuilder errors,
        StringBuilder warnings) {

        // Get the root parameters for display condition evaluation
        JsonNode rootParametersJsonNode = createRootParametersJsonNode(arrayValueJsonNode, propertyPath);

        for (int i = 0; i < arrayValueJsonNode.size(); i++) {
            JsonNode jsonNode = arrayValueJsonNode.get(i);

            String elementPath = propertyPath + "[" + i + "]";

            if (!jsonNode.isObject()) {
                String actualType = JsonUtils.getJsonNodeType(jsonNode);

                StringUtils.appendWithNewline(
                    errors, ValidationErrorUtils.typeError(elementPath, "object", actualType));

                continue;
            }

            // Check for extra properties (warnings) - considering display conditions
            final int currentIndex = i;
            Iterator<String> stringIterator = jsonNode.fieldNames();

            stringIterator.forEachRemaining(fieldName -> {
                JsonNode fieldDefinitionJsonNode = objectDefinitionJsonNode.get(fieldName);

                if (fieldDefinitionJsonNode == null) {
                    // Property is not defined in a schema at all
                    StringUtils.appendWithNewline(
                        warnings,
                        "Property '" + propertyPath + "[index]." + fieldName + "' is not defined in task definition");
                } else if (fieldDefinitionJsonNode.isTextual()) {
                    // Property is defined but check if it should be visible based on a display condition
                    String fieldDefinitionText = fieldDefinitionJsonNode.asText();

                    if (hasDisplayCondition(fieldDefinitionText)) {
                        String condition = extractDisplayCondition(fieldDefinitionText);
                        String resolvedCondition = replaceIndexPlaceholder(condition, currentIndex);

                        boolean shouldShowProperty = WorkflowUtils.extractAndEvaluateCondition(
                            "@" + resolvedCondition + "@", rootParametersJsonNode);

                        if (!shouldShowProperty) {
                            // Property exists but display condition is false - generate warning
                            StringUtils.appendWithNewline(
                                warnings,
                                "Property '" + propertyPath + "[" + currentIndex + "]." + fieldName +
                                    "' is not defined in task definition");
                        }
                    }
                }
            });

            // Validate each property in the object definition
            Iterator<String> stringIterator1 = objectDefinitionJsonNode.fieldNames();

            stringIterator1.forEachRemaining(fieldName -> {
                JsonNode fieldDefinitionJsonNode = objectDefinitionJsonNode.get(fieldName);
                String fieldPath = elementPath + "." + fieldName;

                if (fieldDefinitionJsonNode.isTextual()) {
                    boolean shouldValidateProperty = true;
                    String fieldDefinitionText = fieldDefinitionJsonNode.asText();

                    boolean isRequired = fieldDefinitionText.contains("(required)");

                    // Check display condition
                    if (hasDisplayCondition(fieldDefinitionText)) {
                        String condition = extractDisplayCondition(fieldDefinitionText);

                        String resolvedCondition = replaceIndexPlaceholder(condition, currentIndex);

                        try {
                            shouldValidateProperty = WorkflowUtils.extractAndEvaluateCondition(
                                "@" + resolvedCondition + "@", rootParametersJsonNode);
                        } catch (Exception e) {
                            if (logger.isTraceEnabled()) {
                                logger.trace(e.getMessage());
                            }
                        }
                    }

                    if (shouldValidateProperty) {
                        // Property should be validated based on the display condition
                        if (isRequired && !jsonNode.has(fieldName)) {
                            StringUtils.appendWithNewline(errors, "Missing required property: " + fieldPath);
                        } else if (jsonNode.has(fieldName)) {
                            fieldDefinitionText = fieldDefinitionText.replaceAll("@[^@]*@", "");

                            JsonNode actualValueJsonNode = jsonNode.get(fieldName);
                            String expectedType = org.apache.commons.lang3.StringUtils.trim(
                                fieldDefinitionText.replace("(required)", ""));

                            if (!isTypeValid(actualValueJsonNode, expectedType)) {
                                String actualType = JsonUtils.getJsonNodeType(actualValueJsonNode);

                                StringUtils.appendWithNewline(
                                    errors,
                                    ValidationErrorUtils.typeError(fieldPath, expectedType, actualType));
                            }
                        }
                    }
                }
            });
        }
    }

    /**
     * Validates a property definition and its value, handling required properties and type checking.
     */
    private static void validateStringTypeDefinition(
        JsonNode currentNode, String propertyName, String propertyDefinition, String propertyPath, StringBuilder errors,
        String originalCurrentParams) {

        if (propertyDefinition.contains("@")) {
            try {
                boolean shouldInclude = WorkflowUtils.extractAndEvaluateCondition(
                    propertyDefinition, com.bytechef.commons.util.JsonUtils.readTree(originalCurrentParams));

                if (!shouldInclude) {
                    return; // Skip validation if condition is false
                }
            } catch (Exception e) {
                errors.append(e.getMessage());
            }
        }

        boolean isRequired = propertyDefinition.contains(REQUIRED_MARKER);
        String expectedType = org.apache.commons.lang3.StringUtils.trim(
            propertyDefinition.replace(REQUIRED_MARKER, ""));

        // Remove inline conditions from type definition
        expectedType = org.apache.commons.lang3.StringUtils.trim(expectedType.replaceAll("@[^@]+@", ""));

        if (isRequired && !currentNode.has(propertyName)) {
            StringUtils.appendWithNewline(errors, ValidationErrorUtils.missingProperty(propertyPath));
        } else if (currentNode.has(propertyName)) {
            JsonNode currentValueJsonNode = currentNode.get(propertyName);

            if ("object".equalsIgnoreCase(expectedType) && currentValueJsonNode.isObject()) {
                return;
            }

            // Inline type validation to avoid wrapper function
            // Skip type validation for data pill expressions
            if (currentValueJsonNode.isTextual() && isDataPillExpression(currentValueJsonNode.asText())) {
                // Data pill expressions will be validated separately by validateTaskDataPills
                return;
            }

            // Handle format validation for date/time types
            if (currentValueJsonNode.isTextual() && isDateTimeType(expectedType)) {
                String formatError = validateDateTimeTypeWithError(
                    currentValueJsonNode.asText(), expectedType, propertyPath);

                if (formatError != null) {
                    StringUtils.appendWithNewline(errors, formatError);
                }
            } else if (!isTypeValid(currentValueJsonNode, expectedType)) {
                String actualType = JsonUtils.getJsonNodeType(currentValueJsonNode);

                StringUtils.appendWithNewline(
                    errors, ValidationErrorUtils.typeError(propertyPath, expectedType, actualType));
            }
        }
    }

    /**
     * Validates TIME format (hh:mm:ss).
     */
    private static boolean validateTimeFormat(String timeValue) {
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
        JsonNode arrayValueJsonNode, JsonNode allowedTypesJsonNode, String propertyPath, StringBuilder errors) {

        for (int i = 0; i < arrayValueJsonNode.size(); i++) {
            JsonNode element = arrayValueJsonNode.get(i);
            boolean matchesAnyType = false;

            // Check if an element matches any of the allowed types
            for (int j = 0; j < allowedTypesJsonNode.size(); j++) {
                JsonNode jsonNode = allowedTypesJsonNode.get(j);

                String allowedType = jsonNode.asText();

                if (isTypeValid(element, allowedType)) {
                    matchesAnyType = true;

                    break;
                }
            }

            if (!matchesAnyType) {
                String elementValue = formatElementValue(element);
                String propertyName = PropertyUtils.extractPropertyNameFromPath(propertyPath);
                String actualType = JsonUtils.getJsonNodeType(element);

                // Build expected types string
                StringBuilder expectedTypes = new StringBuilder();

                for (int j = 0; j < allowedTypesJsonNode.size(); j++) {
                    if (j > 0) {
                        expectedTypes.append(" or ");
                    }

                    JsonNode jsonNode = allowedTypesJsonNode.get(j);

                    expectedTypes.append(jsonNode.asText());
                }

                StringUtils.appendWithNewline(
                    errors,
                    ValidationErrorUtils.arrayElementError(
                        elementValue, propertyName, expectedTypes.toString(), actualType));
            }
        }
    }
}
