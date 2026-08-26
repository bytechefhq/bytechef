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

package com.bytechef.automation.knowledgebase.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.execution.dto.JobParametersDTO;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.automation.knowledgebase.dto.CreateKnowledgeBaseSourceInput;
import com.bytechef.automation.knowledgebase.dto.UpdateKnowledgeBaseSourceInput;
import com.bytechef.automation.knowledgebase.service.WorkspaceKnowledgeBaseSourceService;
import com.bytechef.automation.knowledgebase.util.KnowledgeBaseSourceWorkflowGenerator;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.component.definition.ClusterElementDefinition.ClusterElementType;
import com.bytechef.component.definition.datastream.ItemReader;
import com.bytechef.jackson.config.JacksonConfiguration;
import com.bytechef.platform.component.domain.ClusterElementDefinition;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBaseSource;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBaseSourceStatus;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseSourceService;
import com.bytechef.platform.workflow.execution.facade.PrincipalJobFacade;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.test.context.ContextConfiguration;
import tools.jackson.core.type.TypeReference;

/**
 * Unit test for {@link WorkspaceKnowledgeBaseSourceFacadeImpl}. Verifies the orchestration: source row creation,
 * workspace-relation row registration, workflow generation + persistence, project-deployment-workflow wiring,
 * workflow_id linking back to the source, the membership-mismatch guard on every mutating method, the
 * auto-pick-first-ItemReader resolution path, and the cadence-mutation / enabled-toggle / delete / refreshNow /
 * setEnabled paths.
 *
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = JacksonConfiguration.class)
@ExtendWith(ObjectMapperSetupExtension.class)
class WorkspaceKnowledgeBaseSourceFacadeImplTest {

    private static final Long WORKSPACE_ID = 1L;
    private static final Long OTHER_WORKSPACE_ID = 2L;
    private static final Long SOURCE_ID = 100L;
    private static final Long KNOWLEDGE_BASE_ID = 500L;
    private static final long PROJECT_ID = 50L;
    private static final long PROJECT_DEPLOYMENT_ID = 60L;
    private static final long PROJECT_DEPLOYMENT_WORKFLOW_ID = 70L;
    private static final String WORKFLOW_ID = "wf-uuid-abc";
    private static final long JOB_ID = 999L;

    private ComponentDefinitionService componentDefinitionService;
    private KnowledgeBaseSourceService knowledgeBaseSourceService;
    private PrincipalJobFacade principalJobFacade;
    private ProjectDeploymentFacade projectDeploymentFacade;
    private ProjectDeploymentService projectDeploymentService;
    private ProjectDeploymentWorkflowService projectDeploymentWorkflowService;
    private ProjectService projectService;
    private ProjectWorkflowService projectWorkflowService;
    private WorkflowService workflowService;
    private WorkspaceKnowledgeBaseSourceService workspaceKnowledgeBaseSourceService;
    private WorkspaceKnowledgeBaseSourceFacadeImpl facade;

    @BeforeEach
    void setUp() {
        componentDefinitionService = mock(ComponentDefinitionService.class);
        knowledgeBaseSourceService = mock(KnowledgeBaseSourceService.class);
        principalJobFacade = mock(PrincipalJobFacade.class);
        projectDeploymentFacade = mock(ProjectDeploymentFacade.class);
        projectDeploymentService = mock(ProjectDeploymentService.class);
        projectDeploymentWorkflowService = mock(ProjectDeploymentWorkflowService.class);
        projectService = mock(ProjectService.class);
        projectWorkflowService = mock(ProjectWorkflowService.class);
        workflowService = mock(WorkflowService.class);
        workspaceKnowledgeBaseSourceService = mock(WorkspaceKnowledgeBaseSourceService.class);

        facade = new WorkspaceKnowledgeBaseSourceFacadeImpl(
            componentDefinitionService, knowledgeBaseSourceService, principalJobFacade, projectDeploymentFacade,
            projectDeploymentService, projectDeploymentWorkflowService, projectService, projectWorkflowService,
            new SyncTaskExecutor(), workflowService, workspaceKnowledgeBaseSourceService, List.of());

        // Default: hubspot v1 has a single ItemReader cluster element named "searchContacts".
        ComponentDefinition hubspotDefinition = componentDefinitionWithItemReader("searchContacts");

        when(componentDefinitionService.getComponentDefinition("hubspot", 1)).thenReturn(hubspotDefinition);

        // sourceService.create returns the same source with id assigned.
        when(knowledgeBaseSourceService.create(any(KnowledgeBaseSource.class)))
            .thenAnswer(invocation -> {
                KnowledgeBaseSource argument = invocation.getArgument(0);

                argument.setId(SOURCE_ID);

                return argument;
            });

        when(knowledgeBaseSourceService.update(any(KnowledgeBaseSource.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // Project lookup: not yet present, then created.
        when(projectService.fetchProject(anyString())).thenReturn(Optional.empty());

        Project createdProject = new Project();

        createdProject.setId(PROJECT_ID);
        createdProject.setName("__KNOWLEDGE_BASE__1");
        createdProject.setWorkspaceId(WORKSPACE_ID);

        when(projectService.create(any(Project.class))).thenReturn(createdProject);
        when(projectService.getProject(PROJECT_ID)).thenReturn(createdProject);

        when(projectDeploymentService.fetchProjectDeployment(eq(PROJECT_ID), any(Environment.class)))
            .thenReturn(Optional.empty());

        ProjectDeployment createdDeployment = new ProjectDeployment();

        createdDeployment.setId(PROJECT_DEPLOYMENT_ID);
        createdDeployment.setProjectId(PROJECT_ID);

        when(projectDeploymentService.create(any(ProjectDeployment.class))).thenReturn(createdDeployment);

        Workflow workflow = new Workflow(WORKFLOW_ID, "{}", Workflow.Format.JSON);

        when(workflowService.create(anyString(), any(), any())).thenReturn(workflow);

        ProjectDeploymentWorkflow createdProjectDeploymentWorkflow = new ProjectDeploymentWorkflow();

        createdProjectDeploymentWorkflow.setId(PROJECT_DEPLOYMENT_WORKFLOW_ID);
        createdProjectDeploymentWorkflow.setWorkflowId(WORKFLOW_ID);
        createdProjectDeploymentWorkflow.setProjectDeploymentId(PROJECT_DEPLOYMENT_ID);

        when(projectDeploymentWorkflowService.create(any(ProjectDeploymentWorkflow.class)))
            .thenReturn(createdProjectDeploymentWorkflow);

        when(principalJobFacade.createJob(any(JobParametersDTO.class), anyLong(), any(PlatformType.class)))
            .thenReturn(JOB_ID);
    }

    @Test
    void testCreateInsertsSourceStampedWithWorkspaceAndAutoGeneratesWorkflow() {
        CreateKnowledgeBaseSourceInput input = newCreateInput("HubSpot", "@hourly", "searchContacts");

        KnowledgeBaseSource result = facade.create(WORKSPACE_ID, input);

        assertThat(result.getId()).isEqualTo(SOURCE_ID);
        assertThat(result.getWorkflowId()).isEqualTo(WORKFLOW_ID);
        assertThat(result.getStatus()).isEqualTo(KnowledgeBaseSourceStatus.BUILDING_PREVIEW);

        // Source row inserted with status=BUILDING_PREVIEW + initial nulls, then updated with workflow id.
        ArgumentCaptor<KnowledgeBaseSource> createCaptor = ArgumentCaptor.forClass(KnowledgeBaseSource.class);

        verify(knowledgeBaseSourceService).create(createCaptor.capture());
        verify(knowledgeBaseSourceService).update(any(KnowledgeBaseSource.class));

        // Workspace ownership is stamped onto the inserted source row itself.
        assertThat(createCaptor.getValue()
            .getWorkspaceId()).isEqualTo(WORKSPACE_ID);

        // Workflow definition was generated and persisted.
        ArgumentCaptor<String> definitionCaptor = ArgumentCaptor.forClass(String.class);
        verify(workflowService).create(
            definitionCaptor.capture(), eq(Workflow.Format.JSON), eq(Workflow.SourceType.JDBC));

        Map<String, Object> definitionMap =
            JsonUtils.read(definitionCaptor.getValue(), new TypeReference<>() {});

        @SuppressWarnings("unchecked")
        Map<String, Object> metadata = (Map<String, Object>) definitionMap.get("metadata");

        assertThat(metadata.get("knowledgeBaseSourceId")).isEqualTo(SOURCE_ID.intValue());

        // Project + deployment auto-created.
        verify(projectService).create(any(Project.class));
        verify(projectDeploymentService).create(any(ProjectDeployment.class));

        // ProjectWorkflow + ProjectDeploymentWorkflow wired.
        verify(projectWorkflowService).addWorkflow(eq(PROJECT_ID), anyInt(), eq(WORKFLOW_ID));
        verify(projectDeploymentWorkflowService).create(any(ProjectDeploymentWorkflow.class));

        // Creating the row enabled does not register the cron trigger — only the facade does. Without this the
        // initial sync below would be the only run the source ever had.
        verify(projectDeploymentFacade).enableProjectDeploymentWorkflow(PROJECT_DEPLOYMENT_ID, WORKFLOW_ID, true);

        // Initial sync triggered.
        verify(principalJobFacade).createJob(any(JobParametersDTO.class), eq(PROJECT_DEPLOYMENT_ID),
            eq(PlatformType.AUTOMATION));
    }

    @Test
    void testCreateAutoPicksFirstItemReaderWhenClusterElementNameOmitted() {
        CreateKnowledgeBaseSourceInput input = newCreateInput("HubSpot", "@hourly", null);

        KnowledgeBaseSource result = facade.create(WORKSPACE_ID, input);

        assertThat(result.getSourceClusterElementName()).isEqualTo("searchContacts");
    }

    @Test
    void testCreateThreadsParametersThroughToPersistedSourceAndWorkflow() {
        Map<String, Object> sourceParameters = new java.util.LinkedHashMap<>();

        sourceParameters.put("objectType", "contacts");
        sourceParameters.put("limit", 100);

        CreateKnowledgeBaseSourceInput input = new CreateKnowledgeBaseSourceInput(
            "HubSpot", "hubspot", 1, "searchContacts", null, KNOWLEDGE_BASE_ID, "@hourly", sourceParameters, null,
            null, null);

        ArgumentCaptor<KnowledgeBaseSource> insertCaptor = ArgumentCaptor.forClass(KnowledgeBaseSource.class);

        facade.create(WORKSPACE_ID, input);

        verify(knowledgeBaseSourceService).create(insertCaptor.capture());

        KnowledgeBaseSource captured = insertCaptor.getValue();

        @SuppressWarnings("unchecked")
        Map<String, Object> persistedParameters = (Map<String, Object>) captured.getParameters();

        assertThat(persistedParameters)
            .isNotNull()
            .containsEntry("objectType", "contacts")
            .containsEntry("limit", 100);

        ArgumentCaptor<String> definitionCaptor = ArgumentCaptor.forClass(String.class);
        verify(workflowService).create(
            definitionCaptor.capture(), eq(Workflow.Format.JSON), eq(Workflow.SourceType.JDBC));

        Map<String, Object> definitionMap =
            JsonUtils.read(definitionCaptor.getValue(), new TypeReference<>() {});

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> tasks = (List<Map<String, Object>>) definitionMap.get("tasks");

        @SuppressWarnings("unchecked")
        Map<String, Object> clusterElements = (Map<String, Object>) tasks.get(0)
            .get("clusterElements");

        @SuppressWarnings("unchecked")
        Map<String, Object> sourceElement = (Map<String, Object>) clusterElements.get("source");

        @SuppressWarnings("unchecked")
        Map<String, Object> embeddedParameters = (Map<String, Object>) sourceElement.get("parameters");

        assertThat(embeddedParameters)
            .containsEntry("objectType", "contacts")
            .containsEntry("limit", 100);
    }

    @Test
    void testCreateThrowsWhenComponentHasNoItemReader() {
        ComponentDefinition emptyDefinition = componentDefinitionWithoutItemReader();

        when(componentDefinitionService.getComponentDefinition("hubspot", 1)).thenReturn(emptyDefinition);

        CreateKnowledgeBaseSourceInput input = newCreateInput("HubSpot", "@hourly", null);

        assertThatThrownBy(() -> facade.create(WORKSPACE_ID, input))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("hubspot")
            .hasMessageContaining("ItemReader");

        verify(knowledgeBaseSourceService, never()).create(any(KnowledgeBaseSource.class));
    }

    @Test
    void testCreateRollsBackOnWorkflowFailure() {
        when(workflowService.create(anyString(), any(), any()))
            .thenThrow(new RuntimeException("workflow service unavailable"));

        CreateKnowledgeBaseSourceInput input = newCreateInput("HubSpot", "@hourly", "searchContacts");

        assertThatThrownBy(() -> facade.create(WORKSPACE_ID, input))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("workflow service unavailable");

        // No deployment / workflow wiring should have happened past the failure.
        verify(projectDeploymentService, never()).create(any(ProjectDeployment.class));
        verify(projectDeploymentWorkflowService, never()).create(any(ProjectDeploymentWorkflow.class));
        verify(principalJobFacade, never())
            .createJob(any(JobParametersDTO.class), anyLong(), any(PlatformType.class));
    }

    @Test
    void testUpdateRejectsMembershipMismatch() {
        when(workspaceKnowledgeBaseSourceService.fetchWorkspaceIdByKnowledgeBaseSourceId(SOURCE_ID))
            .thenReturn(Optional.of(OTHER_WORKSPACE_ID));

        UpdateKnowledgeBaseSourceInput input =
            new UpdateKnowledgeBaseSourceInput("renamed", null, null, null, null, null);

        assertThatThrownBy(() -> facade.update(WORKSPACE_ID, SOURCE_ID, input))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining(String.valueOf(SOURCE_ID));

        verify(knowledgeBaseSourceService, never()).update(any(KnowledgeBaseSource.class));
    }

    @Test
    void testUpdateChangesCadenceMutatesWorkflowTrigger() {
        when(workspaceKnowledgeBaseSourceService.fetchWorkspaceIdByKnowledgeBaseSourceId(SOURCE_ID))
            .thenReturn(Optional.of(WORKSPACE_ID));

        KnowledgeBaseSource existingSource = newSourceWithWorkflow("@hourly");

        when(knowledgeBaseSourceService.get(SOURCE_ID)).thenReturn(existingSource);

        // The existing workflow has the original definition; capture it from a fresh generation.
        String originalDefinition = KnowledgeBaseSourceWorkflowGenerator.generate(existingSource);

        Workflow existingWorkflow = new Workflow(WORKFLOW_ID, originalDefinition, Workflow.Format.JSON);

        when(workflowService.getWorkflow(WORKFLOW_ID)).thenReturn(existingWorkflow);

        UpdateKnowledgeBaseSourceInput updateInput =
            new UpdateKnowledgeBaseSourceInput(null, "@daily", null, null, null, null);

        facade.update(WORKSPACE_ID, SOURCE_ID, updateInput);

        ArgumentCaptor<String> updatedDefinitionCaptor = ArgumentCaptor.forClass(String.class);
        verify(workflowService).update(eq(WORKFLOW_ID), updatedDefinitionCaptor.capture(), anyInt());

        Map<String, Object> mutatedDefinition =
            JsonUtils.read(updatedDefinitionCaptor.getValue(), new TypeReference<>() {});

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> triggers = (List<Map<String, Object>>) mutatedDefinition.get("triggers");

        @SuppressWarnings("unchecked")
        Map<String, Object> triggerParameters = (Map<String, Object>) triggers.get(0)
            .get("parameters");

        assertThat(triggerParameters.get("expression")).isEqualTo("0 0 * * *");
        assertThat(existingSource.getCadence()).isEqualTo("@daily");
    }

    @Test
    void testUpdateTogglesProjectDeploymentWorkflowOnEnabledChange() {
        when(workspaceKnowledgeBaseSourceService.fetchWorkspaceIdByKnowledgeBaseSourceId(SOURCE_ID))
            .thenReturn(Optional.of(WORKSPACE_ID));

        KnowledgeBaseSource existingSource = newSourceWithWorkflow("@hourly");

        when(knowledgeBaseSourceService.get(SOURCE_ID)).thenReturn(existingSource);

        ProjectDeployment existingDeployment = new ProjectDeployment();

        existingDeployment.setId(PROJECT_DEPLOYMENT_ID);

        when(projectDeploymentService.fetchProjectDeployment(eq(PROJECT_ID), any(Environment.class)))
            .thenReturn(Optional.of(existingDeployment));

        Project existingProject = new Project();

        existingProject.setId(PROJECT_ID);
        existingProject.setName("__KNOWLEDGE_BASE__1");

        when(projectService.fetchProject(anyString())).thenReturn(Optional.of(existingProject));

        ProjectDeploymentWorkflow existingProjectDeploymentWorkflow = new ProjectDeploymentWorkflow();

        existingProjectDeploymentWorkflow.setId(PROJECT_DEPLOYMENT_WORKFLOW_ID);

        when(projectDeploymentWorkflowService.fetchProjectDeploymentWorkflow(PROJECT_DEPLOYMENT_ID, WORKFLOW_ID))
            .thenReturn(Optional.of(existingProjectDeploymentWorkflow));

        UpdateKnowledgeBaseSourceInput updateInput =
            new UpdateKnowledgeBaseSourceInput(null, null, false, null, null, null);

        facade.update(WORKSPACE_ID, SOURCE_ID, updateInput);

        // Must go through ProjectDeploymentFacade: flipping the enabled column alone never unregisters the
        // scheduler cron trigger.
        verify(projectDeploymentFacade).enableProjectDeploymentWorkflow(PROJECT_DEPLOYMENT_ID, WORKFLOW_ID, false);
        assertThat(existingSource.isEnabled()).isFalse();
    }

    @Test
    void testDeleteRejectsMembershipMismatch() {
        when(workspaceKnowledgeBaseSourceService.fetchWorkspaceIdByKnowledgeBaseSourceId(SOURCE_ID))
            .thenReturn(Optional.of(OTHER_WORKSPACE_ID));

        assertThatThrownBy(() -> facade.delete(WORKSPACE_ID, SOURCE_ID))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining(String.valueOf(SOURCE_ID));

        verify(knowledgeBaseSourceService, never()).delete(anyLong());
    }

    @Test
    void testDeleteRemovesProjectDeploymentWorkflowAndWorkflowAndSource() {
        when(workspaceKnowledgeBaseSourceService.fetchWorkspaceIdByKnowledgeBaseSourceId(SOURCE_ID))
            .thenReturn(Optional.of(WORKSPACE_ID));

        KnowledgeBaseSource existingSource = newSourceWithWorkflow("@hourly");

        when(knowledgeBaseSourceService.get(SOURCE_ID)).thenReturn(existingSource);

        Project existingProject = new Project();

        existingProject.setId(PROJECT_ID);
        existingProject.setName("__KNOWLEDGE_BASE__1");

        when(projectService.fetchProject(anyString())).thenReturn(Optional.of(existingProject));

        ProjectDeployment existingDeployment = new ProjectDeployment();

        existingDeployment.setId(PROJECT_DEPLOYMENT_ID);

        when(projectDeploymentService.fetchProjectDeployment(eq(PROJECT_ID), any(Environment.class)))
            .thenReturn(Optional.of(existingDeployment));

        ProjectDeploymentWorkflow existingProjectDeploymentWorkflow = new ProjectDeploymentWorkflow();

        existingProjectDeploymentWorkflow.setId(PROJECT_DEPLOYMENT_WORKFLOW_ID);

        when(projectDeploymentWorkflowService.fetchProjectDeploymentWorkflow(PROJECT_DEPLOYMENT_ID, WORKFLOW_ID))
            .thenReturn(Optional.of(existingProjectDeploymentWorkflow));

        facade.delete(WORKSPACE_ID, SOURCE_ID);

        verify(projectDeploymentWorkflowService).delete(PROJECT_DEPLOYMENT_WORKFLOW_ID);
        verify(projectWorkflowService).delete(eq(PROJECT_ID), anyInt(), eq(WORKFLOW_ID));
        verify(workflowService).delete(WORKFLOW_ID);
        verify(knowledgeBaseSourceService).delete(SOURCE_ID);
    }

    @Test
    void testRefreshNowRejectsMembershipMismatch() {
        when(workspaceKnowledgeBaseSourceService.fetchWorkspaceIdByKnowledgeBaseSourceId(SOURCE_ID))
            .thenReturn(Optional.of(OTHER_WORKSPACE_ID));

        assertThatThrownBy(() -> facade.refreshNow(WORKSPACE_ID, SOURCE_ID))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining(String.valueOf(SOURCE_ID));

        verify(principalJobFacade, never())
            .createJob(any(JobParametersDTO.class), anyLong(), any(PlatformType.class));
    }

    @Test
    void testRefreshNowCreatesJobViaPrincipalJobFacade() {
        when(workspaceKnowledgeBaseSourceService.fetchWorkspaceIdByKnowledgeBaseSourceId(SOURCE_ID))
            .thenReturn(Optional.of(WORKSPACE_ID));

        KnowledgeBaseSource existingSource = newSourceWithWorkflow("@hourly");

        when(knowledgeBaseSourceService.get(SOURCE_ID)).thenReturn(existingSource);

        Project existingProject = new Project();

        existingProject.setId(PROJECT_ID);
        existingProject.setName("__KNOWLEDGE_BASE__1");

        when(projectService.fetchProject(anyString())).thenReturn(Optional.of(existingProject));

        ProjectDeployment existingDeployment = new ProjectDeployment();

        existingDeployment.setId(PROJECT_DEPLOYMENT_ID);

        when(projectDeploymentService.fetchProjectDeployment(eq(PROJECT_ID), any(Environment.class)))
            .thenReturn(Optional.of(existingDeployment));

        long jobId = facade.refreshNow(WORKSPACE_ID, SOURCE_ID);

        assertThat(jobId).isEqualTo(JOB_ID);

        ArgumentCaptor<JobParametersDTO> jobParameterCaptor = ArgumentCaptor.forClass(JobParametersDTO.class);
        verify(principalJobFacade).createJob(
            jobParameterCaptor.capture(), eq(PROJECT_DEPLOYMENT_ID), eq(PlatformType.AUTOMATION));

        assertThat(jobParameterCaptor.getValue()
            .getWorkflowId())
                .isEqualTo(WORKFLOW_ID);
    }

    @Test
    void testSetEnabledRejectsMembershipMismatch() {
        when(workspaceKnowledgeBaseSourceService.fetchWorkspaceIdByKnowledgeBaseSourceId(SOURCE_ID))
            .thenReturn(Optional.of(OTHER_WORKSPACE_ID));

        assertThatThrownBy(() -> facade.setEnabled(WORKSPACE_ID, SOURCE_ID, false))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining(String.valueOf(SOURCE_ID));

        verify(knowledgeBaseSourceService, never()).update(any(KnowledgeBaseSource.class));
        verify(projectDeploymentWorkflowService, never()).updateEnabled(anyLong(), anyBoolean());
    }

    @Test
    void testSetEnabledTogglesSourceAndProjectDeploymentWorkflow() {
        when(workspaceKnowledgeBaseSourceService.fetchWorkspaceIdByKnowledgeBaseSourceId(SOURCE_ID))
            .thenReturn(Optional.of(WORKSPACE_ID));

        KnowledgeBaseSource existingSource = newSourceWithWorkflow("@hourly");

        when(knowledgeBaseSourceService.get(SOURCE_ID)).thenReturn(existingSource);

        ProjectDeployment existingDeployment = new ProjectDeployment();

        existingDeployment.setId(PROJECT_DEPLOYMENT_ID);

        when(projectDeploymentService.fetchProjectDeployment(eq(PROJECT_ID), any(Environment.class)))
            .thenReturn(Optional.of(existingDeployment));

        Project existingProject = new Project();

        existingProject.setId(PROJECT_ID);
        existingProject.setName("__KNOWLEDGE_BASE__1");

        when(projectService.fetchProject(anyString())).thenReturn(Optional.of(existingProject));

        ProjectDeploymentWorkflow existingProjectDeploymentWorkflow = new ProjectDeploymentWorkflow();

        existingProjectDeploymentWorkflow.setId(PROJECT_DEPLOYMENT_WORKFLOW_ID);

        when(projectDeploymentWorkflowService.fetchProjectDeploymentWorkflow(PROJECT_DEPLOYMENT_ID, WORKFLOW_ID))
            .thenReturn(Optional.of(existingProjectDeploymentWorkflow));

        facade.setEnabled(WORKSPACE_ID, SOURCE_ID, false);

        // Must go through ProjectDeploymentFacade: flipping the enabled column alone never unregisters the
        // scheduler cron trigger.
        verify(projectDeploymentFacade).enableProjectDeploymentWorkflow(PROJECT_DEPLOYMENT_ID, WORKFLOW_ID, false);
        verify(knowledgeBaseSourceService, times(1)).update(any(KnowledgeBaseSource.class));

        assertThat(existingSource.isEnabled()).isFalse();
    }

    @Test
    void testSetEnabledReconcilesDisabledParentDeployment() {
        stubSetEnabledCollaborators(false);

        facade.setEnabled(WORKSPACE_ID, SOURCE_ID, true);

        // The system-managed parent deployment must be enabled so the per-workflow trigger-registration guard in
        // ProjectDeploymentFacadeImpl ("if (projectDeployment.isEnabled())") does not silently swallow the cron.
        verify(projectDeploymentService).updateEnabled(PROJECT_DEPLOYMENT_ID, true);
        verify(projectDeploymentFacade).enableProjectDeploymentWorkflow(PROJECT_DEPLOYMENT_ID, WORKFLOW_ID, true);
    }

    @Test
    void testSetEnabledLeavesAlreadyEnabledParentDeploymentUntouched() {
        stubSetEnabledCollaborators(true);

        facade.setEnabled(WORKSPACE_ID, SOURCE_ID, true);

        verify(projectDeploymentService, never()).updateEnabled(anyLong(), anyBoolean());
        verify(projectDeploymentFacade).enableProjectDeploymentWorkflow(PROJECT_DEPLOYMENT_ID, WORKFLOW_ID, true);
    }

    /**
     * Shared setup for the two parent-deployment reconciliation tests: a disabled source whose workflow already exists,
     * hanging off a parent deployment in the given enabled state.
     */
    private void stubSetEnabledCollaborators(boolean parentDeploymentEnabled) {
        when(workspaceKnowledgeBaseSourceService.fetchWorkspaceIdByKnowledgeBaseSourceId(SOURCE_ID))
            .thenReturn(Optional.of(WORKSPACE_ID));

        KnowledgeBaseSource existingSource = newSourceWithWorkflow("@hourly");

        existingSource.setEnabled(false);

        when(knowledgeBaseSourceService.get(SOURCE_ID)).thenReturn(existingSource);

        ProjectDeployment existingDeployment = new ProjectDeployment();

        existingDeployment.setId(PROJECT_DEPLOYMENT_ID);
        existingDeployment.setEnabled(parentDeploymentEnabled);

        when(projectDeploymentService.fetchProjectDeployment(eq(PROJECT_ID), any(Environment.class)))
            .thenReturn(Optional.of(existingDeployment));

        Project existingProject = new Project();

        existingProject.setId(PROJECT_ID);
        existingProject.setName("__KNOWLEDGE_BASE__1");

        when(projectService.fetchProject(anyString())).thenReturn(Optional.of(existingProject));
    }

    private static CreateKnowledgeBaseSourceInput
        newCreateInput(String name, String cadence, String clusterElementName) {
        return new CreateKnowledgeBaseSourceInput(
            name, "hubspot", 1, clusterElementName, null, KNOWLEDGE_BASE_ID, cadence, null, null, null, null);
    }

    private static KnowledgeBaseSource newSourceWithWorkflow(String cadence) {
        KnowledgeBaseSource source = new KnowledgeBaseSource();

        source.setId(SOURCE_ID);
        source.setName("HubSpot");
        source.setSourceComponentName("hubspot");
        source.setSourceComponentVersion(1);
        source.setSourceClusterElementName("searchContacts");
        source.setKnowledgeBaseId(KNOWLEDGE_BASE_ID);
        source.setCadence(cadence);
        source.setStatus(KnowledgeBaseSourceStatus.READY);
        source.setEnabled(true);
        source.setWorkflowId(WORKFLOW_ID);

        return source;
    }

    private static ComponentDefinition componentDefinitionWithItemReader(String clusterElementName) {
        ComponentDefinition componentDefinition = mock(ComponentDefinition.class);
        ClusterElementDefinition itemReaderElement = mock(ClusterElementDefinition.class);

        when(itemReaderElement.getType()).thenReturn(ItemReader.SOURCE);
        when(itemReaderElement.getName()).thenReturn(clusterElementName);
        when(componentDefinition.getClusterElements()).thenReturn(List.of(itemReaderElement));

        return componentDefinition;
    }

    private static ComponentDefinition componentDefinitionWithoutItemReader() {
        ComponentDefinition componentDefinition = mock(ComponentDefinition.class);
        ClusterElementDefinition unrelatedElement = mock(ClusterElementDefinition.class);
        ClusterElementType unrelatedType = new ClusterElementType("OTHER", "other", "Other");

        when(unrelatedElement.getType()).thenReturn(unrelatedType);
        when(componentDefinition.getClusterElements()).thenReturn(List.of(unrelatedElement));

        return componentDefinition;
    }
}
