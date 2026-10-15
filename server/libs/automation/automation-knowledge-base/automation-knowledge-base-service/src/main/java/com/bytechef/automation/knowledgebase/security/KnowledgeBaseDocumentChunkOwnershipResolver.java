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
import com.bytechef.platform.knowledgebase.domain.KnowledgeBaseDocumentChunk;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseDocumentChunkService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Maps a knowledge base document chunk to the workspace owning its document, delegating the document-to-workspace hop
 * to {@link KnowledgeBaseDocumentOwnershipResolver} so both answer "whose is this" the same way. Fails closed when the
 * chunk does not exist.
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnProperty(prefix = "bytechef.ai.knowledge-base", name = "enabled", havingValue = "true")
public class KnowledgeBaseDocumentChunkOwnershipResolver implements ResourceOwnershipResolver {

    private final KnowledgeBaseDocumentChunkService knowledgeBaseDocumentChunkService;
    private final KnowledgeBaseDocumentOwnershipResolver knowledgeBaseDocumentOwnershipResolver;

    @SuppressFBWarnings("EI")
    public KnowledgeBaseDocumentChunkOwnershipResolver(
        KnowledgeBaseDocumentChunkService knowledgeBaseDocumentChunkService,
        KnowledgeBaseDocumentOwnershipResolver knowledgeBaseDocumentOwnershipResolver) {

        this.knowledgeBaseDocumentChunkService = knowledgeBaseDocumentChunkService;
        this.knowledgeBaseDocumentOwnershipResolver = knowledgeBaseDocumentOwnershipResolver;
    }

    @Override
    public String resourceType() {
        return "KnowledgeBaseDocumentChunk";
    }

    @Override
    public ResourceOwner resolveOwner(long id) {
        Optional<KnowledgeBaseDocumentChunk> knowledgeBaseDocumentChunk =
            knowledgeBaseDocumentChunkService.fetchKnowledgeBaseDocumentChunk(id);

        if (knowledgeBaseDocumentChunk.isEmpty()) {
            return ResourceOwner.unknown();
        }

        return knowledgeBaseDocumentOwnershipResolver.resolveOwner(
            (long) knowledgeBaseDocumentChunk.get()
                .getKnowledgeBaseDocumentId());
    }
}
