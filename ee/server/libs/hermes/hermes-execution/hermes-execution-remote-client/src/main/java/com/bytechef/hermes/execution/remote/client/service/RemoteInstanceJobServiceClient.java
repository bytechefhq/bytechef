/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.hermes.execution.remote.client.service;

import com.bytechef.commons.webclient.LoadBalancedWebClient;
import com.bytechef.hermes.execution.domain.InstanceJob;
import com.bytechef.hermes.execution.service.InstanceJobService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class RemoteInstanceJobServiceClient implements InstanceJobService {

    private static final String EXECUTION_APP = "execution-app";
    private static final String INSTANCE_JOB_SERVICE = "/remote/instance-job-service";

    private final LoadBalancedWebClient loadBalancedWebClient;

    @SuppressFBWarnings("EI")
    public RemoteInstanceJobServiceClient(LoadBalancedWebClient loadBalancedWebClient) {
        this.loadBalancedWebClient = loadBalancedWebClient;
    }

    @Override
    public InstanceJob create(long jobId, long instanceId, int type) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<Long> fetchJobInstanceId(long jobId, int type) {
        return Optional.ofNullable(
            loadBalancedWebClient.get(
                uriBuilder -> uriBuilder
                    .host(EXECUTION_APP)
                    .path(INSTANCE_JOB_SERVICE + "/fetch-job-instance-id/{jobId}/{type}")
                    .build(jobId, type),
                Long.class));
    }
}
