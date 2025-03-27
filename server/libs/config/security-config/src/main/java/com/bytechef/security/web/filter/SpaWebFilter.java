/*
 * Copyright 2023-present ByteChef Inc.
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

import jakarta.servlet.FilterChain;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * @author Ivica Cardic
 */
public class SpaWebFilter extends OncePerRequestFilter {

    private static final List<String> NON_SPA_PATH_PREFIXES = Arrays.asList(
        "/api", "/approvals", "/actuator", "/auditevents", "/file-entries", "/graphql", "/graphiql", "/mcp", "/sse",
        "/v3/api-docs", "/webhooks");

    /**
     * Forwards any unmapped paths (except those containing a period) to the client {@code index.html}.
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        // Request URI includes the contextPath if any, removed it.
        String requestURI = request.getRequestURI();
        String contextPath = request.getContextPath();

        String path = requestURI.substring(contextPath.length());

        if (isNonSpaPath(path) && !path.contains(".") && path.matches("/(.*)")) {
            RequestDispatcher requestDispatcher = request.getRequestDispatcher("/index.html");

            requestDispatcher.forward(request, response);

            return;
        }

        filterChain.doFilter(request, response);
    }

    private static boolean isNonSpaPath(String path) {
        return NON_SPA_PATH_PREFIXES.stream()
            .noneMatch(path::startsWith);
    }
}
