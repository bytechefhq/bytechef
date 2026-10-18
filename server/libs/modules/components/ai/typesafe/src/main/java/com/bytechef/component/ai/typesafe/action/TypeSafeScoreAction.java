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

import static com.bytechef.component.ai.typesafe.constant.TypeSafeConstants.CONFIDENCE;
import static com.bytechef.component.ai.typesafe.constant.TypeSafeConstants.CRITERIA;
import static com.bytechef.component.ai.typesafe.constant.TypeSafeConstants.INSTRUCTIONS;
import static com.bytechef.component.ai.typesafe.constant.TypeSafeConstants.LEGEND;
import static com.bytechef.component.ai.typesafe.constant.TypeSafeConstants.LEVELS;
import static com.bytechef.component.ai.typesafe.constant.TypeSafeConstants.MODEL_OUTPUT_PROPERTY;
import static com.bytechef.component.ai.typesafe.constant.TypeSafeConstants.MODEL_PROPERTY;
import static com.bytechef.component.ai.typesafe.constant.TypeSafeConstants.NEAREST_LABEL;
import static com.bytechef.component.ai.typesafe.constant.TypeSafeConstants.NEAREST_LEVEL;
import static com.bytechef.component.ai.typesafe.constant.TypeSafeConstants.PROBABILITIES;
import static com.bytechef.component.ai.typesafe.constant.TypeSafeConstants.SCORE;
import static com.bytechef.component.ai.typesafe.constant.TypeSafeConstants.STATE_PROPERTY;
import static com.bytechef.component.ai.typesafe.constant.TypeSafeConstants.TYPE;
import static com.bytechef.component.ai.typesafe.constant.TypeSafeConstants.USAGE_OUTPUT_PROPERTY;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Property.ControlType.TEXT_AREA;

import com.bytechef.component.ai.typesafe.util.TypeSafeUtils;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Places the state on an ordered rubric; the score is continuous and probability-weighted, not a rounded level.
 *
 * @author Ivica Cardic
 */
public class TypeSafeScoreAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(SCORE)
        .title("Score")
        .description(
            "Place the state on an ordered rubric. The score is continuous and probability-weighted: 1.1 on a " +
                "three-level rubric sits just past level 1, leaning toward level 2, so a threshold such as 2.0 is " +
                "a real threshold rather than a rounding artefact.")
        .properties(
            MODEL_PROPERTY,
            STATE_PROPERTY,
            string(INSTRUCTIONS)
                .label("Instructions")
                .description("The question the rubric answers, e.g. \"How frustrated is the customer?\"")
                .controlType(TEXT_AREA)
                .required(true),
            array(LEVELS)
                .label("Levels")
                .description(
                    "The rubric, ordered from the lowest level (0) to the highest. Keep the levels genuine degrees " +
                        "of one thing; if they are alternatives, use Choice instead.")
                .items(string())
                .minItems(2)
                .required(true))
        .output(
            outputSchema(
                object()
                    .properties(
                        string(TYPE)
                            .description("The answer type, always `score`."),
                        number(SCORE)
                            .description("The probability-weighted level, between 0 and the highest level index."),
                        object(LEGEND)
                            .description("The description of every level, keyed by level index.")
                            .additionalProperties(string()),
                        object(PROBABILITIES)
                            .description("The probability of every level, keyed by level index.")
                            .additionalProperties(number()),
                        number(CONFIDENCE)
                            .description("How concentrated the probabilities are, 0 to 1."),
                        integer(NEAREST_LEVEL)
                            .description("The index of the most probable level."),
                        string(NEAREST_LABEL)
                            .description("The description of the most probable level."),
                        MODEL_OUTPUT_PROPERTY,
                        USAGE_OUTPUT_PROPERTY)))
        .help("", "https://docs.bytechef.io/reference/components/typesafe_v1#score")
        .perform(TypeSafeScoreAction::perform);

    private TypeSafeScoreAction() {
    }

    public static Map<String, Object> perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        List<String> levels = inputParameters.getRequiredList(LEVELS, new TypeReference<>() {});

        if (levels.size() < 2) {
            throw new IllegalArgumentException("A score needs at least two levels");
        }

        Map<String, Object> question = new LinkedHashMap<>();

        question.put(TYPE, SCORE);
        question.put(INSTRUCTIONS, inputParameters.getRequiredString(INSTRUCTIONS));
        question.put(CRITERIA, levels);

        Map<String, Object> answer = TypeSafeUtils.askQuestion(inputParameters, question, context);

        int nearestLevel = getNearestLevel(answer.get(PROBABILITIES));

        answer.put(NEAREST_LEVEL, nearestLevel);
        answer.put(NEAREST_LABEL, nearestLevel >= 0 && nearestLevel < levels.size() ? levels.get(nearestLevel) : null);

        return answer;
    }

    /**
     * The most probable level, read off the probabilities map whose JSON keys are the level indexes as strings.
     */
    private static int getNearestLevel(Object probabilities) {
        int nearestLevel = -1;

        if (!(probabilities instanceof Map<?, ?> probabilityMap)) {
            return nearestLevel;
        }

        double highestProbability = Double.NEGATIVE_INFINITY;

        for (Map.Entry<?, ?> entry : probabilityMap.entrySet()) {
            if (!(entry.getValue() instanceof Number probability) ||
                probability.doubleValue() <= highestProbability) {

                continue;
            }

            try {
                nearestLevel = Integer.parseInt(String.valueOf(entry.getKey()));

                highestProbability = probability.doubleValue();
            } catch (NumberFormatException numberFormatException) {
                // a key that is not a level index is not a level; skip it
                continue;
            }
        }

        return nearestLevel;
    }
}
