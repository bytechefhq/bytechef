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

import com.bytechef.automation.knowledgebase.domain.KnowledgeBaseDocumentChunk;
import com.bytechef.automation.knowledgebase.repository.KnowledgeBaseDocumentChunkRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional
@ConditionalOnProperty(prefix = "bytechef.knowledge-base", name = "enabled", havingValue = "true")
public class KnowledgeBaseDocumentChunkServiceImpl implements KnowledgeBaseDocumentChunkService {

    private final KnowledgeBaseDocumentChunkRepository knowledgeBaseDocumentChunkRepository;

    public KnowledgeBaseDocumentChunkServiceImpl(
        KnowledgeBaseDocumentChunkRepository knowledgeBaseDocumentChunkRepository) {

        this.knowledgeBaseDocumentChunkRepository = knowledgeBaseDocumentChunkRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public KnowledgeBaseDocumentChunk getKnowledgeBaseDocumentChunk(Long id) {
        return knowledgeBaseDocumentChunkRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("KnowledgeBase document chunk not found: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<KnowledgeBaseDocumentChunk> getKnowledgeBaseDocumentChunkByVectorStoreId(String vectorStoreId) {
        return knowledgeBaseDocumentChunkRepository.findByVectorStoreId(vectorStoreId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<KnowledgeBaseDocumentChunk> getKnowledgeBaseDocumentChunks(List<Long> ids) {
        return knowledgeBaseDocumentChunkRepository.findAllById(ids);
    }

    @Override
    @Transactional(readOnly = true)
    public List<KnowledgeBaseDocumentChunk> getKnowledgeBaseDocumentChunksByDocumentId(Long documentId) {
        return knowledgeBaseDocumentChunkRepository.findAllByKnowledgeBaseDocumentId(documentId);
    }

    @Override
    public KnowledgeBaseDocumentChunk saveKnowledgeBaseDocumentChunk(
        KnowledgeBaseDocumentChunk knowledgeBaseDocumentChunk) {

        return knowledgeBaseDocumentChunkRepository.save(knowledgeBaseDocumentChunk);
    }

    @Override
    public List<KnowledgeBaseDocumentChunk> saveKnowledgeBaseDocumentChunks(
        List<KnowledgeBaseDocumentChunk> knowledgeBaseDocumentChunks) {

        return knowledgeBaseDocumentChunkRepository.saveAll(knowledgeBaseDocumentChunks);
    }

    @Override
    public void deleteKnowledgeBaseDocumentChunk(Long id) {
        knowledgeBaseDocumentChunkRepository.deleteById(id);
    }

    @Override
    public void deleteKnowledgeBaseDocumentChunks(List<KnowledgeBaseDocumentChunk> knowledgeBaseDocumentChunks) {
        knowledgeBaseDocumentChunkRepository.deleteAll(knowledgeBaseDocumentChunks);
    }
}
