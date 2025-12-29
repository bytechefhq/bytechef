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
import tools.jackson.databind.JsonNode;

/**
 * Utility class for validating individual fields and their properties. Contains validation logic for required fields,
 * field types, and field patterns.
 *
 * @author Marko Kriskovic
 */
class FieldValidator {

    private FieldValidator() {
    }

    /**
     * Validates that a required string field exists and is of correct type.
     */
    public static void appendErrorRequiredStringField(JsonNode jsonNode, String fieldName, StringBuilder errors) {
        if (!jsonNode.has(fieldName)) {
            StringUtils.appendWithNewline("Missing required field: " + fieldName, errors);
        } else {
            JsonNode fieldJsonNode = jsonNode.get(fieldName);

            if (!fieldJsonNode.isTextual()) {
                StringUtils.appendWithNewline("Field '" + fieldName + "' must be a string", errors);
            }
        }
    }
}
