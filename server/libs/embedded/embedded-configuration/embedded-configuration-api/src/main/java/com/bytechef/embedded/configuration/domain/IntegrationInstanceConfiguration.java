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

package com.bytechef.embedded.configuration.domain;

import com.bytechef.commons.data.jdbc.wrapper.EncryptedMapWrapper;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.platform.constant.Environment;
import com.bytechef.platform.tag.domain.Tag;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.lang.Nullable;

/**
 * @author Ivica Cardic
 */
@Table("integration_instance_configuration")
public class IntegrationInstanceConfiguration {

    @Column("connection_parameters")
    private EncryptedMapWrapper connectionParameters;

    @CreatedBy
    @Column("created_by")
    private String createdBy;

    @Column("created_date")
    @CreatedDate
    private LocalDateTime createdDate;

    @Column
    private String description;

    @Column
    private boolean enabled;

    @Column
    private int environment;

    @Id
    private Long id;

    @Column("integration_id")
    private AggregateReference<Integration, Long> integrationId;

    @Column("integration_version")
    private Integer integrationVersion;

    @MappedCollection(idColumn = "integration_instance_configuration_id")
    private Set<IntegrationInstanceConfigurationTag> integrationInstanceConfigurationTags = Collections.emptySet();

    @Column
    private String name;

    @Column("last_modified_by")
    @LastModifiedBy
    private String lastModifiedBy;

    @Column("last_modified_date")
    @LastModifiedDate
    private LocalDateTime lastModifiedDate;

    @Version
    private int version;

    public IntegrationInstanceConfiguration() {
        this.connectionParameters = new EncryptedMapWrapper(Collections.emptyMap());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        IntegrationInstanceConfiguration integration = (IntegrationInstanceConfiguration) o;

        return Objects.equals(id, integration.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    public Map<String, ?> getParameters() {
        return Collections.unmodifiableMap(connectionParameters == null ? Map.of() : connectionParameters.getMap());
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public String getDescription() {
        return description;
    }

    public Environment getEnvironment() {
        return Environment.values()[environment];
    }

    public Long getId() {
        return id;
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public LocalDateTime getLastModifiedDate() {
        return lastModifiedDate;
    }

    public Long getIntegrationId() {
        return integrationId == null ? null : integrationId.getId();
    }

    @Nullable
    public Integer getIntegrationVersion() {
        return integrationVersion;
    }

    public String getName() {
        return name;
    }

    public List<Long> getTagIds() {
        return integrationInstanceConfigurationTags
            .stream()
            .map(IntegrationInstanceConfigurationTag::getTagId)
            .toList();
    }

    public int getVersion() {
        return version;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setEnvironment(Environment environment) {
        this.environment = environment.ordinal();
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setIntegrationId(Long integrationId) {
        this.integrationId = integrationId == null ? null : AggregateReference.to(integrationId);
    }

    public void setIntegrationVersion(Integer integrationVersion) {
        this.integrationVersion = integrationVersion;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setConnectionParameters(Map<String, ?> connectionParameters) {
        if (!MapUtils.isEmpty(connectionParameters)) {
            this.connectionParameters = new EncryptedMapWrapper(connectionParameters);
        }
    }

    public void setTagIds(List<Long> tagIds) {
        this.integrationInstanceConfigurationTags = new HashSet<>();

        if (!CollectionUtils.isEmpty(tagIds)) {
            for (Long tagId : tagIds) {
                integrationInstanceConfigurationTags.add(new IntegrationInstanceConfigurationTag(tagId));
            }
        }
    }

    public void setTags(List<Tag> tags) {
        if (!CollectionUtils.isEmpty(tags)) {
            setTagIds(CollectionUtils.map(tags, Tag::getId));
        }
    }

    public void setVersion(int version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return "IntegrationInstanceConfiguration{" +
            "id=" + id +
            ", name='" + name + '\'' +
            ", description='" + description + '\'' +
            ", enabled=" + enabled +
            ", environment='" + environment + '\'' +
            ", integrationId=" + integrationId +
            ", integrationInstanceConfigurationTags=" + integrationInstanceConfigurationTags +
            ", connectionParameters=" + connectionParameters +
            ", createdBy='" + createdBy + '\'' +
            ", createdDate=" + createdDate +
            ", lastModifiedBy='" + lastModifiedBy + '\'' +
            ", lastModifiedDate=" + lastModifiedDate +
            ", version=" + version +
            '}';
    }
}
