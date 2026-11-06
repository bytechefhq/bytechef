/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * @version ee
 */
@Table("ai_gateway_connected_user_settings")
public final class AiGatewayConnectedUserSettings {

    @Column("budget_cap")
    private @Nullable BigDecimal budgetCap;

    @Column("connected_user_id")
    private long connectedUserId;

    @Column("created_date")
    @CreatedDate
    private Instant createdDate;

    @Id
    private Long id;

    @Column("last_modified_date")
    @LastModifiedDate
    private Instant lastModifiedDate;

    @Column("routing_policy_id")
    private @Nullable Long routingPolicyId;

    @Version
    private int version;

    private AiGatewayConnectedUserSettings() {
    }

    public AiGatewayConnectedUserSettings(long connectedUserId) {
        this.connectedUserId = connectedUserId;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof AiGatewayConnectedUserSettings settings)) {
            return false;
        }

        return Objects.equals(id, settings.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    public @Nullable BigDecimal getBudgetCap() {
        return budgetCap;
    }

    public long getConnectedUserId() {
        return connectedUserId;
    }

    public Long getId() {
        return id;
    }

    public @Nullable Long getRoutingPolicyId() {
        return routingPolicyId;
    }

    public void setBudgetCap(@Nullable BigDecimal budgetCap) {
        this.budgetCap = budgetCap;
    }

    public void setRoutingPolicyId(@Nullable Long routingPolicyId) {
        this.routingPolicyId = routingPolicyId;
    }

    @Override
    public String toString() {
        return "AiGatewayConnectedUserSettings{id=" + id + ", connectedUserId=" + connectedUserId +
            ", routingPolicyId=" + routingPolicyId + ", budgetCap=" + budgetCap + '}';
    }
}
