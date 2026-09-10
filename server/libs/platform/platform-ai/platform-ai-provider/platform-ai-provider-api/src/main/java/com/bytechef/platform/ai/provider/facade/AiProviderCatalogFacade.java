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
import com.bytechef.platform.ai.provider.dto.AiProviderCatalogItemDTO;
import java.util.List;

/**
 * Facade for reading the AI provider catalog. Hosts the {@code USER} authorization guard so it applies to every caller
 * of the facade rather than only the GraphQL entry point.
 *
 * @author Ivica Cardic
 */
public interface AiProviderCatalogFacade {

    AiDefaultModelDTO getAiDefaultModel(int environment);

    List<AiProviderCatalogItemDTO> getAiProviderCatalog(int environment);
}
