/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.contextstore.facade;

import com.bytechef.ee.automation.contextstore.service.WorkspaceContextStoreService;
import com.bytechef.ee.platform.contextstore.domain.ContextStore;
import com.bytechef.ee.platform.contextstore.service.ContextStoreService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.tag.domain.Tag;
import com.bytechef.platform.tag.service.TagService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implements {@link WorkspaceContextStoreFacade}. Wraps {@link ContextStoreService} (platform-side) with the workspace
 * ↔ store relation, and enforces app-layer uniqueness of {@code (workspace, environment, name)}.
 *
 * @author Ivica Cardic
 * @version ee
 */
@Service
@Transactional
@ConditionalOnProperty(prefix = "bytechef.context-store", name = "enabled", havingValue = "true")
public class WorkspaceContextStoreFacadeImpl implements WorkspaceContextStoreFacade {

    private final ContextStoreService contextStoreService;
    private final TagService tagService;
    private final WorkspaceContextStoreService workspaceContextStoreService;

    @SuppressFBWarnings("EI2")
    public WorkspaceContextStoreFacadeImpl(
        ContextStoreService contextStoreService, TagService tagService,
        WorkspaceContextStoreService workspaceContextStoreService) {

        this.contextStoreService = contextStoreService;
        this.tagService = tagService;
        this.workspaceContextStoreService = workspaceContextStoreService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContextStore> getWorkspaceContextStores(Long workspaceId, long environmentId) {
        Environment environment = resolveEnvironment(environmentId);

        return workspaceContextStoreService.getAllStoresByWorkspaceIdAndEnvironment(workspaceId, environment);
    }

    @Override
    public ContextStore createWorkspaceContextStore(ContextStore contextStore, Long workspaceId, long environmentId) {
        Environment environment = resolveEnvironment(environmentId);

        contextStore.setEnvironment(environment);

        // App-layer uniqueness check on (workspace, environment, name): the relation row holds workspace_id but the
        // name lives on context_store, so a SQL UNIQUE can't span the two tables. Enforce before saving.
        boolean nameTaken = workspaceContextStoreService.getAllStoresByWorkspaceIdAndEnvironment(
            workspaceId, environment)
            .stream()
            .anyMatch(existing -> Objects.equals(existing.getName(), contextStore.getName()));

        if (nameTaken) {
            throw new IllegalArgumentException(
                "Context Store named '%s' already exists in this workspace at environment %s".formatted(
                    contextStore.getName(), environment.name()));
        }

        ContextStore created = contextStoreService.create(contextStore);

        workspaceContextStoreService.create(created.getId(), workspaceId);

        return created;
    }

    @Override
    public ContextStore updateWorkspaceContextStore(Long workspaceId, ContextStore contextStore) {
        Long contextStoreId = Objects.requireNonNull(contextStore.getId(), "contextStore.id");

        if (!workspaceContextStoreService.isStoreInWorkspace(workspaceId, contextStoreId)) {
            throw new IllegalArgumentException(
                "Context Store " + contextStoreId + " is not in workspace " + workspaceId);
        }

        ContextStore current = contextStoreService.get(contextStoreId);

        current.setName(contextStore.getName());
        current.setDescription(contextStore.getDescription());
        current.setTagIds(contextStore.getTagIds());
        current.setVersion(contextStore.getVersion());

        return contextStoreService.update(current);
    }

    @Override
    public void deleteWorkspaceContextStore(Long workspaceId, Long contextStoreId) {
        if (!workspaceContextStoreService.isStoreInWorkspace(workspaceId, contextStoreId)) {
            throw new IllegalArgumentException(
                "Context Store " + contextStoreId + " is not in workspace " + workspaceId);
        }

        workspaceContextStoreService.deleteByContextStoreId(contextStoreId);
        contextStoreService.delete(contextStoreId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Long> findContextStoreIdByName(Long workspaceId, String name, long environmentId) {
        Environment environment = resolveEnvironment(environmentId);

        // App-layer name uniqueness within (workspace, environment) is enforced on create/update, so at most one
        // match is possible. Reusing getAllStoresByWorkspaceIdAndEnvironment keeps the lookup honest with whatever
        // scoping the parent facade applies (relation-table join, environment ordinal coercion, etc.).
        return workspaceContextStoreService.getAllStoresByWorkspaceIdAndEnvironment(workspaceId, environment)
            .stream()
            .filter(store -> Objects.equals(store.getName(), name))
            .map(ContextStore::getId)
            .findFirst();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Tag> getWorkspaceContextStoreTags(Long workspaceId) {
        // Collect tag ids across every store in the workspace (any environment) and resolve once via TagService.
        // Using LinkedHashMap to dedupe while preserving first-seen order — gives the picker a stable list.
        Map<Long, Tag> distinctTags = new LinkedHashMap<>();

        List<Long> tagIds = workspaceContextStoreService.getAllStoresByWorkspaceId(workspaceId)
            .stream()
            .flatMap(store -> store.getTagIds()
                .stream())
            .distinct()
            .toList();

        if (tagIds.isEmpty()) {
            return List.of();
        }

        for (Tag tag : tagService.getTags(tagIds)) {
            distinctTags.put(tag.getId(), tag);
        }

        return new ArrayList<>(distinctTags.values());
    }

    @Override
    public List<Tag> updateWorkspaceContextStoreTags(Long workspaceId, Long contextStoreId, List<Tag> tags) {
        if (!workspaceContextStoreService.isStoreInWorkspace(workspaceId, contextStoreId)) {
            throw new IllegalArgumentException(
                "Context Store " + contextStoreId + " is not in workspace " + workspaceId);
        }

        // TagService.save resolves existing tags by id and persists new ones (id == null) — same pattern as
        // ProjectTagFacade / KB tag flow. Returned tags carry the persisted ids that we then write onto the store.
        List<Tag> resolvedTags = tagService.save(tags == null ? List.of() : tags);

        ContextStore current = contextStoreService.get(contextStoreId);

        current.setTags(resolvedTags);

        contextStoreService.update(current);

        return resolvedTags;
    }

    private Environment resolveEnvironment(long environmentId) {
        Environment[] environments = Environment.values();

        if (environmentId < 0 || environmentId >= environments.length) {
            throw new IllegalArgumentException("Invalid environmentId: " + environmentId);
        }

        return environments[(int) environmentId];
    }
}
