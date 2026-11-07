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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ai.copilot.tool.context.AgentToolInvocationContext;
import com.bytechef.automation.data.table.configuration.facade.WorkspaceDataTableFacade;
import com.bytechef.platform.data.table.configuration.domain.DataTableInfo;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;

/**
 * @author Ivica Cardic
 */
class CloneDataTableToolCallbackTest {

    private static final long ENVIRONMENT_ID = 0L;
    private static final long SOURCE_DATA_TABLE_ID = 42L;
    private static final long WORKSPACE_ID = 7L;

    private final WorkspaceDataTableFacade workspaceDataTableFacade = mock(WorkspaceDataTableFacade.class);

    private final CloneDataTableToolCallback toolCallback =
        new CloneDataTableToolCallback(workspaceDataTableFacade);

    @Test
    void testCloningAssignsTheCopyToTheWorkspace() {
        givenTheSourceTableIsInTheWorkspace();

        when(workspaceDataTableFacade.getDataTableId("orders_test")).thenReturn(99L);

        String result = toolCallback.call(cloneInput("orders_test"), toolContext());

        verify(workspaceDataTableFacade).duplicateTable(SOURCE_DATA_TABLE_ID, "orders_test", ENVIRONMENT_ID);

        assertThat(result).contains("\"dataTableId\":99");
        assertThat(result).contains("orders_test");
    }

    @Test
    void testACollidingNameIsSurfacedVerbatim() {
        givenTheSourceTableIsInTheWorkspace();

        doThrow(new IllegalArgumentException("Table already exists: orders_test"))
            .when(workspaceDataTableFacade)
            .duplicateTable(SOURCE_DATA_TABLE_ID, "orders_test", ENVIRONMENT_ID);

        String result = toolCallback.call(cloneInput("orders_test"), toolContext());

        assertThat(result).contains("Table already exists: orders_test");
    }

    @Test
    void testATableOutsideTheWorkspaceIsRefused() {
        when(workspaceDataTableFacade.listTables(WORKSPACE_ID, ENVIRONMENT_ID)).thenReturn(List.of());

        String result = toolCallback.call(cloneInput("orders_test"), toolContext());

        assertThat(result).contains("not found in the current workspace");

        verify(workspaceDataTableFacade, never()).duplicateTable(anyLong(), anyString(), anyLong());
    }

    private void givenTheSourceTableIsInTheWorkspace() {
        when(workspaceDataTableFacade.listTables(WORKSPACE_ID, ENVIRONMENT_ID))
            .thenReturn(
                List.of(new DataTableInfo(SOURCE_DATA_TABLE_ID, "orders", null, List.of(), Instant.now())));
    }

    private static String cloneInput(String newBaseName) {
        return "{\"dataTableId\": \"" + SOURCE_DATA_TABLE_ID + "\", \"newBaseName\": \"" + newBaseName + "\"}";
    }

    private static ToolContext toolContext() {
        return new ToolContext(
            AgentToolInvocationContext.builder()
                .environmentId(ENVIRONMENT_ID)
                .workspaceId(WORKSPACE_ID)
                .build()
                .toToolContext());
    }
}
