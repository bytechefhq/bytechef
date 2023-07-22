
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

import com.bytechef.helios.execution.facade.ExecutionFacade;
import com.bytechef.helios.execution.web.rest.model.ExecutionBasicModel;
import com.bytechef.helios.execution.web.rest.model.ExecutionModel;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * @author Ivica Cardic
 */
@RestController
@RequestMapping("${openapi.openAPIDefinition.base-path:}/automation")
public class ProjectExecutionController implements ExecutionsApi {

    private final ConversionService conversionService;
    private final ExecutionFacade executionFacade;

    @SuppressFBWarnings("EI")
    public ProjectExecutionController(
        ConversionService conversionService, ExecutionFacade executionFacade) {

        this.conversionService = conversionService;
        this.executionFacade = executionFacade;
    }

    @Override
    @SuppressFBWarnings("NP")
    public ResponseEntity<ExecutionModel> getExecution(Long id) {
        return ResponseEntity.ok(
            conversionService.convert(executionFacade.getExecution(id),
                ExecutionModel.class));
    }

    @Override
    public ResponseEntity<Page> getExecutions(
        String jobStatus, LocalDateTime jobStartDate, LocalDateTime jobEndDate, Long projectId, Long projectInstanceId,
        String workflowId, Integer pageNumber) {

        return ResponseEntity.ok(
            executionFacade
                .getExecutions(
                    jobStatus, jobStartDate, jobEndDate, projectId, projectInstanceId, workflowId, pageNumber)
                .map(workflowExecutionDTO -> conversionService.convert(
                    workflowExecutionDTO, ExecutionBasicModel.class)));
    }
}
