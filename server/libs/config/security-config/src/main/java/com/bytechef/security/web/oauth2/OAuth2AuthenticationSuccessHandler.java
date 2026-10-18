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

package com.bytechef.security.web.oauth2;

import com.bytechef.tenant.constant.TenantConstants;
import com.bytechef.tenant.service.TenantService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.RememberMeServices;

/**
 * OAuth2 authentication success handler. Binds the session to the signed-in user's tenant in multi-tenant mode (the
 * internal API resolves its tenant from the session), triggers remember-me for persistent sessions and redirects to the
 * frontend OAuth2 redirect page.
 *
 * @author Ivica Cardic
 */
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final RememberMeServices rememberMeServices;
    private final TenantService tenantService;

    @SuppressFBWarnings("EI")
    public OAuth2AuthenticationSuccessHandler(RememberMeServices rememberMeServices, TenantService tenantService) {
        this.rememberMeServices = rememberMeServices;
        this.tenantService = tenantService;
    }

    @Override
    public void onAuthenticationSuccess(
        HttpServletRequest request, HttpServletResponse response, Authentication authentication)
        throws IOException {

        if (tenantService.isMultiTenantEnabled()) {
            Optional<String> tenantId = resolveTenantId(authentication);

            if (tenantId.isEmpty()) {
                response.sendRedirect(OAuth2AuthenticationFailureHandler.LOGIN_ERROR_URL);

                return;
            }

            request.getSession()
                .setAttribute(TenantConstants.CURRENT_TENANT_ID, tenantId.get());
        }

        rememberMeServices.loginSuccess(request, response, authentication);

        response.sendRedirect("/oauth2/redirect");
    }

    /**
     * The tenant holding the signed-in user, called in multi-tenant mode only.
     */
    protected Optional<String> resolveTenantId(Authentication authentication) {
        List<String> tenantIds = tenantService.getTenantIdsByUserLogin(authentication.getName());

        return tenantIds.stream()
            .findFirst();
    }
}
