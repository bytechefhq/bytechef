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

package com.bytechef.platform.mcp.domain;

import com.bytechef.platform.tag.domain.Tag;
import java.util.Objects;
import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Domain class representing a join entity between {@link McpServer} and {@link Tag}.
 *
 * @author Ivica Cardic
 */
@Table("mcp_server_tag")
public final class McpServerTag {

    @Column("tag_id")
    private AggregateReference<Tag, Long> tagId;

    private McpServerTag() {
    }

    public McpServerTag(long tagId) {
        this.tagId = AggregateReference.to(tagId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof McpServerTag that)) {
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
        return "McpServerTag{" +
            "tagId=" + tagId +
            '}';
    }
}
