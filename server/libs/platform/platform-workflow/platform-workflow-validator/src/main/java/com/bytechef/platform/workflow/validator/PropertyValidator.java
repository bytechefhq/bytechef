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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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

    private static final String REQUIRED_MARKER = "(required)";

    private PropertyValidator() {
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
                    taskParametersJsonNode, fieldName, curDefinitionJsonNode, propertyPath, originalCurrentParameters,
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
                        taskParametersJsonNode, fieldName, curDefinitionJsonNode, propertyPath,
                        originalTaskDefinitionForArrays, errors, warnings);
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
        String originalCurrentParameters, StringBuilder errors, StringBuilder warnings) {

        String text = definitionJsonNode.asText();

        String definitionText = org.apache.commons.lang3.StringUtils.trim(text.replace("(required)", ""));

        if (("object".equalsIgnoreCase(definitionText) || "array".equalsIgnoreCase(definitionText)) &&
            taskParametersJsonNode.has(fieldName)) {

            JsonNode valueJsonNode = taskParametersJsonNode.get(fieldName);

            boolean correctType = ("object".equalsIgnoreCase(definitionText) && valueJsonNode.isObject()) ||
                ("array".equalsIgnoreCase(definitionText) && valueJsonNode.isArray());

            if (correctType && "object".equalsIgnoreCase(definitionText)) {
                // Generate warnings for all nested properties since they're not defined
                Iterator<String> fieldNamesIterator = valueJsonNode.fieldNames();

                fieldNamesIterator.forEachRemaining(curFieldName -> {
                    String fieldPath = buildPropertyPath(propertyPath, curFieldName);

                    StringUtils.appendWithNewline(ValidationErrorUtils.notDefined(fieldPath), warnings);
                });

                return;
            }
        }

        validateStringTypeDefinition(taskParametersJsonNode, fieldName, text, propertyPath, originalCurrentParameters,
            errors);
    }

    /**
     * Checks if a field definition has a display condition.
     */
    private static boolean hasDisplayCondition(String fieldDefinitionText) {
        return fieldDefinitionText.contains("@") && fieldDefinitionText.matches(".*@[^@]+@.*");
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
            } else {
                // Array of objects - use new validation with warnings
                JsonNode arrayElementDefinitionJsonNode = definitionJsonNode.get(0);

                validateObjectArrayElementsWithWarnings(
                    valueJsonNode, arrayElementDefinitionJsonNode, propertyPath, errors, warnings);
            }
        }
    }

    /**
     * Validates array property with array support for display conditions.
     */
    private static void validateArrayPropertyWithArraySupport(
        JsonNode taskParametersJsonNode, String fieldName, JsonNode definitionJsonNode, String propertyPath,
        String originalTaskDefinitionForArrays, StringBuilder errors, StringBuilder warnings) {

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

                        continue;
                    }

                    // Recursively validate the sub-array using the array element definition
                    try {
                        JsonNode originalTaskDefinitionJsonNode = com.bytechef.commons.util.JsonUtils.readTree(
                            originalTaskDefinitionForArrays);
                        JsonNode originalParametersJsonNode = originalTaskDefinitionJsonNode.get("parameters");
                        JsonNode originalArrayDefinitionJsonNode = WorkflowUtils.getNestedField(
                            originalParametersJsonNode, fieldName);

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
                    JsonNode originalArrayDefinition = WorkflowUtils.getNestedField(
                        originalParametersNode, fieldName);

                    if (originalArrayDefinition != null && originalArrayDefinition.isArray() &&
                        !originalArrayDefinition.isEmpty()) {

                        JsonNode originalArrayElementDefinition = originalArrayDefinition.get(0);

                        validateObjectArrayElementsWithWarnings(
                            valueJsonNode, originalArrayElementDefinition, propertyPath, errors, warnings);
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
                    ValidationErrorUtils.typeError(elementPath, "object", actualType), errors);

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
        JsonNode definitionJsonNode, String fieldName, StringBuilder errors) {

        Iterator<String> fieldNamesIterator = definitionJsonNode.fieldNames();

        fieldNamesIterator.forEachRemaining(curFieldName -> {
            JsonNode curDefinitionJsonNode = definitionJsonNode.get(curFieldName);
            String fullFieldPath = PropertyUtils.buildPropertyPath(fieldName, curFieldName);

            String text = curDefinitionJsonNode.asText();

            if (curDefinitionJsonNode.isTextual() && text.contains(REQUIRED_MARKER)) {
                StringUtils.appendWithNewline(
                    ValidationErrorUtils.missingProperty(fullFieldPath), errors);
            } else if (curDefinitionJsonNode.isObject()) {
                validateMissingObjectWithRequiredFields(curDefinitionJsonNode, fullFieldPath, errors);
            }
        });
    }

    /**
     * Validates nested object properties with array support.
     */
    private static void validateNestedObject(
        JsonNode taskParametersJsonNode, String fieldName, JsonNode definitionJsonNode, String propertyPath,
        String originalTaskDefinition, @Nullable String originalTaskDefinitionForArrays,
        String originalCurrentParameters, StringBuilder errors, StringBuilder warnings) {

        if (!taskParametersJsonNode.has(fieldName)) {
            validateMissingObjectWithRequiredFields(definitionJsonNode, propertyPath, errors);

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
        } else {
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

                continue;
            }

            // Validate each required property in the object
            Iterator<String> fieldNamesIterator = objectDefinitionJsonNode.fieldNames();

            fieldNamesIterator.forEachRemaining(fieldName -> {
                JsonNode fieldDefinitionJsonNode = objectDefinitionJsonNode.get(fieldName);
                String fieldPath = elementPath + "." + fieldName;

                if (fieldDefinitionJsonNode.isTextual()) {
                    String fieldDefText = fieldDefinitionJsonNode.asText();

                    boolean isRequired = fieldDefText.contains("(required)");

                    if (isRequired && !jsonNode.has(fieldName)) {
                        StringUtils.appendWithNewline("Missing required property: " + fieldPath, errors);
                    } else if (jsonNode.has(fieldName)) {
                        JsonNode valueJsonNode = jsonNode.get(fieldName);
                        String expectedType = org.apache.commons.lang3.StringUtils.trim(
                            fieldDefText.replace("(required)", ""));

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
                    ValidationErrorUtils.typeError(elementPath, "object", actualType), errors);

                continue;
            }

            // Check for extra properties (warnings) - considering display conditions
            final int currentIndex = i;
            Iterator<String> fieldNamesIterator = jsonNode.fieldNames();

            fieldNamesIterator.forEachRemaining(fieldName -> {
                JsonNode fieldDefinitionJsonNode = objectDefinitionJsonNode.get(fieldName);

                if (fieldDefinitionJsonNode == null) {
                    // Property is not defined in a schema at all
                    StringUtils.appendWithNewline(
                        "Property '" + propertyPath + "[index]." + fieldName + "' is not defined in task definition",
                        warnings);
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
                                "Property '" + propertyPath + "[" + currentIndex + "]." + fieldName +
                                    "' is not defined in task definition",
                                warnings);
                        }
                    }
                }
            });

            // Validate each property in the object definition
            fieldNamesIterator = objectDefinitionJsonNode.fieldNames();

            fieldNamesIterator.forEachRemaining(fieldName -> {
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
                            StringUtils.appendWithNewline("Missing required property: " + fieldPath, errors);
                        } else if (jsonNode.has(fieldName)) {
                            fieldDefinitionText = fieldDefinitionText.replaceAll("@[^@]*@", "");

                            JsonNode valueJsonNode = jsonNode.get(fieldName);
                            String expectedType = org.apache.commons.lang3.StringUtils.trim(
                                fieldDefinitionText.replace("(required)", ""));

                            if (!isTypeValid(valueJsonNode, expectedType)) {
                                String actualType = JsonUtils.getJsonNodeType(valueJsonNode);

                                StringUtils.appendWithNewline(
                                    ValidationErrorUtils.typeError(fieldPath, expectedType, actualType), errors);
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
        JsonNode taskParametersJsonNode, String fieldName, String propertyDefinition, String propertyPath,
        String originalCurrentParameters, StringBuilder errors) {

        if (propertyDefinition.contains("@")) {
            try {
                boolean shouldInclude = WorkflowUtils.extractAndEvaluateCondition(
                    propertyDefinition, com.bytechef.commons.util.JsonUtils.readTree(originalCurrentParameters));

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

        if (isRequired && !taskParametersJsonNode.has(fieldName)) {
            StringUtils.appendWithNewline(ValidationErrorUtils.missingProperty(propertyPath), errors);
        } else if (taskParametersJsonNode.has(fieldName)) {
            JsonNode valueJsonNode = taskParametersJsonNode.get(fieldName);

            if ("object".equalsIgnoreCase(expectedType) && valueJsonNode.isObject()) {
                return;
            }

            // Inline type validation to avoid wrapper function
            // Skip type validation for data pill expressions
            if (valueJsonNode.isTextual() && isDataPillExpression(valueJsonNode.asText())) {
                // Data pill expressions will be validated separately by validateTaskDataPills
                return;
            }

            // Handle format validation for date/time types
            if (valueJsonNode.isTextual() && isDateTimeType(expectedType)) {
                String formatError = validateDateTimeTypeWithError(
                    valueJsonNode.asText(), expectedType, propertyPath);

                if (formatError != null) {
                    StringUtils.appendWithNewline(formatError, errors);
                }
            } else if (!isTypeValid(valueJsonNode, expectedType)) {
                String actualType = JsonUtils.getJsonNodeType(valueJsonNode);

                StringUtils.appendWithNewline(
                    ValidationErrorUtils.typeError(propertyPath, expectedType, actualType), errors);
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
}
