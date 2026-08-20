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

import com.bytechef.automation.configuration.audit.ProjectAuditEvent;
import com.bytechef.automation.configuration.audit.ProjectAuditPublisher;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectVersion;
import com.bytechef.automation.configuration.domain.ProjectVersion.Status;
import com.bytechef.automation.configuration.exception.ProjectErrorType;
import com.bytechef.automation.configuration.listener.ProjectGitSyncEventListener;
import com.bytechef.automation.configuration.repository.ProjectRepository;
import com.bytechef.automation.configuration.security.ProjectVisibilityFilter;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.security.domain.ResourceVisibility;
import com.bytechef.platform.security.domain.ResourceVisibilityPolicyRegistry;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional
public class ProjectServiceImpl implements ProjectService {

    private final ApplicationContext applicationContext;
    private final ProjectAuditPublisher projectAuditPublisher;
    private final ProjectRepository projectRepository;
    private final ResourceVisibilityPolicyRegistry resourceVisibilityPolicyRegistry;

    @SuppressFBWarnings("EI")
    public ProjectServiceImpl(
        ApplicationContext applicationContext, ProjectAuditPublisher projectAuditPublisher,
        ProjectRepository projectRepository, ResourceVisibilityPolicyRegistry resourceVisibilityPolicyRegistry) {

        this.applicationContext = applicationContext;
        this.projectAuditPublisher = projectAuditPublisher;
        this.projectRepository = projectRepository;
        this.resourceVisibilityPolicyRegistry = resourceVisibilityPolicyRegistry;
    }

    @Override
    public long countProjects() {
        return projectRepository.count();
    }

    @Override
    public Project create(Project project) {
        Assert.notNull(project, "'project' must not be null");
        Assert.isTrue(project.getId() == null, "'id' must be null");
        Assert.notNull(project.getName(), "'name' must not be null");

        Project savedProject = projectRepository.save(project);

        projectAuditPublisher.publish(ProjectAuditEvent.PROJECT_CREATED, savedProject.getId());

        return savedProject;
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'Project', 'PROJECT_DELETE')")
    public void delete(long id) {
        projectRepository.deleteById(id);

        projectAuditPublisher.publish(ProjectAuditEvent.PROJECT_DELETED, id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Project> fetchProject(long id) {
        return projectRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Project> fetchProject(String name) {
        return projectRepository.findByNameIgnoreCase(name);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Project> fetchProject(String name, long workspaceId) {
        return projectRepository.findByNameIgnoreCaseAndWorkspaceId(name, workspaceId);
    }

    @Override
    @Transactional(readOnly = true)
    public Project getProjectDeploymentProject(long projectDeploymentId) {
        return OptionalUtils.get(projectRepository.findByProjectDeploymentId(projectDeploymentId));
    }

    @Override
    // @PreAuthorize("hasPermission(#id, 'Project', 'WORKFLOW_VIEW')")
    @Transactional(readOnly = true)
    public Project getProject(long id) {
        return OptionalUtils.get(projectRepository.findById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Project getProject(UUID uuid) {
        return projectRepository.findByUuid(uuid)
            .orElseThrow(() -> new IllegalArgumentException("Project not found"));
    }

    @Override
    public List<ProjectVersion> getProjectVersions(Long id) {
        Project project = getProject(id);

        return project.getProjectVersions()
            .stream()
            .sorted((o1, o2) -> Integer.compare(o2.getVersion(), o1.getVersion()))
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Project> getProjects() {
        return CollectionUtils.toList(projectRepository.findAll(Sort.by("name")));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Project> getProjects(List<Long> ids) {
        return CollectionUtils.toList(projectRepository.findAllById(ids));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Project> getProjects(
        Boolean apiCollections, Long categoryId, Boolean projectDeployments, Long tagId,
        Status status, Long workspaceId) {

        return projectRepository
            .findAllProjects(
                apiCollections, categoryId, projectDeployments, tagId, status == null ? null : status.ordinal(),
                workspaceId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Project> fetchWorkflowProject(String workflowId) {
        return projectRepository.findByWorkflowId(workflowId);
    }

    @Override
    @Transactional(readOnly = true)
    public Project getWorkflowProject(String workflowId) {
        return OptionalUtils.get(projectRepository.findByWorkflowId(workflowId));
    }

    @Override
    public List<Long> getWorkspaceProjectIds(long workspaceId) {
        return projectRepository.findProjectIdsByWorkspaceId(workspaceId);
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'Project', 'DEPLOYMENT_PUSH')")
    public int publishProject(long id, String description, boolean syncWithGit) {
        Project project = getProject(id);

        int newVersion = project.publish(description);

        if (syncWithGit) {
            Map<String, ProjectGitSyncEventListener> beansOfType = applicationContext.getBeansOfType(
                ProjectGitSyncEventListener.class);

            if (!beansOfType.isEmpty()) {
                ProjectGitSyncEventListener projectGitSyncEventListener = beansOfType.values()
                    .stream()
                    .findFirst()
                    .get();

                projectGitSyncEventListener.onBeforePublishProject(project);
            }
        }

        projectRepository.save(project);

        return newVersion;
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'Project', 'WORKFLOW_EDIT')")
    public Project update(long id, List<Long> tagIds) {
        Project project = getProject(id);

        project.setTagIds(tagIds);

        Project savedProject = projectRepository.save(project);

        projectAuditPublisher.publish(ProjectAuditEvent.PROJECT_UPDATED, savedProject.getId());

        return savedProject;
    }

    @Override
    @PreAuthorize("hasPermission(#project.id, 'Project', 'WORKFLOW_EDIT')")
    public Project update(Project project) {
        Assert.notNull(project, "'project' must not be null");
        Assert.notNull(project.getId(), "id");
        Assert.notNull(project.getName(), "name");

        Project curProject = getProject(project.getId());

        curProject.setCategoryId(project.getCategoryId());
        curProject.setDescription(project.getDescription());
        curProject.setName(project.getName());
        curProject.setTagIds(project.getTagIds());
        curProject.setVersion(project.getVersion());

        Project savedProject = projectRepository.save(curProject);

        projectAuditPublisher.publish(ProjectAuditEvent.PROJECT_UPDATED, savedProject.getId());

        return savedProject;
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'Project', 'WORKFLOW_EDIT')")
    public Project updateErrorWorkflow(long id, @Nullable Long errorProjectWorkflowId) {
        Project project = getProject(id);

        project.setErrorProjectWorkflowId(errorProjectWorkflowId);

        return projectRepository.save(project);
    }

    @Override
    public Project updatePermissionExpression(long id, @Nullable String permissionExpression) {
        Project project = getProject(id);

        project.setPermissionExpression(permissionExpression);

        return projectRepository.save(project);
    }

    /**
     * Rejects a rung the project model does not support, so the guarantee does not rest on caller discipline. The
     * sharing facade checks the same thing first and keeps owning the error a caller sees — this is the backstop for a
     * second caller that forgets to, and it throws the identical typed error so the two are indistinguishable to a
     * client. An unsupported rung persisted here would be loud in the worst way: {@code ProjectMapper} and the EE
     * {@code ApiCollectionMapper} map {@code ORGANIZATION} to {@code THROW_EXCEPTION}, so a bad row makes every
     * subsequent READ of the project throw.
     */
    @Override
    public Project updateVisibility(long id, ResourceVisibility visibility) {
        if (!resourceVisibilityPolicyRegistry.supports(ProjectVisibilityFilter.PROJECT, visibility)) {
            throw new ConfigurationException(
                "Project does not support %s visibility".formatted(visibility),
                ProjectErrorType.UNSUPPORTED_VISIBILITY);
        }

        Project project = getProject(id);

        project.setVisibility(visibility);

        // Deliberately no audit here: the sharing facade emits PROJECT_VISIBILITY_CHANGED for this write. Emitting a
        // generic PROJECT_UPDATED as well would log one change twice under two event types, and a reader could not
        // tell that from two changes.
        return projectRepository.save(project);
    }
}
