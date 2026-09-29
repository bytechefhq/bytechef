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

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.facade.ProjectWorkflowFacade;
import com.bytechef.automation.configuration.security.AutomationMethodSecurityConfiguration;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectVersionWorkflowChangeService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.ee.embedded.configuration.domain.AutomationWorkflowProject;
import com.bytechef.ee.embedded.configuration.security.EmbeddedPermissionEvaluator;
import com.bytechef.ee.embedded.configuration.service.AutomationWorkflowProjectConfigurationService;
import com.bytechef.ee.embedded.configuration.service.AutomationWorkflowProjectService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.ee.embedded.security.web.authentication.EmbeddedApiKeyAuthenticationToken;
import com.bytechef.platform.category.service.CategoryService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.dto.VersionWorkflowChanges;
import com.bytechef.platform.configuration.service.WorkflowNodeTestOutputService;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.configuration.workflow.WorkflowPreDeleteListener;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.security.util.SecurityUtils;
import com.bytechef.platform.tag.service.TagService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
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
        AutomationWorkflowProjectFacadeImpl.class,
        AutomationWorkflowProjectFacadeAuthorizationIntTest.MethodSecurityConfiguration.class
    },
    properties = "bytechef.edition=ee")
@MockitoBean(types = {
    AutomationWorkflowProjectConfigurationResolver.class, AutomationWorkflowProjectConfigurationService.class,
    CategoryService.class, ConnectedUserReferenceDeploymentManager.class, ConnectedUserReferenceRolloutManager.class,
    ConnectedUserService.class, EmbeddedPermissionEvaluator.class, IntegrationInstanceConfigurationService.class,
    IntegrationService.class, ProjectWorkflowFacade.class, ProjectWorkflowService.class, TagService.class,
    WorkflowComponentResolver.class, WorkflowNodeTestOutputService.class, WorkflowPreDeleteListener.class,
    WorkflowService.class, WorkflowTestConfigurationService.class
})
class AutomationWorkflowProjectFacadeAuthorizationIntTest {

    private static final String WORKFLOW_UUID = "c4b3a291-8f7e-4d5c-a6b7-e8f9a0b1c2d3";

    @Autowired
    private AutomationWorkflowProjectFacade automationWorkflowProjectFacade;

    @MockitoBean
    private AutomationWorkflowProjectService automationWorkflowProjectService;

    @MockitoBean
    private ProjectService projectService;

    @MockitoBean
    private ProjectVersionWorkflowChangeService projectVersionWorkflowChangeService;

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testGetProjectVersionWorkflowChangesDelegatesForTenantAdmin() {
        VersionWorkflowChanges versionWorkflowChanges = new VersionWorkflowChanges(null, 1, List.of());

        when(projectService.getProject(5L)).thenReturn(new Project());
        when(automationWorkflowProjectService.fetchAutomationWorkflowProject(5L))
            .thenReturn(Optional.of(new AutomationWorkflowProject()));
        when(projectVersionWorkflowChangeService.getVersionWorkflowChanges(5L, 1))
            .thenReturn(versionWorkflowChanges);

        authenticate(tenantAdmin());

        assertThat(automationWorkflowProjectFacade.getProjectVersionWorkflowChanges(5L, 1))
            .isSameAs(versionWorkflowChanges);
    }

    @Test
    void testAdminMethodsRequireTenantAdmin() {
        for (Authentication authentication : List.of(nonAdmin(), connectedUser())) {
            authenticate(authentication);

            assertThatThrownBy(() -> automationWorkflowProjectFacade.createProject("name", "", null, List.of(), null))
                .as("create denied to %s", authentication.getName())
                .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> automationWorkflowProjectFacade.deleteProject(5L))
                .as("delete denied to %s", authentication.getName())
                .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> automationWorkflowProjectFacade.publishProject(5L, null))
                .as("publish denied to %s", authentication.getName())
                .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> automationWorkflowProjectFacade.getProjects())
                .as("projects denied to %s", authentication.getName())
                .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> automationWorkflowProjectFacade.getProjectVersionWorkflowChanges(5L, 1))
                .as("changes denied to %s", authentication.getName())
                .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(
                () -> automationWorkflowProjectFacade.getProjectVersionWorkflowChange(5L, 1, WORKFLOW_UUID))
                    .as("change denied to %s", authentication.getName())
                    .isInstanceOf(AccessDeniedException.class);
        }

        verifyNoInteractions(automationWorkflowProjectService, projectService, projectVersionWorkflowChangeService);
    }

    @Test
    void testGetPublishedProjectsRunsWithoutTenantAdmin() {
        when(automationWorkflowProjectService.getAutomationWorkflowProjects()).thenReturn(List.of());

        authenticate(nonAdmin());

        assertThat(automationWorkflowProjectFacade.getPublishedProjects()).isEmpty();
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
    @EnableAspectJAutoProxy
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
