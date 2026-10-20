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

import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflowConnection;
import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiCollectionEndpoint;
import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiCollectionEndpoint.HttpMethod;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressFBWarnings("EI")
public record ApiCollectionEndpointDTO(
    long apiCollectionId, List<ProjectDeploymentWorkflowConnection> connections,
    String createdBy, Instant createdDate, boolean enabled, HttpMethod httpMethod, Long id, Map<String, ?> inputs,
    String lastModifiedBy, Instant lastModifiedDate, String name, String path, long projectDeploymentWorkflowId,
    int version, String workflowUuid) {

    public ApiCollectionEndpointDTO {
        connections = connections == null ? List.of() : List.copyOf(connections);
        inputs = inputs == null ? Map.of() : Collections.unmodifiableMap(new HashMap<>(inputs));
    }

    public ApiCollectionEndpointDTO(
        long apiCollectionId, String createdBy, Instant createdDate, boolean enabled, HttpMethod httpMethod, Long id,
        String lastModifiedBy, Instant lastModifiedDate, String name, String path, long projectDeploymentWorkflowId,
        int version, String workflowUuid) {

        this(
            apiCollectionId, List.of(), createdBy, createdDate, enabled, httpMethod, id, Map.of(), lastModifiedBy,
            lastModifiedDate, name, path, projectDeploymentWorkflowId, version, workflowUuid);
    }

    public ApiCollectionEndpointDTO(
        ApiCollectionEndpoint apiCollectionEndpoint, boolean enabled, String workflowUuid) {

        this(apiCollectionEndpoint, List.of(), enabled, Map.of(), workflowUuid);
    }

    public ApiCollectionEndpointDTO(
        ApiCollectionEndpoint apiCollectionEndpoint, List<ProjectDeploymentWorkflowConnection> connections,
        boolean enabled, Map<String, ?> inputs, String workflowUuid) {

        this(
            apiCollectionEndpoint.getApiCollectionId(), connections, apiCollectionEndpoint.getCreatedBy(),
            apiCollectionEndpoint.getCreatedDate(), enabled, apiCollectionEndpoint.getHttpMethod(),
            apiCollectionEndpoint.getId(), inputs, apiCollectionEndpoint.getLastModifiedBy(),
            apiCollectionEndpoint.getLastModifiedDate(), apiCollectionEndpoint.getName(),
            apiCollectionEndpoint.getPath(), apiCollectionEndpoint.getProjectDeploymentWorkflowId(),
            apiCollectionEndpoint.getVersion(), workflowUuid);
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
