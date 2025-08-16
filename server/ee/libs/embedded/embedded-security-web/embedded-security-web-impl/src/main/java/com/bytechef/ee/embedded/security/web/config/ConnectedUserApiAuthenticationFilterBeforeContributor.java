/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.security.web.config;

import com.bytechef.ee.embedded.security.web.filter.ConnectedUserApiAuthenticationFilter;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.security.web.config.FilterBeforeContributor;
import com.bytechef.platform.user.service.SigningKeyService;
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
@ConditionalOnEEVersion
public class ConnectedUserApiAuthenticationFilterBeforeContributor implements FilterBeforeContributor {

    private final SigningKeyService signingKeyService;

    @SuppressFBWarnings("EI")
    public ConnectedUserApiAuthenticationFilterBeforeContributor(SigningKeyService signingKeyService) {
        this.signingKeyService = signingKeyService;
    }

    @Override
    @SuppressFBWarnings("EI")
    public Filter getFilter(AuthenticationManager authenticationManager) {
        return new ConnectedUserApiAuthenticationFilter(authenticationManager, signingKeyService);
    }

    @Override
    public Class<? extends Filter> getBeforeFilter() {
        return BasicAuthenticationFilter.class;
    }
}
