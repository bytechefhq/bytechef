/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.repository;

import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayTag;
import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.ListCrudRepository;

/**
 * @version ee
 */
public interface AiGatewayTagRepository extends ListCrudRepository<AiGatewayTag, Long> {

    List<AiGatewayTag> findAllByWorkspaceIdOrderByNameAsc(Long workspaceId);

    Optional<AiGatewayTag> findByWorkspaceIdAndName(Long workspaceId, String name);
}
