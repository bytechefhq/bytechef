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
import com.bytechef.platform.security.constant.AuthorityConstants;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * Implementation of {@link AiProviderCatalogFacade}. Delegates to the shared {@code AiProviderFacade} and carries the
 * {@code USER} guard so it is enforced for every caller of the facade.
 *
 * @author Ivica Cardic
 */
@Component
class AiProviderCatalogFacadeImpl implements AiProviderCatalogFacade {

    private final AiProviderFacade aiProviderFacade;

    @SuppressFBWarnings("EI")
    AiProviderCatalogFacadeImpl(AiProviderFacade aiProviderFacade) {
        this.aiProviderFacade = aiProviderFacade;
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.USER + "\")")
    public AiDefaultModelDTO getAiDefaultModel(int environment) {
        return aiProviderFacade.getAiDefaultChatModel(environment);
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.USER + "\")")
    public List<AiProviderCatalogItemDTO> getAiProviderCatalog(int environment) {
        return aiProviderFacade.getAiChatProviderCatalog(environment);
    }
}
