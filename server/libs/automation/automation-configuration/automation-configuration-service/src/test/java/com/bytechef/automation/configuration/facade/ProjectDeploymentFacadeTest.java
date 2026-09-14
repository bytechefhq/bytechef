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

package com.bytechef.automation.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflowConnection;
import com.bytechef.automation.configuration.dto.ProjectDeploymentDTO;
import com.bytechef.automation.configuration.security.AutomationMethodSecurityExpressionHandler;
import com.bytechef.automation.configuration.security.AutomationPermissionEvaluator;
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
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authorization.method.AuthorizationManagerBeforeMethodInterceptor;
import org.springframework.security.authorization.method.PreAuthorizeAuthorizationManager;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class ProjectDeploymentFacadeTest {

    private static final long ENVIRONMENT_ID = 2L;
    private static final long PROJECT_DEPLOYMENT_ID = 11L;
    private static final long WORKSPACE_ID = 42L;
    private static final long PROJECT_DEPLOYMENT_WORKFLOW_ID = 77L;
    private static final long PROJECT_ID = 42L;
    private static final String PROJECT_DEPLOYMENT_TYPE = "ProjectDeployment";
    private static final String PROJECT_DEPLOYMENT_WORKFLOW_TYPE = "ProjectDeploymentWorkflow";
    private static final String PROJECT_TYPE = "Project";
    private static final String WORKFLOW_ID = "workflow-1";

    @Mock
    private ApplicationProperties applicationProperties;

    @Mock
    private PermissionService permissionService;

    @Mock
    private ProjectDeploymentService projectDeploymentService;

    @Mock
    private ProjectDeploymentWorkflowService projectDeploymentWorkflowService;

    @Mock
    private TagService tagService;

    @InjectMocks
    private ProjectDeploymentFacadeImpl projectDeploymentFacade;

    @Test
    void testUpdateProjectDeploymentWorkflowRefusesAConnectionTheWorkflowMayNotUse() {
        ProjectDeploymentWorkflow storedProjectDeploymentWorkflow = new ProjectDeploymentWorkflow();

        storedProjectDeploymentWorkflow.setProjectDeploymentId(3L);
        storedProjectDeploymentWorkflow.setWorkflowId("workflow-1");

        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setEnvironment(Environment.PRODUCTION);

        ProjectDeploymentWorkflow projectDeploymentWorkflow = new ProjectDeploymentWorkflow();

        projectDeploymentWorkflow.setConnections(
            List.of(new ProjectDeploymentWorkflowConnection(9L, "connection", "node_1")));
        projectDeploymentWorkflow.setId(5L);

        when(projectDeploymentWorkflowService.getProjectDeploymentWorkflow(5L))
            .thenReturn(storedProjectDeploymentWorkflow);
        when(projectDeploymentService.getProjectDeployment(3L))
            .thenReturn(projectDeployment);
        when(permissionService.canUseConnectionInWorkflow(9L, "workflow-1", Environment.PRODUCTION))
            .thenReturn(false);

        assertThatThrownBy(() -> projectDeploymentFacade.updateProjectDeploymentWorkflow(projectDeploymentWorkflow))
            .isInstanceOf(AccessDeniedException.class);

        verify(projectDeploymentWorkflowService, never()).update(any(ProjectDeploymentWorkflow.class));
    }

    @Test
    void testGetProjectDeploymentTags() {
        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setTagIds(List.of(20L, 21L));

        when(projectDeploymentService.getProjectDeployments(false, null, null, null, 7L))
            .thenReturn(List.of(projectDeployment));
        when(tagService.getTags(List.of(20L, 21L))).thenReturn(List.of(new Tag("x"), new Tag("y")));

        List<Tag> tags = projectDeploymentFacade.getProjectDeploymentTags(7L, null);

        assertThat(tags).hasSize(2);
    }

    @Test
    void testValidateInputsAcceptsNonStringValuesForRequiredInputs() {
        Workflow workflow = mock(Workflow.class);

        when(workflow.getInputs()).thenReturn(List.of(
            new Workflow.Input("hourToRun", "Hour", "integer", true),
            new Workflow.Input("minutesToRun", "Minute", "integer", true),
            new Workflow.Input("serviceProviderEmail", "Email", "string", true)));

        Map<String, Object> inputs = Map.of("hourToRun", 11, "minutesToRun", 50, "serviceProviderEmail", "a@b.c");

        assertThatCode(() -> ProjectDeploymentFacadeImpl.validateProjectDeploymentWorkflowInputs(inputs, workflow))
            .doesNotThrowAnyException();
    }

    @Test
    void testValidateInputsRejectsMissingBlankAndNullRequiredValues() {
        Workflow workflow = mock(Workflow.class);

        when(workflow.getInputs()).thenReturn(List.of(new Workflow.Input("name", "Name", "string", true)));

        assertThatIllegalArgumentException()
            .isThrownBy(() -> ProjectDeploymentFacadeImpl.validateProjectDeploymentWorkflowInputs(Map.of(), workflow))
            .withMessageContaining("Missing required param: name");

        assertThatIllegalArgumentException()
            .isThrownBy(() -> ProjectDeploymentFacadeImpl.validateProjectDeploymentWorkflowInputs(
                Map.of("name", "   "), workflow))
            .withMessageContaining("Missing required param: name");

        Map<String, Object> nullValueInputs = new HashMap<>();

        nullValueInputs.put("name", null);

        assertThatIllegalArgumentException()
            .isThrownBy(() -> ProjectDeploymentFacadeImpl.validateProjectDeploymentWorkflowInputs(
                nullValueInputs, workflow))
            .withMessageContaining("Missing required param: name");
    }

    @Test
    void testValidateInputsIgnoresAbsentOptionalInputs() {
        Workflow workflow = mock(Workflow.class);

        when(workflow.getInputs()).thenReturn(List.of(
            new Workflow.Input("destinationFolderName", "Folder", "string", false)));

        assertThatCode(() -> ProjectDeploymentFacadeImpl.validateProjectDeploymentWorkflowInputs(Map.of(), workflow))
            .doesNotThrowAnyException();
    }

    // The two ids differ deliberately: the REST path supplies them independently, so a guard keyed on the wrong one
    // cannot be caught by a fixture that gives them the same value.
    private static ProjectDeploymentWorkflow projectDeploymentWorkflow() {
        ProjectDeploymentWorkflow projectDeploymentWorkflow = new ProjectDeploymentWorkflow();

        projectDeploymentWorkflow.setId(PROJECT_DEPLOYMENT_WORKFLOW_ID);
        projectDeploymentWorkflow.setProjectDeploymentId(PROJECT_DEPLOYMENT_ID);

        return projectDeploymentWorkflow;
    }

    @Test
    void testUpdateProjectDeploymentWorkflowRejectsANewConnectionTheCallerCannotSee() {
        allowConnectionUsageInWorkflow();

        when(projectDeploymentWorkflowService.getProjectDeploymentWorkflow(5L))
            .thenReturn(projectDeploymentWorkflow(5L, 10L));
        when(permissionService.hasResourceScope(42L, "Connection", "CONNECTION_VIEW")).thenReturn(false);

        assertThatThrownBy(
            () -> projectDeploymentFacade.updateProjectDeploymentWorkflow(projectDeploymentWorkflow(5L, 42L)))
                .isInstanceOf(AccessDeniedException.class);

        verify(projectDeploymentWorkflowService, never()).update(any(ProjectDeploymentWorkflow.class));
    }

    @Test
    void testUpdateProjectDeploymentWorkflowDoesNotRecheckAnAlreadyAttachedConnection() {
        allowConnectionUsageInWorkflow();

        ProjectDeploymentWorkflow projectDeploymentWorkflow = projectDeploymentWorkflow(5L, 10L);

        when(projectDeploymentWorkflowService.getProjectDeploymentWorkflow(5L))
            .thenReturn(projectDeploymentWorkflow(5L, 10L));

        projectDeploymentFacade.updateProjectDeploymentWorkflow(projectDeploymentWorkflow);

        verify(permissionService, never()).hasResourceScope(eq(10L), anyString(), anyString());
        verify(projectDeploymentWorkflowService).update(projectDeploymentWorkflow);
    }

    private static ProjectDeploymentWorkflow projectDeploymentWorkflow(long id, long connectionId) {
        ProjectDeploymentWorkflow projectDeploymentWorkflow = new ProjectDeploymentWorkflow();

        projectDeploymentWorkflow.setId(id);
        projectDeploymentWorkflow.setConnections(
            List.of(new ProjectDeploymentWorkflowConnection(connectionId, "connection", "node_1")));
        projectDeploymentWorkflow.setProjectDeploymentId(3L);
        projectDeploymentWorkflow.setWorkflowId("workflow-1");

        return projectDeploymentWorkflow;
    }

    private static ProjectDeploymentDTO projectDeploymentDTO() {
        return new ProjectDeploymentDTO(
            null, null, null, true, Environment.PRODUCTION, PROJECT_DEPLOYMENT_ID, "deployment", null, null, null, null,
            PROJECT_ID, 1, List.of(), List.of(), 0);
    }

    private static ProjectDeploymentDTO projectDeployment(long projectId, Environment environment) {
        return new ProjectDeploymentDTO(
            null, null, null, true, environment, 1L, "deployment", null, null, null, null, projectId, 1, List.of(),
            List.of(), 0);
    }

    private void allowConnectionUsageInWorkflow() {
        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setEnvironment(Environment.PRODUCTION);

        when(projectDeploymentService.getProjectDeployment(3L))
            .thenReturn(projectDeployment);
        when(permissionService.canUseConnectionInWorkflow(anyLong(), eq("workflow-1"), eq(Environment.PRODUCTION)))
            .thenReturn(true);
    }

    @Nested
    class MethodSecurityEnforcement {

        private static final String BODY_REACHED = "body reached";

        @BeforeEach
        void beforeEach() {
            SecurityContext securityContext = SecurityContextHolder.createEmptyContext();

            securityContext.setAuthentication(
                new UsernamePasswordAuthenticationToken(
                    "member", null, List.of(new SimpleGrantedAuthority("ROLE_USER"))));

            SecurityContextHolder.setContext(securityContext);
        }

        @AfterEach
        void afterEach() {
            SecurityContextHolder.clearContext();
        }

        @Test
        void testGetProjectDeploymentDeniesWhenTheDeploymentViewScopeIsRefused() {
            assertGuard(
                facade -> facade.getProjectDeployment(PROJECT_DEPLOYMENT_ID), PROJECT_DEPLOYMENT_ID,
                PROJECT_DEPLOYMENT_TYPE, "DEPLOYMENT_VIEW", false);
        }

        @Test
        void testGetProjectDeploymentAllowsWhenTheDeploymentViewScopeIsGranted() {
            assertGuard(
                facade -> facade.getProjectDeployment(PROJECT_DEPLOYMENT_ID), PROJECT_DEPLOYMENT_ID,
                PROJECT_DEPLOYMENT_TYPE, "DEPLOYMENT_VIEW", true);
        }

        @Test
        void testGetWorkspaceProjectDeploymentsDeniesWhenTheDeploymentViewScopeIsRefused() {
            assertGetWorkspaceProjectDeploymentsGuard(false);
        }

        @Test
        void testGetWorkspaceProjectDeploymentsAllowsWhenTheDeploymentViewScopeIsGranted() {
            assertGetWorkspaceProjectDeploymentsGuard(true);
        }

        @Test
        void testGetProjectDeploymentWorkflowDeniesWhenTheDeploymentViewScopeIsRefused() {
            assertGuard(
                facade -> facade.getProjectDeploymentWorkflow(PROJECT_DEPLOYMENT_ID, "workflow-uuid"),
                PROJECT_DEPLOYMENT_ID, PROJECT_DEPLOYMENT_TYPE, "DEPLOYMENT_VIEW", false);
        }

        @Test
        void testGetProjectDeploymentWorkflowAllowsWhenTheDeploymentViewScopeIsGranted() {
            assertGuard(
                facade -> facade.getProjectDeploymentWorkflow(PROJECT_DEPLOYMENT_ID, "workflow-uuid"),
                PROJECT_DEPLOYMENT_ID, PROJECT_DEPLOYMENT_TYPE, "DEPLOYMENT_VIEW", true);
        }

        @Test
        void testDeleteProjectDeploymentDeniesWhenTheDeploymentScopeIsRefused() {
            assertGuard(
                facade -> facade.deleteProjectDeployment(PROJECT_DEPLOYMENT_ID), PROJECT_DEPLOYMENT_ID,
                PROJECT_DEPLOYMENT_TYPE, "DEPLOYMENT_DELETE", false);
        }

        @Test
        void testDeleteProjectDeploymentAllowsWhenTheDeploymentScopeIsGranted() {
            assertGuard(
                facade -> facade.deleteProjectDeployment(PROJECT_DEPLOYMENT_ID), PROJECT_DEPLOYMENT_ID,
                PROJECT_DEPLOYMENT_TYPE, "DEPLOYMENT_DELETE", true);
        }

        @Test
        void testCreateProjectDeploymentWorkflowJobDeniesWhenTheDeploymentScopeIsRefused() {
            assertGuard(
                facade -> facade.createProjectDeploymentWorkflowJob(PROJECT_DEPLOYMENT_ID, WORKFLOW_ID),
                PROJECT_DEPLOYMENT_ID, PROJECT_DEPLOYMENT_TYPE, "DEPLOYMENT_EDIT", false);
        }

        @Test
        void testCreateProjectDeploymentWorkflowJobAllowsWhenTheDeploymentScopeIsGranted() {
            assertGuard(
                facade -> facade.createProjectDeploymentWorkflowJob(PROJECT_DEPLOYMENT_ID, WORKFLOW_ID),
                PROJECT_DEPLOYMENT_ID, PROJECT_DEPLOYMENT_TYPE, "DEPLOYMENT_EDIT", true);
        }

        @Test
        void testUpdateProjectDeploymentTagsDeniesWhenTheDeploymentScopeIsRefused() {
            assertGuard(
                facade -> facade.updateProjectDeploymentTags(PROJECT_DEPLOYMENT_ID, List.of()), PROJECT_DEPLOYMENT_ID,
                PROJECT_DEPLOYMENT_TYPE, "DEPLOYMENT_EDIT", false);
        }

        @Test
        void testUpdateProjectDeploymentTagsAllowsWhenTheDeploymentScopeIsGranted() {
            assertGuard(
                facade -> facade.updateProjectDeploymentTags(PROJECT_DEPLOYMENT_ID, List.of()), PROJECT_DEPLOYMENT_ID,
                PROJECT_DEPLOYMENT_TYPE, "DEPLOYMENT_EDIT", true);
        }

        @Test
        void testUpdateProjectDeploymentWorkflowDeniesWhenTheDeploymentWorkflowScopeIsRefused() {
            assertGuard(
                facade -> facade.updateProjectDeploymentWorkflow(projectDeploymentWorkflow()),
                PROJECT_DEPLOYMENT_WORKFLOW_ID, PROJECT_DEPLOYMENT_WORKFLOW_TYPE, "DEPLOYMENT_EDIT", false);
        }

        @Test
        void testUpdateProjectDeploymentWorkflowAllowsWhenTheDeploymentWorkflowScopeIsGranted() {
            assertGuard(
                facade -> facade.updateProjectDeploymentWorkflow(projectDeploymentWorkflow()),
                PROJECT_DEPLOYMENT_WORKFLOW_ID, PROJECT_DEPLOYMENT_WORKFLOW_TYPE, "DEPLOYMENT_EDIT", true);
        }

        /**
         * The row written is selected by its own id, so that is what the guard must ask about. Were it keyed on the
         * argument's {@code projectDeploymentId} instead, the two would be independently caller-supplied — the REST
         * path carries the deployment id and the row id as separate variables — and a member who is editor in
         * Development and viewer in Production could satisfy the check against a Development deployment while
         * repointing a Production row's connections and inputs. The argument here is exactly that mismatched pair.
         */
        @Test
        void testUpdateProjectDeploymentWorkflowNeverAsksAboutTheCallerSuppliedDeploymentId() {
            PermissionService guardPermissionService = mock(PermissionService.class);

            lenient().when(
                guardPermissionService.hasResourceScope(
                    PROJECT_DEPLOYMENT_ID, PROJECT_DEPLOYMENT_TYPE, "DEPLOYMENT_EDIT"))
                .thenReturn(true);

            assertInvocationOutcome(
                facade -> facade.updateProjectDeploymentWorkflow(projectDeploymentWorkflow()), guardPermissionService,
                false);

            verify(guardPermissionService)
                .hasResourceScope(PROJECT_DEPLOYMENT_WORKFLOW_ID, PROJECT_DEPLOYMENT_WORKFLOW_TYPE, "DEPLOYMENT_EDIT");
            verifyNoMoreInteractions(guardPermissionService);
        }

        @Test
        void testEnableProjectDeploymentDeniesWhenTheDeploymentScopeIsRefused() {
            assertGuard(
                facade -> facade.enableProjectDeployment(PROJECT_DEPLOYMENT_ID, true), PROJECT_DEPLOYMENT_ID,
                PROJECT_DEPLOYMENT_TYPE, "DEPLOYMENT_EDIT", false);
        }

        @Test
        void testEnableProjectDeploymentAllowsWhenTheDeploymentScopeIsGranted() {
            assertGuard(
                facade -> facade.enableProjectDeployment(PROJECT_DEPLOYMENT_ID, true), PROJECT_DEPLOYMENT_ID,
                PROJECT_DEPLOYMENT_TYPE, "DEPLOYMENT_EDIT", true);
        }

        @Test
        void testEnableProjectDeploymentWorkflowDeniesWhenTheDeploymentScopeIsRefused() {
            assertGuard(
                facade -> facade.enableProjectDeploymentWorkflow(PROJECT_DEPLOYMENT_ID, WORKFLOW_ID, true),
                PROJECT_DEPLOYMENT_ID, PROJECT_DEPLOYMENT_TYPE, "DEPLOYMENT_EDIT", false);
        }

        @Test
        void testEnableProjectDeploymentWorkflowAllowsWhenTheDeploymentScopeIsGranted() {
            assertGuard(
                facade -> facade.enableProjectDeploymentWorkflow(PROJECT_DEPLOYMENT_ID, WORKFLOW_ID, true),
                PROJECT_DEPLOYMENT_ID, PROJECT_DEPLOYMENT_TYPE, "DEPLOYMENT_EDIT", true);
        }

        /**
         * The four-argument overload takes a projectId, so its check is environment-blind by necessity and names the
         * {@code 'Project'} token. Pinned in both directions because an environment-blind check is still a check, and
         * because the overload is reached from the three-argument one by self-invocation and so cannot borrow its
         * guard.
         */
        @Test
        void testEnableProjectDeploymentWorkflowByEnvironmentDeniesWhenTheProjectScopeIsRefused() {
            assertGuard(
                facade -> facade.enableProjectDeploymentWorkflow(
                    PROJECT_ID, WORKFLOW_ID, true, Environment.PRODUCTION),
                PROJECT_ID, PROJECT_TYPE, "DEPLOYMENT_EDIT", false);
        }

        @Test
        void testEnableProjectDeploymentWorkflowByEnvironmentAllowsWhenTheProjectScopeIsGranted() {
            assertGuard(
                facade -> facade.enableProjectDeploymentWorkflow(
                    PROJECT_ID, WORKFLOW_ID, true, Environment.PRODUCTION),
                PROJECT_ID, PROJECT_TYPE, "DEPLOYMENT_EDIT", true);
        }

        @Test
        void testUpdateProjectDeploymentDeniesWhenTheDeploymentScopeIsRefused() {
            assertGuard(
                facade -> facade.updateProjectDeployment(projectDeploymentDTO()), PROJECT_DEPLOYMENT_ID,
                PROJECT_DEPLOYMENT_TYPE, "DEPLOYMENT_CREATE", false);
        }

        @Test
        void testUpdateProjectDeploymentAllowsWhenTheDeploymentScopeIsGranted() {
            assertGuard(
                facade -> facade.updateProjectDeployment(projectDeploymentDTO()), PROJECT_DEPLOYMENT_ID,
                PROJECT_DEPLOYMENT_TYPE, "DEPLOYMENT_CREATE", true);
        }

        /**
         * The update guard is keyed on the deployment id, not on the DTO, so the environment it is judged in comes from
         * the stored row rather than from the request body. Were it the DTO form the promotion branch would run
         * instead, and a caller could name the environment they hold a role in while editing a deployment in another.
         */
        @Test
        void testUpdateProjectDeploymentDoesNotRouteThroughThePromotionBranch() {
            PermissionService guardPermissionService = mock(PermissionService.class);

            assertInvocationOutcome(
                facade -> facade.updateProjectDeployment(projectDeploymentDTO()), guardPermissionService, false);

            // Asserted as "the id form and nothing else", not as "not this one promotion call": a return to the DTO
            // form under any other scope, environment or projectId would leave a scope-specific never() green.
            verify(guardPermissionService)
                .hasResourceScope(PROJECT_DEPLOYMENT_ID, PROJECT_DEPLOYMENT_TYPE, "DEPLOYMENT_CREATE");
            verify(guardPermissionService, never()).hasWorkspaceScopeForProject(anyLong(), anyString());
            verify(guardPermissionService, never()).hasWorkspaceScopeForProject(anyLong(), anyString(), any());
            verifyNoMoreInteractions(guardPermissionService);
        }

        @Test
        void testTheRealGuardExpressionReachesThePromotionBranch() {
            PermissionService guardPermissionService = mock(PermissionService.class);

            when(
                guardPermissionService.hasWorkspaceScopeForProject(
                    PROJECT_ID, "DEPLOYMENT_CREATE", Environment.PRODUCTION))
                        .thenReturn(false);

            ProjectDeploymentDTO projectDeploymentDTO = projectDeployment(PROJECT_ID, Environment.PRODUCTION);

            assertInvocationOutcome(
                facade -> facade.createProjectDeployment(projectDeploymentDTO), guardPermissionService, false);

            // The target environment decided, and it was read off the deployment rather than from any ambient state.
            verify(guardPermissionService)
                .hasWorkspaceScopeForProject(PROJECT_ID, "DEPLOYMENT_CREATE", Environment.PRODUCTION);
        }

        @Test
        void testTheRealGuardExpressionAllowsWhenTheTargetEnvironmentGrantsTheScope() {
            PermissionService guardPermissionService = mock(PermissionService.class);

            when(
                guardPermissionService.hasWorkspaceScopeForProject(
                    PROJECT_ID, "DEPLOYMENT_CREATE", Environment.DEVELOPMENT))
                        .thenReturn(true);

            ProjectDeploymentDTO projectDeploymentDTO = projectDeployment(PROJECT_ID, Environment.DEVELOPMENT);

            assertInvocationOutcome(
                facade -> facade.createProjectDeployment(projectDeploymentDTO), guardPermissionService, true);
        }

        private void assertGetWorkspaceProjectDeploymentsGuard(boolean granted) {
            PermissionService guardPermissionService = mock(PermissionService.class);
            Environment environment = Environment.values()[(int) ENVIRONMENT_ID];

            when(guardPermissionService.hasWorkspaceScope(WORKSPACE_ID, "DEPLOYMENT_VIEW", environment))
                .thenReturn(granted);

            assertInvocationOutcome(
                facade -> facade.getWorkspaceProjectDeployments(WORKSPACE_ID, ENVIRONMENT_ID, null, null, false),
                guardPermissionService, granted);

            verify(guardPermissionService).hasWorkspaceScope(WORKSPACE_ID, "DEPLOYMENT_VIEW", environment);
            verifyNoMoreInteractions(guardPermissionService);
        }

        private void assertGuard(
            Consumer<ProjectDeploymentFacade> invocation, Serializable expectedId, String expectedResourceType,
            String expectedScope, boolean granted) {

            PermissionService guardPermissionService = mock(PermissionService.class);

            when(guardPermissionService.hasResourceScope(expectedId, expectedResourceType, expectedScope))
                .thenReturn(granted);

            assertInvocationOutcome(invocation, guardPermissionService, granted);

            verify(guardPermissionService).hasResourceScope(expectedId, expectedResourceType, expectedScope);
            verifyNoMoreInteractions(guardPermissionService);
        }

        private void assertInvocationOutcome(
            Consumer<ProjectDeploymentFacade> invocation, PermissionService guardPermissionService, boolean allowed) {

            ProjectDeploymentFacade securedProjectDeploymentFacade = secure(
                new ProjectDeploymentFacadeImpl(
                    bodyReachedMock(ConnectionService.class), bodyReachedMock(Evaluator.class),
                    bodyReachedMock(EnvironmentService.class), bodyReachedMock(PrincipalJobFacade.class),
                    bodyReachedMock(PrincipalJobService.class), bodyReachedMock(JobFacade.class),
                    bodyReachedMock(JobService.class), bodyReachedMock(ProjectDeploymentService.class),
                    bodyReachedMock(ProjectDeploymentWorkflowService.class), bodyReachedMock(ProjectService.class),
                    bodyReachedMock(ProjectWorkflowService.class), bodyReachedMock(TagService.class),
                    bodyReachedMock(TriggerDefinitionService.class), bodyReachedMock(TriggerExecutionService.class),
                    bodyReachedMock(TriggerLifecycleFacade.class), applicationProperties,
                    bodyReachedMock(ComponentConnectionFacade.class), bodyReachedMock(PermissionService.class),
                    bodyReachedMock(WorkflowService.class)),
                guardPermissionService);

            if (allowed) {
                assertThatThrownBy(() -> invocation.accept(securedProjectDeploymentFacade))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage(BODY_REACHED);
            } else {
                assertThatThrownBy(() -> invocation.accept(securedProjectDeploymentFacade))
                    .isInstanceOf(AccessDeniedException.class);
            }
        }

        private <T> T bodyReachedMock(Class<T> type) {
            return mock(type, invocation -> {
                throw new IllegalStateException(BODY_REACHED);
            });
        }

        @SuppressWarnings("unchecked")
        private <T> T secure(T target, PermissionService guardPermissionService) {
            AutomationMethodSecurityExpressionHandler expressionHandler =
                new AutomationMethodSecurityExpressionHandler(guardPermissionService);

            expressionHandler.setPermissionEvaluator(new AutomationPermissionEvaluator(guardPermissionService));

            PreAuthorizeAuthorizationManager preAuthorizeAuthorizationManager =
                new PreAuthorizeAuthorizationManager();

            preAuthorizeAuthorizationManager.setExpressionHandler(expressionHandler);

            ProxyFactory proxyFactory = new ProxyFactory(target);

            proxyFactory.addAdvice(
                AuthorizationManagerBeforeMethodInterceptor.preAuthorize(preAuthorizeAuthorizationManager));

            return (T) proxyFactory.getProxy();
        }
    }
}
