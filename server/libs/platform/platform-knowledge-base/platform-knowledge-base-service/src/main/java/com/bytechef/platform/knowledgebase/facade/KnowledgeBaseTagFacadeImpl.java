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

package com.bytechef.platform.knowledgebase.facade;

import com.bytechef.platform.knowledgebase.domain.KnowledgeBase;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseService;
import com.bytechef.platform.tag.domain.Tag;
import com.bytechef.platform.tag.service.TagService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * @author Ivica Cardic
 */
@Service
@ConditionalOnProperty(prefix = "bytechef.ai.knowledge-base", name = "enabled", havingValue = "true")
public class KnowledgeBaseTagFacadeImpl implements KnowledgeBaseTagFacade {

    private final KnowledgeBaseService knowledgeBaseService;
    private final TagService tagService;

    @SuppressFBWarnings("EI")
    public KnowledgeBaseTagFacadeImpl(KnowledgeBaseService knowledgeBaseService, TagService tagService) {
        this.knowledgeBaseService = knowledgeBaseService;
        this.tagService = tagService;
    }

    @Override
    public List<Tag> getTags(List<Long> knowledgeBaseIds) {
        Set<Long> tagIds = new HashSet<>();

        for (KnowledgeBase knowledgeBase : knowledgeBaseService.getKnowledgeBases(knowledgeBaseIds)) {
            List<Long> knowledgeBaseTagIds = knowledgeBase.getTagIds();

            if (knowledgeBaseTagIds != null) {
                tagIds.addAll(knowledgeBaseTagIds);
            }
        }

        if (tagIds.isEmpty()) {
            return List.of();
        }

        return tagService.getTags(new ArrayList<>(tagIds));
    }

    @Override
    public Map<Long, List<Tag>> getTagsByKnowledgeBaseId(List<Long> knowledgeBaseIds) {
        Map<Long, List<Tag>> tagsByKnowledgeBaseId = new HashMap<>();

        for (KnowledgeBase knowledgeBase : knowledgeBaseService.getKnowledgeBases(knowledgeBaseIds)) {
            List<Long> tagIds = knowledgeBase.getTagIds();

            tagsByKnowledgeBaseId.put(
                knowledgeBase.getId(), tagIds == null || tagIds.isEmpty() ? List.of() : tagService.getTags(tagIds));
        }

        return tagsByKnowledgeBaseId;
    }

    @Override
    public void updateTags(long knowledgeBaseId, List<Tag> tags) {
        List<Tag> resolvedTags = resolveTags(tags);

        List<Long> tagIds = resolvedTags.stream()
            .map(Tag::getId)
            .toList();

        knowledgeBaseService.updateKnowledgeBaseTagIds(knowledgeBaseId, tagIds);
    }

    private List<Tag> resolveTags(List<Tag> tags) {
        if (tags == null || tags.isEmpty()) {
            return List.of();
        }

        List<Tag> tagsToSave = tags.stream()
            .filter(tag -> tag.getId() == null)
            .toList();

        if (tagsToSave.isEmpty()) {
            return tags;
        }

        List<Tag> savedTags = tagService.save(tagsToSave);

        List<Tag> resolvedTags = new ArrayList<>(tags.size());
        int savedTagIndex = 0;

        for (Tag tag : tags) {
            if (tag.getId() == null) {
                resolvedTags.add(savedTags.get(savedTagIndex++));
            } else {
                resolvedTags.add(tag);
            }
        }

        return resolvedTags;
    }
}
