
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
import com.bytechef.helios.configuration.domain.ProjectInstance;
import com.bytechef.helios.configuration.service.ProjectInstanceService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * @author Ivica Cardic
 */
@Component
public class ProjectInstanceServiceClient implements ProjectInstanceService {

    private final LoadBalancedWebClient loadBalancedWebClient;

    @SuppressFBWarnings("EI")
    public ProjectInstanceServiceClient(LoadBalancedWebClient loadBalancedWebClient) {
        this.loadBalancedWebClient = loadBalancedWebClient;
    }

    @Override
    public ProjectInstance create(ProjectInstance projectInstance) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<ProjectInstance> fetchJobProjectInstance(long jobId) {
        return Optional.ofNullable(
            loadBalancedWebClient.get(
                uriBuilder -> uriBuilder
                    .host("configuration-service-app")
                    .path("/api/internal/project-instance-service/fetch-project-instance/{jobId}")
                    .build(jobId),
                ProjectInstance.class));
    }

    @Override
    public ProjectInstance getProjectInstance(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Long> getProjectIds() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ProjectInstance> getProjectInstances() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ProjectInstance> getProjectInstances(Long projectId, Long tagId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ProjectInstance update(long id, List<Long> tagIds) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ProjectInstance update(ProjectInstance projectInstance) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateEnabled(long id, boolean enabled) {
        loadBalancedWebClient.get(
            uriBuilder -> uriBuilder
                .host("configuration-service-app")
                .path(
                    "/api/internal/project-instance-service/update-enabled/{id}/{enable}")
                .build(id, enabled));
    }
}
