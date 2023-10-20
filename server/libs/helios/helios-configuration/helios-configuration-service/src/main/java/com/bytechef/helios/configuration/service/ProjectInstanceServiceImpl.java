
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
import com.bytechef.helios.configuration.repository.ProjectInstanceRepository;
import com.bytechef.helios.configuration.domain.ProjectInstance;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

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
        Assert.notNull(projectInstance, "'projectInstance' must not be notNull");

        Assert.isNull(projectInstance.getId(), "'id' must be notNull");
        Assert.notNull(projectInstance.getProjectId(), "'projectId' must not be notNull");
        Assert.notNull(projectInstance.getName(), "'projectId' must not be notNull");

        projectInstance.setEnabled(false);

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
        return getProjectInstances(null, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectInstance> getProjectInstances(Long projectId, Long tagId) {
        Iterable<ProjectInstance> projectInstanceIterable;

        if (projectId == null && tagId == null) {
            projectInstanceIterable = projectInstanceRepository.findAll(Sort.by("name"));
        } else if (projectId != null && tagId == null) {
            projectInstanceIterable = projectInstanceRepository.findAllByProjectIdOrderByName(projectId);
        } else if (projectId == null) {
            projectInstanceIterable = projectInstanceRepository.findAllByTagIdOrderByName(tagId);
        } else {
            projectInstanceIterable = projectInstanceRepository.findAllByProjectIdAndTagIdOrderByName(projectId, tagId);
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
    @SuppressFBWarnings("NP")
    public ProjectInstance update(ProjectInstance projectInstance) {
        Assert.notNull(projectInstance, "'projectInstance' must not be notNull");

        Assert.notNull(projectInstance.getId(), "'id' must not be null");
        Assert.notNull(projectInstance.getProjectId(), "'projectId' must not be null");
        Assert.notNull(projectInstance.getName(), "'projectId' must not be null");

        ProjectInstance curProjectInstance = getProjectInstance(projectInstance.getId());

        curProjectInstance.setDescription(projectInstance.getDescription());
        curProjectInstance.setName(projectInstance.getName());
        curProjectInstance.setEnabled(projectInstance.isEnabled());
        curProjectInstance.setTagIds(projectInstance.getTagIds());
        curProjectInstance.setVersion(projectInstance.getVersion());

        return projectInstanceRepository.save(curProjectInstance);
    }

    @Override
    public void updateEnabled(long id, boolean enabled) {
        ProjectInstance projectInstance = getProjectInstance(id);

        projectInstance.setEnabled(enabled);

        projectInstanceRepository.save(projectInstance);
    }
}
