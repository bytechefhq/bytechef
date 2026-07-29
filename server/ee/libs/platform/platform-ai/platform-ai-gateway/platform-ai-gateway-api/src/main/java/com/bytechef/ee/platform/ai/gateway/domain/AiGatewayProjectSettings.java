/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.domain;

/**
 * Project-scoped AI Gateway guardrail overrides that layer on top of the workspace/global guardrail policy. Persisted
 * as a single {@link com.bytechef.platform.configuration.domain.Property} row (scope={@code PROJECT},
 * key={@value #PROPERTY_KEY}) rather than a dedicated table — the platform property store already handles
 * scope/audit/versioning/encryption and this is plain config data. All fields are nullable; a null value means "inherit
 * from the workspace/global policy". Overrides are additive (they can enable a guardrail the workspace did not, and
 * their blocked terms union in) — they never turn a workspace/global guardrail off, matching the union semantics of the
 * workspace layer.
 *
 * @version ee
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
