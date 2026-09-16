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

package com.bytechef.component.datatable.action;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition.PerformFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.platform.component.definition.ActionContextAware;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.data.table.configuration.domain.DataTable;
import com.bytechef.platform.data.table.configuration.domain.DataTableInfo;
import com.bytechef.platform.data.table.configuration.exception.DataTableException;
import com.bytechef.platform.data.table.configuration.service.DataTableService;
import com.bytechef.platform.data.table.domain.DataTableRef;
import com.bytechef.platform.data.table.domain.DataTableWorkspaceResolver;
import com.bytechef.platform.data.table.execution.service.DataTableRowService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

abstract class AbstractDataTableActionTest {

    protected static final String TABLE_NAME = "conversations";
    protected static final long ENVIRONMENT_ID = 1;
    protected static final long JOB_PRINCIPAL_ID = 1051;
    protected static final long DATA_TABLE_ID = 1051;
    protected static final long WORKSPACE_ID = 7;
    protected static final String WORKFLOW_ID = "workflow-1";

    protected final DataTableRowService dataTableRowService = mock(DataTableRowService.class);
    protected final DataTableService dataTableService = mock(DataTableService.class);
    protected final DataTableWorkspaceResolver dataTableWorkspaceResolver = mock(DataTableWorkspaceResolver.class);

    @BeforeEach
    void beforeEach() {
        when(dataTableWorkspaceResolver.resolveByWorkflowId(WORKFLOW_ID)).thenReturn(OptionalLong.of(WORKSPACE_ID));
        when(dataTableWorkspaceResolver.resolveByJobPrincipalId(JOB_PRINCIPAL_ID, PlatformType.AUTOMATION))
            .thenReturn(OptionalLong.of(WORKSPACE_ID));
    }

    @Test
    void testPerformMissesATableThatExistsOnlyInAnotherWorkspace() {
        when(dataTableService.fetchDataTable(WORKSPACE_ID, "orders")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> perform(createActionDefinition(), createInputParameters("orders")))
            .isInstanceOf(DataTableException.class)
            .hasMessage("Data table 'orders' not found in this workspace");
    }

    protected abstract ModifiableActionDefinition createActionDefinition();

    protected abstract Map<String, Object> createInputParameters(String tableName);

    /**
     * Stubs the two lookups {@code DataTableUtils.resolveDataTable} makes -- the workspace-scoped name lookup and the
     * per-environment column metadata of the table it found -- and returns the ref every row statement is then
     * addressed with.
     */
    protected DataTableRef stubResolvedDataTable() {
        when(dataTableService.fetchDataTable(WORKSPACE_ID, TABLE_NAME))
            .thenReturn(Optional.of(new DataTable(DATA_TABLE_ID, TABLE_NAME)));
        when(dataTableService.fetchDataTableInfo(DATA_TABLE_ID, ENVIRONMENT_ID))
            .thenReturn(
                Optional.of(
                    new DataTableInfo(DATA_TABLE_ID, TABLE_NAME, WORKSPACE_ID, null, List.of(), Instant.EPOCH)));

        return new DataTableRef(DATA_TABLE_ID, ENVIRONMENT_ID);
    }

    protected static Object perform(
        ModifiableActionDefinition actionDefinition, Map<String, Object> inputParameters) throws Exception {

        PerformFunction performFunction = (PerformFunction) actionDefinition.getPerform()
            .orElseThrow();

        ActionContext actionContext = mock(
            ActionContext.class, withSettings().extraInterfaces(ActionContextAware.class));

        ActionContextAware actionContextAware = (ActionContextAware) actionContext;

        when(actionContextAware.getEnvironmentId()).thenReturn(ENVIRONMENT_ID);
        when(actionContextAware.getWorkflowId()).thenReturn(WORKFLOW_ID);

        Parameters parameters = MockParametersFactory.create(inputParameters);

        return performFunction.apply(parameters, MockParametersFactory.create(Map.of()), actionContext);
    }
}
