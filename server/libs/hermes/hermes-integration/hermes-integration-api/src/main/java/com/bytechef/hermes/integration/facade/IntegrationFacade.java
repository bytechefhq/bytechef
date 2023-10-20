
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

package com.bytechef.hermes.integration.facade;

import com.bytechef.atlas.domain.Workflow;
import com.bytechef.hermes.integration.domain.Integration;
import com.bytechef.tag.domain.Tag;

import java.util.List;

/**
 * @author Ivica Cardic
 */
public interface IntegrationFacade {

    Integration addWorkflow(long id, String name, String description, String definition);

    Integration create(Integration integration);

    void delete(Long id);

    Integration getIntegration(Long id);

    List<Integration> getIntegrations(Long categoryId, Long tagId);

    List<Tag> getIntegrationTags();

    List<Workflow> getIntegrationWorkflows(Long id);

    Integration update(Long id, List<Tag> tags);

    Integration update(Integration integration);
}
