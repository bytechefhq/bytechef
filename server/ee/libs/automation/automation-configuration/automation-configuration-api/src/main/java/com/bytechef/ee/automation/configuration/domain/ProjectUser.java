/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.domain;

import com.bytechef.ee.automation.configuration.security.constant.ProjectRole;
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
@Table("project_user")
public class ProjectUser {

    @CreatedBy
    @Column("created_by")
    private String createdBy;

    @Column("created_date")
    @CreatedDate
    private Instant createdDate;

    @Column("custom_role_id")
    private Long customRoleId;

    @Id
    private Long id;

    @Column("last_modified_by")
    @LastModifiedBy
    private String lastModifiedBy;

    @Column("last_modified_date")
    @LastModifiedDate
    private Instant lastModifiedDate;

    @Column("project_id")
    private Long projectId;

    @Column("project_role")
    private Integer projectRole;

    @Column("user_id")
    private Long userId;

    @Version
    private int version;

    /**
     * Required by Spring Data JDBC reflection. Application code must use the public constructor so the role invariants
     * are validated.
     */
    private ProjectUser() {
    }

    /**
     * Creates a project membership with a built-in {@link ProjectRole}. Use {@link #forCustomRole(Long, Long, long)} to
     * construct a custom-role member instead.
     */
    public static ProjectUser forBuiltInRole(Long projectId, Long userId, ProjectRole projectRole) {
        Assert.notNull(projectRole, "'projectRole' must not be null");

        return new ProjectUser(projectId, userId, projectRole.ordinal());
    }

    /**
     * Creates a project membership with a custom role (no built-in {@code projectRole}).
     */
    public static ProjectUser forCustomRole(Long projectId, Long userId, long customRoleId) {
        Assert.notNull(projectId, "'projectId' must not be null");
        Assert.notNull(userId, "'userId' must not be null");
        Assert.isTrue(customRoleId > 0, "'customRoleId' must be positive");

        ProjectUser projectUser = new ProjectUser(projectId, userId, null, customRoleId);

        return projectUser;
    }

    /**
     * Internal constructor used by the {@link #forBuiltInRole(Long, Long, ProjectRole)} and
     * {@link #forCustomRole(Long, Long, long)} factories. Validates the ordinal against {@link ProjectRole} so
     * out-of-range values cannot persist, AND enforces the class-level XOR invariant: exactly one of
     * {@code projectRole} or {@code customRoleId} is non-null. Kept package-private so external callers cannot bypass
     * the factory invariants; Spring Data JDBC reconstructs instances via the no-arg constructor and field injection,
     * which bypasses this check \u2014 the DB CHECK constraint remains the safety net for persisted rows.
     */
    ProjectUser(Long projectId, Long userId, Integer projectRole) {
        this(projectId, userId, projectRole, null);
    }

    ProjectUser(Long projectId, Long userId, Integer projectRole, Long customRoleId) {
        Assert.notNull(projectId, "'projectId' must not be null");
        Assert.notNull(userId, "'userId' must not be null");

        boolean hasBuiltInRole = projectRole != null;
        boolean hasCustomRole = customRoleId != null;

        Assert.isTrue(
            hasBuiltInRole ^ hasCustomRole,
            "Exactly one of 'projectRole' or 'customRoleId' must be set (XOR invariant)");

        if (hasBuiltInRole) {
            ProjectRole[] values = ProjectRole.values();

            Assert.isTrue(
                projectRole >= 0 && projectRole < values.length,
                "'projectRole' ordinal " + projectRole + " is out of range [0," + values.length + ")");
        }

        this.projectId = projectId;
        this.userId = userId;
        this.projectRole = projectRole;
        this.customRoleId = customRoleId;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public Long getCustomRoleId() {
        return customRoleId;
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

    public Long getProjectId() {
        return projectId;
    }

    public Integer getProjectRole() {
        return projectRole;
    }

    public Long getUserId() {
        return userId;
    }

    public int getVersion() {
        return version;
    }

    public void assignBuiltInRole(ProjectRole projectRole) {
        Assert.notNull(projectRole, "'projectRole' must not be null");

        this.projectRole = projectRole.ordinal();
        this.customRoleId = null;
    }

    public void assignCustomRole(long customRoleId) {
        this.customRoleId = customRoleId;
        this.projectRole = null;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (object == null || getClass() != object.getClass()) {
            return false;
        }

        ProjectUser projectUser = (ProjectUser) object;

        if (id == null) {
            return false;
        }

        return Objects.equals(id, projectUser.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "ProjectUser{" +
            "id=" + id +
            ", projectId=" + projectId +
            ", userId=" + userId +
            ", projectRole=" + projectRole +
            ", customRoleId=" + customRoleId +
            ", createdBy='" + createdBy + '\'' +
            ", createdDate=" + createdDate +
            ", lastModifiedBy='" + lastModifiedBy + '\'' +
            ", lastModifiedDate=" + lastModifiedDate +
            ", version=" + version +
            '}';
    }
}
