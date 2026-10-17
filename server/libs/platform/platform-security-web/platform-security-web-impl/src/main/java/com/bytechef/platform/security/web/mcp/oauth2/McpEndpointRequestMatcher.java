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

import com.bytechef.platform.security.web.mcp.McpEndpoint;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Optional;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * Matches the requests of every contributed {@link McpEndpoint}.
 *
 * @author Ivica Cardic
 */
public class McpEndpointRequestMatcher implements RequestMatcher {

    private final List<McpEndpoint> mcpEndpoints;

    public McpEndpointRequestMatcher(List<McpEndpoint> mcpEndpoints) {
        this.mcpEndpoints = List.copyOf(mcpEndpoints);
    }

    @Override
    public boolean matches(HttpServletRequest request) {
        Optional<McpEndpoint> mcpEndpoint = findMcpEndpoint(request);

        return mcpEndpoint.isPresent();
    }

    public Optional<McpEndpoint> findMcpEndpoint(HttpServletRequest request) {
        return findMcpEndpoint(McpProtectedResourceUrls.getPathWithinApplication(request));
    }

    public Optional<McpEndpoint> findMcpEndpoint(String path) {
        return mcpEndpoints.stream()
            .filter(mcpEndpoint -> mcpEndpoint.matches(path))
            .findFirst();
    }
}
