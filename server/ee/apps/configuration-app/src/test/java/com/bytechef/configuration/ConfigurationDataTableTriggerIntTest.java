/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.message.broker.MessageBroker;
import com.bytechef.message.route.MessageRoute;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.data.table.configuration.domain.DataTableWebhookType;
import com.bytechef.platform.data.table.configuration.service.DataTableService;
import com.bytechef.platform.data.table.configuration.service.DataTableWebhookService;
import com.bytechef.platform.data.table.domain.ColumnSpec;
import com.bytechef.platform.data.table.domain.ColumnType;
import com.bytechef.platform.data.table.execution.service.DataTableRowService;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.platform.workflow.coordinator.event.TriggerWebhookEvent;
import com.bytechef.platform.workflow.coordinator.message.route.TriggerCoordinatorMessageRoute;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalAccessor;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalAccessorRegistry;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import com.bytechef.test.config.testcontainers.RedisContainerConfiguration;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Import({
    ConfigurationDataTableTriggerIntTest.RecordingMessageBrokerConfiguration.class,
    PostgreSQLContainerConfiguration.class, RedisContainerConfiguration.class
})
@SpringBootTest(
    classes = ConfigurationApplication.class, properties = {
        "bytechef.mail.host=mailpit", "bytechef.mail.port=1025", "spring.liquibase.contexts=configuration,execution"
    })
class ConfigurationDataTableTriggerIntTest {

    private static final long ENVIRONMENT_ID = 0;

    private static final String WORKFLOW_EXECUTION_ID = WorkflowExecutionId.of(
        PlatformType.AUTOMATION, 1, "workflow-uuid", "trigger_1")
        .toString();

    @Autowired
    private DataTableRowService dataTableRowService;

    @Autowired
    private DataTableService dataTableService;

    @Autowired
    private DataTableWebhookService dataTableWebhookService;

    @MockitoSpyBean
    private JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry;

    @Autowired
    private RecordingMessageBroker recordingMessageBroker;

    @AfterEach
    void afterEach() {
        dataTableService.dropTable("orders", ENVIRONMENT_ID);
    }

    @BeforeEach
    void beforeEach() {
        JobPrincipalAccessor jobPrincipalAccessor = mock(JobPrincipalAccessor.class);

        when(jobPrincipalAccessor.isWorkflowEnabled(anyLong(), anyString())).thenReturn(true);

        doReturn(jobPrincipalAccessor).when(jobPrincipalAccessorRegistry)
            .getJobPrincipalAccessor(PlatformType.AUTOMATION);

        dataTableService.dropTable("orders", ENVIRONMENT_ID);
        dataTableService.createTable("orders", List.of(new ColumnSpec("name", ColumnType.STRING)), ENVIRONMENT_ID);

        recordingMessageBroker.clear();
    }

    @Test
    void testInsertedRowTriggersTheRegisteredWorkflowThroughTheMessageBroker() throws InterruptedException {
        dataTableWebhookService.addWebhook(
            "orders", WORKFLOW_EXECUTION_ID, "http://localhost:9555/webhooks/" + WORKFLOW_EXECUTION_ID,
            DataTableWebhookType.RECORD_CREATED, ENVIRONMENT_ID);

        dataTableRowService.insertRow("orders", Map.of("name", "a"), ENVIRONMENT_ID);

        List<RecordingMessageBroker.SentMessage> triggerMessages = awaitTriggerMessages();

        assertThat(triggerMessages).singleElement()
            .satisfies(sentMessage -> {
                assertThat(sentMessage.route()).isEqualTo(TriggerCoordinatorMessageRoute.TRIGGER_WEBHOOK_EVENTS);
                assertThat(((TriggerWebhookEvent) sentMessage.message()).getWorkflowExecutionId()
                    .toString()).isEqualTo(WORKFLOW_EXECUTION_ID);
            });
    }

    private List<RecordingMessageBroker.SentMessage> awaitTriggerMessages() throws InterruptedException {
        Instant deadline = Instant.now()
            .plus(Duration.ofSeconds(10));

        while (Instant.now()
            .isBefore(deadline)) {

            List<RecordingMessageBroker.SentMessage> triggerMessages = triggerMessages();

            if (!triggerMessages.isEmpty()) {
                Thread.sleep(200);

                return triggerMessages();
            }

            Thread.sleep(50);
        }

        return triggerMessages();
    }

    private List<RecordingMessageBroker.SentMessage> triggerMessages() {
        return recordingMessageBroker.messages()
            .stream()
            .filter(sentMessage -> sentMessage.message() instanceof TriggerWebhookEvent)
            .toList();
    }

    @TestConfiguration
    static class RecordingMessageBrokerConfiguration {

        @Bean
        @Primary
        RecordingMessageBroker recordingMessageBroker() {
            return new RecordingMessageBroker();
        }
    }

    static class RecordingMessageBroker implements MessageBroker {

        private final List<SentMessage> messages = new CopyOnWriteArrayList<>();

        @Override
        public void send(MessageRoute route, Object message) {
            messages.add(new SentMessage(route, message));
        }

        void clear() {
            messages.clear();
        }

        List<SentMessage> messages() {
            return List.copyOf(messages);
        }

        record SentMessage(MessageRoute route, Object message) {
        }
    }
}
