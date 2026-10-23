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

import com.bytechef.platform.ai.provider.dto.WebSearchProviderDTO;
import com.bytechef.platform.ai.websearch.WebSearchProvider;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 */
public interface WebSearchProviderFacade {

    void deleteWebSearchProvider(String name, int environment);

    /**
     * Returns the API key of a web search provider that is enabled in the environment, or {@code null}. A saved
     * provider must be switched on; without a saved provider the key from the application properties counts when the
     * properties enable it.
     */
    @Nullable
    String fetchEnabledApiKey(WebSearchProvider webSearchProvider, int environment);

    List<WebSearchProviderDTO> getWebSearchProviders(int environment);

    void enableWebSearchProvider(String name, boolean enabled, int environment);

    void updateWebSearchProvider(String name, String apiKey, int environment);
}
