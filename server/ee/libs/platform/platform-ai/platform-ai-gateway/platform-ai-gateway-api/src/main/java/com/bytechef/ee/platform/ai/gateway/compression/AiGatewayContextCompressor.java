/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.compression;

import com.bytechef.ee.platform.ai.gateway.dto.AiGatewayChatMessage;
import java.util.List;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AiGatewayContextCompressor {

    List<AiGatewayChatMessage> compress(List<AiGatewayChatMessage> messages, int maxTokenEstimate);
}
