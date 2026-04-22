/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.repository;

import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayRateLimit;
import java.util.List;
import org.springframework.data.repository.ListCrudRepository;

/**
 * @version ee
 */
public interface AiGatewayRateLimitRepository extends ListCrudRepository<AiGatewayRateLimit, Long> {

    List<AiGatewayRateLimit> findByWorkspaceId(Long workspaceId);

    List<AiGatewayRateLimit> findByWorkspaceIdAndEnabled(Long workspaceId, boolean enabled);

    List<AiGatewayRateLimit> findByWorkspaceIdAndProjectId(Long workspaceId, Long projectId);
}
