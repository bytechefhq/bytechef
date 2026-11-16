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

import com.bytechef.platform.knowledgebase.domain.KnowledgeBaseDocument;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBaseDocumentChunk;
import com.bytechef.platform.knowledgebase.dto.DocumentStatusUpdate;
import com.bytechef.platform.knowledgebase.facade.KnowledgeBaseDocumentChunkFacade;
import com.bytechef.platform.knowledgebase.facade.KnowledgeBaseDocumentFacade;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseDocumentService;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseDocumentTagService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional
@ConditionalOnProperty(prefix = "bytechef.ai.knowledge-base", name = "enabled", havingValue = "true")
public class WorkspaceKnowledgeBaseDocumentFacadeImpl implements WorkspaceKnowledgeBaseDocumentFacade {

    private final KnowledgeBaseDocumentChunkFacade knowledgeBaseDocumentChunkFacade;
    private final KnowledgeBaseDocumentFacade knowledgeBaseDocumentFacade;
    private final KnowledgeBaseDocumentService knowledgeBaseDocumentService;
    private final KnowledgeBaseDocumentTagService knowledgeBaseDocumentTagService;

    @SuppressFBWarnings("EI")
    public WorkspaceKnowledgeBaseDocumentFacadeImpl(
        KnowledgeBaseDocumentChunkFacade knowledgeBaseDocumentChunkFacade,
        KnowledgeBaseDocumentFacade knowledgeBaseDocumentFacade,
        KnowledgeBaseDocumentService knowledgeBaseDocumentService,
        KnowledgeBaseDocumentTagService knowledgeBaseDocumentTagService) {

        this.knowledgeBaseDocumentChunkFacade = knowledgeBaseDocumentChunkFacade;
        this.knowledgeBaseDocumentFacade = knowledgeBaseDocumentFacade;
        this.knowledgeBaseDocumentService = knowledgeBaseDocumentService;
        this.knowledgeBaseDocumentTagService = knowledgeBaseDocumentTagService;
    }

    @Override
    @PreAuthorize("hasPermission(#knowledgeBaseDocumentId, 'KnowledgeBaseDocument', 'KNOWLEDGE_BASE_EDIT')")
    public void deleteKnowledgeBaseDocument(long knowledgeBaseDocumentId) {
        knowledgeBaseDocumentFacade.deleteKnowledgeBaseDocument(knowledgeBaseDocumentId);
    }

    @Override
    @PreAuthorize("hasPermission(#knowledgeBaseDocumentChunkId, 'KnowledgeBaseDocumentChunk', 'KNOWLEDGE_BASE_EDIT')")
    public void deleteKnowledgeBaseDocumentChunk(long knowledgeBaseDocumentChunkId) {
        knowledgeBaseDocumentChunkFacade.deleteKnowledgeBaseDocumentChunk(knowledgeBaseDocumentChunkId);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasPermission(#knowledgeBaseDocumentId, 'KnowledgeBaseDocument', 'KNOWLEDGE_BASE_VIEW')")
    public KnowledgeBaseDocument getKnowledgeBaseDocument(long knowledgeBaseDocumentId) {
        return knowledgeBaseDocumentService.getKnowledgeBaseDocument(knowledgeBaseDocumentId);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasPermission(#knowledgeBaseDocumentId, 'KnowledgeBaseDocument', 'KNOWLEDGE_BASE_VIEW')")
    public List<KnowledgeBaseDocumentChunk> getKnowledgeBaseDocumentChunks(long knowledgeBaseDocumentId) {
        return knowledgeBaseDocumentChunkFacade.getKnowledgeBaseDocumentChunksByDocumentId(knowledgeBaseDocumentId);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasPermission(#knowledgeBaseDocumentId, 'KnowledgeBaseDocument', 'KNOWLEDGE_BASE_VIEW')")
    public DocumentStatusUpdate getKnowledgeBaseDocumentStatus(long knowledgeBaseDocumentId) {
        return knowledgeBaseDocumentService.getKnowledgeBaseDocumentStatus(knowledgeBaseDocumentId);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasPermission(#knowledgeBaseId, 'KnowledgeBase', 'KNOWLEDGE_BASE_VIEW')")
    public List<String> getKnowledgeBaseDocumentTags(long knowledgeBaseId) {
        return knowledgeBaseDocumentTagService.getTagNamesByKnowledgeBaseId(knowledgeBaseId);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasPermission(#knowledgeBaseId, 'KnowledgeBase', 'KNOWLEDGE_BASE_VIEW')")
    public Map<Long, List<String>> getKnowledgeBaseDocumentTagsByDocumentId(long knowledgeBaseId) {
        return knowledgeBaseDocumentTagService.getTagNamesByKnowledgeBaseDocumentId(knowledgeBaseId);
    }

    @Override
    @PreAuthorize("hasPermission(#knowledgeBaseDocumentChunkId, 'KnowledgeBaseDocumentChunk', 'KNOWLEDGE_BASE_EDIT')")
    public KnowledgeBaseDocumentChunk updateKnowledgeBaseDocumentChunk(
        long knowledgeBaseDocumentChunkId, String content) {

        return knowledgeBaseDocumentChunkFacade.updateKnowledgeBaseDocumentChunk(knowledgeBaseDocumentChunkId, content);
    }

    @Override
    @PreAuthorize("hasPermission(#knowledgeBaseDocumentId, 'KnowledgeBaseDocument', 'KNOWLEDGE_BASE_EDIT')")
    public void updateKnowledgeBaseDocumentTags(long knowledgeBaseDocumentId, List<String> tagNames) {
        knowledgeBaseDocumentFacade.updateKnowledgeBaseDocumentTags(knowledgeBaseDocumentId, tagNames);
    }
}
