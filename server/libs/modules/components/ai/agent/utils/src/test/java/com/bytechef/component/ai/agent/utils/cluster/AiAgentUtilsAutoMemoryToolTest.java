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

package com.bytechef.component.ai.agent.utils.cluster;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryService;
import com.bytechef.platform.component.definition.ActionContextAware;
import com.bytechef.platform.component.definition.ClusterElementContextAware;
import com.bytechef.platform.component.definition.ai.agent.ToolCallbackProviderFunction;
import com.bytechef.platform.constant.PlatformType;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

/**
 * @author Ivica Cardic
 */
class AiAgentUtilsAutoMemoryToolTest {

    private final AiAutoMemoryService aiAutoMemoryService = mock(AiAutoMemoryService.class);
    private final Parameters connectionParameters = mock(Parameters.class);
    private final Parameters inputParameters = mock(Parameters.class);
    private final ProjectDeploymentService projectDeploymentService = mock(ProjectDeploymentService.class);
    private final ProjectService projectService = mock(ProjectService.class);

    private final ToolCallbackProviderFunction toolCallbackProviderFunction = new AiAgentUtilsAutoMemoryTool(
        aiAutoMemoryService, projectDeploymentService, projectService).clusterElementDefinition.getElement();

    @Test
    void testApplyWithoutJobContextReturnsNoTools() throws Exception {
        ClusterElementContextAware clusterElementContext = mock(ClusterElementContextAware.class);

        ToolCallback[] toolCallbacks = toolCallbackProviderFunction
            .apply(inputParameters, connectionParameters, clusterElementContext)
            .getToolCallbacks();

        assertThat(toolCallbacks).isEmpty();

        verifyNoInteractions(aiAutoMemoryService, projectDeploymentService, projectService);
    }

    @Test
    void testApplyForProjectDeploymentExposesMemoryTools() throws Exception {
        ActionContextAware actionContext = mock(ActionContextAware.class);
        Project project = new Project();
        ProjectDeployment projectDeployment = new ProjectDeployment();

        project.setWorkspaceId(7L);
        projectDeployment.setProjectId(3L);

        when(actionContext.getPlatformType()).thenReturn(PlatformType.AUTOMATION);
        when(actionContext.getJobPrincipalId()).thenReturn(11L);
        when(actionContext.getEnvironmentId()).thenReturn(1L);
        when(projectDeploymentService.getProjectDeployment(11L)).thenReturn(projectDeployment);
        when(projectService.getProject(3L)).thenReturn(project);

        ToolCallback[] toolCallbacks = toolCallbackProviderFunction
            .apply(inputParameters, connectionParameters, actionContext)
            .getToolCallbacks();

        assertThat(Arrays.stream(toolCallbacks)
            .map(ToolCallback::getToolDefinition)
            .map(ToolDefinition::name))
                .containsExactlyInAnyOrder(
                    "MemoryCreate", "MemoryDelete", "MemoryInsert", "MemoryRename", "MemoryStrReplace", "MemoryView");
    }
}
