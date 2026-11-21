/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.workflow.execution.security;

import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.automation.configuration.security.ResourceEnvironmentResolver;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.Serializable;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class EmbeddedJobEnvironmentResolver implements ResourceEnvironmentResolver {

    private final EmbeddedPrincipalEnvironmentResolver embeddedPrincipalEnvironmentResolver;
    private final JobService jobService;
    private final PrincipalJobService principalJobService;

    @SuppressFBWarnings("EI")
    public EmbeddedJobEnvironmentResolver(
        EmbeddedPrincipalEnvironmentResolver embeddedPrincipalEnvironmentResolver, JobService jobService,
        PrincipalJobService principalJobService) {

        this.embeddedPrincipalEnvironmentResolver = embeddedPrincipalEnvironmentResolver;
        this.jobService = jobService;
        this.principalJobService = principalJobService;
    }

    @Override
    public String resourceType() {
        return "Job";
    }

    @Override
    public Optional<Environment> fetchEnvironment(Serializable id) {
        if (!(id instanceof Number number)) {
            return Optional.empty();
        }

        long jobId = number.longValue();

        Optional<Long> principalIdOptional = principalJobService.fetchJobPrincipalId(jobId, PlatformType.EMBEDDED);

        if (principalIdOptional.isEmpty()) {
            return Optional.empty();
        }

        Optional<String> workflowIdOptional = jobService.fetchJob(jobId)
            .map(Job::getWorkflowId);

        if (workflowIdOptional.isEmpty()) {
            return Optional.empty();
        }

        String workflowId = workflowIdOptional.get();

        return embeddedPrincipalEnvironmentResolver.fetchEnvironment(
            principalIdOptional.get(), integrationWorkflow -> workflowId.equals(integrationWorkflow.getWorkflowId()));
    }
}
