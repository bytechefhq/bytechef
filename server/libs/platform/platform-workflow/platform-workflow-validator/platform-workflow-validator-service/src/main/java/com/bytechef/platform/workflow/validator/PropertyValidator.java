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
import com.bytechef.platform.workflow.validator.DisplayConditionEvaluator.DisplayConditionResult;
import com.bytechef.platform.workflow.validator.model.PropertyInfo;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JsonNode;

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
    public static void validateProperties(
        JsonNode taskParametersJsonNode, List<PropertyInfo> propertyInfos, String path,
        String originalCurrentParameters, StringBuilder errors, StringBuilder warnings) {

        if (propertyInfos.isEmpty()) {
            generateWarningsForAllProperties(taskParametersJsonNode, path, warnings);

            return;
        }

        Set<String> validatedPropertyNames = new HashSet<>();

        for (PropertyInfo propertyInfo : propertyInfos) {
            ValidationResult result = validatePropertyWithDisplayCondition(
                taskParametersJsonNode, propertyInfo, path, originalCurrentParameters, errors, warnings);

            if (result.wasProcessed()) {
                validatedPropertyNames.add(propertyInfo.name());
            }
        }

        checkForUndefinedProperties(taskParametersJsonNode, validatedPropertyNames, path, warnings);
    }

    private static ValidationResult validatePropertyWithDisplayCondition(
        JsonNode taskParametersJsonNode, PropertyInfo propertyInfo, String path,
        String originalCurrentParameters, StringBuilder errors, StringBuilder warnings) {

        String fieldName = propertyInfo.name();

        String propertyPath = PropertyUtils.buildPropertyPath(path, fieldName);

        DisplayConditionResult displayConditionResult = DisplayConditionEvaluator.evaluate(
            propertyInfo.displayCondition(),
            com.bytechef.commons.util.JsonUtils.readTree(originalCurrentParameters));

        if (displayConditionResult.isMalformed()) {
            handleMalformedDisplayCondition(
                taskParametersJsonNode, fieldName, propertyPath, displayConditionResult.getMalformedMessage(),
                warnings);

            return ValidationResult.processed();
        }

        if (displayConditionResult.shouldShow()) {
            validateProperty(
                taskParametersJsonNode, propertyInfo, propertyPath, originalCurrentParameters, errors, warnings);

            return ValidationResult.processed();
        }

        return ValidationResult.skipped();
    }

    private static void handleMalformedDisplayCondition(
        JsonNode taskParametersJsonNode, String fieldName, String propertyPath, @Nullable String malformedMessage,
        StringBuilder warnings) {

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

        Iterator<String> fieldNamesIterator = taskParametersJsonNode.propertyNames()
            .iterator();

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

    private static void generateWarningsForAllProperties(
        JsonNode jsonNode, String propertyPath, StringBuilder warnings) {

        Collection<String> propertyNames = jsonNode.propertyNames();

        Iterator<String> propertyNamesIterator = propertyNames.iterator();

        propertyNamesIterator.forEachRemaining(propertyName -> {
            String curPropertyPath = PropertyUtils.buildPropertyPath(propertyPath, propertyName);
            JsonNode valueJsonNode = jsonNode.get(propertyName);

            StringUtils.appendWithNewline(ValidationErrorUtils.notDefined(curPropertyPath), warnings);

            if (valueJsonNode.isObject()) {
                generateWarningsForAllProperties(valueJsonNode, curPropertyPath, warnings);
            }
        });
    }

    private static void reportMissingNestedRequiredProperties(
        PropertyInfo propertyInfo, String propertyPath, StringBuilder errors) {

        List<PropertyInfo> nestedPropertyInfos = propertyInfo.nestedProperties();

        if (nestedPropertyInfos == null || nestedPropertyInfos.isEmpty()) {
            return;
        }

        for (PropertyInfo nestedPropertyInfo : nestedPropertyInfos) {
            if (shouldReportMissingProperty(nestedPropertyInfo)) {
                String nestedPath = PropertyUtils.buildPropertyPath(propertyPath, nestedPropertyInfo.name());

                StringUtils.appendWithNewline(ValidationErrorUtils.missingProperty(nestedPath), errors);

                if ("OBJECT".equalsIgnoreCase(nestedPropertyInfo.type())) {
                    reportMissingNestedRequiredProperties(nestedPropertyInfo, nestedPath, errors);
                }
            }
        }
    }

    private static boolean shouldReportMissingProperty(PropertyInfo propertyInfo) {
        return propertyInfo.required() &&
            (propertyInfo.displayCondition() == null ||
                org.apache.commons.lang3.StringUtils.isEmpty(propertyInfo.displayCondition()));
    }

    private static void validateProperty(
        JsonNode taskParametersJsonNode, PropertyInfo propertyInfo, String propertyPath,
        String originalCurrentParameters, StringBuilder errors, StringBuilder warnings) {

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

        if (valueJsonNode.isString() && TypeValidator.isDataPillExpression(valueJsonNode.asString())) {
            return;
        }

        validatePropertyByType(valueJsonNode, propertyInfo, propertyPath, originalCurrentParameters, errors, warnings);
    }

    private static void validatePropertyByType(
        JsonNode valueJsonNode, PropertyInfo propertyInfo, String propertyPath,
        String originalCurrentParameters, StringBuilder errors, StringBuilder warnings) {

        String type = propertyInfo.type();

        if ("OBJECT".equalsIgnoreCase(type)) {
            ObjectPropertyValidator.validate(
                valueJsonNode, propertyInfo, propertyPath, originalCurrentParameters, errors, warnings);
        } else if ("ARRAY".equalsIgnoreCase(type)) {
            ArrayPropertyValidator.validate(valueJsonNode, propertyInfo, propertyPath, errors, warnings);
        } else {
            TypeValidator.validateType(valueJsonNode, type, propertyPath, errors);
        }
    }

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
