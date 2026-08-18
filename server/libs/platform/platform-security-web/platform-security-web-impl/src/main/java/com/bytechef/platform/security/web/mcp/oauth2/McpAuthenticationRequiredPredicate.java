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

package com.bytechef.platform.security.web.mcp.oauth2;

import com.bytechef.platform.security.web.mcp.McpAuthenticationRequiredResolver;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Asks each {@link McpAuthenticationRequiredResolver} in turn whether the MCP server addressed by the request requires
 * authentication, and answers with the first resolver that claims the request's surface. Feeds
 * {@link McpDiscoveryAuthenticationFilter} so a server that opted out of authentication is not answered with the OAuth2
 * discovery challenge.
 *
 * <p>
 * Fails closed: with no resolvers contributed, or none claiming the request, authentication is required - which is the
 * pre-SPI behavior and the right default for a surface nobody vouched for.
 *
 * @author Ivica Cardic
 */
public class McpAuthenticationRequiredPredicate implements Predicate<HttpServletRequest> {

    private final List<McpAuthenticationRequiredResolver> mcpAuthenticationRequiredResolvers;

    public McpAuthenticationRequiredPredicate(
        List<McpAuthenticationRequiredResolver> mcpAuthenticationRequiredResolvers) {

        this.mcpAuthenticationRequiredResolvers = List.copyOf(mcpAuthenticationRequiredResolvers);
    }

    @Override
    public boolean test(HttpServletRequest request) {
        return mcpAuthenticationRequiredResolvers.stream()
            .map(mcpAuthenticationRequiredResolver -> mcpAuthenticationRequiredResolver
                .resolveAuthenticationRequired(request))
            .flatMap(Optional::stream)
            .findFirst()
            .orElse(true);
    }
}
