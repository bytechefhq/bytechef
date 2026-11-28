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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ai.mcp.server.spi.McpServerToolCallbackContributor;
import com.bytechef.automation.ai.tool.WorkspaceScopeResolver;
import com.bytechef.automation.knowledgebase.facade.WorkspaceKnowledgeBaseFacade;
import com.bytechef.automation.knowledgebase.tool.knowledgebase.KnowledgeBaseToolCallbacksFactory;
import com.bytechef.platform.knowledgebase.facade.KnowledgeBaseDocumentFacade;
import com.bytechef.platform.knowledgebase.facade.KnowledgeBaseFacade;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseDocumentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

/**
 * @author Ivica Cardic
 */
class KnowledgeBaseMcpContributorConfigurationTest {

    private final KnowledgeBaseMcpContributorConfiguration configuration =
        new KnowledgeBaseMcpContributorConfiguration();

    @Test
    void contributesTheSevenFlatKnowledgeBaseToolsWhenFactoryPresent() {
        KnowledgeBaseToolCallbacksFactory knowledgeBaseToolCallbacksFactory = new KnowledgeBaseToolCallbacksFactory(
            mock(WorkspaceKnowledgeBaseFacade.class), mock(KnowledgeBaseFacade.class),
            mock(KnowledgeBaseDocumentFacade.class),
            mock(KnowledgeBaseDocumentService.class));

        McpServerToolCallbackContributor contributor = configuration.knowledgeBaseFlatCrudMcpContributor(
            presentKnowledgeBaseFactory(knowledgeBaseToolCallbacksFactory), mock(WorkspaceScopeResolver.class));

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
            absentKnowledgeBaseFactory(), mock(WorkspaceScopeResolver.class));

        assertThat(contributor.getToolCallbacks()).isEmpty();
    }

    @Test
    void everyKnowledgeBaseToolAcceptsWorkspaceId() {
        KnowledgeBaseToolCallbacksFactory knowledgeBaseToolCallbacksFactory = new KnowledgeBaseToolCallbacksFactory(
            mock(WorkspaceKnowledgeBaseFacade.class), mock(KnowledgeBaseFacade.class),
            mock(KnowledgeBaseDocumentFacade.class),
            mock(KnowledgeBaseDocumentService.class));

        McpServerToolCallbackContributor contributor = configuration.knowledgeBaseFlatCrudMcpContributor(
            presentKnowledgeBaseFactory(knowledgeBaseToolCallbacksFactory), mock(WorkspaceScopeResolver.class));

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
}
