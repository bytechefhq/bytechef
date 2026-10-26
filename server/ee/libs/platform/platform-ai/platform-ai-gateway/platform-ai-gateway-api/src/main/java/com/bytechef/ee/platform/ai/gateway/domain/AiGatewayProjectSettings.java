/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.domain;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public record AiGatewayProjectSettings(
    Long projectId,
    Boolean redactPii,
    Boolean redactSecrets,
    String blockedTerms,
    Boolean moderationEnabled,
    Boolean injectionDetectionEnabled,
    Boolean scanResponses) {
    public static final String PROPERTY_KEY = "ai_gateway_project_settings";
}
