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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.bytechef.automation.knowledgebase.facade.WorkspaceKnowledgeBaseFacade;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 *
 * @author Ivica Cardic
 */
class DeleteKnowledgeBaseToolCallbackTest {

    private final WorkspaceKnowledgeBaseFacade facade = Mockito.mock(WorkspaceKnowledgeBaseFacade.class);
    private final DeleteKnowledgeBaseToolCallback toolCallback = new DeleteKnowledgeBaseToolCallback(facade);

    @Test
    void deletesKnowledgeBaseById() {
        String result = toolCallback.call("{\"id\": 42}");

        verify(facade).deleteWorkspaceKnowledgeBase(42L);
        assertThat(result).contains("\"deleted\":true");
    }

    @Test
    void rejectsMissingId() {
        String result = toolCallback.call("{}");

        assertThat(result).contains("id is required");
    }

    @Test
    void surfacesFacadeIllegalArgumentExceptionAsToolError() {
        doThrow(new IllegalArgumentException("Knowledge base not found"))
            .when(facade)
            .deleteWorkspaceKnowledgeBase(42L);

        String result = toolCallback.call("{\"id\": 42}");

        assertThat(result).contains("Knowledge base not found");
    }
}
