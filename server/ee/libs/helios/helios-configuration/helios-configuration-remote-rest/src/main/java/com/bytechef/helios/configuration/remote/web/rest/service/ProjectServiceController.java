
/*
 * Copyright 2021 <your company/name>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.helios.configuration.remote.web.rest.service;

import com.bytechef.helios.configuration.domain.Project;
import com.bytechef.helios.configuration.service.ProjectService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author Ivica Cardic
 */
@RestController
@RequestMapping("${openapi.openAPIDefinition.base-path:}/internal")
public class ProjectServiceController {

    private final ProjectService projectService;

    public ProjectServiceController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/project-service/get-project/{id}",
        produces = {
            "application/json"
        })
    public ResponseEntity<Project> getProject(@PathVariable long id) {
        return ResponseEntity.ok(projectService.getProject(id));
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/project-service/get-projects",
        produces = {
            "application/json"
        })
    public ResponseEntity<List<Project>> getProject() {
        return ResponseEntity.ok(projectService.getProjects());
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/project-service/get-workflow-project/{workflowId}",
        produces = {
            "application/json"
        })
    public ResponseEntity<Project> getProject(@PathVariable String workflowId) {
        return ResponseEntity.ok(projectService.getWorkflowProject(workflowId));
    }
}
