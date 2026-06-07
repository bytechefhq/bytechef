/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.contextstore.facade;

import com.bytechef.ee.automation.contextstore.dto.CreateContextStoreSourceInput;
import com.bytechef.ee.automation.contextstore.dto.UpdateContextStoreSourceInput;
import com.bytechef.ee.automation.contextstore.service.WorkspaceContextStoreSourceService;
import com.bytechef.ee.platform.contextstore.domain.ContextStoreSource;
import com.bytechef.ee.platform.contextstore.service.ContextStoreSourceService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.security.constant.AuthorityConstants;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * Implementation of {@link ContextStoreSourceFacade}. Delegates to the shared {@link ContextStoreSourceService},
 * {@link WorkspaceContextStoreSourceFacade}, and {@link WorkspaceContextStoreSourceService}, and carries the
 * {@code ADMIN} authorization guard so it is enforced for every caller of the facade.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.context-store", name = "enabled", havingValue = "true")
class ContextStoreSourceFacadeImpl implements ContextStoreSourceFacade {

    private final ContextStoreSourceService contextStoreSourceService;
    private final WorkspaceContextStoreSourceFacade workspaceContextStoreSourceFacade;
    private final WorkspaceContextStoreSourceService workspaceContextStoreSourceService;

    @SuppressFBWarnings("EI")
    ContextStoreSourceFacadeImpl(
        ContextStoreSourceService contextStoreSourceService,
        WorkspaceContextStoreSourceFacade workspaceContextStoreSourceFacade,
        WorkspaceContextStoreSourceService workspaceContextStoreSourceService) {

        this.contextStoreSourceService = contextStoreSourceService;
        this.workspaceContextStoreSourceFacade = workspaceContextStoreSourceFacade;
        this.workspaceContextStoreSourceService = workspaceContextStoreSourceService;
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public ContextStoreSource getContextStoreSource(Long id) {
        return contextStoreSourceService.fetch(id)
            .orElse(null);
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public List<ContextStoreSource> getContextStoreSources(Long workspaceId, Long environmentId, boolean onlyEnabled) {
        if (onlyEnabled) {
            return workspaceContextStoreSourceService.getAllEnabledSourcesByWorkspaceId(workspaceId, environmentId);
        }

        return workspaceContextStoreSourceService.getAllSourcesByWorkspaceId(workspaceId, environmentId);
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public ContextStoreSource createContextStoreSource(Long workspaceId, CreateContextStoreSourceInput input) {
        return workspaceContextStoreSourceFacade.create(workspaceId, input);
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public ContextStoreSource updateContextStoreSource(Long id, UpdateContextStoreSourceInput input) {
        Long workspaceId = workspaceContextStoreSourceService.fetchWorkspaceIdByContextStoreSourceId(id)
            .orElseThrow(() -> new IllegalStateException(
                "ContextStoreSource " + id + " has no owning workspace"));

        return workspaceContextStoreSourceFacade.update(workspaceId, id, input);
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public void deleteContextStoreSource(Long id) {
        Long workspaceId = workspaceContextStoreSourceService.fetchWorkspaceIdByContextStoreSourceId(id)
            .orElseThrow(() -> new IllegalStateException(
                "ContextStoreSource " + id + " has no owning workspace"));

        workspaceContextStoreSourceFacade.delete(workspaceId, id);
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public Long refreshContextStoreSource(Long id) {
        Long workspaceId = workspaceContextStoreSourceService.fetchWorkspaceIdByContextStoreSourceId(id)
            .orElseThrow(() -> new IllegalStateException(
                "ContextStoreSource " + id + " has no owning workspace"));

        return workspaceContextStoreSourceFacade.refreshNow(workspaceId, id);
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public ContextStoreSource setContextStoreSourceEnabled(Long id, boolean enabled) {
        Long workspaceId = workspaceContextStoreSourceService.fetchWorkspaceIdByContextStoreSourceId(id)
            .orElseThrow(() -> new IllegalStateException(
                "ContextStoreSource " + id + " has no owning workspace"));

        workspaceContextStoreSourceFacade.setEnabled(workspaceId, id, enabled);

        return contextStoreSourceService.get(id);
    }
}
