/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.embedded.workflow.execution.web.rest;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.atlas.execution.domain.Job.Status;
import com.bytechef.embedded.configuration.web.rest.model.EnvironmentModel;
import com.bytechef.embedded.workflow.execution.facade.WorkflowExecutionFacade;
import com.bytechef.embedded.workflow.execution.web.rest.model.WorkflowExecutionBasicModel;
import com.bytechef.embedded.workflow.execution.web.rest.model.WorkflowExecutionModel;
import com.bytechef.platform.constant.Environment;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.LocalDateTime;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Ivica Cardic
 */
@RestController("com.bytechef.embedded.workflow.execution.web.rest.WorkflowExecutionApiController")
@RequestMapping("${openapi.openAPIDefinition.base-path.embedded:}/internal")
@ConditionalOnCoordinator
public class WorkflowExecutionApiController implements WorkflowExecutionApi {

    private final ConversionService conversionService;
    private final WorkflowExecutionFacade workflowExecutionFacade;

    @SuppressFBWarnings("EI")
    public WorkflowExecutionApiController(
        ConversionService conversionService, WorkflowExecutionFacade workflowExecutionFacade) {

        this.conversionService = conversionService;
        this.workflowExecutionFacade = workflowExecutionFacade;
    }

    @Override
    public ResponseEntity<WorkflowExecutionModel> getWorkflowExecution(Long id) {
        return ResponseEntity.ok(
            conversionService.convert(
                workflowExecutionFacade.getWorkflowExecution(id), WorkflowExecutionModel.class));
    }

    @Override
    public ResponseEntity<Page> getWorkflowExecutionsPage(
        EnvironmentModel environment, String jobStatus, LocalDateTime jobStartDate, LocalDateTime jobEndDate,
        Long projectId,
        Long integrationInstanceConfigurationId, String workflowId, Integer pageNumber) {

        return ResponseEntity.ok(
            workflowExecutionFacade
                .getWorkflowExecutions(
                    environment == null ? null : Environment.valueOf(environment.name()),
                    jobStatus == null ? null : Status.valueOf(jobStatus), jobStartDate, jobEndDate, projectId,
                    integrationInstanceConfigurationId, workflowId, pageNumber)
                .map(workflowExecutionDTO -> conversionService.convert(
                    workflowExecutionDTO, WorkflowExecutionBasicModel.class)));
    }
}
