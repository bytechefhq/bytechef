/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.mcpserver.repository;

import com.bytechef.ee.ai.hub.mcpserver.AiHubMcpServer;
import java.util.List;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Repository
public interface AiHubMcpServerRepository extends ListCrudRepository<AiHubMcpServer, Long> {

    List<AiHubMcpServer> findAllByUserIdAndWorkspaceId(long userId, long workspaceId);
}
