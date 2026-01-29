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

package com.bytechef.automation.knowledgebase.service;

import com.bytechef.automation.knowledgebase.domain.KnowledgeBaseDocument;
import com.bytechef.automation.knowledgebase.dto.DocumentStatusUpdate;
import com.bytechef.automation.knowledgebase.repository.KnowledgeBaseDocumentRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class KnowledgeBaseDocumentServiceImpl implements KnowledgeBaseDocumentService {

    private final KnowledgeBaseDocumentRepository knowledgeBaseDocumentRepository;

    KnowledgeBaseDocumentServiceImpl(KnowledgeBaseDocumentRepository knowledgeBaseDocumentRepository) {
        this.knowledgeBaseDocumentRepository = knowledgeBaseDocumentRepository;
    }

    @Override
    public void delete(long id) {
        knowledgeBaseDocumentRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public KnowledgeBaseDocument getKnowledgeBaseDocument(long id) {
        return knowledgeBaseDocumentRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("KnowledgeBase document not found: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<KnowledgeBaseDocument> getKnowledgeBaseDocuments(long knowledgeBaseId) {
        return knowledgeBaseDocumentRepository.findAllByKnowledgeBaseId(knowledgeBaseId);
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentStatusUpdate getKnowledgeBaseDocumentStatus(long id) {
        KnowledgeBaseDocument document = getKnowledgeBaseDocument(id);

        return DocumentStatusUpdate.of(document.getId(), document.getStatus());
    }

    @Override
    public KnowledgeBaseDocument saveKnowledgeBaseDocument(KnowledgeBaseDocument knowledgeBaseDocument) {
        return knowledgeBaseDocumentRepository.save(knowledgeBaseDocument);
    }
}
