
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

package com.bytechef.hermes.project.service.impl;

import com.bytechef.hermes.project.domain.Project;
import com.bytechef.hermes.project.repository.ProjectRepository;

import java.util.List;
import java.util.stream.StreamSupport;

import com.bytechef.hermes.project.service.ProjectService;
import com.bytechef.tag.domain.Tag;
import org.springframework.lang.NonNull;
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
        Project project = getProject(id);

        project.addWorkflow(workflowId);

        return projectRepository.save(project);
    }

    @Override
    public Project create(Project project) {
        Assert.notNull(project, "'project' must not be null");

        return projectRepository.save(project);
    }

    @Override
    public void delete(long id) {
        projectRepository.deleteById(id);
    }

    @Override
    public Project getProject(long id) {
        return projectRepository.findById(id)
            .orElseThrow();
    }

    @Override
    public List<Project> getProjects(List<Long> categoryIds, List<Long> tagIds) {
        Iterable<Project> projectIterable;

        if (CollectionUtils.isEmpty(categoryIds) && CollectionUtils.isEmpty(tagIds)) {
            projectIterable = projectRepository.findAll();
        } else if (!CollectionUtils.isEmpty(categoryIds) && CollectionUtils.isEmpty(tagIds)) {
            projectIterable = projectRepository.findByCategoryIdIn(categoryIds);
        } else if (CollectionUtils.isEmpty(categoryIds)) {
            projectIterable = projectRepository.findByTagIdIn(tagIds);
        } else {
            projectIterable = projectRepository.findByCategoryIdsAndTagIds(categoryIds, tagIds);
        }

        return StreamSupport.stream(projectIterable.spliterator(), false)
            .toList();
    }

    @Override
    public Project update(long id, List<Tag> tags) {
        Project project = getProject(id);

        project.setTags(tags);

        return projectRepository.save(project);
    }

    @Override
    public Project update(@NonNull Project project) {
        Assert.notNull(project, "'project' must not be null");
        Assert.notEmpty(project.getWorkflowIds(), "'workflowIds' must not be empty");

        return projectRepository.save(project);
    }
}
