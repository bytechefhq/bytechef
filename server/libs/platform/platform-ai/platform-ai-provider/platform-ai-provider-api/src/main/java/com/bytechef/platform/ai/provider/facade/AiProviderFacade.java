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

package com.bytechef.platform.ai.provider.facade;

import com.bytechef.platform.ai.provider.dto.AiDefaultModelDTO;
import com.bytechef.platform.ai.provider.dto.AiDefaultModelWithApiKeyDTO;
import com.bytechef.platform.ai.provider.dto.AiProviderCatalogItemDTO;
import com.bytechef.platform.ai.provider.dto.AiProviderDTO;
import java.util.List;

/**
 * @author Ivica Cardic
 */
public interface AiProviderFacade {

    void deleteAiProvider(int id, int environment);

    AiDefaultModelDTO getAiDefaultChatModel(int environmentId);

    /**
     * Resolves the default chat model for a specific provider, applying the same eligibility rules as
     * {@link #getAiDefaultChatModel(int)} (provider must be enabled, a chat provider, and have a configured model) but
     * scoped to a single provider key. Returns {@code null} when the provider is not eligible in the environment.
     */
    AiDefaultModelDTO getAiDefaultChatModel(String providerKey, int environmentId);

    AiDefaultModelWithApiKeyDTO getAiDefaultChatModelApiKey(int environmentId);

    AiDefaultModelDTO getAiDefaultEmbeddingModel(int environmentId);

    AiDefaultModelWithApiKeyDTO getAiDefaultEmbeddingModelApiKey(int environmentId);

    List<AiProviderCatalogItemDTO> getAiChatProviderCatalog(int environment);

    List<AiProviderDTO> getAiProviders(int environment);

    String getApiKey(String provider, int environment);

    String getUrl(String provider, int environment);

    void updateAiProvider(int id, boolean enabled, int environment);

    void updateAiProvider(int id, String apiKey, String url, int environment);
}
