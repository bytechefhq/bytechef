/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.scheduler.aws.listener;

import static com.bytechef.ee.platform.scheduler.aws.constant.AwsTriggerSchedulerConstants.DYNAMIC_WEBHOOK_TRIGGER_REFRESH;
import static com.bytechef.ee.platform.scheduler.aws.constant.AwsTriggerSchedulerConstants.SPLITTER_PATTERN;
import static com.bytechef.ee.platform.scheduler.aws.constant.AwsTriggerSchedulerConstants.TRIGGER_SCHEDULER_DYNAMIC_WEBHOOK_TRIGGER_REFRESH_QUEUE;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.component.definition.TriggerDefinition.WebhookEnableOutput;
import com.bytechef.platform.component.facade.TriggerDefinitionFacade;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.configuration.instance.accessor.InstanceAccessor;
import com.bytechef.platform.configuration.instance.accessor.InstanceAccessorRegistry;
import com.bytechef.platform.definition.WorkflowNodeType;
import com.bytechef.platform.workflow.execution.WorkflowExecutionId;
import com.bytechef.platform.workflow.execution.service.TriggerStateService;
import io.awspring.cloud.sqs.annotation.SqsListener;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import software.amazon.awssdk.services.scheduler.SchedulerClient;
import software.amazon.awssdk.services.scheduler.model.UpdateScheduleRequest;

/**
 * @version ee
 *
 * @author Marko Kriskovic
 */
public class DynamicWebhookTriggerRefreshListener {

    private final InstanceAccessorRegistry instanceAccessorRegistry;
    private final TriggerDefinitionFacade remoteTriggerDefinitionFacade;
    private final TriggerStateService triggerStateService;
    private final WorkflowService workflowService;
    private final SchedulerClient schedulerClient;

    public DynamicWebhookTriggerRefreshListener(
        InstanceAccessorRegistry instanceAccessorRegistry, SchedulerClient schedulerClient,
        TriggerDefinitionFacade remoteTriggerDefinitionFacade, TriggerStateService triggerStateService,
        WorkflowService workflowService) {

        this.schedulerClient = schedulerClient;
        this.instanceAccessorRegistry = instanceAccessorRegistry;
        this.remoteTriggerDefinitionFacade = remoteTriggerDefinitionFacade;
        this.triggerStateService = triggerStateService;
        this.workflowService = workflowService;
    }

    @SqsListener(TRIGGER_SCHEDULER_DYNAMIC_WEBHOOK_TRIGGER_REFRESH_QUEUE)
    public void onSchedule(String message) {
        String[] split = message.split(SPLITTER_PATTERN);
        String workflowExecutionId = split[1];
        Long connectionId = Long.valueOf(split[0]);

        LocalDateTime webhookExpirationDate = refreshDynamicWebhookTrigger(
            WorkflowExecutionId.parse(workflowExecutionId), connectionId);

        if (webhookExpirationDate != null) {
            schedulerClient.updateSchedule(
                UpdateScheduleRequest.builder()
                    .clientToken(DYNAMIC_WEBHOOK_TRIGGER_REFRESH + workflowExecutionId.substring(16))
                    .groupName(DYNAMIC_WEBHOOK_TRIGGER_REFRESH)
                    .name(DYNAMIC_WEBHOOK_TRIGGER_REFRESH + workflowExecutionId.substring(0, 16))
                    .startDate(webhookExpirationDate.toInstant(ZoneOffset.UTC))
                    .build());
        }
    }

    private WorkflowNodeType getComponentOperation(WorkflowExecutionId workflowExecutionId) {
        InstanceAccessor instanceAccessor = instanceAccessorRegistry.getInstanceAccessor(workflowExecutionId.getType());

        String workflowId = instanceAccessor.getWorkflowId(
            workflowExecutionId.getInstanceId(), workflowExecutionId.getWorkflowReferenceCode());

        Workflow workflow = workflowService.getWorkflow(workflowId);

        WorkflowTrigger workflowTrigger = WorkflowTrigger.of(workflowExecutionId.getTriggerName(), workflow);

        return WorkflowNodeType.ofType(workflowTrigger.getType());
    }

    private LocalDateTime refreshDynamicWebhookTrigger(WorkflowExecutionId workflowExecutionId, Long connectionId) {
        WorkflowNodeType workflowNodeType = getComponentOperation(workflowExecutionId);
        WebhookEnableOutput output = OptionalUtils.get(triggerStateService.fetchValue(workflowExecutionId));
        LocalDateTime webhookExpirationDate = null;

        output = remoteTriggerDefinitionFacade.executeDynamicWebhookRefresh(
            workflowNodeType.componentName(), workflowNodeType.componentVersion(),
            workflowNodeType.componentOperationName(), output.parameters(), connectionId);

        if (output != null) {
            triggerStateService.save(workflowExecutionId, output);

            webhookExpirationDate = output.webhookExpirationDate();
        }

        return webhookExpirationDate;
    }
}
