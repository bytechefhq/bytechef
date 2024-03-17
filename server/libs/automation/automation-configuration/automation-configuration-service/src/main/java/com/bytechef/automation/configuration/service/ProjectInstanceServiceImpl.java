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

import com.bytechef.automation.configuration.domain.ProjectInstance;
import com.bytechef.automation.configuration.repository.ProjectInstanceRepository;
import com.bytechef.commons.util.OptionalUtils;
import java.util.List;

import com.bytechef.platform.constant.Environment;
import org.apache.commons.lang3.Validate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        Validate.notNull(projectInstance, "'projectInstance' must not be null");
        Validate.isTrue(projectInstance.getId() == null, "'id' must be null");
        Validate.notNull(projectInstance.getProjectId(), "'projectId' must not be null");
        Validate.notNull(projectInstance.getName(), "'projectId' must not be null");

        projectInstance.setEnabled(false);

        return projectInstanceRepository.save(projectInstance);
    }

    @Override
    public void delete(long id) {
        projectInstanceRepository.deleteById(id);
    }

    @Override
    public boolean isProjectInstanceEnabled(long projectInstanceId) {
        ProjectInstance projectInstance = getProjectInstance(projectInstanceId);

        return projectInstance.isEnabled();
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
        return getProjectInstances(null, null, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectInstance> getProjectInstances(long projectId) {
        return getProjectInstances(null, projectId, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectInstance> getProjectInstances(Environment environment, Long projectId, Long tagId) {
        return projectInstanceRepository.findAllProjectInstances(environment, projectId, tagId);
    }

    @Override
    public ProjectInstance update(long id, List<Long> tagIds) {
        ProjectInstance projectInstance = getProjectInstance(id);

        projectInstance.setTagIds(tagIds);

        return projectInstanceRepository.save(projectInstance);
    }

    @Override
    public ProjectInstance update(ProjectInstance projectInstance) {
        Validate.notNull(projectInstance, "'projectInstance' must not be null");
        Validate.notNull(projectInstance.getProjectId(), "'projectId' must not be null");
        Validate.notNull(projectInstance.getName(), "'projectId' must not be null");

        ProjectInstance curProjectInstance = getProjectInstance(Validate.notNull(projectInstance.getId(), "id"));

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
