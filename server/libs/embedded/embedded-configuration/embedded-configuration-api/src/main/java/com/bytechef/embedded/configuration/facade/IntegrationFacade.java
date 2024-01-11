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
import com.bytechef.category.domain.Category;
import com.bytechef.embedded.configuration.dto.IntegrationDTO;
import com.bytechef.tag.domain.Tag;
import java.util.List;
import org.springframework.lang.NonNull;

/**
 * @author Ivica Cardic
 */
public interface IntegrationFacade {

    Workflow addWorkflow(long id, @NonNull String definition);

    IntegrationDTO create(@NonNull IntegrationDTO integrationDTO);

    void delete(long id);

    void deleteWorkflow(long id, @NonNull String workflowId);

    IntegrationDTO getIntegration(long id);

    List<Category> getIntegrationCategories();

    List<Tag> getIntegrationTags();

    List<Workflow> getIntegrationWorkflows(long id);

    List<IntegrationDTO> getIntegrations(Long categoryId, boolean integrationInstances, Long tagId, Boolean published);

    IntegrationDTO update(long id, @NonNull List<Tag> tags);

    IntegrationDTO update(@NonNull IntegrationDTO integration);
}
