/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.bytechef.ee.embedded.configuration.config.IntegrationIntTestConfiguration;
import com.bytechef.ee.embedded.configuration.config.IntegrationIntTestConfigurationSharedMocks;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import java.util.Objects;
import javax.sql.DataSource;
import liquibase.integration.spring.SpringLiquibase;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = IntegrationIntTestConfiguration.class,
    properties = "spring.liquibase.contexts=configuration,user")
@Import(PostgreSQLContainerConfiguration.class)
@IntegrationIntTestConfigurationSharedMocks
class ConnectedUserReferenceDeploymentBackfillIntTest {

    private static final String BACKFILL_CHANGESET_ID = "20261015120000-3";
    private static final String CHANGELOG =
        "classpath:config/liquibase/changelog/embedded/configuration/" +
            "20261015120000_embedded_configuration_added_connected_user_reference_deployment.xml";

    private static final int DEVELOPMENT = 0;
    private static final int PRODUCTION = 2;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ResourceLoader resourceLoader;

    @AfterEach
    void afterEach() {
        jdbcTemplate.update("DELETE FROM connected_user_reference_deployment");
        jdbcTemplate.update("DELETE FROM project_deployment");
        jdbcTemplate.update("DELETE FROM project");
        jdbcTemplate.update("DELETE FROM workspace");
        jdbcTemplate.update("DELETE FROM connected_user");
    }

    @Test
    void testBackfillRelatesEachLegacyReferenceDeploymentToItsConnectedUser() throws Exception {
        long workspaceId = insertWorkspace();

        long catalogProjectId = insertProject(workspaceId, "__EMBEDDED_AUTOMATION__Catalog");
        long ordinaryProjectId = insertProject(workspaceId, "Ordinary");

        long productionConnectedUserId = insertConnectedUser("acme", PRODUCTION);
        long developmentConnectedUserId = insertConnectedUser("acme", DEVELOPMENT);
        long underscoredConnectedUserId = insertConnectedUser("a__b", PRODUCTION);

        insertConnectedUser("no-deployment", PRODUCTION);

        long productionDeploymentId = insertProjectDeployment(
            catalogProjectId, "__EMBEDDED__acme__PRODUCTION", PRODUCTION);
        long developmentDeploymentId = insertProjectDeployment(
            catalogProjectId, "__EMBEDDED__acme__DEVELOPMENT", DEVELOPMENT);
        long underscoredDeploymentId = insertProjectDeployment(
            catalogProjectId, "__EMBEDDED__a__b__PRODUCTION", PRODUCTION);

        insertProjectDeployment(catalogProjectId, "Manual deployment", PRODUCTION);
        insertProjectDeployment(catalogProjectId, "__EMBEDDED__ghost__PRODUCTION", PRODUCTION);
        insertProjectDeployment(catalogProjectId, "__EMBEDDED__acme__DEVELOPMENT", PRODUCTION);
        insertProjectDeployment(ordinaryProjectId, "__EMBEDDED__acme__PRODUCTION", PRODUCTION);

        runChangelog();

        assertThat(getRelations()).containsExactlyInAnyOrder(
            tuple(productionConnectedUserId, catalogProjectId, productionDeploymentId),
            tuple(developmentConnectedUserId, catalogProjectId, developmentDeploymentId),
            tuple(underscoredConnectedUserId, catalogProjectId, underscoredDeploymentId));

        runChangelog();

        assertThat(getRelations()).hasSize(3);

        jdbcTemplate.update("DELETE FROM connected_user WHERE id = ?", productionConnectedUserId);

        assertThat(getRelations()).containsExactlyInAnyOrder(
            tuple(developmentConnectedUserId, catalogProjectId, developmentDeploymentId),
            tuple(underscoredConnectedUserId, catalogProjectId, underscoredDeploymentId));
    }

    @Test
    void testBackfillRelatesADeploymentToOneConnectedUserWhenItsNameMatchesDuplicateConnectedUsers()
        throws Exception {

        long catalogProjectId = insertProject(insertWorkspace(), "__EMBEDDED_AUTOMATION__Catalog");

        long firstConnectedUserId = insertConnectedUser("duplicate", PRODUCTION);

        insertConnectedUser("duplicate", PRODUCTION);

        long firstDeploymentId = insertProjectDeployment(
            catalogProjectId, "__EMBEDDED__duplicate__PRODUCTION", PRODUCTION);

        insertProjectDeployment(catalogProjectId, "__EMBEDDED__duplicate__PRODUCTION", PRODUCTION);

        runChangelog();

        assertThat(getRelations()).containsExactly(tuple(firstConnectedUserId, catalogProjectId, firstDeploymentId));
    }

    private List<Tuple> getRelations() {
        return jdbcTemplate.query(
            "SELECT connected_user_id, project_id, project_deployment_id FROM connected_user_reference_deployment",
            (resultSet, rowNumber) -> tuple(
                resultSet.getLong("connected_user_id"), resultSet.getLong("project_id"),
                resultSet.getLong("project_deployment_id")));
    }

    private void runChangelog() throws Exception {
        jdbcTemplate.update("DELETE FROM databasechangelog WHERE id = ?", BACKFILL_CHANGESET_ID);

        SpringLiquibase springLiquibase = new SpringLiquibase();

        springLiquibase.setChangeLog(CHANGELOG);
        springLiquibase.setContexts("mono");
        springLiquibase.setDataSource(dataSource);
        springLiquibase.setResourceLoader(resourceLoader);

        springLiquibase.afterPropertiesSet();
    }

    private long insertConnectedUser(String externalId, int environment) {
        return Objects.requireNonNull(
            jdbcTemplate.queryForObject(
                """
                    INSERT INTO connected_user
                        (external_id, enabled, environment, created_date, created_by, last_modified_date,
                         last_modified_by, version)
                    VALUES (?, true, ?, now(), 'test', now(), 'test', 0)
                    RETURNING id
                    """,
                Long.class, externalId, environment),
            "connected user id");
    }

    private long insertWorkspace() {
        return Objects.requireNonNull(
            jdbcTemplate.queryForObject(
                """
                    INSERT INTO workspace (name, created_date, created_by, last_modified_date, last_modified_by, version)
                    VALUES ('test', now(), 'test', now(), 'test', 0)
                    RETURNING id
                    """,
                Long.class),
            "workspace id");
    }

    private long insertProject(long workspaceId, String name) {
        return Objects.requireNonNull(
            jdbcTemplate.queryForObject(
                """
                    INSERT INTO project
                        (name, uuid, workspace_id, created_date, created_by, last_modified_date, last_modified_by,
                         version)
                    VALUES (?, gen_random_uuid(), ?, now(), 'test', now(), 'test', 0)
                    RETURNING id
                    """,
                Long.class, name, workspaceId),
            "project id");
    }

    private long insertProjectDeployment(long projectId, String name, int environment) {
        return Objects.requireNonNull(
            jdbcTemplate.queryForObject(
                """
                    INSERT INTO project_deployment
                        (project_id, project_version, name, enabled, environment, created_date, created_by,
                         last_modified_date, last_modified_by, version)
                    VALUES (?, 1, ?, true, ?, now(), 'test', now(), 'test', 0)
                    RETURNING id
                    """,
                Long.class, projectId, name, environment),
            "project deployment id");
    }
}
