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

package com.bytechef.automation.data.table.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.automation.data.table.configuration.service.DataTableService;
import com.bytechef.automation.data.table.configuration.service.DataTableTagService;
import com.bytechef.platform.tag.domain.Tag;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/**
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnCoordinator
@SuppressFBWarnings("EI")
public class DataTableTagGraphQlController {

    private final DataTableTagService dataTableTagService;
    private final DataTableService dataTableService;

    @SuppressFBWarnings("EI")
    public DataTableTagGraphQlController(DataTableTagService dataTableTagService, DataTableService dataTableService) {
        this.dataTableTagService = dataTableTagService;
        this.dataTableService = dataTableService;
    }

    @QueryMapping
    public List<Tag> dataTableTags() {
        return dataTableTagService.getAllTags();
    }

    @QueryMapping
    public List<DataTableTagsEntry> dataTableTagsByTable() {
        Map<String, List<Tag>> tagsByTableName = dataTableTagService.getTagsByTableName();

        return tagsByTableName.entrySet()
            .stream()
            .map(entry -> new DataTableTagsEntry(dataTableService.getIdByBaseName(entry.getKey()), entry.getValue()))
            .toList();
    }

    @MutationMapping
    public boolean updateDataTableTags(@Argument UpdateDataTableTagsInput input) {
        List<Tag> tags = input.tags() == null ? List.of() : input.tags()
            .stream()
            .map(tagInput -> {
                Tag tag = new Tag();

                if (tagInput.id() != null) {
                    tag.setId(tagInput.id());
                }

                tag.setName(tagInput.name());

                return tag;
            })
            .collect(Collectors.toList());

        dataTableTagService.updateTags(input.tableId(), tags);

        return true;
    }

    public record DataTableTagsEntry(Long tableId, List<Tag> tags) {
    }

    public record UpdateDataTableTagsInput(Long tableId, List<TagInput> tags) {
    }

    public record TagInput(Long id, String name) {
    }
}
