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

package com.bytechef.component.ai.universal.text.action;

import static com.bytechef.component.ai.llm.ChatModel.Role.SYSTEM;
import static com.bytechef.component.ai.llm.ChatModel.Role.USER;
import static com.bytechef.component.ai.llm.constant.LLMConstants.MAX_TOKENS_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.MODEL;
import static com.bytechef.component.ai.llm.constant.LLMConstants.ROLE;
import static com.bytechef.component.ai.llm.constant.LLMConstants.TEMPERATURE_PROPERTY;
import static com.bytechef.component.ai.universal.text.action.SummarizeTextAction.SummarizeFormat.BRIEF_TITLE;
import static com.bytechef.component.ai.universal.text.action.SummarizeTextAction.SummarizeFormat.BULLETED_LIST;
import static com.bytechef.component.ai.universal.text.action.SummarizeTextAction.SummarizeFormat.CONCISE_SENTENCE;
import static com.bytechef.component.ai.universal.text.action.SummarizeTextAction.SummarizeFormat.CUSTOM_PROMPT;
import static com.bytechef.component.ai.universal.text.action.SummarizeTextAction.SummarizeFormat.STRUCTURED_SUMMARY;
import static com.bytechef.component.ai.universal.text.constant.AiTextConstants.FORMAT;
import static com.bytechef.component.ai.universal.text.constant.AiTextConstants.MODEL_NO_OPTIONS_PROPERTY;
import static com.bytechef.component.ai.universal.text.constant.AiTextConstants.MODEL_OPTIONS_PROPERTY;
import static com.bytechef.component.ai.universal.text.constant.AiTextConstants.MODEL_URL_PROPERTY;
import static com.bytechef.component.ai.universal.text.constant.AiTextConstants.PROMPT;
import static com.bytechef.component.ai.universal.text.constant.AiTextConstants.PROVIDER_PROPERTY;
import static com.bytechef.component.ai.universal.text.constant.AiTextConstants.TEXT;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.sampleOutput;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.ai.universal.text.action.definition.AiTextActionDefinition;
import com.bytechef.component.ai.universal.text.constant.AiTextConstants;
import com.bytechef.component.definition.Parameters;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.configuration.service.PropertyService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Marko Kriskovic
 */
public class SummarizeTextAction implements AiTextAction {

    public enum SummarizeFormat {
        STRUCTURED_SUMMARY,
        BRIEF_TITLE,
        CONCISE_SENTENCE,
        BULLETED_LIST,
        CUSTOM_PROMPT;
    }

    public static AiTextActionDefinition of(
        ApplicationProperties.Ai.Provider provider, PropertyService propertyService) {

        return new AiTextActionDefinition(
            action(AiTextConstants.SUMMARIZE_TEXT)
                .title("Summarize Text")
                .description("AI reads, analyzes and summarizes your text into a shorter format.")
                .properties(
                    PROVIDER_PROPERTY.apply(provider, propertyService),
                    MODEL_OPTIONS_PROPERTY,
                    MODEL_NO_OPTIONS_PROPERTY,
                    MODEL_URL_PROPERTY,
                    string(TEXT)
                        .label("Text")
                        .description("The text that is to be summarized.")
                        .minLength(100)
                        .required(true),
                    string(FORMAT)
                        .label("Format")
                        .description("In what format do you wish the text summarized?")
                        .options(
                            option("A structured summary with sections", STRUCTURED_SUMMARY.name()),
                            option("A brief title summarizing the content in 4-7 words", BRIEF_TITLE.name()),
                            option("A single, concise sentence", CONCISE_SENTENCE.name()),
                            option("A bulleted list recap", BULLETED_LIST.name()),
                            option("Custom Prompt", CUSTOM_PROMPT.name()))
                        .required(true),
                    string(PROMPT)
                        .label("Custom Prompt")
                        .description("Write your prompt for summarizing text.")
                        .displayCondition("format == '%s'".formatted(CUSTOM_PROMPT))
                        .required(true),
                    MAX_TOKENS_PROPERTY,
                    TEMPERATURE_PROPERTY)
                .output(
                    outputSchema(
                        string()
                            .description("The summarized text.")),
                    sampleOutput("sample summarized text")),
            provider, new SummarizeTextAction(), propertyService);
    }

    private SummarizeTextAction() {
    }

    @Override

    public Parameters createParameters(Parameters inputParameters) {
        Map<String, Object> modelInputParametersMap = new HashMap<>();

        String prompt = switch (inputParameters.getRequired(FORMAT, SummarizeFormat.class)) {
            case STRUCTURED_SUMMARY -> "You will receive a text. Make a structured summary of that text with sections.";
            case BRIEF_TITLE -> "You will receive a text. Make a brief title summarizing the content in 4-7 words.";
            case CONCISE_SENTENCE -> "You will receive a text. Summarize it in a single, concise sentence.";
            case BULLETED_LIST -> "You will receive a text. Create a bullet list recap.";
            case CUSTOM_PROMPT -> "You will receive a text." + inputParameters.getString("prompt");
        };

        modelInputParametersMap.put(
            "messages",
            List.of(
                Map.of("content", prompt, ROLE, SYSTEM.name()),
                Map.of("content", inputParameters.getString(TEXT), ROLE, USER.name())));
        modelInputParametersMap.put("model", inputParameters.getString(MODEL));

        return ParametersFactory.createParameters(modelInputParametersMap);
    }
}
