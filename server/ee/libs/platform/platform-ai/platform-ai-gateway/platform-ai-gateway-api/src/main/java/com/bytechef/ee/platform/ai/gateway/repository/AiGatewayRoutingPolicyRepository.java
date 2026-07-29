/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.repository;

import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayRoutingPolicy;
import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.ListCrudRepository;

/**
 * @version ee
 */
public interface AiGatewayRoutingPolicyRepository extends ListCrudRepository<AiGatewayRoutingPolicy, Long> {

    Optional<AiGatewayRoutingPolicy> findByName(String name);

    List<AiGatewayRoutingPolicy> findAllByEnabled(boolean enabled);

    /**
     * Returns the routing policies owned by the given workspace. A policy with a null {@code workspace_id} belongs to
     * no workspace and is therefore never returned here — SQL equality never matches NULL.
     */
    List<AiGatewayRoutingPolicy> findAllByWorkspaceId(long workspaceId);
}
