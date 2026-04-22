/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.domain;

import java.util.Objects;
import org.apache.commons.lang3.Validate;
import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * @version ee
 */
@Table("ai_observability_trace_tag")
public final class AiObservabilityTraceTag {

    @Column("tag_id")
    private AggregateReference<AiGatewayTag, Long> tagId;

    private AiObservabilityTraceTag() {
    }

    public AiObservabilityTraceTag(Long tagId) {
        Validate.notNull(tagId, "tagId must not be null");

        this.tagId = AggregateReference.to(tagId);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof AiObservabilityTraceTag that)) {
            return false;
        }

        return Objects.equals(getTagId(), that.getTagId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getTagId());
    }

    public Long getTagId() {
        return tagId == null ? null : tagId.getId();
    }

    @Override
    public String toString() {
        return "AiObservabilityTraceTag{" +
            "tagId=" + tagId +
            '}';
    }
}
