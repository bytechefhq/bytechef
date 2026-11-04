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

package com.bytechef.platform.data.table.execution.listener;

import static com.bytechef.platform.data.table.configuration.domain.DataTableWebhookType.RECORD_CREATED;
import static com.bytechef.platform.data.table.configuration.domain.DataTableWebhookType.RECORD_DELETED;
import static com.bytechef.platform.data.table.configuration.domain.DataTableWebhookType.RECORD_UPDATED;
import static com.bytechef.platform.data.table.execution.listener.DataTableWebhookEventListener.WORKFLOW_EXECUTION_ID_HEADER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.component.definition.TriggerDefinition.WebhookBody.ContentType;
import com.bytechef.component.definition.TriggerDefinition.WebhookMethod;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.platform.component.trigger.WebhookRequest;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.data.table.config.DataTableIntTestConfiguration;
import com.bytechef.platform.data.table.config.DisabledWorkflowJobPrincipalAccessor;
import com.bytechef.platform.data.table.config.RecordingMessageBroker;
import com.bytechef.platform.data.table.configuration.domain.DataTableWebhook;
import com.bytechef.platform.data.table.configuration.domain.DataTableWebhookType;
import com.bytechef.platform.data.table.configuration.repository.DataTableWebhookRepository;
import com.bytechef.platform.data.table.configuration.service.DataTableService;
import com.bytechef.platform.data.table.configuration.service.DataTableWebhookService;
import com.bytechef.platform.data.table.domain.ColumnSpec;
import com.bytechef.platform.data.table.domain.ColumnType;
import com.bytechef.platform.data.table.execution.domain.DataTableRow;
import com.bytechef.platform.data.table.execution.event.DataTableWebhookEvent;
import com.bytechef.platform.data.table.execution.service.DataTableRowService;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.platform.workflow.coordinator.event.TriggerWebhookEvent;
import com.bytechef.platform.workflow.coordinator.message.route.TriggerCoordinatorMessageRoute;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(OutputCaptureExtension.class)
@Import(PostgreSQLContainerConfiguration.class)
@SpringBootTest(classes = DataTableIntTestConfiguration.class)
class DataTableWebhookEventListenerIntTest {

    private static final long DEV_ENVIRONMENT_ID = 0;
    private static final long TEST_ENVIRONMENT_ID = 1;

    private static final String CREATED_ID = workflowExecutionId("created");
    private static final String OTHER_CREATED_ID = workflowExecutionId("other_created");
    private static final String DELETED_ID = workflowExecutionId("deleted");
    private static final String UPDATED_ID = workflowExecutionId("updated");

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    private DataTableRowService dataTableRowService;

    @Autowired
    private DataTableService dataTableService;

    @MockitoSpyBean
    private DataTableWebhookService dataTableWebhookService;

    @Autowired
    private DataTableWebhookRepository dataTableWebhookRepository;

    @Autowired
    private DisabledWorkflowJobPrincipalAccessor disabledWorkflowJobPrincipalAccessor;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RecordingMessageBroker messageBroker;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @AfterEach
    void afterEach() {
        disabledWorkflowJobPrincipalAccessor.clear();

        dataTableService.dropTable("orders", DEV_ENVIRONMENT_ID);
        dataTableService.dropTable("invoices", DEV_ENVIRONMENT_ID);
    }

    @BeforeEach
    void beforeEach() {
        dataTableService.dropTable("orders", DEV_ENVIRONMENT_ID);
        dataTableService.dropTable("invoices", DEV_ENVIRONMENT_ID);
        dataTableService.createTable(
            "orders", null,
            List.of(
                new ColumnSpec("name", ColumnType.STRING), new ColumnSpec("shipped_on", ColumnType.DATE),
                new ColumnSpec("created_at", ColumnType.DATE_TIME)),
            DEV_ENVIRONMENT_ID);
        dataTableService.createTable(
            "invoices", null, List.of(new ColumnSpec("name", ColumnType.STRING)), DEV_ENVIRONMENT_ID);

        messageBroker.clear();
    }

    @Test
    void testInsertRowsPublishesOneEventPerRowAndRegistration() {
        register("orders", CREATED_ID, RECORD_CREATED, DEV_ENVIRONMENT_ID);
        register("orders", OTHER_CREATED_ID, RECORD_CREATED, DEV_ENVIRONMENT_ID);

        dataTableRowService.insertRows("orders", List.of(row("a"), row("b"), row("c")), DEV_ENVIRONMENT_ID);

        assertThat(triggerEvents()).hasSize(6)
            .extracting(event -> event.getWorkflowExecutionId()
                .toString())
            .containsOnly(CREATED_ID, OTHER_CREATED_ID);
    }

    @Test
    void testRegistrationOfADisabledWorkflowReceivesNoEvents() {
        String disabledId = WorkflowExecutionId.of(PlatformType.AUTOMATION, 1, "disabled-workflow-uuid", "trigger_1")
            .toString();

        disabledWorkflowJobPrincipalAccessor.disable("disabled-workflow-uuid");

        register("orders", disabledId, RECORD_CREATED, DEV_ENVIRONMENT_ID);
        register("orders", CREATED_ID, RECORD_CREATED, DEV_ENVIRONMENT_ID);

        dataTableRowService.insertRow("orders", row("a"), DEV_ENVIRONMENT_ID);

        assertThat(triggerEvents()).singleElement()
            .satisfies(event -> assertThat(event.getWorkflowExecutionId()
                .toString()).isEqualTo(CREATED_ID));
    }

    @Test
    void testOnlyRegistrationsOfTheEventTypeTableAndEnvironmentReceiveEvents() {
        register("orders", UPDATED_ID, RECORD_UPDATED, DEV_ENVIRONMENT_ID);
        register("invoices", OTHER_CREATED_ID, RECORD_CREATED, DEV_ENVIRONMENT_ID);
        register("orders", OTHER_CREATED_ID, RECORD_CREATED, TEST_ENVIRONMENT_ID);
        register("orders", CREATED_ID, RECORD_CREATED, DEV_ENVIRONMENT_ID);

        dataTableRowService.insertRow("orders", row("a"), DEV_ENVIRONMENT_ID);

        assertThat(triggerEvents()).singleElement()
            .satisfies(event -> assertThat(event.getWorkflowExecutionId()
                .toString()).isEqualTo(CREATED_ID));
    }

    @Test
    @SuppressWarnings("unchecked")
    void testBodyNestsTheRowUnderPayloadAndWritesDatesAsIsoText() {
        register("orders", CREATED_ID, RECORD_CREATED, DEV_ENVIRONMENT_ID);

        DataTableRow dataTableRow = dataTableRowService.insertRow(
            "orders", Map.of("name", "a", "shipped_on", "2026-01-31", "created_at", "2026-01-31T10:00:00+02:00"),
            DEV_ENVIRONMENT_ID);

        TriggerWebhookEvent triggerWebhookEvent = triggerEvents().getFirst();

        WebhookRequest webhookRequest = triggerWebhookEvent.getWebhookRequest();

        WebhookRequest.WebhookBodyImpl webhookBody = webhookRequest.body();

        Map<String, Object> content = webhookBody.getContent(new TypeReference<>() {});

        assertThat(webhookRequest.method()).isEqualTo(WebhookMethod.POST);
        assertThat(webhookRequest.headers()).containsEntry(WORKFLOW_EXECUTION_ID_HEADER, List.of(CREATED_ID));
        assertThat(webhookBody.getContentType()).isEqualTo(ContentType.JSON);
        assertThat(webhookBody.getMimeType()).isEqualTo("application/json");
        assertThat(JsonUtils.readMap(webhookBody.getRawContent())).isEqualTo(content);
        assertThat(content).containsEntry("type", "RECORD_CREATED")
            .containsEntry("table", "orders");
        assertThat(Instant.parse((String) content.get("timestamp"))).isNotNull();

        Map<String, Object> payload = (Map<String, Object>) content.get("payload");

        assertThat(((Number) payload.get("id")).longValue()).isEqualTo(dataTableRow.id());
        assertThat((Map<String, Object>) payload.get("values")).containsEntry("name", "a")
            .containsEntry("shipped_on", "2026-01-31")
            .containsEntry("created_at", "2026-01-31T08:00:00.000Z");
    }

    @Test
    void testEventsAreSentAfterCommitAndNotBefore() {
        register("orders", CREATED_ID, RECORD_CREATED, DEV_ENVIRONMENT_ID);

        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        transactionTemplate.executeWithoutResult(status -> {
            dataTableRowService.insertRow("orders", row("a"), DEV_ENVIRONMENT_ID);

            assertThat(triggerEvents()).isEmpty();
        });

        assertThat(triggerEvents()).hasSize(1);
    }

    @Test
    void testRolledBackWriteSendsNothing() {
        register("orders", CREATED_ID, RECORD_CREATED, DEV_ENVIRONMENT_ID);

        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        transactionTemplate.executeWithoutResult(status -> {
            dataTableRowService.insertRow("orders", row("a"), DEV_ENVIRONMENT_ID);

            status.setRollbackOnly();
        });

        assertThat(triggerEvents()).isEmpty();
    }

    @Test
    void testEventPublishedOutsideATransactionIsSentRightAway() {
        register("orders", CREATED_ID, RECORD_CREATED, DEV_ENVIRONMENT_ID);

        applicationEventPublisher.publishEvent(
            new DataTableWebhookEvent(
                "orders", RECORD_CREATED, List.of(new DataTableRow(7L, row("a"))), DEV_ENVIRONMENT_ID));

        assertThat(triggerEvents()).hasSize(1);
    }

    @Test
    void testRegistrationThatCannotBeParsedIsSkippedAndTheOthersStillFire(CapturedOutput capturedOutput) {
        long webhookId = register("orders", CREATED_ID, RECORD_CREATED, DEV_ENVIRONMENT_ID);

        DataTableWebhook dataTableWebhook = dataTableWebhookRepository.findById(webhookId)
            .orElseThrow();

        DataTableWebhook unparsableDataTableWebhook = new DataTableWebhook();

        unparsableDataTableWebhook.setDataTableId(dataTableWebhook.getDataTableId());
        unparsableDataTableWebhook.setEnvironment(dataTableWebhook.getEnvironment());
        unparsableDataTableWebhook.setType(RECORD_CREATED);
        unparsableDataTableWebhook.setWorkflowExecutionId("bm90LWFuLWlk");

        dataTableWebhookRepository.save(unparsableDataTableWebhook);

        dataTableRowService.insertRow("orders", row("a"), DEV_ENVIRONMENT_ID);

        assertThat(triggerEvents()).hasSize(1);
        assertThat(capturedOutput).contains("bm90LWFuLWlk", "could not be parsed");
    }

    @Test
    void testFailingRegistrationLookupIsLoggedAndTheWriteSucceeds(CapturedOutput capturedOutput) {
        doThrow(new IllegalStateException("database unavailable")).when(dataTableWebhookService)
            .listWebhooks("orders", DEV_ENVIRONMENT_ID);

        DataTableRow dataTableRow = dataTableRowService.insertRow("orders", row("a"), DEV_ENVIRONMENT_ID);

        assertThat(dataTableRowService.getRow("orders", dataTableRow.id(), DEV_ENVIRONMENT_ID)).isNotNull();
        assertThat(triggerEvents()).isEmpty();
        assertThat(capturedOutput).contains("could not be listed");
    }

    @Test
    void testRegistrationLookupThatFailsInTheDatabaseDoesNotRollBackTheWrite(CapturedOutput capturedOutput) {
        register("orders", DELETED_ID, RECORD_DELETED, DEV_ENVIRONMENT_ID);

        dataTableRowService.insertRow("orders", row("a"), DEV_ENVIRONMENT_ID);

        jdbcTemplate.execute("ALTER TABLE data_table_webhook RENAME TO data_table_webhook_unavailable");

        try {
            TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

            transactionTemplate.executeWithoutResult(
                status -> dataTableRowService.deleteAllRows("orders", DEV_ENVIRONMENT_ID));
        } finally {
            jdbcTemplate.execute("ALTER TABLE data_table_webhook_unavailable RENAME TO data_table_webhook");
        }

        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM dt_0_orders", Long.class)).isZero();
        assertThat(triggerEvents()).isEmpty();
        assertThat(capturedOutput).contains("could not be listed");
    }

    @Test
    void testInsertRowsOfManyRowsPublishesOneEventPerRow() {
        register("orders", CREATED_ID, RECORD_CREATED, DEV_ENVIRONMENT_ID);

        dataTableRowService.insertRows(
            "orders", IntStream.range(0, 300)
                .mapToObj(index -> row("row " + index))
                .toList(),
            DEV_ENVIRONMENT_ID);

        assertThat(triggerEvents()).hasSize(300);
    }

    private long register(String baseName, String workflowExecutionId, DataTableWebhookType type, long environmentId) {
        return dataTableWebhookService.addWebhook(
            baseName, workflowExecutionId, "http://localhost:9555/webhooks/" + workflowExecutionId, type,
            environmentId);
    }

    private static Map<String, Object> row(String name) {
        return Map.of("name", name);
    }

    private List<TriggerWebhookEvent> triggerEvents() {
        return messageBroker.messages()
            .stream()
            .filter(sentMessage -> sentMessage.route() == TriggerCoordinatorMessageRoute.TRIGGER_WEBHOOK_EVENTS)
            .map(sentMessage -> (TriggerWebhookEvent) sentMessage.message())
            .toList();
    }

    private static String workflowExecutionId(String triggerName) {
        WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.of(
            PlatformType.AUTOMATION, 1, "workflow-uuid", triggerName);

        return workflowExecutionId.toString();
    }
}
