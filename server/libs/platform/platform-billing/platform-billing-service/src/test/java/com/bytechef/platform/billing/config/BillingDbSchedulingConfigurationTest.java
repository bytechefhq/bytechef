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

package com.bytechef.platform.billing.config;

import com.bytechef.platform.billing.service.BillingUsageService;
import com.bytechef.platform.scheduler.db.task.DbSchedulerTaskDescriptors;
import com.github.kagkarlsson.scheduler.CurrentlyExecuting;
import com.github.kagkarlsson.scheduler.SchedulerClient;
import com.github.kagkarlsson.scheduler.SchedulerState;
import com.github.kagkarlsson.scheduler.task.Execution;
import com.github.kagkarlsson.scheduler.task.ExecutionContext;
import com.github.kagkarlsson.scheduler.task.Task;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import java.time.Instant;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * @author Ivica Cardic
 */
class BillingDbSchedulingConfigurationTest {

    @Test
    void testStripeUsageReportTaskReportsScheduledExecutionTime() {
        BillingUsageService billingUsageService = Mockito.mock(BillingUsageService.class);
        Task<Void> task = new BillingDbSchedulingConfiguration().stripeUsageReportTask(billingUsageService);
        Instant executionTime = Instant.parse("2031-01-01T10:00:00Z");
        TaskInstance<Void> taskInstance = DbSchedulerTaskDescriptors.STRIPE_USAGE_REPORT.instance("recurring")
            .build();
        ExecutionContext executionContext = new ExecutionContext(
            Mockito.mock(SchedulerState.class), new Execution(executionTime, taskInstance),
            Mockito.mock(SchedulerClient.class), Mockito.mock(CurrentlyExecuting.class));

        task.execute(taskInstance, executionContext);

        Mockito.verify(billingUsageService)
            .reportUsage(executionTime);
        Assertions.assertThat(task.getName())
            .isEqualTo(DbSchedulerTaskDescriptors.STRIPE_USAGE_REPORT_NAME);
    }
}
