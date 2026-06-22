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

import com.bytechef.platform.ai.llm.Provider;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

/**
 * Builds {@link Provider}-keyed maps of {@link ChatModel} and {@link ImageModel} from all {@link LLMModelProvider}
 * implementations discovered on the classpath via {@link ServiceLoader}.
 *
 * @author Ivica Cardic
 */
public final class LLMModelRegistry {

    private static final Map<Provider, ChatModel> CHAT_MODELS;
    private static final Map<Provider, ImageModel> IMAGE_MODELS;

    static {
        List<LLMModelProvider> llmModelProviders = new ArrayList<>();

        ServiceLoader.load(LLMModelProvider.class)
            .forEach(llmModelProviders::add);

        CHAT_MODELS = buildChatModels(llmModelProviders);
        IMAGE_MODELS = buildImageModels(llmModelProviders);
    }

    private LLMModelRegistry() {
    }

    public static ChatModel getChatModel(Provider provider) {
        ChatModel chatModel = CHAT_MODELS.get(provider);

        if (chatModel == null) {
            throw new IllegalArgumentException("No chat model registered for provider: " + provider);
        }

        return chatModel;
    }

    public static ImageModel getImageModel(Provider provider) {
        ImageModel imageModel = IMAGE_MODELS.get(provider);

        if (imageModel == null) {
            throw new IllegalArgumentException("No image model registered for provider: " + provider);
        }

        return imageModel;
    }

    public static boolean hasChatModel(Provider provider) {
        return CHAT_MODELS.containsKey(provider);
    }

    public static boolean hasImageModel(Provider provider) {
        return IMAGE_MODELS.containsKey(provider);
    }

    static Map<Provider, ChatModel> buildChatModels(Iterable<LLMModelProvider> llmModelProviders) {
        Map<Provider, ChatModel> chatModels = new EnumMap<>(Provider.class);

        for (LLMModelProvider llmModelProvider : llmModelProviders) {
            ChatModel chatModel = llmModelProvider.getChatModel();

            if (chatModel != null) {
                chatModels.put(llmModelProvider.getProvider(), chatModel);
            }
        }

        return chatModels;
    }

    static Map<Provider, ImageModel> buildImageModels(Iterable<LLMModelProvider> llmModelProviders) {
        Map<Provider, ImageModel> imageModels = new EnumMap<>(Provider.class);

        for (LLMModelProvider llmModelProvider : llmModelProviders) {
            ImageModel imageModel = llmModelProvider.getImageModel();

            if (imageModel != null) {
                imageModels.put(llmModelProvider.getProvider(), imageModel);
            }
        }

        return imageModels;
    }
}
