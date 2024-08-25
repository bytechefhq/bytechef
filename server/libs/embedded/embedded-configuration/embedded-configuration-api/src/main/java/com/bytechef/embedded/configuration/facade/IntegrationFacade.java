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

package com.bytechef.embedded.configuration.facade;

import com.bytechef.embedded.configuration.domain.IntegrationVersion.Status;
import com.bytechef.embedded.configuration.dto.IntegrationDTO;
import com.bytechef.embedded.configuration.dto.IntegrationWorkflowDTO;
import com.bytechef.platform.category.domain.Category;
import com.bytechef.platform.constant.Environment;
import com.bytechef.platform.tag.domain.Tag;
import java.util.List;
import org.springframework.lang.NonNull;

/**
 * @author Ivica Cardic
 */
public interface IntegrationFacade {

    IntegrationWorkflowDTO addWorkflow(long id, @NonNull String definition);

    IntegrationDTO createIntegration(@NonNull IntegrationDTO integrationDTO);

    void deleteIntegration(long id);

    void deleteWorkflow(@NonNull String workflowId);

    List<IntegrationDTO> getEnabledIntegrationInstanceConfigurationIntegrations(Environment environment);

    IntegrationDTO getIntegration(long id);

    List<Category> getIntegrationCategories();

    List<Tag> getIntegrationTags();

    List<IntegrationWorkflowDTO> getIntegrationVersionWorkflows(long id, int integrationVersion);

    IntegrationWorkflowDTO getIntegrationWorkflow(String workflowId);

    IntegrationWorkflowDTO getIntegrationWorkflow(long integrationWorkflowId);

    List<IntegrationWorkflowDTO> getIntegrationWorkflows();

    List<IntegrationWorkflowDTO> getIntegrationWorkflows(long id);

    List<IntegrationDTO> getIntegrations(
        Long categoryId, boolean integrationInstanceConfigurations, Long tagId, Status status);

    void publishIntegration(long id, String description);

    IntegrationDTO updateIntegration(@NonNull IntegrationDTO integration);

    void updateIntegrationTags(long id, @NonNull List<Tag> tags);

    IntegrationWorkflowDTO updateWorkflow(String workflowId, String definition, int version);
}
