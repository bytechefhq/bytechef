/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.domain;

import java.util.Objects;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * A deployed Call-workflow workflow bound to an app under a workflow key. The workflow is identified by its project
 * deployment and its stable uuid; the version-specific workflow id is resolved on every call.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Table("app_workflow")
public class AppWorkflow {

    @Column("app_id")
    private AggregateReference<App, Long> appId;

    @Id
    private Long id;

    @Column("project_deployment_id")
    private Long projectDeploymentId;

    @Version
    private int version;

    @Column("workflow_key")
    private String workflowKey;

    @Column("workflow_uuid")
    private String workflowUuid;

    public AppWorkflow() {
    }

    public Long getAppId() {
        return appId.getId();
    }

    public void setAppId(Long appId) {
        this.appId = AggregateReference.to(appId);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProjectDeploymentId() {
        return projectDeploymentId;
    }

    public void setProjectDeploymentId(Long projectDeploymentId) {
        this.projectDeploymentId = projectDeploymentId;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public String getWorkflowKey() {
        return workflowKey;
    }

    public void setWorkflowKey(String workflowKey) {
        this.workflowKey = workflowKey;
    }

    public String getWorkflowUuid() {
        return workflowUuid;
    }

    public void setWorkflowUuid(String workflowUuid) {
        this.workflowUuid = workflowUuid;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof AppWorkflow that)) {
            return false;
        }

        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
