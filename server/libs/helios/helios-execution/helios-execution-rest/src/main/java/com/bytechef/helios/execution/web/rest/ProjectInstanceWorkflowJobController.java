
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

package com.bytechef.helios.execution.web.rest;

import com.bytechef.helios.configuration.web.rest.ProjectInstanceWorkflowJobsApi;
import com.bytechef.helios.configuration.web.rest.model.CreateProjectInstanceWorkflowJob200ResponseModel;
import com.bytechef.helios.execution.facade.ProjectInstanceWorkflowJobFacade;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Ivica Cardic
 */
@RestController
@RequestMapping("${openapi.openAPIDefinition.base-path:}/automation")
public class ProjectInstanceWorkflowJobController implements ProjectInstanceWorkflowJobsApi {

    private final ProjectInstanceWorkflowJobFacade projectInstanceWorkflowJobFacade;

    public ProjectInstanceWorkflowJobController(ProjectInstanceWorkflowJobFacade projectInstanceWorkflowJobFacade) {
        this.projectInstanceWorkflowJobFacade = projectInstanceWorkflowJobFacade;
    }

    @Override
    public ResponseEntity<CreateProjectInstanceWorkflowJob200ResponseModel> createProjectInstanceWorkflowJob(
        Long id, String workflowId) {

        return ResponseEntity.ok(
            new CreateProjectInstanceWorkflowJob200ResponseModel()
                .jobId(projectInstanceWorkflowJobFacade.createJob(id, workflowId)));
    }
}
