/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.security.web.authentication;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;

/**
 * Puts a session into the "password (or identity provider) accepted, TOTP code pending" state that every login path
 * shares: the session holds a {@link TwoFactorAuthentication}, which no protected endpoint accepts, until
 * {@code /api/mfa/verify} swaps the primary authentication back in. Whether the login asked to be remembered is kept in
 * the session so the remember-me cookie can be issued only once the code is verified.
 *
 * @author Ivica Cardic
 */
public final class TwoFactorAuthenticationSupport {

    public static final String REMEMBER_ME_REQUESTED_ATTRIBUTE =
        TwoFactorAuthenticationSupport.class.getName() + ".REMEMBER_ME_REQUESTED";

    static final String REMEMBER_ME_PARAMETER = "remember-me";

    private TwoFactorAuthenticationSupport() {
    }

    public static void beginSecondFactor(
        HttpServletRequest request, HttpServletResponse response, Authentication authentication) {

        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();

        securityContext.setAuthentication(new TwoFactorAuthentication(authentication));

        SecurityContextHolder.setContext(securityContext);

        new HttpSessionSecurityContextRepository().saveContext(securityContext, request, response);

        if (isRememberMeRequested(request)) {
            request.getSession()
                .setAttribute(REMEMBER_ME_REQUESTED_ATTRIBUTE, Boolean.TRUE);
        }

        issueRotatedCsrfToken(request);
    }

    /**
     * Login rotates the CSRF token and clears its cookie. A completed login re-issues it on the next GET, but a pending
     * one goes straight to the {@code POST /api/mfa/verify}, which would then carry no valid token and be refused, so
     * the rotated token is resolved here and written into this response.
     */
    private static void issueRotatedCsrfToken(HttpServletRequest request) {
        if (request.getAttribute(CsrfToken.class.getName()) instanceof CsrfToken csrfToken) {
            csrfToken.getToken();
        }
    }

    static boolean isRememberMeRequested(HttpServletRequest request) {
        String rememberMe = request.getParameter(REMEMBER_ME_PARAMETER);

        return "true".equalsIgnoreCase(rememberMe) || "on".equalsIgnoreCase(rememberMe) ||
            "yes".equalsIgnoreCase(rememberMe) || "1".equals(rememberMe);
    }
}
