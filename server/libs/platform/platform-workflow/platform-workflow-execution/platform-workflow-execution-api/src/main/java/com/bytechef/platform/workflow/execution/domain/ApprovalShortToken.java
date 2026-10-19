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

package com.bytechef.platform.workflow.execution.domain;

import java.time.Instant;
import java.util.Objects;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * @author Ivica Cardic
 */
@Table("approval_short_token")
public final class ApprovalShortToken {

    @Column("created_date")
    @CreatedDate
    private Instant createdDate;

    @Column("expiry_date")
    private Instant expiryDate;

    @Id
    private Long id;

    @Column("short_id")
    private String shortId;

    @Column
    private String token;

    @Column("token_hash")
    private String tokenHash;

    private ApprovalShortToken() {
    }

    public ApprovalShortToken(String shortId, String token, String tokenHash, Instant expiryDate) {
        this.expiryDate = Objects.requireNonNull(expiryDate, "expiryDate");
        this.shortId = Objects.requireNonNull(shortId, "shortId");
        this.token = Objects.requireNonNull(token, "token");
        this.tokenHash = Objects.requireNonNull(tokenHash, "tokenHash");
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public Instant getExpiryDate() {
        return expiryDate;
    }

    public Long getId() {
        return id;
    }

    public String getShortId() {
        return shortId;
    }

    public String getToken() {
        return token;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public boolean isExpired(Instant now) {
        return expiryDate.isBefore(now);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (object == null || getClass() != object.getClass()) {
            return false;
        }

        ApprovalShortToken approvalShortToken = (ApprovalShortToken) object;

        return Objects.equals(id, approvalShortToken.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "ApprovalShortToken{" +
            "id=" + id +
            ", shortId='" + shortId + '\'' +
            ", expiryDate=" + expiryDate +
            ", createdDate=" + createdDate +
            '}';
    }
}
