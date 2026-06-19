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

package com.bytechef.automation.data.table.security;

import com.bytechef.automation.configuration.security.ResourceOwnershipResolver;
import com.bytechef.automation.data.table.configuration.domain.WorkspaceDataTable;
import com.bytechef.automation.data.table.configuration.repository.WorkspaceDataTableRepository;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Maps a data-table id to its owning workspace via the {@code workspace_data_table} relation. Data tables are
 * collaborative, workspace-scoped resources: EE authorizes by the caller's workspace role; CE is permissive (shared
 * within the single workspace). Fails closed when the table is not mapped to any workspace.
 *
 * @author Ivica Cardic
 */
@Component
public class DataTableOwnershipResolver implements ResourceOwnershipResolver {

    private final WorkspaceDataTableRepository workspaceDataTableRepository;

    @SuppressFBWarnings("EI")
    public DataTableOwnershipResolver(WorkspaceDataTableRepository workspaceDataTableRepository) {
        this.workspaceDataTableRepository = workspaceDataTableRepository;
    }

    @Override
    public String resourceType() {
        return "DataTable";
    }

    @Override
    public ResourceOwner resolveOwner(long id) {
        List<WorkspaceDataTable> workspaceDataTables = workspaceDataTableRepository.findByDataTableId(id);

        if (workspaceDataTables.isEmpty()) {
            return ResourceOwner.unknown();
        }

        WorkspaceDataTable workspaceDataTable = workspaceDataTables.getFirst();

        return ResourceOwner.ofWorkspace(workspaceDataTable.getWorkspaceId());
    }
}
