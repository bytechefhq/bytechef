/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.apiplatform.configuration.domain;

import com.bytechef.automation.configuration.domain.ProjectInstanceWorkflow;
import java.time.LocalDateTime;
import java.util.Objects;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Table("api_collection_endpoint")
public class ApiCollectionEndpoint {

    public enum HttpMethod {
        DELETE, GET, PATCH, POST, PUT
    }

    @Column("api_collection_id")
    private AggregateReference<ApiCollection, Long> apiCollectionId;

    @CreatedBy
    @Column("created_by")
    private String createdBy;

    @Column("created_date")
    @CreatedDate
    private LocalDateTime createdDate;

    @Column("http_method")
    private int httpMethod;

    @Id
    private Long id;

    @Column("last_modified_by")
    @LastModifiedBy
    private String lastModifiedBy;

    @Column("last_modified_date")
    @LastModifiedDate
    private LocalDateTime lastModifiedDate;

    @Column
    private String name;

    @Column
    private String path;

    @Column("project_instance_workflow_id")
    private AggregateReference<ProjectInstanceWorkflow, Long> projectInstanceWorkflowId;

    @Column("workflow_reference_code")
    private String workflowReferenceCode;

    @Version
    private int version;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof ApiCollectionEndpoint openApiConnector)) {
            return false;
        }

        return Objects.equals(id, openApiConnector.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    public Long getApiCollectionId() {
        return apiCollectionId.getId();
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

    public HttpMethod getHttpMethod() {
        return HttpMethod.values()[httpMethod];
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public LocalDateTime getLastModifiedDate() {
        return lastModifiedDate;
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    public Long getProjectInstanceWorkflowId() {
        return projectInstanceWorkflowId.getId();
    }

    public int getVersion() {
        return version;
    }

    public String getWorkflowReferenceCode() {
        return workflowReferenceCode;
    }

    public void setApiCollectionId(Long apiCollectionId) {
        this.apiCollectionId = AggregateReference.to(apiCollectionId);
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

//    public void setProjectInstanceWorkflow(ProjectInstanceWorkflow projectInstanceWorkflow) {
//        this.projectInstanceWorkflowId = AggregateReference.to(projectInstanceWorkflow.getId());
//    }

    public void setProjectInstanceWorkflowId(Long projectInstanceWorkflowId) {
        this.projectInstanceWorkflowId = AggregateReference.to(projectInstanceWorkflowId);
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public void setWorkflowReferenceCode(String workflowReferenceCode) {
        this.workflowReferenceCode = workflowReferenceCode;
    }

    @Override
    public String toString() {
        return "ApiCollectionEndpoint{" +
            "id=" + id +
            ", name='" + name + '\'' +
            ", path='" + path + '\'' +
            ", httpMethod='" + httpMethod + '\'' +
            ", workflowReferenceCode='" + workflowReferenceCode + '\'' +
            ", createdBy='" + createdBy + '\'' +
            ", createdDate=" + createdDate +
            ", lastModifiedBy='" + lastModifiedBy + '\'' +
            ", lastModifiedDate=" + lastModifiedDate +
            ", version=" + version +
            '}';
    }
}
