
/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.hermes.scheduler.job;

import com.bytechef.commons.util.DateUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.hermes.component.definition.TriggerDefinition;
import com.bytechef.hermes.component.registry.service.TriggerDefinitionService;
import com.bytechef.hermes.execution.WorkflowExecutionId;
import com.bytechef.hermes.execution.service.TriggerStateService;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;

/**
 * @author Ivica Cardic
 */
public class DynamicWebhookTriggerRefreshJob implements Job {

    private TriggerDefinitionService triggerDefinitionService;
    private TriggerStateService triggerStateService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobDataMap jobDataMap = context.getMergedJobDataMap();

        String workflowExecutionIdString = jobDataMap.getString("workflowExecutionId");

        WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.parse(workflowExecutionIdString);

        LocalDateTime webhookExpirationDate = refreshDynamicWebhookTrigger(
            workflowExecutionId, workflowExecutionId.getComponentName(),
            workflowExecutionId.getComponentVersion());

        if (webhookExpirationDate != null) {
            Scheduler scheduler = context.getScheduler();
            try {
                TriggerKey triggerKey = TriggerKey.triggerKey(
                    workflowExecutionIdString, "DynamicWebhookTriggerRefresh");

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
    public void setTriggerDefinitionService(TriggerDefinitionService triggerDefinitionService) {
        this.triggerDefinitionService = triggerDefinitionService;
    }

    @Autowired
    public void setTriggerStateService(TriggerStateService triggerStateService) {
        this.triggerStateService = triggerStateService;
    }

    private LocalDateTime refreshDynamicWebhookTrigger(
        WorkflowExecutionId workflowExecutionId, String componentName, int componentVersion) {

        LocalDateTime webhookExpirationDate = null;
        TriggerDefinition.DynamicWebhookEnableOutput output = OptionalUtils.get(
            triggerStateService.fetchValue(workflowExecutionId));

        output = triggerDefinitionService.executeDynamicWebhookRefresh(
            componentName, componentVersion, workflowExecutionId.getComponentTriggerName(), output);

        if (output != null) {
            triggerStateService.save(workflowExecutionId, output);

            webhookExpirationDate = output.webhookExpirationDate();
        }

        return webhookExpirationDate;
    }
}
