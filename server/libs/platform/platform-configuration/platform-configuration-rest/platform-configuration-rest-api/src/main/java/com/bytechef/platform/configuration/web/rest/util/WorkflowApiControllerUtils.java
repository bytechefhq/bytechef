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

package com.bytechef.platform.configuration.web.rest.util;

import com.bytechef.platform.configuration.facade.WorkflowFacade;
import com.bytechef.platform.configuration.web.rest.model.WorkflowModel;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.ResponseEntity;

/**
 * @author Ivica Cardic
 */
public class WorkflowApiControllerUtils {

    public static ResponseEntity<WorkflowModel> getWorkflow(
        String id, ConversionService conversionService, WorkflowFacade workflowFacade) {

        return ResponseEntity.ok(conversionService.convert(workflowFacade.getWorkflow(id), WorkflowModel.class));
    }
}
