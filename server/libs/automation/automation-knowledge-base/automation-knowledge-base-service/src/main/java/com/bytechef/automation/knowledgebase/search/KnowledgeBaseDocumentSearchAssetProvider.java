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

import com.bytechef.automation.knowledgebase.domain.KnowledgeBase;
import com.bytechef.automation.knowledgebase.domain.KnowledgeBaseDocument;
import com.bytechef.automation.knowledgebase.service.KnowledgeBaseDocumentService;
import com.bytechef.automation.knowledgebase.service.KnowledgeBaseService;
import com.bytechef.automation.search.SearchAssetProvider;
import com.bytechef.automation.search.SearchAssetType;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
@ConditionalOnProperty(prefix = "bytechef.knowledge-base", name = "enabled", havingValue = "true")
class KnowledgeBaseDocumentSearchAssetProvider implements SearchAssetProvider {

    private final KnowledgeBaseDocumentService knowledgeBaseDocumentService;
    private final KnowledgeBaseService knowledgeBaseService;

    KnowledgeBaseDocumentSearchAssetProvider(
        KnowledgeBaseDocumentService knowledgeBaseDocumentService, KnowledgeBaseService knowledgeBaseService) {

        this.knowledgeBaseDocumentService = knowledgeBaseDocumentService;
        this.knowledgeBaseService = knowledgeBaseService;
    }

    @Override
    public List<KnowledgeBaseDocumentSearchResult> search(String query, int limit) {
        String queryLower = query.toLowerCase(Locale.ROOT);

        List<KnowledgeBase> knowledgeBases = knowledgeBaseService.getKnowledgeBases();

        List<KnowledgeBaseDocumentSearchResult> results = new ArrayList<>();

        for (KnowledgeBase knowledgeBase : knowledgeBases) {
            List<KnowledgeBaseDocument> documents =
                knowledgeBaseDocumentService.getKnowledgeBaseDocuments(knowledgeBase.getId());

            for (KnowledgeBaseDocument document : documents) {
                if (containsIgnoreCase(document.getName(), queryLower)) {
                    results.add(
                        new KnowledgeBaseDocumentSearchResult(
                            document.getId(), knowledgeBase.getId(), document.getName()));

                    if (results.size() >= limit) {
                        return results;
                    }
                }
            }
        }

        return results;
    }

    @Override
    public SearchAssetType getAssetType() {
        return SearchAssetType.KNOWLEDGE_BASE_DOCUMENT;
    }

    private boolean containsIgnoreCase(String text, String query) {
        if (text == null) {
            return false;
        }

        return text.toLowerCase(Locale.ROOT)
            .contains(query);
    }
}
