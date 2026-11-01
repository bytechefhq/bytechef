/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.connected.user.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.security.AutomationMethodSecurityConfiguration;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserProjectFacade;
import com.bytechef.ee.embedded.configuration.facade.IntegrationInstanceFacade;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserConnectionService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.ee.embedded.connected.user.audit.ConnectedUserAuditEvents;
import com.bytechef.ee.embedded.connected.user.config.ConnectedUserIntTestConfiguration;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.dto.ConnectedUserDTO;
import com.bytechef.ee.embedded.connected.user.dto.ConnectedUserImportResultDTO;
import com.bytechef.ee.embedded.connected.user.repository.ConnectedUserRepository;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.ee.platform.audit.aspect.AuditAspect;
import com.bytechef.ee.platform.audit.aspect.AuditCaptureAspect;
import com.bytechef.ee.platform.audit.aspect.AuditMapperResolver;
import com.bytechef.ee.platform.audit.domain.AuditEventFilter;
import com.bytechef.ee.platform.audit.domain.PersistentAuditEvent;
import com.bytechef.ee.platform.audit.repository.PersistenceAuditEventRepository;
import com.bytechef.ee.platform.audit.service.AuditEventService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.connection.facade.ConnectionFacade;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.security.util.SecurityUtils;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = {
        ConnectedUserIntTestConfiguration.class, ConnectedUserFacadeImpl.class,
        ConnectedUserFacadeAuditIntTest.AuditConfiguration.class
    },
    properties = {
        "bytechef.edition=ee", "spring.liquibase.contexts=user,connection"
    })
@Import(PostgreSQLContainerConfiguration.class)
@MockitoBean(types = {
    ConnectedUserConnectionService.class, ConnectedUserProjectFacade.class, ConnectionFacade.class,
    ConnectionService.class, EnvironmentService.class, IntegrationInstanceFacade.class,
    IntegrationInstanceService.class, IntegrationInstanceConfigurationService.class, IntegrationService.class
})
class ConnectedUserFacadeAuditIntTest {

    private static final String ADMIN_LOGIN = "admin";

    @Autowired
    private ConnectedUserFacade connectedUserFacade;

    @Autowired
    private ConnectedUserRepository connectedUserRepository;

    @Autowired
    private ConnectedUserService connectedUserService;

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

        connectedUserRepository.deleteAll();
        persistenceAuditEventRepository.deleteAll();
    }

    @Test
    void testCreateConnectedUserRecordsTheConnectedUser() {
        String externalId = "external-" + UUID.randomUUID();

        ConnectedUserDTO connectedUserDTO = connectedUserFacade.createConnectedUser(
            externalId, Environment.DEVELOPMENT, "Name", "user@example.com", Map.of("plan", "pro"));

        assertThat(findSingleEvent(ConnectedUserAuditEvents.CONNECTED_USER_CREATED).getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("connectedUserId", String.valueOf(connectedUserDTO.id()))
            .containsEntry("externalId", externalId)
            .containsEntry("environment", Environment.DEVELOPMENT.name());
    }

    @Test
    void testCreateWithAnInvalidExternalIdRecordsAFailure() {
        String externalId = " invalid";

        assertThatThrownBy(
            () -> connectedUserFacade.createConnectedUser(externalId, Environment.DEVELOPMENT, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(findSingleEvent(ConnectedUserAuditEvents.CONNECTED_USER_CREATED).getData())
            .containsEntry("result", "ERROR")
            .containsEntry("errorClass", IllegalArgumentException.class.getName())
            .containsEntry("externalId", externalId)
            .containsEntry("environment", Environment.DEVELOPMENT.name())
            .doesNotContainKey("connectedUserId");
    }

    @Test
    void testUpdateConnectedUserRecordsTheConnectedUser() {
        ConnectedUser connectedUser = createConnectedUser();

        ConnectedUserDTO connectedUserDTO = connectedUserFacade.updateConnectedUser(
            connectedUser.getId(), "Renamed", null, Map.of(), connectedUser.getVersion());

        assertThat(connectedUserDTO.name()).isEqualTo("Renamed");

        assertThat(findSingleEvent(ConnectedUserAuditEvents.CONNECTED_USER_UPDATED).getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("connectedUserId", String.valueOf(connectedUser.getId()))
            .containsEntry("externalId", connectedUser.getExternalId());
    }

    @Test
    void testUpdateWithAStaleVersionRecordsAFailure() {
        ConnectedUser connectedUser = createConnectedUser();

        long connectedUserId = connectedUser.getId();
        int staleVersion = connectedUser.getVersion() + 1;

        assertThatThrownBy(
            () -> connectedUserFacade.updateConnectedUser(connectedUserId, "Renamed", null, Map.of(), staleVersion))
                .isInstanceOf(OptimisticLockingFailureException.class);

        assertThat(findSingleEvent(ConnectedUserAuditEvents.CONNECTED_USER_UPDATED).getData())
            .containsEntry("result", "ERROR")
            .containsEntry("errorClass", OptimisticLockingFailureException.class.getName())
            .containsEntry("connectedUserId", String.valueOf(connectedUserId))
            .doesNotContainKey("externalId");
    }

    @Test
    void testDeleteConnectedUserRecordsTheConnectedUserId() {
        ConnectedUser connectedUser = createConnectedUser();

        connectedUserFacade.deleteConnectedUser(connectedUser.getId());

        assertThat(connectedUserRepository.findById(connectedUser.getId())).isEmpty();

        assertThat(findSingleEvent(ConnectedUserAuditEvents.CONNECTED_USER_DELETED).getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("connectedUserId", String.valueOf(connectedUser.getId()));
    }

    @Test
    void testEnableConnectedUserRecordsTheNewState() {
        ConnectedUser connectedUser = createConnectedUser();

        connectedUserFacade.enableConnectedUser(connectedUser.getId(), false);

        assertThat(connectedUserService.getConnectedUser(connectedUser.getId())
            .isEnabled()).isFalse();

        assertThat(findSingleEvent(ConnectedUserAuditEvents.CONNECTED_USER_ENABLED_UPDATED).getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("connectedUserId", String.valueOf(connectedUser.getId()))
            .containsEntry("enabled", "false");
    }

    @Test
    void testEnableOfAnUnknownConnectedUserRecordsAFailure() {
        assertThatThrownBy(() -> connectedUserFacade.enableConnectedUser(Long.MAX_VALUE, true))
            .isInstanceOf(NoSuchElementException.class);

        assertThat(findSingleEvent(ConnectedUserAuditEvents.CONNECTED_USER_ENABLED_UPDATED).getData())
            .containsEntry("result", "ERROR")
            .containsEntry("errorClass", NoSuchElementException.class.getName())
            .containsEntry("connectedUserId", String.valueOf(Long.MAX_VALUE))
            .containsEntry("enabled", "true");
    }

    @Test
    void testImportConnectedUsersRecordsTheCounts() {
        connectedUserService.createConnectedUser("existing", Environment.DEVELOPMENT, null, null, Map.of());

        ConnectedUserImportResultDTO connectedUserImportResultDTO = connectedUserFacade.importConnectedUsers(
            Environment.DEVELOPMENT, csv("externalId,name\nfirst,First\nsecond,Second\nexisting,E\n\" bad\",X\n"));

        assertThat(connectedUserImportResultDTO.createdCount()).isEqualTo(2);

        assertThat(findSingleEvent(ConnectedUserAuditEvents.CONNECTED_USERS_IMPORTED).getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("environment", Environment.DEVELOPMENT.name())
            .containsEntry("createdCount", "2")
            .containsEntry("skippedCount", "1")
            .containsEntry("rejectedCount", "1");
    }

    @Test
    void testImportOfAnInvalidFileRecordsAFailure() {
        assertThatThrownBy(() -> connectedUserFacade.importConnectedUsers(Environment.DEVELOPMENT, csv("")))
            .isInstanceOf(IllegalArgumentException.class);

        assertThat(findSingleEvent(ConnectedUserAuditEvents.CONNECTED_USERS_IMPORTED).getData())
            .containsEntry("result", "ERROR")
            .containsEntry("errorClass", IllegalArgumentException.class.getName())
            .containsEntry("environment", Environment.DEVELOPMENT.name())
            .doesNotContainKeys("createdCount", "skippedCount", "rejectedCount");
    }

    private ConnectedUser createConnectedUser() {
        return connectedUserService.createConnectedUser(
            "external-" + UUID.randomUUID(), Environment.DEVELOPMENT, "Name", null, Map.of());
    }

    private static ByteArrayInputStream csv(String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
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
        assertThat(persistentAuditEvent.getPlatform()).isEqualTo(PlatformType.EMBEDDED);

        return persistentAuditEvent;
    }

    @Configuration
    @EnableMethodSecurity
    @ImportAutoConfiguration(AutomationMethodSecurityConfiguration.class)
    @Import({
        AuditAspect.class, AuditCaptureAspect.class, AuditEventService.class, AuditMapperResolver.class
    })
    static class AuditConfiguration {

        @Bean
        ApplicationProperties applicationProperties() {
            return new ApplicationProperties();
        }

        @Bean("permissionService")
        PermissionService permissionService() {
            PermissionService permissionService = mock(PermissionService.class);

            when(permissionService.isTenantAdmin())
                .thenAnswer(invocation -> SecurityUtils.hasCurrentUserThisAuthority(AuthorityConstants.ADMIN));

            return permissionService;
        }
    }
}
