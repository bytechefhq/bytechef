/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.workflow.execution.remote.web.rest.service;

import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
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
@RequestMapping("/remote/principal-job-service")
public class RemotePrincipalJobServiceController {

    private final PrincipalJobService principalJobService;

    @SuppressFBWarnings("EI")
    public RemotePrincipalJobServiceController(PrincipalJobService principalJobService) {
        this.principalJobService = principalJobService;
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/fetch-last-job-id/{jobPrincipalId}/{type}",
        consumes = {
            "application/json"
        },
        produces = {
            "application/json"
        })
    public ResponseEntity<Long> fetchLastJobId(@PathVariable long jobPrincipalId, @PathVariable PlatformType type) {
        return principalJobService.fetchLastJobId(jobPrincipalId, type)
            .map(ResponseEntity::ok)
            .orElse(
                ResponseEntity.noContent()
                    .build());
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/fetch-job-instance-id/{jobId}/{type}",
        consumes = {
            "application/json"
        },
        produces = {
            "application/json"
        })
    public ResponseEntity<Long> fetchJobInstanceId(@PathVariable long jobId, @PathVariable PlatformType type) {
        return principalJobService.fetchJobPrincipalId(jobId, type)
            .map(ResponseEntity::ok)
            .orElse(
                ResponseEntity.noContent()
                    .build());
    }
}
