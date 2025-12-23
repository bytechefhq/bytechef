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

package com.bytechef.ee.embedded.security.domain;

import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.user.domain.User;
import java.time.Instant;
import java.util.Objects;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * @author Ivica Cardic
 */
@Table("signing_key")
public class SigningKey {

    @Id
    private Long id;

    @CreatedBy
    @Column("created_by")
    private String createdBy;

    @Column("created_date")
    @CreatedDate
    private Instant createdDate;

    @Column("key_id")
    private String keyId;

    @Column("last_modified_by")
    @LastModifiedBy
    private String lastModifiedBy;

    @Column("last_modified_date")
    @LastModifiedDate
    private Instant lastModifiedDate;

    @Column("last_used_date")
    private Instant lastUsedDate;

    @Column
    private String name;

    @Column
    private String publicKey;

    @Column
    private int environment;

    @Column
    private int type;

    @Column
    private AggregateReference<User, Long> userId;

    public SigningKey() {
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof SigningKey that)) {
            return false;
        }

        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    public Long getId() {
        return id;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public String getKeyId() {
        return keyId;
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public Instant getLastModifiedDate() {
        return lastModifiedDate;
    }

    public Instant getLastUsedDate() {
        return lastUsedDate;
    }

    public String getName() {
        return name;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public PlatformType getType() {
        return PlatformType.values()[type];
    }

    public Environment getEnvironment() {
        return Environment.values()[environment];
    }

    public Long getUserId() {
        return userId.getId();
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public void setKeyId(String keyId) {
        this.keyId = keyId;
    }

    public void setLastUsedDate(Instant lastUsedDate) {
        this.lastUsedDate = lastUsedDate;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public void setType(PlatformType type) {
        this.type = type.ordinal();
    }

    public void setEnvironment(Environment environment) {
        if (environment != null) {
            this.environment = environment.ordinal();
        }
    }

    public void setUserId(Long userId) {
        this.userId = AggregateReference.to(userId);
    }

    @Override
    public String toString() {
        return "SigningKey{" +
            "id=" + id +
            ",keyId=" + keyId +
            ", name='" + name + '\'' +
            ", type=" + type +
            ", publicKey='" + publicKey + '\'' +
            ", userId=" + userId +
            ", lastUsedDate='" + lastUsedDate + '\'' +
            ", createdBy='" + createdBy + '\'' +
            ", createdDate=" + createdDate +
            '}';
    }
}
