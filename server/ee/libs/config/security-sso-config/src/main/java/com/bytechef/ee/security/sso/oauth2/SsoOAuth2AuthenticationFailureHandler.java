/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.security.sso.oauth2;

import com.bytechef.security.web.oauth2.OAuth2AuthenticationFailureHandler;
import com.bytechef.tenant.constant.TenantConstants;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import org.springframework.security.core.AuthenticationException;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class SsoOAuth2AuthenticationFailureHandler extends OAuth2AuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(
        HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
        throws IOException {

        HttpSession session = request.getSession();

        session.removeAttribute(TenantConstants.CURRENT_TENANT_ID);

        super.onAuthenticationFailure(request, response, exception);
    }
}
