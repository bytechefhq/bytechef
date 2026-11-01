/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.security.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.repository.ProjectRepository;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.automation.configuration.repository.WorkspaceUserRepository;
import com.bytechef.ee.automation.configuration.service.PermissionScopeRegistry;
import com.bytechef.ee.automation.configuration.service.WorkspaceScopeCacheService;
import com.bytechef.ee.embedded.security.audit.SigningKeyAuditEvents;
import com.bytechef.ee.embedded.security.config.EmbeddedSecurityIntTestConfiguration;
import com.bytechef.ee.embedded.security.domain.SigningKey;
import com.bytechef.ee.embedded.security.repository.SigningKeyRepository;
import com.bytechef.ee.platform.audit.aspect.AuditAspect;
import com.bytechef.ee.platform.audit.aspect.AuditCaptureAspect;
import com.bytechef.ee.platform.audit.aspect.AuditMapperResolver;
import com.bytechef.ee.platform.audit.domain.AuditEventFilter;
import com.bytechef.ee.platform.audit.domain.PersistentAuditEvent;
import com.bytechef.ee.platform.audit.repository.PersistenceAuditEventRepository;
import com.bytechef.ee.platform.audit.service.AuditEventService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = {
        EmbeddedSecurityIntTestConfiguration.class, SigningKeyFacadeAuditIntTest.AuditConfiguration.class
    },
    properties = {
        "bytechef.edition=ee",
        "spring.liquibase.change-log=classpath:config/liquibase/embedded-security-audit-testint.xml",
        "spring.liquibase.contexts=configuration"
    })
@Import(PostgreSQLContainerConfiguration.class)
@MockitoBean(types = {
    PermissionScopeRegistry.class, ProjectRepository.class, UserService.class, WorkspaceScopeCacheService.class,
    WorkspaceUserRepository.class
})
class SigningKeyFacadeAuditIntTest {

    private static final String ADMIN_LOGIN = "admin";
    private static final long ADMIN_USER_ID = 1001L;

    @Autowired
    private PersistenceAuditEventRepository persistenceAuditEventRepository;

    @Autowired
    private SigningKeyFacade signingKeyFacade;

    @Autowired
    private SigningKeyRepository signingKeyRepository;

    @Autowired
    private UserService userService;

    @BeforeEach
    void beforeEach() {
        User user = new User();

        user.setId(ADMIN_USER_ID);
        user.setLogin(ADMIN_LOGIN);

        when(userService.getCurrentUser()).thenReturn(user);

        SecurityContext securityContext = SecurityContextHolder.getContext();

        securityContext.setAuthentication(
            new UsernamePasswordAuthenticationToken(
                ADMIN_LOGIN, "n/a", List.of(new SimpleGrantedAuthority(AuthorityConstants.ADMIN))));

        persistenceAuditEventRepository.deleteAll();
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();

        signingKeyRepository.deleteAll();
        persistenceAuditEventRepository.deleteAll();
    }

    @Test
    void testCreateRecordsTheKeyWithoutTheGeneratedPrivateKey() {
        String name = "audit-key-" + UUID.randomUUID();

        SigningKey signingKey = newSigningKey(name);

        String privateKey = signingKeyFacade.create(signingKey, PlatformType.EMBEDDED);

        PersistentAuditEvent persistentAuditEvent = findSingleEvent(SigningKeyAuditEvents.SIGNING_KEY_CREATED);

        assertThat(signingKey.getId()).isNotNull();
        assertThat(persistentAuditEvent.getPrincipal()).isEqualTo(ADMIN_LOGIN);
        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("signingKeyId", String.valueOf(signingKey.getId()))
            .containsEntry("name", name)
            .containsEntry("environment", Environment.PRODUCTION.name())
            .containsEntry("type", PlatformType.EMBEDDED.name())
            .doesNotContainValue(privateKey);
    }

    @Test
    void testUpdateRecordsTheRenamedKey() {
        SigningKey signingKey = saveSigningKey();

        SigningKey renamedSigningKey = new SigningKey();

        renamedSigningKey.setId(signingKey.getId());
        renamedSigningKey.setName("renamed");

        signingKeyFacade.update(renamedSigningKey);

        PersistentAuditEvent persistentAuditEvent = findSingleEvent(SigningKeyAuditEvents.SIGNING_KEY_UPDATED);

        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("signingKeyId", String.valueOf(signingKey.getId()))
            .containsEntry("name", "renamed");
    }

    @Test
    void testDeleteRecordsTheKeyId() {
        SigningKey signingKey = saveSigningKey();

        signingKeyFacade.delete(signingKey.getId());

        PersistentAuditEvent persistentAuditEvent = findSingleEvent(SigningKeyAuditEvents.SIGNING_KEY_DELETED);

        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("signingKeyId", String.valueOf(signingKey.getId()));
    }

    @Test
    void testDeleteOfAnUnknownKeyRecordsAFailure() {
        assertThatThrownBy(() -> signingKeyFacade.delete(Long.MAX_VALUE))
            .isInstanceOf(NoSuchElementException.class);

        PersistentAuditEvent persistentAuditEvent = findSingleEvent(SigningKeyAuditEvents.SIGNING_KEY_DELETED);

        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "ERROR")
            .containsEntry("errorClass", NoSuchElementException.class.getName())
            .containsEntry("signingKeyId", String.valueOf(Long.MAX_VALUE));
    }

    private PersistentAuditEvent findSingleEvent(String eventType) {
        Page<PersistentAuditEvent> page = persistenceAuditEventRepository.findAllFiltered(
            AuditEventFilter.empty(), PageRequest.of(0, 25));

        List<PersistentAuditEvent> persistentAuditEvents = page.getContent()
            .stream()
            .filter(persistentAuditEvent -> eventType.equals(persistentAuditEvent.getEventType()))
            .toList();

        assertThat(persistentAuditEvents).hasSize(1);

        return persistentAuditEvents.getFirst();
    }

    private static SigningKey newSigningKey(String name) {
        SigningKey signingKey = new SigningKey();

        signingKey.setEnvironment(Environment.PRODUCTION);
        signingKey.setName(name);

        return signingKey;
    }

    private SigningKey saveSigningKey() {
        SigningKey signingKey = newSigningKey("saved-key-" + UUID.randomUUID());

        signingKey.setKeyId(UUID.randomUUID()
            .toString());
        signingKey.setPublicKey("public-key");
        signingKey.setType(PlatformType.EMBEDDED);
        signingKey.setUserId(ADMIN_USER_ID);

        return signingKeyRepository.save(signingKey);
    }

    @Configuration
    @Import({
        AuditAspect.class, AuditCaptureAspect.class, AuditEventService.class, AuditMapperResolver.class
    })
    static class AuditConfiguration {

        @Bean
        ApplicationProperties applicationProperties() {
            return new ApplicationProperties();
        }
    }
}
