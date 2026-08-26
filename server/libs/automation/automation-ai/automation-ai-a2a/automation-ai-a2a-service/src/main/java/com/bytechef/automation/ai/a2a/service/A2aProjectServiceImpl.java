/*
 * Copyright 2025 ByteChef
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

package com.bytechef.automation.ai.a2a.service;

import com.bytechef.automation.ai.a2a.domain.A2aProject;
import com.bytechef.automation.ai.a2a.repository.A2aProjectRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of the {@link A2aProjectService} interface.
 *
 * @author Ivica Cardic
 */
@Service
@Transactional
public class A2aProjectServiceImpl implements A2aProjectService {

    private final A2aProjectRepository a2aProjectRepository;

    public A2aProjectServiceImpl(A2aProjectRepository a2aProjectRepository) {
        this.a2aProjectRepository = a2aProjectRepository;
    }

    @Override
    public A2aProject create(A2aProject a2aProject) {
        return a2aProjectRepository.save(a2aProject);
    }

    @Override
    public A2aProject create(long projectDeploymentId, long a2aServerId) {
        A2aProject a2aProject = new A2aProject(projectDeploymentId, a2aServerId);

        return create(a2aProject);
    }

    @Override
    public void delete(long a2aProjectId) {
        a2aProjectRepository.deleteById(a2aProjectId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<A2aProject> fetchA2aProject(long a2aProjectId) {
        return a2aProjectRepository.findById(a2aProjectId);
    }

    @Override
    @Transactional(readOnly = true)
    public A2aProject getA2aProject(long a2aProjectId) {
        return a2aProjectRepository.findById(a2aProjectId)
            .orElseThrow(() -> new IllegalArgumentException("A2A project with id " + a2aProjectId + " not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<A2aProject> getA2aProjects() {
        return a2aProjectRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<A2aProject> getA2aServerA2aProjects(long a2aServerId) {
        return a2aProjectRepository.findAllByA2aServerId(a2aServerId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<A2aProject> getProjectDeploymentA2aProjects(long projectDeploymentId) {
        return a2aProjectRepository.findAllByProjectDeploymentId(projectDeploymentId);
    }

    @Override
    public A2aProject update(A2aProject a2aProject) {
        A2aProject currentA2aProject = a2aProjectRepository.findById(a2aProject.getId())
            .orElseThrow(
                () -> new IllegalArgumentException("A2A project with id " + a2aProject.getId() + " not found"));

        currentA2aProject.setProjectDeploymentId(a2aProject.getProjectDeploymentId());
        currentA2aProject.setA2aServerId(a2aProject.getA2aServerId());
        currentA2aProject.setVersion(a2aProject.getVersion());

        return a2aProjectRepository.save(currentA2aProject);
    }
}
