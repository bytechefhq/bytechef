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
import static org.mockito.Mockito.mock;

import com.bytechef.automation.knowledgebase.facade.WorkspaceKnowledgeBaseFacade;
import com.bytechef.platform.knowledgebase.facade.KnowledgeBaseDocumentFacade;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseDocumentService;
import org.junit.jupiter.api.Test;

/**
 * Ticket 732, CRUD-delegate-unwind Task 6: {@code deleteKnowledgeBaseDocument} is now catalog-demoted on the AI Hub
 * BUILD agent (see {@code AiHubConfiguration#knowledgeBaseCatalogToolCallbacks}) rather than reachable only through the
 * dissolved {@code knowledge_base_agent} delegate, whose shared prompt was the only place that told the model to
 * confirm with the user before an irreversible delete. Its sibling {@link DeleteKnowledgeBaseToolCallback} already
 * carries that guidance in its own schema-level {@code DESCRIPTION}; this pins the same promotion for the document
 * delete tool so the invariant survives independent of which prompt happens to be loaded.
 *
 * @author Ivica Cardic
 */
class DeleteKnowledgeBaseDocumentToolCallbackTest {

    private final DeleteKnowledgeBaseDocumentToolCallback toolCallback = new DeleteKnowledgeBaseDocumentToolCallback(
        mock(KnowledgeBaseDocumentFacade.class), mock(KnowledgeBaseDocumentService.class),
        mock(WorkspaceKnowledgeBaseFacade.class), null);

    @Test
    void descriptionWarnsTheCallerToConfirmBeforeAnIrreversibleDelete() {
        String description = toolCallback.getToolDefinition()
            .description();

        assertThat(description).containsIgnoringCase("irreversible");
        assertThat(description).containsIgnoringCase("confirm");
    }
}
