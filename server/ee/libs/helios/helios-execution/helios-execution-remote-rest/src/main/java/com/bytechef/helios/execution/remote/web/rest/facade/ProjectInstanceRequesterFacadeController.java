
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

package com.bytechef.helios.execution.remote.web.rest.facade;

import com.bytechef.helios.execution.facade.ProjectInstanceRequesterFacade;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Ivica Cardic
 */
@RestController
@RequestMapping("${openapi.openAPIDefinition.base-path:}/internal")
public class ProjectInstanceRequesterFacadeController {

    private final ProjectInstanceRequesterFacade projectInstanceRequesterFacade;

    public ProjectInstanceRequesterFacadeController(
        ProjectInstanceRequesterFacade projectInstanceRequesterFacade) {

        this.projectInstanceRequesterFacade = projectInstanceRequesterFacade;
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/project-instance-workflow-facade/create-job/{projectInstanceId}/{workflowId}",
        produces = {
            "application/json"
        })
    public ResponseEntity<Long> createJob(
        @PathVariable long projectInstanceId, @PathVariable String workflowId) {

        return ResponseEntity.ok(projectInstanceRequesterFacade.createJob(projectInstanceId, workflowId));
    }
}
