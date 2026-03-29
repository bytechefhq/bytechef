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

import com.bytechef.platform.scheduler.job.ConnectionOAuth2TokenRefreshJob;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;

/**
 * @author Nikolina Spehar
 */
public class QuartzConnectionRefreshScheduler implements ConnectionRefreshScheduler {

    private final Scheduler scheduler;

    @SuppressFBWarnings("EI")
    public QuartzConnectionRefreshScheduler(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    @Override
    public void cancelConnectionRefresh(Long connectionId) {
        deleteJob(connectionId, "ConnectionOauth2TokenRefresh");
    }

    @Override
    public void scheduleConnectionRefresh(Long connectionId, Instant expiry) {

        JobDetail jobDetail = JobBuilder.newJob(ConnectionOAuth2TokenRefreshJob.class)
            .withIdentity(JobKey.jobKey(connectionId.toString(), "ConnectionOauth2TokenRefresh"))
            .usingJobData("connectionId", connectionId)
            .build();

        Trigger trigger = TriggerBuilder.newTrigger()
            .withIdentity(TriggerKey.triggerKey(connectionId.toString(), "ConnectionOauth2TokenRefresh"))
            .withDescription("Connection OAuth2 token refresh for " + connectionId)
            .startAt(Date.from(expiry.minus(Duration.ofMinutes(5))))
            .build();

        schedule(jobDetail, trigger);
    }

    private void deleteJob(Long connectionId, String triggerKey) {
        try {
            boolean deleted = scheduler.deleteJob(JobKey.jobKey(connectionId.toString(), triggerKey));
            if (!deleted) {
                throw new RuntimeException(
                    "Job not found for connectionId: " + connectionId + ", group: " + triggerKey);
            }
        } catch (SchedulerException e) {
            throw new RuntimeException(e);
        }
    }

    private void schedule(JobDetail jobDetail, Trigger trigger) {
        try {
            scheduler.deleteJob(jobDetail.getKey());

            scheduler.scheduleJob(jobDetail, trigger);
        } catch (SchedulerException e) {
            throw new RuntimeException(e);
        }
    }
}
