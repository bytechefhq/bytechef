/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.security.AutomationMethodSecurityConfiguration;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.embedded.security.web.authentication.EmbeddedApiKeyAuthenticationToken;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.dto.VersionWorkflowChanges;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.security.util.SecurityUtils;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = {
        AutomationWorkflowProjectAdminFacadeImpl.class,
        AutomationWorkflowProjectAdminFacadeAuthorizationIntTest.MethodSecurityConfiguration.class
    },
    properties = "bytechef.edition=ee")
class AutomationWorkflowProjectAdminFacadeAuthorizationIntTest {

    private static final String WORKFLOW_UUID = "c4b3a291-8f7e-4d5c-a6b7-e8f9a0b1c2d3";

    @Autowired
    private AutomationWorkflowProjectAdminFacade automationWorkflowProjectAdminFacade;

    @MockitoBean
    private AutomationWorkflowProjectFacade automationWorkflowProjectFacade;

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testGetProjectVersionWorkflowChangesDelegatesForTenantAdmin() {
        VersionWorkflowChanges versionWorkflowChanges = new VersionWorkflowChanges(null, 1, List.of());

        when(automationWorkflowProjectFacade.getProjectVersionWorkflowChanges(5L, 1))
            .thenReturn(versionWorkflowChanges);

        authenticate(tenantAdmin());

        assertThat(automationWorkflowProjectAdminFacade.getProjectVersionWorkflowChanges(5L, 1))
            .isSameAs(versionWorkflowChanges);
    }

    @Test
    void testProjectVersionWorkflowChangesRequireTenantAdmin() {
        for (Authentication authentication : List.of(nonAdmin(), connectedUser())) {
            authenticate(authentication);

            assertThatThrownBy(() -> automationWorkflowProjectAdminFacade.getProjectVersionWorkflowChanges(5L, 1))
                .as("changes denied to %s", authentication.getName())
                .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(
                () -> automationWorkflowProjectAdminFacade.getProjectVersionWorkflowChange(5L, 1, WORKFLOW_UUID))
                    .as("change denied to %s", authentication.getName())
                    .isInstanceOf(AccessDeniedException.class);
        }

        verifyNoInteractions(automationWorkflowProjectFacade);
    }

    private static void authenticate(Authentication authentication) {
        SecurityContextHolder.setContext(new SecurityContextImpl(authentication));
    }

    private static Authentication connectedUser() {
        return new EmbeddedApiKeyAuthenticationToken(
            Environment.PRODUCTION.ordinal(), 1L, new User("external-user-1", "", List.of()), false);
    }

    private static Authentication nonAdmin() {
        return new UsernamePasswordAuthenticationToken(
            "user", "n/a", List.of(new SimpleGrantedAuthority(AuthorityConstants.USER)));
    }

    private static Authentication tenantAdmin() {
        return new UsernamePasswordAuthenticationToken(
            "admin", "n/a",
            List.of(
                new SimpleGrantedAuthority(AuthorityConstants.ADMIN),
                new SimpleGrantedAuthority(AuthorityConstants.USER)));
    }

    @Configuration
    @EnableMethodSecurity
    @ImportAutoConfiguration(AutomationMethodSecurityConfiguration.class)
    static class MethodSecurityConfiguration {

        @Bean("permissionService")
        PermissionService permissionService() {
            PermissionService permissionService = mock(PermissionService.class);

            when(permissionService.isTenantAdmin())
                .thenAnswer(invocation -> SecurityUtils.hasCurrentUserThisAuthority(AuthorityConstants.ADMIN));

            return permissionService;
        }
    }
}
