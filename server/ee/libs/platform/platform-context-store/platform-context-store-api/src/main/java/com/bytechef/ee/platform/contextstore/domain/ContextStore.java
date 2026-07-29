/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.contextstore.domain;

import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.tag.domain.Tag;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.util.CollectionUtils;

/**
 * Parent Context Store entity. Each row is an env-stamped logical store; sources, entities, records, and ClickHouse
 * projection tables hang off {@code context_store_id} and inherit the environment transitively. Mirrors
 * {@link com.bytechef.platform.knowledgebase.domain.KnowledgeBase}'s parent role over Knowledge Base Sources.
 *
 * <p>
 * A store belongs to at most one workspace, carried by the nullable {@code workspace_id} column. Uniqueness of
 * {@code (workspace_id, environment, name)} is enforced at the application layer in {@code WorkspaceContextStoreFacade}
 * rather than by a SQL UNIQUE, because a null workspace must not participate in it.
 * </p>
 *
 * @author Ivica Cardic
 * @version ee
 */
@Table("context_store")
public class ContextStore {

    @Id
    private Long id;

    private String name;

    @Nullable
    private String description;

    @Column
    private int environment;

    @MappedCollection(idColumn = "context_store_id")
    private Set<ContextStoreTag> contextStoreTags = new HashSet<>();

    @CreatedBy
    @Column("created_by")
    private String createdBy;

    @CreatedDate
    @Column("created_date")
    private Instant createdDate;

    @LastModifiedBy
    @Column("last_modified_by")
    private String lastModifiedBy;

    @LastModifiedDate
    @Column("last_modified_date")
    private Instant lastModifiedDate;

    @Version
    private int version;

    @Column("workspace_id")
    private @Nullable Long workspaceId;

    public ContextStore() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Nullable
    public String getDescription() {
        return description;
    }

    public void setDescription(@Nullable String description) {
        this.description = description;
    }

    public Environment getEnvironment() {
        Environment[] environments = Environment.values();

        if (environment < 0 || environment >= environments.length) {
            throw new IllegalStateException("Invalid environment value: " + environment);
        }

        return environments[environment];
    }

    public long getEnvironmentId() {
        return environment;
    }

    public void setEnvironment(Environment environment) {
        Objects.requireNonNull(environment, "environment");

        this.environment = environment.ordinal();
    }

    public List<Long> getTagIds() {
        return contextStoreTags
            .stream()
            .map(ContextStoreTag::getTagId)
            .toList();
    }

    public void setTagIds(@Nullable List<Long> tagIds) {
        this.contextStoreTags = new HashSet<>();

        if (!CollectionUtils.isEmpty(tagIds)) {
            for (Long tagId : tagIds) {
                contextStoreTags.add(new ContextStoreTag(tagId));
            }
        }
    }

    public void setTags(@Nullable List<Tag> tags) {
        if (CollectionUtils.isEmpty(tags)) {
            setTagIds(List.of());
        } else {
            setTagIds(tags.stream()
                .map(Tag::getId)
                .toList());
        }
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public Instant getLastModifiedDate() {
        return lastModifiedDate;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public @Nullable Long getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(@Nullable Long workspaceId) {
        this.workspaceId = workspaceId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof ContextStore that)) {
            return false;
        }

        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
