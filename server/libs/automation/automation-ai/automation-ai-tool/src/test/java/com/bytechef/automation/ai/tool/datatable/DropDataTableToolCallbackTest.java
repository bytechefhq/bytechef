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
import static org.mockito.Mockito.verify;

import com.bytechef.ai.copilot.tool.context.AgentToolInvocationContext;
import com.bytechef.automation.data.table.configuration.facade.WorkspaceDataTableFacade;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;

/**
 *
 * @author Ivica Cardic
 */
class DropDataTableToolCallbackTest {

    private static ToolContext toolContextWithEnvironment() {
        return new ToolContext(
            AgentToolInvocationContext.builder()
                .environmentId(0L)
                .build()
                .toToolContext());
    }

    @Test
    void dropsDataTableById() {
        WorkspaceDataTableFacade facade = mock(WorkspaceDataTableFacade.class);
        DropDataTableToolCallback toolCallback = new DropDataTableToolCallback(facade);

        String result = toolCallback.call("{\"id\": 42}", toolContextWithEnvironment());

        verify(facade).dropTable(42L, 0L);
        assertThat(result).contains("\"deleted\":true");
    }

    @Test
    void rejectsMissingId() {
        WorkspaceDataTableFacade facade = mock(WorkspaceDataTableFacade.class);
        DropDataTableToolCallback toolCallback = new DropDataTableToolCallback(facade);

        String result = toolCallback.call("{}", toolContextWithEnvironment());

        assertThat(result).contains("id is required");
    }

    @Test
    void surfacesFacadeIllegalArgumentExceptionAsToolError() {
        WorkspaceDataTableFacade facade = mock(WorkspaceDataTableFacade.class);

        doThrow(new IllegalArgumentException("Data table not found"))
            .when(facade)
            .dropTable(42L, 0L);

        DropDataTableToolCallback toolCallback = new DropDataTableToolCallback(facade);

        String result = toolCallback.call("{\"id\": 42}", toolContextWithEnvironment());

        assertThat(result).contains("Data table not found");
    }
}
