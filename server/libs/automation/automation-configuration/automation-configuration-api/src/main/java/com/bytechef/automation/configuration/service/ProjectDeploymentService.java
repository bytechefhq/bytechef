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

package com.bytechef.automation.configuration.service;

import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.platform.configuration.domain.Environment;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * @author Ivica Cardic
 */
public interface ProjectDeploymentService {

    ProjectDeployment create(ProjectDeployment projectDeployment);

    void delete(long id);

    /**
     * Retrieves a project deployment by id without throwing when it does not exist.
     *
     * <p>
     * Callers evaluating an authorization decision must use this rather than {@link #getProjectDeployment(long)}: an
     * exception raised while a {@code @PreAuthorize} expression is being evaluated escapes as a server error instead of
     * an authorization verdict.
     * </p>
     *
     * @param id the project deployment id
     * @return the project deployment, if one exists
     */
    Optional<ProjectDeployment> fetchProjectDeployment(long id);

    Optional<ProjectDeployment> fetchProjectDeployment(long projectId, Environment environment);

    /**
     * {@link #fetchProjectDeployment(long, Environment)} assumes one deployment per project+environment, which only
     * holds because copy-mode gives each connected user their own private project. A catalog project shared by many
     * referencing connected users has one {@link ProjectDeployment} per (project, connected user), disambiguated by
     * name rather than environment.
     */
    Optional<ProjectDeployment> fetchProjectDeploymentByName(long projectId, String name);

    Optional<ProjectDeployment> fetchProjectDeployment(UUID uuid, Environment environment);

    /**
     * Returns every project deployment of the given project, including the system deployments backing API collections,
     * MCP servers and A2A servers, which {@link #getProjectDeployments(long)} hides from display surfaces.
     *
     * @param projectId the id of the project
     * @return every project deployment of the given project
     */
    List<ProjectDeployment> getAllProjectDeployments(long projectId);

    ProjectDeployment getProjectDeployment(long id);

    ProjectDeployment getProjectDeployment(long projectId, Environment environment);

    long getProjectDeploymentId(long projectId, Environment environment);

    List<ProjectDeployment> getProjectDeployments();

    List<ProjectDeployment> getProjectDeployments(List<Long> ids);

    List<ProjectDeployment> getProjectDeployments(long projectId);

    List<ProjectDeployment> getProjectDeployments(
        Boolean embedded, Environment environment, Long projectId, Long tagId, Long workspaceId);

    boolean isProjectDeploymentEnabled(long projectDeploymentId);

    ProjectDeployment update(long id, List<Long> tagIds);

    ProjectDeployment update(ProjectDeployment projectDeployment);

    void disableAllProjectDeployments();

    void updateEnabled(long id, boolean enabled);
}
