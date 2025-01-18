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
import com.bytechef.platform.workflow.execution.domain.PrincipalJob;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
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
public class RemotePrincipalJobServiceClient implements PrincipalJobService {

    private static final String EXECUTION_APP = "execution-app";
    private static final String PRINCIPAL_JOB_SERVICE = "/remote/principal-job-service";

    private final LoadBalancedRestClient loadBalancedRestClient;

    @SuppressFBWarnings("EI")
    public RemotePrincipalJobServiceClient(LoadBalancedRestClient loadBalancedRestClient) {
        this.loadBalancedRestClient = loadBalancedRestClient;
    }

    @Override
    public PrincipalJob create(long jobId, long instanceId, ModeType type) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deletePrincipalJobs(long jobId, ModeType type) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<Long> fetchLastJobId(long instanceId, ModeType type) {
        return Optional.ofNullable(
            loadBalancedRestClient.get(
                uriBuilder -> uriBuilder
                    .host(EXECUTION_APP)
                    .path(PRINCIPAL_JOB_SERVICE + "/fetch-last-job-id/{instanceId}/{type}")
                    .build(instanceId, type),
                Long.class));
    }

    @Override
    public Optional<Long> fetchJobPrincipalId(long jobId, ModeType type) {
        return Optional.ofNullable(
            loadBalancedRestClient.get(
                uriBuilder -> uriBuilder
                    .host(EXECUTION_APP)
                    .path(PRINCIPAL_JOB_SERVICE + "/fetch-job-instance-id/{jobId}/{type}")
                    .build(jobId, type),
                Long.class));
    }

    @Override
    public long getJobPrincipalId(long jobId, ModeType type) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Long> getJobIds(long principalId, ModeType type) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Page<Long> getJobIds(
        Status status, Instant startDate, Instant endDate, List<Long> principalIds, ModeType type,
        List<String> workflowIds, int pageNumber) {

        throw new UnsupportedOperationException();
    }
}
