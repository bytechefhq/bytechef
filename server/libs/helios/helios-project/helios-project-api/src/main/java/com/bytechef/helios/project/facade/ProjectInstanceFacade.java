
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

package com.bytechef.helios.project.facade;

import com.bytechef.helios.project.dto.ProjectInstanceDTO;
import com.bytechef.tag.domain.Tag;

import java.util.List;

/**
 * @author Ivica Cardic
 */
public interface ProjectInstanceFacade {

    ProjectInstanceDTO createProjectInstance(ProjectInstanceDTO projectInstance);

    long createJob(String workflowId, long projectInstanceId);

    void deleteProjectInstance(long projectInstanceId);

    void enableProjectInstance(long id, boolean enable);

    void enableProjectInstanceWorkflow(long id, String workflowId, boolean enable);

    ProjectInstanceDTO getProjectInstance(long projectInstanceId);

    List<Tag> getProjectInstanceTags();

    List<ProjectInstanceDTO> searchProjectInstances(List<Long> projectIds, List<Long> tagIds);

    ProjectInstanceDTO update(ProjectInstanceDTO projectInstance);

    void updateProjectInstanceTags(Long projectInstanceId, List<Tag> tags);
}
