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

package com.bytechef.component.ai.agenticai.config;

import com.embabel.agent.spi.support.springai.SpringAiLlmService;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/**
 * @author Ivica Cardic
 */
@Configuration
public class AgenticAiPlatformConfiguration {

    public static final String CANVAS_PLACEHOLDER_LLM_NAME = "bytechef-canvas";

    @Bean
    SpringAiLlmService byteChefCanvasPlaceholderLlmService() {
        return new SpringAiLlmService(CANVAS_PLACEHOLDER_LLM_NAME, "bytechef", new PlaceholderChatModel());
    }

    @Bean
    @ConditionalOnProperty("embabel.models.default-llm")
    @SuppressWarnings("removal")
    Jackson2ObjectMapperBuilder embabelJackson2ObjectMapperBuilder() {
        return new Jackson2ObjectMapperBuilder();
    }

    private static final class PlaceholderChatModel implements ChatModel {

        @Override
        public ChatResponse call(Prompt prompt) {
            throw new UnsupportedOperationException(
                "The '" + CANVAS_PLACEHOLDER_LLM_NAME + "' model is a startup placeholder: ByteChef's agentic AI " +
                    "routes all model calls through the workflow's Model cluster element and does not use Embabel's " +
                    "internal model registry. If you need Embabel-registered models, register real LlmService beans " +
                    "and set embabel.models.default-llm accordingly.");
        }
    }
}
