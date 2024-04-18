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

import com.bytechef.platform.configuration.facade.WorkflowFacade;
import com.bytechef.platform.configuration.web.rest.model.WorkflowModel;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Ivica Cardic
 */
@RestController
@RequestMapping("${openapi.openAPIDefinition.base-path.platform:}")
public class WorkflowApiController implements WorkflowApi {

    private final ConversionService conversionService;
    private final WorkflowFacade workflowFacade;

    public WorkflowApiController(
        ConversionService conversionService, WorkflowFacade workflowFacade) {

        this.conversionService = conversionService;
        this.workflowFacade = workflowFacade;
    }

    @Override
    public ResponseEntity<WorkflowModel> getWorkflow(String id) {
        return ResponseEntity.ok(
            conversionService.convert(workflowFacade.getWorkflow(id), WorkflowModel.class));
    }

    @Override
    public ResponseEntity<WorkflowModel> updateWorkflow(String id, WorkflowModel workflowModel) {
        return ResponseEntity.ok(
            conversionService.convert(
                workflowFacade.update(id, workflowModel.getDefinition(), workflowModel.getVersion()),
                WorkflowModel.class));
    }
}
