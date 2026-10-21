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

import com.bytechef.automation.configuration.ownership.OwnerContributorMissingException;
import com.bytechef.automation.configuration.ownership.ProjectDeploymentOwner;
import com.bytechef.automation.configuration.ownership.ProjectDeploymentOwnerContributor;
import com.bytechef.automation.configuration.ownership.ProjectOwner;
import com.bytechef.automation.configuration.ownership.ProjectOwnerContributor;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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
    private static final List<String> PROJECT_DEPLOYMENT_ID_COLUMNS = List.of("project_deployment.id");
    private static final List<String> PROJECT_ID_COLUMNS = List.of("project.id", "project_deployment.project_id");

    private final boolean enterpriseEdition;
    private final JdbcClient jdbcClient;
    private final List<ProjectDeploymentOwnerContributor> projectDeploymentOwnerContributors;
    private final List<ProjectOwnerContributor> projectOwnerContributors;

    @SuppressFBWarnings("CT_CONSTRUCTOR_THROW")
    public ProjectOwnershipServiceImpl(
        JdbcClient jdbcClient, List<ProjectDeploymentOwnerContributor> projectDeploymentOwnerContributors,
        List<ProjectOwnerContributor> projectOwnerContributors) {

        this(jdbcClient, projectDeploymentOwnerContributors, projectOwnerContributors, false);
    }

    @SuppressFBWarnings("CT_CONSTRUCTOR_THROW")
    public ProjectOwnershipServiceImpl(
        JdbcClient jdbcClient, List<ProjectDeploymentOwnerContributor> projectDeploymentOwnerContributors,
        List<ProjectOwnerContributor> projectOwnerContributors, boolean enterpriseEdition) {

        this.enterpriseEdition = enterpriseEdition;
        this.jdbcClient = jdbcClient;
        this.projectDeploymentOwnerContributors = List.copyOf(projectDeploymentOwnerContributors);
        this.projectOwnerContributors = List.copyOf(projectOwnerContributors);

        validateClaimConditions();
    }

    @Override
    public void afterSingletonsInstantiated() {
        List<ProjectOwner> projectOwnersWithoutContributor = getProjectOwnersWithoutContributor(
            EnumSet.allOf(ProjectOwner.class));
        List<ProjectDeploymentOwner> projectDeploymentOwnersWithoutContributor =
            getProjectDeploymentOwnersWithoutContributor(EnumSet.allOf(ProjectDeploymentOwner.class));

        List<ProjectOwner> missingProjectOwners = projectOwnersWithoutContributor.stream()
            .filter(projectOwner -> (enterpriseEdition && EMBEDDED_PROJECT_OWNERS.contains(projectOwner)) ||
                hasRows(projectOwner.getOwnerTables()))
            .toList();
        List<ProjectDeploymentOwner> missingProjectDeploymentOwners = projectDeploymentOwnersWithoutContributor
            .stream()
            .filter(projectDeploymentOwner -> (enterpriseEdition &&
                EMBEDDED_PROJECT_DEPLOYMENT_OWNERS.contains(projectDeploymentOwner)) ||
                hasRows(projectDeploymentOwner.getOwnerTables()))
            .toList();

        if (!missingProjectOwners.isEmpty() || !missingProjectDeploymentOwners.isEmpty()) {
            throw new OwnerContributorMissingException(
                "No owner contributor is loaded for project owners " + missingProjectOwners +
                    " and project deployment owners " + missingProjectDeploymentOwners + ", which the enterprise " +
                    "edition requires or whose owned data would show up as regular projects and deployments",
                missingProjectOwners, missingProjectDeploymentOwners);
        }

        if (!projectOwnersWithoutContributor.isEmpty() || !projectDeploymentOwnersWithoutContributor.isEmpty()) {
            log.info(
                "No owner contributor is loaded for project owners {} and project deployment owners {}, so their " +
                    "projects and deployments are not hidden",
                projectOwnersWithoutContributor, projectDeploymentOwnersWithoutContributor);
        }
    }

    @Override
    public String getExcludeProjectsCondition(Set<ProjectOwner> projectOwners, String projectIdColumn) {
        checkIdColumn(PROJECT_ID_COLUMNS, projectIdColumn);

        List<String> conditions = projectOwnerContributors.stream()
            .filter(contributor -> projectOwners.contains(contributor.getProjectOwner()))
            .map(contributor -> contributor.getProjectClaimCondition(projectIdColumn))
            .toList();

        return exclude(conditions);
    }

    @Override
    public String getSelectProjectDeploymentsCondition(
        ProjectDeploymentOwner projectDeploymentOwner, String projectDeploymentIdColumn) {

        checkIdColumn(PROJECT_DEPLOYMENT_ID_COLUMNS, projectDeploymentIdColumn);

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

        checkIdColumn(PROJECT_DEPLOYMENT_ID_COLUMNS, projectDeploymentIdColumn);

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

    private boolean hasRows(List<String> ownerTables) {
        for (String ownerTable : ownerTables) {
            try {
                List<String> qualifiedOwnerTables = jdbcClient
                    .sql(
                        "SELECT table_schema, table_name FROM information_schema.tables WHERE " +
                            "LOWER(table_name) = LOWER(?)")
                    .param(ownerTable)
                    .query((resultSet, rowNum) -> quoteIdentifier(resultSet.getString("table_schema")) + "." +
                        quoteIdentifier(resultSet.getString("table_name")))
                    .list();

                for (String qualifiedOwnerTable : qualifiedOwnerTables) {
                    if (jdbcClient
                        .sql("SELECT EXISTS (SELECT 1 FROM " + qualifiedOwnerTable + ")")
                        .query(Boolean.class)
                        .single()) {

                        return true;
                    }
                }
            } catch (DataAccessException dataAccessException) {
                throw new IllegalStateException(
                    "Checking the owner table " + ownerTable + " for rows failed", dataAccessException);
            }
        }

        return false;
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

    private void validateClaimConditions() {
        for (ProjectOwnerContributor projectOwnerContributor : projectOwnerContributors) {
            PROJECT_ID_COLUMNS.forEach(projectOwnerContributor::getProjectClaimCondition);
        }

        for (ProjectDeploymentOwnerContributor projectDeploymentOwnerContributor : projectDeploymentOwnerContributors) {
            PROJECT_DEPLOYMENT_ID_COLUMNS
                .forEach(projectDeploymentOwnerContributor::getProjectDeploymentClaimCondition);
        }
    }

    private static void checkIdColumn(List<String> idColumns, String idColumn) {
        if (!idColumns.contains(idColumn)) {
            throw new IllegalArgumentException(
                "The id column " + idColumn + " is not one of " + idColumns + " that the owner conditions are " +
                    "validated against at startup");
        }
    }

    private static String quoteIdentifier(String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }

    private static String exclude(List<String> conditions) {
        if (conditions.isEmpty()) {
            return MATCH_EVERYTHING;
        }

        return "(" + conditions.stream()
            .map(condition -> "NOT COALESCE((" + condition + "), FALSE)")
            .collect(Collectors.joining(" AND ")) + ")";
    }

    private static String combine(List<String> conditions) {
        return "(" + conditions.stream()
            .map(condition -> "(" + condition + ")")
            .collect(Collectors.joining(" OR ")) + ")";
    }
}
