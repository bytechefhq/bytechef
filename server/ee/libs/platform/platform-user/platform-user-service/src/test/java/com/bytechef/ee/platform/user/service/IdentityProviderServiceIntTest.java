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
import com.bytechef.ee.platform.user.event.IdentityProviderChangedEvent;
import com.bytechef.ee.platform.user.repository.IdentityProviderRepository;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = IdentityProviderIntTestConfiguration.class, properties = "bytechef.edition=ee")
@RecordApplicationEvents
@Transactional
class IdentityProviderServiceIntTest {

    @Autowired
    private ApplicationEvents applicationEvents;

    @Autowired
    private IdentityProviderRepository identityProviderRepository;

    @Autowired
    private IdentityProviderService identityProviderService;

    @BeforeEach
    void beforeEach() {
        identityProviderRepository.deleteAll();
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
        identityProvider.setAuthorityMappings(Map.of("sales", "ROLE_SALES", "admins", "ROLE_ADMIN"));

        Long id = identityProviderRepository.save(identityProvider)
            .getId();

        IdentityProvider reloaded = identityProviderRepository.findById(id)
            .orElseThrow();

        assertThat(reloaded.isMcpAutomation()).isTrue();
        assertThat(reloaded.isMcpManagement()).isTrue();
        assertThat(reloaded.getAuthoritiesClaim()).isEqualTo("roles");
        assertThat(reloaded.getAuthorityMappings())
            .containsExactlyInAnyOrderEntriesOf(Map.of("sales", "ROLE_SALES", "admins", "ROLE_ADMIN"));
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

    @Test
    void testCreateUpdateAndDeletePublishIdentityProviderChangedEvents() {
        IdentityProvider identityProvider = new IdentityProvider();

        identityProvider.setName("event-idp");
        identityProvider.setIssuerUri("https://event.idp.test");
        identityProvider.setClientId("client-id");
        identityProvider.setClientSecret("client-secret");
        identityProvider.setEnabled(true);

        IdentityProvider createdIdentityProvider = identityProviderService.create(identityProvider);

        createdIdentityProvider.setMcpAutomation(true);

        identityProviderService.update(createdIdentityProvider);

        identityProviderService.delete(createdIdentityProvider.getId());

        assertThat(applicationEvents.stream(IdentityProviderChangedEvent.class)).hasSize(3);
    }
}
