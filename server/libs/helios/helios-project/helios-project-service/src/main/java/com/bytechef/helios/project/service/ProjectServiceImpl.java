
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

package com.bytechef.helios.project.service;

import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.helios.project.domain.Project;
import com.bytechef.helios.project.repository.ProjectRepository;

import java.util.List;
import java.util.Optional;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * @author Ivica Cardic
 */
@Service
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;

    public ProjectServiceImpl(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    @Override
    public Project addWorkflow(long id, String workflowId) {
        Assert.notNull(workflowId, "'workflowId' must not be null");

        Project project = getProject(id);

        project.addWorkflowId(workflowId);

        return projectRepository.save(project);
    }

    @Override
    public Project create(Project project) {
        Assert.notNull(project, "'project' must not be null");

        Assert.isNull(project.getId(), "'id' must be null");
        Assert.notEmpty(project.getWorkflowIds(), "'workflowIds' must not be empty");
        Assert.notNull(project.getName(), "'name' must not be null");

        project.setProjectVersion(1);
        project.setStatus(Project.Status.UNPUBLISHED);

        return projectRepository.save(project);
    }

    @Override
    public void delete(long id) {
        projectRepository.delete(getProject(id));
    }

    @Override
    public Optional<Project> fetchProject(String name) {
        return projectRepository.findByName(name);
    }

    @Override
    public Project getProject(long id) {
        return OptionalUtils.get(projectRepository.findById(id));
    }

    @Override
    public List<Project> getProjects() {
        return com.bytechef.commons.util.CollectionUtils.toList(projectRepository.findAll(Sort.by("name")));
    }

    @Override
    public List<Project> getProjects(List<Long> ids) {
        return com.bytechef.commons.util.CollectionUtils.toList(projectRepository.findAllById(ids));
    }

    @Override
    public List<Project> searchProjects(List<Long> categoryIds, List<Long> ids, List<Long> tagIds) {
        Iterable<Project> projectIterable;

        if (CollectionUtils.isEmpty(categoryIds) && CollectionUtils.isEmpty(tagIds)) {
            projectIterable = projectRepository.findAll(Sort.by("name"));
        } else if (!CollectionUtils.isEmpty(categoryIds) && CollectionUtils.isEmpty(tagIds)) {
            projectIterable = projectRepository.findAllByCategoryIdInOrderByName(categoryIds);
        } else if (CollectionUtils.isEmpty(categoryIds)) {
            projectIterable = projectRepository.findAllByTagIdInOrderByName(tagIds);
        } else {
            projectIterable = projectRepository.findAllByCategoryIdsAndTagIdsOrderByName(categoryIds, tagIds);
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
    public Project update(long id, List<Long> tagIds) {
        Project project = getProject(id);

        project.setTagIds(tagIds);

        return projectRepository.save(project);
    }

    @Override
    @SuppressFBWarnings("NP")
    public Project update(
        long id, Long categoryId, String description, String name, List<Long> tagIds, List<String> workflowIds) {

        Assert.notEmpty(workflowIds, "'workflowIds' must not be empty");
        Assert.notNull(name, "'name' must not be null");

        Project project = getProject(id);

        project.setCategoryId(categoryId);
        project.setDescription(description);
        project.setId(id);
        project.setName(name);
        project.setTagIds(tagIds);
        project.setWorkflowIds(workflowIds);

        return projectRepository.save(project);
    }
}
