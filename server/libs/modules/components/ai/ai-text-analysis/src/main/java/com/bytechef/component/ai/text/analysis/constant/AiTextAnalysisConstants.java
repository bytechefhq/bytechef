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

package com.bytechef.component.ai.text.analysis.constant;

import static com.bytechef.component.ai.llm.constant.LLMConstants.MODEL;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.ai.text.analysis.util.AiTextAnalysisUtil;
import com.bytechef.component.definition.ComponentDsl.ModifiableIntegerProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableStringProperty;
import com.bytechef.component.definition.OptionsDataSource;

/**
 * @author Marko Krišković
 */
public class AiTextAnalysisConstants {

    public static final String TEXT = "text";
    public static final String FORMAT = "format";
    public static final String PROMPT = "prompt";
    public static final String CATEGORIES = "categories";
    public static final String EXAMPLES = "examples";

    public static final String MODEL_PROVIDER = "modelProvider";
    public static final String SUMMARIZE_TEXT = "summarizeText";
    public static final String CLASSIFY_TEXT = "classifyText";

    public static final ModifiableIntegerProperty MODEL_PROVIDER_PROPERTY = integer(MODEL_PROVIDER)
        .label("Model provider")
        .options(
            option("Amazon Bedrock: Anthropic 2", 0),
            option("Amazon Bedrock: Anthropic 3", 1),
            option("Amazon Bedrock: Cohere", 2),
            option("Amazon Bedrock: Jurassic 2", 3),
            option("Amazon Bedrock: Llama", 4),
            option("Amazon Bedrock: Titan", 5),
            option("Anthropic", 6),
            option("Azure Open AI", 7),
            option("Groq", 8),
            option("NVIDIA", 9),
            option("Hugging Face", 10),
            option("Mistral", 11),
            option("Open AI", 12),
            option("Vertex Gemini", 13))
        .required(true);

    public static final ModifiableStringProperty MODEL_OPTIONS_PROPERTY = string(MODEL)
        .label("Model")
        .description("ID of the model to use.")
        .options((OptionsDataSource.ActionOptionsFunction<String>) AiTextAnalysisUtil::createModelProperties)
        .optionsLookupDependsOn(MODEL_PROVIDER)
        .displayCondition("modelProvider <= 6 || (modelProvider >= 11 && modelProvider <= 13)")
        .required(true);

    public static final ModifiableStringProperty MODEL_NO_OPTIONS_PROPERTY = string(MODEL)
        .label("Model")
        .description("ID of the model to use.")
        .displayCondition("modelProvider >= 7 && modelProvider <= 9")
        .required(true);

    public static final ModifiableStringProperty MODEL_URL_PROPERTY = string(MODEL)
        .label("URL")
        .description("Url of the inference endpoint.")
        .displayCondition("modelProvider == 10")
        .required(true);

    private AiTextAnalysisConstants() {
    }
}
