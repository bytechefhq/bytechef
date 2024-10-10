package com.bytechef.ee.platform.scheduler.aws.listeners;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.component.definition.TriggerDefinition;
import com.bytechef.platform.component.facade.TriggerDefinitionFacade;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.configuration.instance.accessor.InstanceAccessor;
import com.bytechef.platform.configuration.instance.accessor.InstanceAccessorRegistry;
import com.bytechef.platform.definition.WorkflowNodeType;
import com.bytechef.platform.workflow.execution.WorkflowExecutionId;
import com.bytechef.platform.workflow.execution.service.TriggerStateService;
import io.awspring.cloud.sqs.annotation.SqsListener;
import software.amazon.awssdk.services.scheduler.SchedulerClient;
import software.amazon.awssdk.services.scheduler.model.UpdateScheduleRequest;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class DynamicWebhookListener {
    private InstanceAccessorRegistry instanceAccessorRegistry;
    private TriggerDefinitionFacade remoteTriggerDefinitionFacade;
    private TriggerStateService triggerStateService;
    private WorkflowService workflowService;

    private final SchedulerClient schedulerClient;
    private static String WEBHOOK_TRIGGER = "DynamicWebhookTriggerRefresh";

    public DynamicWebhookListener(SchedulerClient schedulerClient, InstanceAccessorRegistry instanceAccessorRegistry, TriggerDefinitionFacade remoteTriggerDefinitionFacade, TriggerStateService triggerStateService, WorkflowService workflowService) {
        this.schedulerClient = schedulerClient;
        this.instanceAccessorRegistry = instanceAccessorRegistry;
        this.remoteTriggerDefinitionFacade = remoteTriggerDefinitionFacade;
        this.triggerStateService = triggerStateService;
        this.workflowService = workflowService;
    }

    @SqsListener("webhook-queue")
    public void onSchedule(String message) {
        String[] split = message.split("\\|_\\$plitter_\\|");
        String workflowExecutionId = split[1];
        Long connectionId = Long.valueOf(split[0]);

        LocalDateTime webhookExpirationDate = refreshDynamicWebhookTrigger(
            WorkflowExecutionId.parse(workflowExecutionId), connectionId);

        if (webhookExpirationDate != null) {
            schedulerClient.updateSchedule(UpdateScheduleRequest.builder()
                    .clientToken(WEBHOOK_TRIGGER + workflowExecutionId.substring(16))
                    .groupName(WEBHOOK_TRIGGER)
                    .name(WEBHOOK_TRIGGER + workflowExecutionId.substring(0,16))
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
        TriggerDefinition.WebhookEnableOutput output = OptionalUtils.get(triggerStateService.fetchValue(workflowExecutionId));
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
