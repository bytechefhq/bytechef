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

import com.bytechef.platform.scheduler.job.ConnectionOAuth2TokenRefreshJob;
import com.bytechef.platform.scheduler.util.QuartzConnectionRefreshUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Nikolina Spehar
 * @author Igor Beslic
 */
public class QuartzConnectionRefreshScheduler implements ConnectionRefreshScheduler {

    private final Scheduler scheduler;

    private static final Logger log = LoggerFactory.getLogger(QuartzConnectionRefreshScheduler.class);

    @SuppressFBWarnings("EI")
    public QuartzConnectionRefreshScheduler(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    @Override
    public void cancelConnectionRefresh(Long connectionId, String tenantId) {
        deleteJob(connectionId, tenantId);
    }

    @Override
    public void scheduleConnectionRefresh(Long connectionId, Instant tokenExpirationTime, String tenantId) {
        JobDetail jobDetail = JobBuilder.newJob(ConnectionOAuth2TokenRefreshJob.class)
            .withIdentity(QuartzConnectionRefreshUtils.buildJobKey(connectionId, tenantId))
            .usingJobData("connectionId", connectionId)
            .usingJobData("tenantId", tenantId)
            .build();

        Trigger trigger = QuartzConnectionRefreshUtils.buildRefreshTrigger(
            QuartzConnectionRefreshUtils.buildTriggerKey(connectionId, tenantId),
            connectionId,
            tokenExpirationTime);

        schedule(jobDetail, trigger);
    }

    private void deleteJob(Long connectionId, String tenantId) {
        try {
            JobKey jobKey = QuartzConnectionRefreshUtils.buildJobKey(connectionId, tenantId);

            if (scheduler.checkExists(jobKey) && scheduler.deleteJob(jobKey)) {
                log.trace(
                    "Refresh token job removed for connectionId: {}, tenantId: {}, triggerKey: {}", connectionId,
                    tenantId, CONNECTION_OAUTH_2_TOKEN_REFRESH);

                return;
            }

            log.error(
                "Refresh token job not found for connectionId: {}, tenantId: {}, triggerKey: {}", connectionId,
                tenantId, CONNECTION_OAUTH_2_TOKEN_REFRESH);
        } catch (SchedulerException e) {
            log.error(
                "Unable to delete refresh token job for connectionId: {}, tenantId: {}, triggerKey: {}",
                connectionId, tenantId, CONNECTION_OAUTH_2_TOKEN_REFRESH);
        }
    }

    private void schedule(JobDetail jobDetail, Trigger trigger) {
        try {
            if (scheduler.checkExists(jobDetail.getKey())) {
                scheduler.deleteJob(jobDetail.getKey());
            }

            scheduler.scheduleJob(jobDetail, trigger);

            log.trace("Re-scheduled refresh token job with key: {}", jobDetail.getKey());
        } catch (SchedulerException e) {
            log.error("Unable to re-schedule refresh token job with key: {}", jobDetail.getKey());
        }
    }
}
