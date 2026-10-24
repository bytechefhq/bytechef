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

package com.bytechef.automation.datasync.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.repository.WorkflowCrudRepository;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.domain.ProjectWorkflowType;
import com.bytechef.automation.configuration.domain.SystemProjects;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.automation.configuration.repository.ProjectDeploymentRepository;
import com.bytechef.automation.configuration.repository.ProjectDeploymentWorkflowRepository;
import com.bytechef.automation.configuration.repository.ProjectRepository;
import com.bytechef.automation.configuration.repository.ProjectWorkflowRepository;
import com.bytechef.automation.configuration.repository.WorkspaceRepository;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.automation.datasync.config.AutomationDataSyncIntTestConfiguration;
import com.bytechef.automation.datasync.domain.DataSync;
import com.bytechef.automation.datasync.domain.DataSync.TriggerType;
import com.bytechef.automation.datasync.domain.DataSyncElement;
import com.bytechef.automation.datasync.domain.DataSyncElement.Kind;
import com.bytechef.automation.datasync.dto.DataSyncDTO;
import com.bytechef.automation.datasync.dto.DataSyncDeploymentDTO;
import com.bytechef.automation.datasync.dto.DataSyncVersionDTO;
import com.bytechef.automation.datasync.exception.DataSyncErrorType;
import com.bytechef.automation.datasync.repository.DataSyncElementRepository;
import com.bytechef.automation.datasync.repository.DataSyncRepository;
import com.bytechef.automation.datasync.util.DataSyncWorkflowGenerator;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.domain.WorkflowTestConfiguration;
import com.bytechef.platform.configuration.domain.WorkflowTestConfigurationConnection;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.core.type.TypeReference;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = AutomationDataSyncIntTestConfiguration.class,
    properties = "bytechef.workflow.repository.jdbc.enabled=true")
@Import(PostgreSQLContainerConfiguration.class)
class DataSyncFacadeIntTest {

    @Autowired
    private DataSyncFacade dataSyncFacade;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private ProjectWorkflowService projectWorkflowService;

    @Autowired
    private WorkflowService workflowService;

    @Autowired
    private WorkflowTestConfigurationService workflowTestConfigurationService;

    @Autowired
    private DataSyncRepository dataSyncRepository;

    @Autowired
    private DataSyncElementRepository dataSyncElementRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectWorkflowRepository projectWorkflowRepository;

    @Autowired
    private ProjectDeploymentRepository projectDeploymentRepository;

    @Autowired
    private ProjectDeploymentWorkflowRepository projectDeploymentWorkflowRepository;

    @Autowired
    private WorkflowCrudRepository workflowCrudRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private ComponentDefinitionService componentDefinitionService;

    @Autowired
    private ProjectDeploymentFacade projectDeploymentFacade;

    private Long workspaceId;

    @BeforeEach
    void beforeEach() {
        Workspace workspace = workspaceRepository.save(new Workspace("ws"));

        workspaceId = workspace.getId();
    }

    @AfterEach
    void afterEach() {
        // componentDefinitionService and projectDeploymentFacade are context-scoped Mockito mocks (see
        // AutomationDataSyncIntTestConfiguration), so per-test stubbing would otherwise leak into every later test
        // sharing the same Spring context.
        Mockito.reset(componentDefinitionService, projectDeploymentFacade);

        projectDeploymentWorkflowRepository.deleteAll();
        projectDeploymentRepository.deleteAll();
        dataSyncElementRepository.deleteAll();
        dataSyncRepository.deleteAll();
        projectWorkflowRepository.deleteAll();

        for (Workflow workflow : workflowCrudRepository.findAll()) {
            workflowCrudRepository.deleteById(workflow.getId());
        }

        projectRepository.deleteAll();
        workspaceRepository.deleteAll();
    }

    @Test
    void testCreateWithoutProjectCreatesOrdinaryProjectNamedAfterTitle() {
        DataSyncDTO dataSyncDTO = dataSyncFacade.createDataSync("CRM to DB", "nightly", workspaceId, null);

        Project project = projectService.getProject(dataSyncDTO.projectId());

        assertThat(project.getName()).isEqualTo("CRM to DB");
        assertThat(project.getDescription()).isEqualTo("nightly");
        assertThat(project.getWorkspaceId()).isEqualTo(workspaceId);
        assertThat(SystemProjects.isSystemProject(project)).isFalse();
        assertThat(dataSyncDTO.dataSync()
            .getName()).isEqualTo("crm-to-db");
        assertThat(dataSyncDTO.dataSync()
            .getTriggerType()).isEqualTo(TriggerType.MANUAL);

        ProjectWorkflow projectWorkflow = projectWorkflowService
            .fetchProjectWorkflow(
                project.getId(), project.getLastProjectVersion(), String.valueOf(dataSyncDTO.dataSync()
                    .getProjectWorkflowUuid()))
            .orElseThrow();

        assertThat(projectWorkflow.getType()).isEqualTo(ProjectWorkflowType.DATA_SYNC);
        assertThat(projectWorkflow.getWorkflowId()).isEqualTo(dataSyncDTO.draftWorkflowId());

        Workflow draft = workflowService.getWorkflow(dataSyncDTO.draftWorkflowId());

        Map<String, Object> definition = JsonUtils.read(draft.getDefinition(), new TypeReference<>() {});

        assertThat(definition).containsKey("triggers");
        assertThat(projectService.getProjects(null, null, null, null, null, workspaceId))
            .anyMatch(candidate -> candidate.getId()
                .equals(project.getId()));
    }

    @Test
    void testCreateWithoutProjectSuffixesCollidingProjectName() {
        dataSyncFacade.createDataSync("Orders", null, workspaceId, null);

        DataSyncDTO second = dataSyncFacade.createDataSync("Orders", null, workspaceId, null);

        assertThat(projectService.getProject(second.projectId())
            .getName()).isEqualTo("Orders (2)");
    }

    @Test
    void testCreateInExistingProjectAddsDataSyncWorkflow() {
        Project project = saveOrdinaryProject("Existing");
        String ordinaryWorkflowId = addOrdinaryWorkflow(project);

        DataSyncDTO dataSyncDTO = dataSyncFacade.createDataSync("Sync", null, workspaceId, project.getId());

        assertThat(dataSyncDTO.projectId()).isEqualTo(project.getId());
        assertThat(projectService.getProjects(null, null, null, null, null, workspaceId))
            .extracting(Project::getId)
            .containsExactly(project.getId());
        assertThat(projectWorkflowService.getProjectWorkflowIds(project.getId(), project.getLastProjectVersion()))
            .containsExactlyInAnyOrder(ordinaryWorkflowId, dataSyncDTO.draftWorkflowId());
    }

    @Test
    void testCreateInProjectOfAnotherWorkspaceIsRefused() {
        Workspace otherWorkspace = workspaceRepository.save(new Workspace("other"));

        Project otherProject = new Project();

        otherProject.setName("Foreign");
        otherProject.setWorkspaceId(otherWorkspace.getId());

        Project savedOtherProject = projectService.create(otherProject);

        assertThatThrownBy(
            () -> dataSyncFacade.createDataSync("Sync", null, workspaceId, savedOtherProject.getId()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(dataSyncRepository.count()).isZero();
    }

    @Test
    void testTwoSyncsInOneProjectResolveTheirOwnDraftWorkflows() {
        Project project = saveOrdinaryProject("Shared");
        String ordinaryWorkflowId = addOrdinaryWorkflow(project);

        Workflow agentWorkflow = workflowService.create(
            JsonUtils.write(Map.of("label", "agent", "tasks", List.of())), Workflow.Format.JSON,
            Workflow.SourceType.JDBC);

        projectWorkflowService.addWorkflow(
            project.getId(), project.getLastProjectVersion(), agentWorkflow.getId(), ProjectWorkflowType.AI_AGENT);

        DataSyncDTO first = dataSyncFacade.createDataSync("First", null, workspaceId, project.getId());
        DataSyncDTO second = dataSyncFacade.createDataSync("Second", null, workspaceId, project.getId());

        long firstId = first.dataSync()
            .getId();
        long secondId = second.dataSync()
            .getId();

        dataSyncFacade.setDataSyncElement(firstId, Kind.SOURCE, "csvFile", 1, "read", Map.of(), 1L);
        dataSyncFacade.setDataSyncElement(secondId, Kind.SOURCE, "jsonFile", 1, "read", Map.of(), 1L);

        assertThat(List.of(first.projectId(), second.projectId())).containsOnly(project.getId());
        assertThat(first.draftWorkflowId()).isNotEqualTo(second.draftWorkflowId())
            .isNotIn(ordinaryWorkflowId, agentWorkflow.getId());
        assertThat(second.draftWorkflowId()).isNotIn(ordinaryWorkflowId, agentWorkflow.getId());

        assertThat(readDraftDataSyncId(firstId)).isEqualTo(firstId);
        assertThat(readDraftDataSyncId(secondId)).isEqualTo(secondId);
        assertThat(dataSyncFacade.getDataSyncs(workspaceId))
            .extracting(DataSyncDTO::draftWorkflowId)
            .containsExactlyInAnyOrder(first.draftWorkflowId(), second.draftWorkflowId());
    }

    @Test
    void testSyncAddedAfterPublishReportsUnpublished() {
        long firstId = createFullyConfigured();
        long projectId = dataSyncFacade.getDataSync(firstId)
            .projectId();

        publishProject(projectId, null);

        DataSyncDTO second = dataSyncFacade.createDataSync("Second", null, workspaceId, projectId);

        assertThat(second.projectId()).isEqualTo(projectId);
        assertThat(second.lastPublishedVersion()).isZero();
        assertThat(second.publishedDate()).isNull();
        assertThat(second.unpublishedChanges()).isTrue();
        assertThat(dataSyncFacade.getDataSync(firstId)
            .lastPublishedVersion()).isEqualTo(1);
    }

    @Test
    void testLastModifiedDateFollowsDraftWorkflow() {
        DataSyncDTO dataSyncDTO = dataSyncFacade.createDataSync("Sync", null, workspaceId, null);
        long id = dataSyncDTO.dataSync()
            .getId();

        Instant rowLastModifiedDate = dataSyncRepository.findById(id)
            .orElseThrow()
            .getLastModifiedDate();

        Instant createdLastModifiedDate = dataSyncDTO.lastModifiedDate();

        dataSyncFacade.setDataSyncElement(id, Kind.SOURCE, "csvFile", 1, "read", Map.of(), 1L);

        DataSyncDTO editedDataSyncDTO = dataSyncFacade.getDataSync(id);

        assertThat(dataSyncRepository.findById(id)
            .orElseThrow()
            .getLastModifiedDate()).isEqualTo(rowLastModifiedDate);
        assertThat(editedDataSyncDTO.lastModifiedDate()).isAfter(createdLastModifiedDate)
            .isAfter(rowLastModifiedDate)
            .isEqualTo(workflowService.getWorkflow(editedDataSyncDTO.draftWorkflowId())
                .getLastModifiedDate());
    }

    @Test
    void testDuplicateTitleGetsSuffixedSlug() {
        dataSyncFacade.createDataSync("Sync", null, workspaceId, null);

        DataSyncDTO second = dataSyncFacade.createDataSync("Sync", null, workspaceId, null);

        assertThat(second.dataSync()
            .getName()).isEqualTo("sync-2");
    }

    @Test
    void testSetElementRegeneratesDraftAndSyncsTestConnection() {
        DataSyncDTO dataSyncDTO = dataSyncFacade.createDataSync("Sync", null, workspaceId, null);
        long id = dataSyncDTO.dataSync()
            .getId();

        dataSyncFacade.setDataSyncElement(
            id, Kind.SOURCE, "csvFile", 1, "read", Map.of("delimiter", ","), 77L);

        Map<String, Object> definition = readDraft(id);

        @SuppressWarnings("unchecked")
        Map<String, Object> task = ((List<Map<String, Object>>) definition.get("tasks")).get(0);

        @SuppressWarnings("unchecked")
        Map<String, Object> clusterElements = (Map<String, Object>) task.get("clusterElements");

        assertThat(clusterElements).containsKey("source")
            .doesNotContainKey("destination");

        for (Environment environment : Environment.values()) {
            WorkflowTestConfiguration configuration = workflowTestConfigurationService
                .fetchWorkflowTestConfiguration(dataSyncDTO.draftWorkflowId(), environment.ordinal())
                .orElseThrow();

            assertThat(configuration.getConnections())
                .extracting(
                    WorkflowTestConfigurationConnection::getWorkflowNodeName,
                    WorkflowTestConfigurationConnection::getWorkflowConnectionKey,
                    WorkflowTestConfigurationConnection::getConnectionId)
                .contains(org.assertj.core.groups.Tuple.tuple(
                    DataSyncWorkflowGenerator.TASK_NODE_NAME, DataSyncWorkflowGenerator.nodeName(Kind.SOURCE), 77L));
        }
    }

    @Test
    void testReplacingSourceComponentDropsProcessor() {
        long id = createFullyConfigured();

        dataSyncFacade.setDataSyncElement(id, Kind.SOURCE, "jsonFile", 1, "read", Map.of(), null);

        assertThat(dataSyncElementRepository.findAllByDataSyncIdOrderByKindAsc(id))
            .extracting(DataSyncElement::getKind)
            .containsExactly(Kind.SOURCE, Kind.DESTINATION);
    }

    @Test
    void testUpdateElementParametersReplacesWholeMap() {
        long id = createFullyConfigured();

        DataSyncElement source = elementOf(id, Kind.SOURCE);

        dataSyncFacade.updateDataSyncElement(source.getId(), Map.of("path", "/tmp/a.csv"), 5L);

        DataSyncElement reloaded = elementOf(id, Kind.SOURCE);

        @SuppressWarnings("unchecked")
        Map<String, Object> parameters = (Map<String, Object>) reloaded.getParameters();

        assertThat(parameters).containsOnlyKeys("path");
        assertThat(reloaded.getConnectionId()).isEqualTo(5L);
        assertThat(dataSyncElementRepository.findAllByDataSyncIdOrderByKindAsc(id)).hasSize(3);
    }

    @Test
    void testProcessorMustBeFieldMapper() {
        long id = createFullyConfigured();

        assertThatThrownBy(
            () -> dataSyncFacade.setDataSyncElement(id, Kind.PROCESSOR, "script", 1, "run", Map.of(), null))
                .isInstanceOf(ConfigurationException.class)
                .satisfies(exception -> assertThat(((ConfigurationException) exception).getErrorKey())
                    .isEqualTo(DataSyncErrorType.PROCESSOR_NOT_FIELD_MAPPER.getErrorKey()));
    }

    @Test
    void testUpdateTriggerRewritesDraftTrigger() {
        long id = createFullyConfigured();

        dataSyncFacade.updateDataSyncTrigger(
            id, TriggerType.SCHEDULE, Map.of("expression", "0 9 * * ?", "timezone", "UTC", "frequencyKind", "DAILY"));

        @SuppressWarnings("unchecked")
        Map<String, Object> trigger = ((List<Map<String, Object>>) readDraft(id).get("triggers")).get(0);

        assertThat(trigger).containsEntry("type", "schedule/v1/cron");
    }

    @Test
    void testDeleteKeepsProjectSiblingSyncAndWorkflows() {
        Project project = saveOrdinaryProject("Shared");
        String ordinaryWorkflowId = addOrdinaryWorkflow(project);

        DataSyncDTO doomedDataSyncDTO = dataSyncFacade.createDataSync("Doomed", null, workspaceId, project.getId());
        DataSyncDTO siblingDataSyncDTO = dataSyncFacade.createDataSync("Sibling", null, workspaceId, project.getId());

        long doomedId = doomedDataSyncDTO.dataSync()
            .getId();
        long siblingId = siblingDataSyncDTO.dataSync()
            .getId();

        configure(doomedId);
        configure(siblingId);

        publishProject(project.getId(), null);

        String doomedUuid = String.valueOf(doomedDataSyncDTO.dataSync()
            .getProjectWorkflowUuid());

        List<String> doomedWorkflowIds = projectWorkflowService.getProjectWorkflows(project.getId(), doomedUuid)
            .stream()
            .map(ProjectWorkflow::getWorkflowId)
            .toList();

        List<String> survivingWorkflowIds = projectWorkflowRepository.findAllByProjectId(project.getId())
            .stream()
            .filter(projectWorkflow -> !doomedUuid.equals(String.valueOf(projectWorkflow.getUuid())))
            .map(ProjectWorkflow::getWorkflowId)
            .toList();

        assertThat(doomedWorkflowIds).hasSize(2)
            .contains(doomedDataSyncDTO.draftWorkflowId());
        assertThat(survivingWorkflowIds).hasSize(4)
            .contains(ordinaryWorkflowId, siblingDataSyncDTO.draftWorkflowId());
        assertThat(workflowTestConfigurationService.fetchWorkflowTestConfiguration(
            doomedDataSyncDTO.draftWorkflowId(), Environment.DEVELOPMENT.ordinal())).isPresent();

        dataSyncFacade.deleteDataSync(doomedId);

        assertThat(projectService.fetchProject(project.getId())).isPresent();
        assertThat(dataSyncRepository.findById(doomedId)).isEmpty();
        assertThat(dataSyncElementRepository.findAllByDataSyncIdOrderByKindAsc(doomedId)).isEmpty();
        assertThat(dataSyncRepository.findById(siblingId)).isPresent();
        assertThat(dataSyncElementRepository.findAllByDataSyncIdOrderByKindAsc(siblingId)).hasSize(2);
        assertThat(projectWorkflowRepository.findAllByProjectId(project.getId()))
            .extracting(ProjectWorkflow::getWorkflowId)
            .containsExactlyInAnyOrderElementsOf(survivingWorkflowIds);

        for (String doomedWorkflowId : doomedWorkflowIds) {
            assertThat(workflowCrudRepository.findById(doomedWorkflowId)).isEmpty();

            for (Environment environment : Environment.values()) {
                assertThat(workflowTestConfigurationService.fetchWorkflowTestConfiguration(
                    doomedWorkflowId, environment.ordinal())).isEmpty();
            }
        }

        for (String survivingWorkflowId : survivingWorkflowIds) {
            assertThat(workflowCrudRepository.findById(survivingWorkflowId)).isPresent();
        }

        assertThat(readDraftDataSyncId(siblingId)).isEqualTo(siblingId);
    }

    @Test
    void testDeleteRefusedWhileEnabledInADeployment() {
        long id = createFullyConfigured();
        long projectId = dataSyncFacade.getDataSync(id)
            .projectId();

        publishProject(projectId, null);

        long projectDeploymentId = deploy(projectId, Environment.PRODUCTION);

        addDeploymentWorkflow(projectDeploymentId, publishedWorkflowId(projectId), true);

        assertThatThrownBy(() -> dataSyncFacade.deleteDataSync(id))
            .isInstanceOf(ConfigurationException.class)
            .hasMessage("Data Sync " + id + " cannot be deleted while it is enabled in a deployment")
            .satisfies(exception -> assertThat(((ConfigurationException) exception).getErrorKey())
                .isEqualTo(DataSyncErrorType.DATA_SYNC_HAS_DEPLOYMENTS.getErrorKey()));

        assertThat(dataSyncRepository.findById(id)).isPresent();
    }

    @Test
    void testDeleteAllowedOnceDisabledRemovesDisabledDeploymentRow() {
        Project project = saveOrdinaryProject("Deployed");
        String ordinaryWorkflowId = addOrdinaryWorkflow(project);

        DataSyncDTO dataSyncDTO = dataSyncFacade.createDataSync("Sync", null, workspaceId, project.getId());
        long id = dataSyncDTO.dataSync()
            .getId();

        configure(id);

        publishProject(project.getId(), null);

        String publishedOrdinaryWorkflowId = publishedWorkflowId(project.getId(), ordinaryWorkflowId);
        String publishedSyncWorkflowId = projectWorkflowService
            .fetchProjectWorkflow(project.getId(), 1, String.valueOf(dataSyncDTO.dataSync()
                .getProjectWorkflowUuid()))
            .orElseThrow()
            .getWorkflowId();

        long projectDeploymentId = deploy(project.getId(), Environment.PRODUCTION);

        addDeploymentWorkflow(projectDeploymentId, publishedOrdinaryWorkflowId, true);
        addDeploymentWorkflow(projectDeploymentId, publishedSyncWorkflowId, false);

        dataSyncFacade.deleteDataSync(id);

        assertThat(dataSyncRepository.findById(id)).isEmpty();
        assertThat(projectService.fetchProject(project.getId())).isPresent();
        assertThat(projectDeploymentRepository.findById(projectDeploymentId)).isPresent();
        assertThat(projectDeploymentWorkflowRepository.findAllByWorkflowId(publishedSyncWorkflowId)).isEmpty();
        assertThat(projectDeploymentWorkflowRepository.findAllByProjectDeploymentId(projectDeploymentId))
            .extracting(ProjectDeploymentWorkflow::getWorkflowId)
            .containsExactly(publishedOrdinaryWorkflowId);
    }

    @Test
    void testDeleteChecksEveryDeploymentOfAnEnvironment() {
        long id = createFullyConfigured();
        long projectId = dataSyncFacade.getDataSync(id)
            .projectId();

        publishProject(projectId, null);

        String publishedSyncWorkflowId = publishedWorkflowId(projectId);

        long firstProjectDeploymentId = deploy(projectId, Environment.PRODUCTION);

        addDeploymentWorkflow(firstProjectDeploymentId, publishedSyncWorkflowId, false);

        long secondProjectDeploymentId = deploy(projectId, Environment.PRODUCTION);

        addDeploymentWorkflow(secondProjectDeploymentId, publishedSyncWorkflowId, true);

        assertThatThrownBy(() -> dataSyncFacade.deleteDataSync(id))
            .isInstanceOf(ConfigurationException.class)
            .satisfies(exception -> assertThat(((ConfigurationException) exception).getErrorKey())
                .isEqualTo(DataSyncErrorType.DATA_SYNC_HAS_DEPLOYMENTS.getErrorKey()));

        assertThat(dataSyncRepository.findById(id)).isPresent();
    }

    @Test
    void testDeleteProjectDataSyncsRemovesTheProjectsRows() {
        Project project = saveOrdinaryProject("Doomed");

        long firstId = dataSyncFacade.createDataSync("First", null, workspaceId, project.getId())
            .dataSync()
            .getId();
        long secondId = dataSyncFacade.createDataSync("Second", null, workspaceId, project.getId())
            .dataSync()
            .getId();

        configure(firstId);

        long otherId = createFullyConfigured();

        int projectWorkflowRowCount = projectWorkflowRepository.findAllByProjectId(project.getId())
            .size();

        dataSyncFacade.deleteProjectDataSyncs(project.getId());

        assertThat(dataSyncRepository.findById(firstId)).isEmpty();
        assertThat(dataSyncRepository.findById(secondId)).isEmpty();
        assertThat(dataSyncElementRepository.findAllByDataSyncIdOrderByKindAsc(firstId)).isEmpty();
        assertThat(dataSyncRepository.findById(otherId)).isPresent();
        assertThat(dataSyncElementRepository.findAllByDataSyncIdOrderByKindAsc(otherId)).hasSize(3);

        // Only the rows: ProjectFacadeImpl.deleteProject removes the project's workflows and deployments itself, after
        // its delete listeners have run.
        assertThat(projectWorkflowRepository.findAllByProjectId(project.getId())).hasSize(projectWorkflowRowCount);
        assertThat(projectService.fetchProject(project.getId())).isPresent();
    }

    @Test
    void testUnfinishedSyncBlocksProjectPublishNamingIt() {
        long withoutSourceProjectId = createOrdersToWarehouse();

        dataSyncFacade.setDataSyncElement(
            projectDataSyncId(withoutSourceProjectId), Kind.DESTINATION, "postgresql", 1, "insert", Map.of(), 2L);

        assertProjectPublishRefused(
            withoutSourceProjectId, DataSyncErrorType.SOURCE_MISSING, "Data Sync 'Orders to warehouse' has no source.");

        long withoutDestinationProjectId = createOrdersToWarehouse();

        dataSyncFacade.setDataSyncElement(
            projectDataSyncId(withoutDestinationProjectId), Kind.SOURCE, "csvFile", 1, "read", Map.of(), 1L);

        assertProjectPublishRefused(
            withoutDestinationProjectId, DataSyncErrorType.DESTINATION_MISSING,
            "Data Sync 'Orders to warehouse' has no destination.");

        long withoutConnectionProjectId = createOrdersToWarehouse();

        long withoutConnectionId = projectDataSyncId(withoutConnectionProjectId);

        dataSyncFacade.setDataSyncElement(withoutConnectionId, Kind.SOURCE, "csvFile", 1, "read", Map.of(), null);
        dataSyncFacade.setDataSyncElement(
            withoutConnectionId, Kind.DESTINATION, "postgresql", 1, "insert", Map.of(), 2L);

        ComponentDefinition csvFile = Mockito.mock(ComponentDefinition.class);

        Mockito.when(csvFile.isConnectionRequired())
            .thenReturn(true);
        Mockito.when(componentDefinitionService.getComponentDefinition("csvFile", 1))
            .thenReturn(csvFile);

        assertProjectPublishRefused(
            withoutConnectionProjectId, DataSyncErrorType.ELEMENT_CONNECTION_MISSING,
            "Element SOURCE of Data Sync 'Orders to warehouse' needs a connection.");

        long withoutCronProjectId = createOrdersToWarehouse();

        long withoutCronId = projectDataSyncId(withoutCronProjectId);

        dataSyncFacade.setDataSyncElement(withoutCronId, Kind.SOURCE, "csvFile", 1, "read", Map.of(), 1L);
        dataSyncFacade.setDataSyncElement(withoutCronId, Kind.DESTINATION, "postgresql", 1, "insert", Map.of(), 2L);
        dataSyncFacade.updateDataSyncTrigger(withoutCronId, TriggerType.SCHEDULE, Map.of("timezone", "UTC"));

        assertProjectPublishRefused(
            withoutCronProjectId, DataSyncErrorType.SCHEDULE_EXPRESSION_MISSING,
            "Data Sync 'Orders to warehouse' is scheduled but has no cron expression.");
    }

    @Test
    void testProjectPublishRegeneratesSyncsInPlace() {
        Project project = saveOrdinaryProject("Mixed");

        addOrdinaryWorkflow(project);

        DataSyncDTO dataSyncDTO = dataSyncFacade.createDataSync("Sync", null, workspaceId, project.getId());
        long id = dataSyncDTO.dataSync()
            .getId();

        dataSyncFacade.setDataSyncElement(id, Kind.SOURCE, "csvFile", 1, "read", Map.of(), 1L);
        dataSyncFacade.setDataSyncElement(id, Kind.DESTINATION, "postgresql", 1, "insert", Map.of(), 2L);

        // Written straight to the repository so that no facade call regenerates the draft first.
        DataSync dataSync = dataSyncRepository.findById(id)
            .orElseThrow();

        dataSync.setTriggerType(TriggerType.SCHEDULE);
        dataSync.setTriggerParameters(Map.of("expression", "0 9 * * ?"));

        dataSyncRepository.save(dataSync);

        assertThat(firstTrigger(readDraft(id))).doesNotContainEntry("type", "schedule/v1/cron");

        int draftRowCount = projectWorkflowRepository.findAllByProjectId(project.getId())
            .size();

        publishProject(project.getId(), "v1");

        assertThat(projectWorkflowRepository.findAllByProjectId(project.getId())).hasSize(draftRowCount * 2);

        ProjectWorkflow publishedProjectWorkflow = projectWorkflowService
            .fetchProjectWorkflow(project.getId(), 1, String.valueOf(dataSync.getProjectWorkflowUuid()))
            .orElseThrow();

        Map<String, Object> publishedDefinition = JsonUtils.read(
            workflowService.getWorkflow(publishedProjectWorkflow.getWorkflowId())
                .getDefinition(),
            new TypeReference<>() {});

        assertThat(firstTrigger(publishedDefinition)).containsEntry("type", "schedule/v1/cron");
    }

    @Test
    void testPublishMintsVersionAndClearsUnpublishedChanges() {
        long id = createFullyConfigured();

        int version = publishProject(dataSyncFacade.getDataSync(id)
            .projectId(), "first");

        DataSyncDTO published = dataSyncFacade.getDataSync(id);

        // publishProject returns the newly-minted DRAFT version number, exactly as ProjectService.publishProject
        // does (see AiAgentFacadeIntTest.testPublishAgentWithGatedToolPublishesApprovalGateDeliveringOverChat's
        // assertThat(newVersion).isEqualTo(2)) — Project.publish() advances the draft and returns that new number,
        // not the version it just marked PUBLISHED.
        assertThat(version).isEqualTo(2);
        assertThat(published.lastPublishedVersion()).isEqualTo(1);
        assertThat(published.unpublishedChanges()).isFalse();
        assertThat(dataSyncFacade.getDataSyncVersions(id)).extracting(DataSyncVersionDTO::version)
            .containsExactly(2, 1);

        dataSyncFacade.updateDataSyncTrigger(id, TriggerType.SCHEDULE, Map.of("expression", "0 9 * * ?"));

        assertThat(dataSyncFacade.getDataSync(id)
            .unpublishedChanges()).isTrue();
    }

    @Test
    void testDeploymentsListAndRunNow() {
        long id = createFullyConfigured();

        long projectId = dataSyncFacade.getDataSync(id)
            .projectId();

        publishProject(projectId, null);

        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setName("deploy");
        projectDeployment.setProjectId(projectId);
        projectDeployment.setProjectVersion(1);
        projectDeployment.setEnvironment(Environment.PRODUCTION);
        projectDeployment.setEnabled(true);

        ProjectDeployment savedProjectDeployment = projectDeploymentRepository.save(projectDeployment);

        ProjectDeploymentWorkflow projectDeploymentWorkflow = new ProjectDeploymentWorkflow();

        projectDeploymentWorkflow.setProjectDeploymentId(savedProjectDeployment.getId());
        projectDeploymentWorkflow.setWorkflowId(publishedWorkflowId(projectId));
        projectDeploymentWorkflow.setEnabled(true);

        projectDeploymentWorkflowRepository.save(projectDeploymentWorkflow);

        List<DataSyncDeploymentDTO> deployments = dataSyncFacade.getDataSyncDeployments(workspaceId);

        assertThat(deployments).singleElement()
            .satisfies(deployment -> {
                assertThat(deployment.dataSyncId()).isEqualTo(id);
                assertThat(deployment.triggerType()).isEqualTo(TriggerType.MANUAL);
                assertThat(deployment.workflowId()).isNotBlank();
            });

        Mockito.when(projectDeploymentFacade.createProjectDeploymentWorkflowJob(
            Mockito.eq(savedProjectDeployment.getId()), Mockito.eq(publishedWorkflowId(projectId))))
            .thenReturn(99L);

        assertThat(dataSyncFacade.runDataSyncDeployment(id, savedProjectDeployment.getId())).isEqualTo(99L);
    }

    @Test
    void testTwoDeploymentsInOneEnvironmentAreBothListed() {
        Project project = saveOrdinaryProject("Shared");
        String ordinaryWorkflowId = addOrdinaryWorkflow(project);

        DataSyncDTO dataSyncDTO = dataSyncFacade.createDataSync("Sync", null, workspaceId, project.getId());
        long id = dataSyncDTO.dataSync()
            .getId();

        configure(id);

        publishProject(project.getId(), null);

        String publishedOrdinaryWorkflowId = publishedWorkflowId(project.getId(), ordinaryWorkflowId);
        String publishedSyncWorkflowId = publishedWorkflowId(project.getId(), dataSyncDTO.draftWorkflowId());

        long firstProjectDeploymentId = deploy(project.getId(), Environment.PRODUCTION);

        addDeploymentWorkflow(firstProjectDeploymentId, publishedOrdinaryWorkflowId, true);
        addDeploymentWorkflow(firstProjectDeploymentId, publishedSyncWorkflowId, true);

        long secondProjectDeploymentId = deploy(project.getId(), Environment.PRODUCTION);

        addDeploymentWorkflow(secondProjectDeploymentId, publishedOrdinaryWorkflowId, true);
        addDeploymentWorkflow(secondProjectDeploymentId, publishedSyncWorkflowId, false);

        List<DataSyncDeploymentDTO> deployments = dataSyncFacade.getDataSyncDeployments(workspaceId);

        assertThat(deployments)
            .extracting(
                DataSyncDeploymentDTO::id, DataSyncDeploymentDTO::dataSyncId, DataSyncDeploymentDTO::projectId,
                DataSyncDeploymentDTO::environmentId, DataSyncDeploymentDTO::workflowId)
            .containsExactlyInAnyOrder(
                tuple(
                    firstProjectDeploymentId, id, project.getId(), Environment.PRODUCTION.ordinal(),
                    publishedSyncWorkflowId),
                tuple(
                    secondProjectDeploymentId, id, project.getId(), Environment.PRODUCTION.ordinal(),
                    publishedSyncWorkflowId));
    }

    @Test
    void testDeploymentsListOnlyVersionsContainingTheSync() {
        Project project = saveOrdinaryProject("Growing");
        String ordinaryWorkflowId = addOrdinaryWorkflow(project);

        publishProject(project.getId(), null);

        DataSyncDTO dataSyncDTO = dataSyncFacade.createDataSync("Sync", null, workspaceId, project.getId());
        long id = dataSyncDTO.dataSync()
            .getId();

        configure(id);

        publishProject(project.getId(), null);

        String secondVersionSyncWorkflowId = versionWorkflowId(
            project.getId(), 2, String.valueOf(dataSyncDTO.dataSync()
                .getProjectWorkflowUuid()));

        long preSyncProjectDeploymentId = deploy(project.getId(), Environment.PRODUCTION, 1);

        addDeploymentWorkflow(
            preSyncProjectDeploymentId, publishedWorkflowId(project.getId(), ordinaryWorkflowId), true);

        // Pinned to a version holding the Data Sync, but without a project_deployment_workflow row for it.
        long withoutSyncRowProjectDeploymentId = deploy(project.getId(), Environment.STAGING, 2);

        addDeploymentWorkflow(
            withoutSyncRowProjectDeploymentId,
            versionWorkflowId(project.getId(), 2, workflowUuid(project.getId(), ordinaryWorkflowId)), true);

        long syncProjectDeploymentId = deploy(project.getId(), Environment.DEVELOPMENT, 2);

        addDeploymentWorkflow(syncProjectDeploymentId, secondVersionSyncWorkflowId, true);

        assertThat(dataSyncFacade.getDataSyncDeployments(workspaceId))
            .extracting(
                DataSyncDeploymentDTO::id, DataSyncDeploymentDTO::projectVersion, DataSyncDeploymentDTO::workflowId)
            .containsExactly(tuple(syncProjectDeploymentId, 2, secondVersionSyncWorkflowId));
    }

    @Test
    void testDeploymentTriggerTypeIsTheDeployedTrigger() {
        long id = createFullyConfigured();
        long projectId = dataSyncFacade.getDataSync(id)
            .projectId();

        publishProject(projectId, null);

        long manualProjectDeploymentId = deploy(projectId, Environment.PRODUCTION, 1);

        addDeploymentWorkflow(manualProjectDeploymentId, publishedWorkflowId(projectId), true);

        dataSyncFacade.updateDataSyncTrigger(
            id, TriggerType.SCHEDULE, Map.of("expression", "0 9 * * ?", "timezone", "UTC"));

        assertThat(dataSyncFacade.getDataSyncDeployments(workspaceId))
            .extracting(DataSyncDeploymentDTO::id, DataSyncDeploymentDTO::triggerType)
            .containsExactly(tuple(manualProjectDeploymentId, TriggerType.MANUAL));

        publishProject(projectId, null);

        DataSync dataSync = dataSyncRepository.findById(id)
            .orElseThrow();

        long scheduleProjectDeploymentId = deploy(projectId, Environment.PRODUCTION, 2);

        addDeploymentWorkflow(
            scheduleProjectDeploymentId,
            versionWorkflowId(projectId, 2, String.valueOf(dataSync.getProjectWorkflowUuid())), true);

        assertThat(dataSyncFacade.getDataSyncDeployments(workspaceId))
            .extracting(DataSyncDeploymentDTO::id, DataSyncDeploymentDTO::triggerType)
            .containsExactlyInAnyOrder(
                tuple(manualProjectDeploymentId, TriggerType.MANUAL),
                tuple(scheduleProjectDeploymentId, TriggerType.SCHEDULE));
    }

    @Test
    void testRunResolvesTheSyncsWorkflowAmongSeveral() {
        Project project = saveOrdinaryProject("Crowded");
        String ordinaryWorkflowId = addOrdinaryWorkflow(project);

        DataSyncDTO firstDataSyncDTO = dataSyncFacade.createDataSync("First", null, workspaceId, project.getId());
        DataSyncDTO secondDataSyncDTO = dataSyncFacade.createDataSync("Second", null, workspaceId, project.getId());

        long firstId = firstDataSyncDTO.dataSync()
            .getId();
        long secondId = secondDataSyncDTO.dataSync()
            .getId();

        configure(firstId);
        configure(secondId);

        publishProject(project.getId(), null);

        String publishedSecondWorkflowId = publishedWorkflowId(project.getId(), secondDataSyncDTO.draftWorkflowId());

        long projectDeploymentId = deploy(project.getId(), Environment.PRODUCTION);

        addDeploymentWorkflow(projectDeploymentId, publishedWorkflowId(project.getId(), ordinaryWorkflowId), true);
        addDeploymentWorkflow(
            projectDeploymentId, publishedWorkflowId(project.getId(), firstDataSyncDTO.draftWorkflowId()), true);
        addDeploymentWorkflow(projectDeploymentId, publishedSecondWorkflowId, true);

        Mockito.when(projectDeploymentFacade.createProjectDeploymentWorkflowJob(
            Mockito.anyLong(), Mockito.anyString()))
            .thenReturn(99L);

        assertThat(dataSyncFacade.runDataSyncDeployment(secondId, projectDeploymentId)).isEqualTo(99L);

        Mockito.verify(projectDeploymentFacade)
            .createProjectDeploymentWorkflowJob(projectDeploymentId, publishedSecondWorkflowId);
    }

    @Test
    void testRunRefusesForeignDeployment() {
        long id = createFullyConfigured();
        long otherId = createFullyConfigured();

        long otherProjectId = dataSyncFacade.getDataSync(otherId)
            .projectId();

        publishProject(otherProjectId, null);

        long projectDeploymentId = deploy(otherProjectId, Environment.PRODUCTION);

        addDeploymentWorkflow(projectDeploymentId, publishedWorkflowId(otherProjectId), true);

        assertRunRefused(
            id, projectDeploymentId, DataSyncErrorType.DEPLOYMENT_NOT_OWNED,
            "Deployment " + projectDeploymentId + " does not deploy Data Sync 'Sync'");
    }

    @Test
    void testRunRefusesDeploymentPinnedBeforeTheSync() {
        Project project = saveOrdinaryProject("Growing");
        String ordinaryWorkflowId = addOrdinaryWorkflow(project);

        publishProject(project.getId(), null);

        long id = dataSyncFacade.createDataSync("Late", null, workspaceId, project.getId())
            .dataSync()
            .getId();

        configure(id);

        publishProject(project.getId(), null);

        long projectDeploymentId = deploy(project.getId(), Environment.PRODUCTION, 1);

        addDeploymentWorkflow(projectDeploymentId, publishedWorkflowId(project.getId(), ordinaryWorkflowId), true);

        assertRunRefused(
            id, projectDeploymentId, DataSyncErrorType.DEPLOYMENT_NOT_OWNED,
            "Deployment " + projectDeploymentId + " does not deploy Data Sync 'Late'");
    }

    @Test
    void testRunRefusesDisabledDeployment() {
        long id = createFullyConfigured();
        long projectId = dataSyncFacade.getDataSync(id)
            .projectId();

        publishProject(projectId, null);

        long projectDeploymentId = deploy(projectId, Environment.PRODUCTION);

        addDeploymentWorkflow(projectDeploymentId, publishedWorkflowId(projectId), true);

        ProjectDeployment projectDeployment = projectDeploymentRepository.findById(projectDeploymentId)
            .orElseThrow();

        projectDeployment.setEnabled(false);

        projectDeploymentRepository.save(projectDeployment);

        assertRunRefused(
            id, projectDeploymentId, DataSyncErrorType.DEPLOYMENT_DISABLED,
            "Deployment " + projectDeploymentId + " is disabled");
    }

    @Test
    void testRunRefusesDisabledSyncRow() {
        long id = createFullyConfigured();
        long projectId = dataSyncFacade.getDataSync(id)
            .projectId();

        publishProject(projectId, null);

        long projectDeploymentId = deploy(projectId, Environment.PRODUCTION);

        addDeploymentWorkflow(projectDeploymentId, publishedWorkflowId(projectId), false);

        assertRunRefused(
            id, projectDeploymentId, DataSyncErrorType.DEPLOYMENT_DISABLED,
            "Data Sync 'Sync' is disabled in deployment " + projectDeploymentId);
    }

    @Test
    void testRunIsCalledOutsideATransaction() {
        long id = createFullyConfigured();
        long projectId = dataSyncFacade.getDataSync(id)
            .projectId();

        publishProject(projectId, null);

        long projectDeploymentId = deploy(projectId, Environment.PRODUCTION);

        addDeploymentWorkflow(projectDeploymentId, publishedWorkflowId(projectId), true);

        AtomicReference<Boolean> transactionActive = new AtomicReference<>();

        Mockito.when(projectDeploymentFacade.createProjectDeploymentWorkflowJob(
            Mockito.anyLong(), Mockito.anyString()))
            .thenAnswer(invocation -> {
                transactionActive.set(TransactionSynchronizationManager.isActualTransactionActive());

                return 99L;
            });

        dataSyncFacade.runDataSyncDeployment(id, projectDeploymentId);

        assertThat(transactionActive.get()).isFalse();
    }

    @Test
    void testExportDocumentShape() {
        long id = createFullyConfigured();

        dataSyncFacade.updateDataSync(id, "Orders", "nightly");
        dataSyncFacade.updateDataSyncTrigger(
            id, TriggerType.SCHEDULE, Map.of("expression", "0 0 6 * * ?", "timezone", "UTC"));

        String json = dataSyncFacade.exportDataSync(id);

        Map<String, Object> document = JsonUtils.read(json, new TypeReference<>() {});

        assertThat(json).contains("\n")
            .doesNotContain("connectionId")
            .doesNotContain("connection");
        assertThat(document).containsOnlyKeys(
            "exportVersion", "name", "title", "description", "triggerType", "triggerParameters", "elements");
        assertThat(document).containsEntry("exportVersion", 1)
            .containsEntry("name", "sync")
            .containsEntry("title", "Orders")
            .containsEntry("description", "nightly")
            .containsEntry("triggerType", "SCHEDULE")
            .containsEntry("triggerParameters", Map.of("expression", "0 0 6 * * ?", "timezone", "UTC"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> elements = (List<Map<String, Object>>) document.get("elements");

        assertThat(elements)
            .extracting(
                element -> element.get("kind"), element -> element.get("componentName"),
                element -> element.get("componentVersion"), element -> element.get("operationName"))
            .containsExactly(
                tuple("SOURCE", "csvFile", 1, "read"), tuple("DESTINATION", "postgresql", 1, "insert"),
                tuple(
                    "PROCESSOR", DataSyncElement.PROCESSOR_COMPONENT_NAME, DataSyncElement.PROCESSOR_COMPONENT_VERSION,
                    DataSyncElement.PROCESSOR_OPERATION_NAME));
        assertThat(elements.get(0)).containsOnlyKeys(
            "kind", "componentName", "componentVersion", "operationName", "parameters")
            .containsEntry("parameters", Map.of("delimiter", ","));
    }

    @Test
    void testImportRecreatesWithoutConnections() {
        long sourceId = createFullyConfigured();

        String json = dataSyncFacade.exportDataSync(sourceId);

        DataSyncDTO importedDataSyncDTO = dataSyncFacade.importDataSync(workspaceId, json, null);

        DataSync importedDataSync = importedDataSyncDTO.dataSync();
        long importedId = importedDataSync.getId();

        assertThat(importedId).isNotEqualTo(sourceId);
        assertThat(importedDataSync.getName()).isEqualTo("sync-2");
        assertThat(importedDataSync.getTitle()).isEqualTo("Sync");
        assertThat(importedDataSyncDTO.projectId()).isNotEqualTo(dataSyncFacade.getDataSync(sourceId)
            .projectId());
        assertThat(importedDataSync.getWorkspaceId()).isEqualTo(projectService.getProject(
            importedDataSyncDTO.projectId())
            .getWorkspaceId());
        assertThat(dataSyncElementRepository.findAllByDataSyncIdOrderByKindAsc(importedId))
            .extracting(DataSyncElement::getKind, DataSyncElement::getComponentName, DataSyncElement::getConnectionId)
            .containsExactly(
                tuple(Kind.SOURCE, "csvFile", null), tuple(Kind.DESTINATION, "postgresql", null),
                tuple(Kind.PROCESSOR, DataSyncElement.PROCESSOR_COMPONENT_NAME, null));
        assertThat(readDraftDataSyncId(importedId)).isEqualTo(importedId);

        @SuppressWarnings("unchecked")
        Map<String, Object> clusterElements = (Map<String, Object>) ((List<Map<String, Object>>) readDraft(importedId)
            .get("tasks")).get(0)
                .get("clusterElements");

        assertThat(clusterElements).containsKeys("source", "destination");

        ComponentDefinition csvFile = Mockito.mock(ComponentDefinition.class);

        Mockito.when(csvFile.isConnectionRequired())
            .thenReturn(true);
        Mockito.when(componentDefinitionService.getComponentDefinition("csvFile", 1))
            .thenReturn(csvFile);

        assertProjectPublishRefused(
            importedDataSyncDTO.projectId(), DataSyncErrorType.ELEMENT_CONNECTION_MISSING,
            "Element SOURCE of Data Sync 'Sync' needs a connection.");
    }

    @Test
    void testImportIntoExistingProject() {
        Project project = saveOrdinaryProject("Target");

        DataSyncDTO importedDataSyncDTO = dataSyncFacade.importDataSync(
            workspaceId, exportDocument(1, List.of()), project.getId());

        assertThat(importedDataSyncDTO.projectId()).isEqualTo(project.getId());
        assertThat(importedDataSyncDTO.dataSync()
            .getName()).isEqualTo("imported");
        assertThat(importedDataSyncDTO.dataSync()
            .getWorkspaceId()).isEqualTo(workspaceId);
    }

    @Test
    void testImportRejectsNonFieldMapperProcessor() {
        String json = exportDocument(
            1,
            List.of(exportedElement("PROCESSOR", "script", 1, "run"), exportedElement("SOURCE", "csvFile", 1, "read")));

        assertThatThrownBy(() -> dataSyncFacade.importDataSync(workspaceId, json, null))
            .isInstanceOf(ConfigurationException.class)
            .satisfies(exception -> assertThat(((ConfigurationException) exception).getErrorKey())
                .isEqualTo(DataSyncErrorType.PROCESSOR_NOT_FIELD_MAPPER.getErrorKey()));
        assertThat(dataSyncRepository.count()).isZero();
        assertThat(projectService.getProjects(null, null, null, null, null, workspaceId)).isEmpty();
    }

    @Test
    void testImportRejectsUnsupportedExportVersion() {
        String json = exportDocument(2, List.of());

        assertThatThrownBy(() -> dataSyncFacade.importDataSync(workspaceId, json, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("exportVersion");
        assertThat(dataSyncRepository.count()).isZero();
    }

    @Test
    void testCopyProjectDataSyncsKeepsConnections() {
        long sourceId = createFullyConfigured();

        dataSyncFacade.updateDataSyncTrigger(
            sourceId, TriggerType.SCHEDULE, Map.of("expression", "0 0 6 * * ?", "timezone", "UTC"));

        long sourceProjectId = dataSyncFacade.getDataSync(sourceId)
            .projectId();

        Project targetProject = saveOrdinaryProject("Duplicate");

        List<DataSyncDTO> copiedDataSyncDTOs = dataSyncFacade.copyProjectDataSyncs(
            sourceProjectId, targetProject.getId(), workspaceId);

        assertThat(copiedDataSyncDTOs).hasSize(1);

        DataSyncDTO copiedDataSyncDTO = copiedDataSyncDTOs.get(0);

        DataSync copiedDataSync = copiedDataSyncDTO.dataSync();
        long copiedId = copiedDataSync.getId();

        assertThat(copiedId).isNotEqualTo(sourceId);
        assertThat(copiedDataSync.getName()).isEqualTo("sync-2");
        assertThat(copiedDataSync.getTitle()).isEqualTo("Sync");
        assertThat(copiedDataSync.getTriggerType()).isEqualTo(TriggerType.SCHEDULE);
        assertThat(copiedDataSyncDTO.projectId()).isEqualTo(targetProject.getId());
        assertThat(dataSyncElementRepository.findAllByDataSyncIdOrderByKindAsc(copiedId))
            .extracting(DataSyncElement::getKind, DataSyncElement::getComponentName, DataSyncElement::getConnectionId)
            .containsExactly(
                tuple(Kind.SOURCE, "csvFile", 1L), tuple(Kind.DESTINATION, "postgresql", 2L),
                tuple(Kind.PROCESSOR, DataSyncElement.PROCESSOR_COMPONENT_NAME, null));

        ProjectWorkflow copiedProjectWorkflow = projectWorkflowService
            .fetchProjectWorkflow(
                targetProject.getId(), targetProject.getLastProjectVersion(),
                String.valueOf(copiedDataSync.getProjectWorkflowUuid()))
            .orElseThrow();

        assertThat(copiedProjectWorkflow.getType()).isEqualTo(ProjectWorkflowType.DATA_SYNC);
        assertThat(copiedProjectWorkflow.getWorkflowId()).isEqualTo(copiedDataSyncDTO.draftWorkflowId())
            .isNotEqualTo(dataSyncFacade.getDataSync(sourceId)
                .draftWorkflowId());
        assertThat(readDraftDataSyncId(copiedId)).isEqualTo(copiedId);
        assertThat(firstTrigger(readDraft(copiedId))).containsEntry("type", "schedule/v1/cron");

        for (Environment environment : Environment.values()) {
            WorkflowTestConfiguration configuration = workflowTestConfigurationService
                .fetchWorkflowTestConfiguration(copiedDataSyncDTO.draftWorkflowId(), environment.ordinal())
                .orElseThrow();

            assertThat(configuration.getConnections())
                .extracting(
                    WorkflowTestConfigurationConnection::getWorkflowConnectionKey,
                    WorkflowTestConfigurationConnection::getConnectionId)
                .contains(
                    tuple(DataSyncWorkflowGenerator.nodeName(Kind.SOURCE), 1L),
                    tuple(DataSyncWorkflowGenerator.nodeName(Kind.DESTINATION), 2L));
        }
    }

    @Test
    void testUpdateFromExportUpsertsByKind() {
        long id = createFullyConfigured();

        Map<String, Object> document = Map.of(
            "exportVersion", 1,
            "name", "sync",
            "title", "Renamed",
            "description", "pulled",
            "triggerType", "SCHEDULE",
            "triggerParameters", Map.of("expression", "0 0 6 * * ?", "timezone", "UTC"),
            "elements", List.of(
                exportedElement("SOURCE", "csvFile", 1, "read", Map.of("delimiter", ";")),
                exportedElement("DESTINATION", "mysql", 1, "insert", Map.of())));

        DataSyncDTO updatedDataSyncDTO = dataSyncFacade.updateDataSyncFromExport(id, JsonUtils.write(document));

        DataSync updatedDataSync = updatedDataSyncDTO.dataSync();

        assertThat(updatedDataSync.getId()).isEqualTo(id);
        assertThat(updatedDataSync.getTitle()).isEqualTo("Renamed");
        assertThat(updatedDataSync.getDescription()).isEqualTo("pulled");
        assertThat(updatedDataSync.getTriggerType()).isEqualTo(TriggerType.SCHEDULE);
        assertThat(dataSyncElementRepository.findAllByDataSyncIdOrderByKindAsc(id))
            .extracting(DataSyncElement::getKind, DataSyncElement::getComponentName, DataSyncElement::getConnectionId)
            .containsExactly(tuple(Kind.SOURCE, "csvFile", 1L), tuple(Kind.DESTINATION, "mysql", null));
        assertThat(elementOf(id, Kind.SOURCE).getParameters()).isEqualTo(Map.of("delimiter", ";"));
        assertThat(firstTrigger(readDraft(id))).containsEntry("type", "schedule/v1/cron");

        // Test connections are keyed by kind, not component: the replaced destination must not keep running the old
        // component's connection in the editor's Test step.
        for (Environment environment : Environment.values()) {
            WorkflowTestConfiguration configuration = workflowTestConfigurationService
                .fetchWorkflowTestConfiguration(updatedDataSyncDTO.draftWorkflowId(), environment.ordinal())
                .orElseThrow();

            assertThat(configuration.getConnections())
                .extracting(
                    WorkflowTestConfigurationConnection::getWorkflowConnectionKey,
                    WorkflowTestConfigurationConnection::getConnectionId)
                .containsExactly(tuple(DataSyncWorkflowGenerator.nodeName(Kind.SOURCE), 1L));
        }
    }

    @Test
    void testUpdateFromExportWithoutElementsIsRejected() {
        long id = createFullyConfigured();

        String json = JsonUtils.write(
            Map.of("exportVersion", 1, "name", "sync", "title", "Renamed", "triggerType", "MANUAL"));

        assertThatThrownBy(() -> dataSyncFacade.updateDataSyncFromExport(id, json))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("elements");
        assertThat(dataSyncElementRepository.findAllByDataSyncIdOrderByKindAsc(id))
            .extracting(DataSyncElement::getKind, DataSyncElement::getConnectionId)
            .containsExactly(tuple(Kind.SOURCE, 1L), tuple(Kind.DESTINATION, 2L), tuple(Kind.PROCESSOR, null));
        assertThat(dataSyncRepository.findById(id)
            .orElseThrow()
            .getTitle()).isEqualTo("Sync");
    }

    @Test
    void testUpdateFromExportWithMalformedElementIsRejected() {
        long id = createFullyConfigured();

        List<Object> malformedElements = List.of(
            Collections.singletonList(null), List.of("source"), List.of(List.of()));

        for (Object elements : malformedElements) {
            String json = JsonUtils.write(
                Map.of("exportVersion", 1, "name", "sync", "title", "Renamed", "triggerType", "MANUAL", "elements",
                    elements));

            assertThatThrownBy(() -> dataSyncFacade.updateDataSyncFromExport(id, json))
                .as(json)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("elements");
        }

        assertThat(dataSyncElementRepository.findAllByDataSyncIdOrderByKindAsc(id))
            .extracting(DataSyncElement::getKind, DataSyncElement::getConnectionId)
            .containsExactly(tuple(Kind.SOURCE, 1L), tuple(Kind.DESTINATION, 2L), tuple(Kind.PROCESSOR, null));
        assertThat(dataSyncRepository.findById(id)
            .orElseThrow()
            .getTitle()).isEqualTo("Sync");
    }

    @Test
    void testImportRejectsMalformedTriggerTypeOrElements() {
        List<Map<String, Object>> malformedDocuments = List.of(
            Map.of("exportVersion", 1, "title", "Imported", "elements", List.of()),
            Map.of("exportVersion", 1, "title", "Imported", "triggerType", "HOURLY", "elements", List.of()),
            Map.of("exportVersion", 1, "title", "Imported", "triggerType", "MANUAL"),
            Map.of("exportVersion", 1, "title", "Imported", "triggerType", "MANUAL", "elements", "none"),
            Map.of(
                "exportVersion", 1, "title", "Imported", "triggerType", "MANUAL", "elements",
                Collections.singletonList(null)),
            Map.of("exportVersion", 1, "title", "Imported", "triggerType", "MANUAL", "elements", List.of("source")),
            Map.of("exportVersion", 1, "title", "Imported", "triggerType", "MANUAL", "elements", List.of(List.of())));

        for (Map<String, Object> malformedDocument : malformedDocuments) {
            String json = JsonUtils.write(malformedDocument);

            assertThatThrownBy(() -> dataSyncFacade.importDataSync(workspaceId, json, null))
                .as(json)
                .isInstanceOf(IllegalArgumentException.class);
        }

        assertThat(dataSyncRepository.count()).isZero();
        assertThat(projectService.getProjects(null, null, null, null, null, workspaceId)).isEmpty();
    }

    private static String exportDocument(int exportVersion, List<Map<String, Object>> elements) {
        return JsonUtils.write(
            Map.of(
                "exportVersion", exportVersion,
                "name", "imported",
                "title", "Imported",
                "triggerType", "MANUAL",
                "elements", elements));
    }

    private static Map<String, Object> exportedElement(
        String kind, String componentName, int componentVersion, String operationName) {

        return exportedElement(kind, componentName, componentVersion, operationName, Map.of());
    }

    private static Map<String, Object> exportedElement(
        String kind, String componentName, int componentVersion, String operationName, Map<String, Object> parameters) {

        return Map.of(
            "kind", kind, "componentName", componentName, "componentVersion", componentVersion, "operationName",
            operationName, "parameters", parameters);
    }

    private void assertRunRefused(long id, long projectDeploymentId, DataSyncErrorType errorType, String message) {
        assertThatThrownBy(() -> dataSyncFacade.runDataSyncDeployment(id, projectDeploymentId))
            .isInstanceOf(ConfigurationException.class)
            .hasMessage(message)
            .satisfies(exception -> assertThat(((ConfigurationException) exception).getErrorKey())
                .isEqualTo(errorType.getErrorKey()));

        Mockito.verifyNoInteractions(projectDeploymentFacade);
    }

    private long createFullyConfigured() {
        DataSyncDTO dataSyncDTO = dataSyncFacade.createDataSync("Sync", null, workspaceId, null);
        long id = dataSyncDTO.dataSync()
            .getId();

        dataSyncFacade.setDataSyncElement(id, Kind.SOURCE, "csvFile", 1, "read", Map.of("delimiter", ","), 1L);
        dataSyncFacade.setDataSyncElement(id, Kind.DESTINATION, "postgresql", 1, "insert", Map.of(), 2L);
        dataSyncFacade.setDataSyncElement(
            id, Kind.PROCESSOR, DataSyncElement.PROCESSOR_COMPONENT_NAME, DataSyncElement.PROCESSOR_COMPONENT_VERSION,
            DataSyncElement.PROCESSOR_OPERATION_NAME, Map.of("mappings", List.of()), null);

        return id;
    }

    private void configure(long id) {
        dataSyncFacade.setDataSyncElement(id, Kind.SOURCE, "csvFile", 1, "read", Map.of(), 1L);
        dataSyncFacade.setDataSyncElement(id, Kind.DESTINATION, "postgresql", 1, "insert", Map.of(), 2L);
    }

    private long deploy(long projectId, Environment environment) {
        return deploy(projectId, environment, 1);
    }

    private long deploy(long projectId, Environment environment, int projectVersion) {
        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setName("deploy");
        projectDeployment.setProjectId(projectId);
        projectDeployment.setProjectVersion(projectVersion);
        projectDeployment.setEnvironment(environment);
        projectDeployment.setEnabled(true);

        ProjectDeployment savedProjectDeployment = projectDeploymentRepository.save(projectDeployment);

        return savedProjectDeployment.getId();
    }

    private void addDeploymentWorkflow(long projectDeploymentId, String workflowId, boolean enabled) {
        ProjectDeploymentWorkflow projectDeploymentWorkflow = new ProjectDeploymentWorkflow();

        projectDeploymentWorkflow.setProjectDeploymentId(projectDeploymentId);
        projectDeploymentWorkflow.setWorkflowId(workflowId);
        projectDeploymentWorkflow.setEnabled(enabled);

        projectDeploymentWorkflowRepository.save(projectDeploymentWorkflow);
    }

    /** The version-1 copy of a draft workflow, matched through the {@code project_workflow.uuid} both rows share. */
    private String publishedWorkflowId(long projectId, String draftWorkflowId) {
        List<ProjectWorkflow> projectWorkflows = projectWorkflowRepository.findAllByProjectId(projectId);

        ProjectWorkflow draftProjectWorkflow = projectWorkflows.stream()
            .filter(projectWorkflow -> draftWorkflowId.equals(projectWorkflow.getWorkflowId()))
            .findFirst()
            .orElseThrow();

        return projectWorkflows.stream()
            .filter(projectWorkflow -> projectWorkflow.getProjectVersion() == 1)
            .filter(projectWorkflow -> Objects.equals(projectWorkflow.getUuid(), draftProjectWorkflow.getUuid()))
            .findFirst()
            .orElseThrow()
            .getWorkflowId();
    }

    private String workflowUuid(long projectId, String workflowId) {
        return projectWorkflowRepository.findAllByProjectId(projectId)
            .stream()
            .filter(projectWorkflow -> workflowId.equals(projectWorkflow.getWorkflowId()))
            .findFirst()
            .orElseThrow()
            .getUuidAsString();
    }

    private String versionWorkflowId(long projectId, int projectVersion, String workflowUuid) {
        return projectWorkflowService.fetchProjectWorkflow(projectId, projectVersion, workflowUuid)
            .orElseThrow()
            .getWorkflowId();
    }

    private long createOrdersToWarehouse() {
        return dataSyncFacade.createDataSync("Orders to warehouse", null, workspaceId, null)
            .projectId();
    }

    private long projectDataSyncId(long projectId) {
        return dataSyncFacade.getDataSyncs(workspaceId)
            .stream()
            .filter(dataSyncDTO -> dataSyncDTO.projectId() == projectId)
            .findFirst()
            .orElseThrow()
            .dataSync()
            .getId();
    }

    private void assertProjectPublishRefused(long projectId, DataSyncErrorType errorType, String message) {
        assertThatThrownBy(() -> publishProject(projectId, "v1"))
            .isInstanceOf(ConfigurationException.class)
            .hasMessageContaining(message)
            .satisfies(exception -> assertThat(((ConfigurationException) exception).getErrorKey())
                .isEqualTo(errorType.getErrorKey()));

        assertThat(projectService.getProject(projectId)
            .getLastPublishedProjectVersion()).isNull();
    }

    /**
     * The body of {@code ProjectFacadeImpl.publishProject}, which this slice does not wire: the draft's workflow list
     * is snapshotted before {@code ProjectService.publishProject} runs the pre-listeners, and exactly that list is
     * duplicated into the new version.
     */
    private int publishProject(long projectId, String description) {
        int oldProjectVersion = projectService.getProject(projectId)
            .getLastProjectVersion();

        List<ProjectWorkflow> oldProjectWorkflows = projectWorkflowService.getProjectWorkflows(
            projectId, oldProjectVersion);

        int newProjectVersion = projectService.publishProject(projectId, description, false);

        for (ProjectWorkflow oldProjectWorkflow : oldProjectWorkflows) {
            String oldWorkflowId = oldProjectWorkflow.getWorkflowId();

            Workflow duplicatedWorkflow = workflowService.duplicateWorkflow(oldWorkflowId);

            oldProjectWorkflow.setProjectVersion(newProjectVersion);
            oldProjectWorkflow.setWorkflowId(duplicatedWorkflow.getId());

            projectWorkflowService.publishWorkflow(projectId, oldProjectVersion, oldWorkflowId, oldProjectWorkflow);
        }

        return newProjectVersion;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> firstTrigger(Map<String, Object> definition) {
        return ((List<Map<String, Object>>) definition.get("triggers")).get(0);
    }

    private Project saveOrdinaryProject(String name) {
        Project project = new Project();

        project.setName(name);
        project.setWorkspaceId(workspaceId);

        return projectService.create(project);
    }

    private String addOrdinaryWorkflow(Project project) {
        Workflow workflow = workflowService.create(
            JsonUtils.write(Map.of("label", "ordinary", "tasks", List.of())), Workflow.Format.JSON,
            Workflow.SourceType.JDBC);

        projectWorkflowService.addWorkflow(project.getId(), project.getLastProjectVersion(), workflow.getId());

        return workflow.getId();
    }

    private DataSyncElement elementOf(long id, Kind kind) {
        return dataSyncElementRepository.findAllByDataSyncIdOrderByKindAsc(id)
            .stream()
            .filter(element -> element.getKind() == kind)
            .findFirst()
            .orElseThrow();
    }

    private Map<String, Object> readDraft(long id) {
        String draftWorkflowId = dataSyncFacade.getDataSync(id)
            .draftWorkflowId();

        return JsonUtils.read(workflowService.getWorkflow(draftWorkflowId)
            .getDefinition(), new TypeReference<>() {});
    }

    private long readDraftDataSyncId(long id) {
        @SuppressWarnings("unchecked")
        Map<String, Object> metadata = (Map<String, Object>) readDraft(id).get("metadata");

        return ((Number) metadata.get(DataSyncWorkflowGenerator.METADATA_DATA_SYNC_ID)).longValue();
    }

    private String publishedWorkflowId(long projectId) {
        return projectWorkflowService.getProjectWorkflowIds(projectId, 1)
            .get(0);
    }
}
