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

package com.bytechef.platform.configuration.repository;

import com.bytechef.platform.configuration.domain.McpServer;
import java.util.List;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for managing {@link McpServer} entities.
 *
 * @author Ivica Cardic
 */
@Repository
public interface McpServerRepository extends ListCrudRepository<McpServer, Long> {

    /**
     * Finds all MCP servers that have the specified tag.
     *
     * @param tagId the ID of the tag to filter by
     * @return a list of MCP servers with the specified tag
     */
    @Query("""
        SELECT mcp_server.* FROM mcp_server
        JOIN mcp_server_tag ON mcp_server.id = mcp_server_tag.mcp_server_id
        WHERE mcp_server_tag.tag_id = :tagId
        ORDER BY LOWER(mcp_server.name) ASC
        """)
    List<McpServer> findAllByTagId(@Param("tagId") Long tagId);
}
