/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.remote.client.service;

import com.bytechef.ee.remote.client.LoadBalancedRestClient;
import com.bytechef.embedded.configuration.domain.AppEvent;
import com.bytechef.embedded.configuration.service.AppEventService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class RemoteAppEventServiceClient implements AppEventService {

    private static final String CONFIGURATION_APP = "configuration-app";
    private static final String PROJECT_SERVICE = "/remote/app-event-service";

    private final LoadBalancedRestClient loadBalancedRestClient;

    @SuppressFBWarnings("EI")
    public RemoteAppEventServiceClient(LoadBalancedRestClient loadBalancedRestClient) {
        this.loadBalancedRestClient = loadBalancedRestClient;
    }

    @Override
    public AppEvent create(@NonNull AppEvent appEvent) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public AppEvent getAppEvent(long id) {
        return loadBalancedRestClient.get(
            uriBuilder -> uriBuilder
                .host(CONFIGURATION_APP)
                .path(PROJECT_SERVICE + "/get-app-event/{id}")
                .build(id),
            AppEvent.class);
    }

    @Override
    public List<AppEvent> getAppEvents() {
        throw new UnsupportedOperationException();
    }

    @Override
    public AppEvent update(@NonNull AppEvent appEvent) {
        throw new UnsupportedOperationException();
    }
}
