
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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

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
        Assert.notNull(workflowId, "'workflowId' must not be null");

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
        Assert.notNull(project, "'project' must not be null");

        Assert.isNull(project.getId(), "'id' must be null");
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
    public Optional<Project> fetchJobProject(long jobId) {
        return projectRepository.findByJobId(jobId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Project> fetchProject(String name) {
        return projectRepository.findByName(name);
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
    public Project update(Project project) {
        Assert.notNull(project, "'project' must not be null");

        Assert.notNull(project.getId(), "'id' must not be null");
        Assert.notNull(project.getName(), "'name' must not be null");

        Project curProject = getProject(project.getId());

        curProject.setCategoryId(project.getCategoryId());
        curProject.setDescription(project.getDescription());
        curProject.setName(project.getName());
        curProject.setTagIds(project.getTagIds());
        curProject.setWorkflowIds(project.getWorkflowIds());

        return projectRepository.save(curProject);
    }
}
