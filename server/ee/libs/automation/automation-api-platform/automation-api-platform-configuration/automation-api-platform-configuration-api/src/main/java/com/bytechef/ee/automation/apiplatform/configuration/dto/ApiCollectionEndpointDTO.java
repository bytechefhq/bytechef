/*
 * Copyright 2025 ByteChef
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

import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiCollectionEndpoint;
import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiCollectionEndpoint.HttpMethod;
import java.time.Instant;

public record ApiCollectionEndpointDTO(
    long apiCollectionId,
    String createdBy, Instant createdDate, boolean enabled, HttpMethod httpMethod, Long id, String lastModifiedBy,
    Instant lastModifiedDate, String name, String path, long projectDeploymentWorkflowId, int version,
    String workflowUuid) {

    public ApiCollectionEndpointDTO(
        ApiCollectionEndpoint apiCollectionEndpoint, boolean enabled, String workflowUuid) {

        this(
            apiCollectionEndpoint.getApiCollectionId(), apiCollectionEndpoint.getCreatedBy(),
            apiCollectionEndpoint.getCreatedDate(), enabled,
            apiCollectionEndpoint.getHttpMethod(), apiCollectionEndpoint.getId(),
            apiCollectionEndpoint.getLastModifiedBy(), apiCollectionEndpoint.getLastModifiedDate(),
            apiCollectionEndpoint.getName(), apiCollectionEndpoint.getPath(),
            apiCollectionEndpoint.getProjectDeploymentWorkflowId(), apiCollectionEndpoint.getVersion(),
            workflowUuid);
    }

    public ApiCollectionEndpoint toApiCollectionEndpoint() {
        ApiCollectionEndpoint apiCollectionEndpoint = new ApiCollectionEndpoint();

        apiCollectionEndpoint.setApiCollectionId(apiCollectionId);
        apiCollectionEndpoint.setHttpMethod(httpMethod);
        apiCollectionEndpoint.setId(id);
        apiCollectionEndpoint.setName(name);
        apiCollectionEndpoint.setPath(path);

        return apiCollectionEndpoint;
    }
}
