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
import com.bytechef.platform.knowledgebase.domain.KnowledgeBaseDocument;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseDocumentService;
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
class KnowledgeBaseDocumentSearchAssetProviderTest {

    @Mock
    private KnowledgeBaseDocumentService knowledgeBaseDocumentService;

    @Mock
    private KnowledgeBaseService knowledgeBaseService;

    @Mock
    private WorkspaceKnowledgeBaseRepository workspaceKnowledgeBaseRepository;

    @Test
    void testSearchStampsDocumentWithItsKnowledgeBaseWorkspace() {
        KnowledgeBase knowledgeBase = new KnowledgeBase();

        knowledgeBase.setId(1L);

        KnowledgeBaseDocument knowledgeBaseDocument = new KnowledgeBaseDocument();

        knowledgeBaseDocument.setId(5L);
        knowledgeBaseDocument.setName("pricing sheet");

        when(knowledgeBaseService.getKnowledgeBases()).thenReturn(List.of(knowledgeBase));
        when(knowledgeBaseDocumentService.getKnowledgeBaseDocuments(1L)).thenReturn(List.of(knowledgeBaseDocument));
        when(workspaceKnowledgeBaseRepository.findByKnowledgeBaseId(1L)).thenReturn(
            List.of(new WorkspaceKnowledgeBase(1L, 7L)));

        KnowledgeBaseDocumentSearchAssetProvider knowledgeBaseDocumentSearchAssetProvider =
            new KnowledgeBaseDocumentSearchAssetProvider(
                knowledgeBaseDocumentService, knowledgeBaseService,
                new KnowledgeBaseWorkspaceResolver(workspaceKnowledgeBaseRepository));

        List<KnowledgeBaseDocumentSearchResult> knowledgeBaseDocumentSearchResults =
            knowledgeBaseDocumentSearchAssetProvider.search("PRICING", 10);

        assertThat(knowledgeBaseDocumentSearchResults).containsExactly(
            new KnowledgeBaseDocumentSearchResult(5L, 1L, "pricing sheet", 7L));
    }
}
