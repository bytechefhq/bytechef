/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.facade;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.facade.ProjectFacade;
import com.bytechef.automation.configuration.facade.ProjectWorkflowFacade;
import com.bytechef.automation.configuration.security.AutomationMethodSecurityConfiguration;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.ee.automation.configuration.service.ProjectGitConfigurationService;
import com.bytechef.ee.automation.configuration.service.ProjectGitService;
import com.bytechef.ee.automation.configuration.service.WorkspaceService;
import com.bytechef.ee.platform.configuration.facade.GitConfigurationFacade;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Proves the Git guards are enforced on the real proxied {@link ProjectGitFacadeImpl} bean, not merely present as
 * annotation text — {@link ProjectGitFacadeAuthorizationTest} covers the expressions themselves.
 * <p>
 * Each guard is exercised both ways. The allow direction stubs the first collaborator the method body touches to throw
 * a sentinel, so a passing allow test proves the body was entered; the deny direction then proves the sentinel was
 * never reached, which is what rules out the guard having been satisfied by something other than the scope.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = ProjectGitFacadeEnforcementIntTest.Config.class)
class ProjectGitFacadeEnforcementIntTest {

    private static final String BODY_REACHED = "body reached";
    private static final long PROJECT_ID = 42L;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private ProjectGitFacade projectGitFacade;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private WorkspaceService workspaceService;

    @BeforeEach
    void authenticateAsNonAdmin() {
        SecurityContextHolder.getContext()
            .setAuthentication(
                new UsernamePasswordAuthenticationToken(
                    "viewer", "n/a", List.of(new SimpleGrantedAuthority("ROLE_USER"))));

        // Re-established each test because the mocks are shared through the cached Spring context: a grant left behind
        // by an allow test would make the next deny test pass for the wrong reason.
        reset(permissionService, projectService, workspaceService);

        when(permissionService.isTenantAdmin()).thenReturn(false);
        when(permissionService.hasResourceScope(any(), anyString(), anyString())).thenReturn(false);

        when(workspaceService.getProjectWorkspace(anyLong())).thenThrow(new IllegalStateException(BODY_REACHED));
        when(projectService.getProject(anyLong())).thenThrow(new IllegalStateException(BODY_REACHED));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testPullProjectFromGitIsDeniedWithoutTheProjectPullScope() {
        assertThatThrownBy(() -> projectGitFacade.pullProjectFromGit(PROJECT_ID))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testPullProjectFromGitIsAllowedWithTheProjectPullScope() {
        grant("PROJECT_PULL");

        assertThatThrownBy(() -> projectGitFacade.pullProjectFromGit(PROJECT_ID))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage(BODY_REACHED);
    }

    @Test
    void testPullProjectFromGitIsDeniedWhenOnlyThePushScopeIsHeld() {
        grant("PROJECT_PUSH");

        assertThatThrownBy(() -> projectGitFacade.pullProjectFromGit(PROJECT_ID))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testPushProjectToGitIsDeniedWithoutTheProjectPushScope() {
        assertThatThrownBy(() -> projectGitFacade.pushProjectToGit(PROJECT_ID, "Update workflows"))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testPushProjectToGitIsAllowedWithTheProjectPushScope() {
        grant("PROJECT_PUSH");

        assertThatThrownBy(() -> projectGitFacade.pushProjectToGit(PROJECT_ID, "Update workflows"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage(BODY_REACHED);
    }

    private void grant(String scope) {
        when(permissionService.hasResourceScope(PROJECT_ID, "Project", scope)).thenReturn(true);
    }

    // @SpringBootConfiguration (not @TestConfiguration) because @SpringBootTest(classes = Config.class) requires a
    // primary Spring Boot configuration class. The facade is built by hand so that only the collaborators these
    // enforcement tests stub need to be beans.
    @SpringBootConfiguration
    @EnableMethodSecurity
    @ImportAutoConfiguration(AutomationMethodSecurityConfiguration.class)
    static class Config {

        @Bean("permissionService")
        PermissionService permissionService() {
            return mock(PermissionService.class);
        }

        @Bean
        ProjectService projectService() {
            return mock(ProjectService.class);
        }

        @Bean
        WorkspaceService workspaceService() {
            return mock(WorkspaceService.class);
        }

        @Bean
        ProjectGitFacade projectGitFacade(ProjectService projectService, WorkspaceService workspaceService) {
            return new ProjectGitFacadeImpl(
                mock(GitConfigurationFacade.class), mock(ProjectFacade.class),
                mock(ProjectGitConfigurationService.class), mock(ProjectGitService.class), projectService,
                mock(ProjectWorkflowFacade.class), mock(ProjectWorkflowService.class), mock(WorkflowService.class),
                workspaceService);
        }
    }
}
