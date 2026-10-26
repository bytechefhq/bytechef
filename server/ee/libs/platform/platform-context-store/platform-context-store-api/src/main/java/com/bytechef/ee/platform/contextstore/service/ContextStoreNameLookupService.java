/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.contextstore.service;

import java.util.Optional;

/**
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
