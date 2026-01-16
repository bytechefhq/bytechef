/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.workflow.execution.remote.client.facade;

import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.dto.JobParametersDTO;
import com.bytechef.ee.remote.client.LoadBalancedRestClient;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.workflow.execution.facade.PrincipalJobFacade;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class RemotePrincipalJobFacadeClient implements PrincipalJobFacade {

    private static final String PRINCIPAL_JOB_FACADE = "/remote/principal-job-facade";
    private static final String EXECUTION_APP = "execution-app";

    private final LoadBalancedRestClient loadBalancedRestClient;

    @SuppressFBWarnings("EI")
    public RemotePrincipalJobFacadeClient(LoadBalancedRestClient loadBalancedRestClient) {
        this.loadBalancedRestClient = loadBalancedRestClient;
    }

    @Override
    public long createJob(JobParametersDTO jobParametersDTO, long jobPrincipalId, PlatformType type) {
        return loadBalancedRestClient.post(
            uriBuilder -> uriBuilder
                .host(EXECUTION_APP)
                .path(PRINCIPAL_JOB_FACADE + "/create-job")
                .build(),
            new CreateJobRequest(jobParametersDTO, jobPrincipalId, type), Long.class);
    }

    @Override
    public Job createSyncJob(JobParametersDTO jobParametersDTO, long jobPrincipalId, PlatformType type) {
        return loadBalancedRestClient.post(
            uriBuilder -> uriBuilder
                .host(EXECUTION_APP)
                .path(PRINCIPAL_JOB_FACADE + "/create-sync-job")
                .build(),
            new CreateJobRequest(jobParametersDTO, jobPrincipalId, type), Job.class);
    }

    @SuppressFBWarnings("EI")
    public record CreateJobRequest(JobParametersDTO jobParameters, long jobPrincipalId, PlatformType type) {
    }
}
