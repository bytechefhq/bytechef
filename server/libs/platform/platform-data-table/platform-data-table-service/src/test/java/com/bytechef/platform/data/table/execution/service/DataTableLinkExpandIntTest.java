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

package com.bytechef.platform.data.table.execution.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.bytechef.platform.data.table.config.DataTableIntTestConfiguration;
import com.bytechef.platform.data.table.config.DataTableIntTestUtils;
import com.bytechef.platform.data.table.configuration.exception.DataTableErrorType;
import com.bytechef.platform.data.table.configuration.exception.DataTableException;
import com.bytechef.platform.data.table.configuration.service.DataTableService;
import com.bytechef.platform.data.table.domain.ColumnSpec;
import com.bytechef.platform.data.table.domain.ColumnType;
import com.bytechef.platform.data.table.domain.LinkDeleteRule;
import com.bytechef.platform.data.table.domain.LinkSpec;
import com.bytechef.platform.data.table.execution.domain.DataTableRow;
import com.bytechef.platform.data.table.execution.domain.DataTableRowConditions;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = DataTableIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
class DataTableLinkExpandIntTest {

    private static final long ENVIRONMENT_ID = 0;
    private static final long WORKSPACE_ID = 1L;

    @Autowired
    private DataTableRowService dataTableRowService;

    @Autowired
    private DataTableService dataTableService;

    @MockitoSpyBean
    private JdbcTemplate jdbcTemplate;

    private long customersId;
    private long ordersId;

    @BeforeEach
    void beforeEach() {
        DataTableIntTestUtils.dropAll(jdbcTemplate);

        customersId = dataTableService.createTable(
            WORKSPACE_ID, "customers", null, List.of(new ColumnSpec("name", ColumnType.STRING)), ENVIRONMENT_ID);
        ordersId = dataTableService.createTable(
            WORKSPACE_ID, "orders", null, List.of(new ColumnSpec("title", ColumnType.STRING)), ENVIRONMENT_ID);

        dataTableService.addColumn(
            ordersId,
            new ColumnSpec("customer", ColumnType.LINK, new LinkSpec(customersId, LinkDeleteRule.BLOCK, "name")),
            ENVIRONMENT_ID);
    }

    @Test
    void testFindRowsExpandsLink() {
        long customerId = insertCustomer("Ann", "C-1");

        dataTableRowService.insertRow(ordersId, Map.of("title", "o1", "customer", customerId), ENVIRONMENT_ID);

        List<DataTableRow> dataTableRows = findOrders(List.of("customer"));

        assertThat(dataTableRows).hasSize(1);

        Object customer = dataTableRows.getFirst()
            .values()
            .get("customer");

        assertThat(customer).isInstanceOf(DataTableRow.class);

        DataTableRow customerRow = (DataTableRow) customer;

        assertThat(customerRow.id()).isEqualTo(customerId);
        assertThat(customerRow.externalId()).isEqualTo("C-1");
        assertThat(customerRow.values()).containsEntry("name", "Ann");
    }

    @Test
    void testFindRowsExpandMatchesColumnNameCaseInsensitively() {
        long customerId = insertCustomer("Ann", null);

        dataTableRowService.insertRow(ordersId, Map.of("title", "o1", "customer", customerId), ENVIRONMENT_ID);

        List<DataTableRow> dataTableRows = findOrders(List.of("Customer"));

        assertThat(dataTableRows.getFirst()
            .values()
            .get("customer")).isInstanceOf(DataTableRow.class);
    }

    @Test
    void testFindRowsWithoutExpandKeepsTheLinkId() {
        long customerId = insertCustomer("Ann", null);

        dataTableRowService.insertRow(ordersId, Map.of("title", "o1", "customer", customerId), ENVIRONMENT_ID);

        List<DataTableRow> dataTableRows = findOrders(List.of());

        assertThat(dataTableRows.getFirst()
            .values()
            .get("customer")).isEqualTo(customerId);
    }

    @Test
    void testExpandUsesOneQueryPerLinkColumn() {
        long customerId = insertCustomer("Ann", null);
        long otherCustomerId = insertCustomer("Bob", null);

        for (int index = 0; index < 50; index++) {
            dataTableRowService.insertRow(
                ordersId, Map.of("title", "o" + index, "customer", index % 2 == 0 ? customerId : otherCustomerId),
                ENVIRONMENT_ID);
        }

        Mockito.clearInvocations(jdbcTemplate);

        List<DataTableRow> dataTableRows = findOrders(List.of("customer"));

        assertThat(dataTableRows).hasSize(50);

        List<String> selectStatements = Mockito.mockingDetails(jdbcTemplate)
            .getInvocations()
            .stream()
            .filter(invocation -> "query".equals(
                invocation.getMethod()
                    .getName())
                && invocation.getArguments().length == 3 && invocation.getArgument(2) instanceof RowMapper)
            .map(invocation -> invocation.getArgument(0))
            .filter(argument -> argument instanceof String)
            .map(argument -> (String) argument)
            .filter(sql -> sql.startsWith("SELECT"))
            .toList();

        List<String> dataSelectStatements = selectStatements.stream()
            .filter(sql -> !sql.contains("information_schema"))
            .toList();

        assertThat(dataSelectStatements).hasSize(2);
        assertThat(selectStatements).hasSize(5);
    }

    @Test
    void testExpandOfEmptyLinkIsNull() {
        dataTableRowService.insertRow(ordersId, new HashMap<>(Map.of("title", "o1")), ENVIRONMENT_ID);

        List<DataTableRow> dataTableRows = findOrders(List.of("customer"));

        assertThat(dataTableRows).hasSize(1);

        Map<String, Object> values = dataTableRows.getFirst()
            .values();

        assertThat(values).containsKey("customer");
        assertThat(values.get("customer")).isNull();
    }

    @Test
    void testExpandAfterClearDeleteIsNull() {
        dataTableService.removeColumn(ordersId, "customer", ENVIRONMENT_ID);
        dataTableService.addColumn(
            ordersId,
            new ColumnSpec("customer", ColumnType.LINK, new LinkSpec(customersId, LinkDeleteRule.CLEAR, "name")),
            ENVIRONMENT_ID);

        long customerId = insertCustomer("Ann", null);

        DataTableRow order = dataTableRowService.insertRow(
            ordersId, Map.of("title", "o1", "customer", customerId), ENVIRONMENT_ID);

        dataTableRowService.deleteRow(customersId, customerId, ENVIRONMENT_ID);

        DataTableRow reloadedOrder = dataTableRowService.getRow(
            ordersId, order.id(), List.of("customer"), ENVIRONMENT_ID);

        assertThat(reloadedOrder).isNotNull();
        assertThat(reloadedOrder.values()
            .get("customer")).isNull();
    }

    @Test
    void testExpandRejectsNonLinkColumn() {
        insertCustomer("Ann", null);

        Mockito.clearInvocations(jdbcTemplate);

        DataTableException dataTableException =
            assertThrows(DataTableException.class, () -> findOrders(List.of("title")));

        assertThat(dataTableException.getErrorType()).isEqualTo(DataTableErrorType.COLUMN_NOT_FOUND);

        boolean rowQueryRan = Mockito.mockingDetails(jdbcTemplate)
            .getInvocations()
            .stream()
            .anyMatch(invocation -> invocation.getArguments().length > 0 &&
                invocation.getArgument(0) instanceof String sql && sql.contains("FROM \"dt_"));

        assertThat(rowQueryRan).isFalse();

        assertThrows(DataTableException.class, () -> findOrders(List.of("missing")));
        assertThrows(
            DataTableException.class,
            () -> dataTableRowService.getRow(ordersId, 1L, List.of("title"), ENVIRONMENT_ID));
    }

    @Test
    void testGetRowExpandsLink() {
        long customerId = insertCustomer("Ann", "C-1");

        DataTableRow order = dataTableRowService.insertRow(
            ordersId, Map.of("title", "o1", "customer", customerId), ENVIRONMENT_ID);

        DataTableRow reloadedOrder = dataTableRowService.getRow(
            ordersId, order.id(), List.of("customer"), ENVIRONMENT_ID);

        assertThat(reloadedOrder).isNotNull();

        Object customer = reloadedOrder.values()
            .get("customer");

        assertThat(customer).isInstanceOf(DataTableRow.class);
        assertThat(((DataTableRow) customer).values()).containsEntry("name", "Ann");

        DataTableRow plainOrder = dataTableRowService.getRow(ordersId, order.id(), ENVIRONMENT_ID);

        assertThat(plainOrder).isNotNull();
        assertThat(plainOrder.values()
            .get("customer")).isEqualTo(customerId);
    }

    private List<DataTableRow> findOrders(List<String> expand) {
        return dataTableRowService.findRows(
            ordersId, DataTableRowConditions.matchAll(), List.of(), 100, 0, expand, ENVIRONMENT_ID);
    }

    private long insertCustomer(String name, String externalId) {
        DataTableRow dataTableRow = dataTableRowService.insertRow(
            customersId, Map.of("name", name), externalId, ENVIRONMENT_ID);

        return dataTableRow.id();
    }
}
