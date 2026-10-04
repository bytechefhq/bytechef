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

import static com.bytechef.automation.ai.tool.DeleteProjectDeploymentToolCallbackTest.toolContext;
import static com.bytechef.automation.ai.tool.DeleteProjectDeploymentToolCallbackTest.workspaceFacade;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.dto.ProjectDeploymentDTO;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.platform.configuration.domain.Environment;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * @author Ivica Cardic
 */
class RollbackProjectDeploymentToolCallbackTest {

    private final JsonMapper jsonMapper = new JsonMapper();

    @Test
    void testRollbackChangesProjectVersionPreservingEnabledState() throws Exception {
        ProjectDeploymentFacade facade = workspaceFacade(7L);

        ProjectDeploymentDTO existing = ProjectDeploymentDTO.builder()
            .id(7L)
            .projectId(42L)
            .projectVersion(3)
            .environment(Environment.PRODUCTION)
            .enabled(true)
            .name("prod-deploy")
            .version(5)
            .build();

        when(facade.getProjectDeployment(7L)).thenReturn(existing);

        RollbackProjectDeploymentToolCallback callback = new RollbackProjectDeploymentToolCallback(facade);

        String result = callback.call("{\"projectDeploymentId\":\"7\",\"projectVersion\":2}", toolContext());

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.get("projectDeploymentId")
            .asLong()).isEqualTo(7L);
        assertThat(node.get("projectVersion")
            .asInt()).isEqualTo(2);
        assertThat(node.get("previousProjectVersion")
            .asInt()).isEqualTo(3);

        ArgumentCaptor<ProjectDeploymentDTO> captor = ArgumentCaptor.forClass(ProjectDeploymentDTO.class);

        verify(facade).updateProjectDeployment(captor.capture());

        ProjectDeploymentDTO captured = captor.getValue();

        assertThat(captured.id()).isEqualTo(7L);
        assertThat(captured.projectVersion()).isEqualTo(2);
        assertThat(captured.version()).isEqualTo(5);
        assertThat(captured.enabled()).isTrue();
        assertThat(captured.environment()).isEqualTo(Environment.PRODUCTION);
    }

    @Test
    void testRejectsRollbackToCurrentVersion() throws Exception {
        ProjectDeploymentFacade facade = workspaceFacade(7L);

        ProjectDeploymentDTO existing = ProjectDeploymentDTO.builder()
            .id(7L)
            .projectId(42L)
            .projectVersion(3)
            .environment(Environment.PRODUCTION)
            .enabled(true)
            .build();

        when(facade.getProjectDeployment(7L)).thenReturn(existing);

        RollbackProjectDeploymentToolCallback callback = new RollbackProjectDeploymentToolCallback(facade);

        String result = callback.call("{\"projectDeploymentId\":\"7\",\"projectVersion\":3}", toolContext());

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.has("error")).isTrue();
        assertThat(node.get("error")
            .asText()).contains("already on projectVersion=3");

        verify(facade, never()).updateProjectDeployment(any(ProjectDeploymentDTO.class));
    }

    @Test
    void testRejectsMissingId() throws Exception {
        RollbackProjectDeploymentToolCallback callback = new RollbackProjectDeploymentToolCallback(
            mock(ProjectDeploymentFacade.class));

        String result = callback.call("{\"projectVersion\":2}");

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.has("error")).isTrue();
        assertThat(node.get("error")
            .asText()).contains("projectDeploymentId");
    }

    @Test
    void testRefusesDeploymentOutsideTheCurrentWorkspace() throws Exception {
        ProjectDeploymentFacade facade = workspaceFacade(7L);

        RollbackProjectDeploymentToolCallback callback = new RollbackProjectDeploymentToolCallback(facade);

        String result = callback.call("{\"projectDeploymentId\":\"8\",\"projectVersion\":2}", toolContext());

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.get("error")
            .asText()).contains("8", "not found in the current workspace");

        verify(facade, never()).getProjectDeployment(anyLong());
        verify(facade, never()).updateProjectDeployment(any(ProjectDeploymentDTO.class));
    }

    @Test
    void testRefusesWithoutWorkspaceContext() throws Exception {
        ProjectDeploymentFacade facade = workspaceFacade(7L);

        RollbackProjectDeploymentToolCallback callback = new RollbackProjectDeploymentToolCallback(facade);

        String result = callback.call("{\"projectDeploymentId\":\"7\",\"projectVersion\":2}");

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.get("error")
            .asText()).contains("Workspace context unavailable");

        verify(facade, never()).getProjectDeployment(anyLong());
        verify(facade, never()).updateProjectDeployment(any(ProjectDeploymentDTO.class));
    }
}
