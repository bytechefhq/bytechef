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
import com.bytechef.automation.knowledgebase.service.WorkspaceKnowledgeBaseService;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBaseDocument;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseDocumentService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
@ConditionalOnProperty(prefix = "bytechef.ai.knowledge-base", name = "enabled", havingValue = "true")
public class KnowledgeBaseDocumentOwnershipResolver implements ResourceOwnershipResolver {

    private final KnowledgeBaseDocumentService knowledgeBaseDocumentService;
    private final WorkspaceKnowledgeBaseService workspaceKnowledgeBaseService;

    @SuppressFBWarnings("EI")
    public KnowledgeBaseDocumentOwnershipResolver(
        KnowledgeBaseDocumentService knowledgeBaseDocumentService,
        WorkspaceKnowledgeBaseService workspaceKnowledgeBaseService) {

        this.knowledgeBaseDocumentService = knowledgeBaseDocumentService;
        this.workspaceKnowledgeBaseService = workspaceKnowledgeBaseService;
    }

    @Override
    public String resourceType() {
        return "KnowledgeBaseDocument";
    }

    @Override
    public ResourceOwner resolveOwner(long id) {
        Optional<KnowledgeBaseDocument> knowledgeBaseDocumentOptional =
            knowledgeBaseDocumentService.fetchKnowledgeBaseDocument(id);

        if (knowledgeBaseDocumentOptional.isEmpty()) {
            return ResourceOwner.unknown();
        }

        KnowledgeBaseDocument knowledgeBaseDocument = knowledgeBaseDocumentOptional.get();

        Long knowledgeBaseId = knowledgeBaseDocument.getKnowledgeBaseId();

        if (knowledgeBaseId == null) {
            return ResourceOwner.unknown();
        }

        return workspaceKnowledgeBaseService.fetchWorkspaceId(knowledgeBaseId)
            .map(ResourceOwner::ofWorkspace)
            .orElseGet(ResourceOwner::unknown);
    }
}
