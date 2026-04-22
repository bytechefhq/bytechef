/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.repository;

import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayProvider;
import java.util.List;
import org.springframework.data.repository.ListCrudRepository;

/**
 * @version ee
 */
public interface AiGatewayProviderRepository extends ListCrudRepository<AiGatewayProvider, Long> {

    List<AiGatewayProvider> findAllByEnabled(boolean enabled);
}
