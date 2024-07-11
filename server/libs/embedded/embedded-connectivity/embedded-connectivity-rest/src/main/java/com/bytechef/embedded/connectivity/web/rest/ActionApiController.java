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

package com.bytechef.embedded.connectivity.web.rest;

import com.bytechef.embedded.connectivity.facade.ActionFacade;
import com.bytechef.embedded.connectivity.web.rest.model.ExecuteAction200ResponseModel;
import com.bytechef.embedded.connectivity.web.rest.model.ExecuteActionRequestModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Ivica Cardic
 */
@RestController
@RequestMapping("${openapi.openAPIDefinition.base-path.embedded:}/v1")
public class ActionApiController implements ActionApi {

    private final ActionFacade actionFacade;

    public ActionApiController(ActionFacade actionFacade) {
        this.actionFacade = actionFacade;
    }

    @Override
    public ResponseEntity<ExecuteAction200ResponseModel> executeAction(
        String componentName, Integer componentVersion, String actionName,
        ExecuteActionRequestModel executeActionRequestModel) {

        return ResponseEntity.ok(
            new ExecuteAction200ResponseModel().output(
                actionFacade.executeAction(
                    componentName, componentVersion, actionName, executeActionRequestModel.getInput())));
    }
}
