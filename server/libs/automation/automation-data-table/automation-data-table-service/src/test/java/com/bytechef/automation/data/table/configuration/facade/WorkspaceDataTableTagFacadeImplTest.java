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

package com.bytechef.automation.data.table.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.data.table.configuration.domain.WorkspaceDataTable;
import com.bytechef.automation.data.table.configuration.service.WorkspaceDataTableService;
import com.bytechef.platform.data.table.configuration.service.DataTableTagService;
import com.bytechef.platform.tag.domain.Tag;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class WorkspaceDataTableTagFacadeImplTest {

    @Mock
    private DataTableTagService dataTableTagService;

    @Mock
    private WorkspaceDataTableService workspaceDataTableService;

    @InjectMocks
    private WorkspaceDataTableFacadeImpl workspaceDataTableFacade;

    @Test
    void testGetDataTableTagsScopesToWorkspace() {
        when(workspaceDataTableService.getWorkspaceDataTables(5L))
            .thenReturn(List.of(workspaceDataTable(1L), workspaceDataTable(2L)));
        when(dataTableTagService.getTags(List.of(1L, 2L))).thenReturn(List.of(new Tag("a"), new Tag("b")));

        List<Tag> tags = workspaceDataTableFacade.getDataTableTags(5L);

        assertThat(tags).hasSize(2);

        verify(dataTableTagService).getTags(List.of(1L, 2L));
    }

    private static WorkspaceDataTable workspaceDataTable(long dataTableId) {
        WorkspaceDataTable workspaceDataTable = new WorkspaceDataTable();

        workspaceDataTable.setDataTableId(dataTableId);

        return workspaceDataTable;
    }
}
