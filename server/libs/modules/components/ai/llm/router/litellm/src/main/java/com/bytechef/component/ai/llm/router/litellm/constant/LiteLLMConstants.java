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

package com.bytechef.component.ai.llm.router.litellm.constant;

import static com.bytechef.component.ai.llm.constant.LLMConstants.MODEL;
import static com.bytechef.component.ai.llm.constant.LLMConstants.REASONING;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.ai.llm.ChatModel;
import com.bytechef.component.definition.ComponentDsl.ModifiableStringProperty;

/**
 * @author Aarish Yadav
 */
public class LiteLLMConstants {

    public static final String BASE_URL = "baseUrl";
    public static final String DEFAULT_BASE_URL = "http://localhost:4000/v1";

    public static final ModifiableStringProperty CHAT_MODEL_PROPERTY = string(MODEL)
        .label("Model")
        .description(
            "ID of the model to use. The available models depend on your LiteLLM proxy configuration.")
        .options(
            option("gpt-4o", "gpt-4o"),
            option("gpt-4o-mini", "gpt-4o-mini"),
            option("gpt-4.1", "gpt-4.1"),
            option("gpt-4.1-mini", "gpt-4.1-mini"),
            option("gpt-4.1-nano", "gpt-4.1-nano"),
            option("o3", "o3"),
            option("o3-mini", "o3-mini"),
            option("o4-mini", "o4-mini"),
            option("claude-sonnet-4-20250514", "claude-sonnet-4-20250514"),
            option("claude-3-5-sonnet-20241022", "claude-3-5-sonnet-20241022"),
            option("claude-3-5-haiku-20241022", "claude-3-5-haiku-20241022"),
            option("gemini-2.5-flash", "gemini-2.5-flash"),
            option("gemini-2.5-pro", "gemini-2.5-pro"),
            option("gemini-2.0-flash", "gemini-2.0-flash"),
            option("deepseek-chat", "deepseek-chat"),
            option("deepseek-reasoner", "deepseek-reasoner"),
            option("mistral-large-latest", "mistral-large-latest"),
            option("codestral-latest", "codestral-latest"))
        .required(true);

    public static final ModifiableStringProperty EMBEDDING_MODEL_PROPERTY = string(MODEL)
        .label("Model")
        .description(
            "ID of the embedding model to use. The available models depend on your LiteLLM proxy configuration.")
        .options(
            option("text-embedding-3-small", "text-embedding-3-small"),
            option("text-embedding-3-large", "text-embedding-3-large"),
            option("text-embedding-ada-002", "text-embedding-ada-002"))
        .required(true);

    public static final ModifiableStringProperty REASONING_PROPERTY = string(REASONING)
        .label("Reasoning effort")
        .description(
            "Constrains effort on reasoning. Reducing reasoning effort can result in faster responses and fewer " +
                "tokens used on reasoning in a response.")
        .options(
            option("none", "none"),
            option("low", "low"),
            option("medium", "medium"),
            option("high", "high"))
        .displayCondition("response.responseFormat == '%s'".formatted(ChatModel.ResponseFormat.TEXT.name()))
        .advancedOption(true);

    private LiteLLMConstants() {
    }
}
