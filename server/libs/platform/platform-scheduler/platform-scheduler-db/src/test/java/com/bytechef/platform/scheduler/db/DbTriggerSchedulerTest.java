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

import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.scheduler.db.task.DbSchedulerTaskDescriptors;
import com.bytechef.platform.scheduler.db.task.DynamicWebhookRefreshData;
import com.bytechef.platform.scheduler.db.task.OneTimeResumeData;
import com.bytechef.platform.scheduler.db.task.PollingTriggerData;
import com.bytechef.platform.scheduler.db.task.ScheduleTriggerData;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.tenant.TenantContext;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import com.github.kagkarlsson.scheduler.ScheduledExecution;
import com.github.kagkarlsson.scheduler.SchedulerClient;
import com.github.kagkarlsson.scheduler.exceptions.TaskInstanceCurrentlyExecutingException;
import com.github.kagkarlsson.scheduler.exceptions.TaskInstanceException;
import com.github.kagkarlsson.scheduler.exceptions.TaskInstanceNotFoundException;
import com.github.kagkarlsson.scheduler.task.SchedulableInstance;
import com.github.kagkarlsson.scheduler.task.TaskInstanceId;
import java.time.Instant;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

@ExtendWith(ObjectMapperSetupExtension.class)
class DbTriggerSchedulerTest {

    private final Set<String> existingInstanceKeys = new HashSet<>();
    private final SchedulerClient schedulerClient = Mockito.mock(SchedulerClient.class);
    private final DbTriggerScheduler triggerScheduler = new DbTriggerScheduler(schedulerClient, 5);
    private final WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.of(
        PlatformType.AUTOMATION, 1L, "workflow-uuid", "trigger_1");

    @BeforeEach
    void setUp() {
        Mockito.when(schedulerClient.reschedule(Mockito.any(SchedulableInstance.class)))
            .thenAnswer(invocation -> rescheduleExistingInstanceOrThrow(invocation.getArgument(0)));
        Mockito.when(schedulerClient.scheduleIfNotExists(Mockito.any(SchedulableInstance.class)))
            .thenAnswer(invocation -> existingInstanceKeys.add(instanceKey(invocation.getArgument(0))));
        Mockito.when(schedulerClient.schedule(
            Mockito.any(SchedulableInstance.class), Mockito.eq(SchedulerClient.ScheduleOptions.WHEN_EXISTS_RESCHEDULE)))
            .thenAnswer(invocation -> scheduleWithWhenExistsReschedule(invocation.getArgument(0)));
    }

    @Test
    void testCancelOneTimeTaskCancelsTheScheduledResumeOfTheJob() {
        TaskInstanceId taskInstanceId = DbSchedulerTaskDescriptors.ONE_TIME_RESUME.instanceId("public:42");

        Mockito.when(schedulerClient.getScheduledExecution(taskInstanceId))
            .thenReturn(Optional.of(Mockito.mock(ScheduledExecution.class)));

        triggerScheduler.cancelOneTimeTask(42L);

        Mockito.verify(schedulerClient)
            .cancel(taskInstanceId);
        Mockito.verify(schedulerClient, Mockito.never())
            .getScheduledExecution(DbSchedulerTaskDescriptors.ONE_TIME_RESUME.instanceId("42"));
    }

    @Test
    void testCancelOneTimeTaskSkipsAJobWithoutAScheduledResume() {
        Mockito.when(schedulerClient.getScheduledExecution(Mockito.any()))
            .thenReturn(Optional.empty());

        triggerScheduler.cancelOneTimeTask(42L);

        Mockito.verify(schedulerClient, Mockito.never())
            .cancel(Mockito.any());
    }

    @Test
    void testCancelOneTimeTaskCancelsAResumeScheduledUnderTheLegacyInstanceId() {
        TaskInstanceId legacyTaskInstanceId = DbSchedulerTaskDescriptors.ONE_TIME_RESUME.instanceId("42");

        ScheduledExecution<Object> legacyScheduledExecution = scheduledExecution(
            new OneTimeResumeData(42L, null, "tenant_1"));

        Mockito.when(schedulerClient.getScheduledExecution(Mockito.any()))
            .thenReturn(Optional.empty());
        Mockito.when(schedulerClient.getScheduledExecution(legacyTaskInstanceId))
            .thenReturn(Optional.of(legacyScheduledExecution));

        TenantContext.runWithTenantId("tenant_1", () -> triggerScheduler.cancelOneTimeTask(42L));

        Mockito.verify(schedulerClient)
            .cancel(legacyTaskInstanceId);
    }

    @Test
    void testCancelOneTimeTaskKeepsALegacyResumeOfAnotherTenant() {
        TaskInstanceId legacyTaskInstanceId = DbSchedulerTaskDescriptors.ONE_TIME_RESUME.instanceId("42");

        ScheduledExecution<Object> legacyScheduledExecution = scheduledExecution(
            new OneTimeResumeData(42L, null, "tenant_2"));

        Mockito.when(schedulerClient.getScheduledExecution(Mockito.any()))
            .thenReturn(Optional.empty());
        Mockito.when(schedulerClient.getScheduledExecution(legacyTaskInstanceId))
            .thenReturn(Optional.of(legacyScheduledExecution));

        TenantContext.runWithTenantId("tenant_1", () -> triggerScheduler.cancelOneTimeTask(42L));

        Mockito.verify(schedulerClient, Mockito.never())
            .cancel(Mockito.any());
    }

    @Test
    void testCancelOneTimeTaskKeepsTheLegacyResumeWhenTheTenantScopedResumeExists() {
        TaskInstanceId taskInstanceId = DbSchedulerTaskDescriptors.ONE_TIME_RESUME.instanceId("tenant_1:42");

        Mockito.when(schedulerClient.getScheduledExecution(taskInstanceId))
            .thenReturn(Optional.of(Mockito.mock(ScheduledExecution.class)));

        TenantContext.runWithTenantId("tenant_1", () -> triggerScheduler.cancelOneTimeTask(42L));

        Mockito.verify(schedulerClient)
            .cancel(taskInstanceId);
        Mockito.verify(schedulerClient, Mockito.never())
            .cancel(DbSchedulerTaskDescriptors.ONE_TIME_RESUME.instanceId("42"));
    }

    @Test
    void testScheduleOneTimeTaskKeepsTheResumesOfTwoTenantsWithTheSameJobIdApart() {
        Instant executeAt = Instant.parse("2031-01-01T00:00:00Z");

        TenantContext.runWithTenantId(
            "tenant_1", () -> triggerScheduler.scheduleOneTimeTask(executeAt, Map.of(), 42L));
        TenantContext.runWithTenantId(
            "tenant_2", () -> triggerScheduler.scheduleOneTimeTask(executeAt, Map.of(), 42L));

        Assertions.assertThat(existingInstanceKeys)
            .contains(
                DbSchedulerTaskDescriptors.ONE_TIME_RESUME.getTaskName() + "|tenant_1:42",
                DbSchedulerTaskDescriptors.ONE_TIME_RESUME.getTaskName() + "|tenant_2:42");
    }

    @Test
    void testScheduleScheduleTriggerStoresCronZoneAndOutputAccordingToData() {
        triggerScheduler.scheduleScheduleTrigger(
            "0 0 9 * * ?", "Europe/Zagreb", Map.of("expression", "0 9 * * *"), workflowExecutionId);

        SchedulableInstance<?> instance = capturedSchedule();

        Assertions.assertThat(instance.getTaskName())
            .isEqualTo(DbSchedulerTaskDescriptors.SCHEDULE_TRIGGER_NAME);
        Assertions.assertThat(instance.getId())
            .isEqualTo(workflowExecutionId.toString());
        Assertions.assertThat((ScheduleTriggerData) instance.getTaskInstance()
            .getData())
            .isEqualTo(new ScheduleTriggerData("0 0 9 * * ?", "Europe/Zagreb", "{\"expression\":\"0 9 * * *\"}"));
    }

    @Test
    void testSchedulePollingTriggerIsDueNowWithConfiguredPeriod() {
        Instant before = Instant.now();

        triggerScheduler.schedulePollingTrigger(workflowExecutionId);

        SchedulableInstance<?> instance = capturedSchedule();

        Assertions.assertThat(instance.getTaskName())
            .isEqualTo(DbSchedulerTaskDescriptors.POLLING_TRIGGER_NAME);
        Assertions.assertThat((PollingTriggerData) instance.getTaskInstance()
            .getData())
            .isEqualTo(new PollingTriggerData(5));
        Assertions.assertThat(instance.getNextExecutionTime(before))
            .isBetween(before, Instant.now());
    }

    @Test
    void testScheduleDynamicWebhookTriggerRefreshUsesExpiryAndConnectionId() {
        Instant expiry = Instant.parse("2031-01-01T00:00:00Z");

        triggerScheduler.scheduleDynamicWebhookTriggerRefresh(expiry, "component", 1, workflowExecutionId, 77L);

        SchedulableInstance<?> instance = capturedSchedule();

        Assertions.assertThat(instance.getTaskName())
            .isEqualTo(DbSchedulerTaskDescriptors.DYNAMIC_WEBHOOK_REFRESH_NAME);
        Assertions.assertThat((DynamicWebhookRefreshData) instance.getTaskInstance()
            .getData())
            .isEqualTo(new DynamicWebhookRefreshData(77L));
        Assertions.assertThat(instance.getNextExecutionTime(Instant.now()))
            .isEqualTo(expiry);
    }

    @Test
    void testScheduleOneTimeTaskStoresContinueParametersOnlyWhenPresent() {
        Instant executeAt = Instant.parse("2031-01-01T00:00:00Z");

        triggerScheduler.scheduleOneTimeTask(executeAt, Map.of("k", "v"), 42L);
        triggerScheduler.scheduleOneTimeTask(executeAt, Map.of(), 43L);

        ArgumentCaptor<SchedulableInstance<?>> captor = ArgumentCaptor.forClass(SchedulableInstance.class);

        Mockito.verify(schedulerClient, Mockito.times(2))
            .schedule(captor.capture(), Mockito.eq(SchedulerClient.ScheduleOptions.WHEN_EXISTS_RESCHEDULE));

        Assertions.assertThat((OneTimeResumeData) captor.getAllValues()
            .get(0)
            .getTaskInstance()
            .getData())
            .isEqualTo(new OneTimeResumeData(42L, "{\"k\":\"v\"}", TenantContext.DEFAULT_TENANT_ID));
        Assertions.assertThat((OneTimeResumeData) captor.getAllValues()
            .get(1)
            .getTaskInstance()
            .getData())
            .isEqualTo(new OneTimeResumeData(43L, null, TenantContext.DEFAULT_TENANT_ID));
    }

    @Test
    void testScheduleOneTimeTaskCapturesCurrentTenantId() {
        Instant executeAt = Instant.parse("2031-01-01T00:00:00Z");

        TenantContext.runWithTenantId(
            "tenant_1", () -> triggerScheduler.scheduleOneTimeTask(executeAt, Map.of(), 44L));

        SchedulableInstance<?> instance = capturedSchedule();

        Assertions.assertThat((OneTimeResumeData) instance.getTaskInstance()
            .getData())
            .isEqualTo(new OneTimeResumeData(44L, null, "tenant_1"));
        Assertions.assertThat(instance.getId())
            .isEqualTo("tenant_1:44");
    }

    @Test
    void testScheduleOfExistingInstanceReschedulesInPlaceViaSingleUpsertCall() {
        triggerScheduler.schedulePollingTrigger(workflowExecutionId);
        triggerScheduler.schedulePollingTrigger(workflowExecutionId);

        Mockito.verify(schedulerClient, Mockito.times(2))
            .schedule(
                Mockito.any(SchedulableInstance.class),
                Mockito.eq(SchedulerClient.ScheduleOptions.WHEN_EXISTS_RESCHEDULE));
        Mockito.verify(schedulerClient, Mockito.never())
            .scheduleIfNotExists(Mockito.any(SchedulableInstance.class));
    }

    @Test
    void testScheduleDoesNotThrowWhenInstanceIsCurrentlyExecuting() {
        Mockito.when(schedulerClient.schedule(
            Mockito.any(SchedulableInstance.class), Mockito.eq(SchedulerClient.ScheduleOptions.WHEN_EXISTS_RESCHEDULE)))
            .thenThrow(new TaskInstanceCurrentlyExecutingException(
                DbSchedulerTaskDescriptors.POLLING_TRIGGER_NAME, workflowExecutionId.toString()));

        Assertions.assertThatCode(() -> triggerScheduler.schedulePollingTrigger(workflowExecutionId))
            .doesNotThrowAnyException();
    }

    @Test
    void testCancelPollingTriggerCancelsByTaskAndInstanceId() {
        triggerScheduler.cancelPollingTrigger(workflowExecutionId.toString());

        Mockito.verify(schedulerClient)
            .cancel(DbSchedulerTaskDescriptors.POLLING_TRIGGER.instanceId(workflowExecutionId.toString()));
    }

    @Test
    void testCancelDynamicWebhookTriggerRefreshTargetsItsOwnTask() {
        triggerScheduler.cancelDynamicWebhookTriggerRefresh(workflowExecutionId.toString());

        Mockito.verify(schedulerClient)
            .cancel(DbSchedulerTaskDescriptors.DYNAMIC_WEBHOOK_REFRESH.instanceId(workflowExecutionId.toString()));
    }

    @Test
    void testCancelOfMissingInstanceDoesNotThrow() {
        Mockito.doThrow(new TaskInstanceException("missing", "polling-trigger", "id"))
            .when(schedulerClient)
            .cancel(Mockito.any());

        Assertions.assertThatCode(() -> triggerScheduler.cancelScheduleTrigger(workflowExecutionId.toString()))
            .doesNotThrowAnyException();
    }

    private SchedulableInstance<?> capturedSchedule() {
        ArgumentCaptor<SchedulableInstance<?>> captor = ArgumentCaptor.forClass(SchedulableInstance.class);

        Mockito.verify(schedulerClient)
            .schedule(captor.capture(), Mockito.eq(SchedulerClient.ScheduleOptions.WHEN_EXISTS_RESCHEDULE));

        return captor.getValue();
    }

    private boolean rescheduleExistingInstanceOrThrow(SchedulableInstance<?> instance) {
        if (!existingInstanceKeys.contains(instanceKey(instance))) {
            throw new TaskInstanceNotFoundException(instance.getTaskName(), instance.getId());
        }

        return true;
    }

    private boolean scheduleWithWhenExistsReschedule(SchedulableInstance<?> instance) {
        if (existingInstanceKeys.add(instanceKey(instance))) {
            return true;
        }

        return rescheduleExistingInstanceOrThrow(instance);
    }

    @SuppressWarnings("unchecked")
    private static ScheduledExecution<Object> scheduledExecution(OneTimeResumeData oneTimeResumeData) {
        ScheduledExecution<Object> scheduledExecution = Mockito.mock(ScheduledExecution.class);

        Mockito.when(scheduledExecution.getData())
            .thenReturn(oneTimeResumeData);

        return scheduledExecution;
    }

    private static String instanceKey(SchedulableInstance<?> instance) {
        return instance.getTaskName() + "|" + instance.getId();
    }
}
