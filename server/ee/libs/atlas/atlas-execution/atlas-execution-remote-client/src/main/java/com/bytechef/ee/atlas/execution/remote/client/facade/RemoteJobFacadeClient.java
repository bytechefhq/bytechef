/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.atlas.execution.remote.client.facade;

import com.bytechef.atlas.execution.dto.JobParametersDTO;
import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.ee.remote.client.LoadBalancedRestClient;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class RemoteJobFacadeClient implements JobFacade {

    private final LoadBalancedRestClient loadBalancedRestClient;

    @SuppressFBWarnings("EI")
    public RemoteJobFacadeClient(LoadBalancedRestClient loadBalancedRestClient) {
        this.loadBalancedRestClient = loadBalancedRestClient;
    }

    @Override
    public long createJob(JobParametersDTO jobParametersDTO) {
        return loadBalancedRestClient.post(
            uriBuilder -> uriBuilder
                .host("execution-app")
                .path("/remote/job-facade/create-async-job")
                .build(),
            jobParametersDTO, Long.class);
    }

    @Override
    public void deleteJob(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void restartJob(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void stopJob(long id) {
        throw new UnsupportedOperationException();
    }
}
