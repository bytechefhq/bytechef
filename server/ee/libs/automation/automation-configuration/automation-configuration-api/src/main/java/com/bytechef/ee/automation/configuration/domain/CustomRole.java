/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.domain;

import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.util.Assert;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Table("custom_role")
public class CustomRole {

    @CreatedBy
    @Column("created_by")
    private String createdBy;

    @Column("created_date")
    @CreatedDate
    private Instant createdDate;

    @Column("description")
    private String description;

    @Id
    private Long id;

    @Column("last_modified_by")
    @LastModifiedBy
    private String lastModifiedBy;

    @Column("last_modified_date")
    @LastModifiedDate
    private Instant lastModifiedDate;

    @Column("name")
    private String name;

    @MappedCollection(idColumn = "custom_role_id")
    private Set<CustomRoleScope> scopes = new HashSet<>();

    @Version
    private int version;

    /**
     * Required by Spring Data JDBC reflection. Application code must use {@link #CustomRole(String, Set)} so the
     * non-blank-name and non-empty-scopes invariants are validated at construction rather than at save time.
     */
    private CustomRole() {
    }

    /**
     * Creates a validated custom role. Enforces the invariants that persistence alone cannot: a non-blank name and a
     * non-empty scope set (a role with no scopes grants no permissions, which is almost never what the caller means and
     * would leave affected users silently locked out).
     */
    public CustomRole(String name, Set<String> scopeNames) {
        Assert.hasText(name, "'name' must not be blank");
        Assert.notEmpty(scopeNames, "'scopeNames' must not be empty");

        this.name = name;
        this.scopes = scopeNames.stream()
            .map(CustomRoleScope::new)
            .collect(Collectors.toCollection(HashSet::new));
    }

    /**
     * Returns the granted scope names, unwrapping the persistence-layer {@link CustomRoleScope} wrappers. Prefer this
     * over {@link #getScopes()} when you just need the logical scope set — the wrapper only exists because Spring Data
     * JDBC requires a mapped row entity for child collections. Validity of a name (membership in the registry) is the
     * service layer's concern, not this entity's.
     */
    public Set<String> getScopeNames() {
        return scopes.stream()
            .map(CustomRoleScope::scope)
            .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Replaces the scope set from a collection of scope names. Internally wraps each name in a {@link CustomRoleScope}
     * for Spring Data JDBC persistence. Enforces the same {@code notEmpty} invariant as
     * {@link #CustomRole(String, Set)} so a role cannot be silently mutated to grant zero permissions, which would lock
     * out every member holding only this role.
     */
    public void setScopeNames(Set<String> scopeNames) {
        Assert.notEmpty(scopeNames, "'scopeNames' must not be empty");

        this.scopes = scopeNames.stream()
            .map(CustomRoleScope::new)
            .collect(Collectors.toCollection(HashSet::new));
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public String getDescription() {
        return description;
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

    public String getName() {
        return name;
    }

    public Set<CustomRoleScope> getScopes() {
        return Collections.unmodifiableSet(scopes);
    }

    public int getVersion() {
        return version;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Mirrors the non-blank invariant enforced by {@link #CustomRole(String, Set)} so a role cannot be renamed to a
     * blank string after construction. Without this guard the constructor's {@code hasText} check is asymmetric and the
     * {@code update} path lets {@code ""} through, undermining the invariant the class Javadoc documents.
     */
    public void setName(String name) {
        Assert.hasText(name, "'name' must not be blank");

        this.name = name;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (object == null || getClass() != object.getClass()) {
            return false;
        }

        CustomRole customRole = (CustomRole) object;

        return Objects.equals(id, customRole.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "CustomRole{" +
            "id=" + id +
            ", name='" + name + '\'' +
            ", description='" + description + '\'' +
            ", scopes=" + scopes +
            ", createdBy='" + createdBy + '\'' +
            ", createdDate=" + createdDate +
            ", lastModifiedBy='" + lastModifiedBy + '\'' +
            ", lastModifiedDate=" + lastModifiedDate +
            ", version=" + version +
            '}';
    }
}
