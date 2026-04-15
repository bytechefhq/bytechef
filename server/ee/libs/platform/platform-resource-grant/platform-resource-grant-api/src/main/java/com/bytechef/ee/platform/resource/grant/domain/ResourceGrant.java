/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.resource.grant.domain;

import java.time.Instant;
import java.util.Objects;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/**
 * One user's access to one resource its owner has withheld.
 *
 * <p>
 * Conveys visibility only. What the recipient may then do with the resource is decided by the workspace-role scope
 * machinery, exactly as if the resource had been left visible to the whole workspace — so a grant restores default
 * access to one person rather than inventing a second permission vocabulary alongside {@code PermissionScope}.
 *
 * <p>
 * {@code resourceId} is polymorphic and carries no foreign key: the referenced row lives in whichever table
 * {@code resourceType} names. The application validates existence, workspace and type before writing, because a
 * database constraint cannot express that relationship.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Table("resource_grant")
public final class ResourceGrant {

    @Id
    private Long id;

    private String resourceType;

    private long resourceId;

    private long userId;

    @CreatedBy
    private String createdBy;

    @CreatedDate
    private Instant createdDate;

    public ResourceGrant() {
    }

    public ResourceGrant(String resourceType, long resourceId, long userId) {
        this.resourceType = Objects.requireNonNull(resourceType, "resourceType");
        this.resourceId = resourceId;
        this.userId = userId;
    }

    public Long getId() {
        return id;
    }

    public String getResourceType() {
        return resourceType;
    }

    public long getResourceId() {
        return resourceId;
    }

    public long getUserId() {
        return userId;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof ResourceGrant that)) {
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
        return "ResourceGrant{" +
            "id=" + id +
            ", resourceType='" + resourceType + '\'' +
            ", resourceId=" + resourceId +
            ", userId=" + userId +
            ", createdBy='" + createdBy + '\'' +
            ", createdDate=" + createdDate +
            '}';
    }
}
