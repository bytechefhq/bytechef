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

import com.bytechef.embedded.configuration.domain.IntegrationWorkflow;
import java.util.List;

/**
 * @author Ivica Cardic
 */
public interface IntegrationWorkflowService {

    IntegrationWorkflow addWorkflow(long integrationId, int integrationVersion, String workflowId);

    void deleteIntegrationWorkflows(List<Long> ids);

    IntegrationWorkflow getIntegrationWorkflow(long id);

    List<Long> getIntegrationWorkflowIds(long integrationId, int integrationVersion);

    List<IntegrationWorkflow> getIntegrationWorkflows();

    List<IntegrationWorkflow> getIntegrationWorkflows(long integrationId);

    List<IntegrationWorkflow> getIntegrationWorkflows(long integrationId, int integrationVersion);

    List<String> getWorkflowIds(long integrationId);

    List<String> getWorkflowIds(long integrationId, int integrationVersion);

    IntegrationWorkflow getWorkflowIntegrationWorkflow(String workflowId);

    void removeWorkflow(long integrationId, int integrationVersion, String workflowId);

    IntegrationWorkflow update(IntegrationWorkflow integrationWorkflow);
}
