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
import com.bytechef.automation.knowledgebase.repository.KnowledgeBaseDocumentRepository;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.SequencedSet;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * @author Ivica Cardic
 */
@Service
@ConditionalOnProperty(prefix = "bytechef.ai.knowledge-base", name = "enabled", havingValue = "true")
public class KnowledgeBaseDocumentTagServiceImpl implements KnowledgeBaseDocumentTagService {

    private final KnowledgeBaseDocumentRepository knowledgeBaseDocumentRepository;

    @SuppressFBWarnings("EI")
    public KnowledgeBaseDocumentTagServiceImpl(KnowledgeBaseDocumentRepository knowledgeBaseDocumentRepository) {
        this.knowledgeBaseDocumentRepository = knowledgeBaseDocumentRepository;
    }

    @Override
    public List<String> getAllTagNames() {
        SequencedSet<String> tagNames = new LinkedHashSet<>();

        for (KnowledgeBaseDocument document : knowledgeBaseDocumentRepository.findAll()) {
            List<String> documentTagNames = document.getTagNames();

            if (documentTagNames != null) {
                tagNames.addAll(documentTagNames);
            }
        }

        return new ArrayList<>(tagNames);
    }

    @Override
    public List<String> getTagNamesByKnowledgeBaseId(Long knowledgeBaseId) {
        SequencedSet<String> tagNames = new LinkedHashSet<>();

        for (KnowledgeBaseDocument document : knowledgeBaseDocumentRepository.findAllByKnowledgeBaseId(
            knowledgeBaseId)) {

            List<String> documentTagNames = document.getTagNames();

            if (documentTagNames != null) {
                tagNames.addAll(documentTagNames);
            }
        }

        return new ArrayList<>(tagNames);
    }

    @Override
    public Map<Long, List<String>> getTagNamesByKnowledgeBaseDocumentId() {
        Map<Long, List<String>> map = new HashMap<>();

        List<KnowledgeBaseDocument> documents = new ArrayList<>();

        knowledgeBaseDocumentRepository.findAll()
            .forEach(documents::add);

        for (KnowledgeBaseDocument document : documents) {
            List<String> tagNames = document.getTagNames();

            map.put(document.getId(), tagNames == null ? List.of() : tagNames);
        }

        return map;
    }

    @Override
    public Map<String, List<String>> getTagNamesByKnowledgeBaseDocumentName() {
        Map<String, List<String>> map = new HashMap<>();

        List<KnowledgeBaseDocument> documents = new ArrayList<>();

        knowledgeBaseDocumentRepository.findAll()
            .forEach(documents::add);

        for (KnowledgeBaseDocument document : documents) {
            List<String> tagNames = document.getTagNames();

            map.put(document.getName(), tagNames == null ? List.of() : tagNames);
        }

        return map;
    }

    @Override
    public void updateTagNames(long knowledgeBaseDocumentId, List<String> tagNames) {
        KnowledgeBaseDocument document = knowledgeBaseDocumentRepository.findById(knowledgeBaseDocumentId)
            .orElseThrow(
                () -> new IllegalArgumentException(
                    "KnowledgeBaseDocument with id=" + knowledgeBaseDocumentId + " not found"));

        document.setTagNames(tagNames == null ? List.of() : tagNames);

        knowledgeBaseDocumentRepository.save(document);
    }
}
