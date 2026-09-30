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

import com.bytechef.automation.knowledgebase.domain.WorkspaceKnowledgeBase;
import com.bytechef.automation.knowledgebase.repository.WorkspaceKnowledgeBaseRepository;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Resolves a knowledge base to its owning workspace through the {@code workspace_knowledge_base} relation, so the
 * knowledge base and document search providers can stamp the workspace on their results.
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnProperty(prefix = "bytechef.ai.knowledge-base", name = "enabled", havingValue = "true")
class KnowledgeBaseWorkspaceResolver {

    private final WorkspaceKnowledgeBaseRepository workspaceKnowledgeBaseRepository;

    @SuppressFBWarnings("EI")
    KnowledgeBaseWorkspaceResolver(WorkspaceKnowledgeBaseRepository workspaceKnowledgeBaseRepository) {
        this.workspaceKnowledgeBaseRepository = workspaceKnowledgeBaseRepository;
    }

    @Nullable
    Long getWorkspaceId(long knowledgeBaseId) {
        List<WorkspaceKnowledgeBase> workspaceKnowledgeBases = workspaceKnowledgeBaseRepository.findByKnowledgeBaseId(
            knowledgeBaseId);

        return workspaceKnowledgeBases.stream()
            .map(WorkspaceKnowledgeBase::getWorkspaceId)
            .findFirst()
            .orElse(null);
    }
}
