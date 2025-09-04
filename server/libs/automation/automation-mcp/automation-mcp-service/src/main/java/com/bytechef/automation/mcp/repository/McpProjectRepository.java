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

package com.bytechef.automation.mcp.repository;

import com.bytechef.automation.mcp.domain.McpProject;
import java.util.List;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for managing {@link McpProject} entities.
 */
@Repository
public interface McpProjectRepository extends ListCrudRepository<McpProject, Long> {

    /**
     * Find all projects associated with a specific MCP server.
     *
     * @param mcpServerId the ID of the MCP server
     * @return list of projects associated with the specified server
     */
    List<McpProject> findAllByMcpServerId(Long mcpServerId);
}
