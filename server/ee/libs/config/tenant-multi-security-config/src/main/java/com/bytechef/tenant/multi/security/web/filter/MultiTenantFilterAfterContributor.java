/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.tenant.multi.security.web.filter;

import com.bytechef.platform.security.web.filter.FilterAfterContributor;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.servlet.Filter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class MultiTenantFilterAfterContributor implements FilterAfterContributor {

    private final MultiTenantFilter multiTenantFilter;

    public MultiTenantFilterAfterContributor() {
        this.multiTenantFilter = new MultiTenantFilter();
    }

    @Override
    @SuppressFBWarnings("EI")
    public Filter getFilter() {
        return multiTenantFilter;
    }

    @Override
    public Class<? extends Filter> getAfterFilter() {
        return BasicAuthenticationFilter.class;
    }
}
