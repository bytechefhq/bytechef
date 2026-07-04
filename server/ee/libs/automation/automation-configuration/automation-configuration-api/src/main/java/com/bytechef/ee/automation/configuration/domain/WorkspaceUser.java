/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.domain;

import com.bytechef.ee.automation.configuration.security.constant.WorkspaceRole;
import java.time.Instant;
import java.util.Objects;
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
@Table("workspace_user")
public class WorkspaceUser {

    @CreatedBy
    @Column("created_by")
    private String createdBy;

    @Column("created_date")
    @CreatedDate
    private Instant createdDate;

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

    @Column("workspace_id")
    private Long workspaceId;

    @Column("workspace_role")
    private Integer workspaceRole;

    @Column("custom_role_id")
    private Long customRoleId;

    /**
     * Required by Spring Data JDBC reflection. Application code must use {@link #forRole(Long, Long, WorkspaceRole)} or
     * {@link #forCustomRole(Long, Long, long)} so exactly one of the role columns is supplied.
     */
    private WorkspaceUser() {
    }

    /**
     * Raw-ordinal constructor. Prefer {@link #forRole(Long, Long, WorkspaceRole)} — this overload exists for tests and
     * low-level code paths that already hold a validated ordinal (e.g., deserialization), not for typical callers.
     * Using the typed factory prevents primitive-obsession mistakes where the three arguments (two ids + a role index)
     * can be swapped silently at the call site.
     */
    public WorkspaceUser(Long userId, Long workspaceId, int workspaceRole) {
        this(userId, workspaceId, workspaceRole, null);
    }

    /**
     * Internal constructor used by the {@link #forRole(Long, Long, WorkspaceRole)} and
     * {@link #forCustomRole(Long, Long, long)} factories. Enforces the XOR invariant — exactly one of
     * {@code workspaceRole} or {@code customRoleId} is non-null — and validates the ordinal against
     * {@link WorkspaceRole}. Kept package-private so external callers cannot bypass the factories.
     */
    WorkspaceUser(Long userId, Long workspaceId, Integer workspaceRole, Long customRoleId) {
        Assert.notNull(userId, "'userId' must not be null");
        Assert.notNull(workspaceId, "'workspaceId' must not be null");

        boolean hasBuiltInRole = workspaceRole != null;
        boolean hasCustomRole = customRoleId != null;

        Assert.isTrue(
            hasBuiltInRole ^ hasCustomRole,
            "Exactly one of 'workspaceRole' or 'customRoleId' must be set (XOR invariant)");

        if (hasBuiltInRole) {
            WorkspaceRole[] values = WorkspaceRole.values();

            Assert.isTrue(
                workspaceRole >= 0 && workspaceRole < values.length,
                "'workspaceRole' ordinal " + workspaceRole + " is out of range [0," + values.length + ")");
        }

        this.userId = userId;
        this.workspaceId = workspaceId;
        this.workspaceRole = workspaceRole;
        this.customRoleId = customRoleId;
    }

    /**
     * Preferred factory: constructs a {@code WorkspaceUser} with a typed built-in role. Callers cannot silently pass an
     * out-of-range ordinal or transpose role/workspace arguments.
     */
    public static WorkspaceUser forRole(Long userId, Long workspaceId, WorkspaceRole workspaceRole) {
        Assert.notNull(workspaceRole, "'workspaceRole' must not be null");

        return new WorkspaceUser(userId, workspaceId, workspaceRole.ordinal(), null);
    }

    /**
     * Creates a workspace membership backed by a custom role (no built-in {@code workspaceRole}).
     */
    public static WorkspaceUser forCustomRole(Long userId, Long workspaceId, long customRoleId) {
        Assert.isTrue(customRoleId > 0, "'customRoleId' must be positive");

        return new WorkspaceUser(userId, workspaceId, null, customRoleId);
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedDate() {
        return createdDate;
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

    public Long getWorkspaceId() {
        return workspaceId;
    }

    public Integer getWorkspaceRole() {
        return workspaceRole;
    }

    public Long getCustomRoleId() {
        return customRoleId;
    }

    /**
     * Assigns a built-in role, clearing any custom-role assignment to preserve the XOR invariant.
     */
    public void assignRole(WorkspaceRole workspaceRole) {
        Assert.notNull(workspaceRole, "'workspaceRole' must not be null");

        this.workspaceRole = workspaceRole.ordinal();
        this.customRoleId = null;
    }

    /**
     * Assigns a custom role, clearing any built-in role to preserve the XOR invariant.
     */
    public void assignCustomRole(long customRoleId) {
        Assert.isTrue(customRoleId > 0, "'customRoleId' must be positive");

        this.customRoleId = customRoleId;
        this.workspaceRole = null;
    }

    public void setWorkspaceRole(Integer workspaceRole) {
        Assert.notNull(workspaceRole, "'workspaceRole' must not be null");

        WorkspaceRole[] values = WorkspaceRole.values();

        // Mirror the constructor's range check so a stray -1 / 99 from a raw-int caller (e.g., JSON payload) cannot
        // land in the DB. PermissionServiceImpl.toWorkspaceRole() already fails closed on a corrupted read, but
        // enforcing the same invariant on write prevents the bad value from being persisted in the first place.
        Assert.isTrue(
            workspaceRole >= 0 && workspaceRole < values.length,
            "'workspaceRole' ordinal " + workspaceRole + " is out of range [0," + values.length + ")");

        this.workspaceRole = workspaceRole;
        this.customRoleId = null;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (object == null || getClass() != object.getClass()) {
            return false;
        }

        WorkspaceUser workspaceUser = (WorkspaceUser) object;

        if (id == null) {
            return false;
        }

        return Objects.equals(id, workspaceUser.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "WorkspaceUser{" +
            "id=" + id +
            ", userId=" + userId +
            ", workspaceId=" + workspaceId +
            ", workspaceRole=" + workspaceRole +
            ", customRoleId=" + customRoleId +
            ", createdBy='" + createdBy + '\'' +
            ", createdDate=" + createdDate +
            ", lastModifiedBy='" + lastModifiedBy + '\'' +
            ", lastModifiedDate=" + lastModifiedDate +
            ", version=" + version +
            '}';
    }
}
