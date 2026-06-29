/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.agent.catalog;

import static com.bytechef.component.ai.llm.constant.LLMConstants.MODEL;
import static com.bytechef.component.definition.Authorization.TOKEN;

import com.bytechef.component.ai.llm.Provider;
import com.bytechef.component.ai.llm.anthropic.action.AnthropicChatAction;
import com.bytechef.component.ai.llm.deepseek.action.DeepSeekChatAction;
import com.bytechef.component.ai.llm.gemini.action.GeminiChatAction;
import com.bytechef.component.ai.llm.groq.action.GroqChatAction;
import com.bytechef.component.ai.llm.mistral.action.MistralChatAction;
import com.bytechef.component.ai.llm.nvidia.action.NvidiaChatAction;
import com.bytechef.component.ai.llm.openai.action.OpenAiChatAction;
import com.bytechef.component.ai.llm.perplexity.action.PerplexityChatAction;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.component.definition.ParametersFactory;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

/**
 * Builds a Spring-AI {@link ChatModel} for a catalog {@link Provider} + model name + platform API key, by reusing each
 * component's existing {@code CHAT_MODEL} lambda fed synthetic {@link Parameters}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class CatalogChatModelFactory {

    public @Nullable ChatModel createChatModel(Provider provider, String model, String apiKey) {
        com.bytechef.component.ai.llm.ChatModel chatModelFactory = resolveFactory(provider);

        if (chatModelFactory == null) {
            return null;
        }

        Parameters inputParameters = ParametersFactory.create(Map.of(MODEL, model));
        Parameters connectionParameters = ParametersFactory.create(Map.of(TOKEN, apiKey));

        return chatModelFactory.createChatModel(inputParameters, connectionParameters, false);
    }

    private static com.bytechef.component.ai.llm.@Nullable ChatModel resolveFactory(Provider provider) {
        return switch (provider) {
            case OPEN_AI -> OpenAiChatAction.CHAT_MODEL;
            case ANTHROPIC -> AnthropicChatAction.CHAT_MODEL;
            case MISTRAL -> MistralChatAction.CHAT_MODEL;
            case VERTEX_GEMINI -> GeminiChatAction.CHAT_MODEL;
            case GROQ -> GroqChatAction.CHAT_MODEL;
            case PERPLEXITY -> PerplexityChatAction.CHAT_MODEL;
            case NVIDIA -> NvidiaChatAction.CHAT_MODEL;
            case DEEPSEEK -> DeepSeekChatAction.CHAT_MODEL;
            // AZURE_OPEN_AI and HUGGING_FACE need extra connection params not stored in the catalog.
            default -> null;
        };
    }
}
