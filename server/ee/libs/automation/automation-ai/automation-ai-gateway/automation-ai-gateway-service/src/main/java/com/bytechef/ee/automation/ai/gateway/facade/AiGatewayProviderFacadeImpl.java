/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.facade;

import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayProvider;
import com.bytechef.ee.platform.ai.gateway.service.AiGatewayProviderService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.security.constant.AuthorityConstants;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * Implementation of {@link AiGatewayProviderFacade}. Delegates to the shared {@code AiGatewayProviderService} and
 * carries the {@code ADMIN} authorization guard so it is enforced for every caller of the facade.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
class AiGatewayProviderFacadeImpl implements AiGatewayProviderFacade {

    private final AiGatewayProviderService aiGatewayProviderService;

    @SuppressFBWarnings("EI")
    AiGatewayProviderFacadeImpl(AiGatewayProviderService aiGatewayProviderService) {
        this.aiGatewayProviderService = aiGatewayProviderService;
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public AiGatewayProvider getProvider(long id) {
        return aiGatewayProviderService.getProvider(id);
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public List<AiGatewayProvider> getProviders() {
        return aiGatewayProviderService.getProviders();
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public AiGatewayProvider create(AiGatewayProvider provider) {
        return aiGatewayProviderService.create(provider);
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public void delete(long id) {
        aiGatewayProviderService.delete(id);
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public AiGatewayProvider update(AiGatewayProvider provider) {
        return aiGatewayProviderService.update(provider);
    }
}
