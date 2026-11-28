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

package com.bytechef.ai.copilot.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.ai.agent.facade.AiAgentFacade;
import com.bytechef.automation.ai.agent.tool.aiagent.AiAgentToolCallbacksFactory;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.automation.knowledgebase.facade.WorkspaceKnowledgeBaseFacade;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.ObjectProvider;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class ToolCallbackContributorConfigurationTest {

    private final ToolCallbackContributorConfiguration configuration =
        new ToolCallbackContributorConfiguration();
    private final WorkspaceFacade workspaceFacade = mock(WorkspaceFacade.class);

    @Nested
    class AiAgentCrudContributorTest {

        private final AiAgentFacade aiAgentFacade = mock(AiAgentFacade.class);
        private final WorkspaceKnowledgeBaseFacade workspaceKnowledgeBaseFacade =
            mock(WorkspaceKnowledgeBaseFacade.class);

        @Test
        void testContributesTheAgentBuilderTools() {
            assertThat(getToolCallbacks())
                .extracting(toolCallback -> toolCallback.getToolDefinition()
                    .name())
                .containsExactlyInAnyOrder(
                    "listAiAgents", "getAiAgent", "searchAiAgentTools", "searchAiAgentReferences", "createAiAgent",
                    "updateAiAgent",
                    "addAiAgentChannel", "deleteAiAgentChannel", "addAiAgentElement", "updateAiAgentElement",
                    "deleteAiAgentElement", "updateAiAgentSettings");
        }

        @Test
        void testOnlyListCreateAndSearchReferencesAcceptWorkspaceId() {
            Set<String> workspaceScopedNames = Set.of("listAiAgents", "createAiAgent", "searchAiAgentReferences");

            assertThat(getToolCallbacks())
                .allSatisfy(toolCallback -> {
                    String inputSchema = toolCallback.getToolDefinition()
                        .inputSchema();

                    if (workspaceScopedNames.contains(toolCallback.getToolDefinition()
                        .name())) {

                        assertThat(inputSchema).contains("workspaceId");
                    } else {
                        assertThat(inputSchema).doesNotContain("workspaceId");
                    }
                });
        }

        @Test
        void testListAiAgentsListsTheGivenWorkspace() {
            when(workspaceFacade.getMyWorkspaces()).thenReturn(List.of(new Workspace(7L, "Workspace")));

            String result = getToolCallbacks().stream()
                .filter(toolCallback -> "listAiAgents".equals(toolCallback.getToolDefinition()
                    .name()))
                .findFirst()
                .orElseThrow()
                .call("{\"workspaceId\": 7}");

            assertThat(result).isEqualTo("[]");
            verify(aiAgentFacade).getAgents(7L);
        }

        @Test
        void testSearchAiAgentReferencesFindsKnowledgeBasesOfTheGivenWorkspace() {
            when(workspaceFacade.getMyWorkspaces()).thenReturn(List.of(new Workspace(7L, "Workspace")));

            String result = getToolCallbacks().stream()
                .filter(toolCallback -> "searchAiAgentReferences".equals(toolCallback.getToolDefinition()
                    .name()))
                .findFirst()
                .orElseThrow()
                .call("{\"kind\": \"KNOWLEDGE_BASE\", \"workspaceId\": 7}");

            assertThat(result).isEqualTo("[]");
            verify(workspaceKnowledgeBaseFacade).getWorkspaceKnowledgeBases(eq(7L), anyLong());
        }

        @SuppressWarnings("unchecked")
        private List<ToolCallback> getToolCallbacks() {
            ObjectProvider<AiAgentToolCallbacksFactory> provider = mock(ObjectProvider.class);

            when(provider.getIfAvailable()).thenReturn(
                new AiAgentToolCallbacksFactory(aiAgentFacade, mock(ClusterElementDefinitionService.class), null,
                    workspaceKnowledgeBaseFacade));

            return configuration.aiAgentFlatCrudMcpContributor(provider, workspaceFacade)
                .getToolCallbacks();
        }
    }
}
