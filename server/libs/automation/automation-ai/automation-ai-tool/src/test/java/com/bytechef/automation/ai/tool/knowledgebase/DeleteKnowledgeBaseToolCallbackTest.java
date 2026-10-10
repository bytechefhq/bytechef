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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ai.copilot.tool.context.AgentToolInvocationContext;
import com.bytechef.automation.knowledgebase.facade.WorkspaceKnowledgeBaseFacade;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBase;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;

/**
 * @author Ivica Cardic
 */
class DeleteKnowledgeBaseToolCallbackTest {

    private static final long WORKSPACE_ID = 1L;

    private final WorkspaceKnowledgeBaseFacade facade = mock(WorkspaceKnowledgeBaseFacade.class);
    private final DeleteKnowledgeBaseToolCallback toolCallback = new DeleteKnowledgeBaseToolCallback(facade);

    @BeforeEach
    void beforeEach() {
        when(facade.getWorkspaceKnowledgeBases(WORKSPACE_ID, 0L)).thenReturn(List.of(knowledgeBase(42L)));
    }

    @Test
    void deletesKnowledgeBaseById() {
        String result = toolCallback.call("{\"id\": 42}", toolContext());

        verify(facade).deleteWorkspaceKnowledgeBase(42L);
        assertThat(result).contains("\"deleted\":true");
    }

    @Test
    void refusesKnowledgeBaseOutsideTheCurrentWorkspace() {
        String result = toolCallback.call("{\"id\": 99}", toolContext());

        verify(facade, never()).deleteWorkspaceKnowledgeBase(anyLong());
        assertThat(result).contains("not found in the current workspace");
    }

    @Test
    void refusesWithoutWorkspaceContext() {
        String result = toolCallback.call("{\"id\": 42}");

        verify(facade, never()).deleteWorkspaceKnowledgeBase(anyLong());
        assertThat(result).contains("Workspace context unavailable");
    }

    @Test
    void rejectsMissingId() {
        String result = toolCallback.call("{}", toolContext());

        assertThat(result).contains("id is required");
    }

    @Test
    void surfacesFacadeIllegalArgumentExceptionAsToolError() {
        doThrow(new IllegalArgumentException("Knowledge base not found"))
            .when(facade)
            .deleteWorkspaceKnowledgeBase(42L);

        String result = toolCallback.call("{\"id\": 42}", toolContext());

        assertThat(result).contains("Knowledge base not found");
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
