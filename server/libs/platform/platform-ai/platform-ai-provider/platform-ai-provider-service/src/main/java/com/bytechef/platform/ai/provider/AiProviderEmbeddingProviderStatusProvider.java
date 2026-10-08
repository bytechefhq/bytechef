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

package com.bytechef.platform.ai.provider;

import com.bytechef.platform.ai.provider.facade.AiProviderFacade;
import com.bytechef.platform.configuration.ai.EmbeddingProviderStatusProvider;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.stereotype.Component;

/**
 * Embeddings are active for an environment when a default embedding provider is activated and its API key resolves --
 * the same predicate the runtime embedding model uses to build its delegate.
 *
 * @author Ivica Cardic
 */
@Component
public class AiProviderEmbeddingProviderStatusProvider implements EmbeddingProviderStatusProvider {

    private final AiProviderFacade aiProviderFacade;

    @SuppressFBWarnings("EI")
    public AiProviderEmbeddingProviderStatusProvider(AiProviderFacade aiProviderFacade) {
        this.aiProviderFacade = aiProviderFacade;
    }

    @Override
    public boolean isEmbeddingActive(int environment) {
        return aiProviderFacade.getAiDefaultEmbeddingModelApiKey(environment) != null;
    }
}
