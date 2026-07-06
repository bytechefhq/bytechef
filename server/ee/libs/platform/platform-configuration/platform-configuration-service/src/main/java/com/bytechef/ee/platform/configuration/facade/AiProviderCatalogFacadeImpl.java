/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.configuration.facade;

import com.bytechef.ee.platform.configuration.dto.AiDefaultModelDTO;
import com.bytechef.ee.platform.configuration.dto.AiProviderCatalogItemDTO;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.security.constant.AuthorityConstants;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * Implementation of {@link AiProviderCatalogFacade}. Delegates to the shared {@code AiProviderFacade} and carries the
 * {@code USER} guard so it is enforced for every caller of the facade.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
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
