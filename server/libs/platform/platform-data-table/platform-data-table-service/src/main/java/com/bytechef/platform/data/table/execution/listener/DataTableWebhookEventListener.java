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

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.component.definition.TriggerDefinition.WebhookBody.ContentType;
import com.bytechef.component.definition.TriggerDefinition.WebhookMethod;
import com.bytechef.platform.component.trigger.WebhookRequest;
import com.bytechef.platform.component.trigger.WebhookRequest.WebhookBodyImpl;
import com.bytechef.platform.configuration.facade.WorkflowNodeTestOutputFacade;
import com.bytechef.platform.data.table.configuration.domain.DataTableWebhookType;
import com.bytechef.platform.data.table.configuration.service.DataTableWebhookService;
import com.bytechef.platform.data.table.execution.domain.DataTableRow;
import com.bytechef.platform.data.table.execution.event.DataTableWebhookEvent;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.platform.workflow.coordinator.event.TriggerWebhookEvent;
import com.bytechef.platform.workflow.coordinator.event.TriggerWebhookEvent.WebhookParameters;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalAccessor;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalAccessorRegistry;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @author Ivica Cardic
 */
@Component
public class DataTableWebhookEventListener {

    public static final String WORKFLOW_EXECUTION_ID_HEADER = "X-ByteChef-Workflow-Execution-Id";

    private static final long EDITOR_TEST_JOB_PRINCIPAL_ID = -1;

    private static final Logger log = LoggerFactory.getLogger(DataTableWebhookEventListener.class);

    private final ApplicationEventPublisher applicationEventPublisher;
    private final DataTableWebhookService dataTableWebhookService;
    private final JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry;
    private final TransactionTemplate lookupTransactionTemplate;
    private final TransactionTemplate testOutputTransactionTemplate;
    private final WorkflowNodeTestOutputFacade workflowNodeTestOutputFacade;

    @SuppressFBWarnings("EI")
    public DataTableWebhookEventListener(
        DataTableWebhookService dataTableWebhookService, ApplicationEventPublisher applicationEventPublisher,
        JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry,
        PlatformTransactionManager platformTransactionManager,
        WorkflowNodeTestOutputFacade workflowNodeTestOutputFacade) {

        this.applicationEventPublisher = applicationEventPublisher;
        this.dataTableWebhookService = dataTableWebhookService;
        this.jobPrincipalAccessorRegistry = jobPrincipalAccessorRegistry;
        this.workflowNodeTestOutputFacade = workflowNodeTestOutputFacade;

        lookupTransactionTemplate = new TransactionTemplate(platformTransactionManager);

        lookupTransactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_NESTED);

        testOutputTransactionTemplate = new TransactionTemplate(platformTransactionManager);

        testOutputTransactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @EventListener
    public void onDataTableWebhookEvent(DataTableWebhookEvent event) {
        String baseName = event.getBaseName();
        long environmentId = event.getEnvironmentId();
        List<DataTableRow> rows = event.getRows();
        DataTableWebhookType type = event.getType();

        List<DataTableWebhookService.Webhook> hooks;

        try {
            hooks = lookupTransactionTemplate.execute(status -> listWebhooks(baseName, environmentId, type));
        } catch (RuntimeException runtimeException) {
            log.error(
                "Data table webhooks for {} on table '{}' in environment {} could not be listed; {} rows were not sent",
                type, baseName, environmentId, rows.size(), runtimeException);

            return;
        }

        for (DataTableWebhookService.Webhook hook : hooks) {
            WorkflowExecutionId workflowExecutionId;

            try {
                workflowExecutionId = WorkflowExecutionId.parse(hook.workflowExecutionId());
            } catch (RuntimeException runtimeException) {
                log.error(
                    "Data table webhook {} registration '{}' could not be parsed; {} rows were not sent",
                    hook.id(), hook.workflowExecutionId(), rows.size(), runtimeException);

                continue;
            }

            if (workflowExecutionId.getJobPrincipalId() == EDITOR_TEST_JOB_PRINCIPAL_ID) {
                saveTestOutputs(hook, workflowExecutionId, environmentId, type, baseName, rows);

                continue;
            }

            if (!isWorkflowEnabled(hook, workflowExecutionId, rows.size())) {
                continue;
            }

            for (DataTableRow row : rows) {
                WebhookRequest webhookRequest = createWebhookRequest(
                    hook.workflowExecutionId(), createBody(type, baseName, row));

                applicationEventPublisher.publishEvent(
                    new TriggerWebhookEvent(new WebhookParameters(workflowExecutionId, webhookRequest)));
            }
        }
    }

    private boolean isWorkflowEnabled(
        DataTableWebhookService.Webhook hook, WorkflowExecutionId workflowExecutionId, int rowCount) {

        try {
            Boolean workflowEnabled = lookupTransactionTemplate.execute(status -> {
                JobPrincipalAccessor jobPrincipalAccessor = jobPrincipalAccessorRegistry.getJobPrincipalAccessor(
                    workflowExecutionId.getType());

                return jobPrincipalAccessor.isWorkflowEnabled(
                    workflowExecutionId.getJobPrincipalId(), workflowExecutionId.getWorkflowUuid());
            });

            if (Boolean.TRUE.equals(workflowEnabled)) {
                return true;
            }

            log.debug("Data table webhook {} belongs to a disabled workflow; {} rows were not sent", hook.id(),
                rowCount);
        } catch (RuntimeException runtimeException) {
            log.error(
                "Data table webhook {} workflow state could not be read; {} rows were not sent", hook.id(), rowCount,
                runtimeException);
        }

        return false;
    }

    private void saveTestOutputs(
        DataTableWebhookService.Webhook hook, WorkflowExecutionId workflowExecutionId, long environmentId,
        DataTableWebhookType type, String baseName, List<DataTableRow> rows) {

        List<WebhookRequest> webhookRequests = new ArrayList<>();

        for (DataTableRow row : rows) {
            webhookRequests.add(createWebhookRequest(hook.workflowExecutionId(), createBody(type, baseName, row)));
        }

        Runnable saveRunnable = () -> {
            for (WebhookRequest webhookRequest : webhookRequests) {
                try {
                    testOutputTransactionTemplate.executeWithoutResult(
                        status -> workflowNodeTestOutputFacade.saveWorkflowNodeTestOutput(
                            workflowExecutionId, environmentId, webhookRequest.asValidated()));
                } catch (RuntimeException runtimeException) {
                    log.error(
                        "Data table webhook {} test output could not be saved", hook.id(), runtimeException);
                }
            }
        };

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {

                @Override
                public void afterCommit() {
                    saveRunnable.run();
                }
            });
        } else {
            saveRunnable.run();
        }
    }

    private List<DataTableWebhookService.Webhook> listWebhooks(
        String baseName, long environmentId, DataTableWebhookType type) {

        return dataTableWebhookService.listWebhooks(baseName, environmentId)
            .stream()
            .filter(webhook -> webhook.type() == type)
            .toList();
    }

    private static Map<String, Object> createBody(DataTableWebhookType type, String baseName, DataTableRow row) {
        Map<String, Object> body = new LinkedHashMap<>();
        Map<String, Object> payload = new LinkedHashMap<>();

        payload.put("id", row.id());
        payload.put("values", toPayloadValues(row.values()));

        body.put("type", type.name());
        body.put("table", baseName);
        body.put("timestamp", String.valueOf(Instant.now()));
        body.put("payload", payload);

        return body;
    }

    private static WebhookRequest createWebhookRequest(String workflowExecutionId, Map<String, Object> body) {
        String rawContent = JsonUtils.write(body);

        WebhookBodyImpl webhookBody = new WebhookBodyImpl(
            JsonUtils.read(rawContent), ContentType.JSON, "application/json", rawContent);

        return new WebhookRequest(
            Map.of(WORKFLOW_EXECUTION_ID_HEADER, List.of(workflowExecutionId)), Map.of(), webhookBody,
            WebhookMethod.POST);
    }

    private static Map<String, @Nullable Object> toPayloadValues(Map<String, Object> values) {
        Map<String, @Nullable Object> payloadValues = new HashMap<>();

        for (Map.Entry<String, Object> entry : values.entrySet()) {
            Object value = entry.getValue();

            if (value instanceof Date date) {
                LocalDate localDate = date.toLocalDate();

                value = localDate.toString();
            }

            payloadValues.put(entry.getKey(), value);
        }

        return payloadValues;
    }
}
