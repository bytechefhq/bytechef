/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.user.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.user.domain.IdentityProvider;
import com.bytechef.platform.user.domain.IdentityProviderDomain;
import com.bytechef.platform.user.service.IdentityProviderService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
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

    private final IdentityProviderService identityProviderService;

    @SuppressFBWarnings("EI")
    public IdentityProviderGraphQlController(IdentityProviderService identityProviderService) {
        this.identityProviderService = identityProviderService;
    }

    @MutationMapping(name = "createIdentityProvider")
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public IdentityProviderDTO createIdentityProvider(@Argument IdentityProviderInput input) {
        IdentityProvider identityProvider = toIdentityProvider(input);

        return toDTO(identityProviderService.create(identityProvider));
    }

    @MutationMapping(name = "deleteIdentityProvider")
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public Boolean deleteIdentityProvider(@Argument Long id) {
        identityProviderService.delete(id);

        return true;
    }

    @QueryMapping(name = "identityProvider")
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public IdentityProviderDTO identityProvider(@Argument Long id) {
        return toDTO(identityProviderService.getIdentityProvider(id));
    }

    @QueryMapping(name = "identityProviders")
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public List<IdentityProviderDTO> identityProviders() {
        return identityProviderService.getIdentityProviders()
            .stream()
            .map(this::toDTO)
            .toList();
    }

    @MutationMapping(name = "updateIdentityProvider")
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public IdentityProviderDTO updateIdentityProvider(@Argument Long id, @Argument IdentityProviderInput input) {
        IdentityProvider identityProvider = toIdentityProvider(input);

        identityProvider.setId(id);

        return toDTO(identityProviderService.update(identityProvider));
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
            identityProvider.getName(),
            identityProvider.getScopes(),
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

        identityProvider.setIssuerUri(input.issuerUri());
        identityProvider.setName(input.name());

        if (input.scopes() != null) {
            identityProvider.setScopes(input.scopes());
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
        Long lastModifiedDate, String name, String scopes, String type) {
    }

    record IdentityProviderInput(
        Boolean autoProvision, String clientId, String clientSecret, String defaultAuthority, List<String> domains,
        Boolean enabled, Boolean enforced, String issuerUri, String name, String scopes, String type) {
    }
}
