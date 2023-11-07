/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.hermes.execution.remote.web.rest.service;

import com.bytechef.hermes.execution.service.InstanceJobService;
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
@RequestMapping("/remote/instance-job-service")
public class RemoteInstanceJobServiceController {

    private final InstanceJobService instanceJobService;

    public RemoteInstanceJobServiceController(InstanceJobService instanceJobService) {
        this.instanceJobService = instanceJobService;
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
    public ResponseEntity<Long> getTriggerExecution(@PathVariable long jobId, @PathVariable int type) {
        return instanceJobService.fetchJobInstanceId(jobId, type)
            .map(ResponseEntity::ok)
            .orElse(
                ResponseEntity.noContent()
                    .build());
    }
}
