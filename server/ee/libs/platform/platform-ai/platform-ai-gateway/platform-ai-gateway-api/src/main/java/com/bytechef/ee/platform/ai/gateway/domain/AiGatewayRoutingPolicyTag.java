/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.domain;

import com.bytechef.platform.tag.domain.Tag;
import java.util.Objects;
import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Routing-policy ↔ tag join. Mirrors {@code com.bytechef.automation.configuration.domain.ProjectTag} — the embedded
 * value object on the parent's {@code tags} aggregate carrying just the platform {@link Tag} reference. The DB-level
 * composite PK on (ai_gateway_routing_policy_id, tag_id) lives in the Liquibase init.
 *
 * @version ee
 */
@Table("ai_gateway_routing_policy_tag")
public final class AiGatewayRoutingPolicyTag {

    @Column("tag_id")
    private AggregateReference<Tag, Long> tagId;

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
