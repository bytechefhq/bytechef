/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.domain;

import com.bytechef.commons.data.jdbc.wrapper.EncryptedMapWrapper;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.tag.domain.Tag;
import java.time.Instant;
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

/**
 * @version ee
 *
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
    private Instant createdDate;

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
    private Instant lastModifiedDate;

    @Column("authorization_type")
    private int authorizationType;

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

    public String getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public String getDescription() {
        return description;
    }

    public Environment getEnvironment() {
        return Environment.values()[environment];
    }

    public long getEnvironmentId() {
        return environment;
    }

    public Long getId() {
        return id;
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public Instant getLastModifiedDate() {
        return lastModifiedDate;
    }

    public Long getIntegrationId() {
        return integrationId == null ? null : integrationId.getId();
    }

    public Integer getIntegrationVersion() {
        return integrationVersion;
    }

    public String getName() {
        return name;
    }

    public Map<String, ?> getConnectionParameters() {
        return Collections.unmodifiableMap(connectionParameters == null ? Map.of() : connectionParameters.getMap());
    }

    public List<Long> getTagIds() {
        return integrationInstanceConfigurationTags.stream()
            .map(IntegrationInstanceConfigurationTag::getTagId)
            .toList();
    }

    public AuthorizationType getAuthorizationType() {
        return AuthorizationType.values()[authorizationType];
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
        if (environment != null) {
            this.environment = environment.ordinal();
        }
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

    public void setAuthorizationType(AuthorizationType authorizationType) {
        this.authorizationType = authorizationType.ordinal();
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
            ", integrationVersion=" + integrationVersion +
            ", integrationInstanceConfigurationTags=" + integrationInstanceConfigurationTags +
            ", connectionParameters=" + connectionParameters +
            ", authorizationType=" + authorizationType +
            ", createdBy='" + createdBy + '\'' +
            ", createdDate=" + createdDate +
            ", lastModifiedBy='" + lastModifiedBy + '\'' +
            ", lastModifiedDate=" + lastModifiedDate +
            ", version=" + version +
            '}';
    }
}
