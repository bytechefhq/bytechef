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

package com.bytechef.ai.mcp.server.configuration.web.graphql;

import com.bytechef.ai.mcp.server.configuration.service.ManagementMcpServerService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/**
 * GraphQL controller for managing MCP Server configuration. Delegates to {@link ManagementMcpServerService}, where the
 * tenant-admin authorization for the secret-exposing platform MCP URL lives.
 *
 * @author Ivica Cardic
 */
@Controller
class ManagementMcpServerGraphQlController {

    private final ManagementMcpServerService managementMcpServerService;

    @SuppressFBWarnings("EI")
    ManagementMcpServerGraphQlController(ManagementMcpServerService managementMcpServerService) {
        this.managementMcpServerService = managementMcpServerService;
    }

    @QueryMapping
    String managementMcpServerUrl() {
        return managementMcpServerService.getManagementMcpServerUrl();
    }

    @MutationMapping
    String updateManagementMcpServerUrl() {
        return managementMcpServerService.updateManagementMcpServerUrl();
    }

    @QueryMapping
    boolean managementMcpServerAuthenticationRequired() {
        return managementMcpServerService.isAuthenticationRequired();
    }

    @MutationMapping
    boolean updateManagementMcpServerAuthenticationRequired(@Argument boolean authenticationRequired) {
        return managementMcpServerService.updateAuthenticationRequired(authenticationRequired);
    }
}
