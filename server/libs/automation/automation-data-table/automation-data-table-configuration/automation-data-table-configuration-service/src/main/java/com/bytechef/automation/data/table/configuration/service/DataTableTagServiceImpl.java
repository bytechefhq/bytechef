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

package com.bytechef.automation.data.table.configuration.service;

import com.bytechef.automation.data.table.configuration.domain.DataTable;
import com.bytechef.automation.data.table.configuration.repository.DataTableRepository;
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
public class DataTableTagServiceImpl implements DataTableTagService {

    private final DataTableRepository dataTableRepository;
    private final TagService tagService;

    @SuppressFBWarnings("EI")
    public DataTableTagServiceImpl(DataTableRepository dataTableRepository, TagService tagService) {
        this.dataTableRepository = dataTableRepository;
        this.tagService = tagService;
    }

    @Override
    public List<Tag> getAllTags() {
        Set<Long> ids = new HashSet<>();

        for (DataTable dataTable : dataTableRepository.findAll()) {
            List<Long> tagIds = dataTable.getTagIds();

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
    public Map<String, List<Tag>> getTagsByTableName() {
        Map<String, List<Tag>> map = new HashMap<>();

        List<DataTable> dataTables = new ArrayList<>();

        dataTableRepository.findAll()
            .forEach(dataTables::add);

        Map<String, List<Long>> idsByName = dataTables.stream()
            .collect(Collectors.toMap(
                DataTable::getName,
                dataTable -> dataTable.getTagIds() == null ? List.of() : dataTable.getTagIds()));

        for (Map.Entry<String, List<Long>> entry : idsByName.entrySet()) {
            List<Long> ids = entry.getValue();

            map.put(entry.getKey(), ids == null || ids.isEmpty() ? List.of() : tagService.getTags(ids));
        }

        return map;
    }

    @Override
    public void updateTags(long tableId, List<Tag> tags) {
        DataTable dataTable = dataTableRepository.findById(tableId)
            .orElseThrow(() -> new IllegalArgumentException("Data table with id=" + tableId + " not found"));

        List<Tag> resolvedTags;

        if (tags == null || tags.isEmpty()) {
            resolvedTags = List.of();
        } else {
            List<Tag> tagsToSave = tags.stream()
                .filter(t -> t.getId() == null)
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

        dataTable.setTags(resolvedTags);

        dataTableRepository.save(dataTable);
    }
}
