
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

package com.bytechef.helios.configuration.remote.client.service;

import com.bytechef.commons.webclient.LoadBalancedWebClient;
import com.bytechef.helios.configuration.domain.Project;
import com.bytechef.helios.configuration.service.ProjectService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * @author Ivica Cardic
 */
@Component
public class ProjectServiceClient implements ProjectService {

    private final LoadBalancedWebClient loadBalancedWebClient;

    @SuppressFBWarnings("EI")
    public ProjectServiceClient(LoadBalancedWebClient loadBalancedWebClient) {
        this.loadBalancedWebClient = loadBalancedWebClient;
    }

    @Override
    public Project addWorkflow(long id, String workflowId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long countProjects() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Project create(Project project) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<Project> fetchProject(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Project getWorkflowProject(String workflowId) {
        return loadBalancedWebClient.get(
            uriBuilder -> uriBuilder
                .host("configuration-service-app")
                .path("/internal/project-service/get-workflow-project/{workflowId}")
                .build(workflowId),
            Project.class);
    }

    @Override
    public Project getProject(long id) {
        return loadBalancedWebClient.get(
            uriBuilder -> uriBuilder
                .host("configuration-service-app")
                .path("/internal/project-service/get-project/{id}")
                .build(id),
            Project.class);
    }

    @Override
    public List<Project> getProjects() {
        return loadBalancedWebClient.get(
            uriBuilder -> uriBuilder
                .host("configuration-service-app")
                .path("/internal/project-service/get-projects")
                .build(),
            new ParameterizedTypeReference<List<Project>>() {});
    }

    @Override
    public List<Project> getProjects(List<Long> ids) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Project> getProjects(Long categoryId, List<Long> ids, Long tagId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Project update(long id, List<Long> tagIds) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Project update(Project project) {
        throw new UnsupportedOperationException();
    }
}
