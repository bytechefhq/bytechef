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
import com.bytechef.automation.data.table.configuration.repository.WorkspaceDataTableRepository;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional
public class WorkspaceDataTableServiceImpl implements WorkspaceDataTableService {

    private final WorkspaceDataTableRepository workspaceDataTableRepository;

    @SuppressFBWarnings("EI")
    public WorkspaceDataTableServiceImpl(WorkspaceDataTableRepository workspaceDataTableRepository) {
        this.workspaceDataTableRepository = workspaceDataTableRepository;
    }

    @Override
    public void assignDataTableToWorkspace(Long dataTableId, Long workspaceId) {
        WorkspaceDataTable existing =
            workspaceDataTableRepository.findByWorkspaceIdAndDataTableId(workspaceId, dataTableId);

        if (existing == null) {
            workspaceDataTableRepository.save(new WorkspaceDataTable(dataTableId, workspaceId));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkspaceDataTable> getWorkspaceDataTables(Long workspaceId) {
        return workspaceDataTableRepository.findAllByWorkspaceId(workspaceId);
    }

    @Override
    public void removeDataTableFromWorkspace(Long dataTableId) {
        List<WorkspaceDataTable> workspaceDataTables = workspaceDataTableRepository.findByDataTableId(dataTableId);

        if (!workspaceDataTables.isEmpty()) {
            workspaceDataTableRepository.deleteAll(workspaceDataTables);
        }
    }
}
