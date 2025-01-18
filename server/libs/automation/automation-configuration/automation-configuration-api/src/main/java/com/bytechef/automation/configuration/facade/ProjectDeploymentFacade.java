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

import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.dto.ProjectDeploymentDTO;
import com.bytechef.platform.constant.Environment;
import com.bytechef.platform.tag.domain.Tag;
import java.util.List;

/**
 * @author Ivica Cardic
 */
public interface ProjectDeploymentFacade {

    long createProjectDeployment(ProjectDeploymentDTO projectDeploymentDTO);

    long createProjectDeploymentWorkflowJob(Long id, String workflowId);

    void deleteProjectDeployment(long id);

    void enableProjectDeployment(long id, boolean enable);

    void enableProjectDeploymentWorkflow(long projectDeploymentId, String workflowId, boolean enable);

    ProjectDeploymentDTO getProjectDeployment(long id);

    List<Tag> getProjectDeploymentTags();

    List<ProjectDeploymentDTO> getWorkspaceProjectDeployments(
        long id, Environment environment, Long projectId, Long tagId, boolean includeAllFields);

    void updateProjectDeployment(ProjectDeploymentDTO projectDeploymentDTO);

    void updateProjectDeploymentTags(long id, List<Tag> tags);

    void updateProjectDeploymentWorkflow(ProjectDeploymentWorkflow projectDeploymentWorkflow);
}
