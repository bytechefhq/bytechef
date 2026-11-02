/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.audit.aspect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.audit.config.AuditIntTestConfiguration;
import com.bytechef.ee.platform.audit.domain.AuditEventFilter;
import com.bytechef.ee.platform.audit.domain.PersistentAuditEvent;
import com.bytechef.ee.platform.audit.repository.PersistenceAuditEventRepository;
import com.bytechef.platform.oauth2.authorizationserver.audit.OAuth2ClientAuditEvents;
import com.bytechef.platform.oauth2.authorizationserver.audit.OAuth2ClientAuditMapper;
import com.bytechef.platform.oauth2.authorizationserver.facade.RegisteredClientFacade;
import com.bytechef.platform.oauth2.authorizationserver.facade.RegisteredClientFacadeImpl;
import com.bytechef.platform.oauth2.authorizationserver.service.OAuth2AuthorizationGrantService;
import com.bytechef.platform.oauth2.authorizationserver.service.RegisteredClientService;
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
    OAuth2ClientAuditIntTest.RegisteredClientFacadeConfiguration.class, OAuth2ClientAuditMapper.class,
    PostgreSQLContainerConfiguration.class
})
class OAuth2ClientAuditIntTest {

    private static final String REGISTERED_CLIENT_ID = "client-1";

    @MockitoBean
    private OAuth2AuthorizationGrantService oAuth2AuthorizationGrantService;

    @Autowired
    private PersistenceAuditEventRepository persistenceAuditEventRepository;

    @Autowired
    private RegisteredClientFacade registeredClientFacade;

    @MockitoBean
    private RegisteredClientService registeredClientService;

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
    void testDeleteRecordsTheClient() {
        when(registeredClientService.deleteRegisteredClient(REGISTERED_CLIENT_ID)).thenReturn(true);

        registeredClientFacade.deleteRegisteredClient(REGISTERED_CLIENT_ID);

        verify(registeredClientService).deleteRegisteredClient(REGISTERED_CLIENT_ID);

        PersistentAuditEvent persistentAuditEvent = findSingle();

        assertThat(persistentAuditEvent.getEventType()).isEqualTo(OAuth2ClientAuditEvents.OAUTH2_CLIENT_DELETED);
        assertThat(persistentAuditEvent.getPrincipal()).isEqualTo("admin@localhost.com");
        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("registeredClientId", REGISTERED_CLIENT_ID)
            .doesNotContainKey("dataError");
    }

    @Test
    void testDeleteOfAMissingClientIsNotRecorded() {
        when(registeredClientService.deleteRegisteredClient(REGISTERED_CLIENT_ID)).thenReturn(false);

        assertThat(registeredClientFacade.deleteRegisteredClient(REGISTERED_CLIENT_ID)).isFalse();

        Page<PersistentAuditEvent> page =
            persistenceAuditEventRepository.findAllFiltered(AuditEventFilter.empty(), PageRequest.of(0, 25));

        assertThat(page.getContent()).isEmpty();
    }

    @Test
    void testRevokeGrantRecordsTheClientAndThePrincipal() {
        registeredClientFacade.revokeRegisteredClientGrant(REGISTERED_CLIENT_ID, "bob@localhost.com");

        verify(oAuth2AuthorizationGrantService).revokeGrant(REGISTERED_CLIENT_ID, "bob@localhost.com");

        PersistentAuditEvent persistentAuditEvent = findSingle();

        assertThat(persistentAuditEvent.getEventType()).isEqualTo(OAuth2ClientAuditEvents.OAUTH2_CLIENT_GRANT_REVOKED);
        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("registeredClientId", REGISTERED_CLIENT_ID)
            .containsEntry("principalName", "bob@localhost.com")
            .doesNotContainKey("dataError");
    }

    @Test
    void testRevokeAllGrantsRecordsTheClient() {
        registeredClientFacade.revokeRegisteredClientGrants(REGISTERED_CLIENT_ID);

        verify(oAuth2AuthorizationGrantService).revokeGrants(REGISTERED_CLIENT_ID);

        PersistentAuditEvent persistentAuditEvent = findSingle();

        assertThat(persistentAuditEvent.getEventType())
            .isEqualTo(OAuth2ClientAuditEvents.OAUTH2_CLIENT_ALL_GRANTS_REVOKED);
        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("registeredClientId", REGISTERED_CLIENT_ID)
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
    static class RegisteredClientFacadeConfiguration {

        @Bean
        RegisteredClientFacade registeredClientFacade(
            OAuth2AuthorizationGrantService oAuth2AuthorizationGrantService,
            RegisteredClientService registeredClientService) {

            return new RegisteredClientFacadeImpl(oAuth2AuthorizationGrantService, registeredClientService);
        }
    }
}
