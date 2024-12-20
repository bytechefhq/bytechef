/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.workflow.execution.remote.client.service;

import com.bytechef.atlas.execution.domain.Job.Status;
import com.bytechef.ee.remote.client.LoadBalancedRestClient;
import com.bytechef.platform.constant.ModeType;
import com.bytechef.platform.workflow.execution.domain.InstanceJob;
import com.bytechef.platform.workflow.execution.service.InstanceJobService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
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
    public InstanceJob create(long jobId, long instanceId, ModeType type) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteInstanceJobs(long jobId, ModeType type) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<Long> fetchLastJobId(long instanceId, ModeType type) {
        return Optional.ofNullable(
            loadBalancedRestClient.get(
                uriBuilder -> uriBuilder
                    .host(EXECUTION_APP)
                    .path(INSTANCE_JOB_SERVICE + "/fetch-last-job-id/{instanceId}/{type}")
                    .build(instanceId, type),
                Long.class));
    }

    @Override
    public Optional<Long> fetchJobInstanceId(long jobId, ModeType type) {
        return Optional.ofNullable(
            loadBalancedRestClient.get(
                uriBuilder -> uriBuilder
                    .host(EXECUTION_APP)
                    .path(INSTANCE_JOB_SERVICE + "/fetch-job-instance-id/{jobId}/{type}")
                    .build(jobId, type),
                Long.class));
    }

    @Override
    public long getJobInstanceId(long jobId, ModeType type) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Long> getJobIds(long instanceId, ModeType type) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Page<Long> getJobIds(
        Status status, Instant startDate, Instant endDate, List<Long> instanceIds, ModeType type,
        List<String> workflowIds, int pageNumber) {

        throw new UnsupportedOperationException();
    }
}
