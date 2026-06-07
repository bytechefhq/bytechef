/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.contextstore.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.automation.contextstore.facade.ContextStoreSourceFacade;
import com.bytechef.ee.automation.contextstore.service.WorkspaceContextStoreSourceService;
import com.bytechef.ee.automation.contextstore.web.graphql.dto.ContextStoreSourceFilter;
import com.bytechef.ee.automation.contextstore.web.graphql.dto.CreateContextStoreSourceGraphQlInput;
import com.bytechef.ee.automation.contextstore.web.graphql.dto.UpdateContextStoreSourceGraphQlInput;
import com.bytechef.ee.platform.contextstore.domain.ContextStoreSource;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

/**
 * GraphQL controller for the Context Store source surface. All mutations are admin-only and route through
 * {@link ContextStoreSourceFacade} — the same code path used by the AI Hub chat-define tools and the UI, so there is no
 * parallel implementation to maintain. Workspace-scoped reads flow through {@link WorkspaceContextStoreSourceService}
 * which joins the relation table with the platform-side source service.
 *
 * <p>
 * Source absorbed the former Entity layer in Phase 2: the per-source record-shape fields ({@code entityName},
 * {@code idField}, {@code indexedFields}, etc.) live directly on the source row, so there are no separate entity
 * mutations or resolvers.
 * </p>
 *
 * <p>
 * Authorization is enforced on {@link ContextStoreSourceFacade}, not here.
 * </p>
 *
 * @author Ivica Cardic
 * @version ee
 */
@Controller
@ConditionalOnEEVersion
@ConditionalOnCoordinator
@ConditionalOnProperty(prefix = "bytechef.context-store", name = "enabled", havingValue = "true")
public class ContextStoreSourceGraphQlController {

    private final ContextStoreSourceFacade contextStoreSourceFacade;
    private final WorkspaceContextStoreSourceService workspaceContextStoreSourceService;

    @SuppressFBWarnings("EI")
    public ContextStoreSourceGraphQlController(
        ContextStoreSourceFacade contextStoreSourceFacade,
        WorkspaceContextStoreSourceService workspaceContextStoreSourceService) {

        this.contextStoreSourceFacade = contextStoreSourceFacade;
        this.workspaceContextStoreSourceService = workspaceContextStoreSourceService;
    }

    @QueryMapping
    public ContextStoreSource contextStoreSource(@Argument Long id) {
        return contextStoreSourceFacade.getContextStoreSource(id);
    }

    @QueryMapping
    public List<ContextStoreSource> contextStoreSources(
        @Argument Long workspaceId, @Argument Long environmentId, @Argument ContextStoreSourceFilter filter) {

        boolean onlyEnabled = filter != null && Boolean.TRUE.equals(filter.enabled());

        return contextStoreSourceFacade.getContextStoreSources(workspaceId, environmentId, onlyEnabled);
    }

    @SchemaMapping(typeName = "ContextStoreSource", field = "workspaceId")
    public Long workspaceId(ContextStoreSource source) {
        return workspaceContextStoreSourceService.fetchWorkspaceIdByContextStoreSourceId(source.getId())
            .orElse(null);
    }

    @MutationMapping
    public ContextStoreSource createContextStoreSource(@Argument CreateContextStoreSourceGraphQlInput input) {
        return contextStoreSourceFacade.createContextStoreSource(input.workspaceId(), input.toFacadeInput());
    }

    @MutationMapping
    public ContextStoreSource updateContextStoreSource(
        @Argument Long id, @Argument UpdateContextStoreSourceGraphQlInput input) {

        return contextStoreSourceFacade.updateContextStoreSource(id, input.toFacadeInput());
    }

    @MutationMapping
    public boolean deleteContextStoreSource(@Argument Long id) {
        contextStoreSourceFacade.deleteContextStoreSource(id);

        return true;
    }

    @MutationMapping
    public Long refreshContextStoreSource(@Argument Long id) {
        return contextStoreSourceFacade.refreshContextStoreSource(id);
    }

    @MutationMapping
    public ContextStoreSource setContextStoreSourceEnabled(@Argument Long id, @Argument boolean enabled) {
        return contextStoreSourceFacade.setContextStoreSourceEnabled(id, enabled);
    }
}
