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

package com.bytechef.embedded.configuration.service;

import com.bytechef.embedded.configuration.domain.IntegrationInstanceWorkflow;
import com.bytechef.embedded.configuration.domain.IntegrationInstanceWorkflowConnection;
import java.util.List;
import java.util.Optional;

/**
 * @author Ivica Cardic
 */
public interface IntegrationInstanceWorkflowService {

    List<IntegrationInstanceWorkflow> create(List<IntegrationInstanceWorkflow> integrationInstanceWorkflows);

    void delete(Long id);

    Optional<IntegrationInstanceWorkflowConnection> fetchIntegrationInstanceWorkflowConnection(
        long integrationInstanceId, String workflowId, String operationName, String key);

    boolean isIntegrationInstanceWorkflowEnabled(long integrationInstanceId, String workflowId);

    IntegrationInstanceWorkflow getIntegrationInstanceWorkflow(long integrationInstanceId, String workflowId);

    IntegrationInstanceWorkflowConnection getIntegrationInstanceWorkflowConnection(
        long integrationInstanceId, String workflowId, String operationName, String key);

    List<IntegrationInstanceWorkflowConnection> getIntegrationInstanceWorkflowConnections(
        Long integrationInstanceId, String workflowId, String operationName);

    List<IntegrationInstanceWorkflow> getIntegrationInstanceWorkflows(long integrationInstanceId);

    List<IntegrationInstanceWorkflow> getIntegrationInstanceWorkflows(List<Long> integrationInstanceIds);

    IntegrationInstanceWorkflow update(IntegrationInstanceWorkflow integrationInstanceWorkflow);

    List<IntegrationInstanceWorkflow> update(List<IntegrationInstanceWorkflow> integrationInstanceWorkflows);

    void updateEnabled(Long id, boolean enable);

}
