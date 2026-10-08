/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.security.ConnectedUserAccessDecider.Decision;
import com.bytechef.automation.configuration.security.ResourceEnvironmentResolver;
import com.bytechef.automation.configuration.security.ResourceOwnershipResolver;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProject;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectDTO;
import com.bytechef.ee.embedded.configuration.dto.ConnectedUserWorkflowTemplateDTO;
import com.bytechef.ee.embedded.configuration.facade.AutomationWorkflowProjectFacade;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserConnectionService;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserProjectService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.security.web.authentication.ConnectedUserAuthentication;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class ConnectedUserAccessDeciderTest {

    private static final long OWN_PROJECT_ID = 100L;
    private static final long TEMPLATE_PROJECT_ID = 200L;
    private static final String TEMPLATE_WORKFLOW_UUID = "7a9d4a52-2b0f-4f5e-9a3e-6c1b2d3e4f50";

    @Mock
    private AutomationWorkflowProjectFacade automationWorkflowProjectFacade;

    @Mock
    private ConnectedUserConnectionService connectedUserConnectionService;

    @Mock
    private ConnectedUserProjectService connectedUserProjectService;

    @Mock
    private IntegrationInstanceService integrationInstanceService;

    @Mock
    private ProjectService projectService;

    @Mock
    private ProjectWorkflowService projectWorkflowService;

    private ResourceEnvironmentResolver projectDeploymentEnvironmentResolver;
    private ResourceOwnershipResolver projectDeploymentResolver;
    private ResourceEnvironmentResolver projectDeploymentWorkflowEnvironmentResolver;
    private ResourceOwnershipResolver projectDeploymentWorkflowResolver;
    private ResourceOwnershipResolver projectResolver;
    private ConnectedUserAccessDeciderImpl decider;

    @BeforeEach
    void setUp() {
        projectDeploymentEnvironmentResolver = environmentResolver("ProjectDeployment");
        projectDeploymentResolver = resolver("ProjectDeployment");
        projectDeploymentWorkflowEnvironmentResolver = environmentResolver("ProjectDeploymentWorkflow");
        projectDeploymentWorkflowResolver = resolver("ProjectDeploymentWorkflow");
        projectResolver = resolver("Project");

        decider = new ConnectedUserAccessDeciderImpl(
            automationWorkflowProjectFacade, connectedUserConnectionService, connectedUserProjectService,
            integrationInstanceService, projectService, projectWorkflowService,
            List.of(projectDeploymentEnvironmentResolver, projectDeploymentWorkflowEnvironmentResolver),
            List.of(projectDeploymentResolver, projectDeploymentWorkflowResolver, projectResolver));

        SecurityContextHolder.getContext()
            .setAuthentication(connectedUserToken(42L, "ext-1", Environment.PRODUCTION.ordinal()));

        ConnectedUserProject connectedUserProject = new ConnectedUserProject();

        connectedUserProject.setProjectId(OWN_PROJECT_ID);

        lenient().when(connectedUserProjectService.fetchConnectUserProject("ext-1", Environment.PRODUCTION))
            .thenReturn(Optional.of(connectedUserProject));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testPlatformUserIsNotGoverned() {
        SecurityContextHolder.getContext()
            .setAuthentication(new UsernamePasswordAuthenticationToken("admin", "", List.of()));

        assertThat(decider.decide(1L, "Project", "WORKFLOW_EDIT")).isEqualTo(Decision.NOT_GOVERNED);
        assertThat(decider.decideWorkflow("wf-own", "WORKFLOW_EDIT")).isEqualTo(Decision.NOT_GOVERNED);
        assertThat(decider.decideWorkspace(1049L, "WORKFLOW_VIEW")).isEqualTo(Decision.NOT_GOVERNED);
        assertThat(decider.decideInEnvironment(1L, "Project", "DEPLOYMENT_CREATE", Environment.DEVELOPMENT))
            .isEqualTo(Decision.NOT_GOVERNED);
    }

    @Test
    void testOwnProjectResourceIsGrantedForAnyScope() {
        when(projectDeploymentResolver.resolveProjectId(5L)).thenReturn(OptionalLong.of(OWN_PROJECT_ID));
        when(projectDeploymentEnvironmentResolver.fetchEnvironment(5L)).thenReturn(Optional.of(Environment.PRODUCTION));

        assertThat(decider.decide(5L, "ProjectDeployment", "DEPLOYMENT_EDIT")).isEqualTo(Decision.GRANT);
    }

    @Test
    void testOwnProjectDeploymentInAnotherEnvironmentIsDenied() {
        when(projectDeploymentResolver.resolveProjectId(5L)).thenReturn(OptionalLong.of(OWN_PROJECT_ID));
        when(projectDeploymentEnvironmentResolver.fetchEnvironment(5L))
            .thenReturn(Optional.of(Environment.DEVELOPMENT));

        assertThat(decider.decide(5L, "ProjectDeployment", "DEPLOYMENT_VIEW")).isEqualTo(Decision.DENY);
    }

    @Test
    void testOwnProjectDeploymentWithoutEnvironmentIsDenied() {
        when(projectDeploymentResolver.resolveProjectId(5L)).thenReturn(OptionalLong.of(OWN_PROJECT_ID));
        when(projectDeploymentEnvironmentResolver.fetchEnvironment(5L)).thenReturn(Optional.empty());

        assertThat(decider.decide(5L, "ProjectDeployment", "DEPLOYMENT_VIEW")).isEqualTo(Decision.DENY);
    }

    @Test
    void testOwnProjectDeploymentWorkflowIsGrantedOnlyInOwnEnvironment() {
        when(projectDeploymentWorkflowResolver.resolveProjectId(7L)).thenReturn(OptionalLong.of(OWN_PROJECT_ID));
        when(projectDeploymentWorkflowResolver.resolveProjectId(8L)).thenReturn(OptionalLong.of(OWN_PROJECT_ID));
        when(projectDeploymentWorkflowEnvironmentResolver.fetchEnvironment(7L))
            .thenReturn(Optional.of(Environment.PRODUCTION));
        when(projectDeploymentWorkflowEnvironmentResolver.fetchEnvironment(8L))
            .thenReturn(Optional.of(Environment.STAGING));

        assertThat(decider.decide(7L, "ProjectDeploymentWorkflow", "DEPLOYMENT_EDIT")).isEqualTo(Decision.GRANT);
        assertThat(decider.decide(8L, "ProjectDeploymentWorkflow", "DEPLOYMENT_EDIT")).isEqualTo(Decision.DENY);
    }

    @Test
    void testDeploymentScopeInAnotherEnvironmentIsDenied() {
        when(projectResolver.resolveProjectId(OWN_PROJECT_ID)).thenReturn(OptionalLong.of(OWN_PROJECT_ID));

        assertThat(
            decider.decideInEnvironment(OWN_PROJECT_ID, "Project", "DEPLOYMENT_CREATE", Environment.DEVELOPMENT))
                .isEqualTo(Decision.DENY);
        assertThat(
            decider.decideInEnvironment(OWN_PROJECT_ID, "Project", "DEPLOYMENT_CREATE", Environment.PRODUCTION))
                .isEqualTo(Decision.GRANT);
    }

    @Test
    void testNonDeploymentScopeIgnoresTheRequestedEnvironment() {
        when(projectResolver.resolveProjectId(OWN_PROJECT_ID)).thenReturn(OptionalLong.of(OWN_PROJECT_ID));

        assertThat(decider.decideInEnvironment(OWN_PROJECT_ID, "Project", "WORKFLOW_EDIT", Environment.DEVELOPMENT))
            .isEqualTo(decider.decide(OWN_PROJECT_ID, "Project", "WORKFLOW_EDIT"))
            .isEqualTo(Decision.GRANT);
    }

    @Test
    void testAnotherProjectsResourceIsDenied() {
        when(projectDeploymentResolver.resolveProjectId(6L)).thenReturn(OptionalLong.of(999L));

        assertThat(decider.decide(6L, "ProjectDeployment", "DEPLOYMENT_VIEW")).isEqualTo(Decision.DENY);
    }

    @Test
    void testWorkspaceLevelChecksAreDenied() {
        assertThat(decider.decideWorkspace(1049L, "WORKFLOW_VIEW")).isEqualTo(Decision.DENY);
        assertThat(decider.decide(3L, "DataTable", "DATA_TABLE_VIEW")).isEqualTo(Decision.DENY);
    }

    @Test
    void testUnknownResourceTypeIsDenied() {
        assertThat(decider.decide(3L, "Unheard", "X")).isEqualTo(Decision.DENY);
    }

    @Test
    void testWorkflowOfOwnProjectIsGranted() {
        Project project = new Project();

        project.setId(OWN_PROJECT_ID);

        when(projectService.fetchWorkflowProject("wf-own")).thenReturn(Optional.of(project));

        assertThat(decider.decideWorkflow("wf-own", "WORKFLOW_EDIT")).isEqualTo(Decision.GRANT);
    }

    @Test
    void testIntegrationWorkflowWithoutProjectIsDenied() {
        when(projectService.fetchWorkflowProject("wf-integration")).thenReturn(Optional.empty());

        assertThat(decider.decideWorkflow("wf-integration", "WORKFLOW_EDIT")).isEqualTo(Decision.DENY);
    }

    @Test
    void testPublishedTemplateWorkflowIsViewOnly() {
        Project template = new Project();

        template.setId(TEMPLATE_PROJECT_ID);

        when(projectService.fetchWorkflowProject("wf-template")).thenReturn(Optional.of(template));
        when(projectWorkflowService.getWorkflowProjectWorkflow("wf-template"))
            .thenReturn(projectWorkflow(TEMPLATE_WORKFLOW_UUID, 3));
        when(automationWorkflowProjectFacade.getPublishedProjects("ext-1", Environment.PRODUCTION))
            .thenReturn(List.of(templateDto(TEMPLATE_PROJECT_ID, 3, TEMPLATE_WORKFLOW_UUID)));

        assertThat(decider.decideWorkflow("wf-template", "WORKFLOW_VIEW")).isEqualTo(Decision.GRANT);
        assertThat(decider.decideWorkflow("wf-template", "WORKFLOW_EDIT")).isEqualTo(Decision.DENY);
    }

    @Test
    void testTemplateWorkflowNotPublishedToTheConnectedUserIsDenied() {
        Project template = new Project();

        template.setId(TEMPLATE_PROJECT_ID);

        when(projectService.fetchWorkflowProject("wf-hidden")).thenReturn(Optional.of(template));
        when(projectWorkflowService.getWorkflowProjectWorkflow("wf-hidden"))
            .thenReturn(projectWorkflow("0b6f2c1e-8f7d-4c5b-a1e2-3d4c5b6a7f80", 3));
        when(automationWorkflowProjectFacade.getPublishedProjects("ext-1", Environment.PRODUCTION))
            .thenReturn(List.of(templateDto(TEMPLATE_PROJECT_ID, 3, TEMPLATE_WORKFLOW_UUID)));

        assertThat(decider.decideWorkflow("wf-hidden", "WORKFLOW_VIEW")).isEqualTo(Decision.DENY);
    }

    @Test
    void testDraftVersionOfPublishedTemplateWorkflowIsDenied() {
        Project template = new Project();

        template.setId(TEMPLATE_PROJECT_ID);

        when(projectService.fetchWorkflowProject("wf-draft")).thenReturn(Optional.of(template));
        when(projectWorkflowService.getWorkflowProjectWorkflow("wf-draft"))
            .thenReturn(projectWorkflow(TEMPLATE_WORKFLOW_UUID, 4));
        when(automationWorkflowProjectFacade.getPublishedProjects("ext-1", Environment.PRODUCTION))
            .thenReturn(List.of(templateDto(TEMPLATE_PROJECT_ID, 3, TEMPLATE_WORKFLOW_UUID)));

        assertThat(decider.decideWorkflow("wf-draft", "WORKFLOW_VIEW")).isEqualTo(Decision.DENY);
    }

    @Test
    void testOwnConnectionIsGranted() {
        when(connectedUserConnectionService.getConnectionIds(42L)).thenReturn(List.of(8L));

        assertThat(decider.decide(8L, "Connection", "CONNECTION_VIEW")).isEqualTo(Decision.GRANT);
        assertThat(decider.decide(9L, "Connection", "CONNECTION_VIEW")).isEqualTo(Decision.DENY);
    }

    @Test
    void testConnectionOfOwnIntegrationInstanceIsGranted() {
        IntegrationInstance integrationInstance = new IntegrationInstance();

        integrationInstance.setConnectionId(11L);

        when(connectedUserConnectionService.getConnectionIds(42L)).thenReturn(List.of());
        when(integrationInstanceService.getConnectedUserIntegrationInstances(42L, Environment.PRODUCTION))
            .thenReturn(List.of(integrationInstance));

        assertThat(decider.decide(11L, "Connection", "CONNECTION_EDIT")).isEqualTo(Decision.GRANT);
        assertThat(decider.decide(12L, "Connection", "CONNECTION_EDIT")).isEqualTo(Decision.DENY);
    }

    @Test
    void testMcpServerAndMcpToolAreDeniedForAConnectedUser() {
        assertThat(decider.decide(1L, "McpServer", "MCP_VIEW")).isEqualTo(Decision.DENY);
        assertThat(decider.decide(30L, "McpTool", "MCP_VIEW")).isEqualTo(Decision.DENY);
    }

    @Test
    void testRequestedEnvironmentIsIgnoredForOwnProject() {
        Project project = new Project();

        project.setId(OWN_PROJECT_ID);

        when(projectService.fetchWorkflowProject("wf-own")).thenReturn(Optional.of(project));

        assertThat(decider.decideWorkflow("wf-own", "WORKFLOW_EDIT")).isEqualTo(Decision.GRANT);
        verify(connectedUserProjectService).fetchConnectUserProject("ext-1", Environment.PRODUCTION);
    }

    @Test
    void testNoConnectedUserProjectDenies() {
        when(connectedUserProjectService.fetchConnectUserProject("ext-1", Environment.PRODUCTION))
            .thenReturn(Optional.empty());
        lenient().when(projectResolver.resolveProjectId(OWN_PROJECT_ID))
            .thenReturn(OptionalLong.of(OWN_PROJECT_ID));

        assertThat(decider.decide(OWN_PROJECT_ID, "Project", "WORKFLOW_EDIT")).isEqualTo(Decision.DENY);
        verify(connectedUserProjectService, never()).create(anyLong(), anyLong());
        verify(connectedUserProjectService, never()).create(any(ConnectedUserProject.class));
        verify(projectService, never()).create(any(Project.class));
    }

    @Test
    void testLookupFailureDenies() {
        when(projectDeploymentResolver.resolveProjectId(5L)).thenThrow(new IllegalStateException("db down"));

        assertThat(decider.decide(5L, "ProjectDeployment", "DEPLOYMENT_VIEW")).isEqualTo(Decision.DENY);
    }

    @Test
    void testWorkflowLookupFailureDenies() {
        when(projectService.fetchWorkflowProject("wf-own")).thenThrow(new IllegalStateException("db down"));

        assertThat(decider.decideWorkflow("wf-own", "WORKFLOW_VIEW")).isEqualTo(Decision.DENY);
    }

    @Test
    void testEachGovernedDecisionIsLoggedAtDebugWithThePrincipalAndResource() {
        Logger deciderLogger = (Logger) LoggerFactory.getLogger(ConnectedUserAccessDeciderImpl.class);
        Level previousLevel = deciderLogger.getLevel();
        ListAppender<ILoggingEvent> logAppender = new ListAppender<>();

        logAppender.start();

        deciderLogger.addAppender(logAppender);
        deciderLogger.setLevel(Level.DEBUG);

        try {
            when(projectDeploymentResolver.resolveProjectId(6L)).thenReturn(OptionalLong.of(999L));

            decider.decide(6L, "ProjectDeployment", "DEPLOYMENT_VIEW");

            assertThat(logAppender.list)
                .extracting(ILoggingEvent::getFormattedMessage)
                .containsExactly("Connected user 42 in environment 2: ProjectDeployment 6 DEPLOYMENT_VIEW -> DENY");
        } finally {
            deciderLogger.detachAppender(logAppender);
            deciderLogger.setLevel(previousLevel);
        }
    }

    @Test
    void testApiPlatformScopesAreDeniedEvenOnTheOwnProject() {
        Project project = new Project();

        project.setId(OWN_PROJECT_ID);

        lenient().when(projectResolver.resolveProjectId(OWN_PROJECT_ID))
            .thenReturn(OptionalLong.of(OWN_PROJECT_ID));
        lenient().when(projectService.fetchWorkflowProject("wf-own"))
            .thenReturn(Optional.of(project));

        assertThat(decider.decide(OWN_PROJECT_ID, "Project", "API_PLATFORM_CREATE")).isEqualTo(Decision.DENY);
        assertThat(
            decider.decideInEnvironment(OWN_PROJECT_ID, "Project", "API_PLATFORM_CREATE", Environment.PRODUCTION))
                .isEqualTo(Decision.DENY);
        assertThat(decider.decideWorkflow("wf-own", "API_PLATFORM_VIEW")).isEqualTo(Decision.DENY);
    }

    @Test
    void testDeploymentScopeWithoutAnEnvironmentIsDenied() {
        lenient().when(projectResolver.resolveProjectId(OWN_PROJECT_ID))
            .thenReturn(OptionalLong.of(OWN_PROJECT_ID));

        assertThat(decider.decideInEnvironment(OWN_PROJECT_ID, "Project", "DEPLOYMENT_CREATE", null))
            .isEqualTo(Decision.DENY);
    }

    @Test
    void testOutOfRangeEnvironmentDenies() {
        SecurityContextHolder.getContext()
            .setAuthentication(connectedUserToken(42L, "ext-1", 99L));

        Project project = new Project();

        project.setId(OWN_PROJECT_ID);

        when(projectResolver.resolveProjectId(OWN_PROJECT_ID)).thenReturn(OptionalLong.of(OWN_PROJECT_ID));
        when(projectService.fetchWorkflowProject("wf-own")).thenReturn(Optional.of(project));

        assertThat(decider.decide(OWN_PROJECT_ID, "Project", "WORKFLOW_VIEW")).isEqualTo(Decision.DENY);
        assertThat(decider.decideWorkflow("wf-own", "WORKFLOW_VIEW")).isEqualTo(Decision.DENY);
    }

    private static TestConnectedUserAuthentication connectedUserToken(
        long connectedUserId, String externalUserId, long environmentId) {

        return new TestConnectedUserAuthentication(connectedUserId, externalUserId, environmentId);
    }

    private static ProjectWorkflow projectWorkflow(String workflowUuid, int projectVersion) {
        ProjectWorkflow projectWorkflow = new ProjectWorkflow();

        projectWorkflow.setUuid(workflowUuid);
        projectWorkflow.setProjectVersion(projectVersion);

        return projectWorkflow;
    }

    private static ResourceEnvironmentResolver environmentResolver(String resourceType) {
        ResourceEnvironmentResolver resourceEnvironmentResolver = mock(ResourceEnvironmentResolver.class);

        when(resourceEnvironmentResolver.resourceType()).thenReturn(resourceType);

        return resourceEnvironmentResolver;
    }

    private static ResourceOwnershipResolver resolver(String resourceType) {
        ResourceOwnershipResolver resourceOwnershipResolver = mock(ResourceOwnershipResolver.class);

        when(resourceOwnershipResolver.resourceType()).thenReturn(resourceType);

        return resourceOwnershipResolver;
    }

    private static AutomationWorkflowProjectDTO templateDto(
        long projectId, int lastPublishedVersion, String workflowUuid) {

        ConnectedUserWorkflowTemplateDTO workflowTemplate = new ConnectedUserWorkflowTemplateDTO(
            workflowUuid, "Template", null, null, List.of(), List.of(), null);

        return new AutomationWorkflowProjectDTO(
            projectId, "Templates", null, null, List.of(), true, lastPublishedVersion + 1, lastPublishedVersion,
            List.of(workflowTemplate), null);
    }

    private static final class TestConnectedUserAuthentication extends AbstractAuthenticationToken
        implements ConnectedUserAuthentication {

        private final long connectedUserId;
        private final String externalUserId;
        private final long environmentId;

        private TestConnectedUserAuthentication(long connectedUserId, String externalUserId, long environmentId) {
            super(List.of());

            this.connectedUserId = connectedUserId;
            this.externalUserId = externalUserId;
            this.environmentId = environmentId;

            setAuthenticated(true);
        }

        @Override
        public Object getCredentials() {
            return null;
        }

        @Override
        public Object getPrincipal() {
            return externalUserId;
        }

        @Override
        public long connectedUserId() {
            return connectedUserId;
        }

        @Override
        public String externalUserId() {
            return externalUserId;
        }

        @Override
        public long environmentId() {
            return environmentId;
        }
    }
}
