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

package com.bytechef.component.ai.llm.anthropic.constant;

import static com.bytechef.component.ai.llm.constant.LLMConstants.ATTACHMENTS_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.FORMAT_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.MAX_TOKENS;
import static com.bytechef.component.ai.llm.constant.LLMConstants.MESSAGES_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.MODEL;
import static com.bytechef.component.ai.llm.constant.LLMConstants.PROMPT_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.RESPONSE_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.STOP_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.SYSTEM_PROMPT_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.TEMPERATURE_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.TOP_K_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.TOP_P_PROPERTY;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.ai.llm.util.ModelUtils;
import com.bytechef.component.definition.ComponentDsl.ModifiableIntegerProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableStringProperty;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Property;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class AnthropicConstants {

    public static final ModifiableIntegerProperty MAX_TOKENS_PROPERTY = integer(MAX_TOKENS)
        .label("Max Tokens")
        .description("The maximum number of tokens to generate in the chat completion.")
        .required(true);

    private static final Map<String, String> MODEL_MAP = new LinkedHashMap<>();

    static {
        MODEL_MAP.put("claude-opus-4-6", "claude-opus-4-6");
        MODEL_MAP.put("claude-sonnet-4-6", "claude-sonnet-4-6");
        MODEL_MAP.put("claude-haiku-4-5", "claude-haiku-4-5");
        MODEL_MAP.put("claude-opus-4-5", "claude-opus-4-5");
        MODEL_MAP.put("claude-sonnet-4-5", "claude-sonnet-4-5");
        MODEL_MAP.put("claude-opus-4-1", "claude-opus-4-1");
        MODEL_MAP.put("claude-opus-4-0", "claude-opus-4-0");
        MODEL_MAP.put("claude-sonnet-4-0", "claude-sonnet-4-0");
        MODEL_MAP.put("claude-3-haiku-20240307", "claude-3-haiku-20240307");
    }

    public static final List<Option<String>> MODELS = ModelUtils.getEnumOptions(MODEL_MAP);

    public static final ModifiableStringProperty CHAT_MODEL_PROPERTY = string(MODEL)
        .label("Model")
        .description("ID of the model to use.")
        .options(MODELS)
        .required(true);

    public static final List<Property> ASK_PROPERTIES = List.of(
        CHAT_MODEL_PROPERTY,
        FORMAT_PROPERTY,
        PROMPT_PROPERTY,
        SYSTEM_PROMPT_PROPERTY,
        ATTACHMENTS_PROPERTY,
        MESSAGES_PROPERTY,
        MAX_TOKENS_PROPERTY,
        RESPONSE_PROPERTY,
        TEMPERATURE_PROPERTY,
        TOP_P_PROPERTY,
        TOP_K_PROPERTY,
        STOP_PROPERTY);
}
