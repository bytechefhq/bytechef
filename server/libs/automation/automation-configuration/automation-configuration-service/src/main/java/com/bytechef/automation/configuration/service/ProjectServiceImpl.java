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
import com.bytechef.automation.configuration.event.ProjectCreatedEvent;
import com.bytechef.automation.configuration.listener.ProjectGitSyncEventListener;
import com.bytechef.automation.configuration.repository.ProjectRepository;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.platform.user.service.UserService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
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

    private static final Logger logger = LoggerFactory.getLogger(ProjectServiceImpl.class);

    private final ApplicationContext applicationContext;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final ProjectRepository projectRepository;
    private final UserService userService;

    @SuppressFBWarnings("EI")
    public ProjectServiceImpl(
        ApplicationContext applicationContext, ApplicationEventPublisher applicationEventPublisher,
        ProjectRepository projectRepository, UserService userService) {

        this.applicationContext = applicationContext;
        this.applicationEventPublisher = applicationEventPublisher;
        this.projectRepository = projectRepository;
        this.userService = userService;
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

        // Auto-admin assignment requires an authenticated creator. Without a SecurityContext (system-initiated
        // creation, async thread without context propagation) the project ends up with no admin row and is
        // unreachable through @PreAuthorize until a tenant admin manually fixes the membership. Log loudly so this
        // surfaces in operations dashboards rather than silently rotting.
        userService.fetchCurrentUser()
            .ifPresentOrElse(
                creator -> applicationEventPublisher.publishEvent(
                    new ProjectCreatedEvent(savedProject.getId(), creator.getId())),
                () -> logger.error(
                    "ORPHAN PROJECT WARNING: Created project id={} without an authenticated creator. "
                        + "No project_user ADMIN row was seeded. The project will be inaccessible to "
                        + "non-tenant-admin users until membership is repaired.",
                    savedProject.getId()));

        return savedProject;
    }

    @Override
    @PreAuthorize("@permissionService.hasProjectScope(#id, 'PROJECT_DELETE')")
    public void delete(long id) {
        projectRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Project> fetchProject(String name) {
        return projectRepository.findByNameIgnoreCase(name);
    }

    @Override
    @Transactional(readOnly = true)
    public Project getProjectDeploymentProject(long projectDeploymentId) {
        return OptionalUtils.get(projectRepository.findByProjectDeploymentId(projectDeploymentId));
    }

    @Override
    @PreAuthorize("@permissionService.hasProjectScope(#id, 'WORKFLOW_VIEW')")
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
    public Project getWorkflowProject(String workflowId) {
        return OptionalUtils.get(projectRepository.findByWorkflowId(workflowId));
    }

    @Override
    @PreAuthorize("@permissionService.hasProjectScope(#id, 'DEPLOYMENT_PUSH')")
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
    @PreAuthorize("@permissionService.hasProjectScope(#id, 'WORKFLOW_EDIT')")
    public Project update(long id, List<Long> tagIds) {
        Project project = getProject(id);

        project.setTagIds(tagIds);

        return projectRepository.save(project);
    }

    @Override
    @PreAuthorize("@permissionService.hasProjectScope(#project.id, 'WORKFLOW_EDIT')")
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

        return projectRepository.save(curProject);
    }
}
