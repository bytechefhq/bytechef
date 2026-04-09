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

package com.bytechef.component.ai.universal.text.constant;

import static com.bytechef.component.ai.llm.Provider.ANTHROPIC;
import static com.bytechef.component.ai.llm.Provider.AZURE_OPEN_AI;
import static com.bytechef.component.ai.llm.Provider.DEEPSEEK;
import static com.bytechef.component.ai.llm.Provider.GROQ;
import static com.bytechef.component.ai.llm.Provider.MISTRAL;
import static com.bytechef.component.ai.llm.Provider.NVIDIA;
import static com.bytechef.component.ai.llm.Provider.OPEN_AI;
import static com.bytechef.component.ai.llm.Provider.PERPLEXITY;
import static com.bytechef.component.ai.llm.Provider.VERTEX_GEMINI;
import static com.bytechef.component.ai.llm.constant.LLMConstants.MODEL;
import static com.bytechef.component.ai.llm.constant.LLMConstants.PROVIDER;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.config.ApplicationProperties.Ai;

import com.bytechef.component.ai.universal.text.util.AiTextUtils;
import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableStringProperty;
import com.bytechef.platform.component.definition.ActionContextAware;
import com.bytechef.platform.configuration.service.PropertyService;
import java.util.function.BiFunction;

/**
 * @author Marko Krišković
 */
public class AiTextConstants {

    public static final String TEXT = "text";
    public static final String QUERY = "query";
    public static final String FORMAT = "format";
    public static final String PROMPT = "prompt";
    public static final String CATEGORIES = "categories";
    public static final String CRITERIA = "criteria";
    public static final String EXAMPLES = "examples";
    public static final String CRITERION = "criterion";
    public static final String LOWEST_SCORE = "lowestScore";
    public static final String HIGHEST_SCORE = "highestScore";
    public static final String IS_DECIMAL = "isDecimal";
    public static final String NUM_RESULTS = "numResults";
    public static final String CHUNK_SIZE = "chunkSize";
    public static final String DATA_FIELDS = "dataFields";
    public static final String FIELD_NAME = "fieldName";
    public static final String FIELD_TYPE = "fieldType";
    public static final String FIELD_DESCRIPTION = "fieldDescription";
    public static final String EXTRACT_LIST = "extractList";
    public static final String ADDITIONAL_CONTEXT = "additionalContext";
    public static final String SENSITIVE_KEYWORDS = "sensitiveKeywords";
    public static final String PII_DETECTION = "piiDetection";
    public static final String CUSTOM_PATTERNS = "customRegexPatterns";
    public static final String MODE = "mode";
    public static final String MODE_CLASSIFY = "classify";
    public static final String MODE_SANITIZE = "sanitize";
    public static final String BLOCKED_MESSAGE = "blockedMessage";
    public static final String MASK = "mask";
    public static final String SUMMARIZE_TEXT = "summarizeText";
    public static final String TEXT_GENERATION = "textGeneration";
    public static final String SIMILARITY_SEARCH = "similaritySearch";
    public static final String CLASSIFY_TEXT = "classifyText";
    public static final String SENTIMENT_ANALYSIS = "sentimentAnalysis";
    public static final String EXTRACT_DATA = "extractData";
    public static final String SCORE = "score";

    public static final BiFunction<Ai.Provider, PropertyService, ModifiableStringProperty> PROVIDER_PROPERTY =
        (aiProvider, propertyService) -> string(PROVIDER)
            .label("Provider")
            .options(
                (OptionsFunction<String>) (
                    inputParameters, connectionParameters, lookupDependsOnPaths, searchText, context) -> AiTextUtils
                        .getProviderOptions(
                            aiProvider, propertyService, ((ActionContextAware) context).getEnvironmentId()))
            .required(true);

    public static final ModifiableStringProperty MODEL_OPTIONS_PROPERTY = string(MODEL)
        .label("Model")
        .description("The model name to use.")
        .options((OptionsFunction<String>) AiTextUtils::getModelOptions)
        .optionsLookupDependsOn(PROVIDER)
        .displayCondition(
            "contains({'%s','%s','%s','%s'}, provider)".formatted(
                ANTHROPIC, MISTRAL, OPEN_AI, VERTEX_GEMINI))
        .required(true);

    public static final ModifiableStringProperty MODEL_NO_OPTIONS_PROPERTY = string(MODEL)
        .label("Model")
        .description("ID of the model to use.")
        .displayCondition("contains({'%s','%s','%s','%s','%s'}, provider)".formatted(AZURE_OPEN_AI, GROQ, NVIDIA,
            DEEPSEEK, PERPLEXITY))
        .required(true);

    public static final ModifiableStringProperty MODEL_URL_PROPERTY = string(MODEL)
        .label("URL")
        .description("Url of the inference endpoint.")
        .displayCondition("provider == '%s'".formatted(VERTEX_GEMINI))
        .required(true);

    private AiTextConstants() {
    }
}
