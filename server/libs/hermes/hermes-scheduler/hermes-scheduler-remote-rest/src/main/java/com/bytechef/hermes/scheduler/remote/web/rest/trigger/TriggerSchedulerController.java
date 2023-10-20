
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

package com.bytechef.hermes.scheduler.remote.web.rest.trigger;

import com.bytechef.hermes.scheduler.TriggerScheduler;
import com.bytechef.hermes.workflow.WorkflowExecutionId;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
@RestController
@RequestMapping("${openapi.openAPIDefinition.base-path:}/internal")
public class TriggerSchedulerController {

    private final TriggerScheduler triggerScheduler;

    public TriggerSchedulerController(TriggerScheduler triggerScheduler) {
        this.triggerScheduler = triggerScheduler;
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/trigger-scheduler/cancel-dynamic-webhook-trigger-refresh",
        consumes = {
            "application/json"
        })
    void cancelDynamicWebhookRefreshTask(@Valid @RequestBody String workflowExecutionId) {
        triggerScheduler.cancelDynamicWebhookTriggerRefresh(workflowExecutionId);
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/trigger-scheduler/cancel-polling-trigger",
        consumes = {
            "application/json"
        })
    void cancelPollTask(@Valid @RequestBody String workflowExecutionId) {
        triggerScheduler.cancelPollingTrigger(workflowExecutionId);
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/trigger-scheduler/cancel-schedule-trigger",
        consumes = {
            "application/json"
        })
    void cancelTriggerWorkflowTask(@Valid @RequestBody String workflowExecutionId) {
        triggerScheduler.cancelScheduleTrigger(workflowExecutionId);
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/trigger-scheduler/schedule-dynamic-webhook-trigger-refresh",
        consumes = {
            "application/json"
        })
    void scheduleDynamicWebhookRefreshTask(
        @Valid @RequestBody DynamicWebhookRefreshTaskRequest dynamicWebhookRefreshTaskRequest) {

        triggerScheduler.scheduleDynamicWebhookTriggerRefresh(
            dynamicWebhookRefreshTaskRequest.webhookExpirationDate, dynamicWebhookRefreshTaskRequest.componentName,
            dynamicWebhookRefreshTaskRequest.componentVersion, dynamicWebhookRefreshTaskRequest.workflowExecutionId);
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/trigger-scheduler/schedule-polling-trigger",
        consumes = {
            "application/json"
        })
    void schedulePollTask(@Valid @RequestBody WorkflowExecutionId workflowExecutionId) {
        triggerScheduler.schedulePollingTrigger(workflowExecutionId);
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/trigger-scheduler/schedule-schedule-trigger",
        consumes = {
            "application/json"
        })
    void scheduleTriggerWorkflowTask(@Valid @RequestBody TriggerWorkflowTaskRequest triggerWorkflowTaskRequest) {
        triggerScheduler.scheduleScheduleTrigger(
            triggerWorkflowTaskRequest.pattern, triggerWorkflowTaskRequest.zoneId, triggerWorkflowTaskRequest.output,
            triggerWorkflowTaskRequest.workflowExecutionId);
    }

    @SuppressFBWarnings("EI")
    private record DynamicWebhookRefreshTaskRequest(
        LocalDateTime webhookExpirationDate, String componentName, int componentVersion,
        WorkflowExecutionId workflowExecutionId) {
    }

    @SuppressFBWarnings("EI")
    private record TriggerWorkflowTaskRequest(
        String pattern, String zoneId, Map<String, Object> output, String workflowExecutionId) {
    }
}
