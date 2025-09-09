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

/**
 * Utility class for orchestrating property-level validation. Handles recursive property validation, extra property
 * detection, and defined property validation.
 */
public class PropertyValidator {

    private PropertyValidator() {
        // Utility class
    }

    /**
     * Recursively validates properties in current parameters against their definition.
     */
    public static void validatePropertiesRecursively(
        JsonNode currentNode, JsonNode definitionNode, String path,
        StringBuilder errors, StringBuilder warnings,
        String originalTaskDefinition, String originalCurrentParams) {
        // Check for extra properties (generate warnings)
        if (WorkflowParser.isEmptyContainer(definitionNode)) {
            generateWarningsForAllProperties(currentNode, path, warnings);
        } else {
            currentNode.fieldNames()
                .forEachRemaining(fieldName -> {
                    if (!definitionNode.has(fieldName)) {
                        String propertyPath = PropertyUtils.buildPropertyPath(path, fieldName);
                        JsonNode currentValue = currentNode.get(fieldName);

                        ValidationErrorBuilder.append(warnings, ValidationErrorBuilder.notDefined(propertyPath));

                        if (currentValue.isObject()) {
                            generateWarningsForAllProperties(currentValue, propertyPath, warnings);
                        }
                    }
                });
        }

        // Validate defined properties
        definitionNode.fieldNames()
            .forEachRemaining(propertyName -> {
                JsonNode defValue = definitionNode.get(propertyName);
                String propertyPath = PropertyUtils.buildPropertyPath(path, propertyName);

                if (defValue.isTextual()) {
                    handleTextualProperty(currentNode, propertyName, defValue, propertyPath,
                        errors, warnings, originalTaskDefinition, originalCurrentParams);
                } else if (defValue.isObject()) {
                    validateNestedObject(currentNode, propertyName, defValue, propertyPath,
                        errors, warnings, originalTaskDefinition, originalCurrentParams);
                } else if (defValue.isArray() && defValue.size() > 0) {
                    validateArrayProperty(currentNode, propertyName, defValue, propertyPath, errors, warnings);
                }
            });
    }

    /**
     * Validates array property with warnings support.
     */
    private static void validateArrayProperty(
        JsonNode currentNode, String propertyName, JsonNode defValue, String propertyPath,
        StringBuilder errors, StringBuilder warnings) {
        if (!currentNode.has(propertyName)) {
            return;
        }

        JsonNode currentValue = currentNode.get(propertyName);
        if (!currentValue.isArray()) {
            String actualType = JsonUtils.getJsonNodeType(currentValue);
            ValidationErrorBuilder.append(errors, ValidationErrorBuilder.typeError(propertyPath, "array", actualType));
            return;
        }

        // Check if this is a union type or object array
        JsonNode firstElement = defValue.get(0);
        boolean isUnionType = false;

        if (firstElement.isTextual()) {
            // Check if all elements are simple text types (union)
            isUnionType = true;
            for (int i = 0; i < defValue.size(); i++) {
                if (!defValue.get(i).isTextual()) {
                    isUnionType = false;
                    break;
                }
            }
        }

        if (isUnionType) {
            // Array of simple types - use existing validation without warnings
            FieldValidator.validateArrayProperty(currentNode, propertyName, defValue, propertyPath, errors);
        } else {
            // Array of objects - use new validation with warnings
            JsonNode arrayElementDef = defValue.get(0);
            FieldValidator.validateObjectArrayElementsWithWarnings(currentValue, arrayElementDef, propertyPath, errors, warnings);
        }
    }

    /**
     * Generates warnings for all properties recursively.
     */
    private static void generateWarningsForAllProperties(JsonNode currentNode, String path, StringBuilder warnings) {
        currentNode.fieldNames()
            .forEachRemaining(fieldName -> {
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
    private static void validateNestedObject(
        JsonNode currentNode, String propertyName, JsonNode defValue,
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
            String actualType = JsonUtils.getJsonNodeType(currentValue);
            ValidationErrorBuilder.appendWithNewline(errors,
                ValidationErrorBuilder.typeError(propertyPath, "object", actualType));
        }
    }

    /**
     * Handles validation for textual property definitions (like "string (required)").
     */
    private static void handleTextualProperty(
        JsonNode currentNode, String propertyName, JsonNode defValue,
        String propertyPath, StringBuilder errors, StringBuilder warnings,
        String originalTaskDefinition, String originalCurrentParams) {
        String defText = defValue.asText()
            .replace("(required)", "")
            .trim();

        if (("object".equalsIgnoreCase(defText) || "array".equalsIgnoreCase(defText))
            && currentNode.has(propertyName)) {
            JsonNode currentValue = currentNode.get(propertyName);
            boolean correctType = ("object".equalsIgnoreCase(defText) && currentValue.isObject()) ||
                ("array".equalsIgnoreCase(defText) && currentValue.isArray());

            if (correctType && "object".equalsIgnoreCase(defText)) {
                // Generate warnings for all nested properties since they're not defined
                currentValue.fieldNames()
                    .forEachRemaining(fieldName -> {
                        String fieldPath = WorkflowParser.buildPropertyPath(propertyPath, fieldName);
                        ValidationErrorBuilder.append(warnings, ValidationErrorBuilder.notDefined(fieldPath));
                    });
                return;
            }
        }

        FieldValidator.validateStringTypeDefinition(currentNode, propertyName, defValue.asText(),
            propertyPath, errors, warnings, originalTaskDefinition, originalCurrentParams);
    }

    /**
     * Recursively validates properties with array support for display conditions.
     */
    public static void validatePropertiesRecursively(
        JsonNode currentNode, JsonNode definitionNode, String path,
        StringBuilder errors, StringBuilder warnings,
        String originalTaskDefinition, String originalTaskDefinitionForArrays, String originalCurrentParams) {
        // Check for extra properties (generate warnings)
        if (WorkflowParser.isEmptyContainer(definitionNode)) {
            generateWarningsForAllProperties(currentNode, path, warnings);
        } else {
            currentNode.fieldNames()
                .forEachRemaining(fieldName -> {
                    if (!definitionNode.has(fieldName)) {
                        String propertyPath = PropertyUtils.buildPropertyPath(path, fieldName);
                        JsonNode currentValue = currentNode.get(fieldName);

                        ValidationErrorBuilder.append(warnings, ValidationErrorBuilder.notDefined(propertyPath));

                        if (currentValue.isObject()) {
                            generateWarningsForAllProperties(currentValue, propertyPath, warnings);
                        }
                    }
                });
        }

        // Validate defined properties
        definitionNode.fieldNames()
            .forEachRemaining(propertyName -> {
                JsonNode defValue = definitionNode.get(propertyName);
                String propertyPath = PropertyUtils.buildPropertyPath(path, propertyName);

                if (defValue.isTextual()) {
                    handleTextualProperty(currentNode, propertyName, defValue, propertyPath,
                        errors, warnings, originalTaskDefinition, originalCurrentParams);
                } else if (defValue.isObject()) {
                    validateNestedObject(currentNode, propertyName, defValue, propertyPath,
                        errors, warnings, originalTaskDefinition, originalTaskDefinitionForArrays, originalCurrentParams);
                } else if (defValue.isArray() && defValue.size() > 0) {
                    validateArrayPropertyWithArraySupport(currentNode, propertyName, defValue, propertyPath, errors, warnings, originalTaskDefinitionForArrays, originalCurrentParams);
                }
            });
    }

    /**
     * Validates nested object properties with array support.
     */
    private static void validateNestedObject(
        JsonNode currentNode, String propertyName, JsonNode defValue,
        String propertyPath, StringBuilder errors, StringBuilder warnings,
        String originalTaskDefinition, String originalTaskDefinitionForArrays, String originalCurrentParams) {
        if (!currentNode.has(propertyName)) {
            FieldValidator.validateMissingObjectWithRequiredFields(defValue, propertyPath, errors);
            return;
        }

        JsonNode currentValue = currentNode.get(propertyName);
        if (currentValue.isObject()) {
            validatePropertiesRecursively(currentValue, defValue, propertyPath,
                errors, warnings, originalTaskDefinition, originalTaskDefinitionForArrays, originalCurrentParams);
        } else {
            String actualType = JsonUtils.getJsonNodeType(currentValue);
            ValidationErrorBuilder.appendWithNewline(errors,
                ValidationErrorBuilder.typeError(propertyPath, "object", actualType));
        }
    }

    /**
     * Validates array property with array support for display conditions.
     */
    private static void validateArrayPropertyWithArraySupport(
        JsonNode currentNode, String propertyName, JsonNode defValue, String propertyPath,
        StringBuilder errors, StringBuilder warnings, String originalTaskDefinitionForArrays, String originalCurrentParams) {
        if (!currentNode.has(propertyName)) {
            return;
        }

        JsonNode currentValue = currentNode.get(propertyName);
        if (!currentValue.isArray()) {
            String actualType = JsonUtils.getJsonNodeType(currentValue);
            ValidationErrorBuilder.append(errors, ValidationErrorBuilder.typeError(propertyPath, "array", actualType));
            return;
        }

        // Check if this is a union type (array of simple types) or object array
        boolean isUnionType = false;

        if (defValue.size() > 0) {
            JsonNode firstElement = defValue.get(0);

            if (firstElement.isTextual()) {
                // Check if all elements are simple text types (union type)
                isUnionType = true;
                for (int i = 0; i < defValue.size(); i++) {
                    if (!defValue.get(i).isTextual()) {
                        isUnionType = false;
                        break;
                    }
                }
            }
        }

        if (isUnionType) {
            // Check if this is a TASK type array first
            if (defValue.size() == 1 && defValue.get(0).isTextual() &&
                "task".equalsIgnoreCase(defValue.get(0).asText())) {
                // Handle TASK type arrays specially
                validateTaskArray(currentValue, propertyPath, errors, warnings);
                return;
            }
            // Array of simple types - use existing validation without warnings
            FieldValidator.validateArrayProperty(currentNode, propertyName, defValue, propertyPath, errors);
        } else if (defValue.size() > 0) {
            JsonNode arrayElementDef = defValue.get(0);

            // Check if this is a TASK type array
            if (arrayElementDef.isTextual() && "task".equalsIgnoreCase(arrayElementDef.asText())) {
                // Handle TASK type arrays specially
                validateTaskArray(currentValue, propertyPath, errors, warnings);
                return;
            }

            // Check if this is an array of arrays (nested arrays)
            if (arrayElementDef.isArray()) {
                // Handle array of arrays - validate each sub-array
                for (int i = 0; i < currentValue.size(); i++) {
                    JsonNode subArray = currentValue.get(i);
                    String subArrayPath = propertyPath + "[" + i + "]";

                    if (!subArray.isArray()) {
                        String actualType = JsonUtils.getJsonNodeType(subArray);
                        ValidationErrorBuilder.append(errors, ValidationErrorBuilder.typeError(subArrayPath, "array", actualType));
                        continue;
                    }

                    // Recursively validate the sub-array using the array element definition
                    try {
                        JsonNode originalTaskDefNode = WorkflowParser.parseJsonString(originalTaskDefinitionForArrays);
                        JsonNode originalParametersNode = originalTaskDefNode.get("parameters");
                        JsonNode originalArrayDef = WorkflowParser.getNestedField(originalParametersNode, propertyName);
                        if (originalArrayDef != null && originalArrayDef.isArray() && originalArrayDef.size() > 0) {
                            JsonNode originalSubArrayElementDef = originalArrayDef.get(0);
                            validateDiscriminatedUnionArray(subArray, originalSubArrayElementDef, subArrayPath, errors, warnings);
                        } else {
                            // Fallback to processed definition
                            validateDiscriminatedUnionArray(subArray, arrayElementDef, subArrayPath, errors, warnings);
                        }
                    } catch (Exception e) {
                        // Fallback to processed definition
                        validateDiscriminatedUnionArray(subArray, arrayElementDef, subArrayPath, errors, warnings);
                    }
                }
            } else {
                // Array of objects - use original task definition to get array element definition with display conditions
                try {
                    JsonNode originalTaskDefNode = WorkflowParser.parseJsonString(originalTaskDefinitionForArrays);
                    JsonNode originalParametersNode = originalTaskDefNode.get("parameters");
                    JsonNode originalArrayDef = WorkflowParser.getNestedField(originalParametersNode, propertyName);
                    if (originalArrayDef != null && originalArrayDef.isArray() && originalArrayDef.size() > 0) {
                        JsonNode originalArrayElementDef = originalArrayDef.get(0);
                        FieldValidator.validateObjectArrayElementsWithWarnings(currentValue, originalArrayElementDef, propertyPath, errors, warnings);
                    } else {
                        // Fallback to processed definition
                        FieldValidator.validateObjectArrayElementsWithWarnings(currentValue, arrayElementDef, propertyPath, errors, warnings);
                    }
                } catch (Exception e) {
                    // Fallback to processed definition
                    FieldValidator.validateObjectArrayElementsWithWarnings(currentValue, arrayElementDef, propertyPath, errors, warnings);
                }
            }
        }
    }

    /**
     * Validates an array containing TASK objects.
     */
    public static void validateTaskArray(JsonNode arrayValue, String propertyPath, StringBuilder errors, StringBuilder warnings) {
        for (int i = 0; i < arrayValue.size(); i++) {
            JsonNode taskElement = arrayValue.get(i);
            String elementPath = propertyPath + "[" + i + "]";

            if (!taskElement.isObject()) {
                String actualType = JsonUtils.getJsonNodeType(taskElement);
                ValidationErrorBuilder.append(errors, ValidationErrorBuilder.typeError(elementPath, "object", actualType));
                continue;
            }

            // Validate task structure using WorkflowValidator.validateTaskStructure
            WorkflowValidator.validateTaskStructure(taskElement.toString(), errors);

            // If task has parameters, validate them recursively if we have the task type
            if (taskElement.has("parameters") && taskElement.has("type")) {
                JsonNode parameters = taskElement.get("parameters");
                String taskType = taskElement.get("type").asText();

                // For now, we'll skip detailed parameter validation since we don't have task definitions
                // This could be enhanced in the future to lookup task definitions and validate parameters

                // Basic parameter structure validation
                if (!parameters.isObject()) {
                    String actualType = JsonUtils.getJsonNodeType(parameters);
                    ValidationErrorBuilder.append(errors, ValidationErrorBuilder.typeError(elementPath + ".parameters", "object", actualType));
                }
            }
        }
    }

    /**
     * Validates an array that contains objects matching different schemas based on a discriminator field (like "type").
     */
    private static void validateDiscriminatedUnionArray(JsonNode arrayValue, JsonNode unionDef, String propertyPath, StringBuilder errors, StringBuilder warnings) {
        for (int i = 0; i < arrayValue.size(); i++) {
            JsonNode element = arrayValue.get(i);
            String elementPath = propertyPath + "[" + i + "]";

            if (!element.isObject()) {
                String actualType = JsonUtils.getJsonNodeType(element);
                ValidationErrorBuilder.append(errors, ValidationErrorBuilder.typeError(elementPath, "object", actualType));
                continue;
            }

            // Find the matching schema based on discriminator field (typically "type")
            boolean foundMatch = false;
            if (element.has("type")) {
                String typeValue = element.get("type").asText();

                // Look for a schema that matches this type
                if (unionDef.has(typeValue)) {
                    JsonNode schema = unionDef.get(typeValue);
                    if (schema.isObject()) {
                        // Create a single-element array for validation
                        com.fasterxml.jackson.databind.node.ArrayNode singleElementArray =
                            com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.arrayNode();
                        singleElementArray.add(element);

                        // Validate this element against the matching schema
                        FieldValidator.validateObjectArrayElementsWithWarnings(singleElementArray, schema, elementPath.substring(0, elementPath.lastIndexOf('[')), errors, warnings);
                        foundMatch = true;
                    }
                }
            }

            if (!foundMatch) {
                // No matching schema found, but since this is a discriminated union with non-required elements,
                // we'll be lenient and not generate warnings. This allows for flexible validation of union types.
                // ValidationErrorBuilder.append(warnings, "Property '" + elementPath + "' could not be matched to any schema in the union type");
            }
        }
    }
}
