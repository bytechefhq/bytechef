/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.contextstore.service;

import com.bytechef.ee.automation.contextstore.facade.WorkspaceContextStoreFacade;
import com.bytechef.ee.platform.contextstore.service.ContextStoreNameLookupService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Automation-side implementation of {@link ContextStoreNameLookupService}. Bridges the platform-layer SPI to the
 * workspace-scoped {@link WorkspaceContextStoreFacade#findContextStoreIdByName} so the
 * {@code contextStore.searchByStore} action — which lives in the workspace-agnostic component module — can perform
 * env-aware lookups without crossing a module-layering boundary.
 *
 * <p>
 * Gated on {@code bytechef.context-store.enabled} so the bean tree only materialises when the feature is on; the
 * platform-side action handles bean absence gracefully via {@code ObjectProvider}.
 *
 * @author Ivica Cardic
 * @version ee
 */
@Service
@ConditionalOnProperty(prefix = "bytechef.context-store", name = "enabled", havingValue = "true")
public class AutomationContextStoreNameLookupService implements ContextStoreNameLookupService {

    private final WorkspaceContextStoreFacade workspaceContextStoreFacade;

    @SuppressFBWarnings("EI2")
    public AutomationContextStoreNameLookupService(WorkspaceContextStoreFacade workspaceContextStoreFacade) {
        this.workspaceContextStoreFacade = workspaceContextStoreFacade;
    }

    @Override
    public Optional<Long> findIdByName(Long workspaceId, String name, Long environmentId) {
        return workspaceContextStoreFacade.findContextStoreIdByName(workspaceId, name, environmentId);
    }
}
