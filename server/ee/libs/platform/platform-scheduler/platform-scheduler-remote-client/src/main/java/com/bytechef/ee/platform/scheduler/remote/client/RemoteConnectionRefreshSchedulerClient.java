/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.scheduler.remote.client;

import com.bytechef.ee.remote.client.LoadBalancedRestClient;
import com.bytechef.platform.scheduler.ConnectionRefreshScheduler;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class RemoteConnectionRefreshSchedulerClient implements ConnectionRefreshScheduler {

    private static final String CONNECTION_REFRESH_SCHEDULER = "/remote/connection-refresh-scheduler";
    private static final String SCHEDULER_APP = "scheduler-app";

    private final LoadBalancedRestClient loadBalancedRestClient;

    @SuppressFBWarnings("EI")
    public RemoteConnectionRefreshSchedulerClient(LoadBalancedRestClient loadBalancedRestClient) {
        this.loadBalancedRestClient = loadBalancedRestClient;
    }

    @Override
    public void cancelConnectionRefresh(Long connectionId) {
        loadBalancedRestClient.post(
            uriBuilder -> uriBuilder
                .host(SCHEDULER_APP)
                .path(CONNECTION_REFRESH_SCHEDULER + "/cancel-connection-refresh")
                .build(),
            connectionId);
    }

    @Override
    public void scheduleConnectionRefresh(Long connectionId, Instant expiry) {
        loadBalancedRestClient.post(
            uriBuilder -> uriBuilder
                .host(SCHEDULER_APP)
                .path(CONNECTION_REFRESH_SCHEDULER + "/schedule-connection-refresh")
                .build(),
            new ScheduleConnectionRefreshRequest(connectionId, expiry));
    }

    private record ScheduleConnectionRefreshRequest(Long connectionId, Instant expiry) {
    }
}
