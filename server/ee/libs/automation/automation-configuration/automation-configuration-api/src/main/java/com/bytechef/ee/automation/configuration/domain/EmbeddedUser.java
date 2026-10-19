/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.domain;

import com.bytechef.ee.automation.configuration.security.constant.WorkspaceRole;
import com.bytechef.platform.configuration.domain.Environment;
import java.time.Instant;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.util.Assert;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Table("embedded_user")
public final class EmbeddedUser {

    @CreatedBy
    @Column("created_by")
    private String createdBy;

    @Column("created_date")
    @CreatedDate
    private Instant createdDate;

    @Column("custom_role_id")
    private Long customRoleId;

    @Column("environment")
    private Integer environment;

    @Id
    private Long id;

    @Column("last_modified_by")
    @LastModifiedBy
    private String lastModifiedBy;

    @Column("last_modified_date")
    @LastModifiedDate
    private Instant lastModifiedDate;

    @Column("user_id")
    private Long userId;

    @Version
    private int version;

    @Column("embedded_role")
    private Integer embeddedRole;

    private EmbeddedUser() {
    }

    private EmbeddedUser(long userId, Integer embeddedRole, Long customRoleId) {
        boolean hasBuiltInRole = embeddedRole != null;
        boolean hasCustomRole = customRoleId != null;

        Assert.isTrue(
            hasBuiltInRole ^ hasCustomRole,
            "Exactly one of 'embeddedRole' or 'customRoleId' must be set (XOR invariant)");

        if (hasBuiltInRole) {
            WorkspaceRole[] values = WorkspaceRole.values();

            Assert.isTrue(
                embeddedRole >= 0 && embeddedRole < values.length,
                "'embeddedRole' ordinal " + embeddedRole + " is out of range [0," + values.length + ")");
        }

        this.userId = userId;
        this.embeddedRole = embeddedRole;
        this.customRoleId = customRoleId;
    }

    public static EmbeddedUser forRole(long userId, WorkspaceRole embeddedRole) {
        Assert.notNull(embeddedRole, "'embeddedRole' must not be null");

        return new EmbeddedUser(userId, embeddedRole.ordinal(), null);
    }

    public static EmbeddedUser forRole(long userId, WorkspaceRole embeddedRole, @Nullable Environment environment) {
        EmbeddedUser embeddedUser = forRole(userId, embeddedRole);

        embeddedUser.environment = environment == null ? null : environment.ordinal();

        return embeddedUser;
    }

    public static EmbeddedUser forCustomRole(long userId, long customRoleId) {
        Assert.isTrue(customRoleId > 0, "'customRoleId' must be positive");

        return new EmbeddedUser(userId, null, customRoleId);
    }

    public static EmbeddedUser forCustomRole(long userId, long customRoleId, @Nullable Environment environment) {
        EmbeddedUser embeddedUser = forCustomRole(userId, customRoleId);

        embeddedUser.environment = environment == null ? null : environment.ordinal();

        return embeddedUser;
    }

    public void assignRole(WorkspaceRole embeddedRole) {
        Assert.notNull(embeddedRole, "'embeddedRole' must not be null");

        this.embeddedRole = embeddedRole.ordinal();
        this.customRoleId = null;
    }

    public void assignCustomRole(long customRoleId) {
        Assert.isTrue(customRoleId > 0, "'customRoleId' must be positive");

        this.customRoleId = customRoleId;
        this.embeddedRole = null;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public @Nullable Long getCustomRoleId() {
        return customRoleId;
    }

    public @Nullable Environment getEnvironment() {
        if (environment == null) {
            return null;
        }

        Environment[] environments = Environment.values();

        if (environment < 0 || environment >= environments.length) {
            throw new IllegalStateException(
                "embedded_user id=" + id + " stores unknown environment ordinal " + environment);
        }

        return environments[environment];
    }

    public @Nullable WorkspaceRole getEmbeddedRole() {
        if (embeddedRole == null) {
            return null;
        }

        WorkspaceRole[] roles = WorkspaceRole.values();

        if (embeddedRole < 0 || embeddedRole >= roles.length) {
            throw new IllegalStateException(
                "embedded_user id=" + id + " stores unknown embedded role ordinal " + embeddedRole);
        }

        return roles[embeddedRole];
    }

    public Long getId() {
        return id;
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public Instant getLastModifiedDate() {
        return lastModifiedDate;
    }

    public Long getUserId() {
        return userId;
    }

    public int getVersion() {
        return version;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (object == null || getClass() != object.getClass()) {
            return false;
        }

        EmbeddedUser embeddedUser = (EmbeddedUser) object;

        if (id == null) {
            return false;
        }

        return Objects.equals(id, embeddedUser.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "EmbeddedUser{" +
            "id=" + id +
            ", userId=" + userId +
            ", embeddedRole=" + embeddedRole +
            ", customRoleId=" + customRoleId +
            ", environment=" + environment +
            ", createdBy='" + createdBy + '\'' +
            ", createdDate=" + createdDate +
            ", lastModifiedBy='" + lastModifiedBy + '\'' +
            ", lastModifiedDate=" + lastModifiedDate +
            ", version=" + version +
            '}';
    }
}
