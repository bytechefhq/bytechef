/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.ee.automation.configuration.config.EeAutomationConfigurationIntTestConfiguration;
import com.bytechef.ee.automation.configuration.domain.WorkspaceUser;
import com.bytechef.ee.automation.configuration.security.constant.WorkspaceRole;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import javax.sql.DataSource;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Runs {@link WorkspaceUserRepository} against a fresh PostgreSQL database via Testcontainers: verifies the RBAC
 * Liquibase migrations apply cleanly on a fresh install, and covers the per-environment finders and uniqueness
 * constraints.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = EeAutomationConfigurationIntTestConfiguration.class)
@ActiveProfiles("testint")
@Import(PostgreSQLContainerConfiguration.class)
class WorkspaceUserRepositoryIntTest {

    private static final String DEDUP_CHANGE_SET_ID = "20260819120001";
    private static final int DEVELOPMENT = 0;
    private static final int STAGING = 1;
    private static final int PRODUCTION = 2;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private WorkspaceUserRepository workspaceUserRepository;

    private JdbcTemplate jdbcTemplate;

    @Test
    void testRbacTablesExistAfterMigrations() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);

        // Assert table + column structure via information_schema rather than SELECT-with-tautology. A query like
        // `WHERE workspace_role IS NOT NULL OR workspace_role IS NULL` would pass even if workspace_role were a
        // wrong type (e.g., boolean) as long as it exists at all, because the tautology would always be true. The
        // expected data_type is asserted alongside the name, because the enum columns are stored as INT ordinals and
        // a migration that retyped one to text or boolean would keep reading and writing without complaint until an
        // ordinal comparison silently stopped matching.
        assertColumnExists(jdbc, "workspace_user", "workspace_role", "integer");
        assertColumnExists(jdbc, "workspace_user", "custom_role_id", "bigint");
        assertColumnExists(jdbc, "workspace_user", "environment", "integer");
        assertTableExists(jdbc, "custom_role");
        assertTableExists(jdbc, "custom_role_scope");

        // Sanity check that the tables are actually queryable (not just present in the catalog) — catches a subtle
        // case where a table is registered but the liquibase apply failed mid-way, leaving the table with bad
        // permissions or no primary key.
        Long workspaceUserCount = jdbc.queryForObject("SELECT COUNT(*) FROM workspace_user", Long.class);

        assertThat(workspaceUserCount).isNotNull();
    }

    /**
     * Asserts the column exists <em>with the expected PostgreSQL {@code data_type}</em>. The schema is pinned to
     * {@code current_schema()} so the exact count stays meaningful: {@code information_schema} spans every schema of
     * the database, and a second copy of the table anywhere — a tenant schema, a leftover from another test — would
     * otherwise make a count of one an accident.
     */
    private static void assertColumnExists(
        JdbcTemplate jdbc, String tableName, String columnName, String expectedDataType) {

        List<String> dataTypes = jdbc.queryForList(
            "SELECT data_type FROM information_schema.columns "
                + "WHERE table_schema = current_schema() AND table_name = ? AND column_name = ?",
            String.class, tableName, columnName);

        assertThat(dataTypes)
            .as(
                "column %s.%s must be registered exactly once after migrations, with data type %s", tableName,
                columnName, expectedDataType)
            .containsExactly(expectedDataType);
    }

    private static void assertTableExists(JdbcTemplate jdbc, String tableName) {
        Long tableCount = jdbc.queryForObject(
            "SELECT COUNT(*) FROM information_schema.tables "
                + "WHERE table_schema = current_schema() AND table_name = ? AND table_type = 'BASE TABLE'",
            Long.class, tableName);

        assertThat(tableCount)
            .as("table %s must be registered after migrations", tableName)
            .isEqualTo(1L);
    }

    @Test
    void testWorkspaceUserUniquenessIsEnforcedByThePartialIndexes() {
        // Uniqueness per member is split in two: one implicit row per member, and one explicit row per member per
        // environment. We assert via the PostgreSQL catalog rather than attempting a duplicate insert (which would
        // require the full FK chain).
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);

        assertIndexExists(jdbc, "uk_workspace_user_implicit");
        assertIndexExists(jdbc, "uk_workspace_user_explicit");
    }

    /**
     * Runs the dedup statement of changeset {@code 20260819120001} against seeded duplicates on real PostgreSQL.
     *
     * <p>
     * The statement is read out of the changelog rather than restated here, so the test exercises the SQL that actually
     * migrates a database. It runs against a throwaway copy of the relevant columns: the live table carries
     * {@code uk_workspace_user_implicit}, which forbids the very duplicates this reproduces, and dropping a real index
     * mid-test would leave the shared container in an unknown state if the assertion failed.
     *
     * <p>
     * Both seeded rows carry a NULL {@code workspace_role}, which is what a custom-role membership looks like. Under
     * plain {@code &gt;} / {@code =} comparisons both predicates evaluate to NULL for such a pair, so neither row would
     * be deleted and creating {@code uk_workspace_user_implicit} would abort Liquibase.
     */
    @Test
    @SuppressFBWarnings(
        value = "SQL_INJECTION_SPRING_JDBC",
        justification = "The statement comes from a checked-in changelog on the classpath, not from any input")
    void testTheDedupStatementRemovesCustomRoleDuplicatesWhoseRoleIsNull() throws Exception {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);

        // A plain table rather than a TEMPORARY one: JdbcTemplate takes a fresh connection per statement and a
        // temporary table lives only on the connection that made it. Dropped in the finally below either way.
        jdbc.execute("DROP TABLE IF EXISTS workspace_user_dedup_probe");
        jdbc.execute(
            """
                CREATE TABLE workspace_user_dedup_probe (
                    id BIGINT, workspace_id BIGINT, user_id BIGINT, workspace_role INT
                )
                """);

        try {
            // Two custom-role rows for one member: NULL role, distinct ids. Plus a built-in pair, so the test also
            // pins the built-in case.
            jdbc.update(
                """
                    INSERT INTO workspace_user_dedup_probe (id, workspace_id, user_id, workspace_role)
                    VALUES (1, 900, 800, NULL), (2, 900, 800, NULL), (3, 900, 801, 1), (4, 900, 801, 0)
                    """);

            jdbc.execute(readDedupStatement().replace("workspace_user", "workspace_user_dedup_probe"));

            Long customRoleSurvivorId = jdbc.queryForObject(
                "SELECT id FROM workspace_user_dedup_probe WHERE user_id = 800", Long.class);

            assertThat(customRoleSurvivorId)
                .as("two NULL-role duplicates must dedup to the MIN(id) row")
                .isEqualTo(1L);

            Integer builtInSurvivorRole = jdbc.queryForObject(
                "SELECT workspace_role FROM workspace_user_dedup_probe WHERE user_id = 801", Integer.class);

            assertThat(builtInSurvivorRole)
                .as("the most privileged built-in role must survive, ADMIN being ordinal 0")
                .isZero();
        } finally {
            jdbc.execute("DROP TABLE IF EXISTS workspace_user_dedup_probe");
        }
    }

    /**
     * The text of the {@code <sql>} element in changeset {@code 20260819120001}. Reading it keeps the test bound to the
     * migration instead of to a copy of it that can drift.
     */
    private static String readDedupStatement() throws Exception {
        ClassPathResource changelogResource = new ClassPathResource(
            "config/liquibase/changelog/automation/configuration/"
                + "20260819120000_automation_configuration_added_column_workspace_user_environment.xml");

        try (InputStream inputStream = changelogResource.getInputStream()) {
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();

            documentBuilderFactory.setNamespaceAware(true);

            // The changelog names an XSD, and resolving anything at all from a parser is unnecessary here: the text of
            // one element is all that is wanted. Refusing DOCTYPE and both entity kinds closes the XXE surface.
            documentBuilderFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            documentBuilderFactory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            documentBuilderFactory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            documentBuilderFactory.setXIncludeAware(false);
            documentBuilderFactory.setExpandEntityReferences(false);

            Document document = documentBuilderFactory.newDocumentBuilder()
                .parse(inputStream);

            NodeList changeSetNodes = document.getElementsByTagNameNS("*", "changeSet");

            List<Element> dedupChangeSets = IntStream.range(0, changeSetNodes.getLength())
                .mapToObj(index -> (Element) changeSetNodes.item(index))
                .filter(changeSet -> DEDUP_CHANGE_SET_ID.equals(changeSet.getAttribute("id")))
                .toList();

            assertThat(dedupChangeSets)
                .as("the changelog must carry exactly one changeset %s", DEDUP_CHANGE_SET_ID)
                .hasSize(1);

            Element dedupChangeSet = dedupChangeSets.getFirst();

            NodeList sqlNodes = dedupChangeSet.getElementsByTagNameNS("*", "sql");

            assertThat(sqlNodes.getLength())
                .as("the changeset must carry exactly one sql element")
                .isEqualTo(1);

            return sqlNodes.item(0)
                .getTextContent();
        }
    }

    private static void assertIndexExists(JdbcTemplate jdbc, String indexName) {
        Long indexCount = jdbc.queryForObject(
            "SELECT COUNT(*) FROM pg_indexes WHERE tablename = 'workspace_user' AND indexname = ?",
            Long.class, indexName);

        assertThat(indexCount)
            .as("index %s must be registered after migrations", indexName)
            .isEqualTo(1L);
    }

    @BeforeEach
    void setUp() {
        jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Test
    void testAllowsOneImplicitRowPerMember() {
        long workspaceId = insertWorkspace("implicit");

        insertMember(1L, workspaceId, null);

        assertThatThrownBy(() -> insertMember(1L, workspaceId, null)).isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void testAllowsOneRowPerEnvironment() {
        long workspaceId = insertWorkspace("per-environment");

        insertMember(2L, workspaceId, DEVELOPMENT);
        insertMember(2L, workspaceId, STAGING);

        assertThatThrownBy(() -> insertMember(2L, workspaceId, DEVELOPMENT)).isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void testAllowsImplicitAndExplicitRowsToCoexistInTheDatabase() {
        long workspaceId = insertWorkspace("coexistence");

        insertMember(3L, workspaceId, null);

        // The database permits the combination, because a partial index cannot express "not both modes". Exactly one
        // mode at a time is a service-layer invariant enforced by WorkspaceUserServiceImpl, not by the schema.
        assertThatCode(() -> insertMember(3L, workspaceId, PRODUCTION)).doesNotThrowAnyException();
    }

    @Test
    void testWorkspaceUserHasNoUniqueConstraint() {
        Long constraintCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM information_schema.table_constraints "
                + "WHERE table_schema = current_schema() AND table_name = 'workspace_user' "
                + "AND constraint_type = 'UNIQUE'",
            Long.class);

        assertThat(constraintCount)
            .as("a unique constraint would forbid per-environment rows; the partial indexes hold uniqueness")
            .isEqualTo(0L);
    }

    @Test
    void testBothPartialUniqueIndexesAreRegistered() {
        assertThat(indexCount("uk_workspace_user_implicit"))
            .as("uk_workspace_user_implicit must be registered after migrations")
            .isEqualTo(1L);
        assertThat(indexCount("uk_workspace_user_explicit"))
            .as("uk_workspace_user_explicit must be registered after migrations")
            .isEqualTo(1L);
    }

    @Test
    void testImplicitFinderIgnoresEnvironmentRows() {
        long workspaceId = insertWorkspace("implicit-finder");

        workspaceUserRepository.save(WorkspaceUser.forRole(10L, workspaceId, WorkspaceRole.EDITOR, null));
        workspaceUserRepository.save(
            WorkspaceUser.forRole(10L, workspaceId, WorkspaceRole.VIEWER, Environment.PRODUCTION));

        Optional<WorkspaceUser> implicitRow =
            workspaceUserRepository.findByUserIdAndWorkspaceIdAndEnvironmentIsNull(10L, workspaceId);

        assertThat(implicitRow).isPresent();
        assertThat(implicitRow.get()
            .getEnvironment()).isNull();
    }

    @Test
    void testEnvironmentFinderReturnsOnlyThatEnvironment() {
        long workspaceId = insertWorkspace("environment-finder");

        workspaceUserRepository.save(
            WorkspaceUser.forRole(11L, workspaceId, WorkspaceRole.EDITOR, Environment.DEVELOPMENT));

        assertThat(
            workspaceUserRepository.findByUserIdAndWorkspaceIdAndEnvironment(
                11L, workspaceId, Environment.DEVELOPMENT.ordinal()))
                    .isPresent();
        assertThat(
            workspaceUserRepository.findByUserIdAndWorkspaceIdAndEnvironment(
                11L, workspaceId, Environment.PRODUCTION.ordinal()))
                    .isEmpty();
    }

    @Test
    void testListFinderReturnsEveryRowForTheMember() {
        long workspaceId = insertWorkspace("list-finder");

        workspaceUserRepository.save(
            WorkspaceUser.forRole(12L, workspaceId, WorkspaceRole.EDITOR, Environment.DEVELOPMENT));
        workspaceUserRepository.save(
            WorkspaceUser.forRole(12L, workspaceId, WorkspaceRole.VIEWER, Environment.STAGING));

        assertThat(workspaceUserRepository.findAllByUserIdAndWorkspaceId(12L, workspaceId)).hasSize(2);
    }

    @Test
    void testCountsImplicitAndExplicitAdminsPerEnvironment() {
        long workspaceId = insertWorkspace("admin-count");

        // An implicit admin administers every environment; an explicit admin only the one they name.
        workspaceUserRepository.save(WorkspaceUser.forRole(20L, workspaceId, WorkspaceRole.ADMIN));
        workspaceUserRepository.save(
            WorkspaceUser.forRole(21L, workspaceId, WorkspaceRole.ADMIN, Environment.PRODUCTION));
        workspaceUserRepository.save(
            WorkspaceUser.forRole(22L, workspaceId, WorkspaceRole.VIEWER, Environment.PRODUCTION));

        assertThat(
            workspaceUserRepository.countAdminsForEnvironment(
                workspaceId, WorkspaceRole.ADMIN.ordinal(), Environment.PRODUCTION.ordinal()))
                    .as("the implicit admin plus the Production admin, but not the Production viewer")
                    .isEqualTo(2L);
        assertThat(
            workspaceUserRepository.countAdminsForEnvironment(
                workspaceId, WorkspaceRole.ADMIN.ordinal(), Environment.STAGING.ordinal()))
                    .as("only the implicit admin reaches Staging")
                    .isEqualTo(1L);
    }

    @Test
    void testExistsFinderHoldsForAMemberWhoOnlyHoldsEnvironmentRows() {
        long workspaceId = insertWorkspace("exists-finder");

        // The membership question, and the reason it is not an Optional-returning finder: two rows for one member would
        // make a single-result derived query throw IncorrectResultSizeDataAccessException against this very schema.
        workspaceUserRepository.save(
            WorkspaceUser.forRole(13L, workspaceId, WorkspaceRole.EDITOR, Environment.DEVELOPMENT));
        workspaceUserRepository.save(
            WorkspaceUser.forRole(13L, workspaceId, WorkspaceRole.VIEWER, Environment.PRODUCTION));

        assertThat(workspaceUserRepository.existsByUserIdAndWorkspaceId(13L, workspaceId))
            .as("a member holding only environment rows is still a member")
            .isTrue();
        assertThat(workspaceUserRepository.existsByUserIdAndWorkspaceId(14L, workspaceId))
            .as("and somebody with no row at all is not")
            .isFalse();
    }

    @Test
    void testCountsOneAdminHoldingTwoRowsForAnEnvironmentAsOneAdmin() {
        long workspaceId = insertWorkspace("distinct-admin-count");

        workspaceUserRepository.save(WorkspaceUser.forRole(30L, workspaceId, WorkspaceRole.ADMIN));
        workspaceUserRepository.save(
            WorkspaceUser.forRole(30L, workspaceId, WorkspaceRole.ADMIN, Environment.PRODUCTION));

        assertThat(
            workspaceUserRepository.countAdminsForEnvironment(
                workspaceId, WorkspaceRole.ADMIN.ordinal(), Environment.PRODUCTION.ordinal()))
                    .as("an implicit and a Production ADMIN row held by one person are one admin, not two")
                    .isEqualTo(1L);

        workspaceUserRepository.save(WorkspaceUser.forRole(31L, workspaceId, WorkspaceRole.ADMIN));

        assertThat(
            workspaceUserRepository.countAdminsForEnvironment(
                workspaceId, WorkspaceRole.ADMIN.ordinal(), Environment.PRODUCTION.ordinal()))
                    .as("a second admin makes two")
                    .isEqualTo(2L);
    }

    @Test
    void testLocksAnExistingWorkspaceRow() {
        long workspaceId = insertWorkspace("lock");

        assertThat(workspaceUserRepository.lockWorkspace(workspaceId)).hasValue(workspaceId);
        assertThat(workspaceUserRepository.lockWorkspace(-1L)).isEmpty();
    }

    @Test
    void testExistsEnvironmentRowFinderIgnoresTheImplicitRow() {
        long workspaceId = insertWorkspace("exists-environment-row");

        insertMember(40L, workspaceId, null);

        assertThat(workspaceUserRepository.existsByUserIdAndWorkspaceIdAndEnvironmentIsNotNull(40L, workspaceId))
            .isFalse();

        insertMember(40L, workspaceId, PRODUCTION);

        assertThat(workspaceUserRepository.existsByUserIdAndWorkspaceIdAndEnvironmentIsNotNull(40L, workspaceId))
            .isTrue();
    }

    private Long indexCount(String indexName) {
        return jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM pg_indexes WHERE tablename = 'workspace_user' AND indexname = ?", Long.class,
            indexName);
    }

    private long insertWorkspace(String name) {
        // workspace.name carries a unique constraint, so each test seeds its own workspace under its own name.
        Long workspaceId = jdbcTemplate.queryForObject(
            "INSERT INTO workspace (name, created_date, created_by, last_modified_date, last_modified_by, version) "
                + "VALUES (?, NOW(), 'test', NOW(), 'test', 0) RETURNING id",
            Long.class, "per-environment-role-test-" + name);

        assertThat(workspaceId).isNotNull();

        return workspaceId;
    }

    private void insertMember(long userId, long workspaceId, Integer environment) {
        jdbcTemplate.update(
            "INSERT INTO workspace_user "
                + "(user_id, workspace_id, workspace_role, environment, created_date, created_by, "
                + "last_modified_date, last_modified_by, version) "
                + "VALUES (?, ?, 1, ?, NOW(), 'test', NOW(), 'test', 0)",
            userId, workspaceId, environment);
    }
}
