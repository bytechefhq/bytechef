/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.contextstore.facade;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
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
import com.bytechef.ee.platform.contextstore.domain.ContextStoreSource;
import com.bytechef.ee.platform.contextstore.service.ContextStoreSourceService;
import com.bytechef.platform.component.domain.ClusterElementDefinition;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.workflow.execution.facade.PrincipalJobFacade;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

    @Mock
    private ObjectProvider<ClickHouseTableProvisioner> clickHouseTableProvisionerProvider;
    @Mock
    private ComponentDefinitionService componentDefinitionService;
    @Mock
    private ContextStoreSourceAuditPublisher contextStoreSourceAuditPublisher;
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

    private WorkspaceContextStoreSourceFacadeImpl workspaceContextStoreSourceFacade;

    @BeforeEach
    void setUp() {
        workspaceContextStoreSourceFacade = new WorkspaceContextStoreSourceFacadeImpl(
            clickHouseTableProvisionerProvider, componentDefinitionService, contextStoreSourceAuditPublisher,
            contextStoreSourceService, principalJobFacade, projectDeploymentFacade, projectDeploymentService,
            projectDeploymentWorkflowService, projectService, projectWorkflowService, taskExecutor,
            workflowService, workspaceContextStoreService, List.of());

        when(project.getId()).thenReturn(PROJECT_ID);
        when(project.getLastProjectVersion()).thenReturn(PROJECT_VERSION);
        when(projectService.fetchProject(anyString())).thenReturn(Optional.of(project));
        when(projectService.getProject(PROJECT_ID)).thenReturn(project);

        when(projectDeployment.getId()).thenReturn(PROJECT_DEPLOYMENT_ID);
        when(projectDeploymentService.fetchProjectDeployment(PROJECT_ID, Environment.DEVELOPMENT))
            .thenReturn(Optional.of(projectDeployment));
    }

    @Test
    void testSetEnabledEnablesDisabledParentDeploymentAndRegistersTriggerViaFacade() {
        ContextStoreSource source = new ContextStoreSource();

        source.setId(SOURCE_ID);
        source.setEnabled(false);
        source.setWorkflowId(WORKFLOW_ID);

        when(contextStoreSourceService.get(SOURCE_ID)).thenReturn(source);
        when(projectDeployment.isEnabled()).thenReturn(false);

        workspaceContextStoreSourceFacade.setEnabled(WORKSPACE_ID, SOURCE_ID, true);

        // Defect B: the system-managed parent deployment must be enabled so the per-workflow trigger-registration
        // guard in ProjectDeploymentFacadeImpl ("if (projectDeployment.isEnabled())") does not silently swallow it.
        verify(projectDeploymentService).updateEnabled(PROJECT_DEPLOYMENT_ID, true);

        // Defect A: enabling must go through the facade that actually (un)registers the scheduler cron trigger, not
        // the raw ProjectDeploymentWorkflowService that only flips a DB column.
        verify(projectDeploymentFacade).enableProjectDeploymentWorkflow(PROJECT_DEPLOYMENT_ID, WORKFLOW_ID, true);
    }

    @Test
    void testSetEnabledLeavesAlreadyEnabledDeploymentUntouched() {
        ContextStoreSource source = new ContextStoreSource();

        source.setId(SOURCE_ID);
        source.setEnabled(false);
        source.setWorkflowId(WORKFLOW_ID);

        when(contextStoreSourceService.get(SOURCE_ID)).thenReturn(source);
        when(projectDeployment.isEnabled()).thenReturn(true);

        workspaceContextStoreSourceFacade.setEnabled(WORKSPACE_ID, SOURCE_ID, true);

        verify(projectDeploymentService, never()).updateEnabled(PROJECT_DEPLOYMENT_ID, true);
        verify(projectDeploymentFacade).enableProjectDeploymentWorkflow(PROJECT_DEPLOYMENT_ID, WORKFLOW_ID, true);
    }

    @Test
    void testCreateRegistersScheduleTriggerViaFacade() {
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

        workspaceContextStoreSourceFacade.create(WORKSPACE_ID, newCreateInput());

        // Creating the ProjectDeploymentWorkflow row enabled does not register the cron trigger with the scheduler —
        // ProjectDeploymentWorkflowService.create is a plain repository save. Only the facade arms it, so without
        // this call the initial sync would be the only run the source ever had.
        verify(projectDeploymentFacade).enableProjectDeploymentWorkflow(PROJECT_DEPLOYMENT_ID, WORKFLOW_ID, true);
    }

    private static CreateContextStoreSourceInput newCreateInput() {
        return new CreateContextStoreSourceInput(
            CONTEXT_STORE_ID, "HubSpot contacts", "contact", null, "hubspot", 1, "searchContacts", null, "@hourly",
            null, null, "id", null, Map.of(), null, null);
    }
}
