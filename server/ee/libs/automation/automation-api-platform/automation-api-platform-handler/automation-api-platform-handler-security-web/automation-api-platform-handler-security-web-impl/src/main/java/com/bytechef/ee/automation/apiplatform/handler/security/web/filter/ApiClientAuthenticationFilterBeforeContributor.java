/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.apiplatform.handler.security.web.filter;

import com.bytechef.platform.security.web.filter.FilterBeforeContributor;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.servlet.Filter;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class ApiClientAuthenticationFilterBeforeContributor implements FilterBeforeContributor {

    @Override
    @SuppressFBWarnings("EI")
    public Filter getFilter(AuthenticationManager authenticationManager) {
        return new ApiClientAuthenticationFilter(authenticationManager);
    }

    @Override
    public Class<? extends Filter> getBeforeFilter() {
        return BasicAuthenticationFilter.class;
    }
}
