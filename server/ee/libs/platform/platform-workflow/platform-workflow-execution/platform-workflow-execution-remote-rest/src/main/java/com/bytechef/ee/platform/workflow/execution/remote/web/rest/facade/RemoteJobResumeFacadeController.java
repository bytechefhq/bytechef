/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.workflow.execution.remote.web.rest.facade;

import com.bytechef.platform.workflow.execution.facade.JobResumeFacade;
import com.bytechef.platform.workflow.execution.facade.JobResumeFacade.JobResumeOutcome;
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
@RequestMapping("/remote/job-resume-facade")
public class RemoteJobResumeFacadeController {

    private final JobResumeFacade jobResumeFacade;

    @SuppressFBWarnings("EI")
    public RemoteJobResumeFacadeController(JobResumeFacade jobResumeFacade) {
        this.jobResumeFacade = jobResumeFacade;
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/resume-expired-job")
    public ResponseEntity<JobResumeOutcome> resumeExpiredJob(
        @Valid @RequestBody ResumeExpiredJobRequest resumeExpiredJobRequest) {

        return ResponseEntity.ok(jobResumeFacade.resumeExpiredJob(resumeExpiredJobRequest.id()));
    }

    public record ResumeExpiredJobRequest(String id) {
    }
}
