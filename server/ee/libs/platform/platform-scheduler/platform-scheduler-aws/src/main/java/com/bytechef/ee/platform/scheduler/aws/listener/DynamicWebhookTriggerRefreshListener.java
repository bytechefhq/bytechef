/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.scheduler.aws.listener;

import static com.bytechef.ee.platform.scheduler.aws.constant.AwsTriggerSchedulerConstants.DYNAMIC_WEBHOOK_TRIGGER_REFRESH;
import static com.bytechef.ee.platform.scheduler.aws.constant.AwsTriggerSchedulerConstants.SCHEDULER_DYNAMIC_WEBHOOK_TRIGGER_REFRESH_QUEUE;
import static com.bytechef.ee.platform.scheduler.aws.constant.AwsTriggerSchedulerConstants.SPLITTER;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.component.definition.TriggerDefinition.WebhookEnableOutput;
import com.bytechef.platform.component.facade.TriggerDefinitionFacade;
import com.bytechef.platform.configuration.accessor.JobPrincipalAccessor;
import com.bytechef.platform.configuration.accessor.JobPrincipalAccessorRegistry;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.definition.WorkflowNodeType;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.platform.workflow.execution.service.TriggerStateService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.awspring.cloud.sqs.annotation.SqsListener;
import java.time.Instant;
import software.amazon.awssdk.services.scheduler.SchedulerClient;
import software.amazon.awssdk.services.scheduler.model.UpdateScheduleRequest;

/**
 * @version ee
 *
 * @author Marko Kriskovic
 */
public class DynamicWebhookTriggerRefreshListener {

    private final JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry;
    private final TriggerDefinitionFacade remoteTriggerDefinitionFacade;
    private final TriggerStateService triggerStateService;
    private final WorkflowService workflowService;
    private final SchedulerClient schedulerClient;

    @SuppressFBWarnings("EI")
    public DynamicWebhookTriggerRefreshListener(
        JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry, SchedulerClient schedulerClient,
        TriggerDefinitionFacade remoteTriggerDefinitionFacade, TriggerStateService triggerStateService,
        WorkflowService workflowService) {

        this.schedulerClient = schedulerClient;
        this.jobPrincipalAccessorRegistry = jobPrincipalAccessorRegistry;
        this.remoteTriggerDefinitionFacade = remoteTriggerDefinitionFacade;
        this.triggerStateService = triggerStateService;
        this.workflowService = workflowService;
    }

    @SqsListener(SCHEDULER_DYNAMIC_WEBHOOK_TRIGGER_REFRESH_QUEUE)
    public void onSchedule(String message) {
        String[] split = message.split(SPLITTER);
        String workflowExecutionId = split[0];
        Long connectionId = Long.valueOf(split[1]);

        Instant webhookExpirationDate = refreshDynamicWebhookTrigger(
            WorkflowExecutionId.parse(workflowExecutionId), connectionId);

        if (webhookExpirationDate != null) {
            schedulerClient.updateSchedule(
                UpdateScheduleRequest.builder()
                    .clientToken(DYNAMIC_WEBHOOK_TRIGGER_REFRESH + workflowExecutionId.substring(16))
                    .groupName(DYNAMIC_WEBHOOK_TRIGGER_REFRESH)
                    .name(DYNAMIC_WEBHOOK_TRIGGER_REFRESH + workflowExecutionId.substring(0, 16))
                    .startDate(webhookExpirationDate)
                    .build());
        }
    }

    private WorkflowNodeType getComponentOperation(WorkflowExecutionId workflowExecutionId) {
        JobPrincipalAccessor jobPrincipalAccessor =
            jobPrincipalAccessorRegistry.getJobPrincipalAccessor(workflowExecutionId.getType());

        String workflowId = jobPrincipalAccessor.getWorkflowId(
            workflowExecutionId.getJobPrincipalId(), workflowExecutionId.getWorkflowUuid());

        Workflow workflow = workflowService.getWorkflow(workflowId);

        WorkflowTrigger workflowTrigger = WorkflowTrigger.of(workflowExecutionId.getTriggerName(), workflow);

        return WorkflowNodeType.ofType(workflowTrigger.getType());
    }

    private Instant refreshDynamicWebhookTrigger(WorkflowExecutionId workflowExecutionId, Long connectionId) {
        WorkflowNodeType workflowNodeType = getComponentOperation(workflowExecutionId);
        WebhookEnableOutput output = OptionalUtils.get(triggerStateService.fetchValue(workflowExecutionId));
        Instant webhookExpirationDate = null;

        output = remoteTriggerDefinitionFacade.executeDynamicWebhookRefresh(
            workflowNodeType.name(), workflowNodeType.version(),
            workflowNodeType.operation(), output.parameters(), connectionId);

        if (output != null) {
            triggerStateService.save(workflowExecutionId, output);

            webhookExpirationDate = output.webhookExpirationDate();
        }

        return webhookExpirationDate;
    }
}
