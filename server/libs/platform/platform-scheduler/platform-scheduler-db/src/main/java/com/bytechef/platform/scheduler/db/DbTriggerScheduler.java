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

package com.bytechef.platform.scheduler.db;

import static com.bytechef.platform.scheduler.db.task.DbSchedulerTaskDescriptors.DYNAMIC_WEBHOOK_REFRESH;
import static com.bytechef.platform.scheduler.db.task.DbSchedulerTaskDescriptors.ONE_TIME_RESUME;
import static com.bytechef.platform.scheduler.db.task.DbSchedulerTaskDescriptors.POLLING_TRIGGER;
import static com.bytechef.platform.scheduler.db.task.DbSchedulerTaskDescriptors.SCHEDULE_TRIGGER;

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.platform.scheduler.TriggerScheduler;
import com.bytechef.platform.scheduler.db.task.DynamicWebhookRefreshData;
import com.bytechef.platform.scheduler.db.task.OneTimeResumeData;
import com.bytechef.platform.scheduler.db.task.PollingTriggerData;
import com.bytechef.platform.scheduler.db.task.ScheduleTriggerData;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.tenant.TenantContext;
import com.github.kagkarlsson.scheduler.ScheduledExecution;
import com.github.kagkarlsson.scheduler.SchedulerClient;
import com.github.kagkarlsson.scheduler.exceptions.TaskInstanceException;
import com.github.kagkarlsson.scheduler.task.SchedulableInstance;
import com.github.kagkarlsson.scheduler.task.TaskDescriptor;
import com.github.kagkarlsson.scheduler.task.TaskInstanceId;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Ivica Cardic
 */
public class DbTriggerScheduler implements TriggerScheduler {

    private static final Logger log = LoggerFactory.getLogger(DbTriggerScheduler.class);

    private final int pollingTriggerCheckPeriod;
    private final SchedulerClient schedulerClient;

    @SuppressFBWarnings("EI")
    public DbTriggerScheduler(SchedulerClient schedulerClient, int pollingTriggerCheckPeriod) {
        this.schedulerClient = schedulerClient;
        this.pollingTriggerCheckPeriod = pollingTriggerCheckPeriod;
    }

    @Override
    public void cancelDynamicWebhookTriggerRefresh(String workflowExecutionId) {
        cancel(DYNAMIC_WEBHOOK_REFRESH, workflowExecutionId);
    }

    @Override
    public void cancelOneTimeTask(long jobId) {
        String tenantId = TenantContext.getCurrentTenantId();

        String oneTimeTaskInstanceId = getOneTimeTaskInstanceId(tenantId, jobId);

        if (schedulerClient.getScheduledExecution(ONE_TIME_RESUME.instanceId(oneTimeTaskInstanceId))
            .isPresent()) {

            cancel(ONE_TIME_RESUME, oneTimeTaskInstanceId);

            return;
        }

        String legacyInstanceId = String.valueOf(jobId);

        Optional<ScheduledExecution<Object>> legacyScheduledExecution = schedulerClient.getScheduledExecution(
            ONE_TIME_RESUME.instanceId(legacyInstanceId));

        if (legacyScheduledExecution.isPresent() && isOfTenant(legacyScheduledExecution.get(), tenantId)) {
            cancel(ONE_TIME_RESUME, legacyInstanceId);
        }
    }

    @Override
    public void cancelScheduleTrigger(String workflowExecutionId) {
        cancel(SCHEDULE_TRIGGER, workflowExecutionId);
    }

    @Override
    public void cancelPollingTrigger(String workflowExecutionId) {
        cancel(POLLING_TRIGGER, workflowExecutionId);
    }

    @Override
    public void scheduleDynamicWebhookTriggerRefresh(
        Instant webhookExpirationDate, String componentName, int componentVersion,
        WorkflowExecutionId workflowExecutionId, Long connectionId) {

        replace(
            DYNAMIC_WEBHOOK_REFRESH.instance(workflowExecutionId.toString())
                .data(new DynamicWebhookRefreshData(connectionId))
                .scheduledTo(webhookExpirationDate));
    }

    @Override
    public void scheduleScheduleTrigger(
        String pattern, String zoneId, Map<String, Object> output, WorkflowExecutionId workflowExecutionId) {

        replace(
            SCHEDULE_TRIGGER.instance(workflowExecutionId.toString())
                .data(new ScheduleTriggerData(pattern, zoneId, JsonUtils.write(output)))
                .scheduledAccordingToData());
    }

    @Override
    public void schedulePollingTrigger(WorkflowExecutionId workflowExecutionId) {
        replace(
            POLLING_TRIGGER.instance(workflowExecutionId.toString())
                .data(new PollingTriggerData(pollingTriggerCheckPeriod))
                .scheduledTo(Instant.now()));
    }

    @Override
    public void scheduleOneTimeTask(Instant executeAt, Map<String, ?> output, long jobId) {
        String continueParameters = output == null || output.isEmpty() ? null : JsonUtils.write(output);
        String tenantId = TenantContext.getCurrentTenantId();

        replace(
            ONE_TIME_RESUME.instance(getOneTimeTaskInstanceId(tenantId, jobId))
                .data(new OneTimeResumeData(jobId, continueParameters, tenantId))
                .scheduledTo(executeAt));
    }

    static String getOneTimeTaskInstanceId(String tenantId, long jobId) {
        return tenantId + ":" + jobId;
    }

    private static boolean isOfTenant(ScheduledExecution<Object> scheduledExecution, String tenantId) {
        Object data;

        try {
            data = scheduledExecution.getData();
        } catch (RuntimeException exception) {
            log.warn("Unable to read the data of task instance {}", scheduledExecution.getTaskInstance(), exception);

            return true;
        }

        if (data instanceof OneTimeResumeData oneTimeResumeData && oneTimeResumeData.tenantId() != null) {
            return Objects.equals(oneTimeResumeData.tenantId(), tenantId);
        }

        return true;
    }

    private void cancel(TaskDescriptor<?> taskDescriptor, String instanceId) {
        TaskInstanceId taskInstanceId = taskDescriptor.instanceId(instanceId);

        try {
            schedulerClient.cancel(taskInstanceId);

            log.trace("Cancelled task {} instance {}", taskDescriptor.getTaskName(), instanceId);
        } catch (TaskInstanceException e) {
            log.warn(
                "Task {} instance {} not found for cancellation", taskDescriptor.getTaskName(), instanceId, e);
        }
    }

    private <T> void replace(SchedulableInstance<T> schedulableInstance) {
        try {
            if (!schedulerClient.schedule(
                schedulableInstance, SchedulerClient.ScheduleOptions.WHEN_EXISTS_RESCHEDULE)) {

                logNeitherRescheduledNorCreated(schedulableInstance, null);
            }
        } catch (TaskInstanceException e) {
            logNeitherRescheduledNorCreated(schedulableInstance, e);
        }
    }

    private <T> void
        logNeitherRescheduledNorCreated(SchedulableInstance<T> schedulableInstance, TaskInstanceException e) {
        log.warn(
            "Task {} instance {} was neither rescheduled nor created; it is probably executing right now",
            schedulableInstance.getTaskName(), schedulableInstance.getId(), e);
    }
}
