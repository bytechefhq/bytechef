/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.mcpserver.repository;

import com.bytechef.ee.ai.hub.mcpserver.AiHubMcpServerTool;
import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Repository
public interface AiHubMcpServerToolRepository extends ListCrudRepository<AiHubMcpServerTool, Long> {

    List<AiHubMcpServerTool> findAllByMcpServerId(long mcpServerId);

    Optional<AiHubMcpServerTool> findByMcpServerIdAndName(long mcpServerId, String name);
}
