/*
 * Copyright 2025 ByteChef
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

package com.bytechef.automation.ai.agent.domain;

import com.bytechef.platform.tag.domain.Tag;
import java.util.Objects;
import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Join row of the {@code ai_agent_tag} many-to-many relation. Only the tag side is mapped — the owning
 * {@code ai_agent_id} is written by Spring Data JDBC from {@link AiAgent}'s {@code @MappedCollection}. Same shape as
 * {@code ProjectTag}/{@code DataTableTag}.
 *
 * @author Ivica Cardic
 */
@Table("ai_agent_tag")
public final class AiAgentTag {

    @Column("tag_id")
    private AggregateReference<Tag, Long> tagId;

    private AiAgentTag() {
    }

    public AiAgentTag(Long tagId) {
        this.tagId = tagId == null ? null : AggregateReference.to(tagId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof AiAgentTag that)) {
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
        return "AiAgentTag{" +
            "tagId=" + tagId +
            '}';
    }
}
