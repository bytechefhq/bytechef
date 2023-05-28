
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

package com.bytechef.hermes.scheduler.web.rest;

import com.bytechef.hermes.scheduler.TriggerScheduler;
import com.bytechef.hermes.trigger.WorkflowTrigger;
import com.bytechef.hermes.workflow.WorkflowExecutionId;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

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
        value = "/trigger-scheduler/cancel-dynamic-webhook-refresh-task",
        consumes = {
            "application/json"
        })
    void cancelDynamicWebhookRefreshTask(@Valid @RequestBody WorkflowExecutionId workflowExecutionId) {
        triggerScheduler.cancelDynamicWebhookRefreshTask(workflowExecutionId);
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/trigger-scheduler/cancel-poll-task",
        consumes = {
            "application/json"
        })
    void cancelPollTask(@Valid @RequestBody WorkflowExecutionId workflowExecutionId) {
        triggerScheduler.cancelPollTask(workflowExecutionId);
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/trigger-scheduler/schedule-dynamic-webhook-refresh-task",
        consumes = {
            "application/json"
        })
    void scheduleDynamicWebhookRefreshTask(
        @Valid @RequestBody DynamicWebhookRefreshOneTimeTaskRequest dynamicWebhookRefreshOneTimeTaskRequest) {

        triggerScheduler.scheduleDynamicWebhookRefreshTask(
            dynamicWebhookRefreshOneTimeTaskRequest.workflowTrigger,
            dynamicWebhookRefreshOneTimeTaskRequest.workflowExecutionId,
            dynamicWebhookRefreshOneTimeTaskRequest.webhookExpirationDate);
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/trigger-scheduler/schedule-poll-task",
        consumes = {
            "application/json"
        })
    void schedulePollTask(@Valid @RequestBody WorkflowExecutionId workflowExecutionId) {
        triggerScheduler.schedulePollTask(workflowExecutionId);
    }

    private record DynamicWebhookRefreshOneTimeTaskRequest(
        WorkflowTrigger workflowTrigger, WorkflowExecutionId workflowExecutionId, LocalDateTime webhookExpirationDate) {
    }
}
