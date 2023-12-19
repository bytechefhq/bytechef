/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.hermes.execution.remote.client.service;

import com.bytechef.commons.rest.client.LoadBalancedRestClient;
import com.bytechef.hermes.execution.domain.InstanceJob;
import com.bytechef.hermes.execution.service.InstanceJobService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
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

    private final LoadBalancedRestClient loadBalancedRestClient;

    @SuppressFBWarnings("EI")
    public RemoteInstanceJobServiceClient(LoadBalancedRestClient loadBalancedRestClient) {
        this.loadBalancedRestClient = loadBalancedRestClient;
    }

    @Override
    public InstanceJob create(long jobId, long instanceId, int type) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<Long> fetchLastJobId(long instanceId, int type) {
        return Optional.ofNullable(
            loadBalancedRestClient.get(
                uriBuilder -> uriBuilder
                    .host(EXECUTION_APP)
                    .path(INSTANCE_JOB_SERVICE + "/fetch-last-job-id/{instanceId}/{type}")
                    .build(instanceId, type),
                Long.class));
    }

    @Override
    public Optional<Long> fetchJobInstanceId(long jobId, int type) {
        return Optional.ofNullable(
            loadBalancedRestClient.get(
                uriBuilder -> uriBuilder
                    .host(EXECUTION_APP)
                    .path(INSTANCE_JOB_SERVICE + "/fetch-job-instance-id/{jobId}/{type}")
                    .build(jobId, type),
                Long.class));
    }

    @Override
    public Page<Long> getJobIds(
        String status, LocalDateTime startDate, LocalDateTime endDate, Long instanceId, int type,
        List<String> workflowIds, int pageNumber) {

        throw new UnsupportedOperationException();
    }
}
