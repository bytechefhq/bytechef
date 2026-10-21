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

package com.bytechef.automation.ai.tool;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectVersion;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;

/**
 * @author Ivica Cardic
 */
class AppCallableDraftGuardTest {

    static final String WORKFLOW_UUID = "11111111-1111-4111-8111-111111111111";

    private AppCallableDraftGuard appCallableDraftGuard;
    private ProjectService projectService;
    private ProjectWorkflowService projectWorkflowService;

    @BeforeEach
    void beforeEach() {
        projectService = mock(ProjectService.class);
        projectWorkflowService = mock(ProjectWorkflowService.class);

        appCallableDraftGuard = new AppCallableDraftGuard(projectService, projectWorkflowService);
    }

    @Test
    void testWithoutTheFlagNothingIsLookedUp() {
        appCallableDraftGuard.checkDraft(new ToolContext(Map.of("other", true)), "wf-published");
        appCallableDraftGuard.checkDraft(null, "wf-published");

        verifyNoInteractions(projectService, projectWorkflowService);
    }

    @Test
    void testADraftWorkflowPasses() {
        stubPublishedAndDraft(projectService, projectWorkflowService);

        assertThatCode(() -> appCallableDraftGuard.checkDraft(appCallableContext(), "wf-draft"))
            .doesNotThrowAnyException();
    }

    @Test
    void testAPublishedWorkflowIsRefusedNamingTheDraft() {
        stubPublishedAndDraft(projectService, projectWorkflowService);

        assertThatThrownBy(() -> appCallableDraftGuard.checkDraft(appCallableContext(), "wf-published"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("wf-published")
            .hasMessageContaining("edit the DRAFT workflow wf-draft");
    }

    static ToolContext appCallableContext() {
        return new ToolContext(Map.of(AppCallableDraftGuard.APP_CALLABLE_KEY, true));
    }

    static void stubPublishedAndDraft(ProjectService projectService, ProjectWorkflowService projectWorkflowService) {
        Project project = new Project();

        project.setId(7L);
        project.setProjectVersions(
            List.of(
                new ProjectVersion(1, ProjectVersion.Status.PUBLISHED.ordinal(), Instant.now(), "v1"),
                new ProjectVersion(2)));

        lenient().when(projectService.getProject(7L))
            .thenReturn(project);
        lenient().when(projectWorkflowService.getWorkflowProjectWorkflow("wf-published"))
            .thenReturn(new ProjectWorkflow(7L, 1, "wf-published", UUID.fromString(WORKFLOW_UUID)));
        lenient().when(projectWorkflowService.getWorkflowProjectWorkflow("wf-draft"))
            .thenReturn(new ProjectWorkflow(7L, 2, "wf-draft", UUID.fromString(WORKFLOW_UUID)));
        lenient().when(projectWorkflowService.getLastWorkflowId(WORKFLOW_UUID))
            .thenReturn("wf-draft");
    }
}
