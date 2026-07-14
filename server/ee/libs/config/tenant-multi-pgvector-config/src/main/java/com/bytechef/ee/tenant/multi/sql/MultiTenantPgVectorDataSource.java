/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.tenant.multi.sql;

import com.bytechef.tenant.sql.BaseDataSource;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import javax.sql.DataSource;

/**
 * Multi-tenant DataSource wrapper for PgVector that sets the PostgreSQL search_path to the tenant's vector store
 * schema.
 *
 * @version ee
 *
 * @author Ivica Cardic
 * @author Igor Beslic
 */
public class MultiTenantPgVectorDataSource extends BaseDataSource {

    private static final String VECTOR_STORE_SCHEMA_SUFFIX = "vectorstore";

    @SuppressFBWarnings("EI")
    public MultiTenantPgVectorDataSource(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    protected String getVectorSchemaSuffix() {
        return VECTOR_STORE_SCHEMA_SUFFIX;
    }

    /**
     * Appends the {@code public} schema so that pgvector's {@code vector} type and its operators (e.g. {@code <=>}),
     * installed via {@code CREATE EXTENSION vector SCHEMA public}, remain resolvable while the search_path is otherwise
     * narrowed to the tenant vector schema.
     */
    @Override
    protected String getSearchPath(String currentDatabaseSchema) {
        return currentDatabaseSchema + ", public";
    }
}
