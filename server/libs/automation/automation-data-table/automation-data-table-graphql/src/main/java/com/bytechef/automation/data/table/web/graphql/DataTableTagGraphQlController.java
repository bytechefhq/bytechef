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
import com.bytechef.automation.data.table.configuration.facade.WorkspaceDataTableFacade;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.data.table.configuration.domain.DataTableInfo;
import com.bytechef.platform.data.table.configuration.service.DataTableTagService;
import com.bytechef.platform.tag.domain.Tag;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

/**
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnCoordinator
@SuppressFBWarnings("EI")
public class DataTableTagGraphQlController {

    private final DataTableTagService dataTableTagService;
    private final EnvironmentService environmentService;
    private final WorkspaceDataTableFacade workspaceDataTableFacade;

    @SuppressFBWarnings("EI")
    public DataTableTagGraphQlController(
        DataTableTagService dataTableTagService, EnvironmentService environmentService,
        WorkspaceDataTableFacade workspaceDataTableFacade) {

        this.dataTableTagService = dataTableTagService;
        this.environmentService = environmentService;
        this.workspaceDataTableFacade = workspaceDataTableFacade;
    }

    @QueryMapping
    @PreAuthorize("hasWorkspaceScopeInEnvironmentId(#workspaceId, 'DATA_TABLE_VIEW', #environmentId)")
    public List<Tag> dataTableTags(@Argument Long environmentId, @Argument Long workspaceId) {
        Map<Long, List<Tag>> tagsByTableId = getTagsByTableId(environmentId, workspaceId);

        return tagsByTableId.values()
            .stream()
            .flatMap(List::stream)
            .distinct()
            .toList();
    }

    @QueryMapping
    @PreAuthorize("hasWorkspaceScopeInEnvironmentId(#workspaceId, 'DATA_TABLE_VIEW', #environmentId)")
    public List<DataTableTagsEntry> dataTableTagsByTable(@Argument Long environmentId, @Argument Long workspaceId) {
        Map<Long, List<Tag>> tagsByTableId = getTagsByTableId(environmentId, workspaceId);

        return tagsByTableId.entrySet()
            .stream()
            .map(entry -> new DataTableTagsEntry(entry.getKey(), entry.getValue()))
            .toList();
    }

    @MutationMapping
    @PreAuthorize("hasPermission(#input.tableId, 'DataTable', 'DATA_TABLE_EDIT')")
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

    private Map<Long, List<Tag>> getTagsByTableId(Long environmentId, Long workspaceId) {
        Environment environment = environmentService.getEnvironment(environmentId);

        Map<String, Long> tableIdsByBaseName = new HashMap<>();

        for (DataTableInfo dataTableInfo : workspaceDataTableFacade.listTables(workspaceId, environment.ordinal())) {
            if (dataTableInfo.id() != null) {
                tableIdsByBaseName.put(dataTableInfo.baseName(), dataTableInfo.id());
            }
        }

        Map<String, List<Tag>> tagsByTableName = dataTableTagService.getTagsByTableName();
        Map<Long, List<Tag>> tagsByTableId = new LinkedHashMap<>();

        for (Map.Entry<String, List<Tag>> entry : tagsByTableName.entrySet()) {
            Long tableId = tableIdsByBaseName.get(entry.getKey());

            if (tableId != null) {
                tagsByTableId.put(tableId, entry.getValue());
            }
        }

        return tagsByTableId;
    }

    public record DataTableTagsEntry(Long tableId, List<Tag> tags) {
    }

    public record UpdateDataTableTagsInput(Long tableId, List<TagInput> tags) {
    }

    public record TagInput(Long id, String name) {
    }
}
