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
import static org.mockito.Mockito.when;

import com.bytechef.automation.knowledgebase.domain.WorkspaceKnowledgeBase;
import com.bytechef.automation.knowledgebase.repository.WorkspaceKnowledgeBaseRepository;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBase;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class KnowledgeBaseSearchAssetProviderTest {

    @Mock
    private KnowledgeBaseService knowledgeBaseService;

    @Mock
    private WorkspaceKnowledgeBaseRepository workspaceKnowledgeBaseRepository;

    @Test
    void testSearchStampsEachKnowledgeBaseWithItsWorkspace() {
        when(knowledgeBaseService.getKnowledgeBases()).thenReturn(
            List.of(createKnowledgeBase(1L, "sales docs"), createKnowledgeBase(2L, "sales faq")));
        when(workspaceKnowledgeBaseRepository.findByKnowledgeBaseId(1L)).thenReturn(
            List.of(new WorkspaceKnowledgeBase(1L, 7L)));
        when(workspaceKnowledgeBaseRepository.findByKnowledgeBaseId(2L)).thenReturn(List.of());

        KnowledgeBaseSearchAssetProvider knowledgeBaseSearchAssetProvider = new KnowledgeBaseSearchAssetProvider(
            knowledgeBaseService, new KnowledgeBaseWorkspaceResolver(workspaceKnowledgeBaseRepository));

        List<KnowledgeBaseSearchResult> knowledgeBaseSearchResults = knowledgeBaseSearchAssetProvider.search(
            "SALES", 10);

        assertThat(knowledgeBaseSearchResults).containsExactly(
            new KnowledgeBaseSearchResult(1L, "sales docs", null, 7L),
            new KnowledgeBaseSearchResult(2L, "sales faq", null, null));
    }

    private static KnowledgeBase createKnowledgeBase(long id, String name) {
        KnowledgeBase knowledgeBase = new KnowledgeBase();

        knowledgeBase.setId(id);
        knowledgeBase.setName(name);

        return knowledgeBase;
    }
}
