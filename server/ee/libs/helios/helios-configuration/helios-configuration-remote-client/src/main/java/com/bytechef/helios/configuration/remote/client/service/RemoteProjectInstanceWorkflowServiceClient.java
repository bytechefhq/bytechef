
/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.helios.configuration.remote.client.service;

import com.bytechef.commons.webclient.LoadBalancedWebClient;
import com.bytechef.helios.configuration.domain.ProjectInstanceWorkflow;
import com.bytechef.helios.configuration.domain.ProjectInstanceWorkflowConnection;
import com.bytechef.helios.configuration.service.RemoteProjectInstanceWorkflowService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class RemoteProjectInstanceWorkflowServiceClient implements RemoteProjectInstanceWorkflowService {

    private static final String CONFIGURATION_SERVICE_APP = "configuration-service-app";
    private static final String PROJECT_INSTANCE_WORKFLOW_SERVICE = "/remote/project-instance-workflow-service";
    private final LoadBalancedWebClient loadBalancedWebClient;

    @SuppressFBWarnings("EI")
    public RemoteProjectInstanceWorkflowServiceClient(LoadBalancedWebClient loadBalancedWebClient) {
        this.loadBalancedWebClient = loadBalancedWebClient;
    }

    @Override
    public ProjectInstanceWorkflow getProjectInstanceWorkflow(long projectInstanceId, String workflowId) {
        return loadBalancedWebClient.get(
            uriBuilder -> uriBuilder
                .host(CONFIGURATION_SERVICE_APP)
                .path(
                    PROJECT_INSTANCE_WORKFLOW_SERVICE +
                        "/get-project-instance-workflow/{projectInstanceId}/{workflowId}")
                .build(projectInstanceId, workflowId),
            ProjectInstanceWorkflow.class);
    }

    @Override
    public ProjectInstanceWorkflowConnection getProjectInstanceWorkflowConnection(
        String workflowConnectionOperationName, String workflowConnectionKey) {

        return loadBalancedWebClient.get(
            uriBuilder -> uriBuilder
                .host(CONFIGURATION_SERVICE_APP)
                .path(
                    PROJECT_INSTANCE_WORKFLOW_SERVICE +
                        "/get-project-instance-workflow-connection/{workflowConnectionOperationName}" +
                        "/{workflowConnectionKey}")
                .build(workflowConnectionOperationName, workflowConnectionKey),
            ProjectInstanceWorkflowConnection.class);
    }

    @Override
    @SuppressFBWarnings("NP")
    public long getProjectInstanceWorkflowConnectionId(
        String workflowConnectionOperationName, String workflowConnectionKey) {

        return loadBalancedWebClient.get(
            uriBuilder -> uriBuilder
                .host(CONFIGURATION_SERVICE_APP)
                .path(
                    PROJECT_INSTANCE_WORKFLOW_SERVICE + "/get-project-instance-workflow-connection-id" +
                        "/{operationName}/{key}")
                .build(workflowConnectionOperationName, workflowConnectionKey),
            Long.class);
    }
}
