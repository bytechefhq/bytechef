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
import com.bytechef.platform.workflow.validator.DisplayConditionEvaluator.DisplayConditionResult;
import com.bytechef.platform.workflow.validator.model.PropertyInfo;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

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
    static void validate(
        JsonNode valueJsonNode, PropertyInfo propertyInfo, String propertyPath, StringBuilder errors,
        StringBuilder warnings) {

        if (valueJsonNode.isNull()) {
            return;
        }

        if (!valueJsonNode.isArray()) {
            String actualType = JsonNodeUtils.getJsonNodeType(valueJsonNode);

            StringUtils.appendWithNewline(ValidationErrorUtils.typeError(propertyPath, "array", actualType), errors);

            return;
        }

        List<PropertyInfo> nestedPropertyInfos = propertyInfo.nestedProperties();

        if (nestedPropertyInfos == null || nestedPropertyInfos.isEmpty()) {
            return;
        }

        if (isTaskTypeArray(nestedPropertyInfos)) {
            TaskValidator.validateTaskArray(valueJsonNode, propertyPath, errors);

            return;
        }

        if (isWrappedDefinition(nestedPropertyInfos)) {
            validateWrappedArray(valueJsonNode, nestedPropertyInfos, propertyPath, errors, warnings);

            return;
        }

        if (!valueJsonNode.isEmpty()) {
            JsonNode firstElement = valueJsonNode.get(0);

            if (firstElement.isObject()) {
                validateObjectArray(valueJsonNode, nestedPropertyInfos, propertyPath, errors, warnings);
            } else {
                validateUnionTypeArray(valueJsonNode, nestedPropertyInfos, propertyPath, errors);
            }
        }
    }

    private static void addUnionTypeError(
        JsonNode valueJsonNode, List<PropertyInfo> allowedTypePropertyInfos, String propertyPath,
        StringBuilder errors) {

        String elementValue = formatValue(valueJsonNode);
        String propertyName = PropertyUtils.extractPropertyNameFromPath(propertyPath);
        String actualType = JsonNodeUtils.getJsonNodeType(valueJsonNode);

        StringBuilder expectedTypes = new StringBuilder();

        for (int i = 0; i < allowedTypePropertyInfos.size(); i++) {
            if (i > 0) {
                expectedTypes.append(" or ");
            }

            PropertyInfo propertyInfo = allowedTypePropertyInfos.get(i);

            expectedTypes.append(org.apache.commons.lang3.StringUtils.lowerCase(propertyInfo.type()));
        }

        StringUtils.appendWithNewline(
            ValidationErrorUtils.arrayElementError(elementValue, propertyName, expectedTypes.toString(), actualType),
            errors);
    }

    private static void addUnionTypeObjectError(
        String elementPath, List<PropertyInfo> unionTypePropertyInfos, StringBuilder errors) {

        StringBuilder typeNames = new StringBuilder();

        for (int i = 0; i < unionTypePropertyInfos.size(); i++) {
            if (i > 0) {
                typeNames.append(", ");
            }

            PropertyInfo propertyInfo = unionTypePropertyInfos.get(i);

            typeNames.append(propertyInfo.name());
        }

        StringUtils.appendWithNewline(
            "Property '" + elementPath + "' does not match any of the expected union types: " + typeNames,
            errors);
    }

    private static void checkDisplayConditionForExtraProperty(
        PropertyInfo propertyInfo, int index, String propertyPath, String fieldName, JsonNode rootParametersJsonNode,
        StringBuilder warnings) {

        DisplayConditionResult displayConditionResult = DisplayConditionEvaluator.evaluateForArrayElement(
            propertyInfo.displayCondition(), index, rootParametersJsonNode);

        if (!displayConditionResult.shouldShow()) {
            StringUtils.appendWithNewline(
                "Property '" + propertyPath + "[" + index + "]." + fieldName + "' is not defined in task definition",
                warnings);
        }
    }

    private static JsonNode createRootParametersJsonNode(JsonNode arrayValue, String propertyPath) {
        ObjectNode rootNode = com.bytechef.commons.util.JsonUtils.createObjectNode();

        rootNode.set(propertyPath, arrayValue);

        return rootNode;
    }

    private static String formatValue(JsonNode jsonNode) {
        return jsonNode.isString() ? "'" + jsonNode.asString() + "'" : jsonNode.toString();
    }

    private static boolean isTaskTypeArray(List<PropertyInfo> nestedPropertyInfos) {
        PropertyInfo nestedPropertyInfo = nestedPropertyInfos.getFirst();

        return nestedPropertyInfos.size() == 1 && "TASK".equalsIgnoreCase(nestedPropertyInfo.type());
    }

    private static boolean isUnionTypeObjectArray(List<PropertyInfo> elementPropertyInfos) {
        return elementPropertyInfos.stream()
            .allMatch(propertyInfo -> "OBJECT".equalsIgnoreCase(propertyInfo.type()) &&
                propertyInfo.nestedProperties() != null &&
                !CollectionUtils.isEmpty(propertyInfo.nestedProperties()));
    }

    private static boolean isWrappedDefinition(List<PropertyInfo> nestedProperties) {
        PropertyInfo propertyInfo = nestedProperties.getFirst();

        List<PropertyInfo> propertyInfos = propertyInfo.nestedProperties();

        return nestedProperties.size() == 1 && propertyInfos != null && !propertyInfos.isEmpty();
    }

    private static boolean matchesAnyUnionType(
        JsonNode elementJsonNode, List<PropertyInfo> unionTypePropertyInfos, String elementPath,
        StringBuilder warnings) {

        for (PropertyInfo unionTypePropertyInfo : unionTypePropertyInfos) {
            List<PropertyInfo> schemaPropertyInfos = unionTypePropertyInfo.nestedProperties();

            if (schemaPropertyInfos == null || schemaPropertyInfos.isEmpty()) {
                continue;
            }

            StringBuilder currentErrors = new StringBuilder();
            StringBuilder currentWarnings = new StringBuilder();

            List<PropertyInfo> simplifiedPropertyInfos = simplifyDisplayConditionsForUnionType(
                schemaPropertyInfos, elementPath.substring(0, elementPath.lastIndexOf('[')));

            PropertyValidator.validateProperties(
                elementJsonNode, simplifiedPropertyInfos, elementPath, elementJsonNode.toString(), currentErrors,
                currentWarnings);

            if (currentErrors.isEmpty()) {
                if (!currentWarnings.isEmpty()) {
                    warnings.append(currentWarnings);
                }

                return true;
            }
        }

        return false;
    }

    private static List<PropertyInfo> simplifyDisplayConditionsForUnionType(
        List<PropertyInfo> propertyInfos, String arrayPath) {

        List<PropertyInfo> simplifiedPropertyInfos = new ArrayList<>();
        String baseArrayName = arrayPath.replaceAll("\\[\\d+]", "");

        for (PropertyInfo propertyInfo : propertyInfos) {
            String displayCondition = propertyInfo.displayCondition();

            if (displayCondition != null && !displayCondition.isEmpty()) {
                String simplifiedDisplayCondition = simplifyDisplayConditionForUnionType(
                    displayCondition, baseArrayName);

                simplifiedPropertyInfos.add(new PropertyInfo(
                    propertyInfo.name(), propertyInfo.type(), propertyInfo.description(), propertyInfo.required(),
                    propertyInfo.expressionEnabled(), simplifiedDisplayCondition, propertyInfo.options(),
                    propertyInfo.nestedProperties()));
            } else {
                simplifiedPropertyInfos.add(propertyInfo);
            }
        }

        return simplifiedPropertyInfos;
    }

    private static String simplifyDisplayConditionForUnionType(String condition, String baseArrayName) {
        String escapedArrayName = baseArrayName.replaceAll("\\[", "\\\\[")
            .replaceAll("]", "\\\\]");

        String simplifiedCondition = condition.replaceAll(escapedArrayName + "\\[index]\\[index]\\.", "");

        simplifiedCondition = simplifiedCondition.replaceAll(escapedArrayName + "\\[index]\\.", "");

        return simplifiedCondition;
    }

    private static void validateArrayOfArrays(
        JsonNode valueJsonNode, PropertyInfo wrapperInfo, String propertyPath, StringBuilder errors,
        StringBuilder warnings) {

        for (int i = 0; i < valueJsonNode.size(); i++) {
            JsonNode elementJsonNode = valueJsonNode.get(i);
            String elementPath = propertyPath + "[" + i + "]";

            PropertyInfo elementInfo = new PropertyInfo(
                null, "ARRAY", null, false, false, null, wrapperInfo.nestedProperties());

            validate(elementJsonNode, elementInfo, elementPath, errors, warnings);
        }
    }

    private static void validateExtraPropertiesInArrayElement(
        JsonNode elementJsonNode, List<PropertyInfo> elementPropertyInfos, String propertyPath, int index,
        JsonNode rootParametersJsonNode, StringBuilder warnings) {

        Collection<String> propertyNames = elementJsonNode.propertyNames();

        Iterator<String> fieldNamesIterator = propertyNames.iterator();

        fieldNamesIterator.forEachRemaining(fieldName -> {
            PropertyInfo matchingProperty = elementPropertyInfos.stream()
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

    /**
     * Validates object array elements.
     */
    private static void validateObjectArray(
        JsonNode arrayJsonNode, List<PropertyInfo> elementPropertyInfos, String propertyPath, StringBuilder errors,
        StringBuilder warnings) {

        if (isUnionTypeObjectArray(elementPropertyInfos)) {
            validateUnionTypeObjectArray(arrayJsonNode, elementPropertyInfos, propertyPath, errors, warnings);

            return;
        }

        JsonNode rootParametersJsonNode = createRootParametersJsonNode(arrayJsonNode, propertyPath);

        for (int i = 0; i < arrayJsonNode.size(); i++) {
            validateObjectArrayElement(
                arrayJsonNode.get(i), elementPropertyInfos, propertyPath, i, rootParametersJsonNode, errors, warnings);
        }
    }

    private static void validateObjectArrayElement(
        JsonNode elementJsonNode, List<PropertyInfo> elementPropertyInfos, String propertyPath, int index,
        JsonNode rootParametersJsonNode, StringBuilder errors, StringBuilder warnings) {

        String elementPath = propertyPath + "[" + index + "]";

        if (!elementJsonNode.isObject()) {
            String actualType = JsonNodeUtils.getJsonNodeType(elementJsonNode);

            StringUtils.appendWithNewline(ValidationErrorUtils.typeError(elementPath, "object", actualType), errors);

            return;
        }

        validateExtraPropertiesInArrayElement(
            elementJsonNode, elementPropertyInfos, propertyPath, index, rootParametersJsonNode, warnings);

        for (PropertyInfo propertyInfo : elementPropertyInfos) {
            validatePropertyInArrayElement(
                elementJsonNode, propertyInfo, elementPath, index, rootParametersJsonNode, errors);
        }
    }

    private static void validatePropertyInArrayElement(
        JsonNode elementJsonNode, PropertyInfo propertyInfo, String elementPath, int index,
        JsonNode rootParametersJsonNode, StringBuilder errors) {

        String fieldName = propertyInfo.name();
        String fieldPath = elementPath + "." + fieldName;
        boolean isRequired = propertyInfo.required();

        DisplayConditionResult displayConditionResult = DisplayConditionEvaluator.evaluateForArrayElement(
            propertyInfo.displayCondition(), index, rootParametersJsonNode);

        if (displayConditionResult.shouldShow()) {
            if (isRequired && !elementJsonNode.has(fieldName)) {
                StringUtils.appendWithNewline("Missing required property: " + fieldPath, errors);
            } else if (elementJsonNode.has(fieldName)) {
                JsonNode valueJsonNode = elementJsonNode.get(fieldName);

                if (!valueJsonNode.isString() || !TypeValidator.isDataPillExpression(valueJsonNode.asString())) {
                    TypeValidator.validateType(valueJsonNode, propertyInfo.type(), fieldPath, errors);
                }
            }
        }
    }

    /**
     * Validates union type array elements (array of simple types).
     */
    private static void validateUnionTypeArray(
        JsonNode arrayJsonNode, List<PropertyInfo> allowedTypePropertyInfos, String propertyPath,
        StringBuilder errors) {

        for (int i = 0; i < arrayJsonNode.size(); i++) {
            JsonNode valueJsonNode = arrayJsonNode.get(i);

            boolean matchesAnyType = allowedTypePropertyInfos.stream()
                .anyMatch(typeInfo -> TypeValidator.isTypeValid(valueJsonNode, typeInfo.type()));

            if (!matchesAnyType) {
                addUnionTypeError(valueJsonNode, allowedTypePropertyInfos, propertyPath, errors);
            }
        }
    }

    /**
     * Validates union type object array where each object must match one of several schemas.
     */
    private static void validateUnionTypeObjectArray(
        JsonNode arrayJsonNode, List<PropertyInfo> unionTypePropertyInfos, String propertyPath, StringBuilder errors,
        StringBuilder warnings) {

        for (int i = 0; i < arrayJsonNode.size(); i++) {
            JsonNode elementJsonNode = arrayJsonNode.get(i);
            String elementPath = propertyPath + "[" + i + "]";

            if (!elementJsonNode.isObject()) {
                String actualType = JsonNodeUtils.getJsonNodeType(elementJsonNode);

                StringUtils.appendWithNewline(
                    ValidationErrorUtils.typeError(elementPath, "object", actualType), errors);

                continue;
            }

            if (!matchesAnyUnionType(elementJsonNode, unionTypePropertyInfos, elementPath, warnings)) {
                addUnionTypeObjectError(elementPath, unionTypePropertyInfos, errors);
            }
        }
    }

    private static void validateWrappedArray(
        JsonNode valueJsonNode, List<PropertyInfo> nestedPropertyInfos, String propertyPath, StringBuilder errors,
        StringBuilder warnings) {

        PropertyInfo wrapperPropertyInfo = nestedPropertyInfos.getFirst();
        String wrapperType = wrapperPropertyInfo.type();

        if ("ARRAY".equalsIgnoreCase(wrapperType)) {
            validateArrayOfArrays(valueJsonNode, wrapperPropertyInfo, propertyPath, errors, warnings);
        } else {
            validateObjectArray(valueJsonNode, wrapperPropertyInfo.nestedProperties(), propertyPath, errors, warnings);
        }
    }
}
