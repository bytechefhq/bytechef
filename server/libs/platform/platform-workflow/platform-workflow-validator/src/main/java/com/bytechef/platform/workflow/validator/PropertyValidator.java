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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;

/**
 * Utility class for orchestrating property-level validation. Handles recursive property validation, extra property
 * detection, and defined property validation.
 *
 * @author Marko Kriskovic
 */
class PropertyValidator {

    private PropertyValidator() {
    }

    /**
     * Recursively validates properties in current parameters against their definition using the PropertyInfo list.
     */
    public static void validatePropertiesFromPropertyInfo(
        JsonNode taskParametersJsonNode, List<PropertyInfo> propertyInfos, String path,
        String originalCurrentParameters, StringBuilder errors, StringBuilder warnings) {

        if (propertyInfos.isEmpty()) {
            generateWarningsForAllProperties(taskParametersJsonNode, path, warnings);

            return;
        }

        Set<String> validatedProperties = new HashSet<>();

        for (PropertyInfo propertyInfo : propertyInfos) {
            ValidationResult result = validatePropertyWithDisplayCondition(
                taskParametersJsonNode, propertyInfo, path, originalCurrentParameters, errors, warnings);

            if (result.wasProcessed()) {
                validatedProperties.add(propertyInfo.name());
            }
        }

        checkForUndefinedProperties(taskParametersJsonNode, validatedProperties, path, warnings);
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

        if (isEmptyContainer(definitionJsonNode)) {
            generateWarningsForAllProperties(taskParametersJsonNode, path, warnings);
        } else {
            checkExtraProperties(taskParametersJsonNode, definitionJsonNode, path, warnings);
        }

        validateDefinedProperties(
            taskParametersJsonNode, definitionJsonNode, path, originalTaskDefinition,
            originalTaskDefinitionForArrays, originalCurrentParameters, errors, warnings);
    }

    private static ValidationResult validatePropertyWithDisplayCondition(
        JsonNode taskParametersJsonNode, PropertyInfo propertyInfo, String path,
        String originalCurrentParameters, StringBuilder errors, StringBuilder warnings) {

        String fieldName = propertyInfo.name();

        String propertyPath = PropertyUtils.buildPropertyPath(path, fieldName);

        DisplayConditionEvaluator.DisplayConditionResult conditionResult =
            DisplayConditionEvaluator.evaluate(
                propertyInfo.displayCondition(),
                com.bytechef.commons.util.JsonUtils.readTree(originalCurrentParameters));

        if (conditionResult.isMalformed()) {
            handleMalformedDisplayCondition(
                taskParametersJsonNode, fieldName, propertyPath, conditionResult.getMalformedMessage(), warnings);

            return ValidationResult.processed();
        }

        if (conditionResult.shouldShow()) {
            validatePropertyFromPropertyInfo(
                taskParametersJsonNode, propertyInfo, propertyPath, originalCurrentParameters, errors, warnings);

            return ValidationResult.processed();
        }

        return ValidationResult.skipped();
    }

    private static void handleMalformedDisplayCondition(
        JsonNode taskParametersJsonNode, String fieldName, String propertyPath,
        String malformedMessage, StringBuilder warnings) {

        if (taskParametersJsonNode.has(fieldName)) {
            StringUtils.appendWithNewline(ValidationErrorUtils.notDefined(propertyPath), warnings);

            JsonNode valueJsonNode = taskParametersJsonNode.get(fieldName);

            if (valueJsonNode.isObject()) {
                generateWarningsForAllProperties(valueJsonNode, propertyPath, warnings);
            }
        }

        StringUtils.appendWithNewline(malformedMessage, warnings);
    }

    private static void checkForUndefinedProperties(
        JsonNode taskParametersJsonNode, Set<String> validatedProperties, String path, StringBuilder warnings) {

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

    private static void checkExtraProperties(
        JsonNode taskParametersJsonNode, JsonNode definitionJsonNode, String path, StringBuilder warnings) {

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

    private static void validateDefinedProperties(
        JsonNode taskParametersJsonNode, JsonNode definitionJsonNode, String path, String originalTaskDefinition,
        @Nullable String originalTaskDefinitionForArrays, String originalCurrentParameters, StringBuilder errors,
        StringBuilder warnings) {

        Iterator<String> fieldNamesIterator = definitionJsonNode.fieldNames();

        fieldNamesIterator.forEachRemaining(fieldName -> {
            JsonNode curDefinitionJsonNode = definitionJsonNode.get(fieldName);
            String propertyPath = PropertyUtils.buildPropertyPath(path, fieldName);

            if (curDefinitionJsonNode.isTextual()) {
                handleTextualProperty(
                    taskParametersJsonNode, fieldName, curDefinitionJsonNode, propertyPath, errors, warnings);
            } else if (curDefinitionJsonNode.isObject()) {
                validateNestedObject(
                    taskParametersJsonNode, fieldName, curDefinitionJsonNode, propertyPath, originalTaskDefinition,
                    originalTaskDefinitionForArrays, originalCurrentParameters, errors, warnings);
            } else if (curDefinitionJsonNode.isArray() && !curDefinitionJsonNode.isEmpty()) {
                validateArrayProperty(
                    taskParametersJsonNode, fieldName, curDefinitionJsonNode, propertyPath,
                    originalTaskDefinitionForArrays, errors);
            }
        });
    }

    private static void handleTextualProperty(
        JsonNode taskParametersJsonNode, String fieldName, JsonNode definitionJsonNode, String propertyPath,
        StringBuilder errors, StringBuilder warnings) {

        String type = definitionJsonNode.asText();

        if (taskParametersJsonNode.has(fieldName)) {
            JsonNode valueJsonNode = taskParametersJsonNode.get(fieldName);

            if (isObjectOrArrayType(type, valueJsonNode)) {
                if ("object".equalsIgnoreCase(type) && valueJsonNode.isObject()) {
                    generateWarningsForUndefinedNestedProperties(valueJsonNode, propertyPath, warnings);
                }

                return;
            }
        }

        validateStringTypeDefinition(taskParametersJsonNode, fieldName, type, propertyPath, errors);
    }

    private static boolean isObjectOrArrayType(String type, JsonNode valueJsonNode) {
        boolean isObjectType = "object".equalsIgnoreCase(type);
        boolean isArrayType = "array".equalsIgnoreCase(type);

        if (isObjectType || isArrayType) {
            return (isObjectType && valueJsonNode.isObject()) || (isArrayType && valueJsonNode.isArray());
        }

        return false;
    }

    private static void generateWarningsForUndefinedNestedProperties(
        JsonNode valueJsonNode, String propertyPath, StringBuilder warnings) {

        Iterator<String> fieldNamesIterator = valueJsonNode.fieldNames();

        fieldNamesIterator.forEachRemaining(curFieldName -> {
            String fieldPath = PropertyUtils.buildPropertyPath(propertyPath, curFieldName);
            StringUtils.appendWithNewline(ValidationErrorUtils.notDefined(fieldPath), warnings);
        });
    }

    private static void validateStringTypeDefinition(
        JsonNode taskParametersJsonNode, String fieldName, String propertyDefinition,
        String propertyPath, StringBuilder errors) {

        if (!taskParametersJsonNode.has(fieldName)) {
            return;
        }

        JsonNode valueJsonNode = taskParametersJsonNode.get(fieldName);

        if ("object".equalsIgnoreCase(propertyDefinition) && valueJsonNode.isObject()) {
            return;
        }

        if (valueJsonNode.isTextual() && TypeValidator.isDataPillExpression(valueJsonNode.asText())) {
            return;
        }

        TypeValidator.validateType(valueJsonNode, propertyDefinition, propertyPath, errors);
    }

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
        } else {
            String actualType = JsonUtils.getJsonNodeType(valueJsonNode);
            StringUtils.appendWithNewline(ValidationErrorUtils.typeError(propertyPath, "object", actualType), errors);
        }
    }

    private static void validateArrayProperty(
        JsonNode taskParametersJsonNode, String fieldName, JsonNode definitionJsonNode, String propertyPath,
        @Nullable String originalTaskDefinitionForArrays, StringBuilder errors) {

        if (!taskParametersJsonNode.has(fieldName)) {
            return;
        }

        JsonNode valueJsonNode = taskParametersJsonNode.get(fieldName);

        if (!valueJsonNode.isArray()) {
            String actualType = JsonUtils.getJsonNodeType(valueJsonNode);
            StringUtils.appendWithNewline(ValidationErrorUtils.typeError(propertyPath, "array", actualType), errors);
            return;
        }

        JsonNode firstElementJsonNode = definitionJsonNode.get(0);

        if (isUnionType(definitionJsonNode, firstElementJsonNode)) {
            validateUnionArrayElements(valueJsonNode, definitionJsonNode, propertyPath, errors);
        } else if (originalTaskDefinitionForArrays == null) {
            validateObjectArrayElements(valueJsonNode, firstElementJsonNode, propertyPath, errors);
        } else {
            if (firstElementJsonNode.isTextual() && "task".equalsIgnoreCase(firstElementJsonNode.asText())) {
                TaskValidator.validateTaskArray(valueJsonNode, propertyPath, errors);
            } else if (firstElementJsonNode.isArray()) {
                validateArrayOfArrays(valueJsonNode, propertyPath, errors);
            }
        }
    }

    private static boolean isUnionType(JsonNode definitionJsonNode, JsonNode firstElement) {
        if (!firstElement.isTextual()) {
            return false;
        }

        for (int i = 0; i < definitionJsonNode.size(); i++) {
            if (!definitionJsonNode.get(i)
                .isTextual()) {
                return false;
            }
        }

        return true;
    }

    private static void validateUnionArrayElements(
        JsonNode arrayJsonNode, JsonNode allowedTypesJsonNode, String propertyPath, StringBuilder errors) {

        for (int i = 0; i < arrayJsonNode.size(); i++) {
            JsonNode valueJsonNode = arrayJsonNode.get(i);
            boolean matchesAnyType = false;

            for (int j = 0; j < allowedTypesJsonNode.size(); j++) {
                String allowedType = allowedTypesJsonNode.get(j)
                    .asText();

                if (TypeValidator.isTypeValid(valueJsonNode, allowedType)) {
                    matchesAnyType = true;
                    break;
                }
            }

            if (!matchesAnyType) {
                addUnionArrayElementError(valueJsonNode, allowedTypesJsonNode, propertyPath, errors);
            }
        }
    }

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
                validateObjectProperties(jsonNode, objectDefinitionJsonNode, elementPath, errors);
            }
        }
    }

    private static void validateObjectProperties(
        JsonNode jsonNode, JsonNode objectDefinitionJsonNode, String elementPath, StringBuilder errors) {

        Iterator<String> fieldNamesIterator = objectDefinitionJsonNode.fieldNames();

        fieldNamesIterator.forEachRemaining(fieldName -> {
            JsonNode fieldDefinitionJsonNode = objectDefinitionJsonNode.get(fieldName);
            String fieldPath = elementPath + "." + fieldName;

            if (fieldDefinitionJsonNode.isTextual()) {
                String expectedType = fieldDefinitionJsonNode.asText();

                if (jsonNode.has(fieldName)) {
                    JsonNode valueJsonNode = jsonNode.get(fieldName);
                    TypeValidator.validateType(valueJsonNode, expectedType, fieldPath, errors);
                }
            }
        });
    }

    private static void validateArrayOfArrays(JsonNode valueJsonNode, String propertyPath, StringBuilder errors) {
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

    private static void addUnionArrayElementError(
        JsonNode valueJsonNode, JsonNode allowedTypesJsonNode, String propertyPath, StringBuilder errors) {

        String elementValue = formatValue(valueJsonNode);
        String propertyName = PropertyUtils.extractPropertyNameFromPath(propertyPath);
        String actualType = JsonUtils.getJsonNodeType(valueJsonNode);

        StringBuilder expectedTypes = new StringBuilder();
        for (int j = 0; j < allowedTypesJsonNode.size(); j++) {
            if (j > 0) {
                expectedTypes.append(" or ");
            }

            JsonNode allowedTypeJsonNode = allowedTypesJsonNode.get(j);

            expectedTypes.append(allowedTypeJsonNode.asText());
        }

        StringUtils.appendWithNewline(
            ValidationErrorUtils.arrayElementError(elementValue, propertyName, expectedTypes.toString(), actualType),
            errors);
    }

    /**
     * Generates warnings for all properties recursively.
     */
    private static void generateWarningsForAllProperties(JsonNode jsonNode, String path, StringBuilder warnings) {
        Iterator<String> fieldNamesIterator = jsonNode.fieldNames();

        fieldNamesIterator.forEachRemaining(fieldName -> {
            String propertyPath = PropertyUtils.buildPropertyPath(path, fieldName);
            JsonNode valueJsonNode = jsonNode.get(fieldName);

            StringUtils.appendWithNewline(ValidationErrorUtils.notDefined(propertyPath), warnings);

            if (valueJsonNode.isObject()) {
                generateWarningsForAllProperties(valueJsonNode, propertyPath, warnings);
            }
        });
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
            if (shouldReportMissingProperty(nestedProp)) {
                String nestedPath = PropertyUtils.buildPropertyPath(propertyPath, nestedProp.name());
                StringUtils.appendWithNewline(ValidationErrorUtils.missingProperty(nestedPath), errors);

                if ("OBJECT".equalsIgnoreCase(nestedProp.type())) {
                    reportMissingNestedRequiredProperties(nestedProp, nestedPath, errors);
                }
            }
        }
    }

    private static boolean shouldReportMissingProperty(PropertyInfo propertyInfo) {
        return propertyInfo.required() &&
            (propertyInfo.displayCondition() == null ||
                org.apache.commons.lang3.StringUtils.isEmpty(propertyInfo.displayCondition()));
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

        if (!taskParametersJsonNode.has(fieldName)) {
            if (isRequired) {
                StringUtils.appendWithNewline(ValidationErrorUtils.missingProperty(propertyPath), errors);

                if ("OBJECT".equalsIgnoreCase(type)) {
                    reportMissingNestedRequiredProperties(propertyInfo, propertyPath, errors);
                }
            }

            return;
        }

        JsonNode valueJsonNode = taskParametersJsonNode.get(fieldName);

        if (valueJsonNode.isTextual() && TypeValidator.isDataPillExpression(valueJsonNode.asText())) {
            return;
        }

        validatePropertyByType(valueJsonNode, propertyInfo, propertyPath, originalCurrentParameters, errors, warnings);
    }

    private static void validatePropertyByType(
        JsonNode valueJsonNode, PropertyInfo propertyInfo, String propertyPath,
        String originalCurrentParameters, StringBuilder errors, StringBuilder warnings) {

        String type = propertyInfo.type();

        if ("OBJECT".equalsIgnoreCase(type)) {
            validateObjectPropertyFromPropertyInfo(
                valueJsonNode, propertyInfo, propertyPath, originalCurrentParameters, errors, warnings);
        } else if ("ARRAY".equalsIgnoreCase(type)) {
            ArrayPropertyValidator.validateFromPropertyInfo(
                valueJsonNode, propertyInfo, propertyPath, errors, warnings);
        } else {
            TypeValidator.validateType(valueJsonNode, type, propertyPath, errors);
        }
    }

    /**
     * Validates an object property using PropertyInfo.
     */
    private static void validateObjectPropertyFromPropertyInfo(
        JsonNode valueJsonNode, PropertyInfo propertyInfo,
        String propertyPath, String originalCurrentParameters, StringBuilder errors, StringBuilder warnings) {

        if (valueJsonNode.isNull()) {
            return;
        }

        if (!valueJsonNode.isObject()) {
            String actualType = JsonUtils.getJsonNodeType(valueJsonNode);

            StringUtils.appendWithNewline(ValidationErrorUtils.typeError(propertyPath, "object", actualType), errors);

            return;
        }

        List<PropertyInfo> nestedProperties = propertyInfo.nestedProperties();
        if (nestedProperties != null && !nestedProperties.isEmpty()) {
            validatePropertiesFromPropertyInfo(
                valueJsonNode, nestedProperties, propertyPath, originalCurrentParameters, errors, warnings);
        } else {
            generateWarningsForUndefinedNestedProperties(valueJsonNode, propertyPath, warnings);
        }
    }

    private static boolean isEmptyContainer(JsonNode node) {
        return (node.isObject() && node.isEmpty()) || (node.isArray() && node.isEmpty());
    }

    private static String formatValue(JsonNode jsonNode) {
        return jsonNode.isTextual() ? "'" + jsonNode.asText() + "'" : jsonNode.toString();
    }

    /**
     * Result of property validation with display condition.
     */
    private static class ValidationResult {
        private final boolean processed;

        private ValidationResult(boolean processed) {
            this.processed = processed;
        }

        static ValidationResult processed() {
            return new ValidationResult(true);
        }

        static ValidationResult skipped() {
            return new ValidationResult(false);
        }

        boolean wasProcessed() {
            return processed;
        }
    }
}
