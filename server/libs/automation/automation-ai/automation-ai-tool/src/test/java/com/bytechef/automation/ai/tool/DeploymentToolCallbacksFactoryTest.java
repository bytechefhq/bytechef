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

import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.ai.tool.ToolCallback;

/**
 * @author Ivica Cardic
 */
class DeploymentToolCallbacksFactoryTest {

    private final DeploymentToolCallbacksFactory factory = new DeploymentToolCallbacksFactory(
        Mockito.mock(ProjectDeploymentFacade.class));

    @Test
    void readListExcludesMutations() {
        List<String> names = toolNames(factory.readToolCallbacks());

        assertThat(names).containsExactlyInAnyOrder("listProjectDeployments");
        assertThat(names).doesNotContain(
            "createProjectDeployment", "updateProjectDeployment", "deleteProjectDeployment",
            "rollbackProjectDeployment", "toggleProjectDeployment", "promoteWorkflow");
    }

    @Test
    void writeListIncludesReadsAndMutations() {
        List<String> names = toolNames(factory.writeToolCallbacks());

        assertThat(names).containsExactlyInAnyOrder(
            "listProjectDeployments", "createProjectDeployment", "updateProjectDeployment",
            "deleteProjectDeployment", "rollbackProjectDeployment", "toggleProjectDeployment", "promoteWorkflow");
    }

    private static List<String> toolNames(List<ToolCallback> toolCallbacks) {
        return toolCallbacks.stream()
            .map(toolCallback -> toolCallback.getToolDefinition()
                .name())
            .collect(Collectors.toList());
    }
}
