/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.repository;

import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayBudget;
import java.util.Optional;
import org.springframework.data.repository.ListCrudRepository;

/**
 * @version ee
 */
public interface AiGatewayBudgetRepository extends ListCrudRepository<AiGatewayBudget, Long> {

    /**
     * Returns the budget owned by the given workspace, if any. A budget with a null {@code workspace_id} belongs to no
     * workspace and is therefore never returned here — SQL equality never matches NULL.
     */
    Optional<AiGatewayBudget> findByWorkspaceId(long workspaceId);
}
