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
import com.bytechef.platform.tag.domain.Tag;
import com.bytechef.platform.tag.service.TagService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * @author Ivica Cardic
 */
@Service
@ConditionalOnProperty(prefix = "bytechef.knowledge-base", name = "enabled", havingValue = "true")
public class KnowledgeBaseDocumentTagServiceImpl implements KnowledgeBaseDocumentTagService {

    private final KnowledgeBaseDocumentRepository knowledgeBaseDocumentRepository;
    private final TagService tagService;

    @SuppressFBWarnings("EI")
    public KnowledgeBaseDocumentTagServiceImpl(
        KnowledgeBaseDocumentRepository knowledgeBaseDocumentRepository, TagService tagService) {

        this.knowledgeBaseDocumentRepository = knowledgeBaseDocumentRepository;
        this.tagService = tagService;
    }

    @Override
    public List<Tag> getAllTags() {
        Set<Long> ids = new HashSet<>();

        for (KnowledgeBaseDocument document : knowledgeBaseDocumentRepository.findAll()) {
            List<Long> tagIds = document.getTagIds();

            if (tagIds != null) {
                ids.addAll(tagIds);
            }
        }

        if (ids.isEmpty()) {
            return List.of();
        }

        return tagService.getTags(new ArrayList<>(ids));
    }

    @Override
    public Map<Long, List<Tag>> getTagsByKnowledgeBaseDocumentId() {
        Map<Long, List<Tag>> map = new HashMap<>();

        List<KnowledgeBaseDocument> documents = new ArrayList<>();

        knowledgeBaseDocumentRepository.findAll()
            .forEach(documents::add);

        Map<Long, List<Long>> tagIdsByDocumentId = documents.stream()
            .collect(Collectors.toMap(
                KnowledgeBaseDocument::getId,
                document -> document.getTagIds() == null ? List.of() : document.getTagIds()));

        for (Map.Entry<Long, List<Long>> entry : tagIdsByDocumentId.entrySet()) {
            List<Long> ids = entry.getValue();

            map.put(entry.getKey(), ids == null || ids.isEmpty() ? List.of() : tagService.getTags(ids));
        }

        return map;
    }

    @Override
    public Map<String, List<Tag>> getTagsByKnowledgeBaseDocumentName() {
        Map<String, List<Tag>> map = new HashMap<>();

        List<KnowledgeBaseDocument> documents = new ArrayList<>();

        knowledgeBaseDocumentRepository.findAll()
            .forEach(documents::add);

        Map<String, List<Long>> idsByName = documents.stream()
            .collect(Collectors.toMap(
                KnowledgeBaseDocument::getName,
                document -> document.getTagIds() == null ? List.of() : document.getTagIds()));

        for (Map.Entry<String, List<Long>> entry : idsByName.entrySet()) {
            List<Long> ids = entry.getValue();

            map.put(entry.getKey(), ids == null || ids.isEmpty() ? List.of() : tagService.getTags(ids));
        }

        return map;
    }

    @Override
    public void updateTags(long knowledgeBaseDocumentId, List<Tag> tags) {
        KnowledgeBaseDocument document = knowledgeBaseDocumentRepository.findById(knowledgeBaseDocumentId)
            .orElseThrow(
                () -> new IllegalArgumentException(
                    "KnowledgeBaseDocument with id=" + knowledgeBaseDocumentId + " not found"));

        List<Tag> resolvedTags;

        if (tags == null || tags.isEmpty()) {
            resolvedTags = List.of();
        } else {
            List<Tag> tagsToSave = tags.stream()
                .filter(tag -> tag.getId() == null)
                .toList();

            if (!tagsToSave.isEmpty()) {
                List<Tag> savedTags = tagService.save(tagsToSave);

                List<Tag> withIds = new ArrayList<>(tags);
                int index = 0;

                for (int i = 0; i < withIds.size(); i++) {
                    Tag tag = withIds.get(i);

                    if (tag.getId() == null) {
                        withIds.set(i, savedTags.get(index++));
                    }
                }

                resolvedTags = withIds;
            } else {
                resolvedTags = tags;
            }
        }

        document.setTags(resolvedTags);

        knowledgeBaseDocumentRepository.save(document);
    }
}
