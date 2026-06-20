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

import com.bytechef.automation.search.SearchAssetProvider;
import com.bytechef.automation.search.SearchAssetType;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Locale;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Workspace-scoped knowledge-base search; lives in automation (not {@code platform-knowledge-base-service}) so it can
 * resolve the knowledge-base &rarr; workspace mapping (the {@code workspace_knowledge_base} relation) and stamp
 * {@code workspaceId} on its results.
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnProperty(prefix = "bytechef.ai.knowledge-base", name = "enabled", havingValue = "true")
class KnowledgeBaseSearchAssetProvider implements SearchAssetProvider {

    private final KnowledgeBaseService knowledgeBaseService;
    private final KnowledgeBaseWorkspaceResolver knowledgeBaseWorkspaceResolver;

    @SuppressFBWarnings("EI")
    KnowledgeBaseSearchAssetProvider(
        KnowledgeBaseService knowledgeBaseService, KnowledgeBaseWorkspaceResolver knowledgeBaseWorkspaceResolver) {

        this.knowledgeBaseService = knowledgeBaseService;
        this.knowledgeBaseWorkspaceResolver = knowledgeBaseWorkspaceResolver;
    }

    @Override
    public List<KnowledgeBaseSearchResult> search(String query, int limit) {
        String queryLower = query.toLowerCase(Locale.ROOT);

        return knowledgeBaseService.getKnowledgeBases()
            .stream()
            .filter(
                knowledgeBase -> containsIgnoreCase(knowledgeBase.getName(), queryLower) ||
                    containsIgnoreCase(knowledgeBase.getDescription(), queryLower))
            .limit(limit)
            .map(
                knowledgeBase -> new KnowledgeBaseSearchResult(
                    knowledgeBase.getId(), knowledgeBase.getName(), knowledgeBase.getDescription(),
                    knowledgeBaseWorkspaceResolver.getWorkspaceId(knowledgeBase.getId())))
            .toList();
    }

    @Override
    public SearchAssetType getAssetType() {
        return SearchAssetType.KNOWLEDGE_BASE;
    }

    private boolean containsIgnoreCase(String text, String query) {
        if (text == null) {
            return false;
        }

        return text.toLowerCase(Locale.ROOT)
            .contains(query);
    }
}
