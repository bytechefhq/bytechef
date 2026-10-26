/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.dto;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record AiGatewayTool(String type, AiGatewayToolFunction function) {

    @SuppressFBWarnings("EI")
    public record AiGatewayToolFunction(String name, String description, Map<String, Object> parameters) {
    }
}
