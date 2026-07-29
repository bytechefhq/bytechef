/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.facade;

import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayProjectSettings;
import java.util.Optional;

/**
 * Facade for managing AI LLM Gateway project-scoped guardrail overrides. Hosts the authorization guards so they apply
 * to every caller of the facade rather than only the GraphQL entry point, and keeps them off the shared
 * {@code AiGatewayProjectSettingsService} which the gateway data plane relies on.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AiGatewayProjectSettingsFacade {

    Optional<AiGatewayProjectSettings> findByProjectId(Long projectId);

    AiGatewayProjectSettings upsert(AiGatewayProjectSettings settings);
}
