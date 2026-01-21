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

package com.bytechef.automation.data.table.configuration.service;

import static com.bytechef.platform.configuration.domain.Environment.DEVELOPMENT;
import static com.bytechef.platform.configuration.domain.Environment.STAGING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.automation.data.table.configuration.config.DataTableIntTestConfiguration;
import com.bytechef.automation.data.table.configuration.domain.DataTableWebhookType;
import com.bytechef.automation.data.table.configuration.repository.DataTableRepository;
import com.bytechef.automation.data.table.configuration.repository.DataTableWebhookRepository;
import com.bytechef.automation.data.table.configuration.repository.WorkspaceDataTableRepository;
import com.bytechef.automation.data.table.domain.ColumnSpec;
import com.bytechef.automation.data.table.domain.ColumnType;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = DataTableIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
public class DataTableWebhookServiceIntTest {

    @Autowired
    private DataTableRepository dataTableRepository;

    @Autowired
    private DataTableService dataTableService;

    @Autowired
    private DataTableWebhookRepository dataTableWebhookRepository;

    @Autowired
    private DataTableWebhookService dataTableWebhookService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private WorkspaceDataTableRepository workspaceDataTableRepository;

    @BeforeEach
    public void beforeEach() {
        cleanupTables();
    }

    @AfterEach
    public void afterEach() {
        cleanupTables();
    }

    private void cleanupTables() {
        dataTableWebhookRepository.deleteAll();
        workspaceDataTableRepository.deleteAll();
        dataTableRepository.deleteAll();

        jdbcTemplate.execute("DROP TABLE IF EXISTS \"dt_0_orders\"");
        jdbcTemplate.execute("DROP TABLE IF EXISTS \"dt_1_orders\"");
    }

    @Test
    public void testAddWebhook() {
        dataTableService.createTable(
            "orders",
            List.of(new ColumnSpec("name", ColumnType.STRING)),
            DEVELOPMENT.ordinal());

        long webhookId = dataTableWebhookService.addWebhook(
            "orders",
            "https://example.com/webhook",
            DataTableWebhookType.RECORD_CREATED,
            DEVELOPMENT.ordinal());

        assertThat(webhookId).isPositive();

        List<DataTableWebhookService.Webhook> webhooks =
            dataTableWebhookService.listWebhooks("orders", DEVELOPMENT.ordinal());

        assertThat(webhooks).hasSize(1);
        assertThat(webhooks.getFirst()
            .url()).isEqualTo("https://example.com/webhook");
        assertThat(webhooks.getFirst()
            .type()).isEqualTo(DataTableWebhookType.RECORD_CREATED);
    }

    @Test
    public void testAddWebhookThrowsExceptionForNonExistentTable() {
        assertThatThrownBy(() -> dataTableWebhookService.addWebhook(
            "nonexistent",
            "https://example.com/webhook",
            DataTableWebhookType.RECORD_CREATED,
            DEVELOPMENT.ordinal()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Table not found");
    }

    @Test
    public void testAddMultipleWebhooks() {
        dataTableService.createTable(
            "orders",
            List.of(new ColumnSpec("name", ColumnType.STRING)),
            DEVELOPMENT.ordinal());

        dataTableWebhookService.addWebhook(
            "orders",
            "https://example.com/webhook1",
            DataTableWebhookType.RECORD_CREATED,
            DEVELOPMENT.ordinal());

        dataTableWebhookService.addWebhook(
            "orders",
            "https://example.com/webhook2",
            DataTableWebhookType.RECORD_UPDATED,
            DEVELOPMENT.ordinal());

        dataTableWebhookService.addWebhook(
            "orders",
            "https://example.com/webhook3",
            DataTableWebhookType.RECORD_DELETED,
            DEVELOPMENT.ordinal());

        List<DataTableWebhookService.Webhook> webhooks =
            dataTableWebhookService.listWebhooks("orders", DEVELOPMENT.ordinal());

        assertThat(webhooks).hasSize(3);
        assertThat(webhooks)
            .extracting(DataTableWebhookService.Webhook::type)
            .containsExactlyInAnyOrder(
                DataTableWebhookType.RECORD_CREATED,
                DataTableWebhookType.RECORD_UPDATED,
                DataTableWebhookType.RECORD_DELETED);
    }

    @Test
    public void testListWebhooksReturnsEmptyForNonExistentTable() {
        List<DataTableWebhookService.Webhook> webhooks =
            dataTableWebhookService.listWebhooks("nonexistent", DEVELOPMENT.ordinal());

        assertThat(webhooks).isEmpty();
    }

    @Test
    public void testListWebhooksFiltersOtherEnvironments() {
        dataTableService.createTable(
            "orders",
            List.of(new ColumnSpec("name", ColumnType.STRING)),
            DEVELOPMENT.ordinal());

        dataTableService.createTable(
            "orders",
            List.of(new ColumnSpec("name", ColumnType.STRING)),
            STAGING.ordinal());

        dataTableWebhookService.addWebhook(
            "orders",
            "https://example.com/dev",
            DataTableWebhookType.RECORD_CREATED,
            DEVELOPMENT.ordinal());

        dataTableWebhookService.addWebhook(
            "orders",
            "https://example.com/staging",
            DataTableWebhookType.RECORD_CREATED,
            STAGING.ordinal());

        List<DataTableWebhookService.Webhook> devWebhooks =
            dataTableWebhookService.listWebhooks("orders", DEVELOPMENT.ordinal());
        List<DataTableWebhookService.Webhook> stagingWebhooks =
            dataTableWebhookService.listWebhooks("orders", STAGING.ordinal());

        assertThat(devWebhooks).hasSize(1);
        assertThat(devWebhooks.getFirst()
            .url()).isEqualTo("https://example.com/dev");

        assertThat(stagingWebhooks).hasSize(1);
        assertThat(stagingWebhooks.getFirst()
            .url()).isEqualTo("https://example.com/staging");
    }

    @Test
    public void testRemoveWebhook() {
        dataTableService.createTable(
            "orders",
            List.of(new ColumnSpec("name", ColumnType.STRING)),
            DEVELOPMENT.ordinal());

        long webhookId = dataTableWebhookService.addWebhook(
            "orders",
            "https://example.com/webhook",
            DataTableWebhookType.RECORD_CREATED,
            DEVELOPMENT.ordinal());

        List<DataTableWebhookService.Webhook> beforeRemove =
            dataTableWebhookService.listWebhooks("orders", DEVELOPMENT.ordinal());

        assertThat(beforeRemove).hasSize(1);

        dataTableWebhookService.removeWebhook(webhookId);

        List<DataTableWebhookService.Webhook> afterRemove =
            dataTableWebhookService.listWebhooks("orders", DEVELOPMENT.ordinal());

        assertThat(afterRemove).isEmpty();
    }

    @Test
    public void testRemoveWebhookDoesNotAffectOtherWebhooks() {
        dataTableService.createTable(
            "orders",
            List.of(new ColumnSpec("name", ColumnType.STRING)),
            DEVELOPMENT.ordinal());

        long webhookId1 = dataTableWebhookService.addWebhook(
            "orders",
            "https://example.com/webhook1",
            DataTableWebhookType.RECORD_CREATED,
            DEVELOPMENT.ordinal());

        dataTableWebhookService.addWebhook(
            "orders",
            "https://example.com/webhook2",
            DataTableWebhookType.RECORD_UPDATED,
            DEVELOPMENT.ordinal());

        dataTableWebhookService.removeWebhook(webhookId1);

        List<DataTableWebhookService.Webhook> remaining =
            dataTableWebhookService.listWebhooks("orders", DEVELOPMENT.ordinal());

        assertThat(remaining).hasSize(1);
        assertThat(remaining.getFirst()
            .url()).isEqualTo("https://example.com/webhook2");
    }

    @Test
    public void testWebhookDataTableIdIsSet() {
        dataTableService.createTable(
            "orders",
            List.of(new ColumnSpec("name", ColumnType.STRING)),
            DEVELOPMENT.ordinal());

        long tableId = dataTableService.getIdByBaseName("orders");

        dataTableWebhookService.addWebhook(
            "orders",
            "https://example.com/webhook",
            DataTableWebhookType.RECORD_CREATED,
            DEVELOPMENT.ordinal());

        List<DataTableWebhookService.Webhook> webhooks =
            dataTableWebhookService.listWebhooks("orders", DEVELOPMENT.ordinal());

        assertThat(webhooks).hasSize(1);
        assertThat(webhooks.getFirst()
            .dataTableId()).isEqualTo(tableId);
    }

    @Test
    public void testWebhookEnvironmentIdIsSet() {
        dataTableService.createTable(
            "orders",
            List.of(new ColumnSpec("name", ColumnType.STRING)),
            DEVELOPMENT.ordinal());

        dataTableWebhookService.addWebhook(
            "orders",
            "https://example.com/webhook",
            DataTableWebhookType.RECORD_CREATED,
            DEVELOPMENT.ordinal());

        List<DataTableWebhookService.Webhook> webhooks =
            dataTableWebhookService.listWebhooks("orders", DEVELOPMENT.ordinal());

        assertThat(webhooks).hasSize(1);
        assertThat(webhooks.getFirst()
            .environmentId()).isEqualTo(DEVELOPMENT.ordinal());
    }
}
