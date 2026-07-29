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

import com.bytechef.automation.ai.tool.ToolArtifactRecorder;
import com.bytechef.automation.knowledgebase.facade.WorkspaceKnowledgeBaseFacade;
import com.bytechef.platform.knowledgebase.facade.KnowledgeBaseDocumentFacade;
import com.bytechef.platform.knowledgebase.facade.KnowledgeBaseFacade;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseDocumentService;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseService;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.ai.tool.ToolCallback;

/**
 *
 * @author Ivica Cardic
 */
class KnowledgeBaseToolCallbacksFactoryTest {

    private final KnowledgeBaseToolCallbacksFactory factory = new KnowledgeBaseToolCallbacksFactory(
        Mockito.mock(WorkspaceKnowledgeBaseFacade.class),
        Mockito.mock(KnowledgeBaseFacade.class),
        Mockito.mock(KnowledgeBaseService.class),
        Mockito.mock(KnowledgeBaseDocumentFacade.class),
        Mockito.mock(KnowledgeBaseDocumentService.class),
        Mockito.mock(ToolArtifactRecorder.class));

    @Test
    void readListExcludesMutations() {
        List<String> names = toolNames(factory.readToolCallbacks());

        assertThat(names).contains("listKnowledgeBases", "queryKnowledgeBase");
        assertThat(names).doesNotContain("createKnowledgeBase", "deleteKnowledgeBase", "addKnowledgeBaseDocument");
    }

    @Test
    void writeListIncludesReadsAndMutations() {
        List<String> names = toolNames(factory.writeToolCallbacks());

        assertThat(names).contains(
            "listKnowledgeBases", "queryKnowledgeBase", "createKnowledgeBase", "addKnowledgeBaseDocument",
            "deleteKnowledgeBaseDocument", "cloneKnowledgeBase", "deleteKnowledgeBase");
    }

    private static List<String> toolNames(List<ToolCallback> toolCallbacks) {
        return toolCallbacks.stream()
            .map(toolCallback -> toolCallback.getToolDefinition()
                .name())
            .collect(Collectors.toList());
    }
}
