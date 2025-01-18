/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.remote.web.rest.service;

import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
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
@RequestMapping("/remote/project/deployment-service")
public class RemoteProjectDeploymentServiceController {

    private final ProjectDeploymentService projectDeploymentService;

    @SuppressFBWarnings("EI")
    public RemoteProjectDeploymentServiceController(ProjectDeploymentService projectDeploymentService) {
        this.projectDeploymentService = projectDeploymentService;
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/get-project/deployment/{id}",
        produces = {
            "application/json"
        })
    public ResponseEntity<ProjectDeployment> getProject(@PathVariable long id) {
        return ResponseEntity.ok(projectDeploymentService.getProjectDeployment(id));
    }
}
