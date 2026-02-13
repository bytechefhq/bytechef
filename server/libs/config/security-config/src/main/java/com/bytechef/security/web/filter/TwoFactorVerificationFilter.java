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

package com.bytechef.security.web.filter;

import com.bytechef.platform.user.service.UserService;
import com.bytechef.security.web.authentication.TwoFactorAuthentication;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Handles {@code POST /api/mfa/verify} requests. Reads the TOTP code from the request body, verifies it against the
 * user's stored secret, and either restores the primary authentication or returns 401.
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnProperty(prefix = "bytechef.security.two-factor-authentication", name = "enabled", havingValue = "true")
public class TwoFactorVerificationFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;
    private final UserService userService;

    TwoFactorVerificationFilter(ObjectMapper objectMapper, UserService userService) {
        this.objectMapper = objectMapper;
        this.userService = userService;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {

        if (!isMfaVerifyRequest(request)) {
            filterChain.doFilter(request, response);

            return;
        }

        SecurityContext securityContext = SecurityContextHolder.getContext();

        Authentication authentication = securityContext.getAuthentication();

        if (!(authentication instanceof TwoFactorAuthentication twoFactorAuthentication)) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());

            return;
        }

        String code = extractCode(request);

        if (code == null) {
            response.setStatus(HttpStatus.BAD_REQUEST.value());

            return;
        }

        Object principal = twoFactorAuthentication.getPrimary()
            .getPrincipal();

        if (!(principal instanceof UserDetails userDetails)) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());

            return;
        }

        boolean valid = userService.verifyTotpCode(userDetails.getUsername(), code);

        if (valid) {
            securityContext.setAuthentication(twoFactorAuthentication.getPrimary());

            new HttpSessionSecurityContextRepository().saveContext(securityContext, request, response);

            response.setStatus(HttpStatus.OK.value());
        } else {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
        }
    }

    private String extractCode(HttpServletRequest request) throws IOException {
        JsonNode jsonNode = objectMapper.readTree(request.getInputStream());

        JsonNode codeNode = jsonNode.get("code");

        if (codeNode != null && codeNode.isTextual()) {
            return codeNode.asText();
        }

        return null;
    }

    private boolean isMfaVerifyRequest(HttpServletRequest request) {
        return "POST".equalsIgnoreCase(request.getMethod()) && "/api/mfa/verify".equals(request.getServletPath());
    }
}
