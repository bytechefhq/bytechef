
            /**
             * The ByteChef Enterprise license (the "Enterprise License")
             * Copyright (c) 2023 - present ByteChef Inc.
             *
             * With regard to the ByteChef Software:
             *
             * This software and associated documentation files (the "Software") may only be
             * used in production, if you (and any entity that you represent) have agreed to,
             * and are in compliance with, the ByteChef Subscription Terms of Service, available
             * via email (support@bytechef.io) (the "Enterprise Terms"), or other
             * agreement governing the use of the Software, as agreed by you and ByteChef,
             * and otherwise have a valid ByteChef Enterprise license for the
             * correct number of user seats. Subject to the foregoing sentence, you are free to
             * modify this Software and publish patches to the Software. You agree that ByteChef
             * and/or its licensors (as applicable) retain all right, title and interest in and
             * to all such modifications and/or patches, and all such modifications and/or
             * patches may only be used, copied, modified, displayed, distributed, or otherwise
             * exploited with a valid ByteChef Enterprise license for the  correct
             * number of user seats.  Notwithstanding the foregoing, you may copy and modify
             * the Software for development and testing purposes, without requiring a
             * subscription.  You agree that ByteChef and/or its licensors (as applicable) retain
             * all right, title and interest in and to all such modifications.  You are not
             * granted any other rights beyond what is expressly stated herein.  Subject to the
             * foregoing, it is forbidden to copy, merge, publish, distribute, sublicense,
             * and/or sell the Software.
             *
             * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
             * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
             * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
             * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
             * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
             * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
             * SOFTWARE.
             *
             * For all third party components incorporated into the ByteChef Software, those
             * components are licensed under the original license provided by the owner of the
             * applicable component.
             */
            
package com.bytechef.hermes.scheduler.remote.web.rest;

import com.bytechef.hermes.scheduler.TriggerScheduler;
import com.bytechef.hermes.execution.WorkflowExecutionId;
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
@RequestMapping("/remote/trigger-scheduler")
public class RemoteTriggerSchedulerController {

    private final TriggerScheduler triggerScheduler;

    public RemoteTriggerSchedulerController(TriggerScheduler triggerScheduler) {
        this.triggerScheduler = triggerScheduler;
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/cancel-dynamic-webhook-trigger-refresh",
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
            WorkflowExecutionId.parse(triggerWorkflowTaskRequest.workflowExecutionId));
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
