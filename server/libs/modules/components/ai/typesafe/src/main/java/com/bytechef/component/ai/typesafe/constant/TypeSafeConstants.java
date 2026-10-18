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

package com.bytechef.component.ai.typesafe.constant;

import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Property.ControlType.TEXT_AREA;

import com.bytechef.component.ai.typesafe.util.TypeSafeUtils;
import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableObjectProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableStringProperty;
import java.util.List;

/**
 * @author Ivica Cardic
 */
public final class TypeSafeConstants {

    public static final String ANSWERS = "answers";
    public static final String CHOICE = "choice";
    public static final String CONFIDENCE = "confidence";
    public static final String CRITERIA = "criteria";
    public static final String DESCRIPTION = "description";
    public static final String INPUT_TOKENS = "input_tokens";
    public static final String INSTRUCTIONS = "instructions";
    public static final String IS_TRUE = "isTrue";
    public static final String LABEL = "label";
    public static final String LEGEND = "legend";
    public static final String LEVELS = "levels";
    public static final String MODEL = "model";
    public static final String MODELS = "models";
    public static final String NAME = "name";
    public static final String NEAREST_LABEL = "nearestLabel";
    public static final String NEAREST_LEVEL = "nearestLevel";
    public static final String NOUL = "noul";
    public static final String OPTIONS = "options";
    public static final String OUTPUT_TOKENS = "output_tokens";
    public static final String PROBABILITIES = "probabilities";
    public static final String QUESTIONS = "questions";
    public static final String SCORE = "score";
    public static final String STATE = "state";
    public static final String THRESHOLD = "threshold";
    public static final String TYPE = "type";
    public static final String USAGE = "usage";
    public static final String WHEN_FALSE = "whenFalse";
    public static final String WHEN_TRUE = "whenTrue";

    /**
     * Every action asks exactly one question, so the name it goes under on the wire is fixed.
     */
    public static final String QUESTION_NAME = "question";

    public static final String JEV_LATEST = "jev-latest";
    public static final String JEV_PREVIEW = "jev-preview";

    /**
     * Aliases the API resolves server-side. They are offered ahead of the pinned versions {@code /v1/models} lists,
     * since the listing is not guaranteed to carry them.
     */
    public static final List<String> MODEL_ALIASES = List.of(JEV_LATEST, JEV_PREVIEW);

    public static final ModifiableStringProperty MODEL_PROPERTY = string(MODEL)
        .label("Model")
        .description(
            "ID of the Jev model to use. The `jev-latest` and `jev-preview` aliases resolve server-side; pick a " +
                "pinned version to keep answers stable across model releases.")
        .options((OptionsFunction<String>) TypeSafeUtils::getModelOptions)
        .defaultValue(JEV_LATEST)
        .required(true);

    public static final ModifiableStringProperty STATE_PROPERTY = string(STATE)
        .label("State")
        .description(
            "The content the question is answered against. Text, or a JSON object or array mapped from a previous " +
                "step; name a field of an object in the instructions to point the question at it.")
        .controlType(TEXT_AREA)
        .required(true);

    public static final ModifiableObjectProperty USAGE_OUTPUT_PROPERTY = object(USAGE)
        .description("Token usage of the request.")
        .properties(
            integer(INPUT_TOKENS)
                .description("Number of input tokens."),
            integer(OUTPUT_TOKENS)
                .description("Number of output tokens."));

    public static final ModifiableStringProperty MODEL_OUTPUT_PROPERTY = string(MODEL)
        .description("The model version the request resolved to, e.g. `jev-1.13.0` for `jev-latest`.");

    private TypeSafeConstants() {
    }
}
