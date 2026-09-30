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

package com.bytechef.platform.data.table.configuration.migration;

import com.bytechef.platform.configuration.domain.Environment;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

/**
 * Moves data tables from the {@code workspace_data_table} link table onto {@code data_table.workspace_id} and renames
 * every legacy {@code dt_<environmentId>_<name>} physical table to {@code dt_<environmentId>_<dataTableId>}.
 *
 * <p>
 * A data table linked to more than one workspace through {@code workspace_data_table} cannot keep a single
 * {@code workspace_id}, so it is split: the first (lowest-id) workspace keeps the original registry row and its
 * physical tables, and every other linked workspace gets its own copy of the registry row, its tags, and a physical
 * table per environment with the same columns and rows. The original's webhooks are removed rather than copied, and
 * each removal is logged at WARN.
 *
 * <p>
 * Every statement is qualified with the schema this call was asked to migrate, never left to the connection's default
 * schema: Liquibase's per-tenant run sets its own default schema without necessarily moving the JDBC connection's
 * {@code search_path}, and trusting the ambient schema there would quietly migrate the wrong one.
 *
 * <p>
 * Every step is idempotent so that a run interrupted after partial DDL -- H2 auto-commits DDL statement by statement,
 * so a crash mid-changeset can leave exactly that -- picks back up where it left off on the next attempt rather than
 * duplicating rows or failing on a collision.
 *
 * <p>
 * The SQL uses only {@code information_schema}, plain DDL and {@code ALTER TABLE ... RENAME TO}, which both Postgres
 * and H2 accept, because it must run against the pre-migration schema rather than through {@code DataTableServiceImpl}.
 *
 * @author Ivica Cardic
 */
public class DataTableWorkspaceScopedNamingMigrator {

    private static final Logger log = LoggerFactory.getLogger(DataTableWorkspaceScopedNamingMigrator.class);

    private static final String LINK_TABLE = "workspace_data_table";

    private static final Pattern PHYSICAL_TABLE_NAME = Pattern.compile("^dt_(\\d++)_(\\d++)$");

    private final JdbcTemplate jdbcTemplate;

    @SuppressFBWarnings("EI")
    public DataTableWorkspaceScopedNamingMigrator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    public MigrationResult migrate(@Nullable String schemaName) {
        String schema = resolveSchema(schemaName);

        int splitTableCount = 0;

        if (tableExists(schema, LINK_TABLE)) {
            splitTableCount = splitMultiWorkspaceTables(schema);

            jdbcTemplate.update(
                "UPDATE " + qualify(schema, "data_table")
                    + " dataTable SET workspace_id = (SELECT MIN(link.workspace_id) "
                    + "FROM " + qualify(schema, LINK_TABLE) + " link WHERE link.data_table_id = dataTable.id) "
                    + "WHERE dataTable.workspace_id IS NULL");
        }

        int renamedTableCount = renameLegacyPhysicalTables(schema);

        warnAboutUnregisteredPhysicalTables(schema);

        if (tableExists(schema, LINK_TABLE)) {
            jdbcTemplate.execute("DROP TABLE " + qualify(schema, LINK_TABLE));
        }

        log.info(
            "Split {} and renamed {} data tables in schema {}", splitTableCount, renamedTableCount, schema);

        return new MigrationResult(splitTableCount, renamedTableCount);
    }

    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    private int splitMultiWorkspaceTables(String schema) {
        List<Map<String, Object>> sharedTables = jdbcTemplate.queryForList(
            "SELECT data_table_id FROM " + qualify(schema, LINK_TABLE)
                + " GROUP BY data_table_id HAVING COUNT(*) > 1");

        for (Map<String, Object> sharedTable : sharedTables) {
            long dataTableId = ((Number) sharedTable.get("data_table_id")).longValue();

            List<Long> workspaceIds = jdbcTemplate.queryForList(
                "SELECT workspace_id FROM " + qualify(schema, LINK_TABLE)
                    + " WHERE data_table_id = ? ORDER BY workspace_id",
                Long.class, dataTableId);

            String name = jdbcTemplate.queryForObject(
                "SELECT name FROM " + qualify(schema, "data_table") + " WHERE id = ?", String.class, dataTableId);

            jdbcTemplate.update(
                "UPDATE " + qualify(schema, "data_table") + " SET workspace_id = ? WHERE id = ?",
                workspaceIds.getFirst(), dataTableId);

            removeWebhooks(schema, dataTableId, name);

            for (Long workspaceId : workspaceIds.subList(1, workspaceIds.size())) {
                Long copyId = findExistingCopyId(schema, name, workspaceId);

                if (copyId == null) {
                    copyId = copyRegistryRow(schema, dataTableId, workspaceId);
                }

                copyPhysicalTables(schema, dataTableId, copyId);
            }
        }

        return sharedTables.size();
    }

    /**
     * The copy a previous, partially completed run already created for this workspace, so a re-run neither duplicates
     * the registry row nor re-copies its tags.
     */
    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    private @Nullable Long findExistingCopyId(String schema, String name, long workspaceId) {
        List<Long> copyIds = jdbcTemplate.queryForList(
            "SELECT id FROM " + qualify(schema, "data_table") + " WHERE name = ? AND workspace_id = ?", Long.class,
            name, workspaceId);

        if (copyIds.isEmpty()) {
            return null;
        }

        return copyIds.getFirst();
    }

    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    private long copyRegistryRow(String schema, long dataTableId, long workspaceId) {
        Map<String, Object> source = jdbcTemplate.queryForMap(
            "SELECT name, description, created_by, last_modified_by FROM " + qualify(schema, "data_table")
                + " WHERE id = ?",
            dataTableId);

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(
            connection -> createRegistryRowInsertStatement(connection, schema, source, workspaceId), keyHolder);

        long copyId = Objects.requireNonNull(keyHolder.getKey())
            .longValue();

        jdbcTemplate.update(
            "INSERT INTO " + qualify(schema, "data_table_tag") + " (data_table_id, tag_id) SELECT ?, tag_id FROM "
                + qualify(schema, "data_table_tag") + " WHERE data_table_id = ?",
            copyId, dataTableId);

        return copyId;
    }

    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    private void removeWebhooks(String schema, long dataTableId, String name) {
        List<String> webhookUrls = jdbcTemplate.queryForList(
            "SELECT url FROM " + qualify(schema, "data_table_webhook") + " WHERE data_table_id = ? ORDER BY id",
            String.class, dataTableId);

        if (webhookUrls.isEmpty()) {
            return;
        }

        jdbcTemplate.update(
            "DELETE FROM " + qualify(schema, "data_table_webhook") + " WHERE data_table_id = ?", dataTableId);

        for (String webhookUrl : webhookUrls) {
            log.warn(
                "Removed webhook {} from shared data table '{}' (id={}) while splitting it by workspace; re-enable "
                    + "the triggers that used it",
                webhookUrl, name, dataTableId);
        }
    }

    /**
     * Builds the registry row insert as a named method, rather than inline in
     * {@link #copyRegistryRow(String, long, long)}'s lambda, so the SQL injection and unsatisfied-obligation
     * suppressions below can be attached to it: {@code JdbcTemplate.update(PreparedStatementCreator, KeyHolder)} closes
     * this statement once it consumes it, and the SQL text carries only the schema-qualified, already-validated
     * {@code data_table} table name, never caller input.
     */
    @SuppressFBWarnings({
        "SQL_INJECTION_JDBC", "OBL_UNSATISFIED_OBLIGATION_EXCEPTION_EDGE"
    })
    private PreparedStatement createRegistryRowInsertStatement(
        Connection connection, String schema, Map<String, Object> source, long workspaceId) throws SQLException {

        PreparedStatement preparedStatement = connection.prepareStatement(
            "INSERT INTO " + qualify(schema, "data_table") + " (name, description, workspace_id, created_date, "
                + "created_by, last_modified_date, last_modified_by, version) "
                + "VALUES (?, ?, ?, CURRENT_TIMESTAMP, ?, CURRENT_TIMESTAMP, ?, 0)",
            new String[] {
                "id"
            });

        preparedStatement.setString(1, (String) source.get("name"));
        preparedStatement.setString(2, (String) source.get("description"));
        preparedStatement.setLong(3, workspaceId);
        preparedStatement.setString(4, (String) source.get("created_by"));
        preparedStatement.setString(5, (String) source.get("last_modified_by"));

        return preparedStatement;
    }

    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    private void copyPhysicalTables(String schema, long sourceId, long copyId) {
        String name = jdbcTemplate.queryForObject(
            "SELECT name FROM " + qualify(schema, "data_table") + " WHERE id = ?", String.class, sourceId);

        for (Environment environment : Environment.values()) {
            String legacyPhysicalName = "dt_" + environment.ordinal() + "_" + name;
            String alreadyRenamedPhysicalName = "dt_" + environment.ordinal() + "_" + sourceId;

            String sourcePhysicalName;

            if (tableExists(schema, legacyPhysicalName)) {
                sourcePhysicalName = legacyPhysicalName;
            } else if (tableExists(schema, alreadyRenamedPhysicalName)) {
                // A previous, interrupted run already renamed the original before this run's split step got to it.
                sourcePhysicalName = alreadyRenamedPhysicalName;
            } else {
                continue;
            }

            String copyPhysicalName = "dt_" + environment.ordinal() + "_" + copyId;

            if (tableExists(schema, copyPhysicalName)) {
                if (!isEmptyCopyMissingRows(schema, copyPhysicalName, sourcePhysicalName)) {
                    // A previous, interrupted run already copied this environment's physical table.
                    continue;
                }

                // A previous, interrupted run committed the CREATE TABLE but not the INSERT ... SELECT -- H2
                // auto-commits DDL and DML as separate statements, so a crash between the two leaves the copy table
                // present but empty. Dropping and falling through recreates it from scratch rather than leaving an
                // unpopulated copy in place forever.
                jdbcTemplate.execute("DROP TABLE " + qualify(schema, copyPhysicalName));
            }

            List<String[]> userColumns = listUserColumns(schema, sourcePhysicalName);

            String userColumnsSql = userColumns.stream()
                .map(column -> quote(column[0]) + " " + column[1])
                .collect(Collectors.joining(", "));

            jdbcTemplate.execute(
                "CREATE TABLE " + qualify(schema, copyPhysicalName) + " (\"id\" BIGSERIAL PRIMARY KEY, "
                    + "\"external_id\" VARCHAR(255)" + (userColumnsSql.isEmpty() ? "" : ", " + userColumnsSql) + ")");
            jdbcTemplate.execute(
                "CREATE UNIQUE INDEX ON " + qualify(schema, copyPhysicalName) + " (\"external_id\")");

            List<String> copiedColumns = new ArrayList<>(List.of("id", "external_id"));

            for (String[] userColumn : userColumns) {
                copiedColumns.add(userColumn[0]);
            }

            String columnList = copiedColumns.stream()
                .map(DataTableWorkspaceScopedNamingMigrator::quote)
                .collect(Collectors.joining(", "));

            jdbcTemplate.execute(
                "INSERT INTO " + qualify(schema, copyPhysicalName) + " (" + columnList + ") SELECT " + columnList
                    + " FROM " + qualify(schema, sourcePhysicalName));

            restartIdentity(schema, copyPhysicalName);
        }
    }

    /**
     * Whether {@code copyPhysicalName} is a copy an interrupted run created but never populated: present yet empty
     * while its source still holds the rows it was meant to receive. A copy that is empty because its source is also
     * empty is not broken -- there was nothing to copy -- so only a source with rows makes an empty copy suspect.
     */
    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    private boolean isEmptyCopyMissingRows(String schema, String copyPhysicalName, String sourcePhysicalName) {
        Long copyRowCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM " + qualify(schema, copyPhysicalName), Long.class);
        Long sourceRowCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM " + qualify(schema, sourcePhysicalName), Long.class);

        return copyRowCount != null && copyRowCount == 0 && sourceRowCount != null && sourceRowCount > 0;
    }

    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    private void restartIdentity(String schema, String physicalName) {
        Long maxId = jdbcTemplate.queryForObject(
            "SELECT COALESCE(MAX(\"id\"), 0) FROM " + qualify(schema, physicalName), Long.class);

        long nextId = Objects.requireNonNull(maxId) + 1;

        if (isH2()) {
            jdbcTemplate.execute(
                "ALTER TABLE " + qualify(schema, physicalName) + " ALTER COLUMN \"id\" RESTART WITH " + nextId);
        } else {
            jdbcTemplate.queryForObject(
                "SELECT setval(pg_get_serial_sequence(?, 'id'), ?, false)", Long.class,
                qualify(schema, physicalName), nextId);
        }
    }

    /**
     * Renames every legacy {@code dt_<env>_<name>} table to {@code dt_<env>_<id>}, oldest registry row first: a split
     * gives the copy row the same {@code name} as the original it was split from, and only the original -- always the
     * lower id, since a copy is always inserted after it -- still owns the legacy physical table. Renaming that one
     * first empties the legacy name before the copy's row is considered, so the copy is never mistaken for owning it. A
     * target that already exists is left alone rather than collided with, which also makes the rename idempotent on a
     * re-run once it has already happened.
     */
    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    private int renameLegacyPhysicalTables(String schema) {
        List<Map<String, Object>> dataTables = jdbcTemplate.queryForList(
            "SELECT id, name FROM " + qualify(schema, "data_table") + " ORDER BY id");

        int renamedTableCount = 0;

        for (Map<String, Object> dataTable : dataTables) {
            long dataTableId = ((Number) dataTable.get("id")).longValue();
            String name = (String) dataTable.get("name");

            for (Environment environment : Environment.values()) {
                String legacyPhysicalName = "dt_" + environment.ordinal() + "_" + name;
                String targetPhysicalName = "dt_" + environment.ordinal() + "_" + dataTableId;

                if (tableExists(schema, targetPhysicalName)) {
                    continue;
                }

                if (!tableExists(schema, legacyPhysicalName)) {
                    continue;
                }

                jdbcTemplate.execute(
                    "ALTER TABLE " + qualify(schema, legacyPhysicalName) + " RENAME TO " + quote(targetPhysicalName));

                renamedTableCount++;
            }
        }

        return renamedTableCount;
    }

    /**
     * Every physical table this schema still carries that does not resolve to a registered data table, per spec
     * &sect;3.2 step 3: left as is, and logged rather than silently ignored, because to the operator it looks identical
     * to a table the migration simply has not gotten to yet.
     */
    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    private void warnAboutUnregisteredPhysicalTables(String schema) {
        List<String> physicalTableNames = jdbcTemplate.queryForList(
            "SELECT table_name FROM information_schema.tables WHERE table_schema = ? AND table_name LIKE 'dt\\_%'",
            String.class, schema);

        Set<Long> registeredIds = new HashSet<>(
            jdbcTemplate.queryForList("SELECT id FROM " + qualify(schema, "data_table"), Long.class));

        for (String physicalTableName : physicalTableNames) {
            Matcher matcher = PHYSICAL_TABLE_NAME.matcher(physicalTableName);

            if (matcher.matches() && registeredIds.contains(Long.parseLong(matcher.group(2)))) {
                continue;
            }

            log.warn(
                "Physical data table '{}' in schema '{}' matches no registered data table and was left untouched",
                physicalTableName, schema);
        }
    }

    /**
     * Excludes {@code id} and {@code external_id} in SQL rather than filtering the mapped rows afterward, because
     * {@code id} is reported back as {@code bigint} -- a type {@link #sqlType} does not map and, by design, throws
     * rather than silently guesses at -- so it must never reach that mapping at all.
     */
    private List<String[]> listUserColumns(String schema, String physicalName) {
        return jdbcTemplate.query(
            "SELECT column_name, data_type, character_maximum_length, numeric_precision, numeric_scale "
                + "FROM information_schema.columns WHERE table_schema = ? AND table_name = ? "
                + "AND column_name NOT IN ('id', 'external_id') ORDER BY ordinal_position",
            (resultSet, rowNumber) -> new String[] {
                resultSet.getString("column_name"),
                sqlType(
                    physicalName, resultSet.getString("column_name"), resultSet.getString("data_type"),
                    resultSet.getObject("character_maximum_length"), resultSet.getObject("numeric_precision"),
                    resultSet.getObject("numeric_scale"))
            },
            schema, physicalName);
    }

    private static String sqlType(
        String physicalName, String columnName, String dataType, @Nullable Object characterMaximumLength,
        @Nullable Object numericPrecision, @Nullable Object numericScale) {

        String lowerCaseType = dataType.toLowerCase(Locale.ROOT);

        if (lowerCaseType.startsWith("timestamp")) {
            return "TIMESTAMP";
        }

        if (lowerCaseType.startsWith("character varying") || lowerCaseType.equals("varchar")) {
            return "VARCHAR(" + (characterMaximumLength == null ? 255 : characterMaximumLength) + ")";
        }

        if (lowerCaseType.equals("numeric") || lowerCaseType.equals("decimal")) {
            return "DECIMAL(" + (numericPrecision == null ? 38 : numericPrecision) + ","
                + (numericScale == null ? 9 : numericScale) + ")";
        }

        return switch (lowerCaseType) {
            case "integer", "int4" -> "INTEGER";
            case "boolean", "bool" -> "BOOLEAN";
            case "date" -> "DATE";
            default -> throw new IllegalStateException(
                "Unable to migrate column '" + columnName + "' of table '" + physicalName + "': unsupported data "
                    + "type '" + dataType + "'");
        };
    }

    private boolean tableExists(String schema, String tableName) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = ? AND table_name = ?",
            Integer.class, schema, tableName);

        return count != null && count > 0;
    }

    private boolean isH2() {
        String product = jdbcTemplate.execute(
            (ConnectionCallback<String>) connection -> connection.getMetaData()
                .getDatabaseProductName());

        return product != null && product.toLowerCase(Locale.ROOT)
            .contains("h2");
    }

    private String resolveSchema(@Nullable String schemaName) {
        if (schemaName != null && !schemaName.isBlank()) {
            return schemaName;
        }

        return jdbcTemplate.queryForObject("SELECT current_schema()", String.class);
    }

    private static String qualify(String schema, String tableName) {
        return quote(schema) + "." + quote(tableName);
    }

    private static String quote(String identifier) {
        return '"' + identifier.replace("\"", "\"\"") + '"';
    }

    public record MigrationResult(int splitTableCount, int renamedTableCount) {
    }
}
