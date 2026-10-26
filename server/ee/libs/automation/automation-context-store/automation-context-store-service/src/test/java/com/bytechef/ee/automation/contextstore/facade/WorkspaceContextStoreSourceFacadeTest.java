/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.contextstore.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.component.definition.datastream.ItemReader;
import com.bytechef.ee.automation.contextstore.audit.ContextStoreSourceAuditPublisher;
import com.bytechef.ee.automation.contextstore.dto.CreateContextStoreSourceInput;
import com.bytechef.ee.automation.contextstore.service.WorkspaceContextStoreService;
import com.bytechef.ee.platform.contextstore.clickhouse.ClickHouseTableProvisioner;
import com.bytechef.ee.platform.contextstore.domain.ContextStore;
import com.bytechef.ee.platform.contextstore.domain.ContextStoreSource;
import com.bytechef.ee.platform.contextstore.service.ContextStoreService;
import com.bytechef.ee.platform.contextstore.service.ContextStoreSourceService;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.component.domain.ClusterElementDefinition;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.workflow.execution.facade.PrincipalJobFacade;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.task.TaskExecutor;

/**
 * @version ee
 */
@ExtendWith({
    MockitoExtension.class, ObjectMapperSetupExtension.class
})
@MockitoSettings(strictness = Strictness.LENIENT)
class WorkspaceContextStoreSourceFacadeTest {

    private static final long WORKSPACE_ID = 1L;
    private static final long SOURCE_ID = 2L;
    private static final long CONTEXT_STORE_ID = 3L;
    private static final long PROJECT_ID = 10L;
    private static final int PROJECT_VERSION = 1;
    private static final long PROJECT_DEPLOYMENT_ID = 20L;
    private static final String WORKFLOW_ID = "wf-1";
    private static final long PROJECT_DEPLOYMENT_WORKFLOW_ID = 30L;
    private static final long PRODUCTION_PROJECT_DEPLOYMENT_ID = 21L;
    private static final long CONNECTION_ID = 40L;

    @Mock
    private ObjectProvider<ClickHouseTableProvisioner> clickHouseTableProvisionerProvider;
    @Mock
    private ComponentDefinitionService componentDefinitionService;
    @Mock
    private ContextStoreSourceAuditPublisher contextStoreSourceAuditPublisher;
    @Mock
    private ConnectionService connectionService;
    @Mock
    private ContextStoreService contextStoreService;
    @Mock
    private ContextStoreSourceService contextStoreSourceService;
    @Mock
    private PrincipalJobFacade principalJobFacade;
    @Mock
    private ProjectDeploymentFacade projectDeploymentFacade;
    @Mock
    private ProjectDeploymentService projectDeploymentService;
    @Mock
    private ProjectDeploymentWorkflowService projectDeploymentWorkflowService;
    @Mock
    private ProjectService projectService;
    @Mock
    private ProjectWorkflowService projectWorkflowService;
    @Mock
    private TaskExecutor taskExecutor;
    @Mock
    private WorkflowService workflowService;
    @Mock
    private WorkspaceContextStoreService workspaceContextStoreService;
    @Mock
    private Project project;
    @Mock
    private ProjectDeployment projectDeployment;
    @Mock
    private ProjectDeployment productionProjectDeployment;

    private WorkspaceContextStoreSourceFacadeImpl workspaceContextStoreSourceFacade;

    @BeforeEach
    void setUp() {
        workspaceContextStoreSourceFacade = new WorkspaceContextStoreSourceFacadeImpl(
            clickHouseTableProvisionerProvider, componentDefinitionService, connectionService,
            contextStoreSourceAuditPublisher, contextStoreService, contextStoreSourceService, principalJobFacade,
            projectDeploymentFacade, projectDeploymentService,
            projectDeploymentWorkflowService, projectService, projectWorkflowService, taskExecutor,
            workflowService, workspaceContextStoreService, List.of());

        when(project.getId()).thenReturn(PROJECT_ID);
        when(project.getLastProjectVersion()).thenReturn(PROJECT_VERSION);
        when(project.getName()).thenReturn(
            WorkspaceContextStoreSourceFacadeImpl.CONTEXT_STORE_PROJECT_NAME_PREFIX + WORKSPACE_ID);
        when(projectService.getWorkspaceProjectIds(anyLong(), any())).thenReturn(List.of(PROJECT_ID));
        when(projectService.getProjects(List.of(PROJECT_ID))).thenReturn(List.of(project));
        when(projectService.getProject(PROJECT_ID)).thenReturn(project);

        when(projectDeployment.getId()).thenReturn(PROJECT_DEPLOYMENT_ID);
        when(projectDeploymentService.fetchProjectDeployment(PROJECT_ID, Environment.DEVELOPMENT))
            .thenReturn(Optional.of(projectDeployment));

        when(productionProjectDeployment.getId()).thenReturn(PRODUCTION_PROJECT_DEPLOYMENT_ID);
        when(productionProjectDeployment.isEnabled()).thenReturn(true);
        when(projectDeploymentService.fetchProjectDeployment(PROJECT_ID, Environment.PRODUCTION))
            .thenReturn(Optional.of(productionProjectDeployment));
        when(projectDeploymentService.getProjectDeployment(PRODUCTION_PROJECT_DEPLOYMENT_ID))
            .thenReturn(productionProjectDeployment);

        givenStoreEnvironment(Environment.DEVELOPMENT);
    }

    @Test
    void testSetEnabledEnablesDisabledParentDeploymentAndRegistersTriggerViaFacade() {
        ContextStoreSource source = new ContextStoreSource();

        source.setId(SOURCE_ID);
        source.setContextStoreId(CONTEXT_STORE_ID);
        source.setEnabled(false);
        source.setWorkflowId(WORKFLOW_ID);

        when(contextStoreSourceService.get(SOURCE_ID)).thenReturn(source);
        when(projectDeployment.isEnabled()).thenReturn(false);

        workspaceContextStoreSourceFacade.setEnabled(WORKSPACE_ID, SOURCE_ID, true);

        verify(projectDeploymentService).updateEnabled(PROJECT_DEPLOYMENT_ID, true);

        verify(projectDeploymentFacade).enableProjectDeploymentWorkflow(PROJECT_DEPLOYMENT_ID, WORKFLOW_ID, true);
    }

    @Test
    void testSetEnabledLeavesAlreadyEnabledDeploymentUntouched() {
        ContextStoreSource source = new ContextStoreSource();

        source.setId(SOURCE_ID);
        source.setContextStoreId(CONTEXT_STORE_ID);
        source.setEnabled(false);
        source.setWorkflowId(WORKFLOW_ID);

        when(contextStoreSourceService.get(SOURCE_ID)).thenReturn(source);
        when(projectDeployment.isEnabled()).thenReturn(true);

        workspaceContextStoreSourceFacade.setEnabled(WORKSPACE_ID, SOURCE_ID, true);

        verify(projectDeploymentService, never()).updateEnabled(PROJECT_DEPLOYMENT_ID, true);
        verify(projectDeploymentFacade).enableProjectDeploymentWorkflow(PROJECT_DEPLOYMENT_ID, WORKFLOW_ID, true);
    }

    @Test
    void testSetEnabledTogglesTheWorkflowOnTheDeploymentThatHoldsIt() {
        ContextStoreSource source = new ContextStoreSource();

        source.setId(SOURCE_ID);
        source.setContextStoreId(CONTEXT_STORE_ID);
        source.setEnabled(true);
        source.setWorkflowId(WORKFLOW_ID);

        when(contextStoreSourceService.get(SOURCE_ID)).thenReturn(source);

        ProjectDeploymentWorkflow projectDeploymentWorkflow = mock(ProjectDeploymentWorkflow.class);

        when(projectDeploymentWorkflow.getProjectDeploymentId()).thenReturn(PRODUCTION_PROJECT_DEPLOYMENT_ID);
        when(projectDeploymentWorkflowService.getWorkflowProjectDeploymentWorkflows(WORKFLOW_ID))
            .thenReturn(List.of(projectDeploymentWorkflow));

        workspaceContextStoreSourceFacade.setEnabled(WORKSPACE_ID, SOURCE_ID, false);

        verify(projectDeploymentFacade)
            .enableProjectDeploymentWorkflow(PRODUCTION_PROJECT_DEPLOYMENT_ID, WORKFLOW_ID, false);
    }

    @Test
    void testCreateDeploysTheSourceInItsStoreEnvironment() {
        givenStoreEnvironment(Environment.PRODUCTION);

        givenCreatableSource();

        workspaceContextStoreSourceFacade.create(WORKSPACE_ID, newCreateInput());

        verify(projectDeploymentFacade)
            .enableProjectDeploymentWorkflow(PRODUCTION_PROJECT_DEPLOYMENT_ID, WORKFLOW_ID, true);
        verify(projectDeploymentService, never()).fetchProjectDeployment(PROJECT_ID, Environment.DEVELOPMENT);
    }

    @Test
    void testCreateCreatesTheMissingDeploymentInTheStoreEnvironment() {
        givenStoreEnvironment(Environment.STAGING);

        givenCreatableSource();

        when(projectDeploymentService.fetchProjectDeployment(PROJECT_ID, Environment.STAGING))
            .thenReturn(Optional.empty());
        when(projectDeploymentService.create(any(ProjectDeployment.class)))
            .thenAnswer(invocation -> {
                ProjectDeployment createdProjectDeployment = invocation.getArgument(0);

                createdProjectDeployment.setId(PROJECT_DEPLOYMENT_ID);
                createdProjectDeployment.setEnabled(false);

                return createdProjectDeployment;
            });

        workspaceContextStoreSourceFacade.create(WORKSPACE_ID, newCreateInput());

        ArgumentCaptor<ProjectDeployment> projectDeploymentCaptor = ArgumentCaptor.forClass(ProjectDeployment.class);

        verify(projectDeploymentService).create(projectDeploymentCaptor.capture());

        ProjectDeployment createdProjectDeployment = projectDeploymentCaptor.getValue();

        assertThat(createdProjectDeployment.getEnvironment()).isEqualTo(Environment.STAGING);

        verify(projectDeploymentService).updateEnabled(PROJECT_DEPLOYMENT_ID, true);
    }

    @Test
    void testCreateRejectsAConnectionFromAnotherEnvironment() {
        givenStoreEnvironment(Environment.PRODUCTION);

        givenCreatableSource();

        Connection connection = mock(Connection.class);

        when(connection.getEnvironmentId()).thenReturn(Environment.DEVELOPMENT.ordinal());
        when(connection.getName()).thenReturn("HubSpot dev");
        when(connectionService.getConnection(CONNECTION_ID)).thenReturn(connection);

        assertThatThrownBy(
            () -> workspaceContextStoreSourceFacade.create(WORKSPACE_ID, newCreateInput(CONNECTION_ID)))
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContaining("PRODUCTION");

        verify(contextStoreSourceService, never()).create(any(ContextStoreSource.class));
    }

    @Test
    void testCreateAcceptsAConnectionFromTheStoreEnvironment() {
        givenStoreEnvironment(Environment.PRODUCTION);

        givenCreatableSource();

        Connection connection = mock(Connection.class);

        when(connection.getEnvironmentId()).thenReturn(Environment.PRODUCTION.ordinal());
        when(connectionService.getConnection(CONNECTION_ID)).thenReturn(connection);

        workspaceContextStoreSourceFacade.create(WORKSPACE_ID, newCreateInput(CONNECTION_ID));

        verify(projectDeploymentFacade)
            .enableProjectDeploymentWorkflow(PRODUCTION_PROJECT_DEPLOYMENT_ID, WORKFLOW_ID, true);
    }

    @Test
    void testCreateRegistersScheduleTriggerViaFacade() {
        givenCreatableSource();

        workspaceContextStoreSourceFacade.create(WORKSPACE_ID, newCreateInput());

        verify(projectDeploymentFacade).enableProjectDeploymentWorkflow(PROJECT_DEPLOYMENT_ID, WORKFLOW_ID, true);
    }

    private void givenCreatableSource() {
        when(workspaceContextStoreService.isStoreInWorkspace(WORKSPACE_ID, CONTEXT_STORE_ID)).thenReturn(true);

        ClusterElementDefinition sourceClusterElement = mock(ClusterElementDefinition.class);

        when(sourceClusterElement.getName()).thenReturn("searchContacts");
        when(sourceClusterElement.getType()).thenReturn(ItemReader.SOURCE);
        when(sourceClusterElement.getProperties()).thenReturn(List.of());

        ComponentDefinition componentDefinition = mock(ComponentDefinition.class);

        when(componentDefinition.getClusterElements()).thenReturn(List.of(sourceClusterElement));
        when(componentDefinitionService.getComponentDefinition("hubspot", 1)).thenReturn(componentDefinition);

        ContextStoreSource createdSource = new ContextStoreSource();

        createdSource.setId(SOURCE_ID);
        createdSource.setContextStoreId(CONTEXT_STORE_ID);
        createdSource.setCadence("@hourly");
        createdSource.setSourceComponentName("hubspot");
        createdSource.setSourceComponentVersion(1);
        createdSource.setSourceClusterElementName("searchContacts");

        when(contextStoreSourceService.create(any(ContextStoreSource.class))).thenReturn(createdSource);
        when(contextStoreSourceService.update(any(ContextStoreSource.class))).thenReturn(createdSource);

        Workflow workflow = mock(Workflow.class);

        when(workflow.getId()).thenReturn(WORKFLOW_ID);
        when(workflowService.create(anyString(), any(Workflow.Format.class), any(Workflow.SourceType.class)))
            .thenReturn(workflow);
    }

    @Test
    void testDeleteDisarmsTheWorkflowTriggerBeforeDroppingItsDeploymentRow() {
        ContextStoreSource source = new ContextStoreSource();

        source.setId(SOURCE_ID);
        source.setWorkflowId(WORKFLOW_ID);

        when(contextStoreSourceService.get(SOURCE_ID)).thenReturn(source);

        ProjectDeploymentWorkflow projectDeploymentWorkflow = mock(ProjectDeploymentWorkflow.class);

        when(projectDeploymentWorkflow.getId()).thenReturn(PROJECT_DEPLOYMENT_WORKFLOW_ID);
        when(projectDeploymentWorkflow.getProjectDeploymentId()).thenReturn(PRODUCTION_PROJECT_DEPLOYMENT_ID);
        when(projectDeploymentWorkflowService.getWorkflowProjectDeploymentWorkflows(WORKFLOW_ID))
            .thenReturn(List.of(projectDeploymentWorkflow));

        workspaceContextStoreSourceFacade.delete(WORKSPACE_ID, SOURCE_ID);

        InOrder inOrder = inOrder(projectDeploymentFacade, projectDeploymentWorkflowService, workflowService);

        inOrder.verify(projectDeploymentFacade)
            .enableProjectDeploymentWorkflow(PRODUCTION_PROJECT_DEPLOYMENT_ID, WORKFLOW_ID, false);
        inOrder.verify(projectDeploymentWorkflowService)
            .delete(PROJECT_DEPLOYMENT_WORKFLOW_ID);
        inOrder.verify(workflowService)
            .delete(WORKFLOW_ID);
    }

    @Test
    void testDeleteLeavesTheTriggerAloneWhenTheSourceHasNoDeploymentRow() {
        ContextStoreSource source = new ContextStoreSource();

        source.setId(SOURCE_ID);
        source.setWorkflowId(WORKFLOW_ID);

        when(contextStoreSourceService.get(SOURCE_ID)).thenReturn(source);
        when(projectDeploymentWorkflowService.getWorkflowProjectDeploymentWorkflows(WORKFLOW_ID))
            .thenReturn(List.of());

        workspaceContextStoreSourceFacade.delete(WORKSPACE_ID, SOURCE_ID);

        verify(projectDeploymentFacade, never()).enableProjectDeploymentWorkflow(anyLong(), anyString(),
            anyBoolean());
        verify(workflowService).delete(WORKFLOW_ID);
    }

    private void givenStoreEnvironment(Environment environment) {
        ContextStore contextStore = new ContextStore();

        contextStore.setEnvironment(environment);

        when(contextStoreService.get(CONTEXT_STORE_ID)).thenReturn(contextStore);
    }

    private static CreateContextStoreSourceInput newCreateInput() {
        return newCreateInput(null);
    }

    private static CreateContextStoreSourceInput newCreateInput(Long connectionId) {
        return new CreateContextStoreSourceInput(
            CONTEXT_STORE_ID, "HubSpot contacts", "contact", null, "hubspot", 1, "searchContacts", connectionId,
            "@hourly", null, null, "id", null, Map.of(), null, null);
    }
}
