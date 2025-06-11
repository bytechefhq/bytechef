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
        "/actuator", "/api", "/approvals", "/callback", "/file-entries", "/graphql", "/graphiql",
        "/icons", "/mcp", "/oauth", "/sse", "/v3/api-docs", "/webhooks");

    /**
     * Forwards any HTTP request with an unmapped path (i.e., not handled by other controllers or static resources),
     * except those containing a period (indicating a file extension), to the client {@code index.html}.
     *
     * <p>
     * This is commonly used in Single Page Application (SPA) setups where client-side routing handles navigation. If
     * the requested path is:
     * <ul>
     * <li>Not matching a predefined server route or static resource</li>
     * <li>Does NOT contain a period (to exclude direct file requests, such as images or scripts)</li>
     * <li>Matches the pattern {@code /(.*)} (i.e., is a valid root-relative path)</li>
     * </ul>
     * then the method forwards the request internally to {@code /index.html}. This allows the front-end application to
     * handle the routing on the client side.
     * <p>
     * All other requests, including paths with file extensions (e.g., {@code index.html}, {@code app.js}), are
     * processed normally.
     *
     * @param request     the current HTTP request
     * @param response    the current HTTP response
     * @param filterChain the filter chain to pass control to the next filter
     * @throws ServletException if an exception occurs during request processing
     * @throws IOException      if an input or output exception occurs
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {

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
