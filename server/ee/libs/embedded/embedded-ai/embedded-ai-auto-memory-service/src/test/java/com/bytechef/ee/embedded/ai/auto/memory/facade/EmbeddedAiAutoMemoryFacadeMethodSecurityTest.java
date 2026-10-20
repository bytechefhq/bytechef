/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.auto.memory.facade;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.security.AutomationMethodSecurityConfiguration;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.automation.configuration.service.ProjectOwnershipService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserProjectService;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserReferenceDeploymentService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryPatch;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryPrincipalType;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.security.util.SecurityUtils;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringJUnitConfig(EmbeddedAiAutoMemoryFacadeMethodSecurityTest.MethodSecurityConfiguration.class)
@TestPropertySource(properties = "bytechef.edition=ee")
class EmbeddedAiAutoMemoryFacadeMethodSecurityTest {

    private static final AiAutoMemoryPatch PATCH = new AiAutoMemoryPatch("Title", null, null, null);

    @MockitoBean
    private AiAutoMemoryService aiAutoMemoryService;

    @MockitoBean
    private ConnectedUserProjectService connectedUserProjectService;

    @MockitoBean
    private ConnectedUserReferenceDeploymentService connectedUserReferenceDeploymentService;

    @MockitoBean
    private ConnectedUserService connectedUserService;

    @Autowired
    private EmbeddedAiAutoMemoryFacade embeddedAiAutoMemoryFacade;

    @MockitoBean
    private IntegrationInstanceConfigurationService integrationInstanceConfigurationService;

    @MockitoBean
    private IntegrationInstanceService integrationInstanceService;

    @MockitoBean
    private IntegrationService integrationService;

    @MockitoBean
    private ProjectOwnershipService projectOwnershipService;

    @MockitoBean
    private ProjectService projectService;

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testUnauthenticatedCallerIsDenied() {
        assertThatExceptionOfType(AccessDeniedException.class)
            .isThrownBy(() -> embeddedAiAutoMemoryFacade.getEmbeddedAiAutoMemories(Environment.DEVELOPMENT, null,
                null));
        assertThatExceptionOfType(AccessDeniedException.class)
            .isThrownBy(() -> embeddedAiAutoMemoryFacade.getEmbeddedAiAutoMemoryConnectedUsers(
                Environment.DEVELOPMENT));
        assertThatExceptionOfType(AuthenticationCredentialsNotFoundException.class)
            .isThrownBy(() -> embeddedAiAutoMemoryFacade.updateEmbeddedAiAutoMemory(
                Environment.DEVELOPMENT, AiAutoMemoryPrincipalType.INTEGRATION_INSTANCE, 1L, 1L, 1L, PATCH));
        assertThatExceptionOfType(AuthenticationCredentialsNotFoundException.class)
            .isThrownBy(() -> embeddedAiAutoMemoryFacade.deleteEmbeddedAiAutoMemory(
                Environment.DEVELOPMENT, AiAutoMemoryPrincipalType.INTEGRATION_INSTANCE, 1L, 1L));
    }

    @Test
    void testNonAdminIsDenied() {
        authenticate(AuthorityConstants.USER);

        assertThatExceptionOfType(AccessDeniedException.class)
            .isThrownBy(() -> embeddedAiAutoMemoryFacade.getEmbeddedAiAutoMemories(Environment.DEVELOPMENT, null,
                null));
        assertThatExceptionOfType(AccessDeniedException.class)
            .isThrownBy(() -> embeddedAiAutoMemoryFacade.getEmbeddedAiAutoMemoryConnectedUsers(
                Environment.DEVELOPMENT));
        assertThatExceptionOfType(AccessDeniedException.class)
            .isThrownBy(() -> embeddedAiAutoMemoryFacade.updateEmbeddedAiAutoMemory(
                Environment.DEVELOPMENT, AiAutoMemoryPrincipalType.INTEGRATION_INSTANCE, 1L, 1L, 1L, PATCH));
        assertThatExceptionOfType(AccessDeniedException.class)
            .isThrownBy(() -> embeddedAiAutoMemoryFacade.deleteEmbeddedAiAutoMemory(
                Environment.DEVELOPMENT, AiAutoMemoryPrincipalType.INTEGRATION_INSTANCE, 1L, 1L));
        assertThatExceptionOfType(AccessDeniedException.class)
            .isThrownBy(() -> embeddedAiAutoMemoryFacade.updateEmbeddedAiAutoMemory(
                Environment.DEVELOPMENT, AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT, 1L, 1L, 1L, PATCH));
        assertThatExceptionOfType(AccessDeniedException.class)
            .isThrownBy(() -> embeddedAiAutoMemoryFacade.deleteEmbeddedAiAutoMemory(
                Environment.DEVELOPMENT, AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT, 1L, 1L));
    }

    @Test
    void testEmbeddedConnectedUserViewerCanList() {
        authenticate(AuthorityConstants.ADMIN, AuthorityConstants.USER);

        assertThatCode(() -> embeddedAiAutoMemoryFacade.getEmbeddedAiAutoMemories(Environment.DEVELOPMENT, null, null))
            .doesNotThrowAnyException();
        assertThatCode(() -> embeddedAiAutoMemoryFacade.getEmbeddedAiAutoMemoryConnectedUsers(Environment.DEVELOPMENT))
            .doesNotThrowAnyException();
    }

    private static void authenticate(String... authorities) {
        SecurityContext securityContext = SecurityContextHolder.getContext();

        securityContext.setAuthentication(
            new UsernamePasswordAuthenticationToken(
                "user", "n/a",
                List.of(authorities)
                    .stream()
                    .map(SimpleGrantedAuthority::new)
                    .toList()));
    }

    @Configuration
    @EnableMethodSecurity
    @ImportAutoConfiguration(AutomationMethodSecurityConfiguration.class)
    @Import(EmbeddedAiAutoMemoryFacadeImpl.class)
    static class MethodSecurityConfiguration {

        @Bean("permissionService")
        PermissionService permissionService() {
            PermissionService permissionService = mock(PermissionService.class);

            when(permissionService.hasEmbeddedScope(eq("CONNECTED_USER_VIEW"), any(Environment.class)))
                .thenAnswer(invocation -> SecurityUtils.hasCurrentUserThisAuthority(AuthorityConstants.ADMIN));

            return permissionService;
        }
    }
}
