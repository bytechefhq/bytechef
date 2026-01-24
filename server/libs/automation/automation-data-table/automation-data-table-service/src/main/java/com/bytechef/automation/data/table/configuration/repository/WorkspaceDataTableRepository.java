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

package com.bytechef.automation.data.table.configuration.repository;

import com.bytechef.automation.data.table.configuration.domain.WorkspaceDataTable;
import java.util.List;
import org.springframework.data.repository.ListCrudRepository;

/**
 * Repository for mapping workspaces to data tables.
 *
 * @author Ivica Cardic
 */
public interface WorkspaceDataTableRepository extends ListCrudRepository<WorkspaceDataTable, Long> {

    List<WorkspaceDataTable> findAllByWorkspaceId(Long workspaceId);

    List<WorkspaceDataTable> findByDataTableId(Long dataTableId);

    WorkspaceDataTable findByWorkspaceIdAndDataTableId(Long workspaceId, Long dataTableId);
}
