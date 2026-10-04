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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.dto.ProjectDeploymentDTO;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.platform.configuration.domain.Environment;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * @author Ivica Cardic
 */
class DeleteProjectDeploymentToolCallbackTest {

    private static final long WORKSPACE_ID = 99L;

    private final JsonMapper jsonMapper = new JsonMapper();

    @Test
    void testDeleteProjectDeploymentSuccess() throws Exception {
        ProjectDeploymentFacade facade = workspaceFacade(11L);

        DeleteProjectDeploymentToolCallback callback = new DeleteProjectDeploymentToolCallback(facade);

        String result = callback.call("{\"projectDeploymentId\":\"11\"}", toolContext());

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.get("projectDeploymentId")
            .asLong()).isEqualTo(11L);
        assertThat(node.get("deleted")
            .asBoolean()).isTrue();

        verify(facade).deleteProjectDeployment(11L);
    }

    @Test
    void testRefusesDeploymentOutsideTheCurrentWorkspace() throws Exception {
        ProjectDeploymentFacade facade = workspaceFacade(11L);

        DeleteProjectDeploymentToolCallback callback = new DeleteProjectDeploymentToolCallback(facade);

        String result = callback.call("{\"projectDeploymentId\":\"12\"}", toolContext());

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.get("error")
            .asText()).contains("12", "not found in the current workspace");

        verify(facade, never()).deleteProjectDeployment(anyLong());
    }

    @Test
    void testRefusesWithoutWorkspaceContext() throws Exception {
        ProjectDeploymentFacade facade = workspaceFacade(11L);

        DeleteProjectDeploymentToolCallback callback = new DeleteProjectDeploymentToolCallback(facade);

        String result = callback.call("{\"projectDeploymentId\":\"11\"}");

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.get("error")
            .asText()).contains("Workspace context unavailable");

        verify(facade, never()).deleteProjectDeployment(anyLong());
    }

    @Test
    void testReportsAnUnknownDeploymentIdAsNotFound() throws Exception {
        ProjectDeploymentFacade facade = workspaceFacade(404L);

        doThrow(new NoSuchElementException("No value present")).when(facade)
            .deleteProjectDeployment(404L);

        DeleteProjectDeploymentToolCallback callback = new DeleteProjectDeploymentToolCallback(facade);

        String result = callback.call("{\"projectDeploymentId\":\"404\"}", toolContext());

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.has("error")).isTrue();
        assertThat(node.get("error")
            .asText()).contains("404", "not found");
    }

    @Test
    void testRejectsNonNumericId() throws Exception {
        DeleteProjectDeploymentToolCallback callback = new DeleteProjectDeploymentToolCallback(
            mock(ProjectDeploymentFacade.class));

        String result = callback.call("{\"projectDeploymentId\":\"foo\"}", toolContext());

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.has("error")).isTrue();
        assertThat(node.get("error")
            .asText()).contains("numeric");
    }

    static ToolContext toolContext() {
        return new ToolContext(Map.of(AutomationToolInvocationContext.TOOL_CONTEXT_WORKSPACE_ID_KEY, WORKSPACE_ID));
    }

    static ProjectDeploymentFacade workspaceFacade(long projectDeploymentId) {
        ProjectDeploymentFacade facade = mock(ProjectDeploymentFacade.class);

        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setId(projectDeploymentId);
        projectDeployment.setProjectId(7L);
        projectDeployment.setEnvironment(Environment.DEVELOPMENT);
        projectDeployment.setVersion(0);

        when(facade.getWorkspaceProjectDeployments(WORKSPACE_ID, null, null, null, false))
            .thenReturn(List.of(new ProjectDeploymentDTO(projectDeployment)));

        return facade;
    }
}
