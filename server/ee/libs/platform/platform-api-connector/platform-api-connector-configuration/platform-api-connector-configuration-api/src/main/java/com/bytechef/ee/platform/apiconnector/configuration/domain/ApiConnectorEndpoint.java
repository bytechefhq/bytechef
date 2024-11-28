/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.apiconnector.configuration.domain;

import java.time.Instant;
import java.util.Objects;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.data.relational.core.mapping.Column;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
//@Table("api_connector_endpoint")
public class ApiConnectorEndpoint {

    public enum HttpMethod {
        DELETE, GET, PATCH, POST, PUT
    }

    @Column("api_collection_id")
    private AggregateReference<ApiConnector, Long> apiConnectorId;

    @CreatedBy
    @Column("created_by")
    private String createdBy;

    @Column("created_date")
    @CreatedDate
    private Instant createdDate;

    @Column
    private String description;

    @Column("http_method")
    private int httpMethod;

    @Id
    private Long id;

    @Column("last_modified_by")
    @LastModifiedBy
    private String lastModifiedBy;

    @Column("last_modified_date")
    @LastModifiedDate
    private Instant lastModifiedDate;

    @Column
    private String name;

    @Column
    private String path;

    @Version
    private int version;

    public ApiConnectorEndpoint() {
    }

    public ApiConnectorEndpoint(String path, String name, String description, HttpMethod httpMethod) {
        this.path = path;
        this.name = name;
        this.description = description;
        this.httpMethod = httpMethod.ordinal();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof ApiConnectorEndpoint openApiConnector)) {
            return false;
        }

        return Objects.equals(id, openApiConnector.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    public Long getApiConnectorId() {
        return apiConnectorId.getId();
    }

    public String getDescription() {
        return description;
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

    public HttpMethod getHttpMethod() {
        return HttpMethod.values()[httpMethod];
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public Instant getLastModifiedDate() {
        return lastModifiedDate;
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    public int getVersion() {
        return version;
    }

    public void setApiConnectorId(Long apiCollectionId) {
        this.apiConnectorId = AggregateReference.to(apiCollectionId);
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setHttpMethod(HttpMethod httpMethod) {
        this.httpMethod = httpMethod.ordinal();
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return "OpenApiEndpoint{" +
            "id=" + id +
            ", path='" + path + '\'' +
            ", httpMethod='" + httpMethod + '\'' +
            ", name='" + name + '\'' +
            ", description='" + description + '\'' +
            ", createdBy='" + createdBy + '\'' +
            ", createdDate=" + createdDate +
            ", lastModifiedBy='" + lastModifiedBy + '\'' +
            ", lastModifiedDate=" + lastModifiedDate +
            ", version=" + version +
            '}';
    }
}
