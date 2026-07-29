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

package com.bytechef.automation.knowledgebase.facade;

import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBaseDocument;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBaseDocumentChunk;
import com.bytechef.platform.knowledgebase.dto.DocumentStatusUpdate;
import com.bytechef.platform.knowledgebase.facade.KnowledgeBaseDocumentChunkFacade;
import com.bytechef.platform.knowledgebase.facade.KnowledgeBaseDocumentFacade;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseDocumentChunkService;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseDocumentService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

/**
 * Authorizes knowledge-base document/chunk operations against the owning knowledge base's workspace role (reusing the
 * {@code KnowledgeBase} ownership resolver) before delegating to the shared platform facades/services. The AI-Hub agent
 * tools keep calling the shared facades directly and are unaffected.
 *
 * @author Ivica Cardic
 */
@Service
@ConditionalOnProperty(prefix = "bytechef.ai.knowledge-base", name = "enabled", havingValue = "true")
class KnowledgeBaseDocumentApiFacadeImpl implements KnowledgeBaseDocumentApiFacade {

    private static final String KNOWLEDGE_BASE_RESOURCE_TYPE = "KnowledgeBase";

    private final KnowledgeBaseDocumentChunkFacade knowledgeBaseDocumentChunkFacade;
    private final KnowledgeBaseDocumentChunkService knowledgeBaseDocumentChunkService;
    private final KnowledgeBaseDocumentFacade knowledgeBaseDocumentFacade;
    private final KnowledgeBaseDocumentService knowledgeBaseDocumentService;
    private final PermissionService permissionService;

    @SuppressFBWarnings("EI")
    KnowledgeBaseDocumentApiFacadeImpl(
        KnowledgeBaseDocumentChunkFacade knowledgeBaseDocumentChunkFacade,
        KnowledgeBaseDocumentChunkService knowledgeBaseDocumentChunkService,
        KnowledgeBaseDocumentFacade knowledgeBaseDocumentFacade,
        KnowledgeBaseDocumentService knowledgeBaseDocumentService, PermissionService permissionService) {

        this.knowledgeBaseDocumentChunkFacade = knowledgeBaseDocumentChunkFacade;
        this.knowledgeBaseDocumentChunkService = knowledgeBaseDocumentChunkService;
        this.knowledgeBaseDocumentFacade = knowledgeBaseDocumentFacade;
        this.knowledgeBaseDocumentService = knowledgeBaseDocumentService;
        this.permissionService = permissionService;
    }

    @Override
    public KnowledgeBaseDocument getKnowledgeBaseDocument(long id) {
        return checkDocumentRole(id, "VIEWER");
    }

    @Override
    public DocumentStatusUpdate getKnowledgeBaseDocumentStatus(long id) {
        checkDocumentRole(id, "VIEWER");

        return knowledgeBaseDocumentService.getKnowledgeBaseDocumentStatus(id);
    }

    @Override
    public List<KnowledgeBaseDocumentChunk> getKnowledgeBaseDocumentChunksByDocumentId(long documentId) {
        checkDocumentRole(documentId, "VIEWER");

        return knowledgeBaseDocumentChunkFacade.getKnowledgeBaseDocumentChunksByDocumentId(documentId);
    }

    @Override
    public List<KnowledgeBaseDocumentChunk> getKnowledgeBaseDocumentChunksByDocumentIdWithoutContent(long documentId) {
        checkDocumentRole(documentId, "VIEWER");

        return knowledgeBaseDocumentChunkService.getKnowledgeBaseDocumentChunksByDocumentId(documentId);
    }

    @Override
    public void deleteKnowledgeBaseDocument(long id) {
        checkDocumentRole(id, "EDITOR");

        knowledgeBaseDocumentFacade.deleteKnowledgeBaseDocument(id);
    }

    @Override
    public void updateKnowledgeBaseDocumentTags(long documentId, List<String> tagNames) {
        checkDocumentRole(documentId, "EDITOR");

        knowledgeBaseDocumentFacade.updateKnowledgeBaseDocumentTags(documentId, tagNames);
    }

    @Override
    public KnowledgeBaseDocumentChunk updateKnowledgeBaseDocumentChunk(long id, String content) {
        checkChunkRole(id, "EDITOR");

        return knowledgeBaseDocumentChunkFacade.updateKnowledgeBaseDocumentChunk(id, content);
    }

    @Override
    public void deleteKnowledgeBaseDocumentChunk(long id) {
        checkChunkRole(id, "EDITOR");

        knowledgeBaseDocumentChunkFacade.deleteKnowledgeBaseDocumentChunk(id);
    }

    /**
     * Resolves the document to its owning knowledge base and authorizes the caller against that knowledge base's
     * workspace role. Fails closed (the document has no knowledge base, or the caller lacks the role).
     */
    private KnowledgeBaseDocument checkDocumentRole(long documentId, String role) {
        KnowledgeBaseDocument knowledgeBaseDocument = knowledgeBaseDocumentService.getKnowledgeBaseDocument(documentId);

        Long knowledgeBaseId = knowledgeBaseDocument.getKnowledgeBaseId();

        if (knowledgeBaseId == null ||
            !permissionService.hasResourceRole(knowledgeBaseId, KNOWLEDGE_BASE_RESOURCE_TYPE, role)) {

            throw new AccessDeniedException("Access to knowledge base document " + documentId + " is denied");
        }

        return knowledgeBaseDocument;
    }

    private void checkChunkRole(long chunkId, String role) {
        KnowledgeBaseDocumentChunk knowledgeBaseDocumentChunk =
            knowledgeBaseDocumentChunkService.getKnowledgeBaseDocumentChunk(chunkId);

        Long documentId = knowledgeBaseDocumentChunk.getKnowledgeBaseDocumentId();

        if (documentId == null) {
            throw new AccessDeniedException("Access to knowledge base document chunk " + chunkId + " is denied");
        }

        checkDocumentRole(documentId, role);
    }
}
