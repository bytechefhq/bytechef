/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.remote.web.rest.service;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.service.ProjectService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.Hidden;
import java.util.List;
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
@RequestMapping("/remote/project-service")
public class RemoteProjectServiceController {

    private final ProjectService projectService;

    @SuppressFBWarnings("EI")
    public RemoteProjectServiceController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/get-project/{id}",
        produces = {
            "application/json"
        })
    public ResponseEntity<Project> getProject(@PathVariable long id) {
        return ResponseEntity.ok(projectService.getProject(id));
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/get-project-instance-project/{projectInstanceId}",
        produces = {
            "application/json"
        })
    public ResponseEntity<Project> getProjectInstanceProject(@PathVariable long projectInstanceId) {
        return ResponseEntity.ok(projectService.getProjectInstanceProject(projectInstanceId));
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/get-workflow-project/{workflowId}",
        produces = {
            "application/json"
        })
    public ResponseEntity<Project> getWorkflowProject(@PathVariable String workflowId) {
        return ResponseEntity.ok(projectService.getWorkflowProject(workflowId));
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/get-projects",
        produces = {
            "application/json"
        })
    public ResponseEntity<List<Project>> getProjects() {
        return ResponseEntity.ok(projectService.getProjects());
    }
}
