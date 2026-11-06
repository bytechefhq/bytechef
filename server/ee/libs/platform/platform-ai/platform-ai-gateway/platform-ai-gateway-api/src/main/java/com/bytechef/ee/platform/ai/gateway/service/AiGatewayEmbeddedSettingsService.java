/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.service;

import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayEmbeddedSettings;
import java.util.Optional;

/**
 * @version ee
 */
public interface AiGatewayEmbeddedSettingsService {

    Optional<AiGatewayEmbeddedSettings> find(long environmentId);

    AiGatewayEmbeddedSettings upsert(AiGatewayEmbeddedSettings settings);
}
