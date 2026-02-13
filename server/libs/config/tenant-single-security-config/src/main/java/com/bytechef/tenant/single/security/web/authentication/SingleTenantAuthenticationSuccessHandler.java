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

package com.bytechef.tenant.single.security.web.authentication;

import com.bytechef.platform.security.web.config.TwoFactorAuthenticationCustomizer;
import com.bytechef.security.web.authentication.TwoFactorAuthentication;
import com.bytechef.tenant.annotation.ConditionalOnSingleTenant;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
@ConditionalOnSingleTenant
public class SingleTenantAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final ObjectProvider<TwoFactorAuthenticationCustomizer> twoFactorAuthenticationCustomizerProvider;

    SingleTenantAuthenticationSuccessHandler(
        ObjectProvider<TwoFactorAuthenticationCustomizer> twoFactorAuthenticationCustomizerProvider) {

        this.twoFactorAuthenticationCustomizerProvider = twoFactorAuthenticationCustomizerProvider;
    }

    @Override
    public void onAuthenticationSuccess(
        HttpServletRequest request, HttpServletResponse response, Authentication authentication) {

        TwoFactorAuthenticationCustomizer twoFactorAuthenticationCustomizer =
            twoFactorAuthenticationCustomizerProvider.getIfAvailable();

        if (twoFactorAuthenticationCustomizer != null &&
            twoFactorAuthenticationCustomizer.isTotpEnabled(authentication)) {

            SecurityContext securityContext = SecurityContextHolder.getContext();

            securityContext.setAuthentication(new TwoFactorAuthentication(authentication));

            new HttpSessionSecurityContextRepository().saveContext(securityContext, request, response);

            response.setStatus(HttpStatus.ACCEPTED.value());
        } else {
            response.setStatus(HttpStatus.OK.value());
        }
    }
}
