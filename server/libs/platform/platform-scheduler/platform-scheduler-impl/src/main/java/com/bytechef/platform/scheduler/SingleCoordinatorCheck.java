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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.concurrent.TimeUnit;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerMetaData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Warns when another coordinator's clustered Quartz scheduler checks in to the same database in the Community Edition,
 * which supports one coordinator.
 *
 * @author Ivica Cardic
 */
public class SingleCoordinatorCheck {

    static final String ANOTHER_COORDINATOR_MESSAGE =
        "Another coordinator is running against this database. The Community Edition supports one coordinator: with " +
            "more, caches go stale and sessions are not shared. Running more than one coordinator requires ByteChef " +
            "Enterprise Edition (bytechef.edition=ee, BYTECHEF_EDITION=ee).";

    private static final long CHECKIN_GRACE_MILLIS = 7_500;
    private static final String OTHER_SCHEDULER_INSTANCE_EXISTS = "SELECT EXISTS (SELECT 1 FROM QRTZ_SCHEDULER_STATE " +
        "WHERE SCHED_NAME = ? AND INSTANCE_NAME <> ? AND LAST_CHECKIN_TIME + CHECKIN_INTERVAL + ? >= ?)";

    private static final Logger log = LoggerFactory.getLogger(SingleCoordinatorCheck.class);

    private final JdbcOperations jdbcOperations;
    private final Scheduler scheduler;

    @SuppressFBWarnings("EI")
    public SingleCoordinatorCheck(JdbcOperations jdbcOperations, Scheduler scheduler) {
        this.jdbcOperations = jdbcOperations;
        this.scheduler = scheduler;
    }

    @Scheduled(initialDelay = 1, fixedDelay = 10, timeUnit = TimeUnit.MINUTES)
    public void check() {
        try {
            if (isAnotherCoordinatorRunning()) {
                log.warn(ANOTHER_COORDINATOR_MESSAGE);
            }
        } catch (DataAccessException | SchedulerException exception) {
            log.debug("Single coordinator check failed", exception);
        }
    }

    boolean isAnotherCoordinatorRunning() throws SchedulerException {
        if (!scheduler.isStarted() || scheduler.isShutdown()) {
            return false;
        }

        SchedulerMetaData schedulerMetaData = scheduler.getMetaData();

        if (!schedulerMetaData.isJobStoreClustered()) {
            return false;
        }

        Boolean exists = jdbcOperations.queryForObject(
            OTHER_SCHEDULER_INSTANCE_EXISTS, Boolean.class, scheduler.getSchedulerName(),
            scheduler.getSchedulerInstanceId(), CHECKIN_GRACE_MILLIS, System.currentTimeMillis());

        return Boolean.TRUE.equals(exists);
    }
}
