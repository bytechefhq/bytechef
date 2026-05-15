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
}
