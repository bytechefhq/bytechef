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

package com.bytechef.ai.copilot.config;

import com.bytechef.platform.ai.llm.Provider;
import com.bytechef.platform.ai.provider.facade.AiProviderFacade;
import com.bytechef.platform.configuration.context.EnvironmentContext;
import com.bytechef.platform.configuration.domain.Environment;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;

/**
 * @author Ivica Cardic
 */
class CopilotQueryEmbeddingModel implements EmbeddingModel {

    private final AiProviderFacade aiProviderFacade;
    private final Map<Integer, KeyedEmbeddingModel> delegates = new ConcurrentHashMap<>();
    private final int dimensions;
    private final Function<String, EmbeddingModel> embeddingModelFactory;

    CopilotQueryEmbeddingModel(
        AiProviderFacade aiProviderFacade, Function<String, EmbeddingModel> embeddingModelFactory, int dimensions) {

        this.aiProviderFacade = aiProviderFacade;
        this.dimensions = dimensions;
        this.embeddingModelFactory = embeddingModelFactory;
    }

    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        EmbeddingModel delegate = resolveDelegate();

        return delegate.call(request);
    }

    @Override
    public float[] embed(Document document) {
        EmbeddingModel delegate = resolveDelegate();

        return delegate.embed(document);
    }

    @Override
    public int dimensions() {
        return dimensions;
    }

    static boolean hasApiKey(AiProviderFacade aiProviderFacade, int environment) {
        String apiKey = aiProviderFacade.getApiKey(Provider.OPEN_AI.getKey(), environment);

        return apiKey != null && !apiKey.isBlank();
    }

    private EmbeddingModel resolveDelegate() {
        Environment environment = EnvironmentContext.getCurrentEnvironment();

        String apiKey = aiProviderFacade.getApiKey(Provider.OPEN_AI.getKey(), environment.ordinal());

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                "Searching the ByteChef documentation and the AI Hub tool catalog needs an OpenAI API key. Add one " +
                    "for the " + environment + " environment on the Settings > AI Providers page.");
        }

        KeyedEmbeddingModel keyedEmbeddingModel = delegates.compute(
            environment.ordinal(),
            (ignoredOrdinal, current) -> current != null && current.hasApiKey(apiKey)
                ? current : new KeyedEmbeddingModel(apiKey, embeddingModelFactory.apply(apiKey)));

        return keyedEmbeddingModel.embeddingModel();
    }

    private record KeyedEmbeddingModel(String apiKey, EmbeddingModel embeddingModel) {

        boolean hasApiKey(String candidateApiKey) {
            return apiKey.equals(candidateApiKey);
        }
    }
}
