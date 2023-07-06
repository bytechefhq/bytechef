
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

package com.bytechef.hermes.test.web.rest;

import com.bytechef.atlas.execution.dto.JobParameters;
import com.bytechef.hermes.test.WorkflowTestExecutor;
import com.bytechef.hermes.test.web.rest.model.JobModel;
import com.bytechef.hermes.test.web.rest.model.JobParametersModel;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Ivica Cardic
 */
@RestController

@RequestMapping("${openapi.openAPIDefinition.base-path:}/core")
public class WorkflowTestController implements WorkflowTestsApi {

    private final ConversionService conversionService;
    private final WorkflowTestExecutor workflowTestExecutor;

    public WorkflowTestController(ConversionService conversionService, WorkflowTestExecutor workflowTestExecutor) {
        this.conversionService = conversionService;
        this.workflowTestExecutor = workflowTestExecutor;
    }

    @Override
    public ResponseEntity<JobModel> testWorkflow(JobParametersModel jobParametersModel) {
        return ResponseEntity.ok(
            conversionService.convert(
                workflowTestExecutor.execute(conversionService.convert(jobParametersModel, JobParameters.class)),
                JobModel.class));
    }
}
