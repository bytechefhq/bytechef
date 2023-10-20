
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

package com.bytechef.hermes.project.service;

import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.hermes.project.domain.ProjectInstance;
import com.bytechef.hermes.project.repository.ProjectInstanceRepository;
import com.bytechef.tag.domain.Tag;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * @author Ivica Cardic
 */
@Service
public class ProjectInstanceServiceImpl implements ProjectInstanceService {

    private final ProjectInstanceRepository projectInstanceRepository;

    public ProjectInstanceServiceImpl(ProjectInstanceRepository projectInstanceRepository) {
        this.projectInstanceRepository = projectInstanceRepository;
    }

    @Override
    public ProjectInstance create(ProjectInstance projectInstance) {
        Assert.notNull(projectInstance, "'projectInstance' must not be null");
        Assert.isNull(projectInstance.getId(), "'id' must be null");
        Assert.notNull(projectInstance.getProjectId(), "'projectId' must not be empty");

        projectInstance.setStatus(ProjectInstance.Status.DISABLED);

        return projectInstanceRepository.save(projectInstance);
    }

    @Override
    public void delete(long id) {
        projectInstanceRepository.delete(getProjectInstance(id));
    }

    @Override
    public ProjectInstance getProjectInstance(long id) {
        return OptionalUtils.get(projectInstanceRepository.findById(id));
    }

    @Override
    public List<Long> getProjectIds() {
        return projectInstanceRepository.findAllProjectId();
    }

    @Override
    public List<ProjectInstance> getProjectInstances() {
        return searchProjectInstances(List.of(), List.of());
    }

    @Override
    public List<ProjectInstance> searchProjectInstances(List<Long> projectIds, List<Long> tagIds) {
        Iterable<ProjectInstance> projectInstanceIterable;

        if (CollectionUtils.isEmpty(projectIds) && CollectionUtils.isEmpty(tagIds)) {
            projectInstanceIterable = projectInstanceRepository.findAll(Sort.by("name"));
        } else if (!CollectionUtils.isEmpty(projectIds) && CollectionUtils.isEmpty(tagIds)) {
            projectInstanceIterable = projectInstanceRepository.findAllByProjectIdInOrderByName(projectIds);
        } else if (CollectionUtils.isEmpty(projectIds)) {
            projectInstanceIterable = projectInstanceRepository.findAllByTagIdInOrderByName(tagIds);
        } else {
            projectInstanceIterable = projectInstanceRepository.findAllByProjectIdsAndTagIdsOrderByName(
                projectIds, tagIds);
        }

        return com.bytechef.commons.util.CollectionUtils.toList(projectInstanceIterable);
    }

    @Override
    public ProjectInstance update(long id, List<Tag> tags) {
        ProjectInstance projectInstance = getProjectInstance(id);

        projectInstance.setTags(tags);

        return projectInstanceRepository.save(projectInstance);
    }

    @Override
    public ProjectInstance update(ProjectInstance projectInstance) {
        Assert.notNull(projectInstance, "'projectInstance' must not be null");
        Assert.notNull(projectInstance.getId(), "'id' must not be null");
        Assert.notNull(projectInstance.getProjectId(), "'projectId' must not be empty");

        ProjectInstance curProjectInstance = getProjectInstance(projectInstance.getId());

        return projectInstanceRepository.save(curProjectInstance.update(projectInstance));
    }
}
