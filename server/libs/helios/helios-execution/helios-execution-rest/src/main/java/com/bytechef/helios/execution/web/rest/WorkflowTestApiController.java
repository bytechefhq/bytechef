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

package com.bytechef.helios.execution.web.rest;

import com.bytechef.atlas.execution.dto.JobParameters;
import com.bytechef.helios.execution.facade.WorkflowExecutionFacade;
import com.bytechef.helios.execution.web.rest.model.TestParametersModel;
import com.bytechef.helios.execution.web.rest.model.WorkflowExecutionModel;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Ivica Cardic
 */
@RestController
@RequestMapping("${openapi.openAPIDefinition.base-path.automation:}")
public class WorkflowTestApiController implements WorkflowTestApi {

    private final ConversionService conversionService;
    private final WorkflowExecutionFacade workflowExecutionFacade;

    public WorkflowTestApiController(
        ConversionService conversionService, WorkflowExecutionFacade workflowExecutionFacade) {

        this.conversionService = conversionService;
        this.workflowExecutionFacade = workflowExecutionFacade;
    }

    @Override
    public ResponseEntity<WorkflowExecutionModel> testWorkflow(TestParametersModel testParametersModel) {
        return ResponseEntity.ok(
            conversionService.convert(
                workflowExecutionFacade.testWorkflow(
                    conversionService.convert(testParametersModel, JobParameters.class)),
                WorkflowExecutionModel.class));
    }
}
