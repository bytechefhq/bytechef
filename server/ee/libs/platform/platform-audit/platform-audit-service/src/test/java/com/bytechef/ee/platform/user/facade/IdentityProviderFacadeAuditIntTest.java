/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.user.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.audit.config.AuditIntTestConfiguration;
import com.bytechef.ee.platform.audit.domain.AuditEventFilter;
import com.bytechef.ee.platform.audit.domain.PersistentAuditEvent;
import com.bytechef.ee.platform.audit.repository.PersistenceAuditEventRepository;
import com.bytechef.ee.platform.user.audit.IdentityProviderAuditEvents;
import com.bytechef.ee.platform.user.domain.IdentityProvider;
import com.bytechef.ee.platform.user.service.IdentityProviderService;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = AuditIntTestConfiguration.class)
@ActiveProfiles("testint")
@Import({
    IdentityProviderFacadeAuditIntTest.IdentityProviderFacadeConfiguration.class,
    PostgreSQLContainerConfiguration.class
})
class IdentityProviderFacadeAuditIntTest {

    private static final String CLIENT_SECRET = "cl1ent-s3cret";
    private static final String SCIM_API_KEY = "sc1m-ap1-key";

    @Autowired
    private IdentityProviderFacade identityProviderFacade;

    @MockitoBean
    private IdentityProviderService identityProviderService;

    @Autowired
    private PersistenceAuditEventRepository persistenceAuditEventRepository;

    @BeforeEach
    void beforeEach() {
        persistenceAuditEventRepository.deleteAll();

        SecurityContext securityContext = SecurityContextHolder.getContext();

        securityContext.setAuthentication(
            new UsernamePasswordAuthenticationToken(
                "admin@localhost.com", "n/a", List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testUpdateRecordsTheProviderSettingsButNeverItsSecrets() {
        IdentityProvider identityProvider = new IdentityProvider();

        identityProvider.setClientSecret(CLIENT_SECRET);
        identityProvider.setEnabled(true);
        identityProvider.setEnforced(true);
        identityProvider.setId(4L);
        identityProvider.setName("okta");
        identityProvider.setScimApiKey(SCIM_API_KEY);

        when(identityProviderService.update(identityProvider)).thenReturn(identityProvider);

        identityProviderFacade.update(identityProvider);

        PersistentAuditEvent persistentAuditEvent = findSingle();

        assertThat(persistentAuditEvent.getEventType())
            .isEqualTo(IdentityProviderAuditEvents.IDENTITY_PROVIDER_UPDATED);
        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("identityProviderId", "4")
            .containsEntry("name", "okta")
            .containsEntry("type", "OIDC")
            .containsEntry("enabled", "true")
            .containsEntry("enforced", "true")
            .doesNotContainValue(CLIENT_SECRET)
            .doesNotContainValue(SCIM_API_KEY)
            .doesNotContainKey("dataError");
    }

    private PersistentAuditEvent findSingle() {
        Page<PersistentAuditEvent> page =
            persistenceAuditEventRepository.findAllFiltered(AuditEventFilter.empty(), PageRequest.of(0, 25));

        List<PersistentAuditEvent> persistentAuditEvents = page.getContent();

        assertThat(persistentAuditEvents).hasSize(1);

        return persistentAuditEvents.getFirst();
    }

    @Configuration
    static class IdentityProviderFacadeConfiguration {

        @Bean
        IdentityProviderFacade identityProviderFacade(IdentityProviderService identityProviderService) {
            return new IdentityProviderFacadeImpl(identityProviderService);
        }
    }
}
