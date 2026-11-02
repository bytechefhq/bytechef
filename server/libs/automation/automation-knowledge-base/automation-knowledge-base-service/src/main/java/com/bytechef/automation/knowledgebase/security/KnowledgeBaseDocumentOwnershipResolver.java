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

package com.bytechef.automation.knowledgebase.security;

import com.bytechef.automation.configuration.security.ResourceOwnershipResolver;
import com.bytechef.automation.knowledgebase.domain.WorkspaceKnowledgeBase;
import com.bytechef.automation.knowledgebase.repository.WorkspaceKnowledgeBaseRepository;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBaseDocument;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseDocumentService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Maps a knowledge base document to the workspace owning its knowledge base, through the
 * {@code workspace_knowledge_base} relation. Fails closed when the document does not exist or its knowledge base is
 * assigned to no workspace.
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnProperty(prefix = "bytechef.ai.knowledge-base", name = "enabled", havingValue = "true")
public class KnowledgeBaseDocumentOwnershipResolver implements ResourceOwnershipResolver {

    private final KnowledgeBaseDocumentService knowledgeBaseDocumentService;
    private final WorkspaceKnowledgeBaseRepository workspaceKnowledgeBaseRepository;

    @SuppressFBWarnings("EI")
    public KnowledgeBaseDocumentOwnershipResolver(
        KnowledgeBaseDocumentService knowledgeBaseDocumentService,
        WorkspaceKnowledgeBaseRepository workspaceKnowledgeBaseRepository) {

        this.knowledgeBaseDocumentService = knowledgeBaseDocumentService;
        this.workspaceKnowledgeBaseRepository = workspaceKnowledgeBaseRepository;
    }

    @Override
    public String resourceType() {
        return "KnowledgeBaseDocument";
    }

    @Override
    public ResourceOwner resolveOwner(long id) {
        Optional<KnowledgeBaseDocument> knowledgeBaseDocument =
            knowledgeBaseDocumentService.fetchKnowledgeBaseDocument(id);

        if (knowledgeBaseDocument.isEmpty()) {
            return ResourceOwner.unknown();
        }

        List<WorkspaceKnowledgeBase> workspaceKnowledgeBases = workspaceKnowledgeBaseRepository.findByKnowledgeBaseId(
            knowledgeBaseDocument.get()
                .getKnowledgeBaseId());

        if (workspaceKnowledgeBases.isEmpty()) {
            return ResourceOwner.unknown();
        }

        WorkspaceKnowledgeBase workspaceKnowledgeBase = workspaceKnowledgeBases.getFirst();

        return ResourceOwner.ofWorkspace(workspaceKnowledgeBase.getWorkspaceId());
    }
}
