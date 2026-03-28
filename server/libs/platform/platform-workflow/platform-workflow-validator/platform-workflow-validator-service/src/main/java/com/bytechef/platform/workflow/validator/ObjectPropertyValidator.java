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
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import tools.jackson.databind.JsonNode;

/**
 * Handles validation of object properties.
 *
 * @author Marko Kriskovic
 */
class ObjectPropertyValidator {

    private ObjectPropertyValidator() {
    }

    /**
     * Validates an object property using PropertyInfo.
     */
    static void validate(
        JsonNode valueJsonNode, PropertyInfo propertyInfo,
        String propertyPath, String originalCurrentParameters, StringBuilder errors, StringBuilder warnings) {

        if (valueJsonNode.isNull()) {
            return;
        }

        if (!valueJsonNode.isObject()) {
            String actualType = JsonNodeUtils.getJsonNodeType(valueJsonNode);

            StringUtils.appendWithNewline(ValidationErrorUtils.typeError(propertyPath, "object", actualType), errors);

            return;
        }

        List<PropertyInfo> nestedProperties = propertyInfo.nestedProperties();

        if (nestedProperties != null && !nestedProperties.isEmpty()) {
            PropertyValidator.validateProperties(
                valueJsonNode, nestedProperties, propertyPath, originalCurrentParameters, errors, warnings);
        } else {
            generateWarningsForUndefinedNestedProperties(valueJsonNode, propertyPath, warnings);
        }
    }

    private static void generateWarningsForUndefinedNestedProperties(
        JsonNode valueJsonNode, String propertyPath, StringBuilder warnings) {

        Collection<String> propertyNames = valueJsonNode.propertyNames();

        Iterator<String> propertyNamesIterator = propertyNames.iterator();

        propertyNamesIterator.forEachRemaining(curPropertyName -> {
            String curPropertyPath = PropertyUtils.buildPropertyPath(propertyPath, curPropertyName);

            StringUtils.appendWithNewline(ValidationErrorUtils.notDefined(curPropertyPath), warnings);
        });
    }
}
