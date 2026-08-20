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

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectVersion;
import com.bytechef.automation.configuration.domain.ProjectVersion.Status;
import com.bytechef.automation.configuration.dto.ProjectDTO;
import com.bytechef.automation.configuration.dto.ProjectTemplateDTO;
import com.bytechef.automation.configuration.dto.ProjectWorkflowDTO;
import com.bytechef.automation.configuration.dto.SharedProjectDTO;
import com.bytechef.automation.configuration.dto.WorkspaceProjectWorkflowDTO;
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

    /**
     * The project the caller may open, as the domain object.
     *
     * <p>
     * The entity-returning sibling of {@link #getProject(long)}, for the GraphQL surface: its {@code Project} type is
     * assembled by field resolvers that take the domain object, and {@code ProjectDTO} carries neither
     * {@code errorProjectWorkflowId} nor the {@code categoryId}/{@code tagIds} those resolvers read. Both carry the
     * same {@code hasPermission(#id, 'Project', 'WORKFLOW_VIEW')} gate, because both disclose the same project. A
     * separate name rather than an overload: the two differ only in return type, which Java cannot overload on.
     *
     * <p>
     * One exception to "the same question", and the only one: this answers for a feature-owned system project
     * ({@code SystemProjects}) where {@link #getProjectRows()} does not list it. The listing hides the auto-provisioned
     * {@code __AI_AGENT__} / {@code __KNOWLEDGE_BASE__} / {@code __CONTEXT_STORE__} / {@code __EMBEDDED_*} projects
     * because they are a feature's bookkeeping rather than something a user made; this read has no such filter, so a
     * caller who already holds one of those ids gets its row. The GraphQL {@code
     * Project} type it feeds exposes name, category, tags, {@code errorProjectWorkflowId} and visibility — no workflow
     * content — and the gate is unchanged, so a caller with no {@code WORKFLOW_VIEW} in the owning workspace is refused
     * either way. Recorded rather than closed: it is an asymmetry between a listing and a by-id read, which is the
     * shape this whole model exists to name out loud. {@code ProjectFacadeRowVisibilityTest} skips system projects in
     * its agreement loop for exactly this reason.
     */
    Project getProjectRow(long id);

    List<ProjectTemplateDTO> getPreBuiltProjectTemplates(String query, String category);

    List<ProjectDTO> getProjects(
        @Nullable Long categoryId, @Nullable Boolean projectDeployments, @Nullable Long tagId, @Nullable Status status);

    /**
     * Every project the caller may open, across every workspace, as domain objects.
     *
     * <p>
     * The listing half of the same GraphQL surface: narrowed to the workspaces the caller holds {@code WORKFLOW_VIEW}
     * in and then filtered through {@code ProjectVisibilityFilter} — the two halves
     * {@code hasResourceScope(id, 'Project', scope)} composes for a project, which is what makes this the same set
     * {@link #getProjectRow(long)} answers one id at a time.
     *
     * <p>
     * Two things drop out here that the by-id read still answers for, so the agreement is exact only outside them.
     * Projects with no owning workspace cannot be scope-checked and are therefore not listed rather than listed
     * unchecked. Feature-owned system projects are not listed either — see {@link #getProjectRow(long)} for why that
     * one is an asymmetry rather than a gate, and why it is left standing.
     *
     * <p>
     * Unlike {@link #getProjectRow(long)} this carries no {@code @PreAuthorize}: there is no id to gate on, and the
     * narrowing in the implementation already refuses an unauthenticated caller. See the implementation for why an
     * annotation would overstate rather than add.
     */
    List<Project> getProjectRows();

    List<ProjectVersion> getProjectVersions(long id);

    SharedProjectDTO getSharedProject(String projectUuid);

    List<ProjectDTO> getWorkspaceProjects(
        Boolean apiCollections, @Nullable Long categoryId, boolean includeAllFields, Boolean projectDeployments,
        @Nullable Status status, @Nullable Long tagId, long workspaceId);

    List<ProjectWorkflowDTO> getWorkspaceProjectWorkflows(long workspaceId);

    List<WorkspaceProjectWorkflowDTO> getWorkspaceLatestProjectWorkflows(long workspaceId);

    long importProject(byte[] projectData, long workspaceId);

    long importProjectTemplate(String id, long workspaceId, boolean sharedProject);

    int publishProject(long id, @Nullable String description, boolean syncWithGit);

    void updateProject(ProjectDTO projectDTO);

    void updateProjectErrorWorkflow(long projectId, @Nullable Long errorProjectWorkflowId);
}
