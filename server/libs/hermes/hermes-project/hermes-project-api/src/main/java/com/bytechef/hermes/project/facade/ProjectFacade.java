
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

package com.bytechef.hermes.project.facade;

import com.bytechef.atlas.domain.Workflow;
import com.bytechef.hermes.project.domain.Project;
import com.bytechef.tag.domain.Tag;

import java.util.List;

/**
 * @author Ivica Cardic
 */
public interface ProjectFacade {

    Project addWorkflow(long id, String name, String description, String definition);

    Project create(Project project);

    void delete(Long id);

    Project getProject(Long id);

    List<Project> getProjects(List<Long> categoryIds, List<Long> tagIds);

    List<Tag> getProjectTags();

    List<Workflow> getProjectWorkflows(Long id);

    Project update(Long id, List<Tag> tags);

    Project update(Project project);
}
