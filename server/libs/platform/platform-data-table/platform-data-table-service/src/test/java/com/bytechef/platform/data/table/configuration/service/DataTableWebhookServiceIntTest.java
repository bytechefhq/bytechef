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

package com.bytechef.platform.data.table.configuration.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.data.table.config.DataTableIntTestConfiguration;
import com.bytechef.platform.data.table.configuration.domain.DataTable;
import com.bytechef.platform.data.table.configuration.domain.DataTableWebhook;
import com.bytechef.platform.data.table.configuration.domain.DataTableWebhookType;
import com.bytechef.platform.data.table.configuration.repository.DataTableRepository;
import com.bytechef.platform.data.table.configuration.repository.DataTableWebhookRepository;
import com.bytechef.platform.data.table.domain.ColumnSpec;
import com.bytechef.platform.data.table.domain.ColumnType;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.test.config.h2.H2DataSourceConfiguration;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = DataTableIntTestConfiguration.class)
@Import(H2DataSourceConfiguration.class)
class DataTableWebhookServiceIntTest {

    private static final long DEV_ENVIRONMENT_ID = 0;
    private static final String WORKFLOW_EXECUTION_ID = WorkflowExecutionId.of(
        PlatformType.AUTOMATION, 1, "workflow-uuid", "trigger_1")
        .toString();
    private static final String WEBHOOK_URL = "http://localhost:9555/webhooks/" + WORKFLOW_EXECUTION_ID;

    @Autowired
    private DataTableService dataTableService;

    @Autowired
    private DataTableRepository dataTableRepository;

    @Autowired
    private DataTableWebhookRepository dataTableWebhookRepository;

    @Autowired
    private DataTableWebhookService dataTableWebhookService;

    @AfterEach
    void afterEach() {
        dataTableService.dropTable("hooks", DEV_ENVIRONMENT_ID);
    }

    @BeforeEach
    void beforeEach() {
        dataTableService.dropTable("hooks", DEV_ENVIRONMENT_ID);
        dataTableService.createTable(
            "hooks", null, List.of(new ColumnSpec("title", ColumnType.STRING)), DEV_ENVIRONMENT_ID);
    }

    @Test
    void testAddWebhookStoresTheWorkflowExecutionId() {
        long webhookId = dataTableWebhookService.addWebhook(
            "hooks", WORKFLOW_EXECUTION_ID, WEBHOOK_URL, DataTableWebhookType.RECORD_CREATED, DEV_ENVIRONMENT_ID);

        assertTrue(
            dataTableWebhookService.listWebhooks("hooks", DEV_ENVIRONMENT_ID)
                .stream()
                .anyMatch(webhook -> webhook.id() == webhookId &&
                    WORKFLOW_EXECUTION_ID.equals(webhook.workflowExecutionId())));
    }

    @Test
    void testAddWebhookAlsoStoresTheWebhookUrlSoThePreviousVersionCanDeliverAfterARollback() {
        long webhookId = dataTableWebhookService.addWebhook(
            "hooks", WORKFLOW_EXECUTION_ID, WEBHOOK_URL, DataTableWebhookType.RECORD_CREATED, DEV_ENVIRONMENT_ID);

        DataTableWebhook dataTableWebhook = dataTableWebhookRepository.findById(webhookId)
            .orElseThrow();

        assertEquals(WEBHOOK_URL, dataTableWebhook.getUrl());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "http://localhost:9555/webhooks/{id}", "https://example.com/proxy/webhooks/{id}?token=1",
        "http://bytechef_server/webhooks/{id}#fragment"
    })
    void testAWebhookRegisteredByThePreviousVersionIsListedWithTheIdFromItsUrl(String urlTemplate) {
        DataTable dataTable = dataTableRepository.findByName("hooks")
            .orElseThrow();

        DataTableWebhook dataTableWebhook = new DataTableWebhook();

        dataTableWebhook.setDataTableId(dataTable.getId());
        dataTableWebhook.setEnvironment(Environment.DEVELOPMENT);
        dataTableWebhook.setType(DataTableWebhookType.RECORD_CREATED);
        dataTableWebhook.setUrl(urlTemplate.replace("{id}", WORKFLOW_EXECUTION_ID));

        dataTableWebhookRepository.save(dataTableWebhook);

        assertEquals(
            List.of(WORKFLOW_EXECUTION_ID),
            dataTableWebhookService.listWebhooks("hooks", DEV_ENVIRONMENT_ID)
                .stream()
                .map(DataTableWebhookService.Webhook::workflowExecutionId)
                .toList());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "", "not an id", "https://example.com/webhooks/abc"
    })
    void testAddWebhookRejectsAValueThatIsNotAWorkflowExecutionId(String workflowExecutionId) {
        IllegalArgumentException illegalArgumentException = assertThrows(
            IllegalArgumentException.class,
            () -> dataTableWebhookService.addWebhook(
                "hooks", workflowExecutionId, WEBHOOK_URL, DataTableWebhookType.RECORD_CREATED, DEV_ENVIRONMENT_ID));

        assertTrue(illegalArgumentException.getMessage()
            .contains("workflow execution id"));
        assertTrue(
            dataTableWebhookService.listWebhooks("hooks", DEV_ENVIRONMENT_ID)
                .isEmpty());
    }
}
