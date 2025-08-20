package utils;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.regex.Pattern;

/**
 * Utility class for validating individual fields and their properties.
 * Contains validation logic for required fields, field types, and field patterns.
 */
public class FieldValidator {

    private static final Pattern TYPE_PATTERN = Pattern.compile("^[a-zA-Z0-9]+/v[0-9]+(/[a-zA-Z0-9]+)?$");
    private static final String REQUIRED_MARKER = "(required)";

    private FieldValidator() {
        // Utility class
    }

    /**
     * Validates that a required string field exists and is of correct type.
     */
    public static void validateRequiredStringField(JsonNode node, String fieldName, StringBuilder errors) {
        if (!node.has(fieldName)) {
            ValidationErrorBuilder.appendWithNewline(errors, "Missing required field: " + fieldName);
        } else if (!node.get(fieldName).isTextual()) {
            ValidationErrorBuilder.appendWithNewline(errors, "Field '" + fieldName + "' must be a string");
        }
    }

    /**
     * Validates that a required array field exists and is of correct type.
     */
    public static void validateRequiredArrayField(JsonNode node, String fieldName, StringBuilder errors) {
        if (!node.has(fieldName)) {
            ValidationErrorBuilder.appendWithNewline(errors, "Missing required field: " + fieldName);
        } else if (!node.get(fieldName).isArray()) {
            ValidationErrorBuilder.appendWithNewline(errors, "Field '" + fieldName + "' must be an array");
        }
    }

    /**
     * Validates that a required object field exists and is of correct type.
     */
    public static void validateRequiredObjectField(JsonNode node, String fieldName, StringBuilder errors) {
        if (!node.has(fieldName)) {
            ValidationErrorBuilder.appendWithNewline(errors, "Missing required field: " + fieldName);
        } else if (!node.get(fieldName).isObject()) {
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
            } else if (triggers.size() != 1) {
                ValidationErrorBuilder.appendWithNewline(errors, "Field 'triggers' must contain exactly one object");
            } else if (!triggers.get(0).isObject()) {
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
        } else if (!taskNode.get("type").isTextual()) {
            ValidationErrorBuilder.appendWithNewline(errors, "Field 'type' must be a string");
        } else {
            String typeValue = taskNode.get("type").asText();
            if (!TYPE_PATTERN.matcher(typeValue).matches()) {
                ValidationErrorBuilder.appendWithNewline(errors,
                    "Field 'type' must match pattern: (alphanumeric)+/v(numeric)+(/(alphanumeric)+)?");
            }
        }
    }

    /**
     * Validates a property definition and its value, handling required properties and type checking.
     */
    public static void validateStringTypeDefinition(JsonNode currentNode, String propertyName, String propertyDef,
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
        String expectedType = propertyDef.replace(REQUIRED_MARKER, "").trim();
        
        // Remove inline conditions from type definition
        expectedType = expectedType.replaceAll("@[^@]+@", "").trim();

        if (isRequired && !currentNode.has(propertyName)) {
            ValidationErrorBuilder.appendWithNewline(errors, ValidationErrorBuilder.missingProperty(propertyPath));
        } else if (currentNode.has(propertyName)) {
            JsonNode currentValue = currentNode.get(propertyName);

            if ("object".equalsIgnoreCase(expectedType) && currentValue.isObject()) {
                return;
            }

            // Inline type validation to avoid wrapper function
            if (!isTypeValid(currentValue, expectedType)) {
                String actualType = WorkflowParser.getJsonNodeType(currentValue);
                ValidationErrorBuilder.append(errors, ValidationErrorBuilder.typeError(propertyPath, expectedType, actualType));
            }
        }
    }

    /**
     * Validates array property and its elements.
     */
    public static void validateArrayProperty(JsonNode currentNode, String propertyName, JsonNode defValue, String propertyPath, StringBuilder errors) {
        if (!currentNode.has(propertyName)) return;

        JsonNode currentValue = currentNode.get(propertyName);
        if (!currentValue.isArray()) {
            String actualType = WorkflowParser.getJsonNodeType(currentValue);
            ValidationErrorBuilder.append(errors, ValidationErrorBuilder.typeError(propertyPath, "array", actualType));
            return;
        }

        JsonNode arrayElementDef = defValue.get(0);
        if (!arrayElementDef.isTextual()) return;

        String expectedElementType = arrayElementDef.asText();
        for (int i = 0; i < currentValue.size(); i++) {
            JsonNode element = currentValue.get(i);
            if (!isTypeValid(element, expectedElementType)) {
                String elementValue = WorkflowParser.formatElementValue(element);
                String propertyName2 = WorkflowParser.extractPropertyNameFromPath(propertyPath);
                String actualType = WorkflowParser.getJsonNodeType(element);
                ValidationErrorBuilder.append(errors, ValidationErrorBuilder.arrayElementError(
                    elementValue, propertyName2, expectedElementType, actualType));
            }
        }
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
            default -> true;
        };
    }

    /**
     * Validates missing object properties that have required fields.
     */
    public static void validateMissingObjectWithRequiredFields(JsonNode defValue, String propertyPath, StringBuilder errors) {
        defValue.fieldNames().forEachRemaining(fieldName -> {
            JsonNode fieldDef = defValue.get(fieldName);
            String fullFieldPath = WorkflowParser.buildPropertyPath(propertyPath, fieldName);

            if (fieldDef.isTextual() && fieldDef.asText().contains(REQUIRED_MARKER)) {
                ValidationErrorBuilder.appendWithNewline(errors, ValidationErrorBuilder.missingProperty(fullFieldPath));
            } else if (fieldDef.isObject()) {
                validateMissingObjectWithRequiredFields(fieldDef, fullFieldPath, errors);
            }
        });
    }
}
