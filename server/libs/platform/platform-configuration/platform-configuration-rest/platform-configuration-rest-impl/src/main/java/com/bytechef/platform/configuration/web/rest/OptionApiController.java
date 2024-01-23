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

import com.bytechef.platform.configuration.web.rest.model.OptionModel;
import java.util.List;
import org.springframework.http.ResponseEntity;

public class OptionApiController implements OptionApi {

    @Override
    public ResponseEntity<List<OptionModel>> getWorkflowOperationOptions(
        String workflowId, String workflowNodeName, String propertyName, String searchText) {
        return OptionApi.super.getWorkflowOperationOptions(workflowId, workflowNodeName, propertyName, searchText);
    }
}
