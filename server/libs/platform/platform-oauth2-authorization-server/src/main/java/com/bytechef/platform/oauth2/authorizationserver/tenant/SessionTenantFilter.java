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

package com.bytechef.platform.oauth2.authorizationserver.tenant;

import static com.bytechef.platform.oauth2.authorizationserver.constant.OAuth2AuthorizationServerConstants.TENANT_ID_CLAIM;

import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.constant.TenantConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Runs token requests in the tenant carried by the authorization code or refresh token of their grant type, token
 * revocation and introspection requests in the tenant carried by the keyed refresh token or the signature-verified
 * {@code tenant_id} claim of the access token, and every other authorization server request in the tenant stored in the
 * user's session.
 *
 * @author Ivica Cardic
 */
public class SessionTenantFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(SessionTenantFilter.class);

    private static final Map<String, String> TENANT_KEYED_PARAMETER_NAMES = Map.of(
        AuthorizationGrantType.AUTHORIZATION_CODE.getValue(), OAuth2ParameterNames.CODE,
        AuthorizationGrantType.REFRESH_TOKEN.getValue(), OAuth2ParameterNames.REFRESH_TOKEN);

    private final JwtDecoder jwtDecoder;
    private final String tokenEndpointUri;
    private final String tokenIntrospectionEndpointUri;
    private final String tokenRevocationEndpointUri;

    public SessionTenantFilter(
        String tokenEndpointUri, String tokenRevocationEndpointUri, String tokenIntrospectionEndpointUri,
        JwtDecoder jwtDecoder) {

        this.jwtDecoder = jwtDecoder;
        this.tokenEndpointUri = tokenEndpointUri;
        this.tokenIntrospectionEndpointUri = tokenIntrospectionEndpointUri;
        this.tokenRevocationEndpointUri = tokenRevocationEndpointUri;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {

        String tenantId = resolveTenantId(request);

        if (tenantId == null) {
            filterChain.doFilter(request, response);

            return;
        }

        String previousTenantId = TenantContext.getCurrentTenantId();

        TenantContext.setCurrentTenantId(tenantId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.setCurrentTenantId(previousTenantId);
        }
    }

    @Nullable
    private String resolveTenantId(HttpServletRequest request) {
        if (!HttpMethod.POST.matches(request.getMethod())) {
            return resolveSessionTenantId(request);
        }

        String requestPath = getRequestPath(request);

        if (tokenEndpointUri.equals(requestPath)) {
            return resolveTokenRequestTenantId(request);
        }

        if (tokenRevocationEndpointUri.equals(requestPath) || tokenIntrospectionEndpointUri.equals(requestPath)) {
            return resolveTokenParameterTenantId(request.getParameter(OAuth2ParameterNames.TOKEN));
        }

        return resolveSessionTenantId(request);
    }

    @Nullable
    private String resolveTokenParameterTenantId(@Nullable String token) {
        if (token == null || token.isBlank()) {
            return null;
        }

        Optional<String> keyedTenantId = TenantKeyedTokenValues.parseTenantId(token);

        if (keyedTenantId.isPresent()) {
            return keyedTenantId.get();
        }

        if (!isJwtShaped(token)) {
            return null;
        }

        Jwt jwt;

        try {
            jwt = jwtDecoder.decode(token);
        } catch (JwtValidationException jwtValidationException) {
            log.debug(
                "Ignoring the tenant_id claim of an access token that failed validation, the request runs in " +
                    "tenant {}: {}",
                TenantContext.getCurrentTenantId(), jwtValidationException.getMessage());

            return null;
        } catch (JwtException jwtException) {
            log.warn(
                "Ignoring the tenant_id claim of an access token that could not be verified, the request runs in " +
                    "tenant {}: {}",
                TenantContext.getCurrentTenantId(), jwtException.getMessage());

            return null;
        }

        String tenantId = jwt.getClaimAsString(TENANT_ID_CLAIM);

        if (!TenantKeyedTokenValues.isValidTenantId(tenantId)) {
            return null;
        }

        return tenantId;
    }

    private static boolean isJwtShaped(String token) {
        int firstSeparatorIndex = token.indexOf('.');

        if (firstSeparatorIndex < 0) {
            return false;
        }

        int secondSeparatorIndex = token.indexOf('.', firstSeparatorIndex + 1);

        return secondSeparatorIndex >= 0 && token.indexOf('.', secondSeparatorIndex + 1) < 0;
    }

    private static String getRequestPath(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        String contextPath = request.getContextPath();

        if (contextPath != null && !contextPath.isEmpty() && requestUri.startsWith(contextPath)) {
            return requestUri.substring(contextPath.length());
        }

        return requestUri;
    }

    @Nullable
    private static String resolveTokenRequestTenantId(HttpServletRequest request) {
        String grantType = request.getParameter(OAuth2ParameterNames.GRANT_TYPE);

        if (grantType == null) {
            return null;
        }

        String parameterName = TENANT_KEYED_PARAMETER_NAMES.get(grantType);

        if (parameterName == null) {
            return null;
        }

        Optional<String> tenantId = TenantKeyedTokenValues.parseTenantId(request.getParameter(parameterName));

        return tenantId.orElse(null);
    }

    @Nullable
    private static String resolveSessionTenantId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);

        if (session == null) {
            return null;
        }

        return (String) session.getAttribute(TenantConstants.CURRENT_TENANT_ID);
    }
}
