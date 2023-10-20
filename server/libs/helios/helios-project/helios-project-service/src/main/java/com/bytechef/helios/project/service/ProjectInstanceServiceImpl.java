
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
import com.bytechef.helios.project.domain.ProjectInstance.Status;
import com.bytechef.helios.project.repository.ProjectInstanceRepository;
import com.bytechef.helios.project.domain.ProjectInstance;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Optional;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional
public class ProjectInstanceServiceImpl implements ProjectInstanceService {

    private final ProjectInstanceRepository projectInstanceRepository;

    public ProjectInstanceServiceImpl(ProjectInstanceRepository projectInstanceRepository) {

        this.projectInstanceRepository = projectInstanceRepository;
    }

    @Override
    public ProjectInstance create(ProjectInstance projectInstance) {
        Assert.notNull(projectInstance, "'projectInstance' must not be empty");

        Assert.isNull(projectInstance.getId(), "'id' must not be empty");
        Assert.notNull(projectInstance.getProjectId(), "'projectId' must not be empty");
        Assert.notNull(projectInstance.getName(), "'projectId' must not be empty");

        projectInstance.setStatus(Status.ENABLED);

        return projectInstanceRepository.save(projectInstance);
    }

    @Override
    public void delete(long id) {
        projectInstanceRepository.delete(getProjectInstance(id));
    }

    @Override
    public Optional<ProjectInstance> fetchJobProjectInstance(long jobId) {
        return projectInstanceRepository.findByJobId(jobId);
    }

    @Override
    public ProjectInstance getJobProjectInstance(long jobId) {
        return OptionalUtils.get(projectInstanceRepository.findByJobId(jobId));
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectInstance getProjectInstance(long id) {
        return OptionalUtils.get(projectInstanceRepository.findById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> getProjectIds() {
        return projectInstanceRepository.findAllProjectId();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectInstance> getProjectInstances() {
        return searchProjectInstances(List.of(), List.of());
    }

    @Override
    @Transactional(readOnly = true)
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
    public ProjectInstance update(long id, List<Long> tagIds) {
        ProjectInstance projectInstance = getProjectInstance(id);

        projectInstance.setTagIds(tagIds);

        return projectInstanceRepository.save(projectInstance);
    }

    @Override
    public ProjectInstance update(long id, Status status) {
        ProjectInstance projectInstance = getProjectInstance(id);

        projectInstance.setStatus(status);

        return projectInstanceRepository.save(projectInstance);
    }

    @Override
    public ProjectInstance update(
        long id, String description, String name, Status status, List<Long> tagIds, int version) {

        ProjectInstance projectInstance = getProjectInstance(id);

        projectInstance.setDescription(description);
        projectInstance.setName(name);
        projectInstance.setStatus(status);
        projectInstance.setTagIds(tagIds);
        projectInstance.setVersion(version);

        return projectInstanceRepository.save(projectInstance);
    }
}
