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

import com.bytechef.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflow;
import com.bytechef.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflowConnection;
import java.util.List;
import java.util.Optional;

/**
 * @author Ivica Cardic
 */
public interface IntegrationInstanceConfigurationWorkflowService {

    IntegrationInstanceConfigurationWorkflow create(
        IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow);

    List<IntegrationInstanceConfigurationWorkflow> create(
        List<IntegrationInstanceConfigurationWorkflow> integrationInstanceConfigurationWorkflows);

    void delete(long id);

    void deleteIntegrationInstanceConfigurationWorkflows(long integrationInstanceConfigurationId);

    Optional<IntegrationInstanceConfigurationWorkflowConnection>
        fetchIntegrationInstanceConfigurationWorkflowConnection(
            long integrationInstanceConfigurationId, String workflowId, String workflowNodeName,
            String workflowConnectionKey);

    IntegrationInstanceConfigurationWorkflow getIntegrationInstanceConfigurationWorkflow(
        long integrationInstanceConfigurationId, String workflowId);

    IntegrationInstanceConfigurationWorkflowConnection getIntegrationInstanceConfigurationWorkflowConnection(
        long integrationInstanceConfigurationId, String workflowId, String operationName, String key);

    List<IntegrationInstanceConfigurationWorkflowConnection> getIntegrationInstanceConfigurationWorkflowConnections(
        Long integrationInstanceConfigurationId, String workflowId, String operationName);

    List<IntegrationInstanceConfigurationWorkflow> getIntegrationInstanceConfigurationWorkflows(
        long integrationInstanceConfigurationId);

    List<IntegrationInstanceConfigurationWorkflow> getIntegrationInstanceConfigurationWorkflows(
        List<Long> integrationInstanceConfigurationIds);

    boolean isConnectionUsed(long connectionId);

    boolean isIntegrationInstanceWorkflowEnabled(long integrationInstanceId, String workflowId);

    IntegrationInstanceConfigurationWorkflow update(
        IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow);

    List<IntegrationInstanceConfigurationWorkflow> update(
        List<IntegrationInstanceConfigurationWorkflow> integrationInstanceConfigurationWorkflows);

    void updateEnabled(Long id, boolean enable);
}
