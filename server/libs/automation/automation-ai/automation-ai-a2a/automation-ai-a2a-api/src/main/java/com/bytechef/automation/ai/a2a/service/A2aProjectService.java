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
import java.util.List;
import java.util.Optional;

/**
 * Service interface for managing {@link A2aProject} entities.
 *
 * @author Ivica Cardic
 */
public interface A2aProjectService {

    A2aProject create(A2aProject a2aProject);

    A2aProject create(long projectDeploymentId, long a2aServerId);

    void delete(long a2aProjectId);

    Optional<A2aProject> fetchA2aProject(long a2aProjectId);

    A2aProject getA2aProject(long a2aProjectId);

    List<A2aProject> getA2aProjects();

    List<A2aProject> getA2aServerA2aProjects(long a2aServerId);

    List<A2aProject> getProjectDeploymentA2aProjects(long projectDeploymentId);

    A2aProject update(A2aProject a2aProject);
}
