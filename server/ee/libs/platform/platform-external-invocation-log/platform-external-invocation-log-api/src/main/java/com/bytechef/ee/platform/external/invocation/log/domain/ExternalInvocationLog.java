/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.external.invocation.log.domain;

import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.external.invocation.ExternalInvocationKind;
import com.bytechef.platform.external.invocation.ExternalInvocationSource;
import com.bytechef.platform.external.invocation.ExternalInvocationStatus;
import java.time.Instant;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Table("external_invocation_log")
public class ExternalInvocationLog {

    @Id
    private Long id;

    @Column("type")
    private int type;

    @Column("source")
    private int source;

    @Column("kind")
    private int kind;

    @Column("invocation_name")
    private String invocationName;

    @Column("component_name")
    private String componentName;

    @Column("component_version")
    private Integer componentVersion;

    @Column("operation_name")
    private String operationName;

    @Column("connection_id")
    private Long connectionId;

    @Column("environment")
    private Integer environment;

    @Column("workspace_id")
    private Long workspaceId;

    @Column("external_user_id")
    private String externalUserId;

    @Column("connected_user_id")
    private Long connectedUserId;

    @Column("integration_instance_id")
    private Long integrationInstanceId;

    @Column("mcp_server_id")
    private Long mcpServerId;

    @Column("a2a_server_id")
    private Long a2aServerId;

    @Column("job_id")
    private Long jobId;

    @Column("status")
    private int status;

    @Column("error_type")
    private String errorType;

    @Column("error_message")
    private String errorMessage;

    @Column("duration_ms")
    private int durationMs;

    @Column("created_date")
    private Instant createdDate;

    public ExternalInvocationLog() {
    }

    public @Nullable Long getId() {
        return id;
    }

    public PlatformType getType() {
        return PlatformType.values()[type];
    }

    public ExternalInvocationSource getSource() {
        return ExternalInvocationSource.values()[source];
    }

    public ExternalInvocationKind getKind() {
        return ExternalInvocationKind.values()[kind];
    }

    public @Nullable String getInvocationName() {
        return invocationName;
    }

    public @Nullable String getComponentName() {
        return componentName;
    }

    public @Nullable Integer getComponentVersion() {
        return componentVersion;
    }

    public @Nullable String getOperationName() {
        return operationName;
    }

    public @Nullable Long getConnectionId() {
        return connectionId;
    }

    public @Nullable Integer getEnvironment() {
        return environment;
    }

    public @Nullable Long getWorkspaceId() {
        return workspaceId;
    }

    public @Nullable String getExternalUserId() {
        return externalUserId;
    }

    public @Nullable Long getConnectedUserId() {
        return connectedUserId;
    }

    public @Nullable Long getIntegrationInstanceId() {
        return integrationInstanceId;
    }

    public @Nullable Long getMcpServerId() {
        return mcpServerId;
    }

    public @Nullable Long getA2aServerId() {
        return a2aServerId;
    }

    public @Nullable Long getJobId() {
        return jobId;
    }

    public ExternalInvocationStatus getStatus() {
        return ExternalInvocationStatus.values()[status];
    }

    public @Nullable String getErrorType() {
        return errorType;
    }

    public @Nullable String getErrorMessage() {
        return errorMessage;
    }

    public int getDurationMs() {
        return durationMs;
    }

    public @Nullable Instant getCreatedDate() {
        return createdDate;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setType(PlatformType type) {
        this.type = type.ordinal();
    }

    public void setSource(ExternalInvocationSource source) {
        this.source = source.ordinal();
    }

    public void setKind(ExternalInvocationKind kind) {
        this.kind = kind.ordinal();
    }

    public void setInvocationName(String invocationName) {
        this.invocationName = invocationName;
    }

    public void setComponentName(String componentName) {
        this.componentName = componentName;
    }

    public void setComponentVersion(Integer componentVersion) {
        this.componentVersion = componentVersion;
    }

    public void setOperationName(String operationName) {
        this.operationName = operationName;
    }

    public void setConnectionId(Long connectionId) {
        this.connectionId = connectionId;
    }

    public void setEnvironment(Integer environment) {
        this.environment = environment;
    }

    public void setWorkspaceId(Long workspaceId) {
        this.workspaceId = workspaceId;
    }

    public void setExternalUserId(String externalUserId) {
        this.externalUserId = externalUserId;
    }

    public void setConnectedUserId(Long connectedUserId) {
        this.connectedUserId = connectedUserId;
    }

    public void setIntegrationInstanceId(Long integrationInstanceId) {
        this.integrationInstanceId = integrationInstanceId;
    }

    public void setMcpServerId(Long mcpServerId) {
        this.mcpServerId = mcpServerId;
    }

    public void setA2aServerId(Long a2aServerId) {
        this.a2aServerId = a2aServerId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

    public void setStatus(ExternalInvocationStatus status) {
        this.status = status.ordinal();
    }

    public void setErrorType(String errorType) {
        this.errorType = errorType;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public void setDurationMs(int durationMs) {
        this.durationMs = durationMs;
    }

    public void setCreatedDate(Instant createdDate) {
        this.createdDate = createdDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof ExternalInvocationLog that)) {
            return false;
        }

        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "ExternalInvocationLog{" +
            "id=" + id +
            ", type=" + type +
            ", source=" + source +
            ", kind=" + kind +
            ", invocationName='" + invocationName + '\'' +
            ", status=" + status +
            ", durationMs=" + durationMs +
            '}';
    }
}
