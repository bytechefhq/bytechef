/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.workflow.execution.remote.client.facade;

import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.ee.remote.client.LoadBalancedRestClient;
import com.bytechef.platform.workflow.execution.facade.ConnectionLifecycleFacade;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class RemoteConnectionLifecycleFacadeClient implements ConnectionLifecycleFacade {

    private static final String CONNECTION_LIFECYCLE_FACADE = "/remote/connection-lifecycle-facade";
    private static final String EXECUTION_APP = "execution-app";

    private final LoadBalancedRestClient loadBalancedRestClient;

    @SuppressFBWarnings("EI")
    public RemoteConnectionLifecycleFacadeClient(LoadBalancedRestClient loadBalancedRestClient) {
        this.loadBalancedRestClient = loadBalancedRestClient;
    }

    @Override
    public void scheduleConnectionRefresh(
        Long connectionId, Map<String, ?> parameters, AuthorizationType authorizationType) {

        loadBalancedRestClient.post(
            uriBuilder -> uriBuilder
                .host(EXECUTION_APP)
                .path(CONNECTION_LIFECYCLE_FACADE + "/schedule-connection-refresh")
                .build(),
            new ScheduleConnectionRefreshRequest(connectionId, parameters, authorizationType));
    }

    @Override
    public void deleteScheduledConnectionRefresh(Long connectionId, AuthorizationType authorizationType) {
        loadBalancedRestClient.post(
            uriBuilder -> uriBuilder
                .host(EXECUTION_APP)
                .path(CONNECTION_LIFECYCLE_FACADE + "/delete-scheduled-connection-refresh")
                .build(),
            new DeleteScheduledConnectionRefreshRequest(connectionId, authorizationType));
    }

    @SuppressFBWarnings("EI")
    private record ScheduleConnectionRefreshRequest(
        Long connectionId, Map<String, ?> parameters, AuthorizationType authorizationType) {
    }

    private record DeleteScheduledConnectionRefreshRequest(Long connectionId, AuthorizationType authorizationType) {
    }
}
