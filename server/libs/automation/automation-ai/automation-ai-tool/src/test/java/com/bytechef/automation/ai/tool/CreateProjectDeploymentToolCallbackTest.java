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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.dto.ProjectDeploymentDTO;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.exception.ErrorType;
import com.bytechef.platform.configuration.domain.Environment;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 *
 * @author Ivica Cardic
 */
class CreateProjectDeploymentToolCallbackTest {

    private final JsonMapper jsonMapper = new JsonMapper();

    @Test
    void testToolDefinitionExposesName() {
        CreateProjectDeploymentToolCallback callback = new CreateProjectDeploymentToolCallback(
            mock(ProjectDeploymentFacade.class));

        ToolDefinition definition = callback.getToolDefinition();

        assertThat(definition.name()).isEqualTo(CreateProjectDeploymentToolCallback.TOOL_NAME);
        assertThat(definition.description()).isNotBlank();
        assertThat(definition.inputSchema()).contains("projectId");
    }

    @Test
    void testCreateProjectDeploymentSuccess() throws Exception {
        ProjectDeploymentFacade facade = mock(ProjectDeploymentFacade.class);

        when(facade.createProjectDeployment(any(ProjectDeploymentDTO.class))).thenReturn(99L);

        CreateProjectDeploymentToolCallback callback = new CreateProjectDeploymentToolCallback(facade);

        String result = callback.call(
            "{\"projectId\":\"42\",\"projectVersion\":3,\"environment\":\"STAGING\",\"name\":\"My deploy\"}");

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.get("projectDeploymentId")
            .asLong()).isEqualTo(99L);
        assertThat(node.get("projectId")
            .asLong()).isEqualTo(42L);
        assertThat(node.get("projectVersion")
            .asInt()).isEqualTo(3);
        assertThat(node.get("environment")
            .asText()).isEqualTo("STAGING");
        assertThat(node.get("enabled")
            .asBoolean()).isFalse();

        ArgumentCaptor<ProjectDeploymentDTO> captor = ArgumentCaptor.forClass(ProjectDeploymentDTO.class);

        verify(facade).createProjectDeployment(captor.capture());

        ProjectDeploymentDTO captured = captor.getValue();

        assertThat(captured.projectId()).isEqualTo(42L);
        assertThat(captured.projectVersion()).isEqualTo(3);
        assertThat(captured.environment()).isEqualTo(Environment.STAGING);
        assertThat(captured.name()).isEqualTo("My deploy");
        // Pin the default-disabled invariant: chat-driven deploys must come up disabled so the user can wire
        // connections
        // before triggers go live. Flipping this default would surprise users with a deploy that fires on first save.
        assertThat(captured.enabled()).isFalse();
    }

    @Test
    void testRejectsMissingProjectId() throws Exception {
        CreateProjectDeploymentToolCallback callback = new CreateProjectDeploymentToolCallback(
            mock(ProjectDeploymentFacade.class));

        String result = callback.call("{\"projectVersion\":1,\"environment\":\"DEVELOPMENT\"}");

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.has("error")).isTrue();
        assertThat(node.get("error")
            .asText()).contains("projectId");
    }

    @Test
    void testRejectsBadEnvironment() throws Exception {
        CreateProjectDeploymentToolCallback callback = new CreateProjectDeploymentToolCallback(
            mock(ProjectDeploymentFacade.class));

        String result = callback.call("{\"projectId\":\"1\",\"projectVersion\":1,\"environment\":\"WHATEVER\"}");

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.has("error")).isTrue();
        assertThat(node.get("error")
            .asText()).contains("WHATEVER");
    }

    @Test
    void testSurfacesConfigurationExceptionAsToolError() throws Exception {
        // The facade enforces "project must be published" and "version must not be DRAFT" via ConfigurationException;
        // those domain rules must surface to the LLM as recoverable tool errors, not as a runtime-failure with the
        // exception's class name in the message — otherwise the agent loop has no signal to suggest a fix to the user.
        ProjectDeploymentFacade facade = mock(ProjectDeploymentFacade.class);

        when(facade.createProjectDeployment(any(ProjectDeploymentDTO.class)))
            .thenThrow(new ConfigurationException("Project id=1 is not published", new ErrorType() {

                @Override
                public Class<?> getErrorClass() {
                    return Object.class;
                }

                @Override
                public int getErrorKey() {
                    return 1;
                }
            }));

        CreateProjectDeploymentToolCallback callback = new CreateProjectDeploymentToolCallback(facade);

        String result = callback.call("{\"projectId\":\"1\",\"projectVersion\":1,\"environment\":\"DEVELOPMENT\"}");

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.has("error")).isTrue();
        assertThat(node.get("error")
            .asText()).contains("not published");
    }
}
