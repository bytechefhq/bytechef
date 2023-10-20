
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

package com.bytechef.helios.configuration.service;

import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.helios.configuration.domain.Project;
import com.bytechef.helios.configuration.repository.ProjectRepository;

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
        project.setStatus(Project.Status.UNPUBLISHED);

        return projectRepository.save(project);
    }

    @Override
    public void delete(long id) {
        projectRepository.delete(getProject(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Project> fetchProject(String name) {
        return projectRepository.findByNameIgnoreCase(name);
    }

    @Override
    public Project getWorkflowProject(String workflowId) {
        return OptionalUtils.get(projectRepository.findByWorkflowId(workflowId));
    }

    @Override
    @Transactional(readOnly = true)
    public Project getProject(long id) {
        return OptionalUtils.get(projectRepository.findById(id));
    }

    @Override
    public Project getProject(String name) {
        return OptionalUtils.get(fetchProject(name));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Project> getProjects() {
        return com.bytechef.commons.util.CollectionUtils.toList(projectRepository.findAll(Sort.by("name")));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Project> getProjects(List<Long> ids) {
        return com.bytechef.commons.util.CollectionUtils.toList(projectRepository.findAllById(ids));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Project> getPublishedProjects(Long categoryId, List<Long> ids, Long tagId) {
        Iterable<Project> projectIterable;

        if (categoryId == null && tagId == null) {
            projectIterable = projectRepository.findAllByPublishedDateNotNullOrderByName();
        } else if (categoryId != null && tagId == null) {
            projectIterable = projectRepository.findAllByCategoryIdAndPublishedDateNotNullOrderByName(categoryId);
        } else if (categoryId == null) {
            projectIterable = projectRepository.findAllByTagIdAndPublishedDateNotNullOrderByName(tagId);
        } else {
            projectIterable = projectRepository.findAllByCategoryIdAndTagIdAndPublishedDateNotNullOrderByName(
                categoryId, tagId);
        }

        List<Project> projects = com.bytechef.commons.util.CollectionUtils.toList(projectIterable);

        if (ids != null) {
            projects = projects.stream()
                .filter(project -> ids.contains(project.getId()))
                .toList();
        }

        return projects;
    }

    @Override
    public void removeWorkflow(long id, String workflowId) {
        Project project = getProject(id);

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
