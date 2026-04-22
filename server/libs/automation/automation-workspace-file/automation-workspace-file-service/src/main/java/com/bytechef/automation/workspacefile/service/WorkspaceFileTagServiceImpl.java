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

package com.bytechef.automation.workspacefile.service;

import com.bytechef.automation.workspacefile.domain.WorkspaceFile;
import com.bytechef.automation.workspacefile.repository.WorkspaceFileRepository;
import com.bytechef.platform.tag.domain.Tag;
import com.bytechef.platform.tag.service.TagService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional
@SuppressFBWarnings("EI2")
public class WorkspaceFileTagServiceImpl implements WorkspaceFileTagService {

    private final WorkspaceFileRepository workspaceFileRepository;
    private final TagService tagService;

    public WorkspaceFileTagServiceImpl(WorkspaceFileRepository workspaceFileRepository, TagService tagService) {
        this.workspaceFileRepository = workspaceFileRepository;
        this.tagService = tagService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Tag> getAllTags() {
        Set<Long> tagIdSet = new HashSet<>();

        for (WorkspaceFile workspaceFile : workspaceFileRepository.findAll()) {
            List<Long> tagIds = workspaceFile.getTagIds();

            if (tagIds != null) {
                tagIdSet.addAll(tagIds);
            }
        }

        if (tagIdSet.isEmpty()) {
            return List.of();
        }

        return tagService.getTags(new ArrayList<>(tagIdSet));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, List<Tag>> getTagsByFileName() {
        Map<String, List<Tag>> tagsByFileName = new HashMap<>();

        for (WorkspaceFile workspaceFile : workspaceFileRepository.findAll()) {
            List<Long> tagIds = workspaceFile.getTagIds();

            if (tagIds == null || tagIds.isEmpty()) {
                tagsByFileName.put(workspaceFile.getName(), List.of());
            } else {
                tagsByFileName.put(workspaceFile.getName(), tagService.getTags(tagIds));
            }
        }

        return tagsByFileName;
    }

    @Override
    public void updateTags(long workspaceFileId, List<Tag> tags) {
        WorkspaceFile workspaceFile = workspaceFileRepository.findById(workspaceFileId)
            .orElseThrow(
                () -> new IllegalArgumentException("WorkspaceFile %d not found".formatted(workspaceFileId)));

        List<Tag> resolvedTags;

        if (tags == null || tags.isEmpty()) {
            resolvedTags = List.of();
        } else {
            List<Tag> tagsToSave = tags.stream()
                .filter(tag -> tag.getId() == null)
                .toList();

            if (!tagsToSave.isEmpty()) {
                List<Tag> savedTags = tagService.save(tagsToSave);

                List<Tag> mergedTags = new ArrayList<>(tags);
                int savedIndex = 0;

                for (int i = 0; i < mergedTags.size(); i++) {
                    Tag tag = mergedTags.get(i);

                    if (tag.getId() == null) {
                        mergedTags.set(i, savedTags.get(savedIndex++));
                    }
                }

                resolvedTags = mergedTags;
            } else {
                resolvedTags = tags;
            }
        }

        workspaceFile.setTags(resolvedTags);

        workspaceFileRepository.save(workspaceFile);
    }
}
