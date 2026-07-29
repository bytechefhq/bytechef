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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.dto.ProjectDeploymentDTO;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.platform.configuration.domain.Environment;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 *
 * @author Ivica Cardic
 */
class UpdateProjectDeploymentToolCallbackTest {

    private final JsonMapper jsonMapper = new JsonMapper();

    @Test
    void testUpdateNameKeepsEnabledEnvironmentVersion() throws Exception {
        ProjectDeploymentFacade facade = mock(ProjectDeploymentFacade.class);

        ProjectDeployment domain = new ProjectDeployment();

        domain.setId(11L);
        domain.setName("Old name");
        domain.setDescription("Keep me");
        domain.setEnabled(true);
        domain.setEnvironment(Environment.PRODUCTION);
        domain.setProjectId(7L);
        domain.setProjectVersion(3);
        domain.setVersion(2);

        when(facade.getProjectDeployment(11L)).thenReturn(new ProjectDeploymentDTO(domain));

        UpdateProjectDeploymentToolCallback callback = new UpdateProjectDeploymentToolCallback(facade);

        String result = callback.call(
            "{\"projectDeploymentId\":\"11\",\"name\":\"New name\"}");

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.get("projectDeploymentId")
            .asLong()).isEqualTo(11L);
        assertThat(node.get("name")
            .asText()).isEqualTo("New name");
        assertThat(node.get("description")
            .asText()).isEqualTo("Keep me");

        ArgumentCaptor<ProjectDeploymentDTO> captor = ArgumentCaptor.forClass(ProjectDeploymentDTO.class);

        verify(facade).updateProjectDeployment(captor.capture());

        ProjectDeploymentDTO sent = captor.getValue();

        // Pin the contract: name updated, the rest of the deployment shape is preserved (enabled,
        // environment, projectVersion, optimistic-lock version).
        assertThat(sent.name()).isEqualTo("New name");
        assertThat(sent.description()).isEqualTo("Keep me");
        assertThat(sent.enabled()).isTrue();
        assertThat(sent.environment()).isEqualTo(Environment.PRODUCTION);
        assertThat(sent.projectVersion()).isEqualTo(3);
        assertThat(sent.version()).isEqualTo(2);
    }

    @Test
    void testRejectsNoOpUpdate() throws Exception {
        ProjectDeploymentFacade facade = mock(ProjectDeploymentFacade.class);

        UpdateProjectDeploymentToolCallback callback = new UpdateProjectDeploymentToolCallback(facade);

        String result = callback.call("{\"projectDeploymentId\":\"11\"}");

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.has("error")).isTrue();
        assertThat(node.get("error")
            .asText()).contains("name or description");

        // The facade must NOT be called when there's nothing to update — input validation short-circuits
        // before any read or write, which prevents a no-op write that would bump lastModifiedDate and
        // confuse audit trails.
        verify(facade, never()).updateProjectDeployment(any(ProjectDeploymentDTO.class));
    }
}
