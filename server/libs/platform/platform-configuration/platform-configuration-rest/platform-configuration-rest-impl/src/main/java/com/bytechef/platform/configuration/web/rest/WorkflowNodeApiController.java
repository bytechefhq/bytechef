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

import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.platform.annotation.ConditionalOnEndpoint;
import com.bytechef.platform.configuration.facade.WorkflowNodeDescriptionFacade;
import com.bytechef.platform.configuration.facade.WorkflowNodeDisplayConditionFacade;
import com.bytechef.platform.configuration.facade.WorkflowNodeDynamicPropertiesFacade;
import com.bytechef.platform.configuration.facade.WorkflowNodeOptionFacade;
import com.bytechef.platform.configuration.facade.WorkflowNodeOutputFacade;
import com.bytechef.platform.configuration.web.rest.model.EvaluateWorkflowNodeDisplayConditionRequestModel;
import com.bytechef.platform.configuration.web.rest.model.GetWorkflowNodeDescription200ResponseModel;
import com.bytechef.platform.configuration.web.rest.model.OptionModel;
import com.bytechef.platform.configuration.web.rest.model.PropertyModel;
import com.bytechef.platform.configuration.web.rest.model.WorkflowNodeOutputModel;
import java.util.List;
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
public class WorkflowNodeApiController implements WorkflowNodeApi {

    private final ConversionService conversionService;
    private final WorkflowNodeDisplayConditionFacade workflowNodeDisplayConditionFacade;
    private final WorkflowNodeDescriptionFacade workflowNodeDescriptionFacade;
    private final WorkflowNodeDynamicPropertiesFacade workflowNodeDynamicPropertiesFacade;
    private final WorkflowNodeOptionFacade workflowNodeOptionFacade;
    private final WorkflowNodeOutputFacade workflowNodeOutputFacade;

    public WorkflowNodeApiController(
        ConversionService conversionService, WorkflowNodeDisplayConditionFacade workflowNodeDisplayConditionFacade,
        WorkflowNodeDescriptionFacade workflowNodeDescriptionFacade,
        WorkflowNodeDynamicPropertiesFacade workflowNodeDynamicPropertiesFacade,
        WorkflowNodeOptionFacade workflowNodeOptionFacade, WorkflowNodeOutputFacade workflowNodeOutputFacade) {

        this.conversionService = conversionService;
        this.workflowNodeDisplayConditionFacade = workflowNodeDisplayConditionFacade;
        this.workflowNodeDescriptionFacade = workflowNodeDescriptionFacade;
        this.workflowNodeDynamicPropertiesFacade = workflowNodeDynamicPropertiesFacade;
        this.workflowNodeOptionFacade = workflowNodeOptionFacade;
        this.workflowNodeOutputFacade = workflowNodeOutputFacade;
    }

    @Override
    public ResponseEntity<Boolean> evaluateWorkflowNodeDisplayCondition(
        String id, String workflowNodeName,
        EvaluateWorkflowNodeDisplayConditionRequestModel evaluateWorkflowNodeDisplayConditionRequestModel) {

        return ResponseEntity.ok(
            workflowNodeDisplayConditionFacade.evaluateWorkflowNodeDisplayCondition(id, workflowNodeName,
                evaluateWorkflowNodeDisplayConditionRequestModel.getDisplayCondition()));
    }

    @Override
    public ResponseEntity<GetWorkflowNodeDescription200ResponseModel> getWorkflowNodeDescription(
        String workflowId, String workflowNodeName) {

        return ResponseEntity.ok(
            new GetWorkflowNodeDescription200ResponseModel().description(
                workflowNodeDescriptionFacade.getWorkflowNodeDescription(workflowId, workflowNodeName)));
    }

    @Override
    public ResponseEntity<List<OptionModel>> getWorkflowNodeOptions(
        String workflowId, String workflowNodeName, String propertyName, String searchText) {

        return ResponseEntity.ok(
            CollectionUtils.map(
                workflowNodeOptionFacade.getWorkflowNodeOptions(workflowId, workflowNodeName, propertyName, searchText),
                option -> conversionService.convert(option, OptionModel.class)));
    }

    @Override
    public ResponseEntity<WorkflowNodeOutputModel> getWorkflowNodeOutput(String workflowId, String workflowNodeName) {
        return ResponseEntity.ok(
            conversionService.convert(
                workflowNodeOutputFacade.getWorkflowNodeOutput(workflowId, workflowNodeName),
                WorkflowNodeOutputModel.class));
    }

    @Override
    public ResponseEntity<List<WorkflowNodeOutputModel>> getWorkflowNodeOutputs(
        String workflowId, String lastWorkflowNodeName) {

        return ResponseEntity.ok(
            CollectionUtils.map(
                workflowNodeOutputFacade.getWorkflowNodeOutputs(workflowId, lastWorkflowNodeName),
                workflowNodeOutputDTO -> conversionService.convert(workflowNodeOutputDTO,
                    WorkflowNodeOutputModel.class)));
    }

    @Override
    public ResponseEntity<List<PropertyModel>> getWorkflowNodeDynamicProperties(
        String workflowId, String workflowNodeName, String propertyName) {

        return ResponseEntity.ok(
            CollectionUtils.map(
                workflowNodeDynamicPropertiesFacade.getWorkflowNodeDynamicProperties(
                    workflowId, workflowNodeName, propertyName),
                property -> conversionService.convert(property, PropertyModel.class)));
    }
}
