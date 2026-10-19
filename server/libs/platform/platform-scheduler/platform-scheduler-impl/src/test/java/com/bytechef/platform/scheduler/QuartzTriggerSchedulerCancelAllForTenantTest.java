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

import static com.bytechef.platform.scheduler.constant.QuartzConnectionRefreshConstant.CONNECTION_OAUTH_2_TOKEN_REFRESH;
import static com.bytechef.platform.scheduler.constant.QuartzTriggerSchedulerConstants.DYNAMIC_WEBHOOK_TRIGGER_REFRESH;
import static com.bytechef.platform.scheduler.constant.QuartzTriggerSchedulerConstants.POLLING_TRIGGER;
import static com.bytechef.platform.scheduler.constant.QuartzTriggerSchedulerConstants.SCHEDULE_TRIGGER;
import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.scheduler.job.PollingTriggerJob;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.quartz.JobBuilder;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.matchers.GroupMatcher;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class QuartzTriggerSchedulerCancelAllForTenantTest {

    private QuartzConnectionRefreshScheduler quartzConnectionRefreshScheduler;
    private QuartzTriggerScheduler quartzTriggerScheduler;
    private Scheduler scheduler;

    @BeforeEach
    void beforeEach() throws SchedulerException {
        Properties properties = new Properties();

        properties.setProperty("org.quartz.jobStore.class", "org.quartz.simpl.RAMJobStore");
        properties.setProperty("org.quartz.scheduler.instanceName", "cancelAllForTenant-" + UUID.randomUUID());
        properties.setProperty("org.quartz.threadPool.threadCount", "1");

        scheduler = new StdSchedulerFactory(properties).getScheduler();

        quartzConnectionRefreshScheduler = new QuartzConnectionRefreshScheduler(scheduler);
        quartzTriggerScheduler = new QuartzTriggerScheduler(
            new ApplicationProperties.Coordinator.Trigger.Polling(), scheduler);
    }

    @AfterEach
    void afterEach() throws SchedulerException {
        scheduler.shutdown();
    }

    @Test
    void testCancelAllForTenantLeavesOtherTenants() throws SchedulerException {
        for (String tenantId : new String[] {
            "000001", "000002"
        }) {
            quartzTriggerScheduler.scheduleScheduleTrigger(
                "0 0 12 * * ?", "UTC", Map.of(), createWorkflowExecutionId(tenantId, "scheduleTrigger"));
            quartzTriggerScheduler.schedulePollingTrigger(createWorkflowExecutionId(tenantId, "pollingTrigger"));
            quartzTriggerScheduler.scheduleDynamicWebhookTriggerRefresh(
                Instant.now()
                    .plus(Duration.ofHours(1)),
                "component", 1, createWorkflowExecutionId(tenantId, "webhookTrigger"), 1L);
            quartzConnectionRefreshScheduler.scheduleConnectionRefresh(
                1L, Instant.now()
                    .plus(Duration.ofHours(1)),
                tenantId);
        }

        assertThat(scheduler.getJobKeys(GroupMatcher.anyJobGroup())).hasSize(8);

        quartzTriggerScheduler.cancelAllForTenant("000001");
        quartzConnectionRefreshScheduler.cancelAllForTenant("000001");

        Set<JobKey> jobKeys = scheduler.getJobKeys(GroupMatcher.anyJobGroup());

        assertThat(jobKeys).containsExactlyInAnyOrder(
            JobKey.jobKey(createWorkflowExecutionId("000002", "scheduleTrigger").toString(), SCHEDULE_TRIGGER),
            JobKey.jobKey(createWorkflowExecutionId("000002", "pollingTrigger").toString(), POLLING_TRIGGER),
            JobKey.jobKey(
                createWorkflowExecutionId("000002", "webhookTrigger").toString(), DYNAMIC_WEBHOOK_TRIGGER_REFRESH),
            JobKey.jobKey("0000021", CONNECTION_OAUTH_2_TOKEN_REFRESH));
    }

    @Test
    void testCancelAllForTenantSkipsKeysThatAreNotWorkflowExecutionIds() throws SchedulerException {
        quartzTriggerScheduler.schedulePollingTrigger(createWorkflowExecutionId("000001", "pollingTrigger"));

        scheduler.addJob(
            JobBuilder.newJob(PollingTriggerJob.class)
                .withIdentity(JobKey.jobKey("not-a-workflow-execution-id", POLLING_TRIGGER))
                .storeDurably()
                .build(),
            false);

        quartzTriggerScheduler.cancelAllForTenant("000001");

        assertThat(scheduler.getJobKeys(GroupMatcher.anyJobGroup()))
            .containsExactly(JobKey.jobKey("not-a-workflow-execution-id", POLLING_TRIGGER));
    }

    private static WorkflowExecutionId createWorkflowExecutionId(String tenantId, String triggerName) {
        return WorkflowExecutionId.parse(
            EncodingUtils.base64EncodeToString(tenantId + ":0:1:workflowUuid:" + triggerName));
    }
}
