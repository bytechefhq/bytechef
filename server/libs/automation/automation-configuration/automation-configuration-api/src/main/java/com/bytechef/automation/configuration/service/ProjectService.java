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

package com.bytechef.automation.configuration.service;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectVersion;
import com.bytechef.automation.configuration.domain.ProjectVersion.Status;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 */
public interface ProjectService {

    long countProjects();

    Project create(Project project);

    void delete(long id);

    Optional<Project> fetchProject(String name);

    Optional<Project> fetchProject(String name, long workspaceId);

    /**
     * Non-throwing counterpart of {@link #getWorkflowProject(String)}, for callers to which "this workflow belongs to
     * no project" is an ordinary answer rather than an error.
     *
     * <p>
     * Prefer this over catching {@code getWorkflowProject}'s exception. That method is {@code @Transactional}, so the
     * instant its proxy sees the throw, Spring's default {@code globalRollbackOnParticipationFailure} marks the
     * CALLER's participating transaction rollback-only — the caller's catch block then runs, but its transaction is
     * already doomed and fails at commit with {@code UnexpectedRollbackException}. Mocked unit tests never exercise
     * that proxy chain and so never reveal it. Compare {@code ProjectCodeWorkflowServiceImpl#getProjectCodeWorkflow},
     * which had to be annotated {@code noRollbackFor} for exactly this reason.
     * </p>
     */
    Optional<Project> fetchWorkflowProject(String workflowId);

    Project getProjectDeploymentProject(long projectDeploymentId);

    Project getProject(long id);

    Project getProject(UUID uuid);

    List<Project> getProjects();

    List<ProjectVersion> getProjectVersions(Long id);

    List<Project> getProjects(List<Long> ids);

    List<Project> getProjects(
        @Nullable Boolean apiCollections, @Nullable Long categoryId, Boolean projectDeployments,
        @Nullable Long tagId, @Nullable Status status, @Nullable Long workspaceId);

    Project getWorkflowProject(String workflowId);

    List<Long> getWorkspaceProjectIds(long workspaceId);

    int publishProject(long id, @Nullable String description, boolean syncWithGit);

    Project update(long id, List<Long> tagIds);

    Project update(Project project);

    Project updateErrorWorkflow(long id, @Nullable Long errorProjectWorkflowId);

    Project updatePermissionExpression(long id, @Nullable String permissionExpression);
}
