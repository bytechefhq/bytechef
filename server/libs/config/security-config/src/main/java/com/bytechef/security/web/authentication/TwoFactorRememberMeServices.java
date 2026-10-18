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

import com.bytechef.platform.security.web.config.TwoFactorAuthenticationCustomizer;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.RememberMeServices;

/**
 * Remember-me services that withhold the cookie from a login that still owes a TOTP code. Spring Security issues the
 * remember-me cookie before the success handler runs, so without this a client could take that cookie, drop the pending
 * session and be signed in by remember-me with the password alone. The cookie is issued by
 * {@code TwoFactorVerificationFilter} once the code is verified.
 *
 * @author Ivica Cardic
 */
public class TwoFactorRememberMeServices implements RememberMeServices {

    private final RememberMeServices delegate;
    private final TwoFactorAuthenticationCustomizer twoFactorAuthenticationCustomizer;

    @SuppressFBWarnings("EI")
    public TwoFactorRememberMeServices(
        RememberMeServices delegate, TwoFactorAuthenticationCustomizer twoFactorAuthenticationCustomizer) {

        this.delegate = delegate;
        this.twoFactorAuthenticationCustomizer = twoFactorAuthenticationCustomizer;
    }

    @Override
    public Authentication autoLogin(HttpServletRequest request, HttpServletResponse response) {
        return delegate.autoLogin(request, response);
    }

    @Override
    public void loginFail(HttpServletRequest request, HttpServletResponse response) {
        delegate.loginFail(request, response);
    }

    @Override
    public void loginSuccess(
        HttpServletRequest request, HttpServletResponse response, Authentication successfulAuthentication) {

        if (twoFactorAuthenticationCustomizer.isTotpEnabled(successfulAuthentication)) {
            return;
        }

        delegate.loginSuccess(request, response, successfulAuthentication);
    }
}
