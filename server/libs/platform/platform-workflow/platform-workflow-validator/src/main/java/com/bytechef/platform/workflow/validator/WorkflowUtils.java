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

import com.bytechef.evaluator.Evaluator;
import com.bytechef.evaluator.SpelEvaluator;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;

/**
 * @author Ivica Cardic
 */
class WorkflowUtils {

    private static final Evaluator EVALUATOR = SpelEvaluator.builder()
        .build();

    public static boolean extractAndEvaluateCondition(String condition, JsonNode actualParameters) {
        if (StringUtils.isBlank(condition)) {
            return true;
        }

        try {
            return evaluateCondition(condition, actualParameters);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid logic for display condition: '" + condition + "'");
        }
    }

    private static boolean evaluateCondition(String condition, JsonNode actualParameters) {
        Map<String, String> map = new HashMap<>();
        map.put("convertedExpression", "=" + condition);

        Map<String, Object> actualParametersMap =
            com.bytechef.commons.util.JsonUtils.read(actualParameters.toString(), new TypeReference<>() {});

        try {
            Map<String, Object> evaluated = EVALUATOR.evaluate(map, actualParametersMap);

            Object convertedExpression = evaluated.get("convertedExpression");

            return parseBoolean(convertedExpression.toString());
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static boolean parseBoolean(String string) {
        if ("true".equalsIgnoreCase(string)) {
            return true;
        } else if ("false".equalsIgnoreCase(string)) {
            return false;
        } else {
            throw new IllegalArgumentException(
                "Invalid boolean value: '" + string + "'. Expected 'true' or 'false'");
        }
    }
}
