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

package com.bytechef.platform.security.domain;

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
@Table("api_key")
public class ApiKey {

    @CreatedBy
    @Column("created_by")
    private String createdBy;

    @Column("created_date")
    @CreatedDate
    private Instant createdDate;

    @Id
    private Long id;

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
    private int environment;

    @Column("secret_key")
    private String secretKey;

    @Column
    private Integer type;

    @Column
    private AggregateReference<User, Long> userId;

    public ApiKey() {
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof ApiKey apiKey)) {
            return false;
        }

        return Objects.equals(id, apiKey.id);
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

    public String getSecretKey() {
        return secretKey;
    }

    public PlatformType getType() {
        if (type == null) {
            return null;
        }

        return PlatformType.values()[type];
    }

    public Environment getEnvironment() {
        return Environment.values()[environment];
    }

    public Long getUserId() {
        return userId.getId();
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public void setType(PlatformType type) {
        if (type != null) {
            this.type = type.ordinal();
        }
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
        return "APIKey{" +
            "id=" + id +
            ", name='" + name + '\'' +
            ", type=" + type +
            ", userId=" + userId +
            ", createdBy='" + createdBy + '\'' +
            ", createdDate=" + createdDate +
            ", secretKey='" + secretKey + '\'' +
            ", lastUsedDate=" + lastUsedDate +
            '}';
    }
}
