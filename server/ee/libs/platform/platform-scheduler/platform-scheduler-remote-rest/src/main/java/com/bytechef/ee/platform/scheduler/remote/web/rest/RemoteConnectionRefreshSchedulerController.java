package com.bytechef.ee.platform.scheduler.remote.web.rest;

import com.bytechef.platform.scheduler.ConnectionRefreshScheduler;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

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
    void cancelConnectionRefresh(@Valid @RequestBody ScheduleConnectionRefreshRequest scheduleConnectionRefreshRequest) {
        connectionRefreshScheduler.cancelConnectionRefresh(
            scheduleConnectionRefreshRequest.connectionId, scheduleConnectionRefreshRequest.tenantId);
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/schedule-connection-refresh",
        consumes = {
            "application/json"
        })
    void scheduleConnectionRefresh(@Valid @RequestBody ScheduleConnectionRefreshRequest scheduleConnectionRefreshRequest) {
        connectionRefreshScheduler.scheduleConnectionRefresh(
            scheduleConnectionRefreshRequest.connectionId,
            scheduleConnectionRefreshRequest.expiry,
            scheduleConnectionRefreshRequest.tenantId);
    }

    @SuppressFBWarnings("EI")
    private record ScheduleConnectionRefreshRequest(Long connectionId, Instant expiry, String tenantId) {
    }

}
