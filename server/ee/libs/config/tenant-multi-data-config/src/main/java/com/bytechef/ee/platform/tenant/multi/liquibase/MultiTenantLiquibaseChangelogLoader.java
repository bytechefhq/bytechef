/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.tenant.multi.liquibase;

import com.bytechef.platform.tenant.service.TenantService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class MultiTenantLiquibaseChangelogLoader implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(MultiTenantLiquibaseChangelogLoader.class);

    private final TenantService tenantService;

    @SuppressFBWarnings("EI")
    public MultiTenantLiquibaseChangelogLoader(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @Override
    public void afterPropertiesSet() {
        List<String> tenantIds = tenantService.getTenantIds();

        if (log.isDebugEnabled()) {
            log.debug("Loading changelog for tenantIds={}", String.join(",", tenantIds));
        }

        tenantService.loadChangelog(tenantIds, "multitenant");
    }
}
