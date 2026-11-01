/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.user.facade;

import com.bytechef.ee.platform.user.audit.IdentityProviderAuditEvents;
import com.bytechef.ee.platform.user.domain.IdentityProvider;
import com.bytechef.ee.platform.user.service.IdentityProviderService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.audit.AuditData;
import com.bytechef.platform.audit.Audited;
import com.bytechef.platform.security.constant.AuthorityConstants;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
class IdentityProviderFacadeImpl implements IdentityProviderFacade {

    private final IdentityProviderService identityProviderService;

    @SuppressFBWarnings("EI")
    IdentityProviderFacadeImpl(IdentityProviderService identityProviderService) {
        this.identityProviderService = identityProviderService;
    }

    @Override
    @Audited(event = IdentityProviderAuditEvents.IDENTITY_PROVIDER_CREATED, data = {
        @AuditData(key = "identityProviderId", value = "#result?.id"),
        @AuditData(key = "name", value = "#identityProvider.name"),
        @AuditData(key = "type", value = "#identityProvider.type")
    })
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public IdentityProvider create(IdentityProvider identityProvider) {
        return identityProviderService.create(identityProvider);
    }

    @Override
    @Audited(event = IdentityProviderAuditEvents.IDENTITY_PROVIDER_DELETED, data = {
        @AuditData(key = "identityProviderId", value = "#id")
    })
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public void delete(Long id) {
        identityProviderService.getIdentityProvider(id);

        identityProviderService.delete(id);
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public IdentityProvider getIdentityProvider(Long id) {
        return identityProviderService.getIdentityProvider(id);
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public List<IdentityProvider> getIdentityProviders() {
        return identityProviderService.getIdentityProviders();
    }

    @Override
    @Audited(event = IdentityProviderAuditEvents.IDENTITY_PROVIDER_UPDATED, data = {
        @AuditData(key = "identityProviderId", value = "#identityProvider.id"),
        @AuditData(key = "name", value = "#identityProvider.name"),
        @AuditData(key = "type", value = "#identityProvider.type"),
        @AuditData(key = "enabled", value = "#identityProvider.enabled"),
        @AuditData(key = "enforced", value = "#identityProvider.enforced")
    })
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public IdentityProvider update(IdentityProvider identityProvider) {
        return identityProviderService.update(identityProvider);
    }
}
