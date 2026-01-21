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

package com.bytechef.automation.data.table.configuration.domain;

import com.bytechef.platform.configuration.domain.Environment;
import java.time.Instant;
import java.util.Objects;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * @author Ivica Cardic
 */
@Table("data_table_webhook")
public class DataTableWebhook {

    @Id
    private Long id;

    private String url;

    @Column
    private int type;

    @Column("data_table_id")
    private Long dataTableId;

    @Column
    private int environment;

    @CreatedDate
    @Column("created_date")
    private Instant createdDate;

    @CreatedBy
    @Column("created_by")
    private String createdBy;

    @LastModifiedDate
    @Column("last_modified_date")
    private Instant lastModifiedDate;

    @LastModifiedBy
    @Column("last_modified_by")
    private String lastModifiedBy;

    @Version
    private Long version;

    public DataTableWebhook() {
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof DataTableWebhook dataTableWebhook)) {
            return false;
        }

        return Objects.equals(id, dataTableWebhook.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public DataTableWebhookType getType() {
        return DataTableWebhookType.values()[type];
    }

    public void setType(DataTableWebhookType type) {
        this.type = type.ordinal();
    }

    public Long getDataTableId() {
        return dataTableId;
    }

    public void setDataTableId(Long dataTableId) {
        this.dataTableId = dataTableId;
    }

    public Environment getEnvironment() {
        return Environment.values()[environment];
    }

    public void setEnvironment(Environment environment) {
        this.environment = environment.ordinal();
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Instant createdDate) {
        this.createdDate = createdDate;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Instant getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setLastModifiedDate(Instant lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public void setLastModifiedBy(String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return "DataTableWebhook{" +
            "id=" + id +
            ", url='" + url + '\'' +
            ", type=" + type +
            ", dataTableId=" + dataTableId +
            ", environment=" + environment +
            ", createdDate=" + createdDate +
            ", createdBy='" + createdBy + '\'' +
            ", lastModifiedDate=" + lastModifiedDate +
            ", lastModifiedBy='" + lastModifiedBy + '\'' +
            ", version=" + version +
            '}';
    }
}
