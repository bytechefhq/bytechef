/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.user.event;

/**
 * Published when an identity provider is created, updated, or deleted, so caches keyed on a tenant's identity providers
 * (e.g. the MCP per-tenant issuer cache) can invalidate promptly rather than waiting for their TTL.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public record IdentityProviderChangedEvent() {
}
