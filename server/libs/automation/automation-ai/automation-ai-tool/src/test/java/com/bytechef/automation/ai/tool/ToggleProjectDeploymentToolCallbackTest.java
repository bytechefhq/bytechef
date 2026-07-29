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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 *
 * @author Ivica Cardic
 */
class ToggleProjectDeploymentToolCallbackTest {

    private final JsonMapper jsonMapper = new JsonMapper();

    @Test
    void testEnableCallsFacadeWithTrue() throws Exception {
        ProjectDeploymentFacade facade = mock(ProjectDeploymentFacade.class);

        ToggleProjectDeploymentToolCallback callback = new ToggleProjectDeploymentToolCallback(facade);

        String result = callback.call("{\"projectDeploymentId\":\"7\",\"enabled\":true}");

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.get("projectDeploymentId")
            .asLong()).isEqualTo(7L);
        assertThat(node.get("enabled")
            .asBoolean()).isTrue();

        verify(facade).enableProjectDeployment(7L, true);
    }

    @Test
    void testDisableCallsFacadeWithFalse() throws Exception {
        ProjectDeploymentFacade facade = mock(ProjectDeploymentFacade.class);

        ToggleProjectDeploymentToolCallback callback = new ToggleProjectDeploymentToolCallback(facade);

        String result = callback.call("{\"projectDeploymentId\":\"7\",\"enabled\":false}");

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.get("enabled")
            .asBoolean()).isFalse();

        verify(facade).enableProjectDeployment(7L, false);
    }

    @Test
    void testRejectsMissingEnabled() throws Exception {
        ToggleProjectDeploymentToolCallback callback = new ToggleProjectDeploymentToolCallback(
            mock(ProjectDeploymentFacade.class));

        String result = callback.call("{\"projectDeploymentId\":\"7\"}");

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.has("error")).isTrue();
        assertThat(node.get("error")
            .asText()).contains("enabled");
    }
}
