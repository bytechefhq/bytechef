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
import static com.bytechef.component.ai.text.analysis.constant.AiTextAnalysisConstants.FORMAT;
import static com.bytechef.component.ai.text.analysis.constant.AiTextAnalysisConstants.MODEL_NO_OPTIONS_PROPERTY;
import static com.bytechef.component.ai.text.analysis.constant.AiTextAnalysisConstants.MODEL_OPTIONS_PROPERTY;
import static com.bytechef.component.ai.text.analysis.constant.AiTextAnalysisConstants.MODEL_PROVIDER_PROPERTY;
import static com.bytechef.component.ai.text.analysis.constant.AiTextAnalysisConstants.MODEL_URL_PROPERTY;
import static com.bytechef.component.ai.text.analysis.constant.AiTextAnalysisConstants.PROMPT;
import static com.bytechef.component.ai.text.analysis.constant.AiTextAnalysisConstants.TEXT;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.ai.text.analysis.action.definition.AiTextAnalysisActionDefinition;
import com.bytechef.component.ai.text.analysis.constant.AiTextAnalysisConstants;
import com.bytechef.component.definition.Parameters;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.component.definition.ParametersFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Marko Kriskovic
 */
public class SummarizeTextAction implements AITextAnalysisAction {

    public final AiTextAnalysisActionDefinition actionDefinition;

    public SummarizeTextAction(ApplicationProperties.Ai.Component component) {
        this.actionDefinition = new AiTextAnalysisActionDefinition(
            action(AiTextAnalysisConstants.SUMMARIZE_TEXT)
                .title("Summarize Text")
                .description("AI reads, analyzes and summarizes your text into a shorter format.")
                .properties(
                    MODEL_PROVIDER_PROPERTY,
                    MODEL_OPTIONS_PROPERTY,
                    MODEL_NO_OPTIONS_PROPERTY,
                    MODEL_URL_PROPERTY,
                    string(TEXT)
                        .label("Text")
                        .description("The text that is to be summarized.")
                        .minLength(100)
                        .required(true),
                    integer(FORMAT)
                        .label("Format")
                        .description("In what format do you wish the text summarized?")
                        .options(
                            option("A structured summary with sections", 0),
                            option("A brief title summarizing the content in 4-7 words", 1),
                            option("A single, concise sentence", 2),
                            option("A bulleted list recap", 3),
                            option("Custom Prompt", 4))
                        .required(true),
                    string(PROMPT)
                        .label("Custom Prompt")
                        .description("Write your prompt for summarizing text.")
                        .displayCondition("format == 4")
                        .required(true),
                    MAX_TOKENS_PROPERTY,
                    TEMPERATURE_PROPERTY)
                .output(),
            component, this);
    }

    public Parameters createParameters(Parameters inputParameters) {
        Map<String, Object> modelInputParametersMap = new HashMap<>();

        String prompt = switch (inputParameters.getRequiredInteger(FORMAT)) {
            case 0 -> "You will receive a text. Make a structured summary of that text with sections.";
            case 1 -> "You will receive a text. Make a brief title summarizing the content in 4-7 words.";
            case 2 -> "You will receive a text. Summarize it in a single, concise sentence.";
            case 3 -> "You will receive a text. Create a bullet list recap.";
            case 4 -> "You will receive a text." + inputParameters.getString("prompt");
            default -> throw new IllegalArgumentException("Invalid format");
        };

        modelInputParametersMap.put("messages",
            List.of(
                Map.of("content", prompt, "role", "system"),
                Map.of("content", inputParameters.getString(TEXT), "role", "user")));
        modelInputParametersMap.put("model", inputParameters.getString(MODEL));

        return ParametersFactory.createParameters(modelInputParametersMap);
    }
}
