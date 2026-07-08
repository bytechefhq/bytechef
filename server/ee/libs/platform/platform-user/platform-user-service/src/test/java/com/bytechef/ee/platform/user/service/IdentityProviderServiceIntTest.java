/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.user.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.platform.user.config.IdentityProviderIntTestConfiguration;
import com.bytechef.ee.platform.user.domain.IdentityProvider;
import com.bytechef.ee.platform.user.repository.IdentityProviderRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for {@link IdentityProviderService#fetchMcpIdentityProvider()}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = IdentityProviderIntTestConfiguration.class, properties = "bytechef.edition=ee")
@Transactional
class IdentityProviderServiceIntTest {

    @Autowired
    private IdentityProviderRepository identityProviderRepository;

    @Autowired
    private IdentityProviderService identityProviderService;

    @BeforeEach
    void beforeEach() {
        identityProviderRepository.deleteAll();
    }

    @Test
    void testFetchMcpIdentityProviderReturnsTheEnabledMcpProvider() {
        save("mcp-idp", "https://mcp.idp.test", true, true);
        save("regular-idp", "https://regular.idp.test", true, false);
        save("disabled-mcp-idp", "https://disabled.idp.test", false, true);

        Optional<IdentityProvider> identityProvider = identityProviderService.fetchMcpIdentityProvider();

        assertThat(identityProvider).isPresent();
        assertThat(identityProvider.get()
            .getIssuerUri()).isEqualTo("https://mcp.idp.test");
    }

    @Test
    void testFetchMcpIdentityProviderReturnsEmptyWhenNoneFlagged() {
        save("regular-idp", "https://regular.idp.test", true, false);

        assertThat(identityProviderService.fetchMcpIdentityProvider()).isEmpty();
    }

    @Test
    void testMcpSurfaceFlagsAndAuthorityMappingRoundTrip() {
        IdentityProvider identityProvider = new IdentityProvider();

        identityProvider.setName("automation-idp");
        identityProvider.setIssuerUri("https://automation.idp.test");
        identityProvider.setClientId("client-id");
        identityProvider.setClientSecret("client-secret");
        identityProvider.setEnabled(true);
        identityProvider.setMcpAutomation(true);
        identityProvider.setMcpManagement(true);
        identityProvider.setAuthoritiesClaim("roles");
        identityProvider.setAuthorityMappings(java.util.Map.of("sales", "ROLE_SALES", "admins", "ROLE_ADMIN"));

        Long id = identityProviderRepository.save(identityProvider)
            .getId();

        IdentityProvider reloaded = identityProviderRepository.findById(id)
            .orElseThrow();

        assertThat(reloaded.isMcpAutomation()).isTrue();
        assertThat(reloaded.isMcpManagement()).isTrue();
        assertThat(reloaded.getAuthoritiesClaim()).isEqualTo("roles");
        assertThat(reloaded.getAuthorityMappings())
            .containsExactlyInAnyOrderEntriesOf(java.util.Map.of("sales", "ROLE_SALES", "admins", "ROLE_ADMIN"));
    }

    @Test
    void testValidateMcpAudienceRoundTrips() {
        IdentityProvider identityProvider = new IdentityProvider();

        identityProvider.setName("audience-idp");
        identityProvider.setIssuerUri("https://audience.idp.test");
        identityProvider.setClientId("client-id");
        identityProvider.setClientSecret("client-secret");
        identityProvider.setEnabled(true);
        identityProvider.setValidateMcpAudience(true);

        Long id = identityProviderRepository.save(identityProvider)
            .getId();

        assertThat(identityProviderRepository.findById(id))
            .get()
            .extracting(IdentityProvider::isValidateMcpAudience)
            .isEqualTo(true);
    }

    private void save(String name, String issuerUri, boolean enabled, boolean mcp) {
        IdentityProvider identityProvider = new IdentityProvider();

        identityProvider.setName(name);
        identityProvider.setIssuerUri(issuerUri);
        identityProvider.setClientId("client-id");
        identityProvider.setClientSecret("client-secret");
        identityProvider.setEnabled(enabled);
        identityProvider.setMcpEmbedded(mcp);

        identityProviderRepository.save(identityProvider);
    }
}
