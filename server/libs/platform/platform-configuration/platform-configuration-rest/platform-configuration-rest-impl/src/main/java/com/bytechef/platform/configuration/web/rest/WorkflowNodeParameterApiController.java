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
import com.bytechef.platform.configuration.web.rest.model.DeleteWorkflowNodeParameter200ResponseModel;
import com.bytechef.platform.configuration.web.rest.model.DeleteWorkflowNodeParameterRequestModel;
import com.bytechef.platform.configuration.web.rest.model.UpdateWorkflowNodeParameterRequestModel;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
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
    @SuppressWarnings("unchecked")
    public ResponseEntity<DeleteWorkflowNodeParameter200ResponseModel> deleteWorkflowNodeParameter(
        String id, DeleteWorkflowNodeParameterRequestModel deleteWorkflowNodeParameterRequestModel) {

        return ResponseEntity.ok(
            new DeleteWorkflowNodeParameter200ResponseModel().parameters(
                (Map<String, Object>) workflowNodeParameterFacade.deleteParameter(
                    id, deleteWorkflowNodeParameterRequestModel.getWorkflowNodeName(),
                    deleteWorkflowNodeParameterRequestModel.getPath(),
                    deleteWorkflowNodeParameterRequestModel.getName(),
                    deleteWorkflowNodeParameterRequestModel.getArrayIndex())));
    }

    @Override
    @SuppressWarnings("unchecked")
    public ResponseEntity<DeleteWorkflowNodeParameter200ResponseModel> updateWorkflowNodeParameter(
        String id, UpdateWorkflowNodeParameterRequestModel updateWorkflowNodeParameterRequestModel) {

        return ResponseEntity.ok(
            new DeleteWorkflowNodeParameter200ResponseModel().parameters(
                (Map<String, Object>) workflowNodeParameterFacade.updateParameter(
                    id, updateWorkflowNodeParameterRequestModel.getWorkflowNodeName(),
                    updateWorkflowNodeParameterRequestModel.getPath(),
                    updateWorkflowNodeParameterRequestModel.getName(),
                    updateWorkflowNodeParameterRequestModel.getArrayIndex(),
                    updateWorkflowNodeParameterRequestModel.getValue())));
    }
}
