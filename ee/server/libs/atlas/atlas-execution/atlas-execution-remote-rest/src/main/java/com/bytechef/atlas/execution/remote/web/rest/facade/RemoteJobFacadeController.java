/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.atlas.execution.remote.web.rest.facade;

import com.bytechef.atlas.execution.dto.JobParameters;
import com.bytechef.atlas.execution.facade.JobFacade;
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
@RequestMapping("/remote/job-facade")
public class RemoteJobFacadeController {

    private final JobFacade jobFacade;

    @SuppressFBWarnings("EI")
    public RemoteJobFacadeController(JobFacade jobFacade) {
        this.jobFacade = jobFacade;
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/create-async-job")
    public ResponseEntity<Long> create(@Valid @RequestBody JobParameters jobParameters) {
        return ResponseEntity.ok(jobFacade.createAsyncJob(jobParameters));
    }
}
