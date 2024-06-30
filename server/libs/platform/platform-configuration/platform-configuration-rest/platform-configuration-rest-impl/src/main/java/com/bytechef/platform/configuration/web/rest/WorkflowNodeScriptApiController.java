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
import com.bytechef.platform.configuration.facade.WorkflowNodeScriptFacade;
import com.bytechef.platform.configuration.web.rest.model.ScriptTestExecutionModel;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Ivica Cardic
 */
@RestController
@RequestMapping("${openapi.openAPIDefinition.base-path.platform:}/internal")
@ConditionalOnEndpoint
public class WorkflowNodeScriptApiController implements WorkflowNodeScriptApi {

    private final ConversionService conversionService;
    private final WorkflowNodeScriptFacade workflowNodeScriptFacade;

    public WorkflowNodeScriptApiController(
        ConversionService conversionService, WorkflowNodeScriptFacade workflowNodeScriptFacade) {

        this.conversionService = conversionService;
        this.workflowNodeScriptFacade = workflowNodeScriptFacade;
    }

    @Override
    public ResponseEntity<ScriptTestExecutionModel> testWorkflowNodeScript(String id, String workflowNodeName) {
        return ResponseEntity.ok(
            conversionService.convert(
                workflowNodeScriptFacade.testWorkflowNodeScript(id, workflowNodeName),
                ScriptTestExecutionModel.class));
    }
}
