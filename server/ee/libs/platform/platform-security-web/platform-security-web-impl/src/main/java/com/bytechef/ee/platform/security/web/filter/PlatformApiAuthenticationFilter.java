/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.security.web.filter;

import com.bytechef.platform.security.web.filter.AbstractApiAuthenticationFilter;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.security.authentication.AuthenticationManager;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class PlatformApiAuthenticationFilter extends AbstractApiAuthenticationFilter {

    @SuppressFBWarnings("EI")
    public PlatformApiAuthenticationFilter(AuthenticationManager authenticationManager) {
        super("^/api/platform/v[0-9]+/.+", authenticationManager);
    }
}
