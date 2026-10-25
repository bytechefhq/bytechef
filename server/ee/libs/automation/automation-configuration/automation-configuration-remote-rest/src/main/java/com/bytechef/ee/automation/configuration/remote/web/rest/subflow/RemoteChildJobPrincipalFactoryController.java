/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.remote.web.rest.subflow;

import com.bytechef.atlas.execution.dto.JobParametersDTO;
import com.bytechef.platform.workflow.task.dispatcher.subflow.ChildJobPrincipalFactory;
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
@RequestMapping("/remote/child-job-principal-factory")
public class RemoteChildJobPrincipalFactoryController {

    private final ChildJobPrincipalFactory childJobPrincipalFactory;

    @SuppressFBWarnings("EI")
    public RemoteChildJobPrincipalFactoryController(ChildJobPrincipalFactory childJobPrincipalFactory) {
        this.childJobPrincipalFactory = childJobPrincipalFactory;
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/create-principal-linked-job")
    public ResponseEntity<Long> createPrincipalLinkedJob(
        @Valid @RequestBody CreatePrincipalLinkedJobRequest createPrincipalLinkedJobRequest) {

        return ResponseEntity.ok(
            childJobPrincipalFactory.createPrincipalLinkedJob(
                createPrincipalLinkedJobRequest.referenceJobId, createPrincipalLinkedJobRequest.jobParameters));
    }

    @SuppressFBWarnings("EI")
    public record CreatePrincipalLinkedJobRequest(long referenceJobId, JobParametersDTO jobParameters) {
    }
}
