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
 */
public interface AiGatewayContextCompressor {

    /**
     * Compress messages by applying JSON minification and message trimming.
     *
     * @param messages         the original messages
     * @param maxTokenEstimate the target max token count (0 = no trimming, only minification)
     * @return compressed messages
     */
    List<AiGatewayChatMessage> compress(List<AiGatewayChatMessage> messages, int maxTokenEstimate);
}
