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
import com.bytechef.platform.scheduler.ConnectionRefreshScheduler;
import com.bytechef.platform.scheduler.TriggerScheduler;
import com.bytechef.platform.scheduler.db.config.DbSchedulerTestConfiguration;
import com.bytechef.platform.scheduler.db.task.DbSchedulerTaskDescriptors;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import com.github.kagkarlsson.scheduler.SchedulerClient;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import org.assertj.core.api.Assertions;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
@SpringBootTest(classes = DbSchedulerTestConfiguration.class, properties = "spring.profiles.active=test")
@Import(PostgreSQLContainerConfiguration.class)
class DbTriggerSchedulerIntTest {

    @Autowired
    private DbSchedulerTestConfiguration.CapturedEvents capturedEvents;

    @Autowired
    private ConnectionRefreshScheduler connectionRefreshScheduler;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SchedulerClient schedulerClient;

    @Autowired
    private TriggerScheduler triggerScheduler;

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("DELETE FROM scheduled_tasks");
        capturedEvents.listenerEvents.clear();
        capturedEvents.pollEvents.clear();
        capturedEvents.resumeEvents.clear();
    }

    @Test
    void testScheduleTriggerFiresTriggerListenerEventWithFireTimeAndDateTime() {
        WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.of(
            PlatformType.AUTOMATION, 1L, "schedule-workflow", "trigger_1");

        triggerScheduler.scheduleScheduleTrigger(
            "0/1 * * * * ?", "UTC", Map.of("expression", "* * * * *", "timezone", "UTC"), workflowExecutionId);

        Awaitility.await()
            .atMost(Duration.ofSeconds(15))
            .until(() -> !capturedEvents.listenerEvents.isEmpty());

        @SuppressWarnings("unchecked")
        Map<String, Object> output = (Map<String, Object>) capturedEvents.listenerEvents.getFirst()
            .getListenerParameters()
            .output();

        Assertions.assertThat(output)
            .containsKeys("fireTime", "dateTime", "expression", "timezone");
        Assertions.assertThat(output.get("dateTime"))
            .isInstanceOf(LocalDateTime.class);
        Assertions.assertThat(schedulerClient.getScheduledExecution(
            DbSchedulerTaskDescriptors.SCHEDULE_TRIGGER.instanceId(workflowExecutionId.toString())))
            .as("recurring instance still scheduled after a fire")
            .isPresent();
    }

    @Test
    void testPollingTriggerFiresTriggerPollEventImmediatelyAndRepeats() {
        WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.of(
            PlatformType.AUTOMATION, 2L, "polling-workflow", "trigger_1");

        triggerScheduler.schedulePollingTrigger(workflowExecutionId);

        Awaitility.await()
            .atMost(Duration.ofSeconds(10))
            .until(() -> !capturedEvents.pollEvents.isEmpty());

        Assertions.assertThat(capturedEvents.pollEvents.getFirst()
            .getWorkflowExecutionId()
            .toString())
            .isEqualTo(workflowExecutionId.toString());
        Assertions.assertThat(schedulerClient.getScheduledExecution(
            DbSchedulerTaskDescriptors.POLLING_TRIGGER.instanceId(workflowExecutionId.toString())))
            .isPresent();
    }

    @Test
    void testOneTimeTaskFiresResumeJobEventOnceAndDisappears() {
        triggerScheduler.scheduleOneTimeTask(Instant.now()
            .plusSeconds(1), Map.of("k", "v"), 42L);

        Awaitility.await()
            .atMost(Duration.ofSeconds(10))
            .until(() -> !capturedEvents.resumeEvents.isEmpty());

        Assertions.assertThat(capturedEvents.resumeEvents.getFirst()
            .getJobId())
            .isEqualTo(42L);
        Awaitility.await()
            .atMost(Duration.ofSeconds(5))
            .until(() -> schedulerClient.getScheduledExecution(
                DbSchedulerTaskDescriptors.ONE_TIME_RESUME.instanceId("42"))
                .isEmpty());
    }

    @Test
    void testCancelRemovesScheduledInstance() {
        WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.of(
            PlatformType.AUTOMATION, 3L, "cancel-workflow", "trigger_1");

        triggerScheduler.scheduleScheduleTrigger("0 0 9 * * ?", "UTC", Map.of(), workflowExecutionId);
        triggerScheduler.cancelScheduleTrigger(workflowExecutionId.toString());

        Assertions.assertThat(schedulerClient.getScheduledExecution(
            DbSchedulerTaskDescriptors.SCHEDULE_TRIGGER.instanceId(workflowExecutionId.toString())))
            .isEmpty();
    }

    @Test
    void testConnectionRefreshIsScheduledFiveMinutesBeforeExpiry() {
        Instant expiry = Instant.now()
            .plus(Duration.ofHours(1));

        connectionRefreshScheduler.scheduleConnectionRefresh(77L, expiry, "public");

        Assertions.assertThat(schedulerClient.getScheduledExecution(
            DbSchedulerTaskDescriptors.OAUTH2_TOKEN_REFRESH.instanceId("public77")))
            .isPresent()
            .get()
            .satisfies(execution -> Assertions.assertThat(execution.getExecutionTime())
                .isCloseTo(expiry.minus(Duration.ofMinutes(5)), Assertions.within(Duration.ofSeconds(1))));
    }
}
