/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.facade.ProjectWorkflowFacadeImpl;
import com.bytechef.automation.configuration.security.AutomationAuthorizationContext;
import com.bytechef.automation.configuration.security.AutomationMethodSecurityConfiguration;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentServiceImpl;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectServiceImpl;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.automation.workflow.execution.facade.ProjectWorkflowExecutionFacadeImpl;
import com.bytechef.ee.automation.configuration.facade.ProjectGitFacadeImpl;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProject;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProjectWorkflow;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectDTO;
import com.bytechef.ee.embedded.configuration.dto.ConnectedUserWorkflowTemplateDTO;
import com.bytechef.ee.embedded.configuration.facade.AutomationWorkflowProjectFacade;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserConnectionService;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserProjectService;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserProjectWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.ee.embedded.security.web.authentication.EmbeddedApiKeyAuthenticationToken;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.facade.WorkflowEvaluationInputsFacade;
import com.bytechef.platform.configuration.facade.WorkflowNodeOptionFacadeImpl;
import com.bytechef.platform.configuration.facade.WorkflowNodeOutputFacade;
import com.bytechef.platform.configuration.facade.WorkflowNodeScriptFacade;
import com.bytechef.platform.configuration.facade.WorkflowNodeScriptFacadeImpl;
import com.bytechef.platform.configuration.facade.WorkflowNodeTestOutputFacade;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.file.storage.TempFileStorage;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import com.bytechef.platform.workflow.test.facade.TestWorkflowExecutor;
import com.bytechef.platform.workflow.test.web.rest.WorkflowTestApiController;
import com.bytechef.platform.workflow.test.web.rest.WorkflowTestApiController.TestWorkflowRequest;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Service;
import org.springframework.test.context.TestPropertySource;

/**
 * Ticket 1051 Stage 2, end to end through the real Spring Security proxy: connected user A must be denied connected
 * user B's {@code workflowId}, {@code connectionId}, {@code projectId} and {@code jobId}, while
 * {@code @SkipAutomationAuthorization}'s full skip mode is armed — which is the mode embedded's own facades run under,
 * and the mode that granted every one of these before this stage.
 *
 * <p>
 * The {@code workflowId} case calls the REAL {@link WorkflowNodeScriptFacadeImpl#testWorkflowNodeScript}, the design's
 * named worst case: bypassed, it executes a node from another connected user's workflow using that workflow's stored
 * connection. Deleting that production annotation turns this test red. The other three types go through a stand-in
 * carrying the same {@code @PreAuthorize} expressions their production sites carry
 * ({@code ProjectWorkflowFacadeImpl:149}, {@code WorkflowTestConfigurationGraphQlController:40},
 * {@code ProjectWorkflowExecutionFacadeImpl:141}), so what they pin is the expression form reaching the evaluator, not
 * those particular call sites.
 *
 * <p>
 * {@link PermissionService} is stubbed to GRANT everything. Nothing here can therefore pass by accident through the
 * ordinary RBAC path — every grant below comes from the resolver, and every denial from it too.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = ConnectedUserResourceMembershipEnforcementIntTest.Config.class)
@TestPropertySource(properties = "bytechef.edition=ee")
class ConnectedUserResourceMembershipEnforcementIntTest {

    private static final String EXTERNAL_USER_A = "connected-user-a";
    private static final String EXTERNAL_USER_B = "connected-user-b";
    private static final String PLATFORM_USER = "admin@localhost.com";

    private static final long CONNECTED_USER_A_ID = 11L;
    private static final long CONNECTED_USER_B_ID = 22L;
    private static final long PROJECT_A_ID = 101L;
    private static final long PROJECT_B_ID = 202L;
    private static final long CONNECTION_A_ID = 301L;
    private static final long CONNECTION_B_ID = 302L;
    private static final long JOB_A_ID = 401L;
    private static final long JOB_B_ID = 402L;
    private static final long CATALOG_JOB_A_ID = 403L;
    private static final long CATALOG_JOB_B_ID = 404L;
    private static final long CONNECTED_USER_PROJECT_A_ID = 55L;
    private static final long PROJECT_DEPLOYMENT_A_ID = 501L;
    private static final long PROJECT_DEPLOYMENT_B_ID = 502L;
    private static final long CATALOG_PROJECT_ID = 909L;

    private static final Long PRODUCTION_ENVIRONMENT_ID = (long) Environment.PRODUCTION.ordinal();

    private static final String WORKFLOW_A_ID = "workflow-a";
    private static final String WORKFLOW_B_ID = "workflow-b";
    private static final String CATALOG_WORKFLOW_ID = "workflow-catalog";
    private static final String CATALOG_WORKFLOW_UUID = "3f1b9c60-0000-4000-8000-00000000cafe";

    @Autowired
    private AutomationWorkflowProjectFacade automationWorkflowProjectFacade;

    @Autowired
    private ConnectedUserConnectionService connectedUserConnectionService;

    @Autowired
    private ConnectedUserProjectService connectedUserProjectService;

    @Autowired
    private ConnectedUserService connectedUserService;

    @Autowired
    private GuardedEmbeddedReads guardedEmbeddedReads;

    @Autowired
    private IntegrationInstanceService integrationInstanceService;

    @Autowired
    private JobService jobService;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private ConnectedUserProjectWorkflowService connectedUserProjectWorkflowService;

    @Autowired
    private PrincipalJobService principalJobService;

    @Autowired
    private ProjectDeploymentService projectDeploymentService;

    @Autowired
    private ProjectWorkflowService projectWorkflowService;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private WorkflowNodeScriptFacade workflowNodeScriptFacade;

    @Autowired
    private TestWorkflowExecutor testWorkflowExecutor;

    @Autowired
    private WorkflowTestApiController workflowTestApiController;

    @BeforeEach
    void setUp() {
        // Deliberately NOT binding EnvironmentContext: no request entry point in the repo binds it, so binding it here
        // would make these tests pass on a condition production never provides. The environment rides the principal.
        authenticateAsConnectedUser(EXTERNAL_USER_A);

        // Connected user A exists and owns project A; B's resources belong to project B.
        when(connectedUserService.fetchConnectedUser(EXTERNAL_USER_A, Environment.PRODUCTION))
            .thenReturn(Optional.of(mock(ConnectedUser.class)));
        when(connectedUserProjectService.fetchConnectUserProject(EXTERNAL_USER_A, Environment.PRODUCTION))
            .thenReturn(Optional.of(connectedUserProject()));

        when(projectService.fetchWorkflowProject(WORKFLOW_A_ID)).thenReturn(Optional.of(project(PROJECT_A_ID)));
        when(projectService.fetchWorkflowProject(WORKFLOW_B_ID)).thenReturn(Optional.of(project(PROJECT_B_ID)));

        // No published catalog templates, so the Workflow predicate's catalog arm never rescues B's workflow.
        when(automationWorkflowProjectFacade.getPublishedProjects(anyString(), any(Environment.class)))
            .thenReturn(List.of());

        IntegrationInstance integrationInstance = mock(IntegrationInstance.class);

        when(integrationInstance.getConnectionId()).thenReturn(CONNECTION_A_ID);
        when(integrationInstanceService.getConnectedUserIntegrationInstances(
            CONNECTED_USER_A_ID, Environment.PRODUCTION)).thenReturn(List.of(integrationInstance));
        when(connectedUserConnectionService.getConnectionIds(CONNECTED_USER_A_ID)).thenReturn(List.of());

        when(jobService.fetchJob(JOB_A_ID)).thenReturn(Optional.of(job(WORKFLOW_A_ID)));
        when(jobService.fetchJob(JOB_B_ID)).thenReturn(Optional.of(job(WORKFLOW_B_ID)));

        // A's own ProjectDeployment set: no deployment of project A itself, one reference deployment provisioned on
        // the CATALOG project -- the shape ConnectedUserCodeWorkflowReferenceFacadeImpl.getOrCreateReference leaves
        // behind. B's deployment 502 is in neither source.
        when(projectDeploymentService.getProjectDeployments(PROJECT_A_ID)).thenReturn(List.of());
        when(connectedUserProjectWorkflowService.getConnectedUserProjectWorkflows(CONNECTED_USER_PROJECT_A_ID))
            .thenReturn(List.of(connectedUserProjectWorkflow(PROJECT_DEPLOYMENT_A_ID)));

        // Both catalog jobs run the SAME admin-owned catalog workflow; only the deployment they ran under differs.
        when(jobService.fetchJob(CATALOG_JOB_A_ID)).thenReturn(Optional.of(job(CATALOG_WORKFLOW_ID)));
        when(jobService.fetchJob(CATALOG_JOB_B_ID)).thenReturn(Optional.of(job(CATALOG_WORKFLOW_ID)));
        when(principalJobService.fetchJobPrincipalId(CATALOG_JOB_A_ID, PlatformType.AUTOMATION))
            .thenReturn(Optional.of(PROJECT_DEPLOYMENT_A_ID));
        when(principalJobService.fetchJobPrincipalId(CATALOG_JOB_B_ID, PlatformType.AUTOMATION))
            .thenReturn(Optional.of(PROJECT_DEPLOYMENT_B_ID));

        // The ordinary RBAC path grants everything, so nothing below can pass through it by accident.
        when(permissionService.hasResourceScope(any(), anyString(), anyString())).thenReturn(true);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();

        reset(
            testWorkflowExecutor, automationWorkflowProjectFacade, connectedUserConnectionService,
            connectedUserProjectService, connectedUserProjectWorkflowService,
            connectedUserService, integrationInstanceService, jobService, permissionService, principalJobService,
            projectDeploymentService, projectService, projectWorkflowService);
    }

    /**
     * The design's named worst case, on the real facade: bypassed, this executes a node from another connected user's
     * workflow using that workflow's stored connection and returns the output.
     */
    @Test
    void testDeniesAnotherConnectedUsersWorkflowIdOnTestWorkflowNodeScript() throws Throwable {
        AutomationAuthorizationContext.callSkippingChecks(() -> {
            assertThatThrownBy(
                () -> workflowNodeScriptFacade.testWorkflowNodeScript(WORKFLOW_B_ID, "node-1", 0L, Map.of()))
                    .isInstanceOf(AccessDeniedException.class);

            return null;
        });
    }

    /**
     * The positive control for the same call: A's own workflow still passes the gate, so the denial above is membership
     * and not a blanket refusal.
     */
    @Test
    void testGrantsOwnWorkflowIdOnTestWorkflowNodeScript() throws Throwable {
        AutomationAuthorizationContext.callSkippingChecks(() -> {
            assertThatCode(
                () -> workflowNodeScriptFacade.testWorkflowNodeScript(WORKFLOW_A_ID, "node-1", 0L, Map.of()))
                    .doesNotThrowAnyException();

            return null;
        });

        // Granted by the resolver, not by the RBAC path -- which never ran.
        verifyNoInteractions(permissionService);
    }

    @Test
    void testDeniesAnotherConnectedUsersConnectionId() throws Throwable {
        AutomationAuthorizationContext.callSkippingChecks(() -> {
            assertThatThrownBy(() -> guardedEmbeddedReads.useConnection(CONNECTION_B_ID))
                .isInstanceOf(AccessDeniedException.class);

            assertThatCode(() -> guardedEmbeddedReads.useConnection(CONNECTION_A_ID)).doesNotThrowAnyException();

            return null;
        });
    }

    @Test
    void testDeniesAnotherConnectedUsersProjectId() throws Throwable {
        AutomationAuthorizationContext.callSkippingChecks(() -> {
            assertThatThrownBy(() -> guardedEmbeddedReads.addWorkflow(PROJECT_B_ID))
                .isInstanceOf(AccessDeniedException.class);

            assertThatCode(() -> guardedEmbeddedReads.addWorkflow(PROJECT_A_ID)).doesNotThrowAnyException();

            return null;
        });
    }

    /**
     * Job membership derives from the job's workflow, so B's job is denied for the same reason B's workflow is.
     */
    @Test
    void testDeniesAnotherConnectedUsersJobId() throws Throwable {
        AutomationAuthorizationContext.callSkippingChecks(() -> {
            assertThatThrownBy(() -> guardedEmbeddedReads.viewExecution(JOB_B_ID))
                .isInstanceOf(AccessDeniedException.class);

            assertThatCode(() -> guardedEmbeddedReads.viewExecution(JOB_A_ID)).doesNotThrowAnyException();

            return null;
        });
    }

    /**
     * Ticket 1051 residual 1, at the proxy. {@code 'ProjectDeployment'} had no arm on the resolver, so it fell to the
     * {@code default} case and answered NOT_APPLICABLE, which {@code ResourceMembershipDecider} maps to DENY -- a
     * connected user could not enable or disable their OWN reference automation. Both halves run through the real
     * evaluator and the real resolver here, because a unit-level {@code Decision} does not prove an
     * {@code AccessDeniedException} ever stopped being thrown.
     */
    @Test
    void testGrantsOwnProjectDeploymentIdAndDeniesAnother() throws Throwable {
        AutomationAuthorizationContext.callSkippingChecks(() -> {
            assertThatCode(() -> guardedEmbeddedReads.enableProjectDeploymentWorkflow(PROJECT_DEPLOYMENT_A_ID))
                .doesNotThrowAnyException();

            assertThatThrownBy(() -> guardedEmbeddedReads.enableProjectDeploymentWorkflow(PROJECT_DEPLOYMENT_B_ID))
                .isInstanceOf(AccessDeniedException.class);

            return null;
        });
    }

    /**
     * Ticket 1051 residual 2, at the proxy. Both jobs run the SAME visible catalog template, so before the narrowing
     * both were granted {@code EXECUTION_VIEW} purely because A could see the template -- a within-tenant cross-user
     * read of B's job inputs, outputs and logs. The only difference between them is the {@code ProjectDeployment} they
     * ran under, which is what the resolver now discriminates on.
     *
     * <p>
     * A's own catalog run must stay readable: a referenced catalog workflow is executed with no copy taken, so denying
     * the arm outright would hide users' own executions from them.
     */
    @Test
    void testGrantsOwnCatalogRunAndDeniesAnotherConnectedUsersRunOfTheSameCatalogWorkflow() throws Throwable {
        stubVisibleCatalogWorkflow();

        AutomationAuthorizationContext.callSkippingChecks(() -> {
            assertThatCode(() -> guardedEmbeddedReads.viewExecution(CATALOG_JOB_A_ID)).doesNotThrowAnyException();

            assertThatThrownBy(() -> guardedEmbeddedReads.viewExecution(CATALOG_JOB_B_ID))
                .isInstanceOf(AccessDeniedException.class);

            return null;
        });
    }

    /**
     * Ticket 1051 residual 3, at the proxy, and the last of the three.
     * {@code ConnectedUserCodeWorkflowReferenceFacadeImpl.getOrCreateReference} provisions a {@code ProjectDeployment}
     * on the CATALOG project, and the whole call chain down to {@code ProjectDeploymentServiceImpl.create} is ungated
     * except for that method's own {@code hasPermission(#projectDeployment.projectId, 'Project', 'DEPLOYMENT_PUSH')} --
     * where {@code projectId} is the catalog project's, not the caller's. Until {@code resolveProject} grew its catalog
     * arm the ownership test was the whole answer, so a governed connected user was denied; the resolver is consulted
     * ahead of the skip check, so {@code @SkipAutomationAuthorization} on the reference facade did not rescue it, and
     * FIRST-USE provisioning of a reference 403'd before the enable toggle residual 1 unblocks was ever reachable.
     *
     * <p>
     * This test replaces the one that pinned that denial. The predicate is the catalog listing's, so both halves are
     * asserted: a catalog project A can SEE grants {@code DEPLOYMENT_PUSH}, and one A cannot see still denies it.
     */
    @Test
    void testGrantsProvisioningAReferenceDeploymentOnAVisibleCatalogProject() throws Throwable {
        stubVisibleCatalogWorkflow();

        AutomationAuthorizationContext.callSkippingChecks(() -> {
            assertThatCode(() -> guardedEmbeddedReads.createProjectDeployment(CATALOG_PROJECT_ID))
                .doesNotThrowAnyException();

            // The caller's own project is granted too, so the grant above is not standing in for a blanket one.
            assertThatCode(() -> guardedEmbeddedReads.createProjectDeployment(PROJECT_A_ID))
                .doesNotThrowAnyException();

            return null;
        });
    }

    /**
     * The other half: {@code setUp} installs an EMPTY catalog listing, so {@value #CATALOG_PROJECT_ID} is a catalog
     * project this connected user cannot see -- and an entitlement nobody granted grants nothing.
     */
    @Test
    void testDeniesProvisioningAReferenceDeploymentOnACatalogProjectTheCallerCannotSee() throws Throwable {
        AutomationAuthorizationContext.callSkippingChecks(() -> {
            assertThatThrownBy(() -> guardedEmbeddedReads.createProjectDeployment(CATALOG_PROJECT_ID))
                .isInstanceOf(AccessDeniedException.class);

            return null;
        });
    }

    /**
     * The allowlist guard, through the real evaluator and the real resolver, and the half that matters most: seeing a
     * catalog project entitles this connected user to provision a reference deployment against it and to NOTHING else.
     * The owned branch of {@code resolveProject} grants every verb, so an entitled branch that did the same would hand
     * every connected user {@code PROJECT_DELETE}, {@code WORKFLOW_CREATE} and the rest on the tenant admin's catalog
     * project -- and a GRANT returns ahead of the tenant-admin check and RBAC, so nothing downstream would refuse it.
     */
    @Test
    void testDeniesEveryOtherProjectScopeOnAVisibleCatalogProject() throws Throwable {
        stubVisibleCatalogWorkflow();

        AutomationAuthorizationContext.callSkippingChecks(() -> {
            assertThatThrownBy(() -> guardedEmbeddedReads.addWorkflow(CATALOG_PROJECT_ID))
                .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> guardedEmbeddedReads.deleteProject(CATALOG_PROJECT_ID))
                .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> guardedEmbeddedReads.pullProjectFromGit(CATALOG_PROJECT_ID))
                .isInstanceOf(AccessDeniedException.class);

            // ... while the caller's own project keeps granting all three.
            assertThatCode(() -> guardedEmbeddedReads.addWorkflow(PROJECT_A_ID)).doesNotThrowAnyException();
            assertThatCode(() -> guardedEmbeddedReads.deleteProject(PROJECT_A_ID)).doesNotThrowAnyException();
            assertThatCode(() -> guardedEmbeddedReads.pullProjectFromGit(PROJECT_A_ID)).doesNotThrowAnyException();

            return null;
        });
    }

    /**
     * An admin-owned catalog workflow on {@value #CATALOG_PROJECT_ID} that connected user A can SEE in the published
     * listing -- matched by the template uuid, the key the catalog listing itself uses. Overrides the empty listing
     * {@code setUp} installs.
     */
    private void stubVisibleCatalogWorkflow() {
        when(projectService.fetchWorkflowProject(CATALOG_WORKFLOW_ID))
            .thenReturn(Optional.of(project(CATALOG_PROJECT_ID)));

        ProjectWorkflow catalogProjectWorkflow = mock(ProjectWorkflow.class);

        when(catalogProjectWorkflow.getUuidAsString()).thenReturn(CATALOG_WORKFLOW_UUID);
        when(projectWorkflowService.fetchWorkflowProjectWorkflow(CATALOG_WORKFLOW_ID))
            .thenReturn(Optional.of(catalogProjectWorkflow));

        ConnectedUserWorkflowTemplateDTO template = new ConnectedUserWorkflowTemplateDTO(
            CATALOG_WORKFLOW_UUID, "Template", "", null, List.of(), List.of(), null);

        AutomationWorkflowProjectDTO catalogProject = new AutomationWorkflowProjectDTO(
            CATALOG_PROJECT_ID, "Catalog", "", null, List.of(), true, 1, 1, List.of(template), null, false);

        when(automationWorkflowProjectFacade.getPublishedProjects(EXTERNAL_USER_A, Environment.PRODUCTION))
            .thenReturn(List.of(catalogProject));
    }

    private static ConnectedUserProjectWorkflow connectedUserProjectWorkflow(long projectDeploymentId) {
        ConnectedUserProjectWorkflow connectedUserProjectWorkflow = new ConnectedUserProjectWorkflow();

        connectedUserProjectWorkflow.setProjectDeploymentId(projectDeploymentId);

        return connectedUserProjectWorkflow;
    }

    /**
     * The embedded workflow builder's property dropdowns. {@code WorkflowNodeOptionFacadeImpl}'s two methods carried no
     * authorization at all until they were gated with {@code WORKFLOW_VIEW}; that gate is a new denial surface for the
     * connected users who drive that builder, so both halves are asserted here through the real evaluator and the real
     * resolver. A's own workflow passes and B's is refused -- the scope argument is ignored by
     * {@code ConnectedUserResourceMembershipResolver#resolve}, so {@code WORKFLOW_VIEW} and {@code WORKFLOW_EDIT} get
     * the identical answer and this covers the whole family.
     */
    @Test
    void testGrantsOwnWorkflowIdAndDeniesAnotherOnWorkflowNodeOptions() throws Throwable {
        AutomationAuthorizationContext.callSkippingChecks(() -> {
            assertThatCode(() -> guardedEmbeddedReads.getWorkflowNodeOptions(WORKFLOW_A_ID)).doesNotThrowAnyException();

            assertThatThrownBy(() -> guardedEmbeddedReads.getWorkflowNodeOptions(WORKFLOW_B_ID))
                .isInstanceOf(AccessDeniedException.class);

            return null;
        });
    }

    /**
     * A connected user with no {@code ConnectedUserProject} at all owns nothing: every answer denies, rather than
     * falling back to the ordinary path.
     */
    @Test
    void testConnectedUserWithoutProjectOwnsNothing() throws Throwable {
        when(connectedUserProjectService.fetchConnectUserProject(EXTERNAL_USER_A, Environment.PRODUCTION))
            .thenReturn(Optional.empty());

        AutomationAuthorizationContext.callSkippingChecks(() -> {
            assertThatThrownBy(
                () -> workflowNodeScriptFacade.testWorkflowNodeScript(WORKFLOW_A_ID, "node-1", 0L, Map.of()))
                    .isInstanceOf(AccessDeniedException.class);

            return null;
        });
    }

    /**
     * Ticket 1051 Critical 1: the embedded builder's Run button. The client posts
     * {@code /workflows/{id}/tests?environmentId=…} ({@code client/src/shared/util/testWorkflow-utils.ts:93}), which
     * {@code WorkflowTestApiController.startWorkflowTest} gates with the {@code hasWorkflowScopeInEnvironment(...)}
     * SpEL built-in rather than {@code hasPermission(...)}. That built-in reads the skip mode directly, so before this
     * stage wired the decider into it, connected user A could press Run on connected user B's workflow and pass —
     * executing B's nodes with B's stored connections, the design's named worst case, through a door
     * {@code hasResourceScope} never sees.
     *
     * <p>
     * Armed with skip mode, which is what {@code @SkipAutomationAuthorization} enters on embedded's own facades. It is
     * the only skip mode left: ticket 1051 Stage 4 removed the narrower resource-scoped one this test was originally
     * written against, once the copilot worker threads — its last arming sites — began binding the caller's tenant and
     * so became governed like every other embedded path.
     */
    @Test
    void testDeniesAnotherConnectedUsersWorkflowIdOnStartWorkflowTest() throws Throwable {
        AutomationAuthorizationContext.callSkippingChecks(() -> {
            assertThatThrownBy(
                () -> workflowTestApiController.startWorkflowTest(WORKFLOW_B_ID, PRODUCTION_ENVIRONMENT_ID, null))
                    .isInstanceOf(AccessDeniedException.class);

            return null;
        });
    }

    /**
     * The positive control for Run: A's own workflow still passes the gate. Only the gate is asserted — whatever the
     * controller body does with its mocked collaborators afterwards is beside the point.
     */
    @Test
    void testGrantsOwnWorkflowIdOnStartWorkflowTest() throws Throwable {
        AutomationAuthorizationContext.callSkippingChecks(() -> {
            assertGatePassed(
                () -> workflowTestApiController.startWorkflowTest(WORKFLOW_A_ID, PRODUCTION_ENVIRONMENT_ID, null));

            return null;
        });
    }

    /**
     * The same denial with no skip mode armed at all — the state a governed principal is actually in, since
     * {@code SkipAutomationAuthorizationAspect} declines to arm anything for one.
     */
    @Test
    void testDeniesAnotherConnectedUsersWorkflowIdOnStartWorkflowTestWithoutSkip() throws Throwable {
        AutomationAuthorizationContext.callSkippingChecks(() -> {
            assertThatThrownBy(
                () -> workflowTestApiController.startWorkflowTest(WORKFLOW_B_ID, PRODUCTION_ENVIRONMENT_ID, null))
                    .isInstanceOf(AccessDeniedException.class);

            return null;
        });
    }

    /**
     * {@link GuardedEmbeddedReads} re-declares three production {@code @PreAuthorize} expressions, so on its own it
     * would stay green with any of them deleted. This pins each stand-in expression to the production annotation it
     * stands for; the two cannot drift without turning this red. Same pattern as
     * {@code WorkflowTestApiControllerAuthorizationTest.assertExpression}.
     */
    @Test
    void testStandInExpressionsMatchTheirProductionAnnotations() throws ClassNotFoundException, NoSuchMethodException {
        assertThat(preAuthorizeValue(GuardedEmbeddedReads.class.getMethod("addWorkflow", long.class)))
            .isEqualTo(
                preAuthorizeValue(
                    ProjectWorkflowFacadeImpl.class.getMethod("addWorkflow", long.class, String.class)));

        // Resolved by name because the GraphQL controller is package-private; a rename or move fails this test with
        // ClassNotFoundException, which is the drift signal we want either way.
        assertThat(preAuthorizeValue(GuardedEmbeddedReads.class.getMethod("useConnection", long.class)))
            .isEqualTo(
                preAuthorizeValue(
                    Class
                        .forName(
                            "com.bytechef.platform.configuration.web.graphql.WorkflowTestConfigurationGraphQlController")
                        .getMethod(
                            "saveClusterElementTestConfigurationConnection", String.class, String.class, String.class,
                            String.class, String.class, long.class, long.class)));

        assertThat(preAuthorizeValue(GuardedEmbeddedReads.class.getMethod("viewExecution", long.class)))
            .isEqualTo(
                preAuthorizeValue(
                    ProjectWorkflowExecutionFacadeImpl.class.getMethod("getWorkflowExecution", long.class)));

        assertThat(preAuthorizeValue(GuardedEmbeddedReads.class.getMethod("getWorkflowNodeOptions", String.class)))
            .isEqualTo(
                preAuthorizeValue(
                    WorkflowNodeOptionFacadeImpl.class.getMethod(
                        "getWorkflowNodeOptions", String.class, String.class, String.class, List.class, String.class,
                        long.class)));

        assertThat(preAuthorizeValue(GuardedEmbeddedReads.class.getMethod("deleteProject", long.class)))
            .isEqualTo(preAuthorizeValue(ProjectServiceImpl.class.getMethod("delete", long.class)));

        assertThat(preAuthorizeValue(GuardedEmbeddedReads.class.getMethod("pullProjectFromGit", long.class)))
            .isEqualTo(preAuthorizeValue(ProjectGitFacadeImpl.class.getMethod("pullProjectFromGit", long.class)));
    }

    /**
     * The provisioning stand-in cannot be pinned by string equality -- production names the id through
     * {@code #projectDeployment.projectId} while the stand-in takes a bare {@code long} -- so the load-bearing half is
     * pinned directly instead: the resource type and the scope TOKEN. {@code CATALOG_PROVISIONABLE_PROJECT_SCOPES}
     * contains exactly {@code DEPLOYMENT_PUSH}, so retokenising this gate would silently stop the catalog arm covering
     * the flow it exists for, and first-use provisioning would go back to 403ing.
     */
    @Test
    void testProvisioningGateStillUsesDeploymentPushOnProject() throws NoSuchMethodException {
        assertThat(preAuthorizeValue(ProjectDeploymentServiceImpl.class.getMethod("create", ProjectDeployment.class)))
            .isEqualTo("hasPermission(#projectDeployment.projectId, 'Project', 'DEPLOYMENT_PUSH')");
    }

    /**
     * The Run gate is asserted against the real controller above, so this only pins that the production expression is
     * still the environment-aware built-in — the one the decider had to be wired into — rather than having been
     * rewritten to {@code hasPermission(...)}, which would silently move the coverage elsewhere.
     */
    @Test
    void testStartWorkflowTestStillUsesTheEnvironmentAwareBuiltIn() throws NoSuchMethodException {
        assertThat(
            preAuthorizeValue(
                WorkflowTestApiController.class.getMethod(
                    "startWorkflowTest", String.class, Long.class, TestWorkflowRequest.class)))
                        .isEqualTo("hasWorkflowScopeInEnvironment(#id, 'WORKFLOW_EDIT', #environmentId)");
    }

    /**
     * The positive control the previous round lacked, and the one that would have caught the regression it shipped: a
     * real connected-user principal, {@code EnvironmentContext} genuinely unbound as it is on every embedded request,
     * granted its OWN workflow. Bind the thread-local in setUp and this test passes for the wrong reason; derive the
     * environment from the thread-local instead of the principal and it 403s.
     */
    @Test
    void testConnectedUserIsGrantedItsOwnWorkflowWithNoEnvironmentContextBound() throws Throwable {
        assertThat(EnvironmentContextProbe.isBound()).isFalse();

        AutomationAuthorizationContext.callSkippingChecks(() -> {
            assertGatePassed(
                () -> workflowNodeScriptFacade.testWorkflowNodeScript(WORKFLOW_A_ID, "node-1", 0L, Map.of()));
            assertGatePassed(
                () -> workflowTestApiController.startWorkflowTest(WORKFLOW_A_ID, PRODUCTION_ENVIRONMENT_ID, null));
            assertThatCode(() -> guardedEmbeddedReads.useConnection(CONNECTION_A_ID)).doesNotThrowAnyException();
            assertThatCode(() -> guardedEmbeddedReads.addWorkflow(PROJECT_A_ID)).doesNotThrowAnyException();
            assertThatCode(() -> guardedEmbeddedReads.viewExecution(JOB_A_ID)).doesNotThrowAnyException();

            return null;
        });

        verifyNoInteractions(permissionService);
    }

    // -- Ticket 1051: the effective environment ---------------------------------------------------------------------

    /**
     * The exact case the reverted environment-match check 403'd, and the reason it was reverted: the embedded client
     * sends {@code environmentId=0} because {@code useEnvironmentStore} defaults to DEVELOPMENT and the embed never
     * moves it, while the principal is in PRODUCTION because the handshake and the converter both default there. The
     * caller asked for nothing unusual, so it must be GRANTED -- and the run must happen in PRODUCTION, the environment
     * actually authorised, not in the 0 the client happened to send.
     *
     * <p>
     * Asserts the value reaching the executor, not just the gate. Gating one environment and executing in another is
     * the bug; a test that stopped at the gate would pass with the hole wide open.
     */
    @Test
    void testConnectedUserRunIsGrantedAndExecutesInThePrincipalsEnvironment() throws Throwable {
        AutomationAuthorizationContext.callSkippingChecks(() -> {
            assertGatePassed(
                () -> workflowTestApiController.startWorkflowTest(
                    WORKFLOW_A_ID, (long) Environment.DEVELOPMENT.ordinal(), null));

            return null;
        });

        verify(testWorkflowExecutor).executeAsync(
            eq(WORKFLOW_A_ID), any(), eq((long) Environment.PRODUCTION.ordinal()), any(), any(), any(), any());
    }

    /**
     * The other half of the same property: the parameter is inert, so it cannot be used to reach an environment the
     * principal is not in either. A PRODUCTION connected user asking for STAGING still runs in PRODUCTION -- the
     * request never gets a say, which is why there is nothing left to mismatch.
     */
    @Test
    void testConnectedUserCannotReachAnotherEnvironmentByAskingForIt() throws Throwable {
        AutomationAuthorizationContext.callSkippingChecks(() -> {
            assertGatePassed(
                () -> workflowTestApiController.startWorkflowTest(
                    WORKFLOW_A_ID, (long) Environment.STAGING.ordinal(), null));

            return null;
        });

        verify(testWorkflowExecutor).executeAsync(
            eq(WORKFLOW_A_ID), any(), eq((long) Environment.PRODUCTION.ordinal()), any(), any(), any(), any());
    }

    /**
     * Containment, and the reason substitution is safe: an ordinary platform user carries no api-key token, so it is
     * not confined to an environment and its request parameter is honoured unchanged -- at the gate AND at the
     * executor. Substituting for such a caller would break every non-embedded Run.
     */
    @Test
    void testSessionPrincipalKeepsItsRequestedEnvironment() throws Throwable {
        authenticateAs(PLATFORM_USER);

        when(connectedUserService.fetchConnectedUser(PLATFORM_USER, Environment.PRODUCTION))
            .thenReturn(Optional.empty());

        AutomationAuthorizationContext.callSkippingChecks(() -> {
            assertGatePassed(
                () -> workflowTestApiController.startWorkflowTest(
                    WORKFLOW_A_ID, (long) Environment.DEVELOPMENT.ordinal(), null));

            return null;
        });

        verify(testWorkflowExecutor).executeAsync(
            eq(WORKFLOW_A_ID), any(), eq((long) Environment.DEVELOPMENT.ordinal()), any(), any(), any(), any());
    }

    /**
     * The mirror image: connected user B is denied A's resources for the same reason, so the denials above are
     * membership rather than an artefact of A's fixture.
     */
    @Test
    void testDeniesTheReverseDirectionToo() throws Throwable {
        authenticateAsConnectedUser(EXTERNAL_USER_B);

        ConnectedUserProject connectedUserProjectB = new ConnectedUserProject();

        connectedUserProjectB.setConnectedUserId(CONNECTED_USER_B_ID);
        connectedUserProjectB.setProjectId(PROJECT_B_ID);

        when(connectedUserService.fetchConnectedUser(EXTERNAL_USER_B, Environment.PRODUCTION))
            .thenReturn(Optional.of(mock(ConnectedUser.class)));
        when(connectedUserProjectService.fetchConnectUserProject(EXTERNAL_USER_B, Environment.PRODUCTION))
            .thenReturn(Optional.of(connectedUserProjectB));
        when(integrationInstanceService.getConnectedUserIntegrationInstances(
            CONNECTED_USER_B_ID, Environment.PRODUCTION)).thenReturn(List.of());
        when(connectedUserConnectionService.getConnectionIds(CONNECTED_USER_B_ID)).thenReturn(List.of());

        AutomationAuthorizationContext.callSkippingChecks(() -> {
            assertThatThrownBy(
                () -> workflowNodeScriptFacade.testWorkflowNodeScript(WORKFLOW_A_ID, "node-1", 0L, Map.of()))
                    .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> guardedEmbeddedReads.useConnection(CONNECTION_A_ID))
                .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> guardedEmbeddedReads.addWorkflow(PROJECT_A_ID))
                .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> guardedEmbeddedReads.viewExecution(JOB_A_ID))
                .isInstanceOf(AccessDeniedException.class);

            assertThatCode(
                () -> workflowNodeScriptFacade.testWorkflowNodeScript(WORKFLOW_B_ID, "node-1", 0L, Map.of()))
                    .doesNotThrowAnyException();

            return null;
        });
    }

    /**
     * Blast-radius containment, end to end: a principal that is not a connected user is not governed, so full skip goes
     * on granting exactly as it did before this stage — and the resolver's membership queries are never run for it.
     */
    @Test
    void testUngovernedPrincipalStillGrantedByFullSkip() throws Throwable {
        authenticateAs(PLATFORM_USER);

        when(connectedUserService.fetchConnectedUser(PLATFORM_USER, Environment.PRODUCTION))
            .thenReturn(Optional.empty());

        AutomationAuthorizationContext.callSkippingChecks(() -> {
            assertThatCode(
                () -> workflowNodeScriptFacade.testWorkflowNodeScript(WORKFLOW_B_ID, "node-1", 0L, Map.of()))
                    .doesNotThrowAnyException();
            assertThatCode(() -> guardedEmbeddedReads.useConnection(CONNECTION_B_ID)).doesNotThrowAnyException();
            assertThatCode(() -> guardedEmbeddedReads.addWorkflow(PROJECT_B_ID)).doesNotThrowAnyException();
            assertThatCode(() -> guardedEmbeddedReads.viewExecution(JOB_B_ID)).doesNotThrowAnyException();

            return null;
        });

        verifyNoInteractions(connectedUserProjectService, permissionService);
    }

    /**
     * The same principal with no skip mode armed falls through to the ordinary RBAC path, which is stubbed to grant —
     * the seam has not displaced it.
     */
    @Test
    void testUngovernedPrincipalWithoutSkipStillReachesPermissionService() {
        authenticateAs(PLATFORM_USER);

        when(connectedUserService.fetchConnectedUser(PLATFORM_USER, Environment.PRODUCTION))
            .thenReturn(Optional.empty());

        assertThatCode(() -> workflowNodeScriptFacade.testWorkflowNodeScript(WORKFLOW_B_ID, "node-1", 0L, Map.of()))
            .doesNotThrowAnyException();
    }

    /**
     * Asserts only that the {@code @PreAuthorize} gate was passed. The method body then runs against mocked
     * collaborators and may fail on its own terms; that is not what these tests are about.
     */
    private static void assertGatePassed(ThrowingCallable throwingCallable) {
        Throwable throwable = catchThrowable(throwingCallable);

        if (throwable != null) {
            assertThat(throwable).isNotInstanceOf(AccessDeniedException.class);
        }
    }

    private static String preAuthorizeValue(Method method) {
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize)
            .as("%s.%s carries no @PreAuthorize", method.getDeclaringClass(), method.getName())
            .isNotNull();

        return preAuthorize.value();
    }

    /**
     * An embedded connected-user principal, built exactly as {@code EmbeddedApiKeyAuthenticationProvider} builds the
     * authenticated token that {@code ApiKeyAuthenticationFilter} puts in the {@code SecurityContext} -- environment
     * included, which is the only place the resolver can now get it from.
     */
    private static void authenticateAsConnectedUser(String externalUserId) {
        SecurityContextHolder.getContext()
            .setAuthentication(
                new EmbeddedApiKeyAuthenticationToken(
                    Environment.PRODUCTION.ordinal(), new User(externalUserId, "", List.of())));
    }

    /**
     * A platform principal: no api-key token, so no environment and nothing for the resolver to govern.
     */
    private static void authenticateAs(String login) {
        SecurityContextHolder.getContext()
            .setAuthentication(
                new UsernamePasswordAuthenticationToken(
                    login, "n/a", List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    /**
     * Lets the test assert its own precondition -- that nothing bound the thread-local -- rather than trusting it.
     */
    private static final class EnvironmentContextProbe {

        private EnvironmentContextProbe() {
        }

        static boolean isBound() {
            return com.bytechef.platform.configuration.context.EnvironmentContext.fetchCurrentEnvironment() != null;
        }
    }

    private static ConnectedUserProject connectedUserProject() {
        ConnectedUserProject connectedUserProject = new ConnectedUserProject();

        connectedUserProject.setId(CONNECTED_USER_PROJECT_A_ID);
        connectedUserProject.setConnectedUserId(CONNECTED_USER_A_ID);
        connectedUserProject.setProjectId(PROJECT_A_ID);

        return connectedUserProject;
    }

    private static Job job(String workflowId) {
        Job job = new Job();

        job.setWorkflowId(workflowId);

        return job;
    }

    private static Project project(long id) {
        Project project = new Project();

        project.setId(id);

        return project;
    }

    // proxyTargetClass so WorkflowTestApiController -- whose startWorkflowTest is a hand-written @PostMapping, not
    // part of the generated WorkflowTestApi interface it implements -- is reachable through its own type rather than
    // only through that interface.
    @SpringBootConfiguration
    @EnableMethodSecurity(proxyTargetClass = true)
    @ImportAutoConfiguration(AutomationMethodSecurityConfiguration.class)
    @Import({
        ConnectedUserConnectionMembership.class, ConnectedUserResourceMembershipResolver.class,
        GuardedEmbeddedReads.class
    })
    static class Config {

        @Bean
        AutomationWorkflowProjectFacade automationWorkflowProjectFacade() {
            return mock(AutomationWorkflowProjectFacade.class);
        }

        @Bean
        ConnectedUserConnectionService connectedUserConnectionService() {
            return mock(ConnectedUserConnectionService.class);
        }

        @Bean
        ConnectedUserProjectService connectedUserProjectService() {
            return mock(ConnectedUserProjectService.class);
        }

        @Bean
        ConnectedUserProjectWorkflowService connectedUserProjectWorkflowService() {
            return mock(ConnectedUserProjectWorkflowService.class);
        }

        @Bean
        ConnectedUserService connectedUserService() {
            return mock(ConnectedUserService.class);
        }

        @Bean
        IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService() {
            return mock(IntegrationInstanceConfigurationWorkflowService.class);
        }

        @Bean
        IntegrationInstanceService integrationInstanceService() {
            return mock(IntegrationInstanceService.class);
        }

        @Bean
        JobService jobService() {
            return mock(JobService.class);
        }

        @Bean("permissionService")
        PermissionService permissionService() {
            return mock(PermissionService.class);
        }

        @Bean
        PrincipalJobService principalJobService() {
            return mock(PrincipalJobService.class);
        }

        @Bean
        ProjectDeploymentService projectDeploymentService() {
            return mock(ProjectDeploymentService.class);
        }

        @Bean
        ProjectService projectService() {
            return mock(ProjectService.class);
        }

        @Bean
        ProjectWorkflowService projectWorkflowService() {
            return mock(ProjectWorkflowService.class);
        }

        @Bean
        TestWorkflowExecutor testWorkflowExecutor() {
            return mock(TestWorkflowExecutor.class);
        }

        /**
         * Returns a workflow with no triggers, so {@code startWorkflowTest} runs its body to the executeAsync call
         * instead of failing earlier -- the environment the run is dispatched with is what these tests are about.
         */
        @Bean
        WorkflowService testWorkflowService() {
            WorkflowService workflowService = mock(WorkflowService.class);
            Workflow workflow = mock(Workflow.class);

            when(workflow.getExtensions(anyString(), any(), anyList())).thenReturn(List.of());
            when(workflowService.getWorkflow(anyString())).thenReturn(workflow);

            return workflowService;
        }

        /**
         * The real controller, so that the Run gate this test asserts is the production annotation rather than a
         * restatement of it, and so the environment reaching the executor is the production resolution.
         */
        @Bean
        WorkflowTestApiController workflowTestApiController(
            TestWorkflowExecutor testWorkflowExecutor, WorkflowService testWorkflowService) {

            return new WorkflowTestApiController(
                mock(TempFileStorage.class), testWorkflowExecutor, testWorkflowService);
        }

        /**
         * The real facade, so that deleting its {@code @PreAuthorize} turns this test red. Its collaborators are
         * mocked; the body swallows whatever they answer, which is enough for a test that only cares whether the gate
         * was passed.
         */
        @Bean
        WorkflowNodeScriptFacade workflowNodeScriptFacade() {
            return new WorkflowNodeScriptFacadeImpl(
                List.of(), mock(ConnectionService.class), mock(Evaluator.class),
                mock(WorkflowEvaluationInputsFacade.class), mock(WorkflowNodeOutputFacade.class),
                mock(WorkflowNodeTestOutputFacade.class), mock(WorkflowService.class),
                mock(WorkflowTestConfigurationService.class));
        }
    }

    /**
     * Stand-in carrying the {@code @PreAuthorize} expressions the Connection-, Project- and Job-keyed production sites
     * carry. It pins the expression form reaching the evaluator; the production annotations themselves are pinned
     * elsewhere.
     */
    @Service
    static class GuardedEmbeddedReads {

        @PreAuthorize("hasPermission(#projectId, 'Project', 'WORKFLOW_CREATE')")
        public void addWorkflow(long projectId) {
        }

        /**
         * {@code ProjectServiceImpl.delete} -- a {@code 'Project'}-keyed token from a DIFFERENT enum
         * ({@code ProjectPermissionScope}) than the one the catalog arm allows, so it shows the allowlist is not merely
         * excluding the rest of {@code DeploymentPermissionScope}.
         */
        @PreAuthorize("hasPermission(#id, 'Project', 'PROJECT_DELETE')")
        public void deleteProject(long id) {
        }

        /**
         * EE {@code ProjectGitFacadeImpl.pullProjectFromGit} -- {@code DEPLOYMENT_PULL}, the sibling
         * {@code DeploymentPermissionScope} token the catalog arm must NOT grant just because it grants
         * {@code DEPLOYMENT_PUSH}.
         */
        @PreAuthorize("hasPermission(#projectId, 'Project', 'DEPLOYMENT_PULL')")
        public void pullProjectFromGit(long projectId) {
        }

        @PreAuthorize("hasPermission(#connectionId, 'Connection', 'CONNECTION_USE')")
        public void useConnection(long connectionId) {
        }

        @PreAuthorize("hasPermission(#id, 'Job', 'EXECUTION_VIEW')")
        public void viewExecution(long id) {
        }

        @PreAuthorize("hasPermission(#workflowId, 'Workflow', 'WORKFLOW_VIEW')")
        public void getWorkflowNodeOptions(String workflowId) {
        }

        /**
         * {@code ProjectDeploymentFacadeImpl.enableProjectDeploymentWorkflow(long, String, boolean)} -- what
         * {@code ConnectedUserCodeWorkflowReferenceFacadeImpl.enableReference} calls, and the surface behind the
         * automation-hub enable toggle.
         */
        @PreAuthorize("hasPermission(#projectDeploymentId, 'ProjectDeployment', 'DEPLOYMENT_EDIT')")
        public void enableProjectDeploymentWorkflow(long projectDeploymentId) {
        }

        /**
         * {@code ProjectDeploymentServiceImpl.create} -- reached, ungated the whole way, from
         * {@code getOrCreateReference} when a reference is provisioned for the first time. Note the resource type is
         * {@code 'Project'} and the id is the CATALOG project's.
         */
        @PreAuthorize("hasPermission(#projectId, 'Project', 'DEPLOYMENT_PUSH')")
        public void createProjectDeployment(long projectId) {
        }
    }
}
