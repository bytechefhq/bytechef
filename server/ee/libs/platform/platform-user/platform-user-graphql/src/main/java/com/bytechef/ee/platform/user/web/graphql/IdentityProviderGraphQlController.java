/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.user.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.platform.user.facade.IdentityProviderFacade;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.user.domain.IdentityProvider;
import com.bytechef.platform.user.domain.IdentityProviderDomain;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/**
 * GraphQL controller for managing Identity Providers (OIDC SSO configuration).
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnEEVersion
@ConditionalOnCoordinator
public class IdentityProviderGraphQlController {

    private final IdentityProviderFacade identityProviderFacade;

    @SuppressFBWarnings("EI")
    public IdentityProviderGraphQlController(IdentityProviderFacade identityProviderFacade) {
        this.identityProviderFacade = identityProviderFacade;
    }

    @MutationMapping(name = "createIdentityProvider")
    public IdentityProviderDTO createIdentityProvider(@Argument IdentityProviderInput input) {
        if (input.clientSecret() == null || input.clientSecret()
            .isEmpty()) {
            throw new IllegalArgumentException("clientSecret is required when creating an identity provider");
        }

        IdentityProvider identityProvider = toIdentityProvider(input);

        return toDTO(identityProviderFacade.create(identityProvider));
    }

    @MutationMapping(name = "deleteIdentityProvider")
    public Boolean deleteIdentityProvider(@Argument Long id) {
        identityProviderFacade.delete(id);

        return true;
    }

    @QueryMapping(name = "identityProvider")
    public IdentityProviderDTO identityProvider(@Argument Long id) {
        return toDTO(identityProviderFacade.getIdentityProvider(id));
    }

    @QueryMapping(name = "identityProviders")
    public List<IdentityProviderDTO> identityProviders() {
        return identityProviderFacade.getIdentityProviders()
            .stream()
            .map(this::toDTO)
            .toList();
    }

    @MutationMapping(name = "updateIdentityProvider")
    public IdentityProviderDTO updateIdentityProvider(@Argument Long id, @Argument IdentityProviderInput input) {
        IdentityProvider identityProvider = toIdentityProvider(input);

        identityProvider.setId(id);

        return toDTO(identityProviderFacade.update(identityProvider));
    }

    private IdentityProviderDTO toDTO(IdentityProvider identityProvider) {
        List<String> domainsList = List.of();

        if (identityProvider.getDomains() != null) {
            domainsList = identityProvider.getDomains()
                .stream()
                .map(IdentityProviderDomain::getDomain)
                .toList();
        }

        return new IdentityProviderDTO(
            identityProvider.isAutoProvision(),
            identityProvider.getClientId(),
            identityProvider.getCreatedBy(),
            identityProvider.getCreatedDate() != null ? identityProvider.getCreatedDate()
                .toEpochMilli() : null,
            identityProvider.getDefaultAuthority(),
            domainsList,
            identityProvider.isEnabled(),
            identityProvider.isEnforced(),
            identityProvider.getId(),
            identityProvider.getIssuerUri(),
            identityProvider.getLastModifiedBy(),
            identityProvider.getLastModifiedDate() != null
                ? identityProvider.getLastModifiedDate()
                    .toEpochMilli()
                : null,
            identityProvider.getMetadataUri(),
            identityProvider.getMfaMethod(),
            identityProvider.isMfaRequired(),
            identityProvider.getName(),
            identityProvider.getNameIdFormat(),
            identityProvider.getScopes(),
            identityProvider.getSigningCertificate(),
            identityProvider.getType());
    }

    private IdentityProvider toIdentityProvider(IdentityProviderInput input) {
        IdentityProvider identityProvider = new IdentityProvider();

        if (input.autoProvision() != null) {
            identityProvider.setAutoProvision(input.autoProvision());
        }

        identityProvider.setClientId(input.clientId());
        identityProvider.setClientSecret(input.clientSecret());

        if (input.defaultAuthority() != null) {
            identityProvider.setDefaultAuthority(input.defaultAuthority());
        }

        if (input.domains() != null) {
            Set<IdentityProviderDomain> domains = input.domains()
                .stream()
                .map(domain -> new IdentityProviderDomain(domain.toLowerCase()))
                .collect(Collectors.toSet());

            identityProvider.setDomains(domains);
        }

        if (input.enabled() != null) {
            identityProvider.setEnabled(input.enabled());
        }

        if (input.enforced() != null) {
            identityProvider.setEnforced(input.enforced());
        }

        if (input.issuerUri() != null) {
            identityProvider.setIssuerUri(input.issuerUri());
        }

        if (input.metadataUri() != null) {
            identityProvider.setMetadataUri(input.metadataUri());
        }

        if (input.mfaMethod() != null) {
            identityProvider.setMfaMethod(input.mfaMethod());
        }

        if (input.mfaRequired() != null) {
            identityProvider.setMfaRequired(input.mfaRequired());
        }

        identityProvider.setName(input.name());

        if (input.nameIdFormat() != null) {
            identityProvider.setNameIdFormat(input.nameIdFormat());
        }

        if (input.scopes() != null) {
            identityProvider.setScopes(input.scopes());
        }

        if (input.signingCertificate() != null) {
            identityProvider.setSigningCertificate(input.signingCertificate());
        }

        if (input.type() != null) {
            identityProvider.setType(input.type());
        }

        return identityProvider;
    }

    @SuppressFBWarnings("EI")
    record IdentityProviderDTO(
        boolean autoProvision, String clientId, String createdBy, Long createdDate, String defaultAuthority,
        List<String> domains, boolean enabled, boolean enforced, Long id, String issuerUri, String lastModifiedBy,
        Long lastModifiedDate, String metadataUri, String mfaMethod, boolean mfaRequired, String name,
        String nameIdFormat, String scopes, String signingCertificate, String type) {
    }

    record IdentityProviderInput(
        Boolean autoProvision, String clientId, String clientSecret, String defaultAuthority, List<String> domains,
        Boolean enabled, Boolean enforced, String issuerUri, String metadataUri, String mfaMethod,
        Boolean mfaRequired, String name, String nameIdFormat, String scopes, String signingCertificate,
        String type) {
    }
}
