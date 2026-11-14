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
import com.bytechef.platform.data.table.domain.DataTableWorkspaceResolver;
import com.bytechef.platform.data.table.execution.facade.DataTableRowFacade;
import com.bytechef.platform.data.table.execution.service.DataTableRowService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
abstract class AbstractDataTableActionTest {

    protected static final long DATA_TABLE_ID = 1051;
    protected static final long ENVIRONMENT_ID = 1;
    protected static final long JOB_PRINCIPAL_ID = 1052;
    protected static final String TABLE_NAME = "conversations";
    protected static final String WORKFLOW_ID = "workflow-1";
    protected static final long WORKSPACE_ID = 7;

    protected final DataTableRowFacade dataTableRowFacade = mock(DataTableRowFacade.class);
    protected final DataTableRowService dataTableRowService = mock(DataTableRowService.class);
    protected final DataTableService dataTableService = mock(DataTableService.class);
    protected final DataTableWorkspaceResolver dataTableWorkspaceResolver = mock(DataTableWorkspaceResolver.class);

    @BeforeEach
    void beforeEach() {
        when(dataTableWorkspaceResolver.resolveByWorkflowId(WORKFLOW_ID)).thenReturn(OptionalLong.of(WORKSPACE_ID));
        when(dataTableWorkspaceResolver.resolveByJobPrincipalId(JOB_PRINCIPAL_ID, PlatformType.AUTOMATION))
            .thenReturn(OptionalLong.of(WORKSPACE_ID));
        when(dataTableService.fetchDataTable(WORKSPACE_ID, TABLE_NAME))
            .thenReturn(Optional.of(new DataTable(DATA_TABLE_ID, TABLE_NAME)));
        when(dataTableService.fetchDataTableInfo(DATA_TABLE_ID, ENVIRONMENT_ID))
            .thenReturn(
                Optional.of(
                    new DataTableInfo(DATA_TABLE_ID, TABLE_NAME, WORKSPACE_ID, null, List.of(), Instant.EPOCH)));
    }

    @Test
    void testPerformMissesATableThatExistsOnlyInAnotherWorkspace() {
        when(dataTableService.fetchDataTable(WORKSPACE_ID, "orders")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> perform(createActionDefinition(), createInputParameters("orders")))
            .isInstanceOf(DataTableException.class)
            .hasMessage("Data table 'orders' not found in this workspace");
    }

    @Test
    void testPerformMissesATableWithoutAPhysicalTableInTheEnvironment() {
        when(dataTableService.fetchDataTableInfo(DATA_TABLE_ID, ENVIRONMENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> perform(createActionDefinition(), createInputParameters(TABLE_NAME)))
            .isInstanceOf(DataTableException.class)
            .hasMessage("Data table 'conversations' not found in this workspace");
    }

    @Test
    void testPerformResolvesTheWorkspaceFromTheJobWhenTheContextHasNoWorkflowId() throws Exception {
        assertThatThrownBy(
            () -> perform(createActionDefinition(), createInputParameters("orders"), null, JOB_PRINCIPAL_ID))
                .isInstanceOf(DataTableException.class)
                .hasMessage("Data table 'orders' not found in this workspace");
    }

    @Test
    void testPerformFailsWhenTheWorkspaceCannotBeResolved() {
        assertThatThrownBy(() -> perform(createActionDefinition(), createInputParameters(TABLE_NAME), null, null))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Unable to determine the workspace of this workflow");
    }

    protected abstract ModifiableActionDefinition createActionDefinition();

    protected abstract Map<String, Object> createInputParameters(String tableName);

    protected static Object perform(
        ModifiableActionDefinition actionDefinition, Map<String, Object> inputParameters) throws Exception {

        return perform(actionDefinition, inputParameters, WORKFLOW_ID, null);
    }

    private static Object perform(
        ModifiableActionDefinition actionDefinition, Map<String, Object> inputParameters,
        @Nullable String workflowId, @Nullable Long jobPrincipalId) throws Exception {

        PerformFunction performFunction = (PerformFunction) actionDefinition.getPerform()
            .orElseThrow();

        ActionContext actionContext = mock(
            ActionContext.class, withSettings().extraInterfaces(ActionContextAware.class));

        ActionContextAware actionContextAware = (ActionContextAware) actionContext;

        when(actionContextAware.getEnvironmentId()).thenReturn(ENVIRONMENT_ID);
        when(actionContextAware.getJobPrincipalId()).thenReturn(jobPrincipalId);
        when(actionContextAware.getPlatformType()).thenReturn(PlatformType.AUTOMATION);
        when(actionContextAware.getWorkflowId()).thenReturn(workflowId);

        Parameters parameters = MockParametersFactory.create(inputParameters);

        return performFunction.apply(parameters, MockParametersFactory.create(Map.of()), actionContext);
    }
}
