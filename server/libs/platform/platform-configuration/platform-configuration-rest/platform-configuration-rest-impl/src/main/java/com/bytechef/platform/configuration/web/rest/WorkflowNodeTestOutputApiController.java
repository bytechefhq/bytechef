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

package com.bytechef.platform.configuration.web.rest;

import com.bytechef.platform.annotation.ConditionalOnEndpoint;
import com.bytechef.platform.configuration.facade.WorkflowNodeTestOutputFacade;
import com.bytechef.platform.configuration.service.WorkflowNodeTestOutputService;
import com.bytechef.platform.configuration.web.rest.model.WorkflowNodeTestOutputModel;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Ivica Cardic
 */
@RestController
@RequestMapping("${openapi.openAPIDefinition.base-path.platform:}")
@ConditionalOnEndpoint
public class WorkflowNodeTestOutputApiController implements WorkflowNodeTestOutputApi {

    private final ConversionService conversionService;
    private final WorkflowNodeTestOutputFacade workflowNodeTestOutputFacade;
    private final WorkflowNodeTestOutputService workflowNodeTestOutputService;

    @SuppressFBWarnings("EI")
    public WorkflowNodeTestOutputApiController(
        ConversionService conversionService, WorkflowNodeTestOutputFacade workflowNodeTestOutputFacade,
        WorkflowNodeTestOutputService workflowNodeTestOutputService) {

        this.conversionService = conversionService;
        this.workflowNodeTestOutputFacade = workflowNodeTestOutputFacade;
        this.workflowNodeTestOutputService = workflowNodeTestOutputService;
    }

    @Override
    public ResponseEntity<Void> deleteWorkflowNodeTestOutput(String workflowId, String workflowNodeName) {
        workflowNodeTestOutputService.deleteWorkflowNodeTestOutput(workflowId, workflowNodeName);

        return ResponseEntity
            .noContent()
            .build();
    }

    @Override
    public ResponseEntity<WorkflowNodeTestOutputModel> saveWorkflowNodeTestOutput(
        String workflowId, String workflowNodeName) {

        return ResponseEntity.ok(
            conversionService.convert(
                workflowNodeTestOutputFacade.saveWorkflowNodeTestOutput(workflowId, workflowNodeName),
                WorkflowNodeTestOutputModel.class));
    }

    @Override
    public ResponseEntity<WorkflowNodeTestOutputModel> uploadWorkflowNodeSampleOutput(
        String workflowId, String workflowNodeName, Object body) {

        return ResponseEntity.ok(
            conversionService.convert(
                workflowNodeTestOutputFacade.saveWorkflowNodeTestOutput(
                    workflowId, workflowNodeName, body),
                WorkflowNodeTestOutputModel.class));
    }
}
