/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.contextstore.facade;

import com.bytechef.ee.automation.contextstore.dto.CreateContextStoreSourceInput;
import com.bytechef.ee.automation.contextstore.dto.UpdateContextStoreSourceInput;
import com.bytechef.ee.platform.contextstore.domain.ContextStoreSource;
import java.util.List;

/**
 * Authorization-enforcing facade for the Context Store source surface. Hosts the {@code ADMIN} guard so it applies to
 * every caller rather than only the GraphQL entry point, and keeps it off the shared
 * {@code WorkspaceContextStoreSourceFacade}, which stays deliberately unguarded for the reads and the data plane.
 * Delegates to those existing shared collaborators.
 *
 * <p>
 * Every source <em>mutation</em> now goes through here, agent tools included: the five source-mutation tool callbacks
 * ({@code Create}/{@code Update}/{@code Delete}/{@code Refresh}/{@code SetContextStoreSourceEnabled}) that
 * {@code ContextStoreToolCallbacksFactory} builds are constructed over this facade, and their LLM-visible descriptions
 * state the admin requirement — so it has to hold on every surface they reach, not only the GraphQL one. Only the reads
 * ({@code listContextSources}, the search tools) still go straight to the unguarded workspace facade/service.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface ContextStoreSourceFacade {

    ContextStoreSource getContextStoreSource(Long id);

    List<ContextStoreSource> getContextStoreSources(Long workspaceId, Long environmentId, boolean onlyEnabled);

    ContextStoreSource createContextStoreSource(Long workspaceId, CreateContextStoreSourceInput input);

    ContextStoreSource updateContextStoreSource(Long id, UpdateContextStoreSourceInput input);

    void deleteContextStoreSource(Long id);

    Long refreshContextStoreSource(Long id);

    ContextStoreSource setContextStoreSourceEnabled(Long id, boolean enabled);
}
