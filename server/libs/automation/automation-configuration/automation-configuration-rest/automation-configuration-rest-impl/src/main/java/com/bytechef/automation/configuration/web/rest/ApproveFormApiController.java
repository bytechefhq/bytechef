/*
 * Copyright 2025 ByteChef
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

package com.bytechef.automation.configuration.web.rest;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.automation.configuration.web.rest.model.ApproveFormModel;
import com.bytechef.platform.workflow.execution.JobResumeId;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * @author Ivica Cardic
 */
@RestController
@RequestMapping("${openapi.openAPIDefinition.base-path.automation:}/internal")
@ConditionalOnCoordinator
public class ApproveFormApiController implements ApproveFormApi {

    private final ConversionService conversionService;
    private final JobService jobService;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI")
    public ApproveFormApiController(
        ConversionService conversionService, JobService jobService, WorkflowService workflowService) {

        this.conversionService = conversionService;
        this.jobService = jobService;
        this.workflowService = workflowService;
    }

    @Override
    public ResponseEntity<ApproveFormModel> getApproveForm(String id) {
        JobResumeId jobResumeId;

        try {
            jobResumeId = JobResumeId.parse(id);
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid id: " + id, exception);
        }

        Job job = jobService.getJob(jobResumeId.getJobId());

        Workflow workflow = workflowService.getWorkflow(job.getWorkflowId());

        WorkflowTask workflowTask = workflow.getTasks()
            .get(job.getCurrentTask());

        Map<String, ?> parameters = workflowTask.getParameters();

        return ResponseEntity.ok(conversionService.convert(parameters, ApproveFormModel.class));
    }
}
