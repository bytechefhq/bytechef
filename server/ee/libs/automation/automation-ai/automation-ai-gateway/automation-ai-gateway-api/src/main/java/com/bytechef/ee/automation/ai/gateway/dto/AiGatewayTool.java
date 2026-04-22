/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.dto;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;

/**
 * @version ee
 */
@SuppressFBWarnings("EI")
public record AiGatewayTool(String type, AiGatewayToolFunction function) {

    /**
     * @version ee
     */
    @SuppressFBWarnings("EI")
    public record AiGatewayToolFunction(String name, String description, Map<String, Object> parameters) {
    }
}
