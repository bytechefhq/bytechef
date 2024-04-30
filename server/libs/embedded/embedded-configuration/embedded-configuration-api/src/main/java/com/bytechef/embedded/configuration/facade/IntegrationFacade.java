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

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.embedded.configuration.domain.IntegrationVersion.Status;
import com.bytechef.embedded.configuration.dto.IntegrationDTO;
import com.bytechef.embedded.configuration.dto.WorkflowDTO;
import com.bytechef.platform.category.domain.Category;
import com.bytechef.platform.configuration.dto.UpdateParameterResultDTO;
import com.bytechef.platform.constant.Environment;
import com.bytechef.platform.tag.domain.Tag;
import java.util.List;
import java.util.Map;
import org.springframework.lang.NonNull;

/**
 * @author Ivica Cardic
 */
public interface IntegrationFacade {

    Workflow addWorkflow(long id, @NonNull String definition);

    IntegrationDTO createIntegration(@NonNull IntegrationDTO integrationDTO);

    void deleteIntegration(long id);

    void deleteWorkflow(@NonNull String workflowId);

    Map<String, ?> deleteWorkflowParameter(
        String workflowId, String workflowNodeName, String path, String name, Integer arrayIndex);

    IntegrationDTO getIntegration(long id);

    List<Category> getIntegrationCategories();

    List<Tag> getIntegrationTags();

    WorkflowDTO getIntegrationWorkflow(String workflowId);

    WorkflowDTO getIntegrationWorkflow(long integrationWorkflowId);

    List<WorkflowDTO> getIntegrationWorkflows();

    List<WorkflowDTO> getIntegrationWorkflows(long id);

    List<WorkflowDTO> getIntegrationVersionWorkflows(long id, int integrationVersion);

    List<IntegrationDTO> getIntegrations(Environment environment);

    List<IntegrationDTO> getIntegrations(
        Long categoryId, boolean integrationInstanceConfigurations, Long tagId, Status status);

    IntegrationDTO updateIntegration(@NonNull IntegrationDTO integration);

    void updateIntegrationTags(long id, @NonNull List<Tag> tags);

    WorkflowDTO updateWorkflow(String workflowId, String definition, int version);

    UpdateParameterResultDTO updateWorkflowParameter(
        String workflowId, String workflowNodeName, String path, String name, Integer arrayIndex, Object value);

}
