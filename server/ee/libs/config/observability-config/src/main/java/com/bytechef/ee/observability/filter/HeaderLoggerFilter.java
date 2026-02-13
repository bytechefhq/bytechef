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

package com.bytechef.ee.observability.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * @Matija Petanjek
 */
class HeaderLoggerFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(HeaderLoggerFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {

        logHeaders(request);

        filterChain.doFilter(request, response);
    }

    private void logHeaders(HttpServletRequest request) {
        request.getHeaderNames()
            .asIterator()
            .forEachRemaining(header -> {
                List<String> values = new ArrayList<>();

                request.getHeaders(header)
                    .asIterator()
                    .forEachRemaining(values::add);

                logger.debug("{}: {}", header, values.size() == 1 ? values.getFirst() : values);
            });
    }
}
