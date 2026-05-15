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
 * Multi-tenant DataSource wrapper that sets the PostgreSQL search_path based on tenant context.
 *
 * @version ee
 *
 * @author Ivica Cardic
 * @author Igor Beslic
 */
public class MultiTenantDataSource extends BaseDataSource {

    @SuppressFBWarnings("EI")
    public MultiTenantDataSource(DataSource dataSource) {
        super(dataSource);
    }

}
