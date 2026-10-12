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

package com.bytechef.platform.oauth2.authorizationserver.web;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

/**
 * Redirects an unauthenticated browser request to the single-page application's login page, passing the original
 * request path and query as the {@code redirect} parameter so the user returns to it after signing in. The
 * {@code Location} header stays relative so the browser resolves it against the host it is talking to, also behind a
 * proxy.
 *
 * @author Ivica Cardic
 */
public class LoginRedirectAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final String LOGIN_PATH = "/login";
    private static final String REDIRECT_PARAMETER = "redirect";

    @Override
    @SuppressFBWarnings(
        value = "UNVALIDATED_REDIRECT",
        justification = "The Location is this application's login page; the request path only travels as its parameter")
    public void commence(
        HttpServletRequest request, HttpServletResponse response, AuthenticationException authenticationException) {

        String target = request.getRequestURI();
        String queryString = request.getQueryString();

        if (queryString != null && !queryString.isEmpty()) {
            target = target + "?" + queryString;
        }

        response.setStatus(HttpServletResponse.SC_FOUND);
        response.setHeader(
            HttpHeaders.LOCATION,
            request.getContextPath() + LOGIN_PATH + "?" + REDIRECT_PARAMETER + "=" +
                URLEncoder.encode(target, StandardCharsets.UTF_8));
    }
}
