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

package com.bytechef.automation.workflow.execution.web.rest;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.atlas.execution.domain.Job.Status;
import com.bytechef.automation.workflow.execution.facade.ProjectWorkflowExecutionFacade;
import com.bytechef.automation.workflow.execution.web.rest.model.WorkflowExecutionBasicModel;
import com.bytechef.automation.workflow.execution.web.rest.model.WorkflowExecutionModel;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.OffsetDateTime;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Ivica Cardic
 */
@RestController("com.bytechef.automation.workflow.execution.web.rest.WorkflowExecutionApiController")
@RequestMapping("${openapi.openAPIDefinition.base-path.automation:}/internal")
@ConditionalOnCoordinator
public class WorkflowExecutionApiController implements WorkflowExecutionApi {

    private final ConversionService conversionService;
    private final ProjectWorkflowExecutionFacade projectWorkflowExecutionFacade;

    @SuppressFBWarnings("EI")
    public WorkflowExecutionApiController(
        ConversionService conversionService, ProjectWorkflowExecutionFacade projectWorkflowExecutionFacade) {

        this.conversionService = conversionService;
        this.projectWorkflowExecutionFacade = projectWorkflowExecutionFacade;
    }

    @Override
    public ResponseEntity<WorkflowExecutionModel> getWorkflowExecution(Long id) {
        return ResponseEntity.ok(
            conversionService.convert(
                projectWorkflowExecutionFacade.getWorkflowExecution(id), WorkflowExecutionModel.class));
    }

    @Override
    public ResponseEntity<Page> getWorkflowExecutionsPage(
        Long workspaceId, Boolean embedded, Long environmentId, String jobStatus, OffsetDateTime jobStartDate,
        OffsetDateTime jobEndDate, Long projectId, Long projectDeploymentId, String workflowId, Integer pageNumber) {

        return ResponseEntity.ok(
            projectWorkflowExecutionFacade
                .getWorkflowExecutions(
                    embedded, environmentId, jobStatus == null ? null : Status.valueOf(jobStatus),
                    jobStartDate == null ? null : jobStartDate.toInstant(),
                    jobEndDate == null ? null : jobEndDate.toInstant(), projectId, projectDeploymentId,
                    workflowId, workspaceId, pageNumber)
                .map(workflowExecutionDTO -> conversionService.convert(
                    workflowExecutionDTO, WorkflowExecutionBasicModel.class)));
    }
}
