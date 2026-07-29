/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.tool.invocation.log;

import com.bytechef.platform.tool.execution.ToolExecutionKind;
import com.bytechef.platform.tool.execution.ToolExecutionOutcome;
import com.bytechef.platform.tool.execution.ToolExecutionSurface;
import java.time.Instant;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * An append-only record of a single direct tool/action execution. Enum-typed dimensions are persisted by ordinal.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Table("tool_invocation_log")
public class ToolInvocationLog {

    @Id
    private Long id;

    @Column("tenant_id")
    private String tenantId;

    @Column("surface")
    private int surface;

    @Column("kind")
    private int kind;

    @Column("tool_name")
    private String toolName;

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

    @Column("job_id")
    private Long jobId;

    @Column("outcome")
    private int outcome;

    @Column("error_type")
    private String errorType;

    @Column("error_message")
    private String errorMessage;

    @Column("duration_ms")
    private int durationMs;

    @Column("created_date")
    private Instant createdDate;

    public ToolInvocationLog() {
    }

    public @Nullable Long getId() {
        return id;
    }

    public @Nullable String getTenantId() {
        return tenantId;
    }

    public ToolExecutionSurface getSurface() {
        return ToolExecutionSurface.values()[surface];
    }

    public ToolExecutionKind getKind() {
        return ToolExecutionKind.values()[kind];
    }

    public @Nullable String getToolName() {
        return toolName;
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

    public @Nullable Long getJobId() {
        return jobId;
    }

    public ToolExecutionOutcome getOutcome() {
        return ToolExecutionOutcome.values()[outcome];
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

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public void setSurface(ToolExecutionSurface surface) {
        this.surface = surface.ordinal();
    }

    public void setKind(ToolExecutionKind kind) {
        this.kind = kind.ordinal();
    }

    public void setToolName(String toolName) {
        this.toolName = toolName;
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

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

    public void setOutcome(ToolExecutionOutcome outcome) {
        this.outcome = outcome.ordinal();
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

        if (!(o instanceof ToolInvocationLog that)) {
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
        return "ToolInvocationLog{" +
            "id=" + id +
            ", surface=" + surface +
            ", kind=" + kind +
            ", toolName='" + toolName + '\'' +
            ", outcome=" + outcome +
            ", durationMs=" + durationMs +
            '}';
    }
}
