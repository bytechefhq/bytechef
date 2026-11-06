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

import static com.bytechef.platform.scheduler.db.task.DbSchedulerTaskDescriptors.OAUTH2_TOKEN_REFRESH;

import com.bytechef.platform.scheduler.ConnectionRefreshScheduler;
import com.bytechef.platform.scheduler.db.task.OAuth2TokenRefreshData;
import com.github.kagkarlsson.scheduler.SchedulerClient;
import com.github.kagkarlsson.scheduler.exceptions.TaskInstanceException;
import com.github.kagkarlsson.scheduler.task.SchedulableInstance;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Nikolina Spehar
 */
public class DbConnectionRefreshScheduler implements ConnectionRefreshScheduler {

    static final Duration TOKEN_REFRESH_OFFSET = Duration.ofMinutes(5);

    private static final Logger log = LoggerFactory.getLogger(DbConnectionRefreshScheduler.class);

    private final SchedulerClient schedulerClient;

    @SuppressFBWarnings("EI")
    public DbConnectionRefreshScheduler(SchedulerClient schedulerClient) {
        this.schedulerClient = schedulerClient;
    }

    @Override
    public void cancelConnectionRefresh(Long connectionId, String tenantId) {
        String instanceId = tenantId + connectionId;

        try {
            schedulerClient.cancel(OAUTH2_TOKEN_REFRESH.instanceId(instanceId));
        } catch (TaskInstanceException e) {
            log.warn(
                "Refresh token task not found for connectionId: {}, tenantId: {}", connectionId, tenantId, e);
        }
    }

    @Override
    public void scheduleConnectionRefresh(Long connectionId, Instant tokenExpirationTime, String tenantId) {
        SchedulableInstance<OAuth2TokenRefreshData> instance = OAUTH2_TOKEN_REFRESH.instance(tenantId + connectionId)
            .data(new OAuth2TokenRefreshData(connectionId, tenantId))
            .scheduledTo(tokenExpirationTime.minus(TOKEN_REFRESH_OFFSET));

        try {
            if (!schedulerClient.schedule(instance, SchedulerClient.ScheduleOptions.WHEN_EXISTS_RESCHEDULE)) {
                logCouldNotBeScheduled(connectionId, tenantId, null);
            }
        } catch (TaskInstanceException e) {
            logCouldNotBeScheduled(connectionId, tenantId, e);
        }
    }

    private void logCouldNotBeScheduled(Long connectionId, String tenantId, TaskInstanceException e) {
        log.warn(
            "Refresh token task for connectionId: {}, tenantId: {} could not be scheduled", connectionId, tenantId,
            e);
    }
}
