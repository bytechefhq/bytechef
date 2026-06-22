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

package com.bytechef.component.ai.llm;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.bytechef.platform.ai.llm.Provider;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class LLMModelRegistryTest {

    private static final ChatModel CHAT_MODEL =
        (inputParameters, connectionParameters, responseFormatRequired) -> null;
    private static final ImageModel IMAGE_MODEL = (inputParameters, connectionParameters) -> null;

    @Test
    void testBuildChatModelsIncludesProvidersWithChatModel() {
        LLMModelProvider chatProvider = new LLMModelProvider() {

            @Override
            public Provider getProvider() {
                return Provider.OPEN_AI;
            }

            @Override
            public ChatModel getChatModel() {
                return CHAT_MODEL;
            }
        };

        Map<Provider, ChatModel> chatModels = LLMModelRegistry.buildChatModels(List.of(chatProvider));

        assertSame(CHAT_MODEL, chatModels.get(Provider.OPEN_AI));
    }

    @Test
    void testBuildChatModelsSkipsProvidersWithoutChatModel() {
        LLMModelProvider imageProvider = new LLMModelProvider() {

            @Override
            public Provider getProvider() {
                return Provider.STABILITY;
            }

            @Override
            public ImageModel getImageModel() {
                return IMAGE_MODEL;
            }
        };

        Map<Provider, ChatModel> chatModels = LLMModelRegistry.buildChatModels(List.of(imageProvider));

        assertFalse(chatModels.containsKey(Provider.STABILITY));
    }

    @Test
    void testBuildImageModelsIncludesProvidersWithImageModel() {
        LLMModelProvider imageProvider = new LLMModelProvider() {

            @Override
            public Provider getProvider() {
                return Provider.STABILITY;
            }

            @Override
            public ImageModel getImageModel() {
                return IMAGE_MODEL;
            }
        };

        Map<Provider, ImageModel> imageModels = LLMModelRegistry.buildImageModels(List.of(imageProvider));

        assertSame(IMAGE_MODEL, imageModels.get(Provider.STABILITY));
    }

    @Test
    void testBuildImageModelsSkipsProvidersWithoutImageModel() {
        LLMModelProvider chatProvider = new LLMModelProvider() {

            @Override
            public Provider getProvider() {
                return Provider.OPEN_AI;
            }

            @Override
            public ChatModel getChatModel() {
                return CHAT_MODEL;
            }
        };

        Map<Provider, ImageModel> imageModels = LLMModelRegistry.buildImageModels(List.of(chatProvider));

        assertFalse(imageModels.containsKey(Provider.OPEN_AI));
    }
}
