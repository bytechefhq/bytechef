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

import com.bytechef.embedded.configuration.domain.Integration;
import com.bytechef.embedded.configuration.domain.IntegrationVersion;
import com.bytechef.embedded.configuration.domain.IntegrationVersion.Status;
import java.util.List;
import java.util.Optional;

/**
 * @author Ivica Cardic
 */
public interface IntegrationService {

    void addVersion(long id, List<String> duplicatedVersionWorkflowIds);

    Integration addWorkflow(long id, String workflowId);

    long countIntegrations();

    Integration create(Integration integration);

    void delete(long id);

    Optional<Integration> fetchWorkflowIntegration(String workflowId);

    Integration getIntegration(long id);

    Optional<Integration> fetchIntegration(String name);

    Integration getIntegrationInstanceIntegration(long integrationInstanceId);

    List<Integration> getIntegrations();

    List<Integration> getIntegrations(List<Long> ids);

    List<IntegrationVersion> getIntegrationVersions(Long id);

    List<Integration> getIntegrations(Long categoryId, List<Long> ids, Long tagId, Status status);

    Integration getWorkflowIntegration(String workflowId);

    void publishIntegration(long id, String description);

    void removeWorkflow(long id, String workflowId);

    Integration update(long id, List<Long> tagIds);

    Integration update(Integration integration);
}
