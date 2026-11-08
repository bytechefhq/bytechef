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

package com.bytechef.automation.ai.tool.knowledgebase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.ai.copilot.tool.context.AgentToolInvocationContext;
import com.bytechef.automation.knowledgebase.facade.WorkspaceKnowledgeBaseFacade;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBase;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBaseDocument;
import com.bytechef.platform.knowledgebase.exception.KnowledgeBaseDocumentNotFoundException;
import com.bytechef.platform.knowledgebase.facade.KnowledgeBaseDocumentFacade;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.ai.chat.model.ToolContext;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class DeleteKnowledgeBaseDocumentToolCallbackTest {

    private static final long DOCUMENT_ID = 7L;
    private static final long KNOWLEDGE_BASE_ID = 42L;
    private static final long WORKSPACE_ID = 1L;

    private static final String TOOL_INPUT = "{\"knowledgeBaseId\":\"42\",\"documentId\":\"7\"}";

    private final KnowledgeBaseDocumentFacade knowledgeBaseDocumentFacade = mock(KnowledgeBaseDocumentFacade.class);
    private final WorkspaceKnowledgeBaseFacade workspaceKnowledgeBaseFacade = mock(WorkspaceKnowledgeBaseFacade.class);

    private final DeleteKnowledgeBaseDocumentToolCallback toolCallback = new DeleteKnowledgeBaseDocumentToolCallback(
        knowledgeBaseDocumentFacade, workspaceKnowledgeBaseFacade);

    @Test
    void descriptionWarnsTheCallerToConfirmBeforeAnIrreversibleDelete() {
        String description = toolCallback.getToolDefinition()
            .description();

        assertThat(description)
            .containsIgnoringCase("irreversible")
            .containsIgnoringCase("confirm");
    }

    @Test
    void testCallWithoutWorkspaceContextReturnsToolErrorAndDeletesNothing() {
        String result = toolCallback.call(TOOL_INPUT, new ToolContext(Map.of()));

        assertThat(result).contains("error")
            .contains("Workspace context unavailable");

        verifyNoInteractions(workspaceKnowledgeBaseFacade, knowledgeBaseDocumentFacade);
    }

    @Test
    void testCallWithKnowledgeBaseOutsideTheWorkspaceReturnsToolErrorAndDeletesNothing() {
        when(workspaceKnowledgeBaseFacade.getWorkspaceKnowledgeBases(WORKSPACE_ID, 0L))
            .thenReturn(List.of(knowledgeBase(99L)));

        String result = toolCallback.call(TOOL_INPUT, toolContext());

        assertThat(result).contains("error")
            .contains("not found in the current workspace");

        verify(knowledgeBaseDocumentFacade, never()).deleteKnowledgeBaseDocument(anyLong());
    }

    @Test
    void testCallWithDocumentOfAnotherKnowledgeBaseDeletesNothing() {
        when(workspaceKnowledgeBaseFacade.getWorkspaceKnowledgeBases(WORKSPACE_ID, 0L))
            .thenReturn(List.of(knowledgeBase(KNOWLEDGE_BASE_ID)));
        when(knowledgeBaseDocumentFacade.getKnowledgeBaseDocument(DOCUMENT_ID)).thenReturn(document(99L));

        String result = toolCallback.call(TOOL_INPUT, toolContext());

        assertThat(result).contains("\"deleted\":false");

        verify(knowledgeBaseDocumentFacade, never()).deleteKnowledgeBaseDocument(anyLong());
    }

    @Test
    void testCallWithMissingDocumentReportsNotDeleted() {
        when(workspaceKnowledgeBaseFacade.getWorkspaceKnowledgeBases(WORKSPACE_ID, 0L))
            .thenReturn(List.of(knowledgeBase(KNOWLEDGE_BASE_ID)));
        when(knowledgeBaseDocumentFacade.getKnowledgeBaseDocument(DOCUMENT_ID))
            .thenThrow(new KnowledgeBaseDocumentNotFoundException(DOCUMENT_ID));

        String result = toolCallback.call(TOOL_INPUT, toolContext());

        assertThat(result).contains("\"deleted\":false");

        verify(knowledgeBaseDocumentFacade, never()).deleteKnowledgeBaseDocument(anyLong());
    }

    @Test
    void testCallDeletesDocumentOfTheWorkspaceKnowledgeBase() {
        when(workspaceKnowledgeBaseFacade.getWorkspaceKnowledgeBases(WORKSPACE_ID, 0L))
            .thenReturn(List.of(knowledgeBase(KNOWLEDGE_BASE_ID)));
        when(knowledgeBaseDocumentFacade.getKnowledgeBaseDocument(DOCUMENT_ID))
            .thenReturn(document(KNOWLEDGE_BASE_ID));

        String result = toolCallback.call(TOOL_INPUT, toolContext());

        assertThat(result).contains("\"deleted\":true")
            .contains("\"documentId\":\"7\"");

        verify(knowledgeBaseDocumentFacade).deleteKnowledgeBaseDocument(DOCUMENT_ID);
    }

    private static KnowledgeBaseDocument document(long knowledgeBaseId) {
        KnowledgeBaseDocument knowledgeBaseDocument = new KnowledgeBaseDocument();

        knowledgeBaseDocument.setId(DOCUMENT_ID);
        knowledgeBaseDocument.setKnowledgeBaseId(knowledgeBaseId);

        return knowledgeBaseDocument;
    }

    private static KnowledgeBase knowledgeBase(long knowledgeBaseId) {
        KnowledgeBase knowledgeBase = new KnowledgeBase();

        knowledgeBase.setId(knowledgeBaseId);

        return knowledgeBase;
    }

    private static ToolContext toolContext() {
        return new ToolContext(
            AgentToolInvocationContext.builder()
                .workspaceId(WORKSPACE_ID)
                .environmentId(0L)
                .build()
                .toToolContext());
    }
}
