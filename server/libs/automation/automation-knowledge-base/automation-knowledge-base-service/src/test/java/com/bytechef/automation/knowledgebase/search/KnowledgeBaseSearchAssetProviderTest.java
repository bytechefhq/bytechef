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

package com.bytechef.automation.knowledgebase.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.automation.knowledgebase.domain.WorkspaceKnowledgeBase;
import com.bytechef.automation.knowledgebase.service.WorkspaceKnowledgeBaseService;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBase;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseService;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class KnowledgeBaseSearchAssetProviderTest {

    @Test
    void testReturnsOnlyCallerWorkspaceKnowledgeBasesUpToLimit() {
        KnowledgeBaseService knowledgeBaseService = mock(KnowledgeBaseService.class);
        WorkspaceKnowledgeBaseService workspaceKnowledgeBaseService = mock(WorkspaceKnowledgeBaseService.class);

        WorkspaceKnowledgeBase ownKnowledgeBase = mock(WorkspaceKnowledgeBase.class);
        WorkspaceKnowledgeBase ownKnowledgeBase2 = mock(WorkspaceKnowledgeBase.class);

        when(ownKnowledgeBase.getKnowledgeBaseId()).thenReturn(3L);
        when(ownKnowledgeBase2.getKnowledgeBaseId()).thenReturn(4L);
        when(workspaceKnowledgeBaseService.getWorkspaceKnowledgeBases(10L)).thenReturn(
            List.of(ownKnowledgeBase, ownKnowledgeBase2));

        List<KnowledgeBase> knowledgeBases = List.of(
            createKnowledgeBase(1L, "Docs foreign"), createKnowledgeBase(2L, "Docs foreign 2"),
            createKnowledgeBase(3L, "Docs own"), createKnowledgeBase(4L, "Docs own 2"));

        when(knowledgeBaseService.getKnowledgeBases()).thenReturn(knowledgeBases);

        KnowledgeBaseSearchAssetProvider knowledgeBaseSearchAssetProvider = new KnowledgeBaseSearchAssetProvider(
            knowledgeBaseService, workspaceKnowledgeBaseService);

        List<KnowledgeBaseSearchResult> results = knowledgeBaseSearchAssetProvider.search("docs", 1, Set.of(10L));

        assertThat(results).extracting(KnowledgeBaseSearchResult::id)
            .containsExactly(3L);
    }

    private static KnowledgeBase createKnowledgeBase(long id, String name) {
        KnowledgeBase knowledgeBase = mock(KnowledgeBase.class);

        when(knowledgeBase.getId()).thenReturn(id);
        when(knowledgeBase.getName()).thenReturn(name);

        return knowledgeBase;
    }
}
