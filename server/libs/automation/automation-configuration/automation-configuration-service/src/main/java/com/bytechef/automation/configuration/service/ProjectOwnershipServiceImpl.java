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
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
public class ProjectOwnershipServiceImpl implements ProjectOwnershipService, SmartInitializingSingleton {

    private static final Logger log = LoggerFactory.getLogger(ProjectOwnershipServiceImpl.class);

    private static final int CHUNK_SIZE = 1000;
    private static final String MATCH_EVERYTHING = "1 = 1";
    private static final String MATCH_NOTHING = "1 = 0";
    private static final Set<ProjectOwner> EMBEDDED_PROJECT_OWNERS = EnumSet.of(
        ProjectOwner.EMBEDDED_CATALOG, ProjectOwner.EMBEDDED_CONNECTED_USER);
    private static final Set<ProjectDeploymentOwner> EMBEDDED_PROJECT_DEPLOYMENT_OWNERS = EnumSet.of(
        ProjectDeploymentOwner.EMBEDDED);

    private final boolean enterpriseEdition;
    private final JdbcClient jdbcClient;
    private final List<ProjectDeploymentOwnerContributor> projectDeploymentOwnerContributors;
    private final List<ProjectOwnerContributor> projectOwnerContributors;

    public ProjectOwnershipServiceImpl(
        JdbcClient jdbcClient, List<ProjectDeploymentOwnerContributor> projectDeploymentOwnerContributors,
        List<ProjectOwnerContributor> projectOwnerContributors) {

        this(jdbcClient, projectDeploymentOwnerContributors, projectOwnerContributors, false);
    }

    public ProjectOwnershipServiceImpl(
        JdbcClient jdbcClient, List<ProjectDeploymentOwnerContributor> projectDeploymentOwnerContributors,
        List<ProjectOwnerContributor> projectOwnerContributors, boolean enterpriseEdition) {

        this.enterpriseEdition = enterpriseEdition;
        this.jdbcClient = jdbcClient;
        this.projectDeploymentOwnerContributors = List.copyOf(projectDeploymentOwnerContributors);
        this.projectOwnerContributors = List.copyOf(projectOwnerContributors);
    }

    @Override
    public void afterSingletonsInstantiated() {
        List<ProjectOwner> projectOwnersWithoutContributor = getProjectOwnersWithoutContributor(
            EMBEDDED_PROJECT_OWNERS);
        List<ProjectDeploymentOwner> projectDeploymentOwnersWithoutContributor =
            getProjectDeploymentOwnersWithoutContributor(EMBEDDED_PROJECT_DEPLOYMENT_OWNERS);

        if (!enterpriseEdition) {
            if ((!projectOwnersWithoutContributor.isEmpty() || !projectDeploymentOwnersWithoutContributor.isEmpty()) &&
                hasAppliedEmbeddedChangesets()) {

                log.warn(
                    "Embedded data exists but the embedded owner contributors are not loaded, so projects of owners " +
                        "{} and deployments of owners {} are not hidden",
                    projectOwnersWithoutContributor, projectDeploymentOwnersWithoutContributor);
            }

            return;
        }

        if (!projectOwnersWithoutContributor.isEmpty() || !projectDeploymentOwnersWithoutContributor.isEmpty()) {
            throw new IllegalStateException(
                "The enterprise edition requires the embedded owner contributors, but project owners " +
                    projectOwnersWithoutContributor + " and project deployment owners " +
                    projectDeploymentOwnersWithoutContributor + " have none");
        }
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

        return combine(conditions);
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
    @Transactional(readOnly = true)
    public Set<Long> getOwnedProjectIds(Collection<Long> projectIds, ProjectOwner projectOwner) {
        return getOwnedProjectIds(projectIds, Set.of(projectOwner));
    }

    @Override
    @Transactional(readOnly = true)
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

        String query = "SELECT project.id FROM project WHERE project.id IN (:ids) AND " + combine(conditions);

        Set<Long> ownedProjectIds = new HashSet<>();

        List<Long> distinctProjectIds = projectIds.stream()
            .distinct()
            .toList();

        for (int fromIndex = 0; fromIndex < distinctProjectIds.size(); fromIndex += CHUNK_SIZE) {
            List<Long> projectIdsChunk = distinctProjectIds.subList(
                fromIndex, Math.min(fromIndex + CHUNK_SIZE, distinctProjectIds.size()));

            ownedProjectIds.addAll(
                jdbcClient
                    .sql(query)
                    .param("ids", projectIdsChunk)
                    .query(Long.class)
                    .list());
        }

        return ownedProjectIds;
    }

    private boolean hasAppliedEmbeddedChangesets() {
        try {
            Long count = jdbcClient
                .sql("SELECT COUNT(*) FROM databasechangelog WHERE filename LIKE '%/changelog/embedded/%'")
                .query(Long.class)
                .single();

            return count > 0;
        } catch (DataAccessException dataAccessException) {
            return false;
        }
    }

    private List<ProjectDeploymentOwner> getProjectDeploymentOwnersWithoutContributor(
        Set<ProjectDeploymentOwner> projectDeploymentOwners) {

        Set<ProjectDeploymentOwner> contributedProjectDeploymentOwners = projectDeploymentOwnerContributors.stream()
            .map(ProjectDeploymentOwnerContributor::getProjectDeploymentOwner)
            .collect(Collectors.toSet());

        return projectDeploymentOwners.stream()
            .filter(projectDeploymentOwner -> !contributedProjectDeploymentOwners.contains(projectDeploymentOwner))
            .sorted()
            .toList();
    }

    private List<ProjectOwner> getProjectOwnersWithoutContributor(Set<ProjectOwner> projectOwners) {
        Set<ProjectOwner> contributedProjectOwners = projectOwnerContributors.stream()
            .map(ProjectOwnerContributor::getProjectOwner)
            .collect(Collectors.toSet());

        return projectOwners.stream()
            .filter(projectOwner -> !contributedProjectOwners.contains(projectOwner))
            .sorted()
            .toList();
    }

    private static String exclude(List<String> conditions) {
        if (conditions.isEmpty()) {
            return MATCH_EVERYTHING;
        }

        return "(" + conditions.stream()
            .map(condition -> "NOT (" + condition + ")")
            .collect(Collectors.joining(" AND ")) + ")";
    }

    private static String combine(List<String> conditions) {
        return "(" + conditions.stream()
            .map(condition -> "(" + condition + ")")
            .collect(Collectors.joining(" OR ")) + ")";
    }
}
