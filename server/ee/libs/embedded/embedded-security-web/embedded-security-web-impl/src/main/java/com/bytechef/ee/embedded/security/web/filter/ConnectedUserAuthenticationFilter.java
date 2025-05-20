/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.security.web.filter;

import com.bytechef.ee.embedded.security.web.authentication.ConnectedUserAuthenticationToken;
import com.bytechef.platform.security.web.filter.AbstractPublicApiAuthenticationFilter;
import com.bytechef.tenant.domain.TenantKey;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.servlet.http.HttpServletRequest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class ConnectedUserAuthenticationFilter extends AbstractPublicApiAuthenticationFilter {

    private static final Pattern EXTERNAL_USER_ID_PATTERN = Pattern.compile(".*/v\\d+/([^/]+)/.*");

    @SuppressFBWarnings("EI")
    public ConnectedUserAuthenticationFilter(AuthenticationManager authenticationManager) {
        super("^/api/embedded/v[0-9]+/(?!frontend/).+", authenticationManager);

    }

    @Override
    protected Authentication getAuthentication(HttpServletRequest request) {
        String token = getAuthToken(request);

        TenantKey tenantKey = TenantKey.parse(token);

        Matcher matcher = EXTERNAL_USER_ID_PATTERN.matcher(request.getRequestURI());

        String externalUserId;

        if (matcher.matches()) {
            externalUserId = matcher.group(1);
        } else {
            throw new IllegalArgumentException("externalUserId parameter is required");
        }

        return new ConnectedUserAuthenticationToken(externalUserId, getEnvironment(request), tenantKey.getTenantId());
    }
}
