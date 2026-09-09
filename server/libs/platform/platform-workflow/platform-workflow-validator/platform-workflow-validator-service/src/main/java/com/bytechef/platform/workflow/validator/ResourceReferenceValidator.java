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
import java.util.List;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JsonNode;

/**
 * @author Ivica Cardic
 */
class ResourceReferenceValidator {

    private ResourceReferenceValidator() {
    }

    static void validate(
        @Nullable JsonNode parametersJsonNode, List<PropertyInfo> taskDefinition, String pathPrefix,
        WorkflowValidator.ResourceReferenceProvider resourceReferenceProvider, StringBuilder errors,
        StringBuilder warnings) {

        if (parametersJsonNode == null || !parametersJsonNode.isObject()) {
            return;
        }

        for (PropertyInfo propertyInfo : taskDefinition) {
            String propertyName = propertyInfo.name();

            if (propertyName == null || !parametersJsonNode.has(propertyName)) {
                continue;
            }

            JsonNode valueJsonNode = parametersJsonNode.get(propertyName);
            String propertyPath = pathPrefix.isEmpty() ? propertyName : pathPrefix + "." + propertyName;

            if (propertyInfo.resourceType() != null) {
                validateReference(
                    valueJsonNode, propertyPath, propertyInfo.resourceType(), resourceReferenceProvider, errors,
                    warnings);
            } else if ("OBJECT".equalsIgnoreCase(propertyInfo.type()) && propertyInfo.nestedProperties() != null) {
                validate(
                    valueJsonNode, propertyInfo.nestedProperties(), propertyPath, resourceReferenceProvider, errors,
                    warnings);
            }
        }
    }

    private static void validateReference(
        JsonNode valueJsonNode, String propertyPath, String resourceType,
        WorkflowValidator.ResourceReferenceProvider resourceReferenceProvider, StringBuilder errors,
        StringBuilder warnings) {

        if (!valueJsonNode.isValueNode() || valueJsonNode.isNull()) {
            return;
        }

        String reference = valueJsonNode.asString();

        if (reference.isBlank() || reference.contains("${")) {
            return;
        }

        try {
            String problem = resourceReferenceProvider.findProblem(resourceType, reference);

            if (problem != null) {
                StringUtils.appendWithNewline(ValidationErrorUtils.missingResource(propertyPath, problem), errors);
            }
        } catch (RuntimeException e) {
            StringUtils.appendWithNewline(
                ValidationErrorUtils.resourceCheckFailed(propertyPath, e.getMessage()), warnings);
        }
    }
}
