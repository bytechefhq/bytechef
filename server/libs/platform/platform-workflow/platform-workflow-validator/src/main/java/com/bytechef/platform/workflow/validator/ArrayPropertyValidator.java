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

import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.StringUtils;
import com.bytechef.platform.workflow.validator.model.PropertyInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Handles validation of array properties.
 *
 * @author Marko Kriskovic
 */
class ArrayPropertyValidator {

    private ArrayPropertyValidator() {
    }

    /**
     * Validates array property using PropertyInfo.
     */
    static void validateFromPropertyInfo(
        JsonNode valueJsonNode, PropertyInfo propertyInfo, String propertyPath,
        StringBuilder errors, StringBuilder warnings) {

        if (valueJsonNode.isNull()) {
            return;
        }

        if (!valueJsonNode.isArray()) {
            String actualType = JsonUtils.getJsonNodeType(valueJsonNode);
            StringUtils.appendWithNewline(
                ValidationErrorUtils.typeError(propertyPath, "array", actualType), errors);
            return;
        }

        List<PropertyInfo> nestedProperties = propertyInfo.nestedProperties();
        if (nestedProperties == null || nestedProperties.isEmpty()) {
            return;
        }

        if (isTaskTypeArray(nestedProperties)) {
            TaskValidator.validateTaskArray(valueJsonNode, propertyPath, errors);
            return;
        }

        if (isWrappedDefinition(nestedProperties)) {
            validateWrappedArray(valueJsonNode, nestedProperties, propertyPath, errors, warnings);

            return;
        }

        if (!valueJsonNode.isEmpty()) {
            JsonNode firstElement = valueJsonNode.get(0);

            if (firstElement.isObject()) {
                validateObjectArray(valueJsonNode, nestedProperties, propertyPath, errors, warnings);
            } else {
                validateUnionTypeArray(valueJsonNode, nestedProperties, propertyPath, errors);
            }
        }
    }

    /**
     * Validates union type array elements (array of simple types).
     */
    private static void validateUnionTypeArray(
        JsonNode arrayJsonNode, List<PropertyInfo> allowedTypes, String propertyPath, StringBuilder errors) {

        for (int i = 0; i < arrayJsonNode.size(); i++) {
            JsonNode valueJsonNode = arrayJsonNode.get(i);

            boolean matchesAnyType = allowedTypes.stream()
                .anyMatch(typeInfo -> TypeValidator.isTypeValid(valueJsonNode, typeInfo.type()));

            if (!matchesAnyType) {
                addUnionTypeError(valueJsonNode, allowedTypes, propertyPath, errors);
            }
        }
    }

    /**
     * Validates object array elements.
     */
    private static void validateObjectArray(
        JsonNode arrayJsonNode, List<PropertyInfo> elementProperties,
        String propertyPath, StringBuilder errors, StringBuilder warnings) {

        if (isUnionTypeObjectArray(elementProperties)) {
            validateUnionTypeObjectArray(arrayJsonNode, elementProperties, propertyPath, errors, warnings);

            return;
        }

        JsonNode rootParametersJsonNode = createRootParametersJsonNode(arrayJsonNode, propertyPath);

        for (int i = 0; i < arrayJsonNode.size(); i++) {
            validateObjectArrayElement(
                arrayJsonNode.get(i), elementProperties, propertyPath, i, rootParametersJsonNode, errors, warnings);
        }
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

            if (!matchesAnyUnionType(elementJsonNode, unionTypes, elementPath, warnings)) {
                addUnionTypeObjectError(elementPath, unionTypes, errors);
            }
        }
    }

    private static void validateObjectArrayElement(
        JsonNode elementJsonNode, List<PropertyInfo> elementProperties, String propertyPath, int index,
        JsonNode rootParametersJsonNode, StringBuilder errors, StringBuilder warnings) {

        String elementPath = propertyPath + "[" + index + "]";

        if (!elementJsonNode.isObject()) {
            String actualType = JsonUtils.getJsonNodeType(elementJsonNode);

            StringUtils.appendWithNewline(ValidationErrorUtils.typeError(elementPath, "object", actualType), errors);

            return;
        }

        validateExtraPropertiesInArrayElement(
            elementJsonNode, elementProperties, propertyPath, index, rootParametersJsonNode, warnings);

        for (PropertyInfo propertyInfo : elementProperties) {
            validatePropertyInArrayElement(
                elementJsonNode, propertyInfo, elementPath, index, rootParametersJsonNode, errors);
        }
    }

    private static void validateExtraPropertiesInArrayElement(
        JsonNode elementJsonNode, List<PropertyInfo> elementProperties,
        String propertyPath, int index, JsonNode rootParametersJsonNode, StringBuilder warnings) {

        Iterator<String> fieldNamesIterator = elementJsonNode.fieldNames();

        fieldNamesIterator.forEachRemaining(fieldName -> {
            PropertyInfo matchingProperty = elementProperties.stream()
                .filter(prop -> fieldName.equals(prop.name()))
                .findFirst()
                .orElse(null);

            if (matchingProperty == null) {
                StringUtils.appendWithNewline(
                    "Property '" + propertyPath + "[index]." + fieldName + "' is not defined in task definition",
                    warnings);
            } else if (matchingProperty.displayCondition() != null &&
                !org.apache.commons.lang3.StringUtils.isEmpty(matchingProperty.displayCondition())) {

                checkDisplayConditionForExtraProperty(
                    matchingProperty, index, propertyPath, fieldName, rootParametersJsonNode, warnings);
            }
        });
    }

    private static void checkDisplayConditionForExtraProperty(
        PropertyInfo propertyInfo, int index, String propertyPath, String fieldName,
        JsonNode rootParametersJsonNode, StringBuilder warnings) {

        DisplayConditionEvaluator.DisplayConditionResult result =
            DisplayConditionEvaluator.evaluateForArrayElement(
                propertyInfo.displayCondition(), index, rootParametersJsonNode);

        if (!result.shouldShow()) {
            StringUtils.appendWithNewline(
                "Property '" + propertyPath + "[" + index + "]." + fieldName + "' is not defined in task definition",
                warnings);
        }
    }

    private static void validatePropertyInArrayElement(
        JsonNode elementJsonNode, PropertyInfo propertyInfo, String elementPath,
        int index, JsonNode rootParametersJsonNode, StringBuilder errors) {

        String fieldName = propertyInfo.name();
        String fieldPath = elementPath + "." + fieldName;
        boolean isRequired = propertyInfo.required();

        DisplayConditionEvaluator.DisplayConditionResult result =
            DisplayConditionEvaluator.evaluateForArrayElement(
                propertyInfo.displayCondition(), index, rootParametersJsonNode);

        if (result.shouldShow()) {
            if (isRequired && !elementJsonNode.has(fieldName)) {
                StringUtils.appendWithNewline("Missing required property: " + fieldPath, errors);
            } else if (elementJsonNode.has(fieldName)) {
                JsonNode valueJsonNode = elementJsonNode.get(fieldName);

                if (!valueJsonNode.isTextual() || !TypeValidator.isDataPillExpression(valueJsonNode.asText())) {
                    TypeValidator.validateType(valueJsonNode, propertyInfo.type(), fieldPath, errors);
                }
            }
        }
    }

    private static boolean matchesAnyUnionType(
        JsonNode elementJsonNode, List<PropertyInfo> unionTypes, String elementPath, StringBuilder warnings) {

        for (PropertyInfo unionType : unionTypes) {
            List<PropertyInfo> schemaProperties = unionType.nestedProperties();

            if (schemaProperties == null || schemaProperties.isEmpty()) {
                continue;
            }

            StringBuilder currentErrors = new StringBuilder();
            StringBuilder currentWarnings = new StringBuilder();

            List<PropertyInfo> simplifiedProperties = simplifyDisplayConditionsForUnionType(
                schemaProperties, elementPath.substring(0, elementPath.lastIndexOf('[')));

            PropertyValidator.validatePropertiesFromPropertyInfo(
                elementJsonNode, simplifiedProperties, elementPath,
                elementJsonNode.toString(), currentErrors, currentWarnings);

            if (currentErrors.isEmpty()) {
                if (!currentWarnings.isEmpty()) {
                    warnings.append(currentWarnings);
                }

                return true;
            }
        }

        return false;
    }

    private static void validateWrappedArray(
        JsonNode valueJsonNode, List<PropertyInfo> nestedProperties,
        String propertyPath, StringBuilder errors, StringBuilder warnings) {

        PropertyInfo wrapperInfo = nestedProperties.getFirst();
        String wrapperType = wrapperInfo.type();

        if ("ARRAY".equalsIgnoreCase(wrapperType)) {
            validateArrayOfArrays(valueJsonNode, wrapperInfo, propertyPath, errors, warnings);
        } else {
            validateObjectArray(valueJsonNode, wrapperInfo.nestedProperties(), propertyPath, errors, warnings);
        }
    }

    private static void validateArrayOfArrays(
        JsonNode valueJsonNode, PropertyInfo wrapperInfo,
        String propertyPath, StringBuilder errors, StringBuilder warnings) {

        for (int i = 0; i < valueJsonNode.size(); i++) {
            JsonNode elementJsonNode = valueJsonNode.get(i);
            String elementPath = propertyPath + "[" + i + "]";

            PropertyInfo elementInfo = new PropertyInfo(
                null, "ARRAY", null, false, false, null, wrapperInfo.nestedProperties());

            validateFromPropertyInfo(elementJsonNode, elementInfo, elementPath, errors, warnings);
        }
    }

    private static boolean isTaskTypeArray(List<PropertyInfo> nestedProperties) {
        return nestedProperties.size() == 1 && "TASK".equalsIgnoreCase(nestedProperties.getFirst()
            .type());
    }

    private static boolean isWrappedDefinition(List<PropertyInfo> nestedProperties) {
        return nestedProperties.size() == 1 &&
            nestedProperties.getFirst()
                .nestedProperties() != null
            &&
            !nestedProperties.getFirst()
                .nestedProperties()
                .isEmpty();
    }

    private static boolean isUnionTypeObjectArray(List<PropertyInfo> elementProperties) {
        return elementProperties.stream()
            .allMatch(prop -> "OBJECT".equalsIgnoreCase(prop.type()) && prop.nestedProperties() != null &&
                !CollectionUtils.isEmpty(prop.nestedProperties()));
    }

    private static JsonNode createRootParametersJsonNode(JsonNode arrayValue, String propertyPath) {
        ObjectNode rootNode = com.bytechef.commons.util.JsonUtils.createObjectNode();

        rootNode.set(propertyPath, arrayValue);

        return rootNode;
    }

    private static List<PropertyInfo> simplifyDisplayConditionsForUnionType(
        List<PropertyInfo> properties, String arrayPath) {

        List<PropertyInfo> simplified = new ArrayList<>();
        String baseArrayName = arrayPath.replaceAll("\\[\\d+]", "");

        for (PropertyInfo prop : properties) {
            String displayCondition = prop.displayCondition();

            if (displayCondition != null && !displayCondition.isEmpty()) {
                String simplifiedCondition = simplifyConditionForUnionType(displayCondition, baseArrayName);

                simplified.add(new PropertyInfo(
                    prop.name(), prop.type(), prop.description(), prop.required(),
                    prop.expressionEnabled(), simplifiedCondition, prop.nestedProperties()));
            } else {
                simplified.add(prop);
            }
        }

        return simplified;
    }

    private static String simplifyConditionForUnionType(String condition, String baseArrayName) {
        String escapedArrayName = baseArrayName.replaceAll("\\[", "\\\\[")
            .replaceAll("]", "\\\\]");

        String simplified = condition.replaceAll(escapedArrayName + "\\[index]\\[index]\\.", "");

        simplified = simplified.replaceAll(escapedArrayName + "\\[index]\\.", "");

        return simplified;
    }

    private static void addUnionTypeError(
        JsonNode valueJsonNode, List<PropertyInfo> allowedTypes, String propertyPath, StringBuilder errors) {

        String elementValue = formatValue(valueJsonNode);
        String propertyName = PropertyUtils.extractPropertyNameFromPath(propertyPath);
        String actualType = JsonUtils.getJsonNodeType(valueJsonNode);

        StringBuilder expectedTypes = new StringBuilder();

        for (int j = 0; j < allowedTypes.size(); j++) {
            if (j > 0) {
                expectedTypes.append(" or ");
            }

            PropertyInfo propertyInfo = allowedTypes.get(j);

            expectedTypes.append(org.apache.commons.lang3.StringUtils.lowerCase(propertyInfo.type()));
        }

        StringUtils.appendWithNewline(
            ValidationErrorUtils.arrayElementError(elementValue, propertyName, expectedTypes.toString(), actualType),
            errors);
    }

    private static void addUnionTypeObjectError(
        String elementPath, List<PropertyInfo> unionTypes, StringBuilder errors) {

        StringBuilder typeNames = new StringBuilder();

        for (int j = 0; j < unionTypes.size(); j++) {
            if (j > 0) {
                typeNames.append(", ");
            }

            PropertyInfo propertyInfo = unionTypes.get(j);

            typeNames.append(propertyInfo.name());
        }

        StringUtils.appendWithNewline(
            "Property '" + elementPath + "' does not match any of the expected union types: " + typeNames,
            errors);
    }

    private static String formatValue(JsonNode jsonNode) {
        return jsonNode.isTextual() ? "'" + jsonNode.asText() + "'" : jsonNode.toString();
    }
}
