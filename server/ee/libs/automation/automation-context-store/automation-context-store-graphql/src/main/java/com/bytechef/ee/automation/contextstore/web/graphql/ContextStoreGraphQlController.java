/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.contextstore.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.automation.contextstore.facade.ContextStoreFacade;
import com.bytechef.ee.automation.contextstore.web.graphql.dto.CreateContextStoreGraphQlInput;
import com.bytechef.ee.automation.contextstore.web.graphql.dto.UpdateContextStoreGraphQlInput;
import com.bytechef.ee.platform.contextstore.domain.ContextStore;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.tag.domain.Tag;
import com.bytechef.platform.tag.service.TagService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

/**
 * GraphQL controller for the parent {@link ContextStore} entity. Sources, entities, and records are surfaced by
 * {@code ContextStoreSourceGraphQlController}; this controller only owns the parent-store CRUD. All mutations are
 * admin-only and route through {@link ContextStoreFacade}.
 *
 * <p>
 * Authorization is enforced on {@link ContextStoreFacade}, not here.
 * </p>
 *
 * @author Ivica Cardic
 * @version ee
 */
@Controller
@ConditionalOnEEVersion
@ConditionalOnCoordinator
@ConditionalOnProperty(prefix = "bytechef.context-store", name = "enabled", havingValue = "true")
public class ContextStoreGraphQlController {

    private final ContextStoreFacade contextStoreFacade;
    private final TagService tagService;

    @SuppressFBWarnings("EI")
    public ContextStoreGraphQlController(ContextStoreFacade contextStoreFacade, TagService tagService) {
        this.contextStoreFacade = contextStoreFacade;
        this.tagService = tagService;
    }

    @SchemaMapping(typeName = "ContextStore", field = "environment")
    public String environment(ContextStore contextStore) {
        return contextStore.getEnvironment()
            .name();
    }

    @SchemaMapping(typeName = "ContextStore", field = "tags")
    public List<Tag> tags(ContextStore contextStore) {
        List<Long> tagIds = contextStore.getTagIds();

        if (tagIds.isEmpty()) {
            return List.of();
        }

        return tagService.getTags(tagIds);
    }

    @QueryMapping
    public List<ContextStore> contextStores(@Argument Long workspaceId, @Argument Long environmentId) {
        return contextStoreFacade.getContextStores(workspaceId, environmentId);
    }

    @QueryMapping
    public ContextStore contextStore(@Argument Long id) {
        return contextStoreFacade.getContextStore(id);
    }

    @QueryMapping
    public List<Tag> contextStoreTags(@Argument Long workspaceId) {
        return contextStoreFacade.getContextStoreTags(workspaceId);
    }

    @QueryMapping
    public Long contextStoreIdByName(
        @Argument Long workspaceId, @Argument String name, @Argument Long environmentId) {

        // Returns null (mapped to GraphQL null) when there's no match — caller decides whether the absence is a
        // workflow misconfiguration or an acceptable "store doesn't exist in this env yet" fallback.
        return contextStoreFacade.findContextStoreIdByName(workspaceId, name, environmentId);
    }

    @MutationMapping
    public ContextStore createContextStore(
        @Argument Long workspaceId, @Argument Long environmentId, @Argument CreateContextStoreGraphQlInput input) {

        ContextStore contextStore = new ContextStore();

        contextStore.setName(input.name());
        contextStore.setDescription(input.description());
        contextStore.setTagIds(input.tagIds());

        return contextStoreFacade.createContextStore(contextStore, workspaceId, environmentId);
    }

    @MutationMapping
    public ContextStore updateContextStore(
        @Argument Long workspaceId, @Argument Long id, @Argument UpdateContextStoreGraphQlInput input) {

        ContextStore contextStore = new ContextStore();

        contextStore.setId(id);
        contextStore.setName(input.name());
        contextStore.setDescription(input.description());
        contextStore.setTagIds(input.tagIds());
        contextStore.setVersion(input.version());

        return contextStoreFacade.updateContextStore(workspaceId, contextStore);
    }

    @MutationMapping
    public List<Tag> updateContextStoreTags(
        @Argument Long workspaceId, @Argument Long id, @Argument List<Tag> tags) {

        return contextStoreFacade.updateContextStoreTags(workspaceId, id, tags);
    }

    @MutationMapping
    public boolean deleteContextStore(@Argument Long workspaceId, @Argument Long id) {
        contextStoreFacade.deleteContextStore(workspaceId, id);

        return true;
    }
}
