/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.domain;

import com.bytechef.commons.data.jdbc.wrapper.MapWrapper;
import com.bytechef.platform.configuration.domain.Environment;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.ReadOnlyProperty;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * A trigger subscription owned by a connected user, backed by a hidden project and deployment.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Table("connected_user_trigger_subscription")
public class TriggerSubscription {

    @Id
    private Long id;

    @Column("connected_user_id")
    private long connectedUserId;

    @Column
    private int environment;

    @Column("component_name")
    private String componentName;

    @Column("component_version")
    private int componentVersion;

    @Column("trigger_name")
    private String triggerName;

    @Column
    private String name;

    @Column("external_id")
    private String externalId;

    @Column
    private MapWrapper parameters = new MapWrapper();

    @Column("webhook_url")
    private String webhookUrl;

    @Column
    private int status;

    @Column("error_code")
    private String errorCode;

    @Column("error_message")
    private String errorMessage;

    @Column("integration_instance_id")
    private Long integrationInstanceId;

    @Column("connection_id")
    private Long connectionId;

    @Column("project_id")
    private Long projectId;

    @Column("project_deployment_id")
    private Long projectDeploymentId;

    @Column("workflow_uuid")
    private String workflowUuid;

    @Column("last_event_date")
    @ReadOnlyProperty
    private Instant lastEventDate;

    @Column("last_delivery_status")
    @ReadOnlyProperty
    private String lastDeliveryStatus;

    @Column("last_delivery_error")
    @ReadOnlyProperty
    private String lastDeliveryError;

    @Column("registration_lease_until")
    private Instant registrationLeaseUntil;

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

    public TriggerSubscription() {
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof TriggerSubscription that)) {
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

    public long getConnectedUserId() {
        return connectedUserId;
    }

    public Environment getEnvironment() {
        return Environment.values()[environment];
    }

    public String getComponentName() {
        return componentName;
    }

    public int getComponentVersion() {
        return componentVersion;
    }

    public String getTriggerName() {
        return triggerName;
    }

    public @Nullable String getName() {
        return name;
    }

    public @Nullable String getExternalId() {
        return externalId;
    }

    public Map<String, ?> getParameters() {
        return Collections.unmodifiableMap(parameters.getMap());
    }

    public @Nullable String getWebhookUrl() {
        return webhookUrl;
    }

    public TriggerSubscriptionStatus getStatus() {
        return TriggerSubscriptionStatus.values()[status];
    }

    public @Nullable TriggerSubscriptionErrorCode getErrorCode() {
        return errorCode == null ? null : TriggerSubscriptionErrorCode.valueOf(errorCode);
    }

    public @Nullable String getErrorMessage() {
        return errorMessage;
    }

    public @Nullable Long getIntegrationInstanceId() {
        return integrationInstanceId;
    }

    public @Nullable Long getConnectionId() {
        return connectionId;
    }

    public @Nullable Long getProjectId() {
        return projectId;
    }

    public @Nullable Long getProjectDeploymentId() {
        return projectDeploymentId;
    }

    public @Nullable String getWorkflowUuid() {
        return workflowUuid;
    }

    public @Nullable Instant getLastEventDate() {
        return lastEventDate;
    }

    public @Nullable String getLastDeliveryStatus() {
        return lastDeliveryStatus;
    }

    public @Nullable String getLastDeliveryError() {
        return lastDeliveryError;
    }

    /**
     * Returns when the registration lease of the subscription expires, or null when no request holds it. A request that
     * registers the trigger sets the lease before the remote call and clears it with its final write, so a concurrent
     * enable that finds an unexpired lease leaves the registration to its holder.
     */
    public @Nullable Instant getRegistrationLeaseUntil() {
        return registrationLeaseUntil;
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

    public void setConnectedUserId(long connectedUserId) {
        this.connectedUserId = connectedUserId;
    }

    public void setEnvironment(Environment environment) {
        this.environment = environment.ordinal();
    }

    public void setComponentName(String componentName) {
        this.componentName = componentName;
    }

    public void setComponentVersion(int componentVersion) {
        this.componentVersion = componentVersion;
    }

    public void setTriggerName(String triggerName) {
        this.triggerName = triggerName;
    }

    public void setName(@Nullable String name) {
        this.name = name;
    }

    public void setExternalId(@Nullable String externalId) {
        this.externalId = externalId;
    }

    public void setParameters(@Nullable Map<String, ?> parameters) {
        this.parameters = parameters == null ? new MapWrapper() : new MapWrapper(parameters);
    }

    public void setWebhookUrl(@Nullable String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    public void setStatus(TriggerSubscriptionStatus status) {
        this.status = status.ordinal();
    }

    public void setErrorCode(@Nullable TriggerSubscriptionErrorCode errorCode) {
        this.errorCode = errorCode == null ? null : errorCode.name();
    }

    public void setErrorMessage(@Nullable String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public void setIntegrationInstanceId(@Nullable Long integrationInstanceId) {
        this.integrationInstanceId = integrationInstanceId;
    }

    public void setConnectionId(@Nullable Long connectionId) {
        this.connectionId = connectionId;
    }

    public void setProjectId(@Nullable Long projectId) {
        this.projectId = projectId;
    }

    public void setProjectDeploymentId(@Nullable Long projectDeploymentId) {
        this.projectDeploymentId = projectDeploymentId;
    }

    public void setWorkflowUuid(@Nullable String workflowUuid) {
        this.workflowUuid = workflowUuid;
    }

    public void setRegistrationLeaseUntil(@Nullable Instant registrationLeaseUntil) {
        this.registrationLeaseUntil = registrationLeaseUntil;
    }

    @Override
    public String toString() {
        return "TriggerSubscription{" +
            "id=" + id +
            ", connectedUserId=" + connectedUserId +
            ", environment=" + environment +
            ", componentName='" + componentName + '\'' +
            ", componentVersion=" + componentVersion +
            ", triggerName='" + triggerName + '\'' +
            ", name='" + name + '\'' +
            ", externalId='" + externalId + '\'' +
            ", status=" + status +
            ", errorCode='" + errorCode + '\'' +
            ", integrationInstanceId=" + integrationInstanceId +
            ", connectionId=" + connectionId +
            ", projectId=" + projectId +
            ", projectDeploymentId=" + projectDeploymentId +
            ", workflowUuid='" + workflowUuid + '\'' +
            ", registrationLeaseUntil=" + registrationLeaseUntil +
            ", version=" + version +
            '}';
    }
}
