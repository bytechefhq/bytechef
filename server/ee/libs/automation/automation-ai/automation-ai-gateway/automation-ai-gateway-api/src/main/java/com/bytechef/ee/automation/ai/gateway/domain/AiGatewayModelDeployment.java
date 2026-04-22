/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.domain;

import java.util.Objects;
import org.apache.commons.lang3.Validate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * @version ee
 */
@Table("ai_gateway_model_deployment")
public class AiGatewayModelDeployment {

    @Column
    private boolean enabled;

    @Id
    private Long id;

    @Column("max_rpm")
    private Integer maxRpm;

    @Column("max_tpm")
    private Integer maxTpm;

    @Column("model_id")
    private Long modelId;

    @Column("priority_order")
    private int priorityOrder;

    @Column("routing_policy_id")
    private Long routingPolicyId;

    @Version
    private int version;

    @Column
    private int weight;

    private AiGatewayModelDeployment() {
    }

    public AiGatewayModelDeployment(Long routingPolicyId, Long modelId) {
        Validate.notNull(routingPolicyId, "routingPolicyId must not be null");
        Validate.notNull(modelId, "modelId must not be null");

        this.enabled = true;
        this.modelId = modelId;
        this.priorityOrder = 0;
        this.routingPolicyId = routingPolicyId;
        this.weight = 1;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof AiGatewayModelDeployment aiGatewayModelDeployment)) {
            return false;
        }

        return Objects.equals(id, aiGatewayModelDeployment.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Long getId() {
        return id;
    }

    public Integer getMaxRpm() {
        return maxRpm;
    }

    public Integer getMaxTpm() {
        return maxTpm;
    }

    public Long getModelId() {
        return modelId;
    }

    public int getPriorityOrder() {
        return priorityOrder;
    }

    public Long getRoutingPolicyId() {
        return routingPolicyId;
    }

    public int getWeight() {
        return weight;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setMaxRpm(Integer maxRpm) {
        if (maxRpm != null) {
            Validate.isTrue(maxRpm > 0, "maxRpm must be positive when set, got: %d", maxRpm);
        }

        this.maxRpm = maxRpm;
    }

    public void setMaxTpm(Integer maxTpm) {
        if (maxTpm != null) {
            Validate.isTrue(maxTpm > 0, "maxTpm must be positive when set, got: %d", maxTpm);
        }

        this.maxTpm = maxTpm;
    }

    public void setPriorityOrder(int priorityOrder) {
        Validate.isTrue(priorityOrder >= 0, "priorityOrder must be non-negative, got: %d", priorityOrder);

        this.priorityOrder = priorityOrder;
    }

    public void setWeight(int weight) {
        Validate.isTrue(weight > 0, "weight must be positive, got: %d", weight);

        this.weight = weight;
    }

    @Override
    public String toString() {
        return "AiGatewayModelDeployment{" +
            "id=" + id +
            ", routingPolicyId=" + routingPolicyId +
            ", modelId=" + modelId +
            ", weight=" + weight +
            ", priorityOrder=" + priorityOrder +
            ", enabled=" + enabled +
            '}';
    }
}
