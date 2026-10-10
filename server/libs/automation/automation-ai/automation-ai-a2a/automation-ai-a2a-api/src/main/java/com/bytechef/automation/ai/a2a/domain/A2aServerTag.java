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

package com.bytechef.automation.ai.a2a.domain;

import java.util.Objects;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Join entity between an {@link A2aServer} and a tag.
 *
 * @author Ivica Cardic
 */
@Table("a2a_server_tag")
public final class A2aServerTag {

    @Column("tag_id")
    private Long tagId;

    private A2aServerTag() {
    }

    public A2aServerTag(long tagId) {
        this.tagId = tagId;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof A2aServerTag a2aServerTag)) {
            return false;
        }

        return Objects.equals(tagId, a2aServerTag.tagId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tagId);
    }

    public Long getTagId() {
        return tagId;
    }

    @Override
    public String toString() {
        return "A2aServerTag{" +
            "tagId=" + tagId +
            '}';
    }
}
