
/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.athena.configuration.domain;

import com.bytechef.tag.domain.Tag;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Persistable;
import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.util.Objects;

/**
 * @author Ivica Cardic
 */
@Table("integration_tag")
public final class IntegrationTag implements Persistable<Long> {

    @Id
    private Long id;

    @Column("tag_id")
    private AggregateReference<Tag, Long> tagId;

    public IntegrationTag() {
    }

    public IntegrationTag(Long tagId) {
        this.tagId = tagId == null ? null : AggregateReference.to(tagId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        IntegrationTag that = (IntegrationTag) o;

        return Objects.equals(id, that.id) && Objects.equals(tagId, that.tagId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public Long getId() {
        return id;
    }

    public Long getTagId() {
        return tagId.getId();
    }

    @Override
    public boolean isNew() {
        return id == null;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "IntegrationTag{id='"
            + id + '\'' + ", tagId='"
            + getTagId() + '\'' + '}';
    }
}
