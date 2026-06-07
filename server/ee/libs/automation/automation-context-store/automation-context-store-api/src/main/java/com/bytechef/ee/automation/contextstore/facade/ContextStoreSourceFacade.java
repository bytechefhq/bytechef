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
 * {@code WorkspaceContextStoreSourceFacade} which the AI Hub chat-define tools and data plane rely on. Delegates to the
 * existing shared collaborators.
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
