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

import com.bytechef.automation.configuration.ownership.ProjectDeploymentOwner;
import com.bytechef.automation.configuration.ownership.ProjectDeploymentOwnerContributor;
import com.bytechef.automation.configuration.ownership.ProjectOwner;
import com.bytechef.automation.configuration.ownership.ProjectOwnerContributor;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional(readOnly = true)
public class ProjectOwnershipServiceImpl implements ProjectOwnershipService {

    private static final String MATCH_EVERYTHING = "1 = 1";
    private static final String MATCH_NOTHING = "1 = 0";

    private final JdbcClient jdbcClient;
    private final List<ProjectDeploymentOwnerContributor> projectDeploymentOwnerContributors;
    private final List<ProjectOwnerContributor> projectOwnerContributors;

    public ProjectOwnershipServiceImpl(
        JdbcClient jdbcClient, List<ProjectDeploymentOwnerContributor> projectDeploymentOwnerContributors,
        List<ProjectOwnerContributor> projectOwnerContributors) {

        this.jdbcClient = jdbcClient;
        this.projectDeploymentOwnerContributors = List.copyOf(projectDeploymentOwnerContributors);
        this.projectOwnerContributors = List.copyOf(projectOwnerContributors);
    }

    @Override
    public String getExcludeProjectsCondition(Set<ProjectOwner> projectOwners, String projectIdColumn) {
        List<String> conditions = projectOwnerContributors.stream()
            .filter(contributor -> projectOwners.contains(contributor.getProjectOwner()))
            .map(contributor -> contributor.getProjectClaimCondition(projectIdColumn))
            .toList();

        return exclude(conditions);
    }

    @Override
    public String getSelectProjectDeploymentsCondition(
        ProjectDeploymentOwner projectDeploymentOwner, String projectDeploymentIdColumn) {

        List<String> conditions = projectDeploymentOwnerContributors.stream()
            .filter(contributor -> contributor.getProjectDeploymentOwner() == projectDeploymentOwner)
            .map(contributor -> contributor.getProjectDeploymentClaimCondition(projectDeploymentIdColumn))
            .toList();

        if (conditions.isEmpty()) {
            return MATCH_NOTHING;
        }

        return "(" + String.join(" OR ", conditions) + ")";
    }

    @Override
    public String getExcludeProjectDeploymentsCondition(
        Set<ProjectDeploymentOwner> projectDeploymentOwners, String projectDeploymentIdColumn) {

        List<String> conditions = projectDeploymentOwnerContributors.stream()
            .filter(contributor -> projectDeploymentOwners.contains(contributor.getProjectDeploymentOwner()))
            .map(contributor -> contributor.getProjectDeploymentClaimCondition(projectDeploymentIdColumn))
            .toList();

        return exclude(conditions);
    }

    @Override
    public Set<Long> getOwnedProjectIds(Collection<Long> projectIds, ProjectOwner projectOwner) {
        return getOwnedProjectIds(projectIds, Set.of(projectOwner));
    }

    @Override
    public Set<Long> getOwnedProjectIds(Collection<Long> projectIds, Set<ProjectOwner> projectOwners) {
        if (projectIds.isEmpty() || projectOwners.isEmpty()) {
            return Set.of();
        }

        List<String> conditions = projectOwnerContributors.stream()
            .filter(contributor -> projectOwners.contains(contributor.getProjectOwner()))
            .map(contributor -> contributor.getProjectClaimCondition("project.id"))
            .toList();

        if (conditions.isEmpty()) {
            return Set.of();
        }

        return new HashSet<>(
            jdbcClient
                .sql("SELECT project.id FROM project WHERE project.id IN (:ids) AND (" +
                    String.join(" OR ", conditions) + ")")
                .param("ids", projectIds)
                .query(Long.class)
                .list());
    }

    private static String exclude(List<String> conditions) {
        if (conditions.isEmpty()) {
            return MATCH_EVERYTHING;
        }

        return "NOT (" + String.join(" OR ", conditions) + ")";
    }
}
