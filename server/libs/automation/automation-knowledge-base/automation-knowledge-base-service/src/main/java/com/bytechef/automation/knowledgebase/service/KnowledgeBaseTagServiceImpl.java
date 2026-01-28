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

import com.bytechef.automation.knowledgebase.domain.KnowledgeBase;
import com.bytechef.automation.knowledgebase.repository.KnowledgeBaseRepository;
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
import org.springframework.stereotype.Service;

/**
 * @author Ivica Cardic
 */
@Service
public class KnowledgeBaseTagServiceImpl implements KnowledgeBaseTagService {

    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final TagService tagService;

    @SuppressFBWarnings("EI")
    public KnowledgeBaseTagServiceImpl(KnowledgeBaseRepository knowledgeBaseRepository, TagService tagService) {
        this.knowledgeBaseRepository = knowledgeBaseRepository;
        this.tagService = tagService;
    }

    @Override
    public List<Tag> getAllTags() {
        Set<Long> ids = new HashSet<>();

        for (KnowledgeBase knowledgeBase : knowledgeBaseRepository.findAll()) {
            List<Long> tagIds = knowledgeBase.getTagIds();

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
    public Map<Long, List<Tag>> getTagsByKnowledgeBaseId() {
        Map<Long, List<Tag>> map = new HashMap<>();

        List<KnowledgeBase> knowledgeBases = new ArrayList<>();

        knowledgeBaseRepository.findAll()
            .forEach(knowledgeBases::add);

        Map<Long, List<Long>> tagIdsByKnowledgeBaseId = knowledgeBases.stream()
            .collect(Collectors.toMap(
                KnowledgeBase::getId,
                knowledgeBase -> knowledgeBase.getTagIds() == null ? List.of() : knowledgeBase.getTagIds()));

        for (Map.Entry<Long, List<Long>> entry : tagIdsByKnowledgeBaseId.entrySet()) {
            List<Long> ids = entry.getValue();

            map.put(entry.getKey(), ids == null || ids.isEmpty() ? List.of() : tagService.getTags(ids));
        }

        return map;
    }

    @Override
    public Map<String, List<Tag>> getTagsByKnowledgeBaseName() {
        Map<String, List<Tag>> map = new HashMap<>();

        List<KnowledgeBase> knowledgeBases = new ArrayList<>();

        knowledgeBaseRepository.findAll()
            .forEach(knowledgeBases::add);

        Map<String, List<Long>> idsByName = knowledgeBases.stream()
            .collect(Collectors.toMap(
                KnowledgeBase::getName,
                knowledgeBase -> knowledgeBase.getTagIds() == null ? List.of() : knowledgeBase.getTagIds()));

        for (Map.Entry<String, List<Long>> entry : idsByName.entrySet()) {
            List<Long> ids = entry.getValue();

            map.put(entry.getKey(), ids == null || ids.isEmpty() ? List.of() : tagService.getTags(ids));
        }

        return map;
    }

    @Override
    public void updateTags(long knowledgeBaseId, List<Tag> tags) {
        KnowledgeBase knowledgeBase = knowledgeBaseRepository.findById(knowledgeBaseId)
            .orElseThrow(() -> new IllegalArgumentException("KnowledgeBase with id=" + knowledgeBaseId + " not found"));

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

        knowledgeBase.setTags(resolvedTags);

        knowledgeBaseRepository.save(knowledgeBase);
    }
}
