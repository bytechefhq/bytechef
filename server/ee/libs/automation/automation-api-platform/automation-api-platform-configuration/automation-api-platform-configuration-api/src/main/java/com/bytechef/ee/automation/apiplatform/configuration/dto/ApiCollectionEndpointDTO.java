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

package com.bytechef.ee.automation.apiplatform.configuration.dto;

import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiCollectionEndpoint;
import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiCollectionEndpoint.HttpMethod;
import java.time.Instant;

public record ApiCollectionEndpointDTO(
    long apiCollectionId,
    String createdBy, Instant createdDate, boolean enabled, HttpMethod httpMethod, Long id, String lastModifiedBy,
    Instant lastModifiedDate, String name, String path, long projectDeploymentWorkflowId, int version,
    String workflowReferenceCode) {

    public ApiCollectionEndpointDTO(
        ApiCollectionEndpoint apiCollectionEndpoint, ProjectDeploymentWorkflow projectDeploymentWorkflow) {

        this(
            apiCollectionEndpoint.getApiCollectionId(), apiCollectionEndpoint.getCreatedBy(),
            apiCollectionEndpoint.getCreatedDate(), projectDeploymentWorkflow.isEnabled(),
            apiCollectionEndpoint.getHttpMethod(), apiCollectionEndpoint.getId(),
            apiCollectionEndpoint.getLastModifiedBy(), apiCollectionEndpoint.getLastModifiedDate(),
            apiCollectionEndpoint.getName(), apiCollectionEndpoint.getPath(),
            apiCollectionEndpoint.getProjectDeploymentWorkflowId(), apiCollectionEndpoint.getVersion(),
            apiCollectionEndpoint.getWorkflowReferenceCode());
    }

    public ApiCollectionEndpoint toApiCollectionEndpoint() {
        ApiCollectionEndpoint apiCollectionEndpoint = new ApiCollectionEndpoint();

        apiCollectionEndpoint.setApiCollectionId(apiCollectionId);
        apiCollectionEndpoint.setHttpMethod(httpMethod);
        apiCollectionEndpoint.setId(id);
        apiCollectionEndpoint.setName(name);
        apiCollectionEndpoint.setPath(path);
        apiCollectionEndpoint.setWorkflowReferenceCode(workflowReferenceCode);

        return apiCollectionEndpoint;
    }
}
