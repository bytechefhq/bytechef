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

package com.bytechef.platform.oauth2.authorizationserver.service;

import com.bytechef.platform.oauth2.authorizationserver.annotation.ConditionalOnOauth2AuthorizationServerEnabled;
import com.bytechef.platform.security.web.mcp.McpEndpoint;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
@ConditionalOnOauth2AuthorizationServerEnabled
public class McpResourceNameResolver {

    private final ObjectProvider<McpEndpoint> mcpEndpointProvider;

    @SuppressFBWarnings("EI2")
    public McpResourceNameResolver(ObjectProvider<McpEndpoint> mcpEndpointProvider) {
        this.mcpEndpointProvider = mcpEndpointProvider;
    }

    public String resolveName(String resource) {
        return mcpEndpointProvider.orderedStream()
            .map(mcpEndpoint -> resolveResourceName(mcpEndpoint, resource))
            .flatMap(Optional::stream)
            .findFirst()
            .orElse(resource);
    }

    private static Optional<String> resolveResourceName(McpEndpoint mcpEndpoint, String resource) {
        try {
            return mcpEndpoint.resolveResourceName(resource);
        } catch (RuntimeException runtimeException) {
            return Optional.empty();
        }
    }
}
