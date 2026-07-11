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
import static com.bytechef.component.ai.llm.constant.LLMConstants.TEMPERATURE;
import static com.bytechef.component.ai.llm.constant.LLMConstants.TOP_K_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.TOP_P;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.anthropic.models.messages.Model;
import com.bytechef.component.ai.llm.util.ModelUtils;
import com.bytechef.component.definition.ComponentDsl.ModifiableIntegerProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableNumberProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableStringProperty;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Property;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Ivica Cardic
 */
public class AnthropicConstants {

    public static final ModifiableIntegerProperty MAX_TOKENS_PROPERTY = integer(MAX_TOKENS)
        .label("Max Tokens")
        .description("The maximum number of tokens to generate in the chat completion.")
        .required(true);

    // Anthropic models accept either Temperature or Top P, never both — leave the unused field empty.
    public static final ModifiableNumberProperty TEMPERATURE_PROPERTY = number(TEMPERATURE)
        .label("Temperature")
        .description(
            "Controls randomness: higher values make the output more random, lower values make it more focused " +
                "and deterministic. Set either Temperature or Top P, not both.")
        .minValue(0)
        .maxValue(1)
        .advancedOption(true);

    public static final ModifiableNumberProperty TOP_P_PROPERTY = number(TOP_P)
        .label("Top P")
        .description(
            "Nucleus sampling: the model considers tokens whose cumulative probability mass adds up to top_p. " +
                "Set either Temperature or Top P, not both.")
        .minValue(0)
        .maxValue(1)
        .advancedOption(true);

    public static final List<Option<String>> MODELS = ModelUtils.getEnumOptions(toChatModelMap());

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

    /**
     * Model ids are read straight off the Anthropic SDK's {@link Model} constants via {@link Model#asString()}, so the
     * options list refreshes automatically on SDK upgrades.
     */
    private static Map<String, String> toChatModelMap() {
        return Arrays.stream(Model.class.getFields())
            .filter(field -> Modifier.isStatic(field.getModifiers()) && field.getType() == Model.class)
            .map(AnthropicConstants::resolveChatModelValue)
            .collect(Collectors.toMap(Function.identity(), Function.identity(), (a, b) -> a));
    }

    private static String resolveChatModelValue(Field field) {
        try {
            return ((Model) field.get(null)).asString();
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
