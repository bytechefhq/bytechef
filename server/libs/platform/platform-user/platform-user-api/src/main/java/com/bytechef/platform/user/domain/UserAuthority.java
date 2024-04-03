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

package com.bytechef.platform.user.domain;

import java.util.Objects;
import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("user_authority")
public class UserAuthority {

    @Column("authority_id")
    private AggregateReference<Authority, Long> authorityId;

    private UserAuthority() {
    }

    public UserAuthority(Long authorityId) {
        this.authorityId = authorityId == null ? null : AggregateReference.to(authorityId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof UserAuthority that)) {
            return false;
        }

        return Objects.equals(authorityId, that.authorityId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(authorityId);
    }

    public Long getAuthorityId() {
        return authorityId.getId();
    }

    @Override
    public String toString() {
        return "UserAuthority{" +
            "authorityId=" + authorityId +
            '}';
    }
}
