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
@Table("ai_gateway_project")
public class AiGatewayProject {

    @Column("caching_enabled")
    private Boolean cachingEnabled;

    @Column("cache_ttl_minutes")
    private Integer cacheTtlMinutes;

    @Column("compression_enabled")
    private Boolean compressionEnabled;

    @Column("created_date")
    @CreatedDate
    private Instant createdDate;

    @Column
    private String description;

    @Id
    private Long id;

    @Column("last_modified_date")
    @LastModifiedDate
    private Instant lastModifiedDate;

    @Column("log_retention_days")
    private Integer logRetentionDays;

    @Column
    private String name;

    @Column("retry_max_attempts")
    private Integer retryMaxAttempts;

    @Column("routing_policy_id")
    private Long routingPolicyId;

    @Column
    private String slug;

    @Column("timeout_seconds")
    private Integer timeoutSeconds;

    @Version
    private int version;

    @Column("workspace_id")
    private Long workspaceId;

    private AiGatewayProject() {
    }

    public AiGatewayProject(long workspaceId, String name, String slug) {
        Validate.notNull(name, "name must not be null");
        Validate.notNull(slug, "slug must not be null");

        this.name = name;
        this.slug = slug;
        this.workspaceId = workspaceId;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof AiGatewayProject aiGatewayProject)) {
            return false;
        }

        return Objects.equals(id, aiGatewayProject.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    public Boolean getCachingEnabled() {
        return cachingEnabled;
    }

    public Integer getCacheTtlMinutes() {
        return cacheTtlMinutes;
    }

    public Boolean getCompressionEnabled() {
        return compressionEnabled;
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

    public Instant getLastModifiedDate() {
        return lastModifiedDate;
    }

    public Integer getLogRetentionDays() {
        return logRetentionDays;
    }

    public String getName() {
        return name;
    }

    public Integer getRetryMaxAttempts() {
        return retryMaxAttempts;
    }

    public Long getRoutingPolicyId() {
        return routingPolicyId;
    }

    public String getSlug() {
        return slug;
    }

    public Integer getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public int getVersion() {
        return version;
    }

    public Long getWorkspaceId() {
        return workspaceId;
    }

    public void setCachingEnabled(Boolean cachingEnabled) {
        this.cachingEnabled = cachingEnabled;
    }

    public void setCacheTtlMinutes(Integer cacheTtlMinutes) {
        this.cacheTtlMinutes = cacheTtlMinutes;
    }

    public void setCompressionEnabled(Boolean compressionEnabled) {
        this.compressionEnabled = compressionEnabled;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setLogRetentionDays(Integer logRetentionDays) {
        this.logRetentionDays = logRetentionDays;
    }

    public void setName(String name) {
        Validate.notNull(name, "name must not be null");

        this.name = name;
    }

    public void setRetryMaxAttempts(Integer retryMaxAttempts) {
        this.retryMaxAttempts = retryMaxAttempts;
    }

    public void setRoutingPolicyId(Long routingPolicyId) {
        this.routingPolicyId = routingPolicyId;
    }

    public void setSlug(String slug) {
        Validate.notNull(slug, "slug must not be null");

        this.slug = slug;
    }

    public void setTimeoutSeconds(Integer timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    @Override
    public String toString() {
        return "AiGatewayProject{" +
            "id=" + id +
            ", workspaceId=" + workspaceId +
            ", name='" + name + '\'' +
            ", slug='" + slug + '\'' +
            ", description='" + description + '\'' +
            ", routingPolicyId=" + routingPolicyId +
            ", createdDate=" + createdDate +
            ", lastModifiedDate=" + lastModifiedDate +
            ", version=" + version +
            '}';
    }
}
