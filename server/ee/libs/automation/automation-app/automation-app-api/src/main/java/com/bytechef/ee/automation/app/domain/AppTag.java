/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.domain;

import java.util.Objects;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * A tag of an app.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Table("app_tag")
public final class AppTag {

    @Column("tag_id")
    private Long tagId;

    private AppTag() {
    }

    public AppTag(Long tagId) {
        this.tagId = tagId;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof AppTag that)) {
            return false;
        }

        return Objects.equals(tagId, that.tagId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(tagId);
    }

    public Long getTagId() {
        return tagId;
    }

    @Override
    public String toString() {
        return "AppTag{tagId=" + tagId + '}';
    }
}
