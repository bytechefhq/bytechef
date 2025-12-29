/*
 * Copyright 2025 ByteChef
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

import com.bytechef.automation.configuration.domain.ProjectVersion.Status;
import com.bytechef.automation.configuration.dto.ProjectDTO;
import com.bytechef.automation.configuration.dto.ProjectTemplateDTO;
import com.bytechef.automation.configuration.dto.ProjectWorkflowDTO;
import com.bytechef.automation.configuration.dto.SharedProjectDTO;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 */
public interface ProjectFacade {

    long createProject(ProjectDTO projectDTO);

    void deleteProject(long id);

    void deleteSharedProject(long id);

    ProjectDTO duplicateProject(long id);

    byte[] exportProject(long id);

    void exportSharedProject(long id, @Nullable String description);

    ProjectTemplateDTO getProjectTemplate(String id, boolean sharedProject);

    ProjectDTO getProject(long id);

    List<ProjectTemplateDTO> getPreBuiltProjectTemplates(String query, String category);

    List<ProjectDTO> getProjects(
        @Nullable Long categoryId, @Nullable Boolean projectDeployments, @Nullable Long tagId, @Nullable Status status);

    SharedProjectDTO getSharedProject(String projectUuid);

    List<ProjectDTO> getWorkspaceProjects(
        Boolean apiCollections, @Nullable Long categoryId, boolean includeAllFields, Boolean projectDeployments,
        @Nullable Status status, @Nullable Long tagId, long workspaceId);

    List<ProjectWorkflowDTO> getWorkspaceProjectWorkflows(long workspaceId);

    long importProject(byte[] projectData, long workspaceId);

    long importProjectTemplate(String id, long workspaceId, boolean sharedProject);

    int publishProject(long id, @Nullable String description, boolean syncWithGit);

    void updateProject(ProjectDTO projectDTO);
}
