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
import com.bytechef.platform.data.table.configuration.domain.DataTableWebhookType;
import com.bytechef.platform.data.table.configuration.exception.DataTableException;
import com.bytechef.platform.data.table.configuration.service.DataTableService;
import com.bytechef.platform.data.table.domain.ColumnSpec;
import com.bytechef.platform.data.table.domain.ColumnType;
import com.bytechef.platform.data.table.domain.LinkDeleteRule;
import com.bytechef.platform.data.table.domain.LinkSpec;
import com.bytechef.platform.data.table.execution.domain.DataTableRow;
import com.bytechef.platform.data.table.execution.event.DataTableWebhookEvent;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = DataTableIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
@RecordApplicationEvents
class DataTableLinkClearEventIntTest {

    private static final long ENVIRONMENT_ID = 0;
    private static final long WORKSPACE_ID = 1L;

    @Autowired
    private ApplicationEvents applicationEvents;

    @Autowired
    private DataTableRowService dataTableRowService;

    @Autowired
    private DataTableService dataTableService;

    @Autowired
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
    }

    @Test
    void testDeleteTargetRowPublishesUpdatedEventForClearedRows() {
        addCustomerLink(LinkDeleteRule.CLEAR);

        long customerId = insertCustomer("Ann");
        long otherCustomerId = insertCustomer("Bob");

        DataTableRow linkedOrder = dataTableRowService.insertRow(
            ordersId, Map.of("title", "o1", "customer", customerId), ENVIRONMENT_ID);

        dataTableRowService.insertRow(ordersId, Map.of("title", "o2", "customer", otherCustomerId), ENVIRONMENT_ID);

        applicationEvents.clear();

        dataTableRowService.deleteRow(customersId, customerId, ENVIRONMENT_ID);

        List<DataTableWebhookEvent> updatedEvents = updatedEvents();

        assertThat(updatedEvents).hasSize(1);

        DataTableWebhookEvent updatedEvent = updatedEvents.getFirst();

        assertThat(updatedEvent.getDataTableId()).isEqualTo(ordersId);
        assertThat(updatedEvent.getRows()).hasSize(1);

        DataTableRow updatedRow = updatedEvent.getRows()
            .getFirst();

        assertThat(updatedRow.id()).isEqualTo(linkedOrder.id());
        assertThat(updatedRow.values()
            .get("customer")).isNull();
    }

    @Test
    void testDeleteRowsByConditionsPublishesOneUpdatedEventPerSourceTable() {
        addCustomerLink(LinkDeleteRule.CLEAR);

        long customerId = insertCustomer("Ann");

        dataTableRowService.insertRow(ordersId, Map.of("title", "o1", "customer", customerId), ENVIRONMENT_ID);
        dataTableRowService.insertRow(ordersId, Map.of("title", "o2", "customer", customerId), ENVIRONMENT_ID);

        applicationEvents.clear();

        dataTableRowService.deleteAllRows(customersId, ENVIRONMENT_ID);

        List<DataTableWebhookEvent> updatedEvents = updatedEvents();

        assertThat(updatedEvents).hasSize(1);
        assertThat(updatedEvents.getFirst()
            .getRows()).hasSize(2);
    }

    @Test
    void testBlockedDeletePublishesNoEvents() {
        addCustomerLink(LinkDeleteRule.BLOCK);

        long customerId = insertCustomer("Ann");

        dataTableRowService.insertRow(ordersId, Map.of("title", "o1", "customer", customerId), ENVIRONMENT_ID);

        applicationEvents.clear();

        assertThrows(
            DataTableException.class, () -> dataTableRowService.deleteRow(customersId, customerId, ENVIRONMENT_ID));

        assertThat(applicationEvents.stream(DataTableWebhookEvent.class)
            .toList()).isEmpty();
    }

    @Test
    void testDeleteWithoutLinkingRowsPublishesNoUpdatedEvent() {
        addCustomerLink(LinkDeleteRule.CLEAR);

        long customerId = insertCustomer("Ann");
        long otherCustomerId = insertCustomer("Bob");

        dataTableRowService.insertRow(ordersId, Map.of("title", "o1", "customer", otherCustomerId), ENVIRONMENT_ID);

        applicationEvents.clear();

        dataTableRowService.deleteRow(customersId, customerId, ENVIRONMENT_ID);

        assertThat(updatedEvents()).isEmpty();
    }

    private void addCustomerLink(LinkDeleteRule linkDeleteRule) {
        dataTableService.addColumn(
            ordersId, new ColumnSpec("customer", ColumnType.LINK, new LinkSpec(customersId, linkDeleteRule, "name")),
            ENVIRONMENT_ID);
    }

    private long insertCustomer(String name) {
        DataTableRow dataTableRow = dataTableRowService.insertRow(
            customersId, Map.of("name", name), null, ENVIRONMENT_ID);

        return dataTableRow.id();
    }

    private List<DataTableWebhookEvent> updatedEvents() {
        return applicationEvents.stream(DataTableWebhookEvent.class)
            .filter(event -> event.getType() == DataTableWebhookType.RECORD_UPDATED)
            .toList();
    }
}
