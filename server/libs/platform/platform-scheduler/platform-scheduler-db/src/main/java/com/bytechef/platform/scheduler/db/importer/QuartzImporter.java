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

import static com.bytechef.platform.scheduler.db.task.DbSchedulerTaskDescriptors.DYNAMIC_WEBHOOK_REFRESH;
import static com.bytechef.platform.scheduler.db.task.DbSchedulerTaskDescriptors.OAUTH2_TOKEN_REFRESH;
import static com.bytechef.platform.scheduler.db.task.DbSchedulerTaskDescriptors.ONE_TIME_RESUME;
import static com.bytechef.platform.scheduler.db.task.DbSchedulerTaskDescriptors.POLLING_TRIGGER;
import static com.bytechef.platform.scheduler.db.task.DbSchedulerTaskDescriptors.SCHEDULE_TRIGGER;

import com.bytechef.platform.scheduler.db.task.DynamicWebhookRefreshData;
import com.bytechef.platform.scheduler.db.task.OAuth2TokenRefreshData;
import com.bytechef.platform.scheduler.db.task.OneTimeResumeData;
import com.bytechef.platform.scheduler.db.task.PollingTriggerData;
import com.bytechef.platform.scheduler.db.task.ScheduleTriggerData;
import com.github.kagkarlsson.scheduler.SchedulerClient;
import com.github.kagkarlsson.scheduler.task.SchedulableInstance;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Ivica Cardic
 */
public class QuartzImporter {

    private static final Logger log = LoggerFactory.getLogger(QuartzImporter.class);

    private final int pollingTriggerCheckPeriod;
    private final QuartzJobReader quartzJobReader;
    private final SchedulerClient schedulerClient;

    @SuppressFBWarnings("EI")
    public QuartzImporter(
        QuartzJobReader quartzJobReader, SchedulerClient schedulerClient, int pollingTriggerCheckPeriod) {

        this.quartzJobReader = quartzJobReader;
        this.schedulerClient = schedulerClient;
        this.pollingTriggerCheckPeriod = pollingTriggerCheckPeriod;
    }

    public ImportSummary importJobs() {
        QuartzJobReader.ReadResult readResult = quartzJobReader.read();

        int imported = 0;
        int alreadyPresent = 0;
        int failed = readResult.failed();

        for (ImportedJob importedJob : readResult.jobs()) {
            try {
                if (schedulerClient.scheduleIfNotExists(toSchedulableInstance(importedJob))) {
                    imported++;
                } else {
                    alreadyPresent++;
                }
            } catch (RuntimeException runtimeException) {
                log.warn("Unable to import Quartz job {}", importedJob.instanceId(), runtimeException);

                failed++;
            }
        }

        int scanned = readResult.jobs()
            .size();

        return new ImportSummary(
            scanned, imported, alreadyPresent, readResult.skippedStatic(), readResult.skippedUnknown(),
            readResult.skippedComplete(), failed, readResult.quartzReadable());
    }

    private SchedulableInstance<?> toSchedulableInstance(ImportedJob importedJob) {
        Instant executionTime = dueNoEarlierThanNow(importedJob.nextFireTime());

        return switch (importedJob) {
            case ImportedJob.ScheduleTrigger scheduleTrigger -> SCHEDULE_TRIGGER
                .instance(scheduleTrigger.instanceId())
                .data(new ScheduleTriggerData(
                    scheduleTrigger.cronPattern(), scheduleTrigger.zoneId(), scheduleTrigger.output()))
                .scheduledTo(executionTime);
            case ImportedJob.PollingTrigger pollingTrigger -> POLLING_TRIGGER.instance(pollingTrigger.instanceId())
                .data(new PollingTriggerData(pollingTriggerCheckPeriod))
                .scheduledTo(executionTime);
            case ImportedJob.DynamicWebhookRefresh dynamicWebhookRefresh -> DYNAMIC_WEBHOOK_REFRESH
                .instance(dynamicWebhookRefresh.instanceId())
                .data(new DynamicWebhookRefreshData(dynamicWebhookRefresh.connectionId()))
                .scheduledTo(executionTime);
            case ImportedJob.OAuth2TokenRefresh oAuth2TokenRefresh -> OAUTH2_TOKEN_REFRESH
                .instance(oAuth2TokenRefresh.instanceId())
                .data(new OAuth2TokenRefreshData(oAuth2TokenRefresh.connectionId(), oAuth2TokenRefresh.tenantId()))
                .scheduledTo(executionTime);
            case ImportedJob.OneTimeResume oneTimeResume -> ONE_TIME_RESUME.instance(oneTimeResume.instanceId())
                .data(new OneTimeResumeData(
                    oneTimeResume.jobId(), oneTimeResume.continueParameters(), oneTimeResume.tenantId()))
                .scheduledTo(executionTime);
        };
    }

    private static Instant dueNoEarlierThanNow(Instant nextFireTime) {
        Instant now = Instant.now();

        return nextFireTime.isBefore(now) ? now : nextFireTime;
    }
}
