/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.domain;

import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.platform.security.domain.ResourceVisibility;
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

/**
 * An AI-built application that reaches third-party systems through ByteChef. The environment, the secret key and the
 * bindings belong to its instances.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Table("app")
public class App {

    @Column("category_id")
    private @Nullable Long categoryId;

    @CreatedBy
    @Column("created_by")
    private String createdBy;

    @Column("created_date")
    @CreatedDate
    private Instant createdDate;

    @Column
    private @Nullable String description;

    @Column
    private @Nullable FileEntry icon;

    @Id
    private Long id;

    @Column("last_modified_by")
    @LastModifiedBy
    private String lastModifiedBy;

    @Column("last_modified_date")
    @LastModifiedDate
    private Instant lastModifiedDate;

    @Column
    private String name;

    @Column("owner_user_id")
    private Long ownerUserId;

    @Column("portal_discoverable")
    private boolean portalDiscoverable;

    @Column
    private int status;

    @MappedCollection(idColumn = "app_id")
    private Set<AppTag> appTags = new HashSet<>();

    @Column
    private String uuid;

    @Version
    private int version;

    @Column
    private int visibility;

    @Column("workspace_id")
    private Long workspaceId;

    public App() {
    }

    public AppStatus getStatus() {
        return AppStatus.values()[status];
    }

    public void setStatus(AppStatus status) {
        this.status = status.ordinal();
    }

    public ResourceVisibility getVisibility() {
        return ResourceVisibility.values()[visibility];
    }

    public void setVisibility(ResourceVisibility visibility) {
        this.visibility = visibility.ordinal();
    }

    public @Nullable Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(@Nullable Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Instant createdDate) {
        this.createdDate = createdDate;
    }

    public @Nullable String getDescription() {
        return description;
    }

    public void setDescription(@Nullable String description) {
        this.description = description;
    }

    public @Nullable FileEntry getIcon() {
        return icon;
    }

    public void setIcon(@Nullable FileEntry icon) {
        this.icon = icon;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public void setLastModifiedBy(String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }

    public Instant getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setLastModifiedDate(Instant lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getOwnerUserId() {
        return ownerUserId;
    }

    public void setOwnerUserId(Long ownerUserId) {
        this.ownerUserId = ownerUserId;
    }

    public boolean isPortalDiscoverable() {
        return portalDiscoverable;
    }

    public void setPortalDiscoverable(boolean portalDiscoverable) {
        this.portalDiscoverable = portalDiscoverable;
    }

    public List<Long> getTagIds() {
        return appTags.stream()
            .map(AppTag::getTagId)
            .sorted()
            .toList();
    }

    public void setTagIds(List<Long> tagIds) {
        Set<AppTag> newAppTags = new HashSet<>();

        for (Long tagId : tagIds) {
            newAppTags.add(new AppTag(tagId));
        }

        this.appTags = newAppTags;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public Long getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(Long workspaceId) {
        this.workspaceId = workspaceId;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof App that)) {
            return false;
        }

        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "App{id=" + id + ", name='" + name + "', workspaceId=" + workspaceId + ", status=" + status + '}';
    }
}
