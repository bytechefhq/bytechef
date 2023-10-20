
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

package com.bytechef.hermes.scheduler.remote.web.rest;

import com.bytechef.hermes.scheduler.TaskScheduler;
import com.bytechef.hermes.workflow.WorkflowExecutionId;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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

    private final TaskScheduler taskScheduler;

    public TriggerSchedulerController(TaskScheduler taskScheduler) {
        this.taskScheduler = taskScheduler;
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/trigger-scheduler/cancel-dynamic-webhook-refresh-task",
        consumes = {
            "application/json"
        })
    void cancelDynamicWebhookRefreshTask(@Valid @RequestBody String workflowExecutionId) {
        taskScheduler.cancelRefreshDynamicWebhookTriggerTask(workflowExecutionId);
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/trigger-scheduler/cancel-poll-task",
        consumes = {
            "application/json"
        })
    void cancelPollTask(@Valid @RequestBody String workflowExecutionId) {
        taskScheduler.cancelPollTriggerTask(workflowExecutionId);
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/trigger-scheduler/schedule-dynamic-webhook-refresh-task",
        consumes = {
            "application/json"
        })
    void scheduleDynamicWebhookRefreshTask(
        @Valid @RequestBody DynamicWebhookRefreshOneTimeTaskRequest dynamicWebhookRefreshOneTimeTaskRequest) {

        taskScheduler.scheduleRefreshDynamicWebhookTriggerTask(
            dynamicWebhookRefreshOneTimeTaskRequest.workflowExecutionId,
            dynamicWebhookRefreshOneTimeTaskRequest.webhookExpirationDate,
            dynamicWebhookRefreshOneTimeTaskRequest.componentName,
            dynamicWebhookRefreshOneTimeTaskRequest.componentVersion);
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/trigger-scheduler/schedule-poll-task",
        consumes = {
            "application/json"
        })
    void schedulePollTask(@Valid @RequestBody WorkflowExecutionId workflowExecutionId) {
        taskScheduler.schedulePollTriggerTask(workflowExecutionId);
    }

    @SuppressFBWarnings("EI")
    private record DynamicWebhookRefreshOneTimeTaskRequest(
        WorkflowExecutionId workflowExecutionId, LocalDateTime webhookExpirationDate, String componentName,
        int componentVersion) {
    }
}
