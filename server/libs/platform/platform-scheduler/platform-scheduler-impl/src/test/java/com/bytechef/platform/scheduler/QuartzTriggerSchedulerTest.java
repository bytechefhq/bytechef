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

package com.bytechef.platform.scheduler;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class QuartzTriggerSchedulerTest {

    private final Scheduler scheduler = mock(Scheduler.class);
    private final WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.of(
        PlatformType.AUTOMATION, 7L, "workflow-uuid", "trigger_1");

    private QuartzTriggerScheduler quartzTriggerScheduler;

    @BeforeEach
    void beforeEach() {
        quartzTriggerScheduler = new QuartzTriggerScheduler(
            new ApplicationProperties.Coordinator.Trigger.Polling(), scheduler);
    }

    @Test
    void testScheduleDynamicWebhookTriggerRefreshThrowsWhenTheJobCannotBeScheduled() throws SchedulerException {
        SchedulerException schedulerException = new SchedulerException("job store unavailable");

        when(scheduler.scheduleJob(any(JobDetail.class), any(Trigger.class))).thenThrow(schedulerException);

        assertThatThrownBy(() -> quartzTriggerScheduler.scheduleDynamicWebhookTriggerRefresh(
            Instant.parse("2026-12-01T00:00:00Z"), "webhook", 1, workflowExecutionId, 42L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(workflowExecutionId.toString())
                .hasCause(schedulerException);
    }

    @Test
    void testScheduleDynamicWebhookTriggerRefreshThrowsWhenTheExistingJobCannotBeReplaced()
        throws SchedulerException {

        SchedulerException schedulerException = new SchedulerException("job store unavailable");

        when(scheduler.checkExists(any(JobKey.class))).thenReturn(true);
        when(scheduler.deleteJob(any(JobKey.class))).thenThrow(schedulerException);

        assertThatThrownBy(() -> quartzTriggerScheduler.scheduleDynamicWebhookTriggerRefresh(
            Instant.parse("2026-12-01T00:00:00Z"), "webhook", 1, workflowExecutionId, 42L))
                .isInstanceOf(IllegalStateException.class)
                .hasCause(schedulerException);
    }

    @Test
    void testSchedulePollingTriggerLogsAFailureToScheduleTheJob() throws SchedulerException {
        when(scheduler.scheduleJob(any(JobDetail.class), any(Trigger.class)))
            .thenThrow(new SchedulerException("job store unavailable"));

        assertThatCode(() -> quartzTriggerScheduler.schedulePollingTrigger(workflowExecutionId))
            .doesNotThrowAnyException();
    }

    @Test
    void testScheduleScheduleTriggerLogsAFailureToScheduleTheJob() throws SchedulerException {
        when(scheduler.scheduleJob(any(JobDetail.class), any(Trigger.class)))
            .thenThrow(new SchedulerException("job store unavailable"));

        assertThatCode(() -> quartzTriggerScheduler.scheduleScheduleTrigger(
            "0 0 * * * ?", "UTC", Map.of(), workflowExecutionId))
                .doesNotThrowAnyException();
    }

    @Test
    void testScheduleOneTimeTaskLogsAFailureToScheduleTheJob() throws SchedulerException {
        when(scheduler.scheduleJob(any(JobDetail.class), any(Trigger.class)))
            .thenThrow(new SchedulerException("job store unavailable"));

        assertThatCode(() -> quartzTriggerScheduler.scheduleOneTimeTask(
            Instant.parse("2026-12-01T00:00:00Z"), Map.of(), 9L))
                .doesNotThrowAnyException();
    }
}
