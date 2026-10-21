/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.tenant.multi.pgvector;

import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.tenant.annotation.ConditionalOnMultiTenant;
import com.bytechef.tenant.teardown.TenantTeardownContributor;
import com.bytechef.tenant.util.TenantIdUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@Order(1100)
@ConditionalOnEEVersion
@ConditionalOnMultiTenant
@ConditionalOnProperty(prefix = "bytechef.ai.vectorstore", name = "provider", havingValue = "pgvector")
@ConditionalOnProperty(prefix = "bytechef.ai.vectorstore.pgvector", name = "url")
class VectorStoreTenantTeardownContributor implements TenantTeardownContributor {

    private final JdbcTemplate jdbcTemplate;

    @SuppressFBWarnings("EI")
    VectorStoreTenantTeardownContributor(@Qualifier("pgVectorJdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    public void afterSchemaDrop(String tenantId) {
        jdbcTemplate.execute("DROP SCHEMA IF EXISTS bytechef_vectorstore_" + TenantIdUtils.requireValid(tenantId) +
            " CASCADE");
    }
}
