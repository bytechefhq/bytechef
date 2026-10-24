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

package com.bytechef.automation.datasync.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.domain.ProjectWorkflowType;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.repository.ProjectRepository;
import com.bytechef.automation.configuration.repository.ProjectWorkflowRepository;
import com.bytechef.automation.configuration.repository.WorkspaceRepository;
import com.bytechef.automation.datasync.config.AutomationDataSyncIntTestConfiguration;
import com.bytechef.automation.datasync.domain.DataSync;
import com.bytechef.automation.datasync.domain.DataSync.TriggerType;
import com.bytechef.automation.datasync.domain.DataSyncElement;
import com.bytechef.automation.datasync.domain.DataSyncElement.Kind;
import com.bytechef.automation.datasync.repository.DataSyncElementRepository;
import com.bytechef.automation.datasync.repository.DataSyncRepository;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = AutomationDataSyncIntTestConfiguration.class,
    properties = "bytechef.workflow.repository.jdbc.enabled=true")
@Import(PostgreSQLContainerConfiguration.class)
class DataSyncServiceIntTest {

    @Autowired
    private DataSyncService dataSyncService;

    @Autowired
    private DataSyncElementService dataSyncElementService;

    @Autowired
    private DataSyncRepository dataSyncRepository;

    @Autowired
    private DataSyncElementRepository dataSyncElementRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectWorkflowRepository projectWorkflowRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @AfterEach
    void afterEach() {
        dataSyncElementRepository.deleteAll();
        dataSyncRepository.deleteAll();
        projectWorkflowRepository.deleteAll();
        projectRepository.deleteAll();
        workspaceRepository.deleteAll();
    }

    @Test
    void testCreateAndListByWorkspace() {
        Workspace workspace = workspaceRepository.save(new Workspace("ws"));
        Project project = projectRepository.save(newProject("project", workspace.getId()));

        DataSync dataSync = dataSyncService.create(newDataSync("crm-to-db", workspace.getId(), project));

        assertThat(dataSync.getId()).isNotNull();
        assertThat(dataSync.getTriggerType()).isEqualTo(TriggerType.MANUAL);
        assertThat(dataSyncService.getDataSyncs(workspace.getId())).extracting(DataSync::getName)
            .containsExactly("crm-to-db");
    }

    @Test
    void testTriggerRoundTrip() {
        Workspace workspace = workspaceRepository.save(new Workspace("ws"));
        Project project = projectRepository.save(newProject("project", workspace.getId()));

        DataSync dataSync = newDataSync("scheduled", workspace.getId(), project);

        dataSync.setTriggerType(TriggerType.SCHEDULE);
        dataSync.setTriggerParameters(Map.of("expression", "0 9 * * ?", "timezone", "UTC"));

        DataSync saved = dataSyncService.create(dataSync);

        DataSync reloaded = dataSyncService.getDataSync(saved.getId());

        assertThat(reloaded.getTriggerType()).isEqualTo(TriggerType.SCHEDULE);
        assertThat(reloaded.getTriggerParameters()
            .get("expression")).isEqualTo("0 9 * * ?");
    }

    @Test
    void testOneElementPerKind() {
        Workspace workspace = workspaceRepository.save(new Workspace("ws"));
        Project project = projectRepository.save(newProject("project", workspace.getId()));
        DataSync dataSync = dataSyncService.create(newDataSync("sync", workspace.getId(), project));

        dataSyncElementService.create(newElement(dataSync.getId(), Kind.SOURCE));

        assertThatThrownBy(() -> dataSyncElementService.create(newElement(dataSync.getId(), Kind.SOURCE)))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void testNameSlugIsEnforced() {
        DataSync dataSync = new DataSync();

        assertThatThrownBy(() -> dataSync.setName("Not A Slug")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testGetProjectIdResolvesThroughProjectWorkflow() {
        Workspace workspace = workspaceRepository.save(new Workspace("ws"));
        Project project = projectRepository.save(newProject("project", workspace.getId()));

        DataSync dataSync = dataSyncService.create(newDataSync("sync", workspace.getId(), project));

        projectWorkflowRepository.save(
            new ProjectWorkflow(
                project.getId(), 2, UUID.randomUUID()
                    .toString(),
                dataSync.getProjectWorkflowUuid(), ProjectWorkflowType.DATA_SYNC));

        assertThat(dataSyncService.getProjectId(dataSync)).isEqualTo(project.getId());
    }

    @Test
    void testGetProjectIdOfSyncWithoutProjectWorkflowFails() {
        Workspace workspace = workspaceRepository.save(new Workspace("ws"));

        DataSync orphanDataSync = new DataSync();

        orphanDataSync.setName("orphan");
        orphanDataSync.setTitle("orphan");
        orphanDataSync.setWorkspaceId(workspace.getId());
        orphanDataSync.setProjectWorkflowUuid(UUID.randomUUID());
        orphanDataSync.setUuid(UUID.randomUUID());
        orphanDataSync.setTriggerType(TriggerType.MANUAL);

        DataSync savedOrphanDataSync = dataSyncService.create(orphanDataSync);

        assertThatThrownBy(() -> dataSyncService.getProjectId(savedOrphanDataSync))
            .isInstanceOf(IllegalStateException.class);
        assertThat(dataSyncService.getProjectIds(List.of(savedOrphanDataSync))).isEmpty();
    }

    @Test
    void testGetProjectIdsBatchesDistinctProjects() {
        Workspace workspace = workspaceRepository.save(new Workspace("ws"));
        Project project = projectRepository.save(newProject("project", workspace.getId()));
        Project otherProject = projectRepository.save(newProject("other-project", workspace.getId()));

        DataSync firstDataSync = dataSyncService.create(newDataSync("first", workspace.getId(), project));
        DataSync secondDataSync = dataSyncService.create(newDataSync("second", workspace.getId(), project));
        DataSync otherDataSync = dataSyncService.create(newDataSync("other", workspace.getId(), otherProject));

        assertThat(dataSyncService.getProjectIds(List.of(firstDataSync, secondDataSync, otherDataSync)))
            .isEqualTo(
                Map.of(
                    firstDataSync.getId(), project.getId(), secondDataSync.getId(), project.getId(),
                    otherDataSync.getId(), otherProject.getId()));
        assertThat(dataSyncService.getProjectIds(List.of())).isEmpty();
    }

    @Test
    void testGetProjectDataSyncsReturnsOnlyThatProjectsSyncs() {
        Workspace workspace = workspaceRepository.save(new Workspace("ws"));
        Project project = projectRepository.save(newProject("project", workspace.getId()));
        Project otherProject = projectRepository.save(newProject("other-project", workspace.getId()));

        DataSync firstDataSync = dataSyncService.create(newDataSync("first", workspace.getId(), project));
        DataSync secondDataSync = dataSyncService.create(newDataSync("second", workspace.getId(), project));
        DataSync otherDataSync = dataSyncService.create(newDataSync("other", workspace.getId(), otherProject));

        assertThat(dataSyncService.getProjectDataSyncs(project.getId()))
            .extracting(DataSync::getId)
            .containsExactly(firstDataSync.getId(), secondDataSync.getId());
        assertThat(dataSyncService.getProjectDataSyncs(otherProject.getId()))
            .extracting(DataSync::getId)
            .containsExactly(otherDataSync.getId());
    }

    @Test
    void testProjectWorkflowUuidIsUnique() {
        Workspace workspace = workspaceRepository.save(new Workspace("ws"));
        Project project = projectRepository.save(newProject("project", workspace.getId()));

        DataSync dataSync = dataSyncService.create(newDataSync("first", workspace.getId(), project));

        DataSync duplicateDataSync = newDataSync("second", workspace.getId(), project);

        duplicateDataSync.setProjectWorkflowUuid(dataSync.getProjectWorkflowUuid());

        assertThatThrownBy(() -> dataSyncService.create(duplicateDataSync))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    private static Project newProject(String name, long workspaceId) {
        Project project = new Project();

        project.setName(name);
        project.setWorkspaceId(workspaceId);

        return project;
    }

    private DataSync newDataSync(String name, long workspaceId, Project project) {
        UUID projectWorkflowUuid = UUID.randomUUID();

        projectWorkflowRepository.save(
            new ProjectWorkflow(
                project.getId(), 1, UUID.randomUUID()
                    .toString(),
                projectWorkflowUuid, ProjectWorkflowType.DATA_SYNC));

        DataSync dataSync = new DataSync();

        dataSync.setName(name);
        dataSync.setTitle(name);
        dataSync.setWorkspaceId(workspaceId);
        dataSync.setProjectWorkflowUuid(projectWorkflowUuid);
        dataSync.setUuid(UUID.randomUUID());
        dataSync.setTriggerType(TriggerType.MANUAL);

        return dataSync;
    }

    private static DataSyncElement newElement(long dataSyncId, Kind kind) {
        DataSyncElement element = new DataSyncElement(dataSyncId, kind);

        element.setComponentName("csvFile");
        element.setComponentVersion(1);
        element.setOperationName("read");

        return element;
    }
}
