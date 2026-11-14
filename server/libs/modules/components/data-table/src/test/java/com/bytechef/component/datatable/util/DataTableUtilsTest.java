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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableArrayProperty;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property.ArrayProperty;
import com.bytechef.component.definition.Property.ValueProperty;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.platform.component.definition.ActionContextAware;
import com.bytechef.platform.component.definition.TriggerContextAware;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.data.table.configuration.domain.DataTable;
import com.bytechef.platform.data.table.configuration.domain.DataTableInfo;
import com.bytechef.platform.data.table.configuration.exception.DataTableException;
import com.bytechef.platform.data.table.configuration.service.DataTableService;
import com.bytechef.platform.data.table.domain.ColumnSpec;
import com.bytechef.platform.data.table.domain.ColumnType;
import com.bytechef.platform.data.table.domain.DataTableWorkspaceResolver;
import com.bytechef.platform.data.table.execution.domain.DataTableRow;
import com.bytechef.platform.data.table.execution.domain.DataTableRowConditions;
import com.bytechef.platform.data.table.execution.domain.DataTableRowFilter;
import com.bytechef.platform.data.table.execution.domain.DataTableRowFilter.Operation;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class DataTableUtilsTest {

    private final DataTableService dataTableService = mock(DataTableService.class);
    private final DataTableWorkspaceResolver dataTableWorkspaceResolver = mock(DataTableWorkspaceResolver.class);

    @Test
    void testConditionsPropertyDisablesExpressionsOnTheOrAndAndConditionArrays() {
        ModifiableArrayProperty conditionsProperty = DataTableUtils.conditionsProperty(
            dataTableService, dataTableWorkspaceResolver);

        List<? extends ValueProperty<?>> items = conditionsProperty.getItems();

        ArrayProperty andConditionProperty = (ArrayProperty) items.getFirst();

        assertFalse(conditionsProperty.getExpressionEnabled());
        assertFalse(andConditionProperty.getExpressionEnabled());
    }

    @Test
    void testActionColumnOptionsOfferTheRowIdFirstAndFilterItBySearchText() throws Exception {
        ActionContextAware actionContext = mock(ActionContextAware.class);

        when(actionContext.getWorkflowId()).thenReturn("workflow-1");
        when(dataTableWorkspaceResolver.resolveByWorkflowId("workflow-1")).thenReturn(OptionalLong.of(7L));
        when(dataTableService.fetchDataTable(7L, "orders")).thenReturn(Optional.of(new DataTable(1L, "orders")));
        when(dataTableService.fetchDataTableInfo(1L, 0L)).thenReturn(
            Optional.of(
                new DataTableInfo(
                    1L, "orders", 7L, null,
                    List.of(new ColumnSpec("name", ColumnType.STRING), new ColumnSpec("open", ColumnType.BOOLEAN)),
                    Instant.EPOCH)));

        ActionDefinition.OptionsFunction<String> optionsFunction = DataTableUtils.getActionColumnOptions(
            dataTableService, dataTableWorkspaceResolver);

        Parameters inputParameters = MockParametersFactory.create(Map.of("table", "orders"));

        assertEquals(
            List.of("id", "name", "open"),
            getOptionValues(optionsFunction.apply(inputParameters, null, null, null, actionContext)));
        assertEquals(
            List.of("id"), getOptionValues(optionsFunction.apply(inputParameters, null, null, "I", actionContext)));
        assertEquals(
            List.of("name"),
            getOptionValues(optionsFunction.apply(inputParameters, null, null, "nam", actionContext)));
    }

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
        String workflowExecutionId = createWorkflowExecutionId(PlatformType.AUTOMATION, 1051L);

        when(dataTableWorkspaceResolver.resolveByJobPrincipalId(1051L, PlatformType.AUTOMATION))
            .thenReturn(OptionalLong.of(8L));

        assertThat(DataTableUtils.resolveWorkspaceId(dataTableWorkspaceResolver, workflowExecutionId)).isEqualTo(8L);
    }

    @Test
    void testResolveWorkspaceIdFallsBackToTheWorkflowUuidWithoutAJobPrincipal() {
        String workflowExecutionId = createWorkflowExecutionId(PlatformType.AUTOMATION, -1L);

        when(dataTableWorkspaceResolver.resolveByWorkflowUuid("workflow-uuid")).thenReturn(OptionalLong.of(9L));

        assertThat(DataTableUtils.resolveWorkspaceId(dataTableWorkspaceResolver, workflowExecutionId)).isEqualTo(9L);
    }

    @Test
    void testResolveWorkspaceIdFailsClosedForAnEmbeddedWorkflowExecutionId() {
        String workflowExecutionId = createWorkflowExecutionId(PlatformType.EMBEDDED, 1051L);

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
    void testResolveDataTableMissesATableWithoutAPhysicalTableInTheEnvironment() {
        when(dataTableService.fetchDataTable(7L, "orders")).thenReturn(Optional.of(new DataTable(1051L, "orders")));
        when(dataTableService.fetchDataTableInfo(1051L, 2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> DataTableUtils.resolveDataTable(dataTableService, 7L, "orders", 2L))
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
    void testGetTableOptionsFiltersBySearchTextIgnoringCase() {
        when(dataTableService.listTables(7L, 0L)).thenReturn(
            List.of(
                new DataTableInfo(1051L, "orders", 7L, null, List.of(), Instant.EPOCH),
                new DataTableInfo(1052L, "customers", 7L, null, List.of(), Instant.EPOCH)));

        assertThat(DataTableUtils.getTableOptions("ORD", dataTableService, 7L))
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

    @Test
    void testGetConditionsDefaultsAndNormalizesOperations() {
        Map<String, Object> conditionWithNullOperation = new HashMap<>();

        conditionWithNullOperation.put("column", "status");
        conditionWithNullOperation.put("operation", null);
        conditionWithNullOperation.put("value", "BOT");

        DataTableRowConditions conditions = DataTableUtils.getConditions(
            MockParametersFactory.create(
                Map.of(
                    "conditions",
                    List.of(
                        List.of(
                            conditionWithNullOperation,
                            Map.of("column", "title", "operation", "contains", "value", "help"))))));

        assertEquals(
            DataTableRowConditions.of(
                List.of(
                    new DataTableRowFilter("status", Operation.EQUALS, "BOT"),
                    new DataTableRowFilter("title", Operation.CONTAINS, "help"))),
            conditions);
    }

    @Test
    void testGetConditionsRejectsConditionsThatAreNotNestedLists() {
        for (Object conditions : List.of(List.of(Map.of("column", "status", "value", "BOT")), "status = BOT")) {
            IllegalArgumentException illegalArgumentException = assertThrows(
                IllegalArgumentException.class,
                () -> DataTableUtils.getConditions(MockParametersFactory.create(Map.of("conditions", conditions))));

            Throwable[] suppressedExceptions = illegalArgumentException.getSuppressed();

            assertEquals(
                "OR Conditions must be a list of AND Condition lists, each holding condition objects: " +
                    suppressedExceptions[0].getMessage(),
                illegalArgumentException.getMessage());
            assertEquals(
                illegalArgumentException.getMessage(),
                ExceptionUtils.getRootCause(illegalArgumentException)
                    .getMessage());
        }
    }

    @Test
    void testGetConditionsRejectsEmptyOrConditions() {
        for (List<?> andConditions : Arrays.asList(List.of(), null)) {
            IllegalArgumentException illegalArgumentException = assertThrows(
                IllegalArgumentException.class,
                () -> DataTableUtils.getConditions(
                    MockParametersFactory.create(
                        Map.of(
                            "conditions",
                            Arrays.asList(List.of(Map.of("column", "status", "value", "BOT")), andConditions)))));

            assertEquals(
                "OR Condition 2 is empty; add a condition to it or remove it", illegalArgumentException.getMessage());
        }
    }

    @Test
    void testGetConditionsRejectsInvalidConditions() {
        IllegalArgumentException unknownOperationException = assertThrows(
            IllegalArgumentException.class,
            () -> DataTableUtils.getConditions(
                MockParametersFactory.create(
                    Map.of(
                        "conditions",
                        List.of(List.of(Map.of("column", "status", "operation", "LIKE", "value", "x")))))));

        assertTrue(unknownOperationException.getMessage()
            .startsWith("Condition 1 of OR Condition 1: Unknown Operation 'LIKE'; expected one of EQUALS"));
        assertEquals(
            unknownOperationException.getMessage(),
            ExceptionUtils.getRootCause(unknownOperationException)
                .getMessage());

        IllegalArgumentException emptyConditionException = assertThrows(
            IllegalArgumentException.class,
            () -> DataTableUtils.getConditions(
                MockParametersFactory.create(
                    Map.of("conditions", List.of(Arrays.asList(Map.of("column", "status", "value", "x"), null))))));

        assertEquals("Condition 2 of OR Condition 1 is empty", emptyConditionException.getMessage());

        IllegalArgumentException blankColumnException = assertThrows(
            IllegalArgumentException.class,
            () -> DataTableUtils.getConditions(
                MockParametersFactory.create(
                    Map.of("conditions", List.of(List.of(Map.of("column", " ", "value", "x")))))));

        assertEquals("Condition 1 of OR Condition 1: Column is required", blankColumnException.getMessage());

        IllegalArgumentException blankOperationException = assertThrows(
            IllegalArgumentException.class,
            () -> DataTableUtils.getConditions(
                MockParametersFactory.create(
                    Map.of(
                        "conditions",
                        List.of(List.of(Map.of("column", "status", "operation", " ", "value", "active")))))));

        assertEquals(
            "Condition 1 of OR Condition 1: Operation must not be blank", blankOperationException.getMessage());

        IllegalArgumentException missingBoundException = assertThrows(
            IllegalArgumentException.class,
            () -> DataTableUtils.getConditions(
                MockParametersFactory.create(
                    Map.of(
                        "conditions",
                        List.of(List.of(Map.of("column", "amount", "operation", "BETWEEN", "value", "1")))))));

        assertEquals(
            "Condition 1 of OR Condition 1: Operation BETWEEN requires To Value", missingBoundException.getMessage());

        IllegalArgumentException missingLowerBoundException = assertThrows(
            IllegalArgumentException.class,
            () -> DataTableUtils.getConditions(
                MockParametersFactory.create(
                    Map.of(
                        "conditions",
                        List.of(List.of(Map.of("column", "amount", "operation", "BETWEEN", "valueTo", " ")))))));

        assertEquals(
            "Condition 1 of OR Condition 1: Operation BETWEEN requires Value", missingLowerBoundException.getMessage());
    }

    @Test
    void testGetConditionsRejectsValueInsteadOfValuesForIn() {
        IllegalArgumentException illegalArgumentException = assertThrows(
            IllegalArgumentException.class,
            () -> DataTableUtils.getConditions(
                MockParametersFactory.create(
                    Map.of(
                        "conditions",
                        List.of(List.of(Map.of("column", "status", "operation", "IN", "value", "BOT")))))));

        assertEquals(
            "Condition 1 of OR Condition 1: Operation IN takes its values from Values, not Value",
            illegalArgumentException.getMessage());
    }

    @Test
    void testGetConditionsRejectsUnknownKeys() {
        IllegalArgumentException illegalArgumentException = assertThrows(
            IllegalArgumentException.class,
            () -> DataTableUtils.getConditions(
                MockParametersFactory.create(
                    Map.of(
                        "conditions",
                        List.of(List.of(Map.of("column", "status", "operator", "NOT_EQUALS", "value", "done")))))));

        assertEquals(
            "Condition 1 of OR Condition 1: Unknown condition key(s) operator; expected column, operation, value, valueTo, "
                +
                "values",
            illegalArgumentException.getMessage());
    }

    private static List<String> getOptionValues(List<? extends Option<String>> options) {
        return options.stream()
            .map(Option::getValue)
            .toList();
    }

    private static String createWorkflowExecutionId(PlatformType platformType, long jobPrincipalId) {
        WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.of(
            platformType, jobPrincipalId, "workflow-uuid", "trigger_1");

        return workflowExecutionId.toString();
    }
}
