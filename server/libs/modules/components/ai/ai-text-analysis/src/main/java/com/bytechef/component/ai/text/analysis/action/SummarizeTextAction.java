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
import static com.bytechef.component.ai.text.analysis.constant.AiTextAnalysisConstants.MODEL_PROVIDER;
import static com.bytechef.component.ai.text.analysis.constant.AiTextAnalysisConstants.PROMPT;
import static com.bytechef.component.ai.text.analysis.constant.AiTextAnalysisConstants.TEXT;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.ai.llm.amazon.bedrock.constant.AmazonBedrockConstants;
import com.bytechef.component.ai.llm.anthropic.constant.AnthropicConstants;
import com.bytechef.component.ai.llm.mistral.constant.MistralConstants;
import com.bytechef.component.ai.llm.vertex.gemini.constant.VertexGeminiConstants;
import com.bytechef.component.ai.text.analysis.action.definition.AiTextAnalysisActionDefinition;
import com.bytechef.component.ai.text.analysis.constant.AiTextAnalysisConstants;
import com.bytechef.component.openai.constant.OpenAiConstants;
import com.bytechef.config.ApplicationProperties;

/**
 * @author Marko Kriskovic
 */
public class SummarizeTextAction {

    public final AiTextAnalysisActionDefinition actionDefinition;

    public SummarizeTextAction(ApplicationProperties.Ai.Component component) {
        this.actionDefinition = new AiTextAnalysisActionDefinition(
            action(AiTextAnalysisConstants.SUMMARIZE_TEXT)
                .title("Summarize Text")
                .description("AI reads, analyzes and summarizes your text into a shorter format.")
                .properties(
                    integer(MODEL_PROVIDER)
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
                        .required(true),
                    string(MODEL)
                        .label("Model")
                        .description("ID of the model to use.")
                        .options(AmazonBedrockConstants.ANTHROPIC2_MODELS)
                        .displayCondition("modelProvider == 0")
                        .required(true),
                    string(MODEL)
                        .label("Model")
                        .description("ID of the model to use.")
                        .options(AmazonBedrockConstants.ANTHROPIC3_MODELS)
                        .displayCondition("modelProvider == 1")
                        .required(true),
                    string(MODEL)
                        .label("Model")
                        .description("ID of the model to use.")
                        .options(AmazonBedrockConstants.COHERE_MODELS)
                        .displayCondition("modelProvider == 2")
                        .required(true),
                    string(MODEL)
                        .label("Model")
                        .description("ID of the model to use.")
                        .options(AmazonBedrockConstants.JURASSIC2_MODELS)
                        .displayCondition("modelProvider == 3")
                        .required(true),
                    string(MODEL)
                        .label("Model")
                        .description("ID of the model to use.")
                        .options(AmazonBedrockConstants.LLAMA_MODELS)
                        .displayCondition("modelProvider == 4")
                        .required(true),
                    string(MODEL)
                        .label("Model")
                        .description("ID of the model to use.")
                        .options(AmazonBedrockConstants.TITAN_MODELS)
                        .displayCondition("modelProvider == 5")
                        .required(true),
                    string(MODEL)
                        .label("Model")
                        .description("ID of the model to use.")
                        .options(AnthropicConstants.MODELS)
                        .displayCondition("modelProvider == 6")
                        .required(true),
                    string(MODEL)
                        .label("Model")
                        .description("ID of the model to use.")
                        .displayCondition("modelProvider >= 7 && modelProvider <= 9")
                        .required(true),
                    string(MODEL)
                        .label("URL")
                        .description("Url of the inference endpoint.")
                        .displayCondition("modelProvider == 10")
                        .required(true),
                    string(MODEL)
                        .label("Model")
                        .description("ID of the model to use.")
                        .options(MistralConstants.MODELS)
                        .displayCondition("modelProvider == 11")
                        .required(true),
                    string(MODEL)
                        .label("Model")
                        .description("ID of the model to use.")
                        .options(OpenAiConstants.MODELS)
                        .displayCondition("modelProvider == 12")
                        .required(true),
                    string(MODEL)
                        .label("Model")
                        .description("ID of the model to use.")
                        .options(VertexGeminiConstants.MODELS)
                        .displayCondition("modelProvider == 13")
                        .required(true),
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
            component);
    }
}
