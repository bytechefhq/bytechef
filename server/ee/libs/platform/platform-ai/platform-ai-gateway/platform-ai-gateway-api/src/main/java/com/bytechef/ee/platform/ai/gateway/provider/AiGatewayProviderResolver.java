/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.provider;

import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayProvider;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayProviderType;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AiGatewayProviderResolver {
    AiGatewayProvider resolveTenantProvider(AiGatewayProviderType type);
}
