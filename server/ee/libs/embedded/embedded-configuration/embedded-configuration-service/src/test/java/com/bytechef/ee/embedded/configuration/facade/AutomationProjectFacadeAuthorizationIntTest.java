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
import com.bytechef.automation.configuration.security.AutomationAuthorizationContext;
import com.bytechef.automation.configuration.security.AutomationMethodSecurityConfiguration;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectVersionWorkflowChangeService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.ee.automation.configuration.service.ProjectCodeWorkflowService;
import com.bytechef.ee.embedded.configuration.domain.AutomationProject;
import com.bytechef.ee.embedded.configuration.security.EmbeddedPermissionEvaluator;
import com.bytechef.ee.embedded.configuration.security.SkipAutomationAuthorizationAspect;
import com.bytechef.ee.embedded.configuration.service.AutomationProjectConfigurationService;
import com.bytechef.ee.embedded.configuration.service.AutomationProjectService;
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
import java.util.concurrent.atomic.AtomicBoolean;
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
        AutomationProjectFacadeImpl.class, SkipAutomationAuthorizationAspect.class,
        AutomationProjectFacadeAuthorizationIntTest.MethodSecurityConfiguration.class
    },
    properties = "bytechef.edition=ee")
@MockitoBean(types = {
    AutomationProjectConfigurationResolver.class, AutomationProjectConfigurationService.class,
    CategoryService.class, ConnectedUserReferenceDeploymentManager.class, ConnectedUserReferenceRolloutManager.class,
    ConnectedUserService.class, EmbeddedPermissionEvaluator.class, IntegrationInstanceConfigurationService.class,
    IntegrationService.class, ProjectCodeWorkflowService.class, ProjectWorkflowFacade.class,
    ProjectWorkflowService.class, TagService.class,
    WorkflowComponentResolver.class, WorkflowNodeTestOutputService.class, WorkflowPreDeleteListener.class,
    WorkflowService.class, WorkflowTestConfigurationService.class
})
class AutomationProjectFacadeAuthorizationIntTest {

    private static final String WORKFLOW_UUID = "c4b3a291-8f7e-4d5c-a6b7-e8f9a0b1c2d3";

    @Autowired
    private AutomationProjectFacade automationProjectFacade;

    @MockitoBean
    private AutomationProjectService automationProjectService;

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
        when(automationProjectService.fetchAutomationProject(5L))
            .thenReturn(Optional.of(new AutomationProject()));
        when(projectVersionWorkflowChangeService.getVersionWorkflowChanges(5L, 1))
            .thenReturn(versionWorkflowChanges);

        authenticate(tenantAdmin());

        assertThat(automationProjectFacade.getProjectVersionWorkflowChanges(5L, 1))
            .isSameAs(versionWorkflowChanges);
    }

    @Test
    void testAdminMethodsRequireTenantAdmin() {
        for (Authentication authentication : List.of(nonAdmin(), connectedUser())) {
            authenticate(authentication);

            assertThatThrownBy(() -> automationProjectFacade.createProject("name", "", null, List.of(), null))
                .as("create denied to %s", authentication.getName())
                .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> automationProjectFacade.deleteProject(5L))
                .as("delete denied to %s", authentication.getName())
                .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> automationProjectFacade.publishProject(5L, null))
                .as("publish denied to %s", authentication.getName())
                .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> automationProjectFacade.getProjects())
                .as("projects denied to %s", authentication.getName())
                .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> automationProjectFacade.getProjectVersionWorkflowChanges(5L, 1))
                .as("changes denied to %s", authentication.getName())
                .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(
                () -> automationProjectFacade.getProjectVersionWorkflowChange(5L, 1, WORKFLOW_UUID))
                    .as("change denied to %s", authentication.getName())
                    .isInstanceOf(AccessDeniedException.class);
        }

        verifyNoInteractions(automationProjectService, projectService, projectVersionWorkflowChangeService);
    }

    @Test
    void testGetPublishedProjectsRunsSkippingChecksWithoutTenantAdmin() {
        AtomicBoolean skippingChecks = new AtomicBoolean();

        when(automationProjectService.getAutomationProjects())
            .thenAnswer(invocation -> {
                skippingChecks.set(AutomationAuthorizationContext.isSkipChecks());

                return List.of();
            });

        authenticate(nonAdmin());

        assertThat(automationProjectFacade.getPublishedProjects()).isEmpty();
        assertThat(skippingChecks).isTrue();
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
