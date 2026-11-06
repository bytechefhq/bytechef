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

package com.bytechef.platform.scheduler.db.importer;

import com.bytechef.platform.scheduler.db.task.DbSchedulerTaskDescriptors;
import com.bytechef.platform.scheduler.db.task.DynamicWebhookRefreshData;
import com.bytechef.platform.scheduler.db.task.OAuth2TokenRefreshData;
import com.bytechef.platform.scheduler.db.task.OneTimeResumeData;
import com.bytechef.platform.scheduler.db.task.PollingTriggerData;
import com.bytechef.platform.scheduler.db.task.ScheduleTriggerData;
import com.github.kagkarlsson.scheduler.SchedulerClient;
import com.github.kagkarlsson.scheduler.task.SchedulableInstance;
import java.time.Instant;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/**
 * @author Ivica Cardic
 */
class QuartzImporterTest {

    private final QuartzJobReader reader = Mockito.mock(QuartzJobReader.class);
    private final SchedulerClient schedulerClient = Mockito.mock(SchedulerClient.class);
    private final QuartzImporter importer = new QuartzImporter(reader, schedulerClient, 5);
    private final Instant future = Instant.parse("2031-01-01T00:00:00Z");

    @Test
    void testImportsEachKindPreservingNextFireTime() {
        Mockito.when(reader.read())
            .thenReturn(new QuartzJobReader.ReadResult(
                List.of(
                    new ImportedJob.ScheduleTrigger("wfe-1", "0 0 9 * * ?", "UTC", "{}", future),
                    new ImportedJob.PollingTrigger("wfe-2", future),
                    new ImportedJob.DynamicWebhookRefresh("wfe-3", 99L, future),
                    new ImportedJob.OAuth2TokenRefresh("00000177", 77L, "000001", future),
                    new ImportedJob.OneTimeResume("42", 42L, "{\"step\":\"1\"}", future)),
                0, 0, 0, 0, true));
        Mockito.when(schedulerClient.scheduleIfNotExists(Mockito.any(SchedulableInstance.class)))
            .thenReturn(true);

        ImportSummary summary = importer.importJobs();

        ArgumentCaptor<SchedulableInstance<?>> captor = ArgumentCaptor.forClass(SchedulableInstance.class);

        Mockito.verify(schedulerClient, Mockito.times(5))
            .scheduleIfNotExists(captor.capture());

        List<SchedulableInstance<?>> instances = captor.getAllValues();

        Assertions.assertThat(instances)
            .allSatisfy(instance -> Assertions.assertThat(instance.getNextExecutionTime(Instant.now()))
                .isEqualTo(future));
        Assertions.assertThat(instances.get(0)
            .getTaskName())
            .isEqualTo(DbSchedulerTaskDescriptors.SCHEDULE_TRIGGER_NAME);
        Assertions.assertThat(instances.get(0)
            .getId())
            .isEqualTo("wfe-1");
        Assertions.assertThat((ScheduleTriggerData) instances.get(0)
            .getTaskInstance()
            .getData())
            .isEqualTo(new ScheduleTriggerData("0 0 9 * * ?", "UTC", "{}"));
        Assertions.assertThat(instances.get(1)
            .getTaskName())
            .isEqualTo(DbSchedulerTaskDescriptors.POLLING_TRIGGER_NAME);
        Assertions.assertThat(instances.get(1)
            .getId())
            .isEqualTo("wfe-2");
        Assertions.assertThat((PollingTriggerData) instances.get(1)
            .getTaskInstance()
            .getData())
            .isEqualTo(new PollingTriggerData(5));
        Assertions.assertThat(instances.get(2)
            .getTaskName())
            .isEqualTo(DbSchedulerTaskDescriptors.DYNAMIC_WEBHOOK_REFRESH_NAME);
        Assertions.assertThat(instances.get(2)
            .getId())
            .isEqualTo("wfe-3");
        Assertions.assertThat((DynamicWebhookRefreshData) instances.get(2)
            .getTaskInstance()
            .getData())
            .isEqualTo(new DynamicWebhookRefreshData(99L));
        Assertions.assertThat(instances.get(3)
            .getTaskName())
            .isEqualTo(DbSchedulerTaskDescriptors.OAUTH2_TOKEN_REFRESH_NAME);
        Assertions.assertThat(instances.get(3)
            .getId())
            .isEqualTo("00000177");
        Assertions.assertThat((OAuth2TokenRefreshData) instances.get(3)
            .getTaskInstance()
            .getData())
            .isEqualTo(new OAuth2TokenRefreshData(77L, "000001"));
        Assertions.assertThat(instances.get(4)
            .getTaskName())
            .isEqualTo(DbSchedulerTaskDescriptors.ONE_TIME_RESUME_NAME);
        Assertions.assertThat(instances.get(4)
            .getId())
            .isEqualTo("42");
        Assertions.assertThat((OneTimeResumeData) instances.get(4)
            .getTaskInstance()
            .getData())
            .isEqualTo(new OneTimeResumeData(42L, "{\"step\":\"1\"}", null));
        Assertions.assertThat(summary.imported())
            .isEqualTo(5);
        Assertions.assertThat(summary.scanned())
            .isEqualTo(5);
    }

    @Test
    void testPastNextFireTimeBecomesDueNow() {
        Instant past = Instant.parse("2020-01-01T00:00:00Z");

        Mockito.when(reader.read())
            .thenReturn(new QuartzJobReader.ReadResult(
                List.of(new ImportedJob.OneTimeResume("42", 42L, null, past)), 0, 0, 0, 0, true));
        Mockito.when(schedulerClient.scheduleIfNotExists(Mockito.any(SchedulableInstance.class)))
            .thenReturn(true);
        Instant before = Instant.now();

        importer.importJobs();

        ArgumentCaptor<SchedulableInstance<?>> captor = ArgumentCaptor.forClass(SchedulableInstance.class);

        Mockito.verify(schedulerClient)
            .scheduleIfNotExists(captor.capture());
        Assertions.assertThat(captor.getValue()
            .getNextExecutionTime(before))
            .isBetween(before, Instant.now());
    }

    @Test
    void testAlreadyPresentInstancesAreCountedNotDuplicated() {
        Mockito.when(reader.read())
            .thenReturn(new QuartzJobReader.ReadResult(
                List.of(new ImportedJob.PollingTrigger("wfe-2", future)), 1, 2, 3, 4, true));
        Mockito.when(schedulerClient.scheduleIfNotExists(Mockito.any(SchedulableInstance.class)))
            .thenReturn(false);

        ImportSummary summary = importer.importJobs();

        Assertions.assertThat(summary)
            .isEqualTo(new ImportSummary(1, 0, 1, 1, 2, 3, 4, true));
    }

    @Test
    void testSchedulerClientFailureIsCountedAndDoesNotAbort() {
        Mockito.when(reader.read())
            .thenReturn(new QuartzJobReader.ReadResult(
                List.of(
                    new ImportedJob.PollingTrigger("wfe-a", future),
                    new ImportedJob.PollingTrigger("wfe-b", future)),
                0, 0, 0, 0, true));
        Mockito.when(schedulerClient.scheduleIfNotExists(Mockito.any(SchedulableInstance.class)))
            .thenThrow(new IllegalStateException("db down"))
            .thenReturn(true);

        ImportSummary summary = importer.importJobs();

        Assertions.assertThat(summary.failed())
            .isEqualTo(1);
        Assertions.assertThat(summary.imported())
            .isEqualTo(1);
    }
}
