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

package com.bytechef.component.datatable.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.platform.component.definition.ActionContextAware;
import com.bytechef.platform.component.definition.TriggerContextAware;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.data.table.configuration.domain.DataTableInfo;
import com.bytechef.platform.data.table.configuration.exception.DataTableException;
import com.bytechef.platform.data.table.configuration.service.DataTableService;
import com.bytechef.platform.data.table.domain.DataTableWorkspaceResolver;
import com.bytechef.platform.data.table.execution.domain.DataTableRow;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class DataTableUtilsTest {

    private final DataTableService dataTableService = mock(DataTableService.class);
    private final DataTableWorkspaceResolver dataTableWorkspaceResolver = mock(DataTableWorkspaceResolver.class);

    @Test
    void testResolveWorkspaceIdUsesTheWorkflowIdFirst() {
        ActionContextAware actionContext = mock(ActionContextAware.class);

        when(actionContext.getWorkflowId()).thenReturn("workflow-1");
        when(dataTableWorkspaceResolver.resolveByWorkflowId("workflow-1")).thenReturn(OptionalLong.of(7L));

        assertThat(DataTableUtils.resolveWorkspaceId(dataTableWorkspaceResolver, actionContext)).isEqualTo(7L);
    }

    @Test
    void testResolveWorkspaceIdFallsBackToTheJobPrincipal() {
        ActionContextAware actionContext = mock(ActionContextAware.class);

        when(actionContext.getJobPrincipalId()).thenReturn(1051L);
        when(actionContext.getPlatformType()).thenReturn(PlatformType.AUTOMATION);
        when(dataTableWorkspaceResolver.resolveByJobPrincipalId(1051L, PlatformType.AUTOMATION))
            .thenReturn(OptionalLong.of(8L));

        assertThat(DataTableUtils.resolveWorkspaceId(dataTableWorkspaceResolver, actionContext)).isEqualTo(8L);
    }

    @Test
    void testResolveWorkspaceIdFailsClosed() {
        ActionContextAware actionContext = mock(ActionContextAware.class);

        assertThatThrownBy(() -> DataTableUtils.resolveWorkspaceId(dataTableWorkspaceResolver, actionContext))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Unable to determine the workspace of this workflow");
    }

    @Test
    void testResolveWorkspaceIdFailsClosedForAnUnscopedActionContext() {
        ActionContext actionContext = mock(ActionContext.class);

        assertThatThrownBy(() -> DataTableUtils.resolveWorkspaceId(dataTableWorkspaceResolver, actionContext))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Unable to determine the workspace of this workflow");
    }

    @Test
    void testResolveWorkspaceIdFailsClosedForAnUnscopedTriggerContext() {
        TriggerContext triggerContext = mock(TriggerContext.class);

        assertThatThrownBy(() -> DataTableUtils.resolveWorkspaceId(dataTableWorkspaceResolver, triggerContext))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Unable to determine the workspace of this workflow");
    }

    @Test
    void testResolveWorkspaceIdUsesTheTriggerWorkflowId() {
        TriggerContextAware triggerContext = mock(TriggerContextAware.class);

        when(triggerContext.getWorkflowId()).thenReturn("workflow-1");
        when(dataTableWorkspaceResolver.resolveByWorkflowId("workflow-1")).thenReturn(OptionalLong.of(7L));

        assertThat(DataTableUtils.resolveWorkspaceId(dataTableWorkspaceResolver, triggerContext)).isEqualTo(7L);
    }

    @Test
    void testResolveWorkspaceIdFailsClosedForATriggerWithoutWorkflowId() {
        TriggerContextAware triggerContext = mock(TriggerContextAware.class);

        assertThatThrownBy(() -> DataTableUtils.resolveWorkspaceId(dataTableWorkspaceResolver, triggerContext))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Unable to determine the workspace of this workflow");
    }

    @Test
    void testResolveWorkspaceIdUsesTheJobPrincipalOfTheWorkflowExecutionId() {
        String workflowExecutionId = WorkflowExecutionId.of(
            PlatformType.AUTOMATION, 1051L, "workflow-uuid", "trigger_1")
            .toString();

        when(dataTableWorkspaceResolver.resolveByJobPrincipalId(1051L, PlatformType.AUTOMATION))
            .thenReturn(OptionalLong.of(8L));

        assertThat(DataTableUtils.resolveWorkspaceId(dataTableWorkspaceResolver, workflowExecutionId)).isEqualTo(8L);
    }

    @Test
    void testResolveWorkspaceIdFallsBackToTheWorkflowUuidWithoutAJobPrincipal() {
        String workflowExecutionId = WorkflowExecutionId.of(
            PlatformType.AUTOMATION, -1L, "workflow-uuid", "trigger_1")
            .toString();

        when(dataTableWorkspaceResolver.resolveByWorkflowUuid("workflow-uuid")).thenReturn(OptionalLong.of(9L));

        assertThat(DataTableUtils.resolveWorkspaceId(dataTableWorkspaceResolver, workflowExecutionId)).isEqualTo(9L);
    }

    @Test
    void testResolveWorkspaceIdFailsClosedForAnEmbeddedWorkflowExecutionId() {
        String workflowExecutionId = WorkflowExecutionId.of(
            PlatformType.EMBEDDED, 1051L, "workflow-uuid", "trigger_1")
            .toString();

        when(dataTableWorkspaceResolver.resolveByJobPrincipalId(1051L, PlatformType.EMBEDDED))
            .thenReturn(OptionalLong.empty());

        assertThatThrownBy(() -> DataTableUtils.resolveWorkspaceId(dataTableWorkspaceResolver, workflowExecutionId))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Unable to determine the workspace of this workflow");
    }

    @Test
    void testResolveDataTableMissesAnotherWorkspacesTable() {
        when(dataTableService.fetchDataTable(7L, "orders")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> DataTableUtils.resolveDataTable(dataTableService, 7L, "orders", 0L))
            .isInstanceOf(DataTableException.class)
            .hasMessage("Data table 'orders' not found in this workspace");
    }

    @Test
    void testGetTableOptionsListsOnlyTheWorkspace() {
        when(dataTableService.listTables(7L, 0L)).thenReturn(
            List.of(new DataTableInfo(1051L, "orders", 7L, "Orders", List.of(), Instant.EPOCH)));

        assertThat(DataTableUtils.getTableOptions(null, dataTableService, 7L))
            .extracting(Option::getValue)
            .containsExactly("orders");
    }

    @Test
    void testFlattenRowLiftsColumnValuesNextToId() {
        Map<String, Object> values = new HashMap<>();

        values.put("phone", "385916039814");
        values.put("staff_reply", "on my way");

        Map<String, Object> flattenedRow = DataTableUtils.flattenRow(new DataTableRow(7, values));

        assertEquals(Map.of("id", 7L, "phone", "385916039814", "staff_reply", "on my way"), flattenedRow);
    }

    @Test
    void testFlattenRowKeepsNullColumnsAsPresentKeys() {
        Map<String, Object> values = new HashMap<>();

        values.put("staff_reply", null);

        Map<String, Object> flattenedRow = DataTableUtils.flattenRow(new DataTableRow(7, values));

        assertTrue(flattenedRow.containsKey("staff_reply"));
        assertNull(flattenedRow.get("staff_reply"));
    }

    @Test
    void testFlattenRowOfMissingRecordStaysNull() {
        assertNull(DataTableUtils.flattenRow(null));
    }

    @Test
    void testFlattenRowsFlattensEveryRow() {
        List<Map<String, Object>> flattenedRows = DataTableUtils.flattenRows(
            List.of(new DataTableRow(1, Map.of("status", "BOT")), new DataTableRow(2, Map.of("status", "CLOSED"))));

        assertEquals(
            List.of(Map.of("id", 1L, "status", "BOT"), Map.of("id", 2L, "status", "CLOSED")), flattenedRows);
    }

    @Test
    void testFlattenPayloadFlattensWebhookRowPayload() {
        Map<String, Object> payload = Map.of("id", 7, "values", Map.of("staff_reply", "on my way"));

        assertEquals(Map.of("id", 7, "staff_reply", "on my way"), DataTableUtils.flattenPayload(payload));
    }

    @Test
    void testFlattenPayloadWithoutValuesIsReturnedAsIs() {
        Map<String, Object> payload = Map.of("id", 7);

        assertEquals(Map.of("id", 7), DataTableUtils.flattenPayload(payload));
    }

    @Test
    void testFlattenPayloadOfUnknownShapeIsReturnedUnchanged() {
        Object payload = List.of("unexpected");

        assertSame(payload, DataTableUtils.flattenPayload(payload));
    }
}
