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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
        // Check for inline display conditions
        if (propertyDef.contains("@") && propertyDef.contains("@")) {
            try {
                boolean shouldInclude = WorkflowParser.extractAndEvaluateCondition(propertyDef,
                    WorkflowParser.parseJsonString(originalCurrentParams));
                if (!shouldInclude) {
                    return; // Skip validation if condition is false
                }
            } catch (Exception e) {
                // If condition evaluation fails, continue with validation
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

            if (!isTypeValid(currentValue, expectedType)) {
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
                        String fieldPath = elementPath + "." + fieldName;
                        ValidationErrorBuilder.append(warnings, "Property '" + propertyPath + "[index]." + fieldName
                            + "' is not defined in task definition");
                    } else if (fieldDef.isTextual()) {
                        // Property is defined but check if it should be visible based on display condition
                        String fieldDefText = fieldDef.asText();
                        if (hasDisplayCondition(fieldDefText)) {
                            String condition = extractDisplayCondition(fieldDefText);
                            String resolvedCondition = replaceIndexPlaceholder(condition, currentIndex);

                            try {
                                boolean shouldShowProperty = WorkflowParser
                                    .extractAndEvaluateCondition("@" + resolvedCondition + "@", rootParameters);
                                if (!shouldShowProperty) {
                                    // Property exists but display condition is false - generate warning
                                    ValidationErrorBuilder.append(warnings, "Property '" + propertyPath + "["
                                        + currentIndex + "]." + fieldName + "' is not defined in task definition");
                                }
                            } catch (Exception e) {
                                // If condition evaluation fails, assume property should be shown
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
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("@([^@]+)@");
        java.util.regex.Matcher matcher = pattern.matcher(fieldDefText);
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
        return switch (expectedType.toLowerCase()) {
            case "string" -> node.isTextual();
            case "float" -> node.isFloatingPointNumber();
            case "integer" -> node.isIntegralNumber();
            case "number" -> node.isNumber();
            case "boolean" -> node.isBoolean();
            case "array" -> node.isArray();
            case "object" -> node.isObject();
            case "null" -> node.isNull();
            default -> true;
        };
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
