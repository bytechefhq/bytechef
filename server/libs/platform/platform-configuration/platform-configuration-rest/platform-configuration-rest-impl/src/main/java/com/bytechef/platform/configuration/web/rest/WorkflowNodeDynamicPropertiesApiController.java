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
import com.bytechef.platform.configuration.facade.WorkflowNodeDynamicPropertiesFacade;
import com.bytechef.platform.configuration.web.rest.model.PropertyModel;
import java.util.List;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Ivica Cardic
 */
@RestController
@RequestMapping("${openapi.openAPIDefinition.base-path.platform:}/internal")
public class WorkflowNodeDynamicPropertiesApiController implements WorkflowNodeDynamicPropertiesApi {

    private final ConversionService conversionService;
    private final WorkflowNodeDynamicPropertiesFacade workflowNodeDynamicPropertiesFacade;

    public WorkflowNodeDynamicPropertiesApiController(
        ConversionService conversionService, WorkflowNodeDynamicPropertiesFacade workflowNodeDynamicPropertiesFacade) {

        this.conversionService = conversionService;
        this.workflowNodeDynamicPropertiesFacade = workflowNodeDynamicPropertiesFacade;
    }

    @Override
    public ResponseEntity<List<PropertyModel>> getWorkflowNodeDynamicProperties(
        String workflowId, String workflowNodeName, String propertyName, List<String> lookupDependsOnPaths) {

        return ResponseEntity.ok(
            CollectionUtils.map(
                workflowNodeDynamicPropertiesFacade.getWorkflowNodeDynamicProperties(
                    workflowId, workflowNodeName, propertyName,
                    lookupDependsOnPaths == null ? List.of() : lookupDependsOnPaths),
                property -> conversionService.convert(property, PropertyModel.class)));
    }
}
