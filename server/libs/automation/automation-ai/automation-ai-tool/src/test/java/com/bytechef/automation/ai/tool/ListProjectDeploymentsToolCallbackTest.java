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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.dto.ProjectDeploymentDTO;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.platform.configuration.domain.Environment;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 *
 * @author Ivica Cardic
 */
class ListProjectDeploymentsToolCallbackTest {

    private final JsonMapper jsonMapper = new JsonMapper();

    @Test
    void testListsDeploymentsScopedByWorkspaceAndEnvironment() throws Exception {
        ProjectDeploymentFacade facade = mock(ProjectDeploymentFacade.class);

        ProjectDeployment domain = new ProjectDeployment();

        domain.setId(11L);
        domain.setName("Prod release");
        domain.setProjectId(7L);
        domain.setProjectVersion(2);
        domain.setEnabled(true);
        domain.setEnvironment(Environment.STAGING);
        domain.setVersion(0);

        ProjectDeploymentDTO dto = new ProjectDeploymentDTO(domain);

        when(facade.getWorkspaceProjectDeployments(eq(99L), eq(5L), eq(null), eq(null), eq(false)))
            .thenReturn(List.of(dto));

        ListProjectDeploymentsToolCallback callback = new ListProjectDeploymentsToolCallback(facade);

        ToolContext toolContext = new ToolContext(Map.of(
            AutomationToolInvocationContext.TOOL_CONTEXT_WORKSPACE_ID_KEY, 99L,
            AutomationToolInvocationContext.TOOL_CONTEXT_ENVIRONMENT_ID_KEY, 5L));

        String result = callback.call("{}", toolContext);

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.isArray()).isTrue();
        assertThat(node.size()).isEqualTo(1);
        assertThat(node.get(0)
            .get("id")
            .asLong()).isEqualTo(11L);
        assertThat(node.get(0)
            .get("projectId")
            .asLong()).isEqualTo(7L);
        assertThat(node.get(0)
            .get("environment")
            .asText()).isEqualTo("STAGING");
        assertThat(node.get(0)
            .get("enabled")
            .asBoolean()).isTrue();
    }

    @Test
    void testProjectIdFilterIsForwardedToFacade() throws Exception {
        ProjectDeploymentFacade facade = mock(ProjectDeploymentFacade.class);

        when(facade.getWorkspaceProjectDeployments(eq(99L), eq(null), eq(7L), eq(null), eq(false)))
            .thenReturn(List.of());

        ListProjectDeploymentsToolCallback callback = new ListProjectDeploymentsToolCallback(facade);

        ToolContext toolContext = new ToolContext(
            Map.of(AutomationToolInvocationContext.TOOL_CONTEXT_WORKSPACE_ID_KEY, 99L));

        callback.call("{\"projectId\":\"7\"}", toolContext);

        // Forwarded to facade as Long 7, not null — the filter narrows the result set even when other params
        // (environmentId, tagId) stay unset.
        verify(facade).getWorkspaceProjectDeployments(eq(99L), eq(null), eq(7L), eq(null), eq(false));
    }

    @Test
    void testRejectsMissingWorkspaceContext() throws Exception {
        ListProjectDeploymentsToolCallback callback = new ListProjectDeploymentsToolCallback(
            mock(ProjectDeploymentFacade.class));

        String result = callback.call("{}", null);

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.has("error")).isTrue();
        assertThat(node.get("error")
            .asText()).contains("Workspace context");
    }
}
