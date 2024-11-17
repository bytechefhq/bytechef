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

import static com.bytechef.component.ai.text.analysis.constant.AiTextAnalysisConstants.CONNECTION_PROVIDER;
import static com.bytechef.component.ai.text.analysis.constant.AiTextAnalysisConstants.FORMAT;
import static com.bytechef.component.ai.text.analysis.constant.AiTextAnalysisConstants.PROMPT;
import static com.bytechef.component.ai.text.analysis.constant.AiTextAnalysisConstants.TEXT;
import static com.bytechef.component.definition.Authorization.TOKEN;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.llm.constant.LLMConstants.MAX_TOKENS_PROPERTY;
import static com.bytechef.component.llm.constant.LLMConstants.MODEL;
import static com.bytechef.component.llm.constant.LLMConstants.TEMPERATURE_PROPERTY;

import com.bytechef.component.ai.text.analysis.AiTextAnalysisActionDefinition;
import com.bytechef.component.ai.text.analysis.AiTextAnalysisConfiguration;
import com.bytechef.component.ai.text.analysis.constant.AiTextAnalysisConstants;
import com.bytechef.component.amazon.bedrock.action.AmazonBedrockAnthropic2ChatAction;
import com.bytechef.component.amazon.bedrock.action.AmazonBedrockAnthropic3ChatAction;
import com.bytechef.component.amazon.bedrock.action.AmazonBedrockCohereChatAction;
import com.bytechef.component.amazon.bedrock.action.AmazonBedrockJurassic2ChatAction;
import com.bytechef.component.amazon.bedrock.action.AmazonBedrockLlamaChatAction;
import com.bytechef.component.amazon.bedrock.action.AmazonBedrockTitanChatAction;
import com.bytechef.component.anthropic.action.AnthropicChatAction;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.llm.Chat;
import com.bytechef.component.mistral.action.MistralChatAction;
import com.bytechef.component.openai.action.OpenAIChatAction;
import com.bytechef.component.vertex.gemini.action.VertexGeminiChatAction;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.component.definition.AbstractActionDefinitionWrapper;


/**
 * @author Marko Kriskovic
 */
public class SummarizeTextAction {

    public final AiTextAnalysisActionDefinition ACTION_DEFINITION;


    public SummarizeTextAction(ApplicationProperties.Ai.Component component) {
        this.ACTION_DEFINITION = new AiTextAnalysisActionDefinition(
            action("summarizeText")
                .title("Summarize Text")
                .description("AI reads, analyzes and summarizes your text into a shorter format.")
                .properties(
                    integer(CONNECTION_PROVIDER)
                        .label("Connection provider")
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
                            option("Vertex Gemini", 13)),
                    string(MODEL)
                        .label("Model")
                        .description("ID of the model to use.")
                        .options(AmazonBedrockAnthropic2ChatAction.MODELS_ENUM)
                        .displayCondition("connectionProvider == 0")
                        .required(true),
                    string(MODEL)
                        .label("Model")
                        .description("ID of the model to use.")
                        .options(AmazonBedrockAnthropic3ChatAction.MODELS_ENUM)
                        .displayCondition("connectionProvider == 1")
                        .required(true),
                    string(MODEL)
                        .label("Model")
                        .description("ID of the model to use.")
                        .options(AmazonBedrockCohereChatAction.MODELS_ENUM)
                        .displayCondition("connectionProvider == 2")
                        .required(true),
                    string(MODEL)
                        .label("Model")
                        .description("ID of the model to use.")
                        .options(AmazonBedrockJurassic2ChatAction.MODELS_ENUM)
                        .displayCondition("connectionProvider == 3")
                        .required(true),
                    string(MODEL)
                        .label("Model")
                        .description("ID of the model to use.")
                        .options(AmazonBedrockLlamaChatAction.MODELS_ENUM)
                        .displayCondition("connectionProvider == 4")
                        .required(true),
                    string(MODEL)
                        .label("Model")
                        .description("ID of the model to use.")
                        .options(AmazonBedrockTitanChatAction.MODELS_ENUM)
                        .displayCondition("connectionProvider == 5")
                        .required(true),
                    string(MODEL)
                        .label("Model")
                        .description("ID of the model to use.")
                        .options(AnthropicChatAction.MODELS_ENUM)
                        .displayCondition("connectionProvider == 6")
                        .required(true),
                    string(MODEL)
                        .label("Model")
                        .description("ID of the model to use.")
                        .displayCondition("connectionProvider >= 7 && connectionProvider <= 9")
                        .required(true),
                    string(MODEL)
                        .label("URL")
                        .description("Url of the inference endpoint.")
                        .displayCondition("connectionProvider == 10")
                        .required(true),
                    string(MODEL)
                        .label("Model")
                        .description("ID of the model to use.")
                        .options(MistralChatAction.MODELS_ENUM)
                        .displayCondition("connectionProvider == 11")
                        .required(true),
                    string(MODEL)
                        .label("Model")
                        .description("ID of the model to use.")
                        .options(OpenAIChatAction.MODELS_ENUM)
                        .displayCondition("connectionProvider == 12")
                        .required(true),
                    string(MODEL)
                        .label("Model")
                        .description("ID of the model to use.")
                        .options(VertexGeminiChatAction.MODELS_ENUM)
                        .displayCondition("connectionProvider == 13")
                        .required(true),
                    string(TEXT)
                        .label("Text")
                        .description("The text that is to be summarized.")
                        .minLength(100),
                    integer(FORMAT)
                        .label("Format")
                        .description("In what format do you wish the text summarized?")
                        .options(
                            option("A structured summary with sections", 0),
                            option("A brief title summarizing the content in 4-7 words", 1),
                            option("A single, concise sentence", 2),
                            option("A bulleted list recap", 3),
                            option("Custom Prompt", 4)),
                    string(PROMPT)
                        .label("Custom Prompt")
                        .description("Write your prompt for summarizing text.")
                        .displayCondition("format == 4"),
                    MAX_TOKENS_PROPERTY,
                    TEMPERATURE_PROPERTY)
                .output()
            , component);
    }

}
