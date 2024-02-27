/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.platform.scheduler.job;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.commons.util.DateUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.component.definition.TriggerDefinition.DynamicWebhookEnableOutput;
import com.bytechef.platform.registry.definition.WorkflowNodeType;
import com.bytechef.platform.component.registry.facade.TriggerDefinitionFacade;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.workflow.execution.WorkflowExecutionId;
import com.bytechef.platform.workflow.execution.service.TriggerStateService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.LocalDateTime;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Ivica Cardic
 */
public class DynamicWebhookTriggerRefreshJob implements Job {

    private TriggerDefinitionFacade remoteTriggerDefinitionFacade;
    private TriggerStateService triggerStateService;
    private WorkflowService workflowService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobDataMap jobDataMap = context.getMergedJobDataMap();

        String workflowExecutionId = jobDataMap.getString("workflowExecutionId");

        LocalDateTime webhookExpirationDate = refreshDynamicWebhookTrigger(
            WorkflowExecutionId.parse(workflowExecutionId));

        if (webhookExpirationDate != null) {
            Scheduler scheduler = context.getScheduler();
            try {
                TriggerKey triggerKey = TriggerKey.triggerKey(
                    workflowExecutionId, "DynamicWebhookTriggerRefresh");

                scheduler.rescheduleJob(
                    triggerKey,
                    TriggerBuilder.newTrigger()
                        .withIdentity(TriggerKey.triggerKey("myTrigger", "myTriggerGroup"))
                        .startAt(DateUtils.getDate(webhookExpirationDate))
                        .build());
            } catch (SchedulerException e) {
                throw new JobExecutionException(e);
            }
        }
    }

    @Autowired
    public void setRemoteTriggerDefinitionFacade(TriggerDefinitionFacade triggerDefinitionService) {
        this.remoteTriggerDefinitionFacade = triggerDefinitionService;
    }

    @Autowired
    public void setTriggerStateService(TriggerStateService triggerStateService) {
        this.triggerStateService = triggerStateService;
    }

    @Autowired
    @SuppressFBWarnings("EI")
    public void setWorkflowService(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    private WorkflowNodeType getComponentOperation(WorkflowExecutionId workflowExecutionId) {
        Workflow workflow = workflowService.getWorkflow(workflowExecutionId.getWorkflowId());

        WorkflowTrigger workflowTrigger = WorkflowTrigger.of(workflowExecutionId.getTriggerName(), workflow);

        return WorkflowNodeType.ofType(workflowTrigger.getType());
    }

    private LocalDateTime refreshDynamicWebhookTrigger(WorkflowExecutionId workflowExecutionId) {
        WorkflowNodeType workflowNodeType = getComponentOperation(workflowExecutionId);
        DynamicWebhookEnableOutput output = OptionalUtils.get(triggerStateService.fetchValue(workflowExecutionId));
        LocalDateTime webhookExpirationDate = null;

        output = remoteTriggerDefinitionFacade.executeDynamicWebhookRefresh(
            workflowNodeType.componentName(), workflowNodeType.componentVersion(),
            workflowNodeType.componentOperationName(), output.parameters());

        if (output != null) {
            triggerStateService.save(workflowExecutionId, output);

            webhookExpirationDate = output.webhookExpirationDate();
        }

        return webhookExpirationDate;
    }
}
