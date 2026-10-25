/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.remote.client.subflow;

import com.bytechef.atlas.execution.dto.JobParametersDTO;
import com.bytechef.ee.remote.client.LoadBalancedRestClient;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.workflow.task.dispatcher.subflow.ChildJobPrincipalFactory;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class RemoteChildJobPrincipalFactoryClient implements ChildJobPrincipalFactory {

    private static final String CHILD_JOB_PRINCIPAL_FACTORY = "/remote/child-job-principal-factory";
    private static final String CONFIGURATION_APP = "configuration-app";

    private final LoadBalancedRestClient loadBalancedRestClient;

    @SuppressFBWarnings("EI")
    public RemoteChildJobPrincipalFactoryClient(LoadBalancedRestClient loadBalancedRestClient) {
        this.loadBalancedRestClient = loadBalancedRestClient;
    }

    @Override
    public long createChildJob(long parentJobId, JobParametersDTO jobParametersDTO) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long createPrincipalLinkedJob(long referenceJobId, JobParametersDTO jobParametersDTO) {
        return loadBalancedRestClient.post(
            uriBuilder -> uriBuilder
                .host(CONFIGURATION_APP)
                .path(CHILD_JOB_PRINCIPAL_FACTORY + "/create-principal-linked-job")
                .build(),
            new CreatePrincipalLinkedJobRequest(referenceJobId, jobParametersDTO), Long.class);
    }

    @SuppressFBWarnings("EI")
    public record CreatePrincipalLinkedJobRequest(long referenceJobId, JobParametersDTO jobParameters) {
    }
}
