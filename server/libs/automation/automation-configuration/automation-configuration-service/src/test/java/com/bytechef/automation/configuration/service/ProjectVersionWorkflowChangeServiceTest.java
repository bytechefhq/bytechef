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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectVersion;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.configuration.dto.VersionWorkflowChange;
import com.bytechef.platform.configuration.dto.VersionWorkflowChangeSummary;
import com.bytechef.platform.configuration.dto.VersionWorkflowChanges;
import com.bytechef.platform.configuration.dto.WorkflowChangeType;
import com.bytechef.platform.configuration.workflow.diff.WorkflowDefinitionDiffServiceImpl;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * @author Ivica Cardic
 */
@ExtendWith({
    MockitoExtension.class, ObjectMapperSetupExtension.class
})
@MockitoSettings(strictness = Strictness.LENIENT)
class ProjectVersionWorkflowChangeServiceTest {

    private static final long PROJECT_ID = 1L;

    @Mock
    private ProjectService projectService;

    @Mock
    private ProjectWorkflowService projectWorkflowService;

    @Mock
    private WorkflowService workflowService;

    @Mock
    private Project project;

    private ProjectVersionWorkflowChangeServiceImpl projectVersionWorkflowChangeService;

    @BeforeEach
    void beforeEach() {
        projectVersionWorkflowChangeService = new ProjectVersionWorkflowChangeServiceImpl(
            projectService, projectWorkflowService, new WorkflowDefinitionDiffServiceImpl(), workflowService);

        when(projectService.getProject(PROJECT_ID)).thenReturn(project);
    }

    @Test
    void testBaseVersionIsPreviousExistingVersion() {
        mockVersions(1, 2, 3);

        VersionWorkflowChanges changes = projectVersionWorkflowChangeService.getVersionWorkflowChanges(PROJECT_ID, 3);

        assertThat(changes.baseVersion()).isEqualTo(2);
        assertThat(changes.version()).isEqualTo(3);
    }

    @Test
    void testBaseVersionSkipsDeletedVersions() {
        mockVersions(1, 3);

        VersionWorkflowChanges changes = projectVersionWorkflowChangeService.getVersionWorkflowChanges(PROJECT_ID, 3);

        assertThat(changes.baseVersion()).isEqualTo(1);
    }

    @Test
    void testFirstVersionHasNoBaseAndAllWorkflowsAdded() {
        mockVersions(1);
        mockWorkflows(1, workflow("uuid-a", "wf-a", "A", "{}"), workflow("uuid-b", "wf-b", "B", "{}"));

        VersionWorkflowChanges changes = projectVersionWorkflowChangeService.getVersionWorkflowChanges(PROJECT_ID, 1);

        assertThat(changes.baseVersion()).isNull();
        assertThat(changes.workflows())
            .extracting(VersionWorkflowChangeSummary::changeType)
            .containsExactly(WorkflowChangeType.ADDED, WorkflowChangeType.ADDED);
    }

    @Test
    void testPairsWorkflowsByUuidAndOmitsUnchanged() {
        mockVersions(1, 2);
        mockWorkflows(
            1, workflow("uuid-a", "wf-a1", "A", "{}"), workflow("uuid-b", "wf-b1", "B", "{}"));
        mockWorkflows(
            2, workflow("uuid-a", "wf-a2", "A", "{}"), workflow("uuid-b", "wf-b2", "B", "{\"description\":\"x\"}"));

        VersionWorkflowChanges changes = projectVersionWorkflowChangeService.getVersionWorkflowChanges(PROJECT_ID, 2);

        assertThat(changes.workflows()).hasSize(1);
        assertThat(changes.workflows()
            .get(0)
            .workflowUuid()).isEqualTo("uuid-b");
        assertThat(changes.workflows()
            .get(0)
            .changeType()).isEqualTo(WorkflowChangeType.MODIFIED);
    }

    @Test
    void testOrdersSummariesByChangeTypeThenLabel() {
        mockVersions(1, 2);
        mockWorkflows(
            1, workflow("uuid-m2", "wf-m2a", "zeta", "{}"), workflow("uuid-m1", "wf-m1a", "Beta", "{}"),
            workflow("uuid-r", "wf-ra", "Removed", "{}"));
        mockWorkflows(
            2, workflow("uuid-m2", "wf-m2b", "zeta", "{\"description\":\"x\"}"),
            workflow("uuid-m1", "wf-m1b", "Beta", "{\"description\":\"x\"}"),
            workflow("uuid-a2", "wf-a2", "alpha", "{}"), workflow("uuid-a1", "wf-a1", "Able", "{}"));

        VersionWorkflowChanges changes = projectVersionWorkflowChangeService.getVersionWorkflowChanges(PROJECT_ID, 2);

        assertThat(changes.workflows())
            .extracting(VersionWorkflowChangeSummary::workflowUuid)
            .containsExactly("uuid-a1", "uuid-a2", "uuid-m1", "uuid-m2", "uuid-r");
    }

    @Test
    void testUnknownVersionThrows() {
        mockVersions(1, 2);

        assertThatThrownBy(() -> projectVersionWorkflowChangeService.getVersionWorkflowChanges(PROJECT_ID, 9))
            .isInstanceOf(ConfigurationException.class);
    }

    @Test
    void testChangeForRemovedWorkflowHasOnlyOldDefinition() {
        mockVersions(1, 2);
        mockWorkflows(1, workflow("uuid-a", "wf-a1", "A", "{}"));
        mockWorkflows(2);

        VersionWorkflowChange change = projectVersionWorkflowChangeService.getVersionWorkflowChange(
            PROJECT_ID, 2, "uuid-a");

        assertThat(change.changeType()).isEqualTo(WorkflowChangeType.REMOVED);
        assertThat(change.baseVersion()).isEqualTo(1);
        assertThat(change.oldDefinition()).isNotNull();
        assertThat(change.newDefinition()).isNull();
    }

    @Test
    void testUnknownWorkflowUuidThrows() {
        mockVersions(1, 2);
        mockWorkflows(1, workflow("uuid-a", "wf-a1", "A", "{}"));
        mockWorkflows(2, workflow("uuid-a", "wf-a2", "A", "{}"));

        assertThatThrownBy(
            () -> projectVersionWorkflowChangeService.getVersionWorkflowChange(PROJECT_ID, 2, "uuid-missing"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Workflow uuid=uuid-missing not found in project version v=2");
    }

    private void mockVersions(int... versions) {
        List<ProjectVersion> projectVersions = new ArrayList<>();

        for (int version : versions) {
            ProjectVersion projectVersion = mock(ProjectVersion.class);

            when(projectVersion.getVersion()).thenReturn(version);
            when(project.fetchProjectVersion(version)).thenReturn(Optional.of(projectVersion));

            projectVersions.add(projectVersion);
        }

        when(project.getProjectVersions()).thenReturn(projectVersions);
        when(project.fetchProjectVersion(9)).thenReturn(Optional.empty());
    }

    private void mockWorkflows(int projectVersion, WorkflowFixture... fixtures) {
        List<ProjectWorkflow> projectWorkflows = new ArrayList<>();

        for (WorkflowFixture fixture : fixtures) {
            ProjectWorkflow projectWorkflow = mock(ProjectWorkflow.class);

            when(projectWorkflow.getUuidAsString()).thenReturn(fixture.uuid());
            when(projectWorkflow.getWorkflowId()).thenReturn(fixture.workflow()
                .getId());
            when(workflowService.getWorkflow(fixture.workflow()
                .getId())).thenReturn(fixture.workflow());

            projectWorkflows.add(projectWorkflow);
        }

        when(projectWorkflowService.getProjectWorkflows(PROJECT_ID, projectVersion)).thenReturn(projectWorkflows);
    }

    private WorkflowFixture workflow(String uuid, String workflowId, String label, String extraJson) {
        String description = extraJson.contains("description") ? ",\"description\":\"x\"" : "";

        return new WorkflowFixture(
            uuid,
            new Workflow(
                workflowId, "{\"label\":\"" + label + "\",\"tasks\":[]" + description + "}", Workflow.Format.JSON));
    }

    private record WorkflowFixture(String uuid, Workflow workflow) {
    }
}
