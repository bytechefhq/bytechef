/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.repository;

import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayConnectedUserSettings;
import java.util.Optional;
import org.springframework.data.repository.ListCrudRepository;

/**
 * @version ee
 */
public interface AiGatewayConnectedUserSettingsRepository
    extends ListCrudRepository<AiGatewayConnectedUserSettings, Long> {

    long countByRoutingPolicyId(long routingPolicyId);

    void deleteByConnectedUserId(long connectedUserId);

    Optional<AiGatewayConnectedUserSettings> findByConnectedUserId(long connectedUserId);
}
