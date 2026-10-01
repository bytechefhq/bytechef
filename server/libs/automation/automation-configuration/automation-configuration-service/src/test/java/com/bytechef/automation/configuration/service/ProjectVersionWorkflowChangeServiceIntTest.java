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

package com.bytechef.automation.configuration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.repository.WorkflowCrudRepository;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.config.ProjectIntTestConfiguration;
import com.bytechef.automation.configuration.config.ProjectIntTestConfigurationSharedMocks;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectVersion;
import com.bytechef.automation.configuration.domain.ProjectWorkflowType;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.repository.ProjectRepository;
import com.bytechef.automation.configuration.repository.ProjectWorkflowRepository;
import com.bytechef.automation.configuration.repository.WorkspaceRepository;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.configuration.dto.VersionWorkflowChange;
import com.bytechef.platform.configuration.dto.VersionWorkflowChangeSummary;
import com.bytechef.platform.configuration.dto.VersionWorkflowChanges;
import com.bytechef.platform.configuration.dto.WorkflowChangeType;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.apache.commons.lang3.Validate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = ProjectIntTestConfiguration.class, properties = "bytechef.workflow.repository.jdbc.enabled=true")
@Import(PostgreSQLContainerConfiguration.class)
@ProjectIntTestConfigurationSharedMocks
class ProjectVersionWorkflowChangeServiceIntTest {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private ProjectVersionWorkflowChangeService projectVersionWorkflowChangeService;

    @Autowired
    private ProjectWorkflowRepository projectWorkflowRepository;

    @Autowired
    private ProjectWorkflowService projectWorkflowService;

    @Autowired
    private WorkflowCrudRepository workflowRepository;

    @Autowired
    private WorkflowService workflowService;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    private long projectId;

    @BeforeEach
    void beforeEach() {
        Workspace workspace = workspaceRepository.save(new Workspace("test"));

        Project project = projectService.create(
            Project.builder()
                .name("name")
                .workspaceId(workspace.getId())
                .build());

        projectId = Validate.notNull(project.getId(), "id");
    }

    @AfterEach
    void afterEach() {
        projectWorkflowRepository.deleteAll();
        projectRepository.deleteAll();

        for (Workflow workflow : workflowRepository.findAll()) {
            workflowRepository.deleteById(workflow.getId());
        }

        workspaceRepository.deleteAll();
    }

    @Test
    void testBaseVersionIsPreviousExistingVersion() {
        createVersions(3);

        VersionWorkflowChanges changes = projectVersionWorkflowChangeService.getVersionWorkflowChanges(projectId, 3);

        assertThat(changes.baseVersion()).isEqualTo(2);
        assertThat(changes.version()).isEqualTo(3);
    }

    @Test
    void testBaseVersionSkipsDeletedVersions() {
        createVersions(3);

        projectService.removeProjectVersion(projectId, 2);

        assertThat(projectService.getProject(projectId)
            .getProjectVersions())
                .extracting(ProjectVersion::getVersion)
                .containsExactlyInAnyOrder(1, 3);

        VersionWorkflowChanges changes = projectVersionWorkflowChangeService.getVersionWorkflowChanges(projectId, 3);

        assertThat(changes.baseVersion()).isEqualTo(1);
    }

    @Test
    void testFirstVersionHasNoBaseAndAllWorkflowsAdded() {
        createVersions(1);
        addWorkflow(1, "a", "A", false);
        addWorkflow(1, "b", "B", false);

        VersionWorkflowChanges changes = projectVersionWorkflowChangeService.getVersionWorkflowChanges(projectId, 1);

        assertThat(changes.baseVersion()).isNull();
        assertThat(changes.workflows())
            .extracting(VersionWorkflowChangeSummary::changeType)
            .containsExactly(WorkflowChangeType.ADDED, WorkflowChangeType.ADDED);
    }

    @Test
    void testPairsWorkflowsByUuidAndOmitsUnchanged() {
        createVersions(2);
        addWorkflow(1, "a", "A", false);
        addWorkflow(1, "b", "B", false);
        addWorkflow(2, "a", "A", false);
        addWorkflow(2, "b", "B", true);

        VersionWorkflowChanges changes = projectVersionWorkflowChangeService.getVersionWorkflowChanges(projectId, 2);

        assertThat(changes.workflows()).hasSize(1);
        assertThat(changes.workflows()
            .get(0)
            .workflowUuid()).isEqualTo(uuidString("b"));
        assertThat(changes.workflows()
            .get(0)
            .changeType()).isEqualTo(WorkflowChangeType.MODIFIED);
    }

    @Test
    void testOrdersSummariesByChangeTypeThenLabel() {
        createVersions(2);
        addWorkflow(1, "m2", "zeta", false);
        addWorkflow(1, "m1", "Beta", false);
        addWorkflow(1, "r", "Removed", false);
        addWorkflow(2, "m2", "zeta", true);
        addWorkflow(2, "m1", "Beta", true);
        addWorkflow(2, "a2", "alpha", false);
        addWorkflow(2, "a1", "Able", false);

        VersionWorkflowChanges changes = projectVersionWorkflowChangeService.getVersionWorkflowChanges(projectId, 2);

        assertThat(changes.workflows())
            .extracting(VersionWorkflowChangeSummary::workflowUuid)
            .containsExactly(
                uuidString("a1"), uuidString("a2"), uuidString("m1"), uuidString("m2"), uuidString("r"));
    }

    @Test
    void testUnknownVersionThrows() {
        createVersions(2);

        assertThatThrownBy(() -> projectVersionWorkflowChangeService.getVersionWorkflowChanges(projectId, 9))
            .isInstanceOf(ConfigurationException.class);
    }

    @Test
    void testChangeForRemovedWorkflowHasOnlyOldDefinition() {
        createVersions(2);
        addWorkflow(1, "a", "A", false);

        VersionWorkflowChange change = projectVersionWorkflowChangeService.getVersionWorkflowChange(
            projectId, 2, uuidString("a"));

        assertThat(change.changeType()).isEqualTo(WorkflowChangeType.REMOVED);
        assertThat(change.baseVersion()).isEqualTo(1);
        assertThat(change.oldDefinition()).isNotNull();
        assertThat(change.newDefinition()).isNull();
    }

    @Test
    void testUnknownWorkflowUuidThrows() {
        createVersions(2);
        addWorkflow(1, "a", "A", false);
        addWorkflow(2, "a", "A", false);

        String missingWorkflowUuid = uuidString("missing");

        assertThatThrownBy(
            () -> projectVersionWorkflowChangeService.getVersionWorkflowChange(projectId, 2, missingWorkflowUuid))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Workflow uuid=" + missingWorkflowUuid + " not found in project version v=2");
    }

    private void addWorkflow(int projectVersion, String key, String label, boolean withDescription) {
        String description = withDescription ? ",\"description\":\"x\"" : "";

        Workflow workflow = workflowService.create(
            "{\"label\":\"" + label + "\",\"tasks\":[]" + description + "}", Workflow.Format.JSON,
            Workflow.SourceType.JDBC);

        projectWorkflowService.addWorkflow(
            projectId, projectVersion, workflow.getId(), uuid(key), ProjectWorkflowType.WORKFLOW);
    }

    private void createVersions(int lastVersion) {
        for (int version = 1; version < lastVersion; version++) {
            projectService.publishProject(projectId, "v" + version, false);
        }
    }

    private static UUID uuid(String key) {
        return UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8));
    }

    private static String uuidString(String key) {
        return String.valueOf(uuid(key));
    }
}
