/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.domain;

import java.math.BigDecimal;
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
@Table("ai_gateway_model")
public class AiGatewayModel {

    @Column
    private String alias;

    @Column
    private String capabilities;

    @Column("context_window")
    private Integer contextWindow;

    @Column("created_date")
    @CreatedDate
    private Instant createdDate;

    @Column("default_routing_policy_id")
    private Long defaultRoutingPolicyId;

    @Column
    private boolean enabled;

    @Id
    private Long id;

    @Column("input_cost_per_m_tokens")
    private BigDecimal inputCostPerMTokens;

    @Column("last_modified_date")
    @LastModifiedDate
    private Instant lastModifiedDate;

    @Column
    private String name;

    @Column("output_cost_per_m_tokens")
    private BigDecimal outputCostPerMTokens;

    @Column("provider_id")
    private Long providerId;

    @Version
    private int version;

    private AiGatewayModel() {
    }

    public AiGatewayModel(Long providerId, String name) {
        Validate.notNull(providerId, "providerId must not be null");
        Validate.notBlank(name, "name must not be blank");

        this.enabled = true;
        this.name = name;
        this.providerId = providerId;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof AiGatewayModel aiGatewayModel)) {
            return false;
        }

        return Objects.equals(id, aiGatewayModel.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    public String getAlias() {
        return alias;
    }

    public String getCapabilities() {
        return capabilities;
    }

    public Integer getContextWindow() {
        return contextWindow;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public Long getDefaultRoutingPolicyId() {
        return defaultRoutingPolicyId;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Long getId() {
        return id;
    }

    public BigDecimal getInputCostPerMTokens() {
        return inputCostPerMTokens;
    }

    /**
     * Currency-aware view of {@link #getInputCostPerMTokens()}. The cost calculator should prefer this to ensure rate ×
     * tokens arithmetic stays in a consistent currency. USD today; a future per-provider currency column on
     * {@code AiGatewayModel} would change only this accessor's Money construction, not every caller.
     */
    public Money getInputCostPerMTokensAsMoney() {
        return inputCostPerMTokens == null ? null : Money.usd(inputCostPerMTokens);
    }

    public Instant getLastModifiedDate() {
        return lastModifiedDate;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getOutputCostPerMTokens() {
        return outputCostPerMTokens;
    }

    /**
     * Currency-aware view of {@link #getOutputCostPerMTokens()}. See {@link #getInputCostPerMTokensAsMoney()} for the
     * adoption rationale.
     */
    public Money getOutputCostPerMTokensAsMoney() {
        return outputCostPerMTokens == null ? null : Money.usd(outputCostPerMTokens);
    }

    public Long getProviderId() {
        return providerId;
    }

    public int getVersion() {
        return version;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public void setCapabilities(String capabilities) {
        this.capabilities = capabilities;
    }

    public void setContextWindow(Integer contextWindow) {
        if (contextWindow != null) {
            Validate.isTrue(contextWindow > 0, "contextWindow must be positive");
        }

        this.contextWindow = contextWindow;
    }

    public void setDefaultRoutingPolicyId(Long defaultRoutingPolicyId) {
        this.defaultRoutingPolicyId = defaultRoutingPolicyId;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setInputCostPerMTokens(BigDecimal inputCostPerMTokens) {
        if (inputCostPerMTokens != null) {
            Validate.isTrue(
                inputCostPerMTokens.compareTo(BigDecimal.ZERO) >= 0, "inputCostPerMTokens must not be negative");
        }

        this.inputCostPerMTokens = inputCostPerMTokens;
    }

    public void setName(String name) {
        Validate.notBlank(name, "name must not be blank");

        this.name = name;
    }

    public void setOutputCostPerMTokens(BigDecimal outputCostPerMTokens) {
        if (outputCostPerMTokens != null) {
            Validate.isTrue(
                outputCostPerMTokens.compareTo(BigDecimal.ZERO) >= 0, "outputCostPerMTokens must not be negative");
        }

        this.outputCostPerMTokens = outputCostPerMTokens;
    }

    @Override
    public String toString() {
        return "AiGatewayModel{" +
            "id=" + id +
            ", name='" + name + '\'' +
            ", alias='" + alias + '\'' +
            ", providerId=" + providerId +
            ", enabled=" + enabled +
            ", createdDate=" + createdDate +
            ", lastModifiedDate=" + lastModifiedDate +
            ", version=" + version +
            '}';
    }
}
