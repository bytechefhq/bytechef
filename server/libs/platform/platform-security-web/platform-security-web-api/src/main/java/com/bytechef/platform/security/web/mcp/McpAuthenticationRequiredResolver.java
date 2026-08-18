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

package com.bytechef.platform.security.web.mcp;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;

/**
 * Reports whether the MCP server addressed by a request's URL path secret requires authentication, so the OAuth2
 * discovery challenge is only issued for a server that actually demands a credential. Each MCP surface contributes its
 * own implementation, because the flag lives in a different place per surface - a {@code mcp_server} row for
 * automation, the {@code mcp.server} platform property for management.
 *
 * <p>
 * Implementations run <em>before</em> any tenant context is established for the request (the discovery filter sits
 * ahead of the credential filters), so they must derive the tenant from the path secret themselves. They must also fail
 * closed: an unresolvable secret or server yields {@code Optional.of(true)}, never {@code Optional.empty()}, so a
 * malformed request is challenged rather than served anonymously.
 *
 * @author Ivica Cardic
 */
@FunctionalInterface
public interface McpAuthenticationRequiredResolver {

    /**
     * Whether the server addressed by this request requires authentication, or empty when the request does not target
     * this resolver's MCP surface so another resolver can answer.
     */
    Optional<Boolean> resolveAuthenticationRequired(HttpServletRequest request);
}
