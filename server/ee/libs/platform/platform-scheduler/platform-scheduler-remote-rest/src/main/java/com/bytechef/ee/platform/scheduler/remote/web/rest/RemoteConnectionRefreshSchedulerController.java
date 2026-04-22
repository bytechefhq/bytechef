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

package com.bytechef.ee.platform.scheduler.remote.web.rest;

import com.bytechef.platform.scheduler.ConnectionRefreshScheduler;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.validation.Valid;
import java.time.Instant;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Nikolina Spehar
 */
@RestController
@RequestMapping("/remote/connection-refresh-scheduler")
public class RemoteConnectionRefreshSchedulerController {

    private final ConnectionRefreshScheduler connectionRefreshScheduler;

    public RemoteConnectionRefreshSchedulerController(ConnectionRefreshScheduler connectionRefreshScheduler) {
        this.connectionRefreshScheduler = connectionRefreshScheduler;
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/cancel-connection-refresh",
        consumes = {
            "application/json"
        })
    void cancelConnectionRefresh(
        @Valid @RequestBody ScheduleConnectionRefreshRequest scheduleConnectionRefreshRequest) {
        connectionRefreshScheduler.cancelConnectionRefresh(
            scheduleConnectionRefreshRequest.connectionId, scheduleConnectionRefreshRequest.tenantId);
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/schedule-connection-refresh",
        consumes = {
            "application/json"
        })
    void scheduleConnectionRefresh(
        @Valid @RequestBody ScheduleConnectionRefreshRequest scheduleConnectionRefreshRequest) {
        connectionRefreshScheduler.scheduleConnectionRefresh(
            scheduleConnectionRefreshRequest.connectionId,
            scheduleConnectionRefreshRequest.expiry,
            scheduleConnectionRefreshRequest.tenantId);
    }

    @SuppressFBWarnings("EI")
    private record ScheduleConnectionRefreshRequest(Long connectionId, Instant expiry, String tenantId) {
    }

}
