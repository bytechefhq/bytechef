/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.workflow.execution.remote.web.rest.facade;

import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.dto.JobParametersDTO;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.workflow.execution.facade.PrincipalJobFacade;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Hidden
@RestController
@RequestMapping("/remote/principal-job-facade")
public class RemotePrincipalJobFacadeController {

    private final PrincipalJobFacade principalJobFacade;

    @SuppressFBWarnings("EI")
    public RemotePrincipalJobFacadeController(PrincipalJobFacade principalJobFacade) {
        this.principalJobFacade = principalJobFacade;
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/create-job")
    public ResponseEntity<Long> createJob(@Valid @RequestBody CreateJobRequest createJobRequest) {
        return ResponseEntity.ok(
            principalJobFacade.createJob(
                createJobRequest.jobParameters, createJobRequest.jobPrincipalId, createJobRequest.type));
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/create-sync-job")
    public ResponseEntity<Job> create(@Valid @RequestBody CreateJobRequest createJobRequest) {
        return ResponseEntity.ok(
            principalJobFacade.createSyncJob(
                createJobRequest.jobParameters, createJobRequest.jobPrincipalId,
                createJobRequest.type));
    }

    @SuppressFBWarnings("EI")
    public record CreateJobRequest(JobParametersDTO jobParameters, long jobPrincipalId, PlatformType type) {
    }
}
