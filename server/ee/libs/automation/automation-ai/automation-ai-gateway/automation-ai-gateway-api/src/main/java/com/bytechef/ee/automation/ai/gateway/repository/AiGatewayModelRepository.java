/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.repository;

import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayModel;
import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.ListCrudRepository;

/**
 * @version ee
 */
public interface AiGatewayModelRepository extends ListCrudRepository<AiGatewayModel, Long> {

    List<AiGatewayModel> findAllByProviderId(Long providerId);

    List<AiGatewayModel> findAllByEnabled(boolean enabled);

    Optional<AiGatewayModel> findByProviderIdAndName(Long providerId, String name);
}
