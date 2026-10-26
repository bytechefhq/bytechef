/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.domain;

import com.bytechef.commons.util.JsonUtils;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.apache.commons.lang3.Validate;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Table;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Table("ai_gateway_routing_policy")
public final class AiGatewayRoutingPolicy {

    @Column
    private String config;

    @Column("created_date")
    @CreatedDate
    private Instant createdDate;

    @Column
    private boolean enabled;

    @Column("fallback_model")
    private String fallbackModel;

    @Id
    private Long id;

    @Column("last_modified_date")
    @LastModifiedDate
    private Instant lastModifiedDate;

    @Column
    private String name;

    @Column
    private int strategy;

    @MappedCollection(idColumn = "ai_gateway_routing_policy_id")
    private Set<AiGatewayRoutingPolicyTag> tags = new HashSet<>();

    @Version
    private int version;

    @Column("workspace_id")
    private @Nullable Long workspaceId;

    private AiGatewayRoutingPolicy() {
    }

    public AiGatewayRoutingPolicy(String name, AiGatewayRoutingStrategyType strategy) {
        Validate.notBlank(name, "name must not be blank");
        Validate.notNull(strategy, "strategy must not be null");

        this.enabled = true;
        this.name = name;
        this.strategy = strategy.ordinal();
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof AiGatewayRoutingPolicy aiGatewayRoutingPolicy)) {
            return false;
        }

        return Objects.equals(id, aiGatewayRoutingPolicy.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    public String getConfig() {
        return config;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getFallbackModel() {
        return fallbackModel;
    }

    public Long getId() {
        return id;
    }

    public Instant getLastModifiedDate() {
        return lastModifiedDate;
    }

    public String getName() {
        return name;
    }

    public AiGatewayRoutingStrategyType getStrategy() {
        return AiGatewayRoutingStrategyType.values()[strategy];
    }

    @Nullable
    public AiGatewayRoutingStrategyConfig getTypedStrategyConfig() {
        if (config == null || config.isBlank()) {
            return null;
        }

        AiGatewayRoutingStrategyConfig typed = JsonUtils.read(config, AiGatewayRoutingStrategyConfig.class);

        if (typed.strategyType() != getStrategy()) {
            throw new IllegalStateException(
                "Routing policy " + id + " config discriminator " + typed.strategyType()
                    + " does not match declared strategy " + getStrategy());
        }

        return typed;
    }

    public void applyStrategyConfig(AiGatewayRoutingStrategyConfig typedConfig) {
        Validate.notNull(typedConfig, "typedConfig must not be null");

        this.strategy = typedConfig.strategyType()
            .ordinal();
        this.config = JsonUtils.write(typedConfig);
        this.fallbackModel = typedConfig instanceof AiGatewayRoutingStrategyConfig.PriorityFallback priorityFallback
            ? priorityFallback.fallbackModel()
            : null;
    }

    public List<Long> getTagIds() {
        return tags.stream()
            .map(AiGatewayRoutingPolicyTag::getTagId)
            .toList();
    }

    public Set<AiGatewayRoutingPolicyTag> getTags() {
        return Set.copyOf(tags);
    }

    public int getVersion() {
        return version;
    }

    public @Nullable Long getWorkspaceId() {
        return workspaceId;
    }

    public void setConfig(String config) {
        if (config != null && !config.isBlank()) {
            JsonUtils.read(config, AiGatewayRoutingStrategyConfig.class);
        }

        this.config = config;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setFallbackModel(String fallbackModel) {
        this.fallbackModel = fallbackModel;
    }

    public void setName(String name) {
        Validate.notBlank(name, "name must not be blank");

        this.name = name;
    }

    public void setStrategy(AiGatewayRoutingStrategyType strategy) {
        Validate.notNull(strategy, "strategy must not be null");

        this.strategy = strategy.ordinal();
    }

    public void setTags(Set<AiGatewayRoutingPolicyTag> tags) {
        this.tags = new HashSet<>(tags);
    }

    public void setWorkspaceId(@Nullable Long workspaceId) {
        this.workspaceId = workspaceId;
    }

    @Override
    public String toString() {
        return "AiGatewayRoutingPolicy{" +
            "id=" + id +
            ", name='" + name + '\'' +
            ", strategy=" + getStrategy() +
            ", enabled=" + enabled +
            ", createdDate=" + createdDate +
            ", lastModifiedDate=" + lastModifiedDate +
            ", version=" + version +
            ", workspaceId=" + workspaceId +
            '}';
    }
}
