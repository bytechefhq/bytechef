/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.automation.security;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.dto.ProjectDeploymentDTO;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacadeImpl;
import com.bytechef.automation.configuration.security.AutomationMethodSecurityConfiguration;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.facade.ComponentConnectionFacade;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.tag.domain.Tag;
import com.bytechef.platform.tag.service.TagService;
import com.bytechef.platform.workflow.execution.facade.PrincipalJobFacade;
import com.bytechef.platform.workflow.execution.facade.TriggerLifecycleFacade;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import com.bytechef.platform.workflow.execution.service.TriggerExecutionService;
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
 * Lives outside {@code com.bytechef.automation.configuration} on purpose: {@code ProjectIntTestConfiguration} component
 * scans that package, so a nested {@code @SpringBootConfiguration} inside it would be swept into every other
 * integration test's context and replace the real {@code PermissionService} with this mock.
 * <p>
 * Proves the new deployment guards are enforced on the real proxied {@code ProjectDeploymentFacadeImpl} bean, not
 * merely present as annotation text — {@code ProjectDeploymentGuardExpressionTest} covers the expressions themselves.
 * <p>
 * Each guard is exercised both ways. The allow direction stubs the first collaborator the method body touches to throw
 * a sentinel, so a passing allow test proves the body was entered rather than that some later stub happened to return
 * null; the deny direction then proves the sentinel was never reached.
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = ProjectDeploymentFacadeEnforcementIntTest.Config.class)
class ProjectDeploymentFacadeEnforcementIntTest {

    private static final String BODY_REACHED = "body reached";
    private static final long PROJECT_DEPLOYMENT_ID = 11L;
    private static final long PROJECT_DEPLOYMENT_WORKFLOW_ID = 77L;
    private static final long PROJECT_ID = 42L;
    private static final String WORKFLOW_ID = "workflow-1";

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private ProjectDeploymentFacade projectDeploymentFacade;

    @Autowired
    private ProjectDeploymentService projectDeploymentService;

    @Autowired
    private ProjectDeploymentWorkflowService projectDeploymentWorkflowService;

    @BeforeEach
    void authenticateAsNonAdmin() {
        SecurityContextHolder.getContext()
            .setAuthentication(
                new UsernamePasswordAuthenticationToken(
                    "viewer", "n/a", List.of(new SimpleGrantedAuthority("ROLE_USER"))));

        // Re-established each test because the mocks are shared through the cached Spring context. Every guard denies
        // by default, and every collaborator the guarded bodies touch first throws, so reaching a body is visible.
        reset(permissionService, projectDeploymentService, projectDeploymentWorkflowService);

        when(permissionService.isTenantAdmin()).thenReturn(false);
        when(permissionService.hasResourceScope(any(), anyString(), anyString())).thenReturn(false);

        when(projectDeploymentService.getProjectDeployment(anyLong()))
            .thenThrow(new IllegalStateException(BODY_REACHED));
        when(projectDeploymentService.update(anyLong(), any()))
            .thenThrow(new IllegalStateException(BODY_REACHED));
        when(projectDeploymentWorkflowService.getProjectDeploymentWorkflow(anyLong(), anyString()))
            .thenThrow(new IllegalStateException(BODY_REACHED));
        when(projectDeploymentWorkflowService.getProjectDeploymentWorkflows(anyLong()))
            .thenThrow(new IllegalStateException(BODY_REACHED));
        when(projectDeploymentService.getProjectDeploymentId(anyLong(), any()))
            .thenThrow(new IllegalStateException(BODY_REACHED));
        when(projectDeploymentWorkflowService.update(any(ProjectDeploymentWorkflow.class)))
            .thenThrow(new IllegalStateException(BODY_REACHED));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testDeleteProjectDeploymentIsDeniedWithoutTheDeploymentScope() {
        assertThatThrownBy(() -> projectDeploymentFacade.deleteProjectDeployment(PROJECT_DEPLOYMENT_ID))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testDeleteProjectDeploymentIsAllowedWithTheDeploymentScope() {
        grant("DEPLOYMENT_DELETE");

        assertThatThrownBy(() -> projectDeploymentFacade.deleteProjectDeployment(PROJECT_DEPLOYMENT_ID))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage(BODY_REACHED);
    }

    @Test
    void testCreateProjectDeploymentWorkflowJobIsDeniedWithoutTheDeploymentScope() {
        assertThatThrownBy(
            () -> projectDeploymentFacade.createProjectDeploymentWorkflowJob(PROJECT_DEPLOYMENT_ID, WORKFLOW_ID))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testCreateProjectDeploymentWorkflowJobIsAllowedWithTheDeploymentScope() {
        grant("DEPLOYMENT_EDIT");

        assertThatThrownBy(
            () -> projectDeploymentFacade.createProjectDeploymentWorkflowJob(PROJECT_DEPLOYMENT_ID, WORKFLOW_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(BODY_REACHED);
    }

    @Test
    void testUpdateProjectDeploymentTagsIsDeniedWithoutTheDeploymentScope() {
        assertThatThrownBy(
            () -> projectDeploymentFacade.updateProjectDeploymentTags(PROJECT_DEPLOYMENT_ID, List.<Tag>of()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testUpdateProjectDeploymentTagsIsAllowedWithTheDeploymentScope() {
        grant("DEPLOYMENT_EDIT");

        assertThatThrownBy(
            () -> projectDeploymentFacade.updateProjectDeploymentTags(PROJECT_DEPLOYMENT_ID, List.<Tag>of()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(BODY_REACHED);
    }

    @Test
    void testUpdateProjectDeploymentWorkflowIsDeniedWithoutTheDeploymentWorkflowScope() {
        assertThatThrownBy(() -> projectDeploymentFacade.updateProjectDeploymentWorkflow(projectDeploymentWorkflow()))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testUpdateProjectDeploymentWorkflowIsAllowedWithTheDeploymentWorkflowScope() {
        grant(PROJECT_DEPLOYMENT_WORKFLOW_ID, "ProjectDeploymentWorkflow", "DEPLOYMENT_EDIT");

        assertThatThrownBy(() -> projectDeploymentFacade.updateProjectDeploymentWorkflow(projectDeploymentWorkflow()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage(BODY_REACHED);
    }

    /**
     * The scope held on the deployment id the caller put in the path must not authorize a row belonging to a different
     * deployment. This is the enforcement half of the same claim {@code ProjectDeploymentGuardExpressionTest} makes
     * about the expression.
     */
    @Test
    void testUpdateProjectDeploymentWorkflowIsDeniedWhenOnlyTheSuppliedDeploymentIdIsHeld() {
        grant(PROJECT_DEPLOYMENT_ID, "ProjectDeployment", "DEPLOYMENT_EDIT");

        assertThatThrownBy(() -> projectDeploymentFacade.updateProjectDeploymentWorkflow(projectDeploymentWorkflow()))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testEnableProjectDeploymentIsDeniedWithoutTheDeploymentScope() {
        assertThatThrownBy(() -> projectDeploymentFacade.enableProjectDeployment(PROJECT_DEPLOYMENT_ID, true))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testEnableProjectDeploymentIsAllowedWithTheDeploymentScope() {
        grant("DEPLOYMENT_EDIT");

        assertThatThrownBy(() -> projectDeploymentFacade.enableProjectDeployment(PROJECT_DEPLOYMENT_ID, true))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage(BODY_REACHED);
    }

    @Test
    void testEnableProjectDeploymentWorkflowIsDeniedWithoutTheDeploymentScope() {
        assertThatThrownBy(
            () -> projectDeploymentFacade.enableProjectDeploymentWorkflow(PROJECT_DEPLOYMENT_ID, WORKFLOW_ID, true))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testEnableProjectDeploymentWorkflowIsAllowedWithTheDeploymentScope() {
        grant("DEPLOYMENT_EDIT");

        assertThatThrownBy(
            () -> projectDeploymentFacade.enableProjectDeploymentWorkflow(PROJECT_DEPLOYMENT_ID, WORKFLOW_ID, true))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(BODY_REACHED);
    }

    @Test
    void testEnableProjectDeploymentWorkflowByEnvironmentIsDeniedWithoutTheProjectScope() {
        assertThatThrownBy(
            () -> projectDeploymentFacade.enableProjectDeploymentWorkflow(
                PROJECT_ID, WORKFLOW_ID, true, Environment.PRODUCTION))
                    .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testEnableProjectDeploymentWorkflowByEnvironmentIsAllowedWithTheProjectScope() {
        grant(PROJECT_ID, "Project", "DEPLOYMENT_EDIT");

        assertThatThrownBy(
            () -> projectDeploymentFacade.enableProjectDeploymentWorkflow(
                PROJECT_ID, WORKFLOW_ID, true, Environment.PRODUCTION))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage(BODY_REACHED);
    }

    @Test
    void testUpdateProjectDeploymentIsDeniedWithoutTheDeploymentScope() {
        assertThatThrownBy(() -> projectDeploymentFacade.updateProjectDeployment(projectDeploymentDTO()))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testUpdateProjectDeploymentIsAllowedWithTheDeploymentScope() {
        grant("DEPLOYMENT_CREATE");

        assertThatThrownBy(() -> projectDeploymentFacade.updateProjectDeployment(projectDeploymentDTO()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage(BODY_REACHED);
    }

    private void grant(String scope) {
        grant(PROJECT_DEPLOYMENT_ID, "ProjectDeployment", scope);
    }

    private void grant(long id, String resourceType, String scope) {
        when(permissionService.hasResourceScope(id, resourceType, scope)).thenReturn(true);
    }

    // The row id and the deployment id differ deliberately: the REST path supplies them independently, so a guard
    // keyed on the wrong one cannot be caught by a fixture that gives them the same value.
    private static ProjectDeploymentWorkflow projectDeploymentWorkflow() {
        ProjectDeploymentWorkflow projectDeploymentWorkflow = new ProjectDeploymentWorkflow();

        projectDeploymentWorkflow.setId(PROJECT_DEPLOYMENT_WORKFLOW_ID);
        projectDeploymentWorkflow.setProjectDeploymentId(PROJECT_DEPLOYMENT_ID);

        return projectDeploymentWorkflow;
    }

    private static ProjectDeploymentDTO projectDeploymentDTO() {
        return new ProjectDeploymentDTO(
            null, null, null, true, Environment.PRODUCTION, PROJECT_DEPLOYMENT_ID, "deployment", null, null, null, null,
            PROJECT_ID, 1, List.of(), List.of(), 0);
    }

    // @SpringBootConfiguration (not @TestConfiguration) because @SpringBootTest(classes = Config.class) requires a
    // primary Spring Boot configuration class. The facade is built by hand so that only the collaborators these
    // enforcement tests stub need to be beans; @Transactional is inert here because transaction management is not
    // enabled, which is what lets the real bean run with mocked collaborators.
    @SpringBootConfiguration
    @EnableMethodSecurity
    @ImportAutoConfiguration(AutomationMethodSecurityConfiguration.class)
    static class Config {

        @Bean("permissionService")
        PermissionService permissionService() {
            return mock(PermissionService.class);
        }

        @Bean
        ProjectDeploymentService projectDeploymentService() {
            return mock(ProjectDeploymentService.class);
        }

        @Bean
        ProjectDeploymentWorkflowService projectDeploymentWorkflowService() {
            return mock(ProjectDeploymentWorkflowService.class);
        }

        @Bean
        ProjectDeploymentFacade projectDeploymentFacade(
            ProjectDeploymentService projectDeploymentService,
            ProjectDeploymentWorkflowService projectDeploymentWorkflowService) {

            return new ProjectDeploymentFacadeImpl(mock(ConnectionService.class), mock(Evaluator.class),
                mock(EnvironmentService.class), mock(PrincipalJobFacade.class), mock(PrincipalJobService.class),
                mock(JobFacade.class), mock(JobService.class), projectDeploymentService,
                projectDeploymentWorkflowService, mock(ProjectService.class), mock(ProjectWorkflowService.class),
                mock(TagService.class), mock(TriggerDefinitionService.class), mock(TriggerExecutionService.class),
                mock(TriggerLifecycleFacade.class), mock(ApplicationProperties.class),
                mock(ComponentConnectionFacade.class), mock(WorkflowService.class));
        }
    }
}
