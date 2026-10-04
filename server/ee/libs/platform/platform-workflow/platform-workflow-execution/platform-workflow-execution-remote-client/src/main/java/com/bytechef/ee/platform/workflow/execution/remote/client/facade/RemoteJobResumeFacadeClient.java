/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.workflow.execution.remote.client.facade;

import com.bytechef.ee.remote.client.LoadBalancedRestClient;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.workflow.execution.facade.JobResumeFacade;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import java.util.function.LongConsumer;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class RemoteJobResumeFacadeClient implements JobResumeFacade {

    private static final String EXECUTION_APP = "execution-app";
    private static final String JOB_RESUME_FACADE = "/remote/job-resume-facade";

    private final LoadBalancedRestClient loadBalancedRestClient;

    @SuppressFBWarnings("EI")
    public RemoteJobResumeFacadeClient(LoadBalancedRestClient loadBalancedRestClient) {
        this.loadBalancedRestClient = loadBalancedRestClient;
    }

    @Override
    public JobResumeOutcome resumeExpiredJob(String id) {
        return loadBalancedRestClient.post(
            uriBuilder -> uriBuilder
                .host(EXECUTION_APP)
                .path(JOB_RESUME_FACADE + "/resume-expired-job")
                .build(),
            new ResumeExpiredJobRequest(id), JobResumeOutcome.class);
    }

    @Override
    public JobResumeOutcome resumeJob(String id, Map<String, Object> data) {
        throw new UnsupportedOperationException();
    }

    @Override
    public JobResumeOutcome resumeJobStreaming(
        String id, Map<String, Object> data, LongConsumer jobIdConsumer) {

        throw new UnsupportedOperationException();
    }

    private record ResumeExpiredJobRequest(String id) {
    }
}
