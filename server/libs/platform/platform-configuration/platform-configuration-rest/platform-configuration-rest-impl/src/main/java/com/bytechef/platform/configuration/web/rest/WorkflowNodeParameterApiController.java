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
import com.bytechef.platform.configuration.facade.WorkflowNodeParameterFacade;
import com.bytechef.platform.configuration.web.rest.model.GetWorkflowNodeParameterDisplayConditions200ResponseModel;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Ivica Cardic
 */
@RestController
@RequestMapping("${openapi.openAPIDefinition.base-path.platform:}")
@ConditionalOnEndpoint
public class WorkflowNodeParameterApiController implements WorkflowNodeParameterApi {

    private final WorkflowNodeParameterFacade workflowNodeParameterFacade;

    @SuppressFBWarnings("EI")
    public WorkflowNodeParameterApiController(WorkflowNodeParameterFacade workflowNodeParameterFacade) {
        this.workflowNodeParameterFacade = workflowNodeParameterFacade;
    }

    @Override
    public ResponseEntity<GetWorkflowNodeParameterDisplayConditions200ResponseModel>
        getWorkflowNodeParameterDisplayConditions(String id, String workflowNodeName) {

        return ResponseEntity.ok(
            new GetWorkflowNodeParameterDisplayConditions200ResponseModel()
                .displayConditions(
                    workflowNodeParameterFacade.getDisplayConditions(id, workflowNodeName)));
    }
}
