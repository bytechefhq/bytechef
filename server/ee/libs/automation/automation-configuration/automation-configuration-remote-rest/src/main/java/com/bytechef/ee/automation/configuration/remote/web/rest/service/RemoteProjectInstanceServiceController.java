/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.remote.web.rest.service;

import com.bytechef.automation.configuration.domain.ProjectInstance;
import com.bytechef.automation.configuration.service.ProjectInstanceService;
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
@RequestMapping("/remote/project-instance-service")
public class RemoteProjectInstanceServiceController {

    private final ProjectInstanceService projectInstanceService;

    @SuppressFBWarnings("EI")
    public RemoteProjectInstanceServiceController(ProjectInstanceService projectInstanceService) {
        this.projectInstanceService = projectInstanceService;
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/get-project-instance/{id}",
        produces = {
            "application/json"
        })
    public ResponseEntity<ProjectInstance> getProject(@PathVariable long id) {
        return ResponseEntity.ok(projectInstanceService.getProjectInstance(id));
    }
}
