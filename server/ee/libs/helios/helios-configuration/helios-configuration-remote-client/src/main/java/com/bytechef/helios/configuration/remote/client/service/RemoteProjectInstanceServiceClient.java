
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
import com.bytechef.helios.configuration.domain.ProjectInstance;
import com.bytechef.helios.configuration.service.RemoteProjectInstanceService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * @author Ivica Cardic
 */
@Component
public class RemoteProjectInstanceServiceClient implements RemoteProjectInstanceService {

    private static final String CONFIGURATION_APP = "configuration-app";
    private static final String PROJECT_INSTANCE_SERVICE = "/remote/project-instance-service";
    private final LoadBalancedWebClient loadBalancedWebClient;

    @SuppressFBWarnings("EI")
    public RemoteProjectInstanceServiceClient(LoadBalancedWebClient loadBalancedWebClient) {
        this.loadBalancedWebClient = loadBalancedWebClient;
    }

    @Override
    public Optional<ProjectInstance> fetchWorkflowProjectInstance(String workflowId) {
        return Optional.ofNullable(
            loadBalancedWebClient.get(
                uriBuilder -> uriBuilder
                    .host(CONFIGURATION_APP)
                    .path(PROJECT_INSTANCE_SERVICE + "/fetch-workflow-project-instance/{workflowId}")
                    .build(workflowId),
                ProjectInstance.class));
    }
}
