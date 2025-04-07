/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.component.ai.llm.watsonx.constant;

import static com.bytechef.component.ai.llm.constant.LLMConstants.MODEL;
import static com.bytechef.component.definition.ComponentDsl.ModifiableIntegerProperty;
import static com.bytechef.component.definition.ComponentDsl.ModifiableNumberProperty;
import static com.bytechef.component.definition.ComponentDsl.ModifiableStringProperty;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.string;

/**
 * @author Monika Kušter
 * @author Marko Kriskovic
 */
public final class WatsonxConstants {

    public static final String DECODING_METHOD = "decodingMethod";
    public static final String MIN_TOKENS = "minTokens";
    public static final String PROJECT_ID = "projectId";
    public static final String REPETITION_PENALTY = "repetitionPenalty";
    public static final String STREAM_ENDPOINT = "streamEndpoint";
    public static final String TEXT_ENDPOINT = "textEndpoint";

    public static final ModifiableStringProperty CHAT_MODEL_PROPERTY = string(MODEL)
        .label("Model")
        .description("Model is the identifier of the LLM Model to be used.")
        .exampleValue("google/flan-ul2")
        .required(false);

    public static final ModifiableStringProperty DECODING_METHOD_PROPERTY = string(DECODING_METHOD)
        .label("Decoding Method")
        .description("Decoding is the process that a model uses to choose the tokens in the generated output.")
        .exampleValue("greedy")
        .advancedOption(true);

    public static final ModifiableIntegerProperty MIN_TOKENS_PROPERTY = integer(MIN_TOKENS)
        .label("Min Tokens")
        .description("Sets how many tokens must the LLM generate.")
        .advancedOption(true);

    public static final ModifiableNumberProperty REPETITION_PENALTY_PROPERTY = number(REPETITION_PENALTY)
        .label("Repetition Penalty")
        .description(
            "Sets how strongly to penalize repetitions. A higher value (e.g., 1.8) will penalize repetitions more " +
                "strongly, while a lower value (e.g., 1.1) will be more lenient.")
        .advancedOption(true);

    private WatsonxConstants() {
    }
}
