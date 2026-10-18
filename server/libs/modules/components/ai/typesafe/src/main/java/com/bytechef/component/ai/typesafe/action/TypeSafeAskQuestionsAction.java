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

import static com.bytechef.component.ai.typesafe.constant.TypeSafeConstants.ANSWERS;
import static com.bytechef.component.ai.typesafe.constant.TypeSafeConstants.ASK_QUESTIONS;
import static com.bytechef.component.ai.typesafe.constant.TypeSafeConstants.CHOICE;
import static com.bytechef.component.ai.typesafe.constant.TypeSafeConstants.DESCRIPTION;
import static com.bytechef.component.ai.typesafe.constant.TypeSafeConstants.INSTRUCTIONS;
import static com.bytechef.component.ai.typesafe.constant.TypeSafeConstants.LABEL;
import static com.bytechef.component.ai.typesafe.constant.TypeSafeConstants.LEVELS;
import static com.bytechef.component.ai.typesafe.constant.TypeSafeConstants.MODEL;
import static com.bytechef.component.ai.typesafe.constant.TypeSafeConstants.MODEL_OUTPUT_PROPERTY;
import static com.bytechef.component.ai.typesafe.constant.TypeSafeConstants.MODEL_PROPERTY;
import static com.bytechef.component.ai.typesafe.constant.TypeSafeConstants.NAME;
import static com.bytechef.component.ai.typesafe.constant.TypeSafeConstants.NOUL;
import static com.bytechef.component.ai.typesafe.constant.TypeSafeConstants.OPTIONS;
import static com.bytechef.component.ai.typesafe.constant.TypeSafeConstants.QUESTIONS;
import static com.bytechef.component.ai.typesafe.constant.TypeSafeConstants.SCORE;
import static com.bytechef.component.ai.typesafe.constant.TypeSafeConstants.STATE_PROPERTY;
import static com.bytechef.component.ai.typesafe.constant.TypeSafeConstants.THRESHOLD;
import static com.bytechef.component.ai.typesafe.constant.TypeSafeConstants.TYPE;
import static com.bytechef.component.ai.typesafe.constant.TypeSafeConstants.USAGE;
import static com.bytechef.component.ai.typesafe.constant.TypeSafeConstants.USAGE_OUTPUT_PROPERTY;
import static com.bytechef.component.ai.typesafe.constant.TypeSafeConstants.WHEN_FALSE;
import static com.bytechef.component.ai.typesafe.constant.TypeSafeConstants.WHEN_TRUE;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
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
 * Asks several named questions of any type against one state in a single request. Jev answers every question from the
 * same read of the state, so several narrow questions cost about as much as one.
 *
 * @author Ivica Cardic
 */
public class TypeSafeAskQuestionsAction {

    private static final String NOUL_CONDITION = "%s[index].%s == '%s'".formatted(QUESTIONS, TYPE, NOUL);
    private static final String CHOICE_CONDITION = "%s[index].%s == '%s'".formatted(QUESTIONS, TYPE, CHOICE);
    private static final String SCORE_CONDITION = "%s[index].%s == '%s'".formatted(QUESTIONS, TYPE, SCORE);

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(ASK_QUESTIONS)
        .title("Ask Questions")
        .description(
            "Ask several named questions about the state in one request, each a Noul, a Choice or a Score. Jev " +
                "answers them all from the same read of the state, so several narrow questions cost about as much " +
                "as one and are cheaper and faster than separate actions.")
        .properties(
            MODEL_PROPERTY,
            STATE_PROPERTY,
            array(QUESTIONS)
                .label("Questions")
                .description("The questions to ask. Each answer is returned under the question's name.")
                .items(
                    object()
                        .properties(
                            string(NAME)
                                .label("Name")
                                .description(
                                    "The key the answer is returned under, e.g. `is_urgent`. Names must be unique.")
                                .required(true),
                            string(TYPE)
                                .label("Type")
                                .description("The kind of answer the question expects.")
                                .options(
                                    option("Noul", NOUL, "A yes/no question answered with a truth value."),
                                    option("Choice", CHOICE, "Pick one label from a fixed set of options."),
                                    option("Score", SCORE, "Place the state on an ordered rubric."))
                                .defaultValue(NOUL)
                                .required(true),
                            string(INSTRUCTIONS)
                                .label("Instructions")
                                .description("The question itself, e.g. \"Does this message convey urgency?\"")
                                .controlType(TEXT_AREA)
                                .required(true),
                            string(WHEN_TRUE)
                                .label("When True")
                                .description("What a true answer means, written as a statement about the state.")
                                .controlType(TEXT_AREA)
                                .displayCondition(NOUL_CONDITION)
                                .required(false),
                            string(WHEN_FALSE)
                                .label("When False")
                                .description("What a false answer means, written as a statement about the state.")
                                .controlType(TEXT_AREA)
                                .displayCondition(NOUL_CONDITION)
                                .required(false),
                            number(THRESHOLD)
                                .label("Threshold")
                                .description("The truth value at or above which Is True is reported as true.")
                                .defaultValue(0.5)
                                .minValue(0)
                                .maxValue(1)
                                .displayCondition(NOUL_CONDITION)
                                .required(false),
                            array(OPTIONS)
                                .label("Options")
                                .description("The labels to choose from. Labels must be unique.")
                                .items(
                                    object()
                                        .properties(
                                            string(LABEL)
                                                .label("Label")
                                                .description("The value returned when this option is chosen.")
                                                .required(true),
                                            string(DESCRIPTION)
                                                .label("Description")
                                                .description("What this option covers.")
                                                .required(false)))
                                .minItems(2)
                                .displayCondition(CHOICE_CONDITION)
                                .required(true),
                            array(LEVELS)
                                .label("Levels")
                                .description("The rubric, ordered from the lowest level (0) to the highest.")
                                .items(string())
                                .minItems(2)
                                .displayCondition(SCORE_CONDITION)
                                .required(true)))
                .minItems(1)
                .required(true))
        .output(
            outputSchema(
                object()
                    .properties(
                        object(ANSWERS)
                            .description(
                                "The answer to every question, keyed by its name. Each answer has the shape the " +
                                    "matching Noul, Choice or Score action returns, without model and usage.")
                            .additionalProperties(object()),
                        MODEL_OUTPUT_PROPERTY,
                        USAGE_OUTPUT_PROPERTY)))
        .help("", "https://docs.bytechef.io/reference/components/typesafe_v1#ask-questions")
        .perform(TypeSafeAskQuestionsAction::perform);

    private TypeSafeAskQuestionsAction() {
    }

    public static Map<String, Object> perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        List<Map<String, Object>> questionParameters = inputParameters.getRequiredList(
            QUESTIONS, new TypeReference<>() {});

        if (questionParameters.isEmpty()) {
            throw new IllegalArgumentException("Ask at least one question");
        }

        Map<String, Map<String, Object>> questions = new LinkedHashMap<>();

        for (Map<String, Object> questionParameter : questionParameters) {
            String name = getRequiredString(questionParameter, NAME, "Every question needs a name");

            if (questions.containsKey(name)) {
                throw new IllegalArgumentException("Question names must be unique, found '%s' twice".formatted(name));
            }

            questions.put(name, createQuestion(questionParameter));
        }

        Map<String, Object> responseBody = TypeSafeUtils.askQuestions(inputParameters, questions, context);

        Map<String, Object> answers = new LinkedHashMap<>();

        for (Map<String, Object> questionParameter : questionParameters) {
            String name = (String) questionParameter.get(NAME);

            answers.put(name, completeAnswer(questionParameter, TypeSafeUtils.getAnswer(responseBody, name)));
        }

        Map<String, Object> result = new LinkedHashMap<>();

        result.put(ANSWERS, answers);
        result.put(MODEL, responseBody.get(MODEL));
        result.put(USAGE, responseBody.get(USAGE));

        return result;
    }

    private static Map<String, Object> createQuestion(Map<String, Object> questionParameter) {
        String instructions = getRequiredString(questionParameter, INSTRUCTIONS, "Every question needs instructions");

        return switch (getType(questionParameter)) {
            case CHOICE -> TypeSafeChoiceAction.createQuestion(
                instructions, getList(questionParameter, OPTIONS));
            case SCORE -> TypeSafeScoreAction.createQuestion(instructions, getList(questionParameter, LEVELS));
            default -> TypeSafeNoulAction.createQuestion(
                instructions, (String) questionParameter.get(WHEN_TRUE), (String) questionParameter.get(WHEN_FALSE));
        };
    }

    /**
     * Adds what the single-question actions derive locally: Is True for a Noul and the nearest level for a Score.
     */
    private static Map<String, Object>
        completeAnswer(Map<String, Object> questionParameter, Map<String, Object> answer) {
        switch (getType(questionParameter)) {
            case NOUL -> TypeSafeNoulAction.putIsTrue(
                answer, questionParameter.get(THRESHOLD) instanceof Number threshold ? threshold.doubleValue() : 0.5);
            case SCORE -> TypeSafeScoreAction.putNearestLevel(answer, getList(questionParameter, LEVELS));
            default -> {
            }
        }

        return answer;
    }

    @SuppressWarnings("unchecked")
    private static <T> List<T> getList(Map<String, Object> questionParameter, String key) {
        if (!(questionParameter.get(key) instanceof List<?> list)) {
            throw new IllegalArgumentException(
                "Question '%s' needs %s".formatted(questionParameter.get(NAME), key));
        }

        return (List<T>) list;
    }

    private static String getRequiredString(Map<String, Object> questionParameter, String key, String message) {
        if (!(questionParameter.get(key) instanceof String value) || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }

        return value;
    }

    private static String getType(Map<String, Object> questionParameter) {
        Object type = questionParameter.get(TYPE);

        if (type == null) {
            return NOUL;
        }

        return switch (String.valueOf(type)) {
            case NOUL, CHOICE, SCORE -> String.valueOf(type);
            default -> throw new IllegalArgumentException(
                "Question '%s' has unknown type '%s'".formatted(questionParameter.get(NAME), type));
        };
    }
}
