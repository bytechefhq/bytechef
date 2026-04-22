/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.service;

import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayCustomProperty;
import java.util.List;

/**
 * @version ee
 */
public interface AiGatewayCustomPropertyService {

    AiGatewayCustomProperty create(AiGatewayCustomProperty customProperty);

    List<AiGatewayCustomProperty> createAll(List<AiGatewayCustomProperty> customProperties);

    List<AiGatewayCustomProperty> getCustomPropertiesByTraceId(long traceId);

    List<AiGatewayCustomProperty> getCustomPropertiesByRequestLogId(long requestLogId);
}
