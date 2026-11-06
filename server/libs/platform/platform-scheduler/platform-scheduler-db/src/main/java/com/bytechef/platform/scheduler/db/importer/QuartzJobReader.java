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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.quartz.CronTrigger;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.impl.matchers.GroupMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Ivica Cardic
 */
public class QuartzJobReader {

    @SuppressFBWarnings("EI")
    public record ReadResult(
        List<ImportedJob> jobs, int skippedStatic, int skippedUnknown, int skippedComplete, int failed,
        boolean quartzReadable) {
    }

    private static final Logger log = LoggerFactory.getLogger(QuartzJobReader.class);

    private final Scheduler scheduler;

    @SuppressFBWarnings("EI")
    public QuartzJobReader(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    public ReadResult read() {
        List<String> groupNames;

        try {
            groupNames = scheduler.getJobGroupNames();
        } catch (SchedulerException schedulerException) {
            log.info(
                "Quartz job store is not readable, nothing to import: {}", schedulerException.getMessage());

            return new ReadResult(List.of(), 0, 0, 0, 0, false);
        }

        List<ImportedJob> jobs = new ArrayList<>();
        Set<JobKey> seenJobKeys = new HashSet<>();
        int skippedStatic = 0;
        int skippedUnknown = 0;
        int skippedComplete = 0;
        int failed = 0;

        for (String groupName : groupNames) {
            Set<JobKey> jobKeys;

            try {
                jobKeys = scheduler.getJobKeys(GroupMatcher.jobGroupEquals(groupName));
            } catch (SchedulerException schedulerException) {
                log.warn("Unable to list Quartz jobs in group {}", groupName, schedulerException);

                failed++;

                continue;
            }

            for (JobKey jobKey : jobKeys) {
                if (!seenJobKeys.add(jobKey)) {
                    continue;
                }

                try {
                    JobDetail jobDetail = scheduler.getJobDetail(jobKey);
                    Class<?> jobClass = jobDetail.getJobClass();
                    String jobClassName = jobClass.getSimpleName();

                    if ("StripeUsageReportingJob".equals(jobClassName)) {
                        skippedStatic++;

                        continue;
                    }

                    if (!isImportable(jobClassName)) {
                        log.warn("Skipping Quartz job {} of unknown class {}", jobKey, jobClassName);

                        skippedUnknown++;

                        continue;
                    }

                    List<? extends Trigger> triggers = scheduler.getTriggersOfJob(jobKey);

                    if (triggers.isEmpty()) {
                        skippedComplete++;

                        continue;
                    }

                    Trigger trigger = triggers.getFirst();
                    Instant nextFireTime = getNextFireTime(trigger);

                    if (nextFireTime == null) {
                        skippedComplete++;

                        continue;
                    }

                    jobs.add(toImportedJob(jobClassName, jobKey, jobDetail.getJobDataMap(), trigger, nextFireTime));
                } catch (Exception exception) {
                    log.warn("Unable to read Quartz job {}", jobKey, exception);

                    failed++;
                }
            }
        }

        return new ReadResult(jobs, skippedStatic, skippedUnknown, skippedComplete, failed, true);
    }

    private static boolean isImportable(String jobClassName) {
        return switch (jobClassName) {
            case "ScheduleTriggerJob", "PollingTriggerJob", "DynamicWebhookTriggerRefreshJob",
                "ConnectionOAuth2TokenRefreshJob", "OneTimeSchedulerJob" -> true;
            default -> false;
        };
    }

    private static @Nullable Instant getNextFireTime(Trigger trigger) {
        Date nextFireTime = trigger.getNextFireTime();

        return nextFireTime == null ? null : nextFireTime.toInstant();
    }

    private static ImportedJob toImportedJob(
        String jobClassName, JobKey jobKey, JobDataMap jobDataMap, Trigger trigger, Instant nextFireTime) {

        String instanceId = jobKey.getName();

        return switch (jobClassName) {
            case "ScheduleTriggerJob" -> {
                if (!(trigger instanceof CronTrigger cronTrigger)) {
                    throw new IllegalStateException(
                        "Quartz job %s of class ScheduleTriggerJob does not carry a CronTrigger".formatted(jobKey));
                }

                yield new ImportedJob.ScheduleTrigger(
                    instanceId, cronTrigger.getCronExpression(),
                    cronTrigger.getTimeZone()
                        .getID(),
                    requireStringValue(jobDataMap, "output", jobKey), nextFireTime);
            }
            case "PollingTriggerJob" -> new ImportedJob.PollingTrigger(instanceId, nextFireTime);
            case "DynamicWebhookTriggerRefreshJob" -> new ImportedJob.DynamicWebhookRefresh(
                instanceId, requireLongValue(jobDataMap, "connectionId", jobKey), nextFireTime);
            case "ConnectionOAuth2TokenRefreshJob" -> new ImportedJob.OAuth2TokenRefresh(
                instanceId, requireLongValue(jobDataMap, "connectionId", jobKey),
                requireStringValue(jobDataMap, "tenantId", jobKey), nextFireTime);
            case "OneTimeSchedulerJob" -> new ImportedJob.OneTimeResume(
                instanceId, requireLongValue(jobDataMap, "jobId", jobKey),
                optionalStringValue(jobDataMap, "continueParameters"), nextFireTime);
            default -> throw new IllegalStateException("Unexpected job class " + jobClassName);
        };
    }

    private static long requireLongValue(JobDataMap jobDataMap, String key, JobKey jobKey) {
        Object value = jobDataMap.get(key);

        if (value instanceof Number number) {
            return number.longValue();
        }

        throw new IllegalStateException(
            "Quartz job %s is missing a numeric '%s' job data value".formatted(jobKey, key));
    }

    private static String requireStringValue(JobDataMap jobDataMap, String key, JobKey jobKey) {
        if (!jobDataMap.containsKey(key)) {
            throw new IllegalStateException(
                "Quartz job %s is missing the '%s' job data value".formatted(jobKey, key));
        }

        return String.valueOf(jobDataMap.get(key));
    }

    private static @Nullable String optionalStringValue(JobDataMap jobDataMap, String key) {
        return jobDataMap.containsKey(key) ? String.valueOf(jobDataMap.get(key)) : null;
    }
}
