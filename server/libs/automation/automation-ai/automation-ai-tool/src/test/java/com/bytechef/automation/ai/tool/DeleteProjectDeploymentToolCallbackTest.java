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
class DeleteProjectDeploymentToolCallbackTest {

    private final JsonMapper jsonMapper = new JsonMapper();

    @Test
    void testDeleteProjectDeploymentSuccess() throws Exception {
        ProjectDeploymentFacade facade = mock(ProjectDeploymentFacade.class);

        DeleteProjectDeploymentToolCallback callback = new DeleteProjectDeploymentToolCallback(facade);

        String result = callback.call("{\"projectDeploymentId\":\"11\"}");

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.get("projectDeploymentId")
            .asLong()).isEqualTo(11L);
        assertThat(node.get("deleted")
            .asBoolean()).isTrue();

        verify(facade).deleteProjectDeployment(11L);
    }

    @Test
    void testRejectsNonNumericId() throws Exception {
        DeleteProjectDeploymentToolCallback callback = new DeleteProjectDeploymentToolCallback(
            mock(ProjectDeploymentFacade.class));

        String result = callback.call("{\"projectDeploymentId\":\"foo\"}");

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.has("error")).isTrue();
        assertThat(node.get("error")
            .asText()).contains("numeric");
    }
}
