/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.domain;

import java.util.Objects;
import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * @version ee
 */
@Table("ai_gateway_routing_policy_tag")
public final class AiGatewayRoutingPolicyTag {

    @Column("tag_id")
    private AggregateReference<AiGatewayTag, Long> tagId;

    private AiGatewayRoutingPolicyTag() {
    }

    public AiGatewayRoutingPolicyTag(Long tagId) {
        this.tagId = tagId == null ? null : AggregateReference.to(tagId);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof AiGatewayRoutingPolicyTag that)) {
            return false;
        }

        return Objects.equals(tagId, that.tagId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tagId);
    }

    public Long getTagId() {
        return tagId.getId();
    }

    @Override
    public String toString() {
        return "AiGatewayRoutingPolicyTag{" +
            "tagId=" + tagId +
            '}';
    }
}
