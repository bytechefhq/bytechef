
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

package com.bytechef.helios.configuration.service;

import com.bytechef.helios.configuration.domain.ProjectInstance;

import java.util.List;

/**
 * @author Ivica Cardic
 */
public interface ProjectInstanceService {

    ProjectInstance create(ProjectInstance projectInstance);

    void delete(long id);

    ProjectInstance getProjectInstance(long id);

    List<Long> getProjectIds();

    List<ProjectInstance> getProjectInstances();

    List<ProjectInstance> getProjectInstances(Long projectId, Long tagId);

    ProjectInstance update(long id, List<Long> tagIds);

    ProjectInstance update(ProjectInstance projectInstance);

    void updateEnabled(long id, boolean enabled);
}
