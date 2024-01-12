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

package com.bytechef.automation.configuration.facade;

import com.bytechef.automation.configuration.domain.ProjectInstanceWorkflow;
import com.bytechef.automation.configuration.dto.ProjectInstanceDTO;
import com.bytechef.tag.domain.Tag;
import java.util.List;

/**
 * @author Ivica Cardic
 */
public interface ProjectInstanceFacade {

    ProjectInstanceDTO createProjectInstance(ProjectInstanceDTO projectInstanceDTO);

    long createProjectInstanceWorkflowJob(Long id, String workflowId);

    void deleteProjectInstance(long id);

    void enableProjectInstance(long id, boolean enable);

    void enableProjectInstanceWorkflow(long projectInstanceId, String workflowId, boolean enable);

    ProjectInstanceDTO getProjectInstance(long id);

    List<Tag> getProjectInstanceTags();

    List<ProjectInstanceDTO> getProjectInstances(Long projectId, Long tagId);

    ProjectInstanceDTO updateProjectInstance(ProjectInstanceDTO projectInstanceDTO);

    void updateProjectInstanceTags(long id, List<Tag> tags);

    ProjectInstanceWorkflow updateProjectInstanceWorkflow(ProjectInstanceWorkflow projectInstanceWorkflow);
}
