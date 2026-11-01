/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.user.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.platform.audit.aspect.AuditAspect;
import com.bytechef.ee.platform.audit.aspect.AuditCaptureAspect;
import com.bytechef.ee.platform.audit.aspect.AuditMapperResolver;
import com.bytechef.ee.platform.audit.domain.AuditEventFilter;
import com.bytechef.ee.platform.audit.domain.PersistentAuditEvent;
import com.bytechef.ee.platform.audit.repository.PersistenceAuditEventRepository;
import com.bytechef.ee.platform.audit.service.AuditEventService;
import com.bytechef.ee.platform.user.audit.IdentityProviderAuditEvents;
import com.bytechef.ee.platform.user.domain.IdentityProvider;
import com.bytechef.ee.platform.user.repository.IdentityProviderRepository;
import com.bytechef.ee.platform.user.service.IdentityProviderServiceImpl;
import com.bytechef.encryption.EncryptionImpl;
import com.bytechef.encryption.EncryptionKey;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.test.config.jdbc.AbstractIntTestJdbcConfiguration;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = IdentityProviderFacadeAuditIntTest.AuditConfiguration.class,
    properties = {
        "bytechef.edition=ee",
        "spring.liquibase.change-log=classpath:config/liquibase/platform-user-audit-testint.xml"
    })
@Import(PostgreSQLContainerConfiguration.class)
class IdentityProviderFacadeAuditIntTest {

    private static final String ADMIN_LOGIN = "admin";
    private static final String CLIENT_SECRET = "plain-client-secret";
    private static final String UPDATED_CLIENT_SECRET = "updated-client-secret";

    @Autowired
    private IdentityProviderFacade identityProviderFacade;

    @Autowired
    private IdentityProviderRepository identityProviderRepository;

    @Autowired
    private PersistenceAuditEventRepository persistenceAuditEventRepository;

    @BeforeEach
    void beforeEach() {
        SecurityContextHolder.setContext(
            new SecurityContextImpl(
                new UsernamePasswordAuthenticationToken(
                    ADMIN_LOGIN, "n/a", List.of(new SimpleGrantedAuthority(AuthorityConstants.ADMIN)))));

        persistenceAuditEventRepository.deleteAll();
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();

        identityProviderRepository.deleteAll();
        persistenceAuditEventRepository.deleteAll();
    }

    @Test
    void testCreateRecordsTheIdentityProviderWithoutTheClientSecret() {
        String name = "identity-provider-" + UUID.randomUUID();

        IdentityProvider identityProvider = identityProviderFacade.create(newIdentityProvider(name));

        String storedClientSecret = getStoredClientSecret(identityProvider.getId());

        assertThat(storedClientSecret).isNotEqualTo(CLIENT_SECRET);

        Map<String, String> data = findSingleEvent(IdentityProviderAuditEvents.IDENTITY_PROVIDER_CREATED).getData();

        assertThat(data)
            .containsEntry("result", "SUCCESS")
            .containsEntry("identityProviderId", String.valueOf(identityProvider.getId()))
            .containsEntry("name", name)
            .containsEntry("type", "OIDC")
            .doesNotContainKey("clientSecret")
            .doesNotContainValue(CLIENT_SECRET)
            .doesNotContainValue(storedClientSecret);
    }

    @Test
    void testUpdateRecordsTheIdentityProviderWithoutTheClientSecret() {
        IdentityProvider identityProvider = identityProviderFacade.create(
            newIdentityProvider("identity-provider-" + UUID.randomUUID()));

        persistenceAuditEventRepository.deleteAll();

        identityProvider.setClientSecret(UPDATED_CLIENT_SECRET);
        identityProvider.setEnabled(false);
        identityProvider.setEnforced(true);
        identityProvider.setName("renamed");

        identityProviderFacade.update(identityProvider);

        String storedClientSecret = getStoredClientSecret(identityProvider.getId());

        assertThat(storedClientSecret).isNotEqualTo(UPDATED_CLIENT_SECRET);

        Map<String, String> data = findSingleEvent(IdentityProviderAuditEvents.IDENTITY_PROVIDER_UPDATED).getData();

        assertThat(data)
            .containsEntry("result", "SUCCESS")
            .containsEntry("identityProviderId", String.valueOf(identityProvider.getId()))
            .containsEntry("name", "renamed")
            .containsEntry("type", "OIDC")
            .containsEntry("enabled", "false")
            .containsEntry("enforced", "true")
            .doesNotContainKey("clientSecret")
            .doesNotContainValue(UPDATED_CLIENT_SECRET)
            .doesNotContainValue(storedClientSecret);
    }

    @Test
    void testUpdateOfAnUnknownIdentityProviderRecordsAFailure() {
        IdentityProvider identityProvider = newIdentityProvider("unknown");

        identityProvider.setId(Long.MAX_VALUE);

        assertThatThrownBy(() -> identityProviderFacade.update(identityProvider))
            .isInstanceOf(NoSuchElementException.class);

        assertThat(findSingleEvent(IdentityProviderAuditEvents.IDENTITY_PROVIDER_UPDATED).getData())
            .containsEntry("result", "ERROR")
            .containsEntry("errorClass", NoSuchElementException.class.getName())
            .containsEntry("identityProviderId", String.valueOf(Long.MAX_VALUE))
            .containsEntry("name", "unknown")
            .doesNotContainValue(CLIENT_SECRET);
    }

    @Test
    void testDeleteRecordsTheIdentityProviderId() {
        IdentityProvider identityProvider = identityProviderFacade.create(
            newIdentityProvider("identity-provider-" + UUID.randomUUID()));

        identityProviderFacade.delete(identityProvider.getId());

        assertThat(identityProviderRepository.findById(identityProvider.getId())).isEmpty();

        assertThat(findSingleEvent(IdentityProviderAuditEvents.IDENTITY_PROVIDER_DELETED).getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("identityProviderId", String.valueOf(identityProvider.getId()));
    }

    @Test
    void testDeleteOfAnUnknownIdentityProviderRecordsAFailure() {
        assertThatThrownBy(() -> identityProviderFacade.delete(Long.MAX_VALUE))
            .isInstanceOf(NoSuchElementException.class);

        assertThat(findSingleEvent(IdentityProviderAuditEvents.IDENTITY_PROVIDER_DELETED).getData())
            .containsEntry("result", "ERROR")
            .containsEntry("errorClass", NoSuchElementException.class.getName())
            .containsEntry("identityProviderId", String.valueOf(Long.MAX_VALUE));
    }

    private String getStoredClientSecret(long identityProviderId) {
        IdentityProvider identityProvider = identityProviderRepository.findById(identityProviderId)
            .orElseThrow();

        return identityProvider.getClientSecret();
    }

    private PersistentAuditEvent findSingleEvent(String eventType) {
        Page<PersistentAuditEvent> page = persistenceAuditEventRepository.findAllFiltered(
            AuditEventFilter.empty(), PageRequest.of(0, 100));

        List<PersistentAuditEvent> persistentAuditEvents = page.getContent()
            .stream()
            .filter(persistentAuditEvent -> eventType.equals(persistentAuditEvent.getEventType()))
            .toList();

        assertThat(persistentAuditEvents).hasSize(1);

        PersistentAuditEvent persistentAuditEvent = persistentAuditEvents.getFirst();

        assertThat(persistentAuditEvent.getPrincipal()).isEqualTo(ADMIN_LOGIN);
        assertThat(persistentAuditEvent.getPlatform()).isNull();

        return persistentAuditEvent;
    }

    private static IdentityProvider newIdentityProvider(String name) {
        IdentityProvider identityProvider = new IdentityProvider();

        identityProvider.setClientId("client-id");
        identityProvider.setClientSecret(CLIENT_SECRET);
        identityProvider.setDomains(Set.of());
        identityProvider.setEnabled(true);
        identityProvider.setIssuerUri("https://issuer.example.com");
        identityProvider.setName(name);

        return identityProvider;
    }

    @Configuration
    @EnableAutoConfiguration
    @EnableMethodSecurity
    @Import({
        AuditAspect.class, AuditCaptureAspect.class, AuditEventService.class, AuditMapperResolver.class,
        AuditJdbcConfiguration.class, EncryptionImpl.class, IdentityProviderFacadeImpl.class,
        IdentityProviderServiceImpl.class
    })
    static class AuditConfiguration {

        @Bean
        ApplicationProperties applicationProperties() {
            return new ApplicationProperties();
        }

        @Bean
        EncryptionKey encryptionKey() {
            return () -> "tTB1/UBIbYLuCXVi4PPfzA==";
        }
    }

    @EnableJdbcAuditing(auditorAwareRef = "auditorProvider", dateTimeProviderRef = "auditingDateTimeProvider")
    static class AuditJdbcConfiguration extends AbstractIntTestJdbcConfiguration {
    }
}
