
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

package com.bytechef.helios.execution.remote.web.rest.job;

import com.bytechef.helios.execution.job.ProjectInstanceWorkflowJobFactory;
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
public class ProjectInstanceWorkflowJobFactoryController {

    private final ProjectInstanceWorkflowJobFactory projectInstanceWorkflowJobFactory;

    public ProjectInstanceWorkflowJobFactoryController(
        ProjectInstanceWorkflowJobFactory projectInstanceWorkflowJobFactory) {

        this.projectInstanceWorkflowJobFactory = projectInstanceWorkflowJobFactory;
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/project-instance-workflow-job-factory/create-job/{instanceId}/{workflowId}",
        produces = {
            "application/json"
        })
    public ResponseEntity<Long> createJob(
        @PathVariable long instanceId, @PathVariable String workflowId) {

        return ResponseEntity.ok(projectInstanceWorkflowJobFactory.createJob(instanceId, workflowId));
    }
}
