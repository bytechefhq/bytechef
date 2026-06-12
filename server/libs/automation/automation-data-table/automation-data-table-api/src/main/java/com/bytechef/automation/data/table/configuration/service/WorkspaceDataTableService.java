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

import com.bytechef.automation.data.table.configuration.domain.WorkspaceDataTable;
import java.util.List;

/**
 * Manages the workspace &harr; data-table relation rows. Workspace scoping for the platform {@code DataTable} domain
 * lives entirely here.
 *
 * @author Ivica Cardic
 */
public interface WorkspaceDataTableService {

    void assignDataTableToWorkspace(Long dataTableId, Long workspaceId);

    List<WorkspaceDataTable> getWorkspaceDataTables(Long workspaceId);

    void removeDataTableFromWorkspace(Long dataTableId);
}
