/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.hermes.execution.remote.web.rest.facade;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.dto.JobParameters;
import com.bytechef.hermes.execution.facade.InstanceJobFacade;
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
@RequestMapping("/remote/instance-job-facade")
public class RemoteInstanceJobFacadeController {

    private final InstanceJobFacade instanceJobFacade;

    @SuppressFBWarnings("EI")
    public RemoteInstanceJobFacadeController(InstanceJobFacade instanceJobFacade) {
        this.instanceJobFacade = instanceJobFacade;
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/create-job")
    public ResponseEntity<Job> create(@Valid @RequestBody CreateJobRequest createJobRequest) {
        Job job;

        if (createJobRequest.workflow == null) {
            job = instanceJobFacade.createJob(
                createJobRequest.jobParameters, createJobRequest.instanceId, createJobRequest.type);
        } else {
            job = instanceJobFacade.createJob(
                createJobRequest.jobParameters, createJobRequest.workflow, createJobRequest.instanceId,
                createJobRequest.type);
        }

        return ResponseEntity.ok(job);
    }

    @SuppressFBWarnings("EI")
    public record CreateJobRequest(JobParameters jobParameters, Workflow workflow, long instanceId, int type) {
    }
}
