package utils;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Utility class for orchestrating property-level validation.
 * Handles recursive property validation, extra property detection, and defined property validation.
 */
public class PropertyValidator {

    private PropertyValidator() {
        // Utility class
    }

    /**
     * Recursively validates properties in current parameters against their definition.
     */
    public static void validatePropertiesRecursively(JsonNode currentNode, JsonNode definitionNode, String path,
                                                    StringBuilder errors, StringBuilder warnings,
                                                    String originalTaskDefinition, String originalCurrentParams) {
        // Check for extra properties (generate warnings)
        if (WorkflowParser.isEmptyContainer(definitionNode)) {
            generateWarningsForAllProperties(currentNode, path, warnings);
        } else {
            currentNode.fieldNames().forEachRemaining(fieldName -> {
                if (!definitionNode.has(fieldName)) {
                    String propertyPath = WorkflowParser.buildPropertyPath(path, fieldName);
                    JsonNode currentValue = currentNode.get(fieldName);

                    ValidationErrorBuilder.append(warnings, ValidationErrorBuilder.notDefined(propertyPath));

                    if (currentValue.isObject()) {
                        generateWarningsForAllProperties(currentValue, propertyPath, warnings);
                    }
                }
            });
        }

        // Validate defined properties
        definitionNode.fieldNames().forEachRemaining(propertyName -> {
            JsonNode defValue = definitionNode.get(propertyName);
            String propertyPath = WorkflowParser.buildPropertyPath(path, propertyName);

            if (defValue.isTextual()) {
                handleTextualProperty(currentNode, propertyName, defValue, propertyPath,
                    errors, warnings, originalTaskDefinition, originalCurrentParams);
            } else if (defValue.isObject()) {
                validateNestedObject(currentNode, propertyName, defValue, propertyPath,
                    errors, warnings, originalTaskDefinition, originalCurrentParams);
            } else if (defValue.isArray() && defValue.size() > 0) {
                FieldValidator.validateArrayProperty(currentNode, propertyName, defValue, propertyPath, errors);
            }
        });
    }

    /**
     * Generates warnings for all properties recursively.
     */
    private static void generateWarningsForAllProperties(JsonNode currentNode, String path, StringBuilder warnings) {
        currentNode.fieldNames().forEachRemaining(fieldName -> {
            String propertyPath = WorkflowParser.buildPropertyPath(path, fieldName);
            JsonNode currentValue = currentNode.get(fieldName);

            ValidationErrorBuilder.append(warnings, ValidationErrorBuilder.notDefined(propertyPath));

            if (currentValue.isObject()) {
                generateWarningsForAllProperties(currentValue, propertyPath, warnings);
            }
        });
    }

    /**
     * Validates nested object properties and handles type mismatches.
     */
    private static void validateNestedObject(JsonNode currentNode, String propertyName, JsonNode defValue,
                                           String propertyPath, StringBuilder errors, StringBuilder warnings,
                                           String originalTaskDefinition, String originalCurrentParams) {
        if (!currentNode.has(propertyName)) {
            FieldValidator.validateMissingObjectWithRequiredFields(defValue, propertyPath, errors);
            return;
        }

        JsonNode currentValue = currentNode.get(propertyName);
        if (currentValue.isObject()) {
            validatePropertiesRecursively(currentValue, defValue, propertyPath,
                errors, warnings, originalTaskDefinition, originalCurrentParams);
        } else {
            String actualType = WorkflowParser.getJsonNodeType(currentValue);
            ValidationErrorBuilder.appendWithNewline(errors, ValidationErrorBuilder.typeError(propertyPath, "object", actualType));
        }
    }

    /**
     * Handles validation for textual property definitions (like "string (required)").
     */
    private static void handleTextualProperty(JsonNode currentNode, String propertyName, JsonNode defValue,
                                            String propertyPath, StringBuilder errors, StringBuilder warnings,
                                            String originalTaskDefinition, String originalCurrentParams) {
        String defText = defValue.asText().replace("(required)", "").trim();

        if (("object".equalsIgnoreCase(defText) || "array".equalsIgnoreCase(defText)) && currentNode.has(propertyName)) {
            JsonNode currentValue = currentNode.get(propertyName);
            boolean correctType = ("object".equalsIgnoreCase(defText) && currentValue.isObject()) ||
                                ("array".equalsIgnoreCase(defText) && currentValue.isArray());

            if (correctType && "object".equalsIgnoreCase(defText)) {
                // Generate warnings for all nested properties since they're not defined
                currentValue.fieldNames().forEachRemaining(fieldName -> {
                    String fieldPath = WorkflowParser.buildPropertyPath(propertyPath, fieldName);
                    ValidationErrorBuilder.append(warnings, ValidationErrorBuilder.notDefined(fieldPath));
                });
                return;
            }
        }

        FieldValidator.validateStringTypeDefinition(currentNode, propertyName, defValue.asText(),
            propertyPath, errors, warnings, originalTaskDefinition, originalCurrentParams);
    }
}
