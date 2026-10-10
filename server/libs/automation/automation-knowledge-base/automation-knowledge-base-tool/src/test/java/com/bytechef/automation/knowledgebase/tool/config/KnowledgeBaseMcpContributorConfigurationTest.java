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

package com.bytechef.automation.knowledgebase.tool.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ai.mcp.server.spi.McpServerToolCallbackContributor;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.automation.knowledgebase.facade.WorkspaceKnowledgeBaseFacade;
import com.bytechef.automation.knowledgebase.tool.knowledgebase.KnowledgeBaseToolCallbacksFactory;
import com.bytechef.platform.knowledgebase.facade.KnowledgeBaseDocumentFacade;
import com.bytechef.platform.knowledgebase.facade.KnowledgeBaseFacade;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

/**
 * @author Ivica Cardic
 */
class KnowledgeBaseMcpContributorConfigurationTest {

    private final KnowledgeBaseMcpContributorConfiguration configuration =
        new KnowledgeBaseMcpContributorConfiguration();
    private final WorkspaceFacade workspaceFacade = memberWorkspaceFacade();

    @Test
    void contributesTheSevenFlatKnowledgeBaseToolsWhenFactoryPresent() {
        KnowledgeBaseToolCallbacksFactory knowledgeBaseToolCallbacksFactory = new KnowledgeBaseToolCallbacksFactory(
            mock(WorkspaceKnowledgeBaseFacade.class), mock(KnowledgeBaseFacade.class),
            mock(KnowledgeBaseDocumentFacade.class));

        McpServerToolCallbackContributor contributor = configuration.knowledgeBaseFlatCrudMcpContributor(
            presentKnowledgeBaseFactory(knowledgeBaseToolCallbacksFactory), workspaceFacade);

        assertThat(contributor.getToolCallbacks())
            .extracting(toolCallback -> toolCallback.getToolDefinition()
                .name())
            .containsExactlyInAnyOrder(
                "listKnowledgeBases", "queryKnowledgeBase", "createKnowledgeBase", "addKnowledgeBaseDocument",
                "deleteKnowledgeBaseDocument", "cloneKnowledgeBase", "deleteKnowledgeBase");
    }

    @Test
    void knowledgeBaseContributorSkipsWhenFactoryAbsent() {
        McpServerToolCallbackContributor contributor = configuration.knowledgeBaseFlatCrudMcpContributor(
            absentKnowledgeBaseFactory(), workspaceFacade);

        assertThat(contributor.getToolCallbacks()).isEmpty();
    }

    @Test
    void everyKnowledgeBaseToolAcceptsWorkspaceId() {
        KnowledgeBaseToolCallbacksFactory knowledgeBaseToolCallbacksFactory = new KnowledgeBaseToolCallbacksFactory(
            mock(WorkspaceKnowledgeBaseFacade.class), mock(KnowledgeBaseFacade.class),
            mock(KnowledgeBaseDocumentFacade.class));

        McpServerToolCallbackContributor contributor = configuration.knowledgeBaseFlatCrudMcpContributor(
            presentKnowledgeBaseFactory(knowledgeBaseToolCallbacksFactory), workspaceFacade);

        assertThat(contributor.getToolCallbacks())
            .allSatisfy(toolCallback -> assertThat(toolCallback.getToolDefinition()
                .inputSchema()).contains("workspaceId"));
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<KnowledgeBaseToolCallbacksFactory> presentKnowledgeBaseFactory(
        KnowledgeBaseToolCallbacksFactory knowledgeBaseToolCallbacksFactory) {

        ObjectProvider<KnowledgeBaseToolCallbacksFactory> provider = mock(ObjectProvider.class);

        when(provider.getIfAvailable()).thenReturn(knowledgeBaseToolCallbacksFactory);

        return provider;
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<KnowledgeBaseToolCallbacksFactory> absentKnowledgeBaseFactory() {
        return mock(ObjectProvider.class);
    }

    private static WorkspaceFacade memberWorkspaceFacade() {
        WorkspaceFacade workspaceFacade = mock(WorkspaceFacade.class);

        lenient().when(workspaceFacade.getMyWorkspaces())
            .thenReturn(
                List.of(
                    new Workspace(1L, "Workspace 1"), new Workspace(7L, "Workspace 7"),
                    new Workspace(42L, "Workspace 42"), new Workspace(43L, "Workspace 43")));

        return workspaceFacade;
    }
}
