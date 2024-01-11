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

package com.bytechef.embedded.workflow.coordinator;

import com.bytechef.commons.util.MapUtils;
import com.bytechef.embedded.configuration.domain.IntegrationInstanceWorkflowConnection;
import com.bytechef.embedded.configuration.service.IntegrationInstanceWorkflowService;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public abstract class AbstractDispatcherPreSendProcessor {

    private final IntegrationInstanceWorkflowService integrationInstanceWorkflowService;

    protected AbstractDispatcherPreSendProcessor(
        IntegrationInstanceWorkflowService integrationInstanceWorkflowService) {

        this.integrationInstanceWorkflowService = integrationInstanceWorkflowService;
    }

    protected Map<String, Long> getConnectionIdMap(
        Long integrationInstanceId, String workflowId, String operationName) {

        List<IntegrationInstanceWorkflowConnection> projectInstanceWorkflowConnections =
            integrationInstanceWorkflowService.getIntegrationInstanceWorkflowConnections(
                integrationInstanceId, workflowId, operationName);

        return MapUtils.toMap(
            projectInstanceWorkflowConnections, IntegrationInstanceWorkflowConnection::getKey,
            IntegrationInstanceWorkflowConnection::getConnectionId);
    }
}
