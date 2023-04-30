
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

package com.bytechef.dione.integration.facade;

import com.bytechef.category.domain.Category;
import com.bytechef.dione.integration.dto.IntegrationDTO;
import com.bytechef.hermes.workflow.WorkflowDTO;
import com.bytechef.tag.domain.Tag;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author Ivica Cardic
 */
public interface IntegrationFacade {

    WorkflowDTO addWorkflow(long id, String label, String description, String definition);

    IntegrationDTO create(IntegrationDTO integrationDTO);

    void delete(long id);

    IntegrationDTO getIntegration(long id);

    @Transactional(readOnly = true)
    List<Category> getIntegrationCategories();

    List<Tag> getIntegrationTags();

    List<WorkflowDTO> getIntegrationWorkflows(long id);

    List<IntegrationDTO> searchIntegrations(List<Long> categoryIds, List<Long> tagIds);

    IntegrationDTO update(long id, List<Tag> tags);

    IntegrationDTO update(IntegrationDTO integration);
}
