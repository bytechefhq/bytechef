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
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;

/**
 * Utility class for workflow expression evaluation and display condition processing.
 *
 * @author Ivica Cardic
 */
class WorkflowUtils {

    private static final Evaluator EVALUATOR = SpelEvaluator.builder()
        .build();

    /**
     * Extracts and evaluates a display condition expression against the actual parameters.
     *
     * <p>
     * <b>Security Note:</b> SpEL expression evaluation is used for evaluating display conditions in workflow property
     * definitions. This determines which workflow fields are visible based on the current parameter values. The
     * SPEL_INJECTION suppression is appropriate because:
     *
     * <ul>
     * <li>Display conditions are defined in component definitions authored by trusted developers</li>
     * <li>The SpelEvaluator provides input validation and method whitelisting</li>
     * <li>Expression evaluation is essential for dynamic form behavior in the workflow editor</li>
     * </ul>
     *
     * <p>
     * The REDOS suppression is inherited from the SpelEvaluator dependency which handles regex-based expression
     * validation safely.
     */
    @SuppressFBWarnings({
        "SPEL_INJECTION", "REDOS"
    })
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
