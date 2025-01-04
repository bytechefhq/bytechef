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

package com.bytechef.component.ai.text.analysis.action;

import static com.bytechef.component.ai.llm.constant.LLMConstants.MAX_TOKENS_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.MODEL;
import static com.bytechef.component.ai.llm.constant.LLMConstants.TEMPERATURE_PROPERTY;
import static com.bytechef.component.ai.text.analysis.constant.AiTextAnalysisConstants.MODEL_NO_OPTIONS_PROPERTY;
import static com.bytechef.component.ai.text.analysis.constant.AiTextAnalysisConstants.MODEL_OPTIONS_PROPERTY;
import static com.bytechef.component.ai.text.analysis.constant.AiTextAnalysisConstants.MODEL_PROVIDER_PROPERTY;
import static com.bytechef.component.ai.text.analysis.constant.AiTextAnalysisConstants.MODEL_URL_PROPERTY;
import static com.bytechef.component.ai.text.analysis.constant.AiTextAnalysisConstants.TEXT;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.ai.text.analysis.action.definition.AiTextAnalysisActionDefinition;
import com.bytechef.component.ai.text.analysis.constant.AiTextAnalysisConstants;
import com.bytechef.component.definition.Parameters;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.component.definition.ParametersFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SentimentAnalysisAction implements AiTextAnalysisAction {
    public final AiTextAnalysisActionDefinition actionDefinition;

    public SentimentAnalysisAction(ApplicationProperties.Ai.Component component) {
        this.actionDefinition = getActionDefinition(component);
    }

    private AiTextAnalysisActionDefinition getActionDefinition(ApplicationProperties.Ai.Component component) {
        return new AiTextAnalysisActionDefinition(
            action(AiTextAnalysisConstants.SENTIMENT_ANALYSIS)
                .title("Sentiment Analysis")
                .description("The sentiment of the text is typically categorized as POSITIVE, NEGATIVE, or NEUTRAL.")
                .properties(
                    MODEL_PROVIDER_PROPERTY,
                    MODEL_OPTIONS_PROPERTY,
                    MODEL_NO_OPTIONS_PROPERTY,
                    MODEL_URL_PROPERTY,
                    string(TEXT)
                        .label("Text")
                        .description("The text that is to be classified.")
                        .required(true),
                    MAX_TOKENS_PROPERTY,
                    TEMPERATURE_PROPERTY)
                .output(),
            component, this);
    }

    public Parameters createParameters(Parameters inputParameters) {
        Map<String, Object> modelInputParametersMap = new HashMap<>();

        String systemPrompt =
            "You are a sentiment analysis specialist. I will give you some text and you will only reply with 'POSITIVE', 'NEGATIVE' or 'NEUTRAL'.";

        String userBuilder = "Text: " + inputParameters.getString(TEXT);

        modelInputParametersMap.put(
            "messages",
            List.of(Map.of("content", systemPrompt, "role", "system"), Map.of("content", userBuilder, "role", "user")));
        modelInputParametersMap.put("model", inputParameters.getString(MODEL));

        return ParametersFactory.createParameters(modelInputParametersMap);
    }
}
