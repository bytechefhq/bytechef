/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.contextstore.facade;

import com.bytechef.ee.platform.contextstore.domain.ContextStore;
import com.bytechef.ee.platform.contextstore.service.ContextStoreService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.tag.domain.Tag;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * Implementation of {@link ContextStoreFacade}. Delegates to the shared {@link WorkspaceContextStoreFacade},
 * {@link ContextStoreService}, and {@link EnvironmentService}, and carries the {@code ADMIN} authorization guard so it
 * is enforced for every caller of the facade.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.context-store", name = "enabled", havingValue = "true")
class ContextStoreFacadeImpl implements ContextStoreFacade {

    private final ContextStoreService contextStoreService;
    private final EnvironmentService environmentService;
    private final WorkspaceContextStoreFacade workspaceContextStoreFacade;

    @SuppressFBWarnings("EI")
    ContextStoreFacadeImpl(
        ContextStoreService contextStoreService, EnvironmentService environmentService,
        WorkspaceContextStoreFacade workspaceContextStoreFacade) {

        this.contextStoreService = contextStoreService;
        this.environmentService = environmentService;
        this.workspaceContextStoreFacade = workspaceContextStoreFacade;
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public List<ContextStore> getContextStores(Long workspaceId, Long environmentId) {
        environmentService.getEnvironment(environmentId);

        return workspaceContextStoreFacade.getWorkspaceContextStores(workspaceId, environmentId);
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public ContextStore getContextStore(Long id) {
        return contextStoreService.fetch(id)
            .orElse(null);
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public List<Tag> getContextStoreTags(Long workspaceId) {
        return workspaceContextStoreFacade.getWorkspaceContextStoreTags(workspaceId);
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public Long findContextStoreIdByName(Long workspaceId, String name, Long environmentId) {
        environmentService.getEnvironment(environmentId);

        return workspaceContextStoreFacade.findContextStoreIdByName(workspaceId, name, environmentId)
            .orElse(null);
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public ContextStore createContextStore(ContextStore contextStore, Long workspaceId, Long environmentId) {
        environmentService.getEnvironment(environmentId);

        return workspaceContextStoreFacade.createWorkspaceContextStore(contextStore, workspaceId, environmentId);
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public ContextStore updateContextStore(Long workspaceId, ContextStore contextStore) {
        return workspaceContextStoreFacade.updateWorkspaceContextStore(workspaceId, contextStore);
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public List<Tag> updateContextStoreTags(Long workspaceId, Long id, List<Tag> tags) {
        return workspaceContextStoreFacade.updateWorkspaceContextStoreTags(workspaceId, id, tags);
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public void deleteContextStore(Long workspaceId, Long id) {
        workspaceContextStoreFacade.deleteWorkspaceContextStore(workspaceId, id);
    }
}
