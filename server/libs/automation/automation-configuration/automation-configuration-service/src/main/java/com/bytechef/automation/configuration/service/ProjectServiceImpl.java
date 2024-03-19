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

package com.bytechef.automation.configuration.service;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.Project.Status;
import com.bytechef.automation.configuration.repository.ProjectRepository;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.platform.configuration.exception.ApplicationException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.Validate;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;

    public ProjectServiceImpl(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    @Override
    public Project addWorkflow(long id, String workflowId) {
        Validate.notNull(workflowId, "'workflowId' must not be null");

        Project project = getProject(id);

        project.addWorkflowId(workflowId);

        return projectRepository.save(project);
    }

    @Override
    public long countProjects() {
        return projectRepository.count();
    }

    @Override
    public Project create(Project project) {
        Validate.notNull(project, "'project' must not be null");
        Validate.isTrue(project.getId() == null, "'id' must be null");
        Validate.notNull(project.getName(), "'name' must not be null");

        project.setProjectVersion(1);
        project.setStatus(Status.UNPUBLISHED);

        return projectRepository.save(project);
    }

    @Override
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
    public boolean isProjectEnabled(long projectId) {
        Project project = getProject(projectId);

        return project.getPublishedDate() != null;
    }

    @Override
    @Transactional(readOnly = true)
    public Project getProjectInstanceProject(long projectInstanceId) {
        return projectRepository.findByProjectInstanceId(projectInstanceId);
    }

    @Override
    @Transactional(readOnly = true)
    public Project getProject(long id) {
        return OptionalUtils.get(projectRepository.findById(id));
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
    public List<Project> getProjects(Long categoryId, List<Long> ids, Long tagId, Status status) {
        return projectRepository.findAllProjects(categoryId, ids, tagId, status == null ? null : status.ordinal());
    }

    @Override
    @Transactional(readOnly = true)
    public Project getWorkflowProject(String workflowId) {
        return OptionalUtils.get(projectRepository.findByWorkflowId(workflowId));
    }

    @Override
    public Project publish(long id) {
        Project curProject = getProject(id);

        curProject.setPublishedDate(LocalDateTime.now());
        curProject.setStatus(Status.PUBLISHED);

        return projectRepository.save(curProject);
    }

    @Override
    public void removeWorkflow(long id, String workflowId) {
        Project project = getProject(id);

        if (CollectionUtils.count(project.getWorkflowIds()) == 1) {
            throw new ApplicationException(
                "The last workflow id=%s cannot be deleted".formatted(workflowId), Project.class, 102);
        }

        project.removeWorkflow(workflowId);

        update(project);
    }

    @Override
    public Project update(long id, List<Long> tagIds) {
        Project project = getProject(id);

        project.setTagIds(tagIds);

        return projectRepository.save(project);
    }

    @Override
    public Project update(Project project) {
        Validate.notNull(project, "'project' must not be null");

        Project curProject = getProject(Validate.notNull(project.getId(), "id"));

        curProject.setCategoryId(project.getCategoryId());
        curProject.setDescription(project.getDescription());
        curProject.setName(Validate.notNull(project.getName(), "name"));
        curProject.setTagIds(project.getTagIds());
        curProject.setWorkflowIds(project.getWorkflowIds());

        return projectRepository.save(curProject);
    }
}
