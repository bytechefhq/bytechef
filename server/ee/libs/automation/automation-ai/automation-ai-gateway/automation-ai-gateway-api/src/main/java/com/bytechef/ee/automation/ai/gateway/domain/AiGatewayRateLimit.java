/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.domain;

import java.time.Instant;
import java.util.Objects;
import org.apache.commons.lang3.Validate;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * @version ee
 */
@Table("ai_gateway_rate_limit")
public class AiGatewayRateLimit {

    @Column("created_date")
    @CreatedDate
    private Instant createdDate;

    @Column
    private boolean enabled;

    @Id
    private Long id;

    @Column("last_modified_date")
    @LastModifiedDate
    private Instant lastModifiedDate;

    @Column("limit_type")
    private int limitType;

    @Column("limit_value")
    private int limitValue;

    @Column
    private String name;

    @Column("project_id")
    private Long projectId;

    @Column("property_key")
    private String propertyKey;

    @Column
    private int scope;

    @Version
    private int version;

    @Column("window_seconds")
    private int windowSeconds;

    @Column("workspace_id")
    private Long workspaceId;

    private AiGatewayRateLimit() {
    }

    public AiGatewayRateLimit(
        Long workspaceId, String name, AiGatewayRateLimitScope scope,
        AiGatewayRateLimitType limitType, int limitValue, int windowSeconds) {

        Validate.notNull(workspaceId, "workspaceId must not be null");
        Validate.notBlank(name, "name must not be blank");
        Validate.notNull(scope, "scope must not be null");
        Validate.notNull(limitType, "limitType must not be null");
        Validate.isTrue(limitValue > 0, "limitValue must be positive");
        Validate.isTrue(windowSeconds > 0, "windowSeconds must be positive");

        this.enabled = true;
        this.limitType = limitType.ordinal();
        this.limitValue = limitValue;
        this.name = name;
        this.scope = scope.ordinal();
        this.windowSeconds = windowSeconds;
        this.workspaceId = workspaceId;
    }

    /**
     * Convenience factory for a PER_PROPERTY rate limit that pairs the scope with its required {@code propertyKey}. A
     * PER_PROPERTY rate limit without a property key is meaningless — it matches nothing — so forcing callers through
     * this factory prevents the "silently zero-matching limit" bug class.
     */
    public static AiGatewayRateLimit perProperty(
        Long workspaceId, String name, String propertyKey, AiGatewayRateLimitType limitType, int limitValue,
        int windowSeconds) {

        Validate.notBlank(propertyKey, "propertyKey must not be blank for PER_PROPERTY scope");

        AiGatewayRateLimit rateLimit = new AiGatewayRateLimit(
            workspaceId, name, AiGatewayRateLimitScope.PER_PROPERTY, limitType, limitValue, windowSeconds);

        rateLimit.propertyKey = propertyKey;

        return rateLimit;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof AiGatewayRateLimit aiGatewayRateLimit)) {
            return false;
        }

        return Objects.equals(id, aiGatewayRateLimit.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Long getId() {
        return id;
    }

    public Instant getLastModifiedDate() {
        return lastModifiedDate;
    }

    public AiGatewayRateLimitType getLimitType() {
        return AiGatewayRateLimitType.values()[limitType];
    }

    public int getLimitValue() {
        return limitValue;
    }

    public String getName() {
        return name;
    }

    public Long getProjectId() {
        return projectId;
    }

    public String getPropertyKey() {
        return propertyKey;
    }

    public AiGatewayRateLimitScope getScope() {
        return AiGatewayRateLimitScope.values()[scope];
    }

    public int getVersion() {
        return version;
    }

    public int getWindowSeconds() {
        return windowSeconds;
    }

    public Long getWorkspaceId() {
        return workspaceId;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setLimitType(AiGatewayRateLimitType limitType) {
        Validate.notNull(limitType, "limitType must not be null");

        this.limitType = limitType.ordinal();
    }

    public void setLimitValue(int limitValue) {
        Validate.isTrue(limitValue > 0, "limitValue must be positive");

        this.limitValue = limitValue;
    }

    public void setName(String name) {
        Validate.notBlank(name, "name must not be blank");

        this.name = name;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    /**
     * Sets the property key used to bucket PER_PROPERTY rate limits. A non-blank value is only meaningful when
     * {@link #getScope()} is {@link AiGatewayRateLimitScope#PER_PROPERTY} — callers that set a propertyKey on a
     * GLOBAL/PER_USER scope silently persist metadata that the checker ignores, so this setter rejects the combination
     * up front.
     */
    public void setPropertyKey(String propertyKey) {
        if (propertyKey != null && !propertyKey.isBlank() && getScope() != AiGatewayRateLimitScope.PER_PROPERTY) {
            throw new IllegalArgumentException(
                "propertyKey is only meaningful for PER_PROPERTY scope; current scope is " + getScope());
        }

        this.propertyKey = propertyKey;
    }

    public void setScope(AiGatewayRateLimitScope scope) {
        Validate.notNull(scope, "scope must not be null");

        // Mirror the AiObservabilityTrace cross-field invariant: if the new scope demands a propertyKey but none is
        // set, or if the scope no longer supports it but one was previously set, surface the mismatch at mutation
        // time instead of at check-time where the error lacks context.
        if (scope == AiGatewayRateLimitScope.PER_PROPERTY
            && (propertyKey == null || propertyKey.isBlank())) {

            throw new IllegalArgumentException(
                "PER_PROPERTY scope requires a non-blank propertyKey — set it before switching scope");
        }

        if (scope != AiGatewayRateLimitScope.PER_PROPERTY && propertyKey != null) {
            // Clear the stale metadata rather than leaving a zombie propertyKey that the checker will silently ignore.
            this.propertyKey = null;
        }

        this.scope = scope.ordinal();
    }

    public void setWindowSeconds(int windowSeconds) {
        Validate.isTrue(windowSeconds > 0, "windowSeconds must be positive");

        this.windowSeconds = windowSeconds;
    }

    @Override
    public String toString() {
        return "AiGatewayRateLimit{" +
            "id=" + id +
            ", workspaceId=" + workspaceId +
            ", name='" + name + '\'' +
            ", scope=" + getScope() +
            ", limitType=" + getLimitType() +
            ", limitValue=" + limitValue +
            ", windowSeconds=" + windowSeconds +
            ", enabled=" + enabled +
            ", createdDate=" + createdDate +
            ", lastModifiedDate=" + lastModifiedDate +
            ", version=" + version +
            '}';
    }
}
