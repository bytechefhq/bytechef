/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.contextstore.service;

import java.util.Optional;

/**
 * SPI for resolving a Context Store by its {@code (workspaceId, name, environmentId)} triple to a concrete
 * {@code contextStoreId}. Lives platform-side so the {@code contextStore} component module — which is forbidden from
 * depending on automation-layer services — can perform env-aware lookups via {@link java.util.Optional} graceful
 * fallback rather than hardcoding the workspace_context_store relation join here.
 *
 * <p>
 * The actual workspace-scoped name match is implemented automation-side (where the workspace_context_store relation
 * lives) and wired in via Spring DI. The platform-side action calls this SPI via {@code ObjectProvider} so deployments
 * without the automation-context-store module on the classpath start cleanly — the action's name-based path simply
 * becomes unreachable rather than blowing up at construction time.
 *
 * <p>
 * Concrete use case: the {@code contextStore.searchByStore} action, when invoked with a {@code contextStoreName} input,
 * resolves the name to an id at perform-time using the running workflow's workspace + environment. That lets a single
 * workflow definition promote across DEVELOPMENT / STAGING / PRODUCTION without hardcoded ids.
 *
 * @author Ivica Cardic
 * @version ee
 */
public interface ContextStoreNameLookupService {

    /**
     * Resolves the Context Store id for a {@code (workspaceId, name, environmentId)} triple. Returns
     * {@link Optional#empty()} when no store matches — callers (typically the search action) decide whether to surface
     * the absence as a clear "store not found in this env" error or fall back to a default.
     */
    Optional<Long> findIdByName(Long workspaceId, String name, Long environmentId);
}
