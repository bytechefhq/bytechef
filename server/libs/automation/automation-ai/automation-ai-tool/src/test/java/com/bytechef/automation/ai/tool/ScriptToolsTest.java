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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.automation.ai.tool.model.WorkflowInfo;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.dto.ProjectWorkflowDTO;
import com.bytechef.automation.configuration.facade.ProjectWorkflowFacade;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.exception.ExecutionException;
import java.util.Optional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ToolContext;

/**
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class ScriptToolsTest {

    private static final long WORKSPACE_ID = 1L;

    @Mock
    private ProjectService projectService;

    @Mock
    private ProjectWorkflowFacade projectWorkflowFacade;

    @Mock
    private ProjectWorkflowService projectWorkflowService;

    @Mock
    private ToolContext toolContext;

    @Mock
    private WorkspaceScopeResolver workspaceScopeResolver;

    @Nested
    class WorkspaceScopeTest {

        @Test
        void testUpdateScriptComponentCodeRejectsAWorkflowOfAnotherWorkspace() {
            mockWorkflowInWorkspace(2L);

            ScriptTools scriptTools = newTools();

            assertThatCode(
                () -> scriptTools.updateScriptComponentCode("wf-uuid-1", "code", "script_1", null, toolContext))
                    .isInstanceOf(ExecutionException.class)
                    .hasMessageContaining("not found in workspace " + WORKSPACE_ID);

            verify(projectWorkflowFacade, never()).getProjectWorkflow(anyString());
        }

        @Test
        void testUpdateScriptComponentCodeReadsAWorkflowOfTheWorkspace() {
            mockWorkflowInWorkspace(WORKSPACE_ID);

            Workflow workflow = new Workflow("wf-uuid-1", "{\"tasks\": []}", Workflow.Format.JSON);

            when(projectWorkflowFacade.getProjectWorkflow("wf-uuid-1"))
                .thenReturn(new ProjectWorkflowDTO(workflow, new ProjectWorkflow(55L), false));

            WorkflowInfo workflowInfo = newTools().updateScriptComponentCode(
                "wf-uuid-1", "code", "script_1", null, toolContext);

            assertThat(workflowInfo).isNull();
            verify(projectWorkflowFacade).getProjectWorkflow("wf-uuid-1");
        }
    }

    private void mockWorkflowInWorkspace(long workspaceId) {
        when(workspaceScopeResolver.resolveWorkspace(null, toolContext))
            .thenReturn(new WorkspaceScopeResolver.Resolved(WORKSPACE_ID, 0L));
        when(projectWorkflowService.getWorkflowProjectWorkflow("wf-uuid-1"))
            .thenReturn(new ProjectWorkflow(7L, 1, "wf-uuid-1"));
        when(projectService.fetchProject(7L)).thenReturn(
            Optional.of(
                Project.builder()
                    .id(7L)
                    .name("project")
                    .workspaceId(workspaceId)
                    .build()));
    }

    private ScriptTools newTools() {
        return new ScriptTools(projectService, projectWorkflowFacade, projectWorkflowService, workspaceScopeResolver);
    }
}
