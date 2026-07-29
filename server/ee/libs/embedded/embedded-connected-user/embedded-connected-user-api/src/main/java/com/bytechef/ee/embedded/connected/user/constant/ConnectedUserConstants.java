/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.connected.user.constant;

import java.util.Set;

/**
 * Canonical home for the reserved-word / reserved-path-segment allowlist shared between
 * {@code ConnectedUserServiceImpl} (rejects the externalId at creation time) and
 * {@code EmbeddedApiKeyAuthenticationConverter} (rejects a non-JWT token whose URL first segment is one of these
 * words). Both modules already depend on {@code embedded-connected-user-api}, so this class avoids introducing a new
 * inter-module dependency.
 *
 * <p>
 * Keeping a single set closes the collision at BOTH ends: a real connected user can never be created with an externalId
 * that the security converter would otherwise treat as a reserved "Frontend" route segment (which used to cause silent
 * 401s on sub-resource routes), and any new no-{@code {externalUserId}} route added under {@code /api/embedded/v<n>/}
 * only needs its literal first path segment added here once.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class ConnectedUserConstants {

    /**
     * Literal first path segments of every "Frontend" (no-{@code {externalUserId}}) route mounted under
     * {@code /api/embedded/v<n>/}, traceable to their {@code openapi.yaml}:
     * <ul>
     * <li>{@code automation}, {@code me}, {@code components}, {@code integration-instances}, {@code integrations} --
     * embedded-configuration-public-rest</li>
     * <li>{@code app-events}, {@code workflows} -- embedded-webhook-public-rest</li>
     * <li>{@code unified} -- embedded-unified-rest ({@code /v1/unified/accounting/**}, {@code /v1/unified/crm/**})</li>
     * </ul>
     * This is a maintained allowlist, not a route-agnostic derivation -- it must be kept in sync by hand when a new
     * no-{@code {externalUserId}} route is added under {@code /api/embedded/v<n>/}.
     *
     * <p>
     * {@code external} is a RETIRED literal: the MCP integration-instance controllers briefly mounted routes under
     * {@code /external/{externalUserId}/...}, letting the converter capture the literal {@code external} as a user id
     * and mint a phantom connected user. The routes are normalized to the bare {@code /{externalUserId}/} prefix, and
     * the segment stays reserved so a stale caller of the old URL is rejected at authentication instead of minting.
     */
    public static final Set<String> FRONTEND_RESERVED_PATH_SEGMENTS = Set.of(
        "app-events", "automation", "components", "external", "integration-instances", "integrations", "me",
        "unified", "workflows");

    private ConnectedUserConstants() {
    }
}
