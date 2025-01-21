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

import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.repository.ProjectDeploymentRepository;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.platform.constant.Environment;
import java.util.List;
import org.apache.commons.lang3.Validate;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional
public class ProjectDeploymentServiceImpl implements ProjectDeploymentService {

    private final ProjectDeploymentRepository projectDeploymentRepository;

    public ProjectDeploymentServiceImpl(ProjectDeploymentRepository projectDeploymentRepository) {

        this.projectDeploymentRepository = projectDeploymentRepository;
    }

    @Override
    public ProjectDeployment create(ProjectDeployment projectDeployment) {
        Validate.notNull(projectDeployment, "'projectDeployment' must not be null");
        Validate.isTrue(projectDeployment.getId() == null, "'id' must be null");
        Validate.notNull(projectDeployment.getProjectId(), "'projectId' must not be null");
        Validate.notNull(projectDeployment.getName(), "'projectId' must not be null");

        projectDeployment.setEnabled(false);

        return projectDeploymentRepository.save(projectDeployment);
    }

    @Override
    public void delete(long id) {
        projectDeploymentRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectDeployment getProjectDeployment(long id) {
        return OptionalUtils.get(projectDeploymentRepository.findById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> getProjectDeploymentProjectIds() {
        return projectDeploymentRepository.findAllProjectDeploymentProjectIds();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectDeployment> getProjectDeployments() {
        return getProjectDeployments(null, null, null, null, List.of());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectDeployment> getProjectDeployments(long projectId) {
        return getProjectDeployments(null, null, projectId, null, List.of());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectDeployment> getProjectDeployments(
        Long workspaceId, Environment environment, Long projectId, Long tagId,
        @NonNull List<Long> excludeProjectDeploymentIds) {

        return projectDeploymentRepository.findAllProjectDeployments(
            workspaceId, environment == null ? null : environment.ordinal(), projectId, tagId,
            excludeProjectDeploymentIds);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isProjectDeploymentEnabled(long projectDeploymentId) {
        return projectDeploymentRepository.findById(projectDeploymentId)
            .map(projectDeployment -> projectDeployment.isEnabled())
            .orElse(false);
    }

    @Override
    public ProjectDeployment update(long id, List<Long> tagIds) {
        ProjectDeployment projectDeployment = getProjectDeployment(id);

        projectDeployment.setTagIds(tagIds);

        return projectDeploymentRepository.save(projectDeployment);
    }

    @Override
    public ProjectDeployment update(ProjectDeployment projectDeployment) {
        Validate.notNull(projectDeployment, "'projectDeployment' must not be null");
        Validate.notNull(projectDeployment.getProjectId(), "'projectId' must not be null");
        Validate.notNull(projectDeployment.getName(), "'name' must not be null");

        ProjectDeployment curProjectDeployment =
            getProjectDeployment(Validate.notNull(projectDeployment.getId(), "id"));

        curProjectDeployment.setDescription(projectDeployment.getDescription());
        curProjectDeployment.setEnabled(projectDeployment.isEnabled());
        curProjectDeployment.setName(projectDeployment.getName());
        curProjectDeployment.setProjectVersion(projectDeployment.getProjectVersion());
        curProjectDeployment.setTagIds(projectDeployment.getTagIds());
        curProjectDeployment.setVersion(projectDeployment.getVersion());

        return projectDeploymentRepository.save(curProjectDeployment);
    }

    @Override
    public void updateEnabled(long id, boolean enabled) {
        ProjectDeployment projectDeployment = getProjectDeployment(id);

        projectDeployment.setEnabled(enabled);

        projectDeploymentRepository.save(projectDeployment);
    }
}
