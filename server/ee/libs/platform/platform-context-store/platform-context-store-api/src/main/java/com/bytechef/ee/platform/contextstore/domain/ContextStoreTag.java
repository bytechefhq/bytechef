/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.contextstore.domain;

import com.bytechef.platform.tag.domain.Tag;
import java.util.Objects;
import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Join entity backing the {@code context_store_tag} table. Mirrors {@code KnowledgeBaseTag} — keys a Context Store to a
 * platform tag, used as a {@code @MappedCollection} on {@link ContextStore}.
 *
 * @author Ivica Cardic
 * @version ee
 */
@Table("context_store_tag")
public final class ContextStoreTag {

    @Column("tag_id")
    private AggregateReference<Tag, Long> tagId;

    public ContextStoreTag() {
    }

    public ContextStoreTag(Long tagId) {
        this.tagId = tagId == null ? null : AggregateReference.to(tagId);
    }

    public Long getTagId() {
        return tagId == null ? null : tagId.getId();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof ContextStoreTag that)) {
            return false;
        }

        return Objects.equals(tagId, that.tagId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tagId);
    }

    @Override
    public String toString() {
        return "ContextStoreTag{" +
            "tagId=" + tagId +
            '}';
    }
}
