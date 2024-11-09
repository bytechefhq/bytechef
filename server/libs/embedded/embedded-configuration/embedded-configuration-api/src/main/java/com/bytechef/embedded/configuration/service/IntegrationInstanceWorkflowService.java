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
import java.util.List;
import java.util.Optional;
import org.springframework.lang.NonNull;

/**
 * @author Ivica Cardic
 */
public interface IntegrationInstanceWorkflowService {

    IntegrationInstanceWorkflow createIntegrationInstanceWorkflow(
        long integrationInstanceId, long integrationInstanceConfigurationWorkflowId);

    void delete(Long id);

    void deleteByIntegrationInstanceConfigurationWorkflowId(Long integrationInstanceConfigurationWorkflowId);

    Optional<IntegrationInstanceWorkflow> fetchIntegrationInstanceWorkflow(
        long integrationInstanceId, @NonNull String workflowId);

    IntegrationInstanceWorkflow getIntegrationInstanceWorkflow(long integrationInstanceId, @NonNull String workflowId);

    List<IntegrationInstanceWorkflow> getIntegrationInstanceWorkflows(long integrationInstanceId);

    void update(@NonNull IntegrationInstanceWorkflow integrationInstanceWorkflow);

    void updateEnabled(Long id, boolean enabled);

}
