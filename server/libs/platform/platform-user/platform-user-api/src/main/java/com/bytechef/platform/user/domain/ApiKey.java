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

import com.bytechef.platform.constant.AppType;
import com.bytechef.platform.constant.Environment;
import java.time.LocalDateTime;
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
    private LocalDateTime createdDate;

    @Column
    private int environment;

    @Id
    private Long id;

    @Column("last_modified_by")
    @LastModifiedBy
    private String lastModifiedBy;

    @Column("last_modified_date")
    @LastModifiedDate
    private LocalDateTime lastModifiedDate;

    @Column("last_used_date")
    private LocalDateTime lastUsedDate;

    @Column
    private String name;

    @Column("secret_key")
    private String secretKey;

    @Column
    private int type;

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

    public Environment getEnvironment() {
        return Environment.values()[environment];
    }

    public Long getId() {
        return id;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public LocalDateTime getLastModifiedDate() {
        return lastModifiedDate;
    }

    public LocalDateTime getLastUsedDate() {
        return lastUsedDate;
    }

    public String getName() {
        return name;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public AppType getType() {
        return AppType.values()[type];
    }

    public Long getUserId() {
        return userId.getId();
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setEnvironment(Environment environment) {
        this.environment = environment.ordinal();
    }

    public void setLastUsedDate(LocalDateTime lastUsedDate) {
        this.lastUsedDate = lastUsedDate;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public void setType(AppType type) {
        this.type = type.ordinal();
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
            ", environment='" + environment + '\'' +
            ", createdBy='" + createdBy + '\'' +
            ", createdDate=" + createdDate +
            ", secretKey='" + secretKey + '\'' +
            ", lastUsedDate=" + lastUsedDate +
            '}';
    }
}
