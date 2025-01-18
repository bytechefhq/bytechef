/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.tenant.multi.security.web.authentication;

import com.bytechef.tenant.constant.TenantConstants;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class MultiTenantAuthenticationFailureHandler implements AuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(
        HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) {

        HttpSession session = request.getSession();

        session.removeAttribute(TenantConstants.CURRENT_TENANT_ID);

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
    }
}
