/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.hermes.execution.remote.client.facade;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.dto.JobParameters;
import com.bytechef.commons.rest.client.LoadBalancedRestClient;
import com.bytechef.hermes.execution.facade.InstanceJobFacade;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class RemoteInstanceJobFacadeClient implements InstanceJobFacade {

    private static final String INSTANCE_JOB_FACADE = "/remote/instance-job-facade";
    private static final String EXECUTION_APP = "execution-app";

    private final LoadBalancedRestClient loadBalancedRestClient;

    @SuppressFBWarnings("EI")
    public RemoteInstanceJobFacadeClient(LoadBalancedRestClient loadBalancedRestClient) {
        this.loadBalancedRestClient = loadBalancedRestClient;
    }

    @Override
    public Job createAsyncJob(JobParameters jobParameters, long instanceId, int type) {
        return post(new CreateJobRequest(jobParameters, null, instanceId, type));
    }

    @Override
    public Job createSyncJob(JobParameters jobParameters, Workflow workflow, long instanceId, int type) {
        return post(new CreateJobRequest(jobParameters, workflow, instanceId, type));
    }

    private Job post(CreateJobRequest createJobRequest) {
        return loadBalancedRestClient.post(
            uriBuilder -> uriBuilder
                .host(EXECUTION_APP)
                .path(INSTANCE_JOB_FACADE + "/create-job")
                .build(),
            createJobRequest, Job.class);
    }

    @SuppressFBWarnings("EI")
    public record CreateJobRequest(JobParameters jobParameters, Workflow workflow, long instanceId, int type) {
    }
}
