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

package com.bytechef.component.ai.typesafe.action;

import static com.bytechef.component.ai.typesafe.constant.TypeSafeConstants.CRITERIA;
import static com.bytechef.component.ai.typesafe.constant.TypeSafeConstants.INSTRUCTIONS;
import static com.bytechef.component.ai.typesafe.constant.TypeSafeConstants.IS_TRUE;
import static com.bytechef.component.ai.typesafe.constant.TypeSafeConstants.MODEL_OUTPUT_PROPERTY;
import static com.bytechef.component.ai.typesafe.constant.TypeSafeConstants.MODEL_PROPERTY;
import static com.bytechef.component.ai.typesafe.constant.TypeSafeConstants.NOUL;
import static com.bytechef.component.ai.typesafe.constant.TypeSafeConstants.STATE_PROPERTY;
import static com.bytechef.component.ai.typesafe.constant.TypeSafeConstants.THRESHOLD;
import static com.bytechef.component.ai.typesafe.constant.TypeSafeConstants.TYPE;
import static com.bytechef.component.ai.typesafe.constant.TypeSafeConstants.USAGE_OUTPUT_PROPERTY;
import static com.bytechef.component.ai.typesafe.constant.TypeSafeConstants.WHEN_FALSE;
import static com.bytechef.component.ai.typesafe.constant.TypeSafeConstants.WHEN_TRUE;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Property.ControlType.TEXT_AREA;

import com.bytechef.component.ai.typesafe.util.TypeSafeUtils;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Asks a yes/no question whose answer is a truth value in {@code [0, 1]} rather than a boolean.
 *
 * @author Ivica Cardic
 */
public class TypeSafeNoulAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(NOUL)
        .title("Noul")
        .description(
            "Ask a yes/no question about the state. The answer is a truth value between 0 and 1 rather than a " +
                "boolean; there is no separate confidence, because the value already is the certainty.")
        .properties(
            MODEL_PROPERTY,
            STATE_PROPERTY,
            string(INSTRUCTIONS)
                .label("Instructions")
                .description("The yes/no question to ask, e.g. \"Does this message convey urgency?\"")
                .controlType(TEXT_AREA)
                .required(true),
            string(WHEN_TRUE)
                .label("When True")
                .description(
                    "What a true answer means, written as a statement about the state, e.g. \"The customer " +
                        "needs help within the hour\".")
                .controlType(TEXT_AREA)
                .required(false),
            string(WHEN_FALSE)
                .label("When False")
                .description(
                    "What a false answer means, written as a statement about the state, e.g. \"The request can " +
                        "wait for the normal queue\".")
                .controlType(TEXT_AREA)
                .required(false),
            number(THRESHOLD)
                .label("Threshold")
                .description("The truth value at or above which Is True is reported as true.")
                .defaultValue(0.5)
                .minValue(0)
                .maxValue(1)
                .advancedOption(true)
                .required(false))
        .output(
            outputSchema(
                object()
                    .properties(
                        string(TYPE)
                            .description("The answer type, always `noul`."),
                        number(NOUL)
                            .description("The truth value, between 0 (false) and 1 (true)."),
                        bool(IS_TRUE)
                            .description("Whether the truth value reached the threshold."),
                        MODEL_OUTPUT_PROPERTY,
                        USAGE_OUTPUT_PROPERTY)))
        .help("", "https://docs.bytechef.io/reference/components/typesafe_v1#noul")
        .perform(TypeSafeNoulAction::perform);

    private TypeSafeNoulAction() {
    }

    public static Map<String, Object> perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        Map<String, Object> question = new LinkedHashMap<>();

        question.put(TYPE, NOUL);
        question.put(INSTRUCTIONS, inputParameters.getRequiredString(INSTRUCTIONS));

        Map<String, Object> criteria = new LinkedHashMap<>();

        String whenTrue = inputParameters.getString(WHEN_TRUE);

        if (whenTrue != null && !whenTrue.isBlank()) {
            criteria.put("true", whenTrue);
        }

        String whenFalse = inputParameters.getString(WHEN_FALSE);

        if (whenFalse != null && !whenFalse.isBlank()) {
            criteria.put("false", whenFalse);
        }

        if (!criteria.isEmpty()) {
            question.put(CRITERIA, criteria);
        }

        Map<String, Object> answer = TypeSafeUtils.askQuestion(inputParameters, question, context);

        double threshold = inputParameters.getDouble(THRESHOLD, 0.5);

        answer.put(IS_TRUE, answer.get(NOUL) instanceof Number value && value.doubleValue() >= threshold);

        return answer;
    }
}
