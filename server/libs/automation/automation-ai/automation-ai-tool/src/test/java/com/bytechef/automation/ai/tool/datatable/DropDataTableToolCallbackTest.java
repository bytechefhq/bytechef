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

package com.bytechef.automation.ai.tool.datatable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ai.copilot.tool.context.AgentToolInvocationContext;
import com.bytechef.automation.data.table.configuration.facade.WorkspaceDataTableFacade;
import com.bytechef.platform.data.table.configuration.domain.DataTableInfo;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.ai.chat.model.ToolContext;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class DropDataTableToolCallbackTest {

    private static ToolContext toolContext() {
        return new ToolContext(
            AgentToolInvocationContext.builder()
                .environmentId(0L)
                .workspaceId(1L)
                .build()
                .toToolContext());
    }

    private static ToolContext toolContextWithoutWorkspace() {
        return new ToolContext(
            AgentToolInvocationContext.builder()
                .environmentId(0L)
                .build()
                .toToolContext());
    }

    private static DataTableInfo dataTableInfo(long id) {
        return new DataTableInfo(id, "base_name_" + id, null, List.of(), null);
    }

    @Test
    void dropsDataTableOwnedByTheWorkspace() {
        WorkspaceDataTableFacade facade = mock(WorkspaceDataTableFacade.class);

        when(facade.listTables(1L, 0L)).thenReturn(List.of(dataTableInfo(42L)));

        DropDataTableToolCallback toolCallback = new DropDataTableToolCallback(facade);

        String result = toolCallback.call("{\"id\": 42}", toolContext());

        verify(facade).dropTable(42L, 0L);
        assertThat(result).contains("\"deleted\":true");
    }

    @Test
    void refusesDataTableOwnedByAnotherWorkspace() {
        WorkspaceDataTableFacade facade = mock(WorkspaceDataTableFacade.class);

        when(facade.listTables(1L, 0L)).thenReturn(List.of(dataTableInfo(7L)));

        DropDataTableToolCallback toolCallback = new DropDataTableToolCallback(facade);

        String result = toolCallback.call("{\"id\": 42}", toolContext());

        verify(facade, never()).dropTable(42L, 0L);
        assertThat(result).contains("Data table not found: 42");
    }

    @Test
    void rejectsMissingWorkspace() {
        WorkspaceDataTableFacade facade = mock(WorkspaceDataTableFacade.class);
        DropDataTableToolCallback toolCallback = new DropDataTableToolCallback(facade);

        String result = toolCallback.call("{\"id\": 42}", toolContextWithoutWorkspace());

        verify(facade, never()).dropTable(42L, 0L);
        assertThat(result).contains("Workspace context unavailable");
    }

    @Test
    void rejectsMissingId() {
        WorkspaceDataTableFacade facade = mock(WorkspaceDataTableFacade.class);
        DropDataTableToolCallback toolCallback = new DropDataTableToolCallback(facade);

        String result = toolCallback.call("{}", toolContext());

        assertThat(result).contains("id is required");
    }

    @Test
    void surfacesFacadeIllegalArgumentExceptionAsToolError() {
        WorkspaceDataTableFacade facade = mock(WorkspaceDataTableFacade.class);

        when(facade.listTables(1L, 0L)).thenReturn(List.of(dataTableInfo(42L)));

        doThrow(new IllegalArgumentException("Data table not found"))
            .when(facade)
            .dropTable(42L, 0L);

        DropDataTableToolCallback toolCallback = new DropDataTableToolCallback(facade);

        String result = toolCallback.call("{\"id\": 42}", toolContext());

        assertThat(result).contains("Data table not found");
    }
}
