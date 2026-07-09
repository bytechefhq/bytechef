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

package com.bytechef.platform.mcp.server;

import java.util.Set;

/**
 * Decides whether a principal may use an MCP component's tools, based on the authorities the principal holds and the
 * authorities that grant the component. Used only when a server enforces tool authorization; the decision is
 * deny-by-default - a component whose granting authorities the principal does not hold (including a component with no
 * granting authority at all) is not exposed.
 *
 * @author Ivica Cardic
 */
public class McpToolAuthorizationEvaluator {

    /**
     * Whether the principal may use a component, i.e. holds at least one of the component's granting authorities (OR
     * semantics). A component with no granting authority is denied to everyone.
     */
    public boolean isComponentAuthorized(Set<String> principalAuthorities, Set<String> componentRequiredAuthorities) {
        if (componentRequiredAuthorities == null || componentRequiredAuthorities.isEmpty()) {
            return false;
        }

        if (principalAuthorities == null || principalAuthorities.isEmpty()) {
            return false;
        }

        return componentRequiredAuthorities.stream()
            .anyMatch(principalAuthorities::contains);
    }
}
