/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.apiplatform.configuration.domain;

import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.platform.tag.domain.Tag;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
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
@Table("api_collection")
public class ApiCollection {

    @Id
    private Long id;

    @Column("project_deployment_id")
    private AggregateReference<ProjectDeployment, Long> projectDeploymentId;

    @Column
    private String name;

    @Column("context_path")
    private String contextPath;

    @Column
    private String description;

    @Column("collection_version")
    private Integer collectionVersion;

    @MappedCollection(idColumn = "api_collection_id")
    private Set<ApiCollectionTag> apiCollectionTags = new HashSet<>();

    @CreatedBy
    @Column("created_by")
    private String createdBy;

    @Column("created_date")
    @CreatedDate
    private Instant createdDate;

    @Column("last_modified_by")
    @LastModifiedBy
    private String lastModifiedBy;

    @Column("last_modified_date")
    @LastModifiedDate
    private Instant lastModifiedDate;

    @Version
    private int version;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof ApiCollection openApiConnector)) {
            return false;
        }

        return Objects.equals(id, openApiConnector.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    public Long getId() {
        return id;
    }

    public Long getProjectDeploymentId() {
        return projectDeploymentId == null ? null : projectDeploymentId.getId();
    }

    public String getName() {
        return name;
    }

    public String getContextPath() {
        return contextPath;
    }

    public Integer getCollectionVersion() {
        return collectionVersion;
    }

    public String getDescription() {
        return description;
    }

    public List<Long> getTagIds() {
        return apiCollectionTags
            .stream()
            .map(ApiCollectionTag::getTagId)
            .toList();
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

    public int getVersion() {
        return version;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setProjectDeploymentId(Long projectDeploymentId) {
        this.projectDeploymentId = projectDeploymentId == null ? null : AggregateReference.to(projectDeploymentId);
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setContextPath(String contextPath) {
        this.contextPath = contextPath;
    }

    public void setCollectionVersion(int collectionVersion) {
        this.collectionVersion = collectionVersion;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setTagIds(List<Long> tagIds) {
        this.apiCollectionTags = new HashSet<>();

        if (!CollectionUtils.isEmpty(tagIds)) {
            for (Long tagId : tagIds) {
                apiCollectionTags.add(new ApiCollectionTag(tagId));
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
        return "ApiCollection{" +
            "id=" + id +
            ", projectDeploymentId=" + projectDeploymentId +
            ", name='" + name + '\'' +
            ", contextPath='" + contextPath + '\'' +
            ", collectionVersion=" + collectionVersion +
            ", description='" + description + '\'' +
            ", apiCollectionTags=" + apiCollectionTags +
            ", createdBy='" + createdBy + '\'' +
            ", createdDate=" + createdDate +
            ", lastModifiedBy='" + lastModifiedBy + '\'' +
            ", lastModifiedDate=" + lastModifiedDate +
            ", version=" + version +
            '}';
    }
}
