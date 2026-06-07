/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.contextstore.clickhouse.schema;

import com.bytechef.ee.platform.contextstore.clickhouse.ClickHouseTableProvisioner;
import com.bytechef.ee.platform.contextstore.domain.ContextStoreSource;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * ClickHouse-side implementation of {@link ClickHouseTableProvisioner}. Composes the sanitiser (for the safe table
 * identifier) with the DDL generator (for the typed-column schema) and executes the {@code CREATE TABLE IF NOT EXISTS}
 * via the qualified {@code clickHouseJdbcTemplate}. Activates only when the ClickHouse datasource bean is wired (i.e.
 * the operator opted in via {@code bytechef.context-store.clickhouse.url}).
 *
 * <p>
 * {@code IF NOT EXISTS} is critical: a retried source-creation that hit a transient ClickHouse error mid-flight needs
 * to succeed on the second attempt without manual table cleanup, and ClickHouse is conservative about throwing on
 * pre-existing tables.
 * </p>
 *
 * @author Ivica Cardic
 * @version ee
 */
@Component
@ConditionalOnBean(name = "clickHouseDataSource")
@ConditionalOnProperty(prefix = "bytechef.context-store", name = "enabled", havingValue = "true")
public class ClickHouseTableProvisionerImpl implements ClickHouseTableProvisioner {

    private static final Logger log = LoggerFactory.getLogger(ClickHouseTableProvisionerImpl.class);

    private final JdbcTemplate jdbcTemplate;

    @SuppressFBWarnings("EI2")
    public ClickHouseTableProvisionerImpl(@Qualifier("clickHouseJdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @SuppressFBWarnings(
        value = "SQL_INJECTION_SPRING_JDBC",
        justification = "Table name comes from ClickHouseTableNameSanitizer.tableNameFor which guarantees the result "
            + "matches VALID_TABLE_NAME_REGEX. DDL body comes from ClickHouseTableDdlGenerator which validates field "
            + "names against its own strict pattern.")
    public String provisionTable(long workspaceId, long contextStoreId, ContextStoreSource source) {
        String tableName = ClickHouseTableNameSanitizer.tableNameFor(
            workspaceId, contextStoreId, source.getId(), source.getEntityName());
        String createTableDdl = ClickHouseTableDdlGenerator.createTableDdl(tableName, source);

        // Insert IF NOT EXISTS after CREATE TABLE so retries are idempotent.
        String idempotentDdl = createTableDdl.replaceFirst("^CREATE TABLE ", "CREATE TABLE IF NOT EXISTS ");

        log.info("Provisioning ClickHouse table {} for source {} ({})", tableName, source.getId(),
            source.getEntityName());

        jdbcTemplate.execute(idempotentDdl);

        return tableName;
    }
}
