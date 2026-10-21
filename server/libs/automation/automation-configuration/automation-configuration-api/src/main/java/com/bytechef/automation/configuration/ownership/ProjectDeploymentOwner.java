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

package com.bytechef.automation.configuration.ownership;

import java.util.List;

/**
 * @author Ivica Cardic
 */
public enum ProjectDeploymentOwner {

    API_COLLECTION("api_collection"),
    A2A_SERVER("a2a_project"),
    EMBEDDED("connected_user_project", "connected_user_reference_deployment"),
    MCP_SERVER("mcp_project");

    private final List<String> ownerTables;

    ProjectDeploymentOwner(String... ownerTables) {
        this.ownerTables = List.of(ownerTables);
    }

    public List<String> getOwnerTables() {
        return ownerTables;
    }
}
