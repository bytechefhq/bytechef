/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.tenant.multi.pgvector;

import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.event.TenantSchemaCreatedEvent;
import com.bytechef.tenant.service.TenantService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.ai.vectorstore.pgvector.autoconfigure.PgVectorStoreProperties;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class MultiTenantPgVectorLoader implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(MultiTenantPgVectorLoader.class);

    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*$");

    private final JdbcTemplate jdbcTemplate;
    private final PgVectorStoreProperties properties;
    private final String tableName;
    private final TenantService tenantService;

    @SuppressFBWarnings("EI")
    public MultiTenantPgVectorLoader(
        JdbcTemplate jdbcTemplate, PgVectorStoreProperties properties, String tableName,
        TenantService tenantService) {

        this.jdbcTemplate = jdbcTemplate;
        this.properties = properties;
        this.tableName = tableName;
        this.tenantService = tenantService;
    }

    @Override
    public void afterPropertiesSet() {
        List<String> tenantIds = tenantService.getTenantIds();

        if (log.isDebugEnabled()) {
            log.debug("Initializing PgVectorStore schema for {} tenants", tenantIds.size());
        }

        for (String tenantId : tenantIds) {
            initializeSchema(tenantId);
        }
    }

    @EventListener
    public void onTenantSchemaCreated(TenantSchemaCreatedEvent event) {
        initializeSchema(event.getTenantId());
    }

    private void initializeSchema(String tenantId) {
        TenantContext.runWithTenantId(tenantId, () -> doInitializeSchema(tenantId));
    }

    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    private void doInitializeSchema(String tenantId) {
        String schemaName = TenantContext.getCurrentDatabaseSchema("vectorstore");

        validateIdentifier(schemaName);
        validateIdentifier(tableName);

        log.info("Initializing PgVectorStore schema for table: {} in schema: {}", tableName, schemaName);

        jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS vector");
        jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS hstore");

        if (properties.getIdType() == PgVectorStore.PgIdType.UUID) {
            jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\"");
        }

        jdbcTemplate.execute("CREATE SCHEMA IF NOT EXISTS " + schemaName);

        if (properties.isRemoveExistingVectorStoreTable()) {
            jdbcTemplate.execute("DROP TABLE IF EXISTS " + schemaName + "." + tableName);
        }

        String idColumnType = properties.getIdType() == PgVectorStore.PgIdType.UUID ? "uuid" : "text";

        String createTableSql = "CREATE TABLE IF NOT EXISTS " + schemaName + "." + tableName + " (" +
            "id " + idColumnType + " PRIMARY KEY, " +
            "content text, " +
            "metadata json, " +
            "embedding public.vector(" + properties.getDimensions() + "))";

        jdbcTemplate.execute(createTableSql);

        if (properties.getIndexType() != PgVectorStore.PgIndexType.NONE) {
            String indexName = tableName + "_embedding_idx";
            String indexType = properties.getIndexType()
                .name()
                .toLowerCase();
            String distanceType = properties.getDistanceType().index;

            String createIndexSql = "CREATE INDEX IF NOT EXISTS " + indexName +
                " ON " + schemaName + "." + tableName +
                " USING " + indexType + " (embedding public." + distanceType + ")";

            jdbcTemplate.execute(createIndexSql);
        }

        if (log.isDebugEnabled()) {
            log.debug("Initialized PgVectorStore schema for tenant {}", tenantId);
        }
    }

    private void validateIdentifier(String identifier) {
        if (!IDENTIFIER_PATTERN.matcher(identifier)
            .matches()) {
            throw new IllegalArgumentException("Invalid SQL identifier: " + identifier);
        }
    }
}
