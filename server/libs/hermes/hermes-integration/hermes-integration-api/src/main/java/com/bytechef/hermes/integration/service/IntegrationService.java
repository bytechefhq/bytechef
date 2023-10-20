
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

package com.bytechef.hermes.integration.service;

import com.bytechef.hermes.integration.domain.Integration;
import com.bytechef.tag.domain.Tag;

import java.util.List;
import java.util.Set;

/**
 * @author Ivica Cardic
 */
public interface IntegrationService {

    Integration create(String name, String description, String category, Set<String> workflowIds, Set<Tag> tags);

    void delete(long id);

    Integration getIntegration(long id);

    List<Integration> getIntegrations();

    Integration update(
        long id, String name, String description, String category, Set<String> workflowIds, Set<Tag> tags);
}
